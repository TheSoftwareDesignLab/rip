package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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

	public static Object AMOUNT_TRANSITIONS = "amountTransitions";
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

	public String configFilePath;

	public JSONObject params;

	private String executionMode;

	private int maxIterations = 10000;

	private int executedIterations = 0;

	private int maxTime = 1000;

	private int elapsedTime = 0;

	private long startTime;
	
	private long finishTime;

	public RIPBase(String configFilePath) throws Exception {
		startTime = System.currentTimeMillis();
		printRIPInitialMessage();
		this.configFilePath = configFilePath;
		params = readConfigFile();
		startTime = System.currentTimeMillis();

		pacName = "";
		isRunning = true;
		statesTable = new Hashtable<>();
		states = new ArrayList<>();
		transitions = new ArrayList<>();

		File newFolder = new File(folderName);
		newFolder.mkdirs();

        Helper.getInstance(folderName);

        // Captures the Android version of the device
		try {
			version = EmulatorHelper.getAndroidVersion();
		} catch (IOException | RipException e) {
			e.printStackTrace();
		}
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

		preProcess(params);

		Transition initialTransition = new Transition(null, TransitionType.FIRST_INTERACTION);
		State initialState = new State(hybridApp, contextualExploration);
		initialState.setId(getSequentialNumber());
		explore(initialState, initialTransition);

		buildFiles();
		System.out.println("EXPLORATION FINISHED, " + statesTable.size() + " states discovered, "+executedIterations+" events executed, in "+elapsedTime+" minutes");
		if(jsConsoleReader != null) {
			jsConsoleReader.kill();
		}
	}

	private JSONObject readConfigFile() {

		//		String apkPath, String outputFolder, String isHybrid, String[] preProcArgs
		JSONParser jsonParser = new JSONParser();
		JSONObject obj = null;
		try (FileReader reader = new FileReader(configFilePath))
		{
			//Read JSON file
			obj = (JSONObject) jsonParser.parse(reader);

			apkLocation = (String) obj.get("apkPath");
			folderName = (String) obj.get("outputFolder");
			hybridApp = (Boolean) obj.get("isHybrid");
			executionMode = (String) obj.get("executionMode");

			JSONObject execParams = (JSONObject) obj.get("executionParams");
			switch (executionMode) {
			case "events":
				maxIterations = Math.toIntExact((long) execParams.get("events"));
				break;
			case "time":
				maxTime = Math.toIntExact((long) execParams.get("time"));
				break;
			default:
				break;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return obj;
	}

	/**
	 * This method allows developers to execute a previous processing before starting the ripping process
	 * @param params2 
	 */
	public void preProcess(JSONObject params2) {

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
	public void buildFiles() throws Exception {


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
			transition.put("screenshot", tempTransition.getScreenshot());
			if(tempTransition.getOriginNode()!=null) {
				JSONObject androidNode = new JSONObject();
				androidNode.put("resourceID", tempTransition.getOriginNode().getResourceID());
				androidNode.put("name", tempTransition.getOriginNode().getName());
				androidNode.put("text", tempTransition.getOriginNode().getText());
				androidNode.put("xpath", tempTransition.getOriginNode().getxPath());
				if(tempTransition.getType()==TransitionType.SCROLL || tempTransition.getType()==TransitionType.SWIPE) {
					int[] p1 = tempTransition.getOriginNode().getPoint1();
					int[] p2 = tempTransition.getOriginNode().getPoint2();

					int tapX = p1[0];
					int tapX2 = (int) (p2[0] / 3) * 2;

					int tapY = p1[1];
					int tapY2 = (int) (p2[1] / 3) * 2;

					String tX = String.valueOf(tapX);
					String tX2 = String.valueOf(tapX2);
					String tY = String.valueOf(tapY);
					String tY2 = String.valueOf(tapY2);

					if(tempTransition.getType()==TransitionType.SCROLL) {
						androidNode.put("action", "["+tX2+","+tY2+"]["+tX2+","+tY+"]");
					} else {
						androidNode.put("action", "["+tX2+","+tY2+"]["+tX+","+tY2+"]");
					}
				}
				androidNode.put("bounds", "["+tempTransition.getOriginNode().getPoint1()[0]+","+tempTransition.getOriginNode().getPoint1()[1]+"]["+
						tempTransition.getOriginNode().getPoint2()[0]+","+tempTransition.getOriginNode().getPoint2()[1]+"]");
				transition.put("androidNode", androidNode);
			}			
			resultTransitions.put(""+i,transition);
		}
		resultFile.put(TRANSITIONS, resultTransitions);
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderName + File.separator + "result.json"));
		writer.write(resultFile.toJSONString());
		writer.close();

		buildTreeJSON();
		buildSequentialJSON();
		buildMetaJSON();
	}

	@SuppressWarnings("unchecked")
	private void buildMetaJSON() throws IOException, RipException {
		// TODO Auto-generated method stub
		JSONObject graph = new JSONObject();
		graph.put("executionMethod", executionMode);
		graph.put("maxEvents", maxIterations+"");
		graph.put("execEvents", executedIterations+"");
		graph.put("maxTime", maxTime+"");
		graph.put("elapsedTime", elapsedTime+"");
		graph.put("startingDate", (new Date(startTime))+"");
		graph.put("finishDate", (new Date(System.currentTimeMillis()))+"");
		graph.put("apk", packageName);
		graph.put("androidVersion", EmulatorHelper.getAndroidVersion());
		graph.put("deviceResolution", EmulatorHelper.getDeviceResolution()+"");
		graph.put("currentOrientation", (EmulatorHelper.getCurrentOrientation()==0)?"Portrait":"Lanscape");
		graph.put("deviceDimensions", EmulatorHelper.getScreenDimensions());
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderName + File.separator + "meta.json"));
		writer.write(graph.toJSONString());
		writer.close();
	}

	@SuppressWarnings("unchecked")
	private void buildSequentialJSON() throws IOException {
		JSONObject graph = new JSONObject();
		JSONArray links = new JSONArray();

		JSONArray resultStates = new JSONArray();
		for (int i = 0; i < states.size(); i++) {
			State tempState = states.get(i);
			JSONObject state = new JSONObject();
			state.put("name", tempState.getActivityName());
			state.put("image", (new File(tempState.getScreenShot())).getName());
			state.put("battery", tempState.getBattery());
			state.put("wifi", tempState.isWifiStatus());
			state.put("memory", tempState.getMemory());
			state.put("cpu", tempState.getCpu());
			state.put("airplane", tempState.isAirplane());
			resultStates.add(state);
		}
		graph.put("nodes", resultStates);

		JSONArray resultTransitions = new JSONArray();
		for (int i = 0; i < transitions.size(); i++) {
			Transition tempTransition = transitions.get(i);
			JSONObject transition = new JSONObject();

			transition.put("id", i);
			String fileName = tempTransition.getScreenshot()==null?tempTransition.getDestination().getScreenShot():tempTransition.getScreenshot();
			transition.put("image", (new File(fileName)).getName());	
			resultTransitions.add(transition);
		}
		graph.put("links", resultTransitions);
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderName + File.separator + "sequential.json"));
		writer.write(graph.toJSONString());
		writer.close();
	}

	@SuppressWarnings("unchecked")
	private void buildTreeJSON() throws Exception {

		JSONObject graph = new JSONObject();

		JSONArray resultStates = new JSONArray();
		for (int i = 0; i < states.size(); i++) {
			State tempState = states.get(i);
			JSONObject state = new JSONObject();
			state.put("id", tempState.getId());
			state.put("name", "("+tempState.getId()+") "+tempState.getActivityName());
			state.put("activityName", tempState.getActivityName().split("/")[1]);
			state.put("imageName", (new File(tempState.getScreenShot())).getName());
			state.put("battery", tempState.getBattery());
			state.put("wifi", tempState.isWifiStatus());
			state.put("memory", tempState.getMemory());
			state.put("cpu", tempState.getCpu());
			state.put("airplane", tempState.isAirplane());
			state.put("model", tempState.getDomainModel());
			resultStates.add(state);
		}
		JSONObject state = new JSONObject();
		state.put("id", states.size());
		state.put("name", "("+(states.size()-1)+") End of execution");
		state.put("activityName", "End of execution");
		state.put("imageName", "N/A");
		state.put("battery", "N/A");
		state.put("wifi", "N/A");
		state.put("memory", "N/A");
		state.put("cpu", "N/A");
		state.put("airplane", "N/A");
		resultStates.add(state);
		graph.put("nodes", resultStates);

		JSONArray resultTransitions = new JSONArray();
		for (int i = 0; i < transitions.size(); i++) {
			Transition tempTransition = transitions.get(i);
			JSONObject transition = new JSONObject();

			transition.put("source", tempTransition.getOrigin().getId()-1);
			transition.put("target", tempTransition.getDestination().getId()-1);
			transition.put("id", i);
			transition.put("tranType", tempTransition.getType().name());
			String fileName = tempTransition.getScreenshot()==null?tempTransition.getDestination().getScreenShot():tempTransition.getScreenshot();
			transition.put("imageName", (new File(fileName)).getName());	
			resultTransitions.add(transition);
		}
		Transition tempTransition = transitions.get(transitions.size()-1);
		Transition finalTrans = new Transition(tempTransition.getDestination(), TransitionType.FINISH_EXECUTION);
		JSONObject transition = new JSONObject();
		transition.put("source", tempTransition.getDestination().getId()-1);
		transition.put("target", states.size());
		transition.put("id", transitions.size());
		transition.put("tranType", TransitionType.FINISH_EXECUTION.name());
		transition.put("imageName", ImageHelper.takeTransitionScreenshot(finalTrans, transitions.size()));
		resultTransitions.add(transition);
		graph.put("links", resultTransitions);
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderName + File.separator + "tree.json"));
		writer.write(graph.toJSONString());
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
	public int enterInput(AndroidNode node) throws IOException, RipException {
		int type = EmulatorHelper.checkInputType();
		Random rm = new Random();

		if (type == 1) {
			String input = "" + (char) (rm.nextInt(26) + 'A') + (char) (rm.nextInt(26) + 'a')
					+ (char) (rm.nextInt(26) + 'a');
			EmulatorHelper.enterInput(input);
		} else {
			String numInput = String.valueOf(rm.nextInt(100));
			EmulatorHelper.enterInput(numInput);
		}
		EmulatorHelper.goBack();
		return type;
	}

	public TransitionType executeTransition(Transition transition) throws IOException, RipException {
		AndroidNode origin;
		switch (transition.getType()) {
		case GUI_CLICK_BUTTON:
			origin = transition.getOriginNode();
			tap(origin);
			return TransitionType.GUI_CLICK_BUTTON;
		case SCROLL:
			origin = transition.getOriginNode();
			scroll(origin, false);
			return TransitionType.SCROLL;
		case SWIPE:
			origin = transition.getOriginNode();
			scroll(origin, true);
			return TransitionType.SWIPE;
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
			return TransitionType.BUTTON_BACK;
		case GUI_INPUT_TEXT:
			AndroidNode originInput = transition.getOriginNode();
			tap(originInput);
			int type = enterInput(originInput);
			return (type==1)?TransitionType.GUI_INPUT_TEXT:TransitionType.GUI_INPUT_NUMBER;

		}
		return null;

	}

	private void scroll(AndroidNode origin, boolean isSwipe) {

		int[] p1 = origin.getPoint1();
		int[] p2 = origin.getPoint2();

		int tapX = p1[0];
		int tapX2 = (int) (p2[0] / 3) * 2;

		int tapY = p1[1];
		int tapY2 = (int) (p2[1] / 3) * 2;

		String tX = String.valueOf(tapX);
		String tX2 = String.valueOf(tapX2);
		String tY = String.valueOf(tapY);
		String tY2 = String.valueOf(tapY2);

		try {
			// Is vertical swipe
			if (!isSwipe) {
				EmulatorHelper.scroll(tX2, tY2, tX2, tY, "1000");
			} else {
				EmulatorHelper.scroll(tX2, tY2, tX, tY2, "1000");
			}
		} catch (Exception e) {
			System.out.println("CANNOT SCROLL");
		}

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
				System.out.println("State Already Exists");

			} else {
				// New state discovered
				currentState.setId(getSequentialNumber());
				String screenShot = EmulatorHelper.takeAndPullScreenshot(""+currentState.getId(), folderName);
				String snapShot = EmulatorHelper.takeAndPullXMLSnapshot(currentState.getId()+"", folderName);
				System.out.println("Current ST: " + currentState.getId());
				State sameState = compareScreenShotWithExisting(screenShot);
				rippingOutsideApp = isRippingOutsideApp(parsedXML);
				if (sameState == null && !rippingOutsideApp) {
					statesTable.put(rawXML, currentState);
					states.add(currentState);
					currentState.setScreenShot(screenShot);
					currentState.retrieveContext(packageName);
					ImageHelper.getNodeImagesFromState(currentState);
				} else {
					// Discard state
					sequentialNumber--;
					if(sameState != null) {
						Helper.deleteFile(sameState.getScreenShot());
						File newScreen = new File(screenShot);
						newScreen.renameTo(new File(sameState.getScreenShot()));
					} else {
						Helper.deleteFile(screenShot);						
					}
					Helper.deleteFile(snapShot);
					currentState = sameState;
					if(EmulatorHelper.isHome()) {
						throw new RipException("Execution closed the app");
					}
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
			while (!stateChanges && validExecution()) {
				stateTransition = currentState.popTransition();
				executeTransition(stateTransition);
				executedIterations++;
				// Waits until the executed transition changes the application current state
				EmulatorHelper.isEventIdle();
				// Checks if the application changes due to the executed transition
				stateChanges = stateChanges();
			}

			// If the state changes, recursively explores the application
			if (stateChanges && validExecution()) {
				String tranScreenshot = ImageHelper.takeTransitionScreenshot(stateTransition, transitions.size());
				stateTransition.setScreenshot(tranScreenshot);
				executedIterations++;
				explore(currentState, stateTransition);
			}

		} catch (NoSuchElementException e) {
			// There are no more possible transitions in the current state
		} catch (ParserConfigurationException | SAXException e) {
			// Error parsing the XML DOM
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// Error getting the current view hierarchy
			e.printStackTrace();
		} catch (RipException e) {
			if(e.getMessage().equals("Execution closed the app")) {
				System.out.println(e.getMessage());
			} else {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public boolean validExecution() {
		long currentTime = System.currentTimeMillis();
		elapsedTime = (int)(currentTime-startTime)/60000;
		return (elapsedTime<maxTime && (maxIterations-executedIterations)>0);
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
		if(args.length<1) {
			System.err.println("Please provide config file location");
		} else {
			try {
				new RIPBase(args[0]);
			} catch (RipException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
                Helper.getInstance("./").closeStream();

            }
		}
	}
}
