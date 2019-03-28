package main;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import helper.ExternalProcess2;
import hybridResources.JSConsoleReader;
import me.tongfei.progressbar.ProgressBar;
import model.AndroidNode;
import model.HybridError;
import model.State;
import model.Transition;
import model.TransitionType;

public class Rip2 {

	/*
	 * Environment variables
	 */
	private String aapt = System.getenv("AAPT_LOCATION");

	/*
	 * Execution settings
	 */

	/**
	 * Indicates if the application is hybrid
	 */
	private boolean hybridApp = true;

	/**
	 * Maximum number of interactions
	 */
	private int maxInteractions;

	/**
	 * Contextual exploration strategy in the device
	 */
	private boolean contextualExploration = false;

	/*
	 * Execution variables
	 */
	/**
	 * The application was installed
	 */
	private boolean appInstalled;

	/**
	 * Folder where the results will be exported
	 */
	private String folderName;

	/**
	 * Location of the APK file
	 */
	private String apkLocation;

	/**
	 * Package name of the application
	 */
	private String packageName;

	private int sequentialNumber;

	/**
	 * Main activity of the application
	 */
	private String mainActivity;

	private State currentState;

	private Hashtable<String, State> statesTable;

	private ArrayList<State> states;

	private ArrayList<Transition> transitions;

	/*
	 * Device information
	 */

	/**
	 * Android version
	 */
	private String version;

	private int resolution;

	private String dimensions;

	private String sensors;

	private String services;

	private FileWriter out;

	private int waitingTime;

	private boolean isRunning;

	private String pacName;

	private boolean rippingOutsideApp;

	private Rip2(String apkPath, String outputFolder, String emulatorId) throws RipException {
		pacName = "";
		isRunning = true;
		statesTable = new Hashtable<>();
		states = new ArrayList<>();
		transitions = new ArrayList<>();
		waitingTime = 500;
		Date nowFolder = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("hhmmss");
		String time = dateFormat.format(nowFolder);
//		folderName = "./generated/generated_" + time;
		folderName ="./"+ outputFolder;
		File newFolder = new File(folderName);
		newFolder.mkdirs();

		// Captures the Android version of the device
		try {
			version = ExternalProcess2.getAndroidVersion(emulatorId);
		} catch (IOException | RipException e) {
			e.printStackTrace();
		}
		apkLocation = apkPath;
		// Installs the APK in the device
		appInstalled = installAPK(apkLocation, emulatorId);

		if (!appInstalled) {
			throw new RipException("APK could not be installed");
		}

		if(aapt==null) {
			throw new RipException("AAPT_LOCATION was not set");
		}

		// Launches the applications' main activity
		try {
			packageName = ExternalProcess2.getPackageName(aapt, apkLocation);
			mainActivity = ExternalProcess2.getMainActivity(aapt, apkLocation);
			ExternalProcess2.startActivity(packageName, mainActivity, emulatorId);
			ProgressBar pb = new ProgressBar("Waiting for the app", 100);
			pb.start();
			for (int i = 5; i > 0; i--) {
				pb.stepBy(20);
				TimeUnit.SECONDS.sleep(1);
			}
			pb.stop();

		} catch (IOException | RipException | InterruptedException e) {
			e.printStackTrace();
		}

		JSConsoleReader jsConsoleReader = null;
		// Explores the application
		if (hybridApp) {
			jsConsoleReader = new JSConsoleReader(this);
			jsConsoleReader.start();
		}

		Transition initialTransition = new Transition(null, TransitionType.FIRST_INTERACTION);
		State initialState = new State(hybridApp, contextualExploration);
		initialState.setId(getSequentialNumber());
		explore(initialState, initialTransition, emulatorId);

		buildFiles();
		System.out.println("EXPLORATION FINISHED, " + statesTable.size() + " states discovered");
		if(jsConsoleReader != null) {
			jsConsoleReader.kill();
		}

		// try {
		// ExternalProcess2.readWebViewConsole();
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }

	}

	private void buildFiles() {

		transitions.forEach(t -> System.out.println(t.toString()));

	}

	private void explore(State previousState, Transition executedTransition, String emulatorId) {
		currentState = new State(hybridApp, contextualExploration);
		try {
			String rawXML = ExternalProcess2.getCurrentViewHierarchy(emulatorId);
			Document parsedXML;
			try {
				parsedXML = loadXMLFromString(rawXML);

				currentState.setParsedXML(parsedXML);
				currentState.setRawXML(rawXML);

				State foundState = findStateInGraph(currentState);
				if (foundState != null) {
					// State already exists
					currentState = foundState;

				} else {
					// New state discovered
					currentState.setId(getSequentialNumber());
					String screenShot = takeScreenShot(emulatorId);
					System.out.println("Current ST: " + currentState.getId());
					State sameState = compareScreenShotWithExisting(screenShot);
					rippingOutsideApp = isRippingOutsideApp(parsedXML);
					if (sameState == null && !rippingOutsideApp) {
						statesTable.put(rawXML, currentState);
						states.add(currentState);
						currentState.setScreenShot(screenShot);
					} else {
						// Discard state
						deleteFile(screenShot);
						currentState = sameState;
						if(rippingOutsideApp) {
							currentState = previousState;
						}
					}
				}

				if (currentState.hasRemainingTransitions() && !rippingOutsideApp) {
					previousState.addPossibleTransition(executedTransition);
				}

				if (!rippingOutsideApp) {
					currentState.addInboundTransition(executedTransition);
					previousState.addOutboundTransition(executedTransition);
					executedTransition.setDestination(currentState);
					executedTransition.setOrigin(previousState);
					transitions.add(executedTransition);
				}

				Transition stateTransition = null;
				TransitionType transitionType = null;
				boolean stateChanges = false;

				// While no changes in in the state are detected
				while (!stateChanges) {
					stateTransition = currentState.popTransition();
					//transitionType = executeTransition(stateTransition);
					// Waits until the executed transition changes the application current state
					waitTime(waitingTime);
					// Checks if the application changes due to the executed transition
					stateChanges = stateChanges(emulatorId);
				}

				// If the state changes, recursively explores the application
				if (stateChanges) {
					explore(currentState, stateTransition, emulatorId);
				}

			} catch (NoSuchElementException e) {
				// There are no more possible transitions in the current state
			} catch (CrawlingOutsideAppException e) {
				// The new state is out of the domain of the current application
			} catch (ParserConfigurationException | SAXException e) {
				// Error parsing the XML DOM
				e.printStackTrace();
			}
		} catch (IOException | RipException e) {
			// Error getting the current view hierarchy
			e.printStackTrace();
		} finally {
		}
	}

	private boolean isRippingOutsideApp(Document parsedXML) throws IOException, RipException {
		String currentPackage = parsedXML.getElementsByTagName("node").item(0).getAttributes().getNamedItem("package")
				.getNodeValue();
		if (pacName.equals("")) {
			pacName = currentPackage;
		}
		System.out.println("pacName: " + pacName);
		System.out.println("packageName: " + packageName);
		// Is exploring outside the application
		if (!currentPackage.equals(pacName)) {
			System.out.println("Ripping outside");
			ExternalProcess2.goBack();
			return true;
		}
		return false;
	}

	private void deleteFile(String file) {

		File toDelete = new File(file);
		toDelete.delete();

	}

	private State compareScreenShotWithExisting(String screenShot) {
		File existing = new File(screenShot);
		for (int i = states.size()-1; i>=0; i--) {
			State state = states.get(i);
			double percentage = compareImage(new File(state.getScreenShot()), existing);
			System.out.println(percentage + " " + state.getId());
			if (percentage >= 97.5) {
				System.out.println("Same!");
				return state;
			}
		}
		return null;
	}

	private int getSequentialNumber() {
		sequentialNumber++;
		return sequentialNumber;
	}

	private String takeScreenShot(String emulatorId) throws IOException, RipException {
		String screencap = "/sdcard/" + currentState.getId() + ".png";
		String screenCapName = currentState.getId() + ".png";
		String local = folderName + File.separator + screenCapName;
		ExternalProcess2.pullScreenshot(screencap, screencap, local, emulatorId);
		return local;
	}

	/**
	 * Explores the existing states to determine if pState exists
	 * 
	 * @param pState
	 */
	private State findStateInGraph(State pState) {
		State found = statesTable.get(pState.getRawXML());
		return found;
	}

	private boolean stateChanges(String emulatorId) throws CrawlingOutsideAppException, IOException, RipException {
		String rawXML = ExternalProcess2.getCurrentViewHierarchy(emulatorId);
		if (rawXML.equals(currentState.getRawXML())) {
			return false;
		}
		return true;
	}

	public TransitionType executeTransition(Transition transition) throws IOException, RipException {
		switch (transition.getType()) {
		case GUI_CLICK_BUTTON:
			AndroidNode origin = transition.getOriginNode();
			tap(origin);
			return TransitionType.GUI_CLICK_BUTTON;
		case CONTEXT_INTERNET_OFF:
			turnInternet(false);
			return TransitionType.CONTEXT_INTERNET_OFF;
		case CONTEXT_INTERNET_ON:
			turnInternet(true);
			return TransitionType.CONTEXT_INTERNET_ON;
		case ROTATE_LANDSCAPE:
			rotateDevice(true);
			return TransitionType.ROTATE_LANDSCAPE;
		case ROTATE_PORTRAIT:
			rotateDevice(false);
			return TransitionType.ROTATE_PORTRAIT;
		case CONTEXT_LOCATION_OFF:
			turnLocation(false);
			return TransitionType.CONTEXT_LOCATION_OFF;
		case CONTEXT_LOCATION_ON:
			turnLocation(true);
			return TransitionType.CONTEXT_LOCATION_ON;
		case BUTTON_BACK:
			goBack();
		}
		return null;

	}

	private void goBack() throws IOException, RipException {
		ExternalProcess2.goBack();
	}

	/**
	 * Changes the status of the internet.
	 * 
	 * @param value
	 *            true ON, false OFF
	 * @throws RipException
	 * @throws IOException
	 */
	public void turnInternet(boolean value) throws IOException, RipException {
		ExternalProcess2.turnInternet(value);
	}

	public void turnLocation(boolean value) throws IOException, RipException {
		ExternalProcess2.turnLocationServices(value);
	}

	/**
	 * Rotates the device
	 * 
	 * @param value
	 *            true landscape, false portrait
	 * @throws RipException
	 * @throws IOException
	 */
	public void rotateDevice(boolean value) throws IOException, RipException {
		if (value) {
			ExternalProcess2.rotateLandscape();
		} else {
			ExternalProcess2.rotatePortrait();
		}
	}

	public void tap(AndroidNode node) throws IOException, RipException {
		ExternalProcess2.tap(node.getCentralX() + "", node.getCentralY() + "");
	}

	public static Document loadXMLFromString(String xml)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}

	private void logMessage(String command, String[] parameters, String errorMessage) {
		// try {
		if (errorMessage == null) {
			// out.write("COMMAND:" + command +"; PARAMETERS" + parameters.toString() +
			// "\n");
			// System.out.println("COMMAND:" + command +"; PARAMETERS" +
			// parameters.toString() + "\n");
		} else {
			// out.write("ERROR "+command);
			// out.write("COMMAND:" + command +"; PARAMETERS" + parameters.toString() +
			// "\n");
			// System.out.println("COMMAND:" + command +"; PARAMETERS" +
			// parameters.toString() + "\n");
		}
		// out.flush();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

	private boolean installAPK(String pathAPK, String emulatorId) {

		try {
			ExternalProcess2.installAPK(pathAPK, emulatorId);
			logMessage("INSTALL APP", new String[] { pathAPK }, null);
			return true;

		} catch (Exception e) {
			logMessage("INSTALL APP", new String[] { pathAPK }, e.getMessage());
			return false;
		}
	}

	/**
	 * Waits pMilliseconds
	 * 
	 * @param pMilliseconds
	 */
	public void waitTime(int pMilliseconds) {
		try {
			Thread.sleep(pMilliseconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void addDetectedErrorToCurrentHybridState(HybridError error) {
		currentState.addError(error);
	}

	public static void main(String[] args) {

		if(args.length<3) {
			System.err.println("Some arguments are missing, please provide apk location, outputfolder and emulator id");
		} else {
			System.out.println("\n 2018, Universidad de los Andes\n The Software Design Lab\n");
			System.out.println("https://thesoftwaredesignlab.github.io/\n");
			String s = String.join("\n", "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥", "🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥",
					"🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥", "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥 ",
					"🔥🔥   🔥🔥    🔥🔥  🔥🔥          ", "🔥🔥    🔥🔥   🔥🔥  🔥🔥          ",
					"🔥🔥     🔥🔥  🔥🔥  🔥🔥          ", " ");


			System.out.println(s);
			try {
				//args[0] apkLocation, args[1] outputFolder, args[2] emulator id
				new Rip2(args[0], args[1], args[2]);
			} catch (RipException e) {
				e.printStackTrace();
			}
		}
	}

	public double compareImage(File fileA, File fileB) {

		float percentage = 0;
		try {
			BufferedImage biA = ImageIO.read(fileA);
			DataBuffer dbA = biA.getData().getDataBuffer();
			int sizeA = dbA.getSize();
			BufferedImage biB = ImageIO.read(fileB);
			DataBuffer dbB = biB.getData().getDataBuffer();
			int sizeB = dbB.getSize();
			int count = 0;
			if (sizeA == sizeB) {

				for (int i = 0; i < sizeA; i++) {

					if (dbA.getElem(i) == dbB.getElem(i)) {
						count = count + 1;
					}

				}
				percentage = (count * 100) / sizeA;
			} else {
				System.out.println("Comparing images of different size");
			}

		} catch (Exception e) {
			System.out.println("Error comparing images");
		}
		return percentage;
	}

	public boolean isRunning() {
		return isRunning;
	}
}
