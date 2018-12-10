package main;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import helper.ExternalProcess2;
import me.tongfei.progressbar.ProgressBar;
import model.AndroidNode;
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
	private boolean hybridApp = false;

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
	private boolean appInstalled = false;

	/**
	 * Folder where the results will be exported
	 */
	private String folderName;

	/**
	 * Location of the APK file
	 */
	private String apkLocation = "androidApps/car.apk";

	/**
	 * Package name of the application
	 */
	private String packageName;

	/**
	 * Main activity of the application
	 */
	private String mainActivity;

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

	public Rip2() throws RipException {

		Date nowFolder = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("hhmmss");
		String time = dateFormat.format(nowFolder);
		folderName = "./generated_" + time;

		// Captures the Android version of the device
		try {
			version = ExternalProcess2.getAndroidVersion();
		} catch (IOException | RipException e) {
			e.printStackTrace();
		}

		// Installs the APK in the device
		appInstalled = installAPK(apkLocation);

		if (!appInstalled) {
			throw new RipException("APK could not be installed");
		}

		// Launches the applications' main activity
		try {
			packageName = ExternalProcess2.getPackageName(aapt, apkLocation);
			mainActivity = ExternalProcess2.getMainActivity(aapt, apkLocation);
			ExternalProcess2.startActivity(packageName, mainActivity);
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

		// Explores the application
		explore();

		// try {
		// ExternalProcess2.readWebViewConsole();
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }

	}

	private void explore() {
		Deque<Integer> stack = new ArrayDeque<Integer>();
		State currentState = new State(hybridApp, contextualExploration);
		try {
			String rawXML = ExternalProcess2.getCurrentViewHierarchy();
			Document parsedXML;
			try {
				parsedXML = loadXMLFromString(rawXML);

				// TODO Evaluate if the state exists in the graph

				currentState.setParsedXML(parsedXML);
				currentState.setRawXML(rawXML);

				boolean stateChanges = false;

				// While no changes in in the state are detected
				while (stateChanges == false) {
					Transition stateTransition = currentState.popTransition();
					executeTransition(stateTransition);
					stateChanges = stateChanges();
				}

			} catch (NoSuchElementException e) {
				// There are no more possible transitions in the current state
			} catch (CrawlingOutsideAppException e) {
				// The new state is out of the domain of the current application
			} catch (ParserConfigurationException | SAXException e) {
				// Error parsing the XML Dom
				e.printStackTrace();
			}
		} catch (IOException | RipException e) {
			// Error getting the current view hierarchy
			e.printStackTrace();
		}
	}

	public boolean stateChanges() throws CrawlingOutsideAppException {
		return false;
	}

	public void executeTransition(Transition transition) throws IOException, RipException {
		switch (transition.getType()) {
		case GUI_CLICK_BUTTON:
			AndroidNode origin = transition.getOriginNode();
			tap(origin);
			break;
		case CONTEXT_INTERNET_OFF:
			turnInternet(false);
			break;
		case CONTEXT_INTERNET_ON:
			turnInternet(true);
			break;
		case ROTATE_LANDSCAPE:
			rotateDevice(true);
			break;
		case ROTATE_PORTRAIT:
			rotateDevice(false);
			break;
		case CONTEXT_LOCATION_OFF:
			turnLocation(false);
			break;
		case CONTEXT_LOCATION_ON:
			turnLocation(true);
			break;
		}
		
	}

	/**
	 * Changes the status of the internet. 
	 * @param value true ON, false OFF
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
	 * @param value true landscape, false portrait
	 * @throws RipException 
	 * @throws IOException 
	 */
	public void rotateDevice(boolean value) throws IOException, RipException {
		if(value) {
			ExternalProcess2.rotateLandscape();
		}
		else {
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

	private boolean installAPK(String pathAPK) {

		try {
			ExternalProcess2.installAPK(pathAPK);
			logMessage("INSTALL APP", new String[] { pathAPK }, null);
			return true;

		} catch (Exception e) {
			logMessage("INSTALL APP", new String[] { pathAPK }, e.getMessage());
			return false;
		}
	}

	public static void main(String[] args) {
		System.out.println("\n 2018, Universidad de los Andes\n The Software Design Lab\n");
		System.out.println("https://thesoftwaredesignlab.github.io/\n");
		String s = String.join("\n", "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥", "🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥",
				"🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥", "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥 ",
				"🔥🔥   🔥🔥    🔥🔥  🔥🔥          ", "🔥🔥    🔥🔥   🔥🔥  🔥🔥          ",
				"🔥🔥     🔥🔥  🔥🔥  🔥🔥          ", " ");

		System.out.println(s);
		try {
			new Rip2();
		} catch (RipException e) {
			e.printStackTrace();
		}
	}
}
