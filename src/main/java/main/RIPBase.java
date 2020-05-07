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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
	public ArrayList<Transition> executedTransitions;

	/*
	 * Device information
	 */

	/**
	 * Android version
	 */
	public String version;

	/**
	 * Device resolution
	 */
	public int resolution;

	/**
	 * Device dimensions
	 */
	public String dimensions;

	/**
	 * Device sensors
	 */
	public String sensors;

	/**
	 * Device services
	 */
	public String services;

	/**
	 * Auxiliary writer
	 */
	public FileWriter out;

	/**
	 * Writing Time
	 */
	public int waitingTime;

	/**
	 * Indicates if rip is running
	 */
	public boolean isRunning;
	/**
	 *
	 */
	public String pacName;
	/**
	 * Indicates if the ripping is doing outside the app
	 */
	public boolean rippingOutsideApp;

	/**
	 * location path to the configuration file
	 */
	public String configFilePath;

	public JSONObject params;
	/**
	 * Indicates the execution mode -events- time- complete
	 */
	private String executionMode;

	private int maxIterations = 10000;

	protected int executedIterations = 0;

	private int maxTime = 1000;

	private int elapsedTime = 0;

	private long startTime;

	private long finishTime;

	//If running ITDroid this parameter represents the target language to be test by the i18n rip family
	private String translateTo = null;

	private String expresiveLanguage = null;

	//An extra parameter to make the translation when using ITDroid
	private String extraPath = null;

	public RIPBase(String configFilePath) throws Exception {
		printRIPInitialMessage();
		this.configFilePath = configFilePath;
		params = readConfigFile();
		startTime = System.currentTimeMillis();

		pacName = "";
		isRunning = true;
		statesTable = new Hashtable<>();
		states = new ArrayList<>();
		transitions = new ArrayList<>();
		executedTransitions = new ArrayList<>();

		new File(folderName).mkdirs();

		List<String> emulators = EmulatorHelper.getActiveEmulators();
		//If there is no any emulator running it starts a new one with no data
		if(emulators.isEmpty()){
			//The param is null to start the default emulator: Nexus_6_API_27
			System.out.println("There are no Emulators running right now");
			EmulatorHelper.startEmulatorWipeData(null);
		}

		if(translateTo != null && !translateTo.equals("")){
//			EmulatorHelper.clearData(packageName);
			EmulatorHelper.changeLanguage(translateTo,expresiveLanguage,extraPath);
		}
		System.out.println("test");

		Helper.getInstance(folderName);

		// Captures the Android version of the device
		try {
			version = EmulatorHelper.getAndroidVersion();
		} catch (IOException | RipException e) {
			e.printStackTrace();
		}
		System.out.println(version);

		// Installs the APK in the device
		appInstalled = EmulatorHelper.installAPK(apkLocation);

		if (!appInstalled) {
			throw new RipException("APK could not be installed");
		}

		if (aapt == null) {
			throw new RipException("AAPT_LOCATION was not set");
		}

		// Launches the applications' main activity
		try {
			packageName = EmulatorHelper.getPackageName(aapt, apkLocation);
			mainActivity = EmulatorHelper.getMainActivity(aapt, apkLocation);
			System.out.println(packageName+" "+mainActivity);
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
		initialTransition.setLeavesAppCore(false);
		State initialState = new State(hybridApp, contextualExploration);
		initialState.setId(getSequentialNumber());
		explore(initialState, initialTransition);

		buildFiles();
		//Shutdown emulators
		EmulatorHelper.shutdownEmulators();

		System.out.println("EXPLORATION FINISHED, " + statesTable.size() + " states discovered, " + executedIterations + " events executed, in " + elapsedTime + " minutes");
		if (jsConsoleReader != null) {
			jsConsoleReader.kill();
		}
	}

	private JSONObject readConfigFile() {

		JSONParser jsonParser = new JSONParser();
		JSONObject obj = null;
		try (FileReader reader = new FileReader(configFilePath))
		{
			//Read JSON file
			obj = (JSONObject) jsonParser.parse(reader);

			apkLocation = (String) obj.get("apkPath");
//			packageName = (String) obj.get("packageName");
			folderName = (String) obj.get("outputFolder");
			hybridApp = (Boolean) obj.get("isHybrid");
			executionMode = (String) obj.get("executionMode");
			translateTo = (String) obj.get("translateTo");
			expresiveLanguage = (String) obj.get("expresiveLanguage");
			extraPath = (String) obj.get("extraPath");

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
		return !rawXML.equals(currentState.getRawXML());
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

		//States - Estados
		JSONObject resultStates = new JSONObject();
		for (int i = 0; i < states.size(); i++) {
			State tempState = states.get(i);
			JSONObject state = new JSONObject();
			state.put("id", tempState.getId());
			state.put("activityName", tempState.getActivityName());
			state.put("rawXML", tempState.getRawXML());
			state.put("screenShot", tempState.getScreenShot());
			resultStates.put(tempState.getId()+"", state);
		}
		resultFile.put(STATES, resultStates);
		//Transitions - Transiciones
		JSONObject resultTransitions = new JSONObject();
		for (int i = 0; i < transitions.size(); i++) {
			Transition tempTransition = transitions.get(i);
			JSONObject transition = new JSONObject();

			transition.put("stState", tempTransition.getOrigin().getId());
			transition.put("dsState", tempTransition.getDestination().getId());
			transition.put("tranType", tempTransition.getType().name());
			transition.put("outside", tempTransition.isLeavesAppCore());
			transition.put("screenshot", tempTransition.getScreenshot());
			if (tempTransition.getType()==TransitionType.GUI_INPUT_TEXT){
				transition.put("inputString",tempTransition.getInputString());
			}
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
			resultTransitions.put(i+"",transition);
		}
		resultFile.put(TRANSITIONS, resultTransitions);
		//All Transitions
		JSONObject allTransitions = new JSONObject();
		for (int i = 0; i < executedTransitions.size(); i++) {
			Transition tempTransition = executedTransitions.get(i);
			JSONObject transition = new JSONObject();

			transition.put("stState", tempTransition.getOrigin().getId());
			transition.put("tranType", tempTransition.getType().name());
			transition.put("outside", tempTransition.isLeavesAppCore());
			transition.put("valTrans", tempTransition.getValuableTransNumber());
			allTransitions.put(i+"",transition);
		}
		resultFile.put("allTransitions", allTransitions);
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderName + File.separator + "result.json"));
		writer.write(resultFile.toJSONString());
		writer.close();

		buildTreeJSON();
		buildSequentialJSON();
		buildMetaJSON();
		EmulatorHelper.saveLogcat("",folderName + File.separator + "logcat.txt");
		EmulatorHelper.saveLogcatErrors("", folderName + File.separator + "logcatErrors.txt");
	}

	@SuppressWarnings("unchecked")
	private void buildMetaJSON() throws IOException, RipException {
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

			if(tempState.getActivityName().split("/").length > 1){
				state.put("activityName", tempState.getActivityName().split("/")[1]);
			}else{
				state.put("activityName", tempState.getActivityName().split("/")[0]);
			}
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
		System.out.println(sequentialNumber);
		return ++sequentialNumber;
	}

	public boolean isRippingOutsideApp(Document parsedXML) throws IOException, RipException {
		String currentPackage = parsedXML.getElementsByTagName("node").item(0).getAttributes().getNamedItem("package")
				.getNodeValue();

		//if(currentPackage.equals(packageName) || currentPackage.equals("com.google.android.packageinstaller") || currentPackage.equals("android")){
		if(currentPackage.equals(packageName) || currentPackage.equals("com.google.android.packageinstaller")){
			return false;
		}
		System.out.println("Current package: " + currentPackage);
		System.out.println("packageName: " + packageName);
		System.out.println("Ripping outside");
		System.out.println("Going back");
		EmulatorHelper.goBack();
		return true;
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
		return statesTable.get(pState.getRawXML());
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void tap(AndroidNode node) throws IOException, RipException {
		EmulatorHelper.tap(node.getCentralX() + "", node.getCentralY() + "");
	}

	public int enterInput(Transition transition) throws IOException, RipException {
		int type = EmulatorHelper.checkInputType();
		Random rm = new Random();
		String input = "";
		if (type == 1) {
			input = "" + (char) (rm.nextInt(26) + 'A') + (char) (rm.nextInt(26) + 'a')
					+ (char) (rm.nextInt(26) + 'a');
		} else {
			input = String.valueOf(rm.nextInt(100));
		}
		EmulatorHelper.moveToEndInput();
		EmulatorHelper.enterInput(input);
		EmulatorHelper.goBack();
		return type;
	}


	public void ifKeyboardHideKeyboard(){
		try {
			if(EmulatorHelper.isKeyboardOpen()){
				EmulatorHelper.goBack();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RipException e) {
			e.printStackTrace();
		}
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
			int type = enterInput(transition);
			return (type==1)?TransitionType.GUI_INPUT_TEXT:TransitionType.GUI_INPUT_NUMBER;
		}
		return null;

	}

	public void scroll(AndroidNode origin, boolean isSwipe) {

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
				EmulatorHelper.scroll(tX2, tY2, tX, tY2, "1000");
			} else {
				EmulatorHelper.scroll(tX2, tY2, tX2, tY, "1000");
			}
		} catch (Exception e) {
			System.out.println("CANNOT SCROLL");
		}

	}

	public void explore(State previousState, Transition executedTransition) {
		System.out.println("NEW STATE EXPLORATION STARTED");
		currentState = new State(hybridApp, contextualExploration);
		try {
			//Process the current state to discover whether is an already existing state or a new one
			processState(previousState,executedTransition);
			Transition stateTransition = null;
			boolean stateChanges = false;

			// While no changes in the state are detected
			while (!stateChanges && validExecution()) {
				stateTransition = currentState.popTransition();
				// Waits until the executed transition changes the application current state
				EmulatorHelper.isActionIdle();
				executeTransition(stateTransition);
				ifKeyboardHideKeyboard();
				executedIterations++;
				// Checks if the application changes due to the executed transition
				stateChanges = stateChanges();
				stateTransition.setValuableTransNumber(transitions.size()-1);
				executedTransitions.add(stateTransition);
				if(EmulatorHelper.isHome()){
					throw new RipException("Execution closed the app");
				}
			}
			if(stateChanges && validExecution()) {
				executedTransitions.remove(executedTransitions.size()-1); 
			}
			// If the state changes, recursively explores the application
			if (validExecution()){
			    EmulatorHelper.isActionIdle();
				String tranScreenshot = ImageHelper.takeTransitionScreenshot(stateTransition, transitions.size());
				stateTransition.setScreenshot(tranScreenshot);
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
			if(e.getMessage().contains("Execution closed the app")) {
				System.out.println(e.getMessage());
			} else {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/**
	 * Process the current state to discover whether is an already existing state or a new one
	 * @param previousState
	 * @param executedTransition
	 * @throws IOException
	 * @throws RipException
	 * @throws InterruptedException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws Exception
	 */
	public void processState(State previousState, Transition executedTransition)
			throws IOException, RipException, InterruptedException, ParserConfigurationException, SAXException, Exception {

		ifKeyboardHideKeyboard();
		EmulatorHelper.isActionIdle();
		currentState.setId(getSequentialNumber());
		String rawXML = EmulatorHelper.getCurrentViewHierarchy();
		rawXML = processXML(rawXML);
		Document parsedXML = loadXMLFromString(rawXML);
		String screenShot = EmulatorHelper.takeAndPullScreenshot(currentState.getId()+"", folderName);
		currentState.setRawXML(rawXML);
		currentState.setParsedXML(parsedXML);

		//Conditions for find a new state
		rippingOutsideApp = isRippingOutsideApp(parsedXML);
		executedTransition.setLeavesAppCore(rippingOutsideApp);
		executedTransition.setValuableTransNumber(transitions.size()-1);
		executedTransitions.add(executedTransition);
		State sameState;
		State foundState;
		if (rippingOutsideApp){
			//Application is ripping outside the app
			Helper.deleteFile(screenShot);
			currentState = previousState;
			System.out.println("Ripping outside the app" );
			sequentialNumber--;
		} else if ((foundState = findStateInGraph(currentState))!= null){
			//The state is already in the states' graph
			currentState = foundState;
			Helper.deleteFile(screenShot);
			System.out.println("State Already Exists: Found state in graph");
			sequentialNumber--;
		} else if ((sameState = compareScreenShotWithExisting(screenShot)) != null){
			//State is already explored and it was found by image comparison
			Helper.deleteFile(sameState.getScreenShot());
			File newScreen = new File(screenShot);
			newScreen.renameTo(new File(sameState.getScreenShot()));
			//Change the XML of the state because it could have some change and because of that it was not found in the graph
			sameState.setRawXML(rawXML);
			sameState.setParsedXML(parsedXML);
			NodeList allNodes = sameState.getParsedXML().getElementsByTagName("node");
			List<AndroidNode> androidNodes = sameState.getStateNodes();
			//Change the nodes' state information because of any change i.e a text change
			for (AndroidNode androidNode: androidNodes) {
				for(int i = 0; i < allNodes.getLength(); i++) {
					Node currentNode = allNodes.item(i);
					AndroidNode auxAndroidNode = new AndroidNode(sameState, currentNode);
					if(androidNode.getResourceID().equals(auxAndroidNode.getResourceID())
							&& androidNode.getxPath().equals(auxAndroidNode.getxPath())) {
						androidNode.loadAttributesFromDom(currentNode);
					}
				}
			}
			currentState = sameState;
			System.out.println("State Already Exists: Found state by images");
			sequentialNumber--;
		} else if(EmulatorHelper.isHome()){
				throw new RipException("Execution closed the app");
		}else {
			//New State
			System.out.println("New state found");
			EmulatorHelper.isActionIdle();
			currentState.generatePossibleTransition();
			String activity = EmulatorHelper.getCurrentFocus();
			EmulatorHelper.takeAndPullXMLSnapshot(currentState.getId()+"", folderName);
			System.out.println("Current ST: " + currentState.getId());
			currentState.setActivityName(activity);
			statesTable.put(rawXML, currentState);
			states.add(currentState);
			currentState.setScreenShot(screenShot);
			currentState.retrieveContext(packageName);
			ImageHelper.getNodeImagesFromState(currentState);
		}

		//Add out and in bound transitions to the previous state and the current one respectively
		if (!rippingOutsideApp) {
			if (currentState.hasRemainingTransitions()) {
				previousState.addPossibleTransition(executedTransition);
			}
			executedTransition.setDestination(currentState);
			executedTransition.setOrigin(previousState);
			currentState.addInboundTransition(executedTransition);
			previousState.addOutboundTransition(executedTransition);
			transitions.add(executedTransition);
		}
	}

	public String processXML(String rawXML) {
		return rawXML;
	}

	public boolean validExecution() {
		long currentTime = System.currentTimeMillis();
		elapsedTime = (int)(currentTime-startTime)/60000;
		return (elapsedTime<maxTime && (maxIterations-executedIterations)>0);
	}

	public void printRIPInitialMessage() {
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
