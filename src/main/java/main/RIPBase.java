package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import helper.EmulatorHelper;
import helper.Helper;
import helper.ImageHelper;
import hybridResources.JSConsoleReader;
import me.tongfei.progressbar.ProgressBar;
import model.AndroidNode;
import model.State;
import model.Transition;
import model.TransitionType;

public class RIPBase {
	
	public static final Object AMOUNT_TRANSITIONS = "amountTransitions";
	public static String STATES = "states";
	public static String TRANSITIONS = "transitions";
	public static String AMOUNT_STATES = "amountStates";

	/*
	 * Environment variables
	 */
	public String aapt = System.getenv("AAPT_LOCATION");

	/*
	 * Execution settings
	 */

	/**
	 * Indicates if the application is hybrid
	 */
	public boolean hybridApp;

	/**
	 * Contextual exploration strategy in the device
	 */
	public boolean contextualExploration = false;

	/*
	 * Execution variables
	 */
	/**
	 * The application was installed
	 */
	public boolean appInstalled;

	/**
	 * Folder where the results will be exported
	 */
	public String folderName;

	/**
	 * Location of the APK file
	 */
	public String apkLocation;

	/**
	 * Package name of the application
	 */
	public String packageName;

	public int sequentialNumber=-1;

	/**
	 * Main activity of the application
	 */
	public String mainActivity;

	public State currentState;

	public Hashtable<String, State> statesTable;

	public ArrayList<State> states;

	public ArrayList<Transition> transitions;

	/*
	 * Device information
	 */

	/**
	 * Android version
	 */
	public String version;

	public int resolution;

	public String dimensions;

	public String sensors;

	public String services;

	public FileWriter out;

	public int waitingTime;

	public boolean isRunning;

	public String pacName;

	public boolean rippingOutsideApp;

	public RIPBase(String apkPath, String outputFolder, String isHybrid, String[] preProcArgs) throws RipException, IOException {
		printRIPInitialMessage();
		hybridApp = Boolean.parseBoolean(isHybrid);
		pacName = "";
		isRunning = true;
		statesTable = new Hashtable<>();
		states = new ArrayList<>();
		transitions = new ArrayList<>();
		waitingTime = 500;

		folderName = outputFolder;
		File newFolder = new File(folderName);
		newFolder.mkdirs();

		// Captures the Android version of the device
		try {
			version = EmulatorHelper.getAndroidVersion();
		} catch (IOException | RipException e) {
			e.printStackTrace();
		}
		apkLocation = apkPath;
		// Installs the APK in the device
		appInstalled = EmulatorHelper.installAPK(apkLocation);

		if (!appInstalled) {
			throw new RipException("APK could not be installed");
		}

		if(aapt==null) {
			throw new RipException("AAPT_LOCATION was not set");
		}

		// Launches the applications' main activity
		try {
			packageName = EmulatorHelper.getPackageName(aapt, apkLocation);
			mainActivity = EmulatorHelper.getMainActivity(aapt, apkLocation);
			EmulatorHelper.startActivity(packageName, mainActivity);
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

		preProcess(preProcArgs);

		Transition initialTransition = new Transition(null, TransitionType.FIRST_INTERACTION);
		State initialState = new State(hybridApp, contextualExploration);
		initialState.setId(getSequentialNumber());
		explore(initialState, initialTransition);

		buildFiles();
		System.out.println("EXPLORATION FINISHED, " + statesTable.size() + " states discovered");
		if(jsConsoleReader != null) {
			jsConsoleReader.kill();
		}
	}

	/**
	 * This method allows developers to execute a previous processing before starting the ripping process
	 * @param preProcArgs 
	 */
	public void preProcess(String[] preProcArgs) {

		//Insert specific implementation

	}

	public boolean stateChanges() throws IOException, RipException {
		String rawXML = EmulatorHelper.getCurrentViewHierarchy();
		if (rawXML.equals(currentState.getRawXML())) {
			return false;
		}
		return true;
	}

	public static Document loadXMLFromString(String xml)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}

	@SuppressWarnings("unchecked")
	public void buildFiles() throws IOException {

		JSONObject resultFile = new JSONObject();
		resultFile.put(AMOUNT_STATES,statesTable.size());
		resultFile.put(AMOUNT_TRANSITIONS, transitions.size());

		JSONObject resultStates = new JSONObject();
		for (int i = 0; i < states.size(); i++) {
			State tempState = states.get(i);
			JSONObject state = new JSONObject();
			state.put("id", tempState.getId());
			state.put("activityName", tempState.getActivityName());
			state.put("rawXML", tempState.getRawXML());
			state.put("screenShot", tempState.getScreenShot());
			resultStates.put(""+tempState.getId(), state);
		}
		resultFile.put(STATES, resultStates);

		JSONObject resultTransitions = new JSONObject();
		for (int i = 0; i < transitions.size(); i++) {
			Transition tempTransition = transitions.get(i);
			JSONObject transition = new JSONObject();

			transition.put("stState", tempTransition.getOrigin().getId());
			transition.put("dsState", tempTransition.getDestination().getId());
			transition.put("tranType", tempTransition.getType().name());
			if(tempTransition.getOriginNode()!=null) {
				JSONObject androidNode = new JSONObject();
				androidNode.put("resourceID", tempTransition.getOriginNode().getResourceID());
				androidNode.put("name", tempTransition.getOriginNode().getName());
				androidNode.put("text", tempTransition.getOriginNode().getText());
				androidNode.put("xpath", tempTransition.getOriginNode().getxPath());
				transition.put("androidNode", androidNode);
			}			
			resultTransitions.put(""+i,transition);
		}
		resultFile.put(TRANSITIONS, resultTransitions);
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderName + File.separator + "result.json"));
		writer.write(resultFile.toJSONString());
		writer.close();
	}

	public int getSequentialNumber() {
		sequentialNumber++;
		return sequentialNumber;
	}

	public boolean isRippingOutsideApp(Document parsedXML) throws IOException, RipException {
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
			EmulatorHelper.goBack();
			return true;
		}
		return false;
	}

	public State compareScreenShotWithExisting(String screenShot) {
		File existing = new File(screenShot);
		for (int i = states.size()-1; i>=0; i--) {
			State state = states.get(i);
			double percentage = ImageHelper.compareImage(new File(state.getScreenShot()), existing);
			System.out.println(percentage + " " + state.getId());
			if (percentage >= 97.5) {
				System.out.println("Same!");
				return state;
			}
		}
		return null;
	}

	/**
	 * Explores the existing states to determine if pState exists
	 * 
	 * @param pState
	 */
	public State findStateInGraph(State pState) {
		State found = statesTable.get(pState.getRawXML());
		return found;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void tap(AndroidNode node) throws IOException, RipException {
		EmulatorHelper.tap(node.getCentralX() + "", node.getCentralY() + "");
	}

	public TransitionType executeTransition(Transition transition) throws IOException, RipException {
		switch (transition.getType()) {
		case GUI_CLICK_BUTTON:
			AndroidNode origin = transition.getOriginNode();
			tap(origin);
			return TransitionType.GUI_CLICK_BUTTON;
		case CONTEXT_INTERNET_OFF:
			EmulatorHelper.turnInternet(false);
			return TransitionType.CONTEXT_INTERNET_OFF;
		case CONTEXT_INTERNET_ON:
			EmulatorHelper.turnInternet(true);
			return TransitionType.CONTEXT_INTERNET_ON;
		case ROTATE_LANDSCAPE:
			EmulatorHelper.rotateLandscape();
			return TransitionType.ROTATE_LANDSCAPE;
		case ROTATE_PORTRAIT:
			EmulatorHelper.rotatePortrait();
			return TransitionType.ROTATE_PORTRAIT;
		case CONTEXT_LOCATION_OFF:
			EmulatorHelper.turnLocationServices(false);
			return TransitionType.CONTEXT_LOCATION_OFF;
		case CONTEXT_LOCATION_ON:
			EmulatorHelper.turnLocationServices(true);
			return TransitionType.CONTEXT_LOCATION_ON;
		case BUTTON_BACK:
			EmulatorHelper.goBack();
		}
		return null;

	}

	public void explore(State previousState, Transition executedTransition) {
		currentState = new State(hybridApp, contextualExploration);
		try {
			String rawXML = EmulatorHelper.getCurrentViewHierarchy();
			Document parsedXML;
			parsedXML = loadXMLFromString(rawXML);
			String activity = EmulatorHelper.getCurrentFocus();
			currentState.setActivityName(activity);
			currentState.setParsedXML(parsedXML);
			
			currentState.setRawXML(rawXML);

			State foundState = findStateInGraph(currentState);
			if (foundState != null) {
				// State already exists
				currentState = foundState;

			} else {
				// New state discovered
				currentState.setId(getSequentialNumber());
				String screenShot = EmulatorHelper.takeAndPullScreenshot(""+currentState.getId(), folderName);
				System.out.println("Current ST: " + currentState.getId());
				State sameState = compareScreenShotWithExisting(screenShot);
				rippingOutsideApp = isRippingOutsideApp(parsedXML);
				if (sameState == null && !rippingOutsideApp) {
					statesTable.put(rawXML, currentState);
					states.add(currentState);
					currentState.setScreenShot(screenShot);
				} else {
					// Discard state
					sequentialNumber--;
					Helper.deleteFile(screenShot);
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
			boolean stateChanges = false;

			// While no changes in in the state are detected
			while (!stateChanges) {
				stateTransition = currentState.popTransition();
				executeTransition(stateTransition);
				// Waits until the executed transition changes the application current state
				Thread.sleep(waitingTime);
				// Checks if the application changes due to the executed transition
				stateChanges = stateChanges();
			}

			// If the state changes, recursively explores the application
			if (stateChanges) {
				explore(currentState, stateTransition);
			}

		} catch (NoSuchElementException e) {
			// There are no more possible transitions in the current state
		} catch (ParserConfigurationException | SAXException e) {
			// Error parsing the XML DOM
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException | RipException e) {
			// Error getting the current view hierarchy
			e.printStackTrace();
		} 
	}

	public static void printRIPInitialMessage() {
		System.out.println("\n 2018, Universidad de los Andes\n The Software Design Lab\n");
		System.out.println("https://thesoftwaredesignlab.github.io/\n");
		String s = String.join("\n", "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥", "🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥",
				"🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥", "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥 ",
				"🔥🔥   🔥🔥    🔥🔥  🔥🔥          ", "🔥🔥    🔥🔥   🔥🔥  🔥🔥          ",
				"🔥🔥     🔥🔥  🔥🔥  🔥🔥          ", " ");
		System.out.println(s);
	}

	public static void main(String[] args) {
		if(args.length<3) {
			System.err.println("Some arguments are missing, please provide apk location and outputfolder");
		} else {
			try {
				new RIPBase(args[0], args[1], args[2], args);
			} catch (RipException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
