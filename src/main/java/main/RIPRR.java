package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import helper.EmulatorHelper;
import helper.Helper;
import helper.ImageHelper;
import model.AndroidNode;
import model.State;
import model.Transition;
import model.TransitionType;

public class RIPRR extends RIPBase {

	public String scriptPath;

	public Hashtable<String, State> oldStatesTable;

	public ArrayList<State> oldStates;

	public ArrayList<Transition> oldTransitions;
	
	public ArrayList<Transition> allOldTransitions;

	AndroidNode transToBeExecAN;


	public RIPRR(String configFilePath) throws Exception {
		super(configFilePath);
	}

	@Override
	public void preProcess(JSONObject preProcArgs) {

		scriptPath = (String) preProcArgs.get("scriptPath");
		oldStatesTable = new Hashtable<>();
		oldStates = new ArrayList<>();
		oldTransitions = new ArrayList<>();
		allOldTransitions = new ArrayList<>();
		//JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();

		try (FileReader reader = new FileReader(scriptPath)) {

			//Read JSON file
			JSONObject obj = (JSONObject) jsonParser.parse(reader);

			int amountStates = Math.toIntExact((long) obj.get(AMOUNT_STATES));
			int amountTransitions = Math.toIntExact((long) obj.get(AMOUNT_TRANSITIONS));
			JSONObject states = (JSONObject) obj.get(STATES);

			for (int i = 0; i < amountStates; i++) {
				State tempState = new State(hybridApp, contextualExploration);
				JSONObject currentState = (JSONObject) states.get((i + 1) + "");
				tempState.setRawXML((String) currentState.get("rawXML"));
				tempState.setActivityName((String) currentState.get("activityName"));
				tempState.setId(Math.toIntExact((long) currentState.get("id")));
				Document parsedXML = loadXMLFromString(tempState.getRawXML());
				tempState.setParsedXML(parsedXML);
				tempState.generatePossibleTransition();
				oldStates.add(tempState);
				oldStatesTable.put(tempState.getRawXML(), tempState);
			}
			
			JSONObject transitions = (JSONObject) obj.get(TRANSITIONS);

			for (int i = 1; i < amountTransitions; i++) {
				JSONObject currentTransition = (JSONObject) transitions.get(i + "");
				int originState = Math.toIntExact((long) currentTransition.get("stState"));
				TransitionType tType = TransitionType.valueOf((String) currentTransition.get("tranType"));
				int destState = Math.toIntExact((long) currentTransition.get("dsState"));
				Transition tempTransition = new Transition(oldStates.get(originState - 1), tType);
				tempTransition.setDestination(oldStates.get(destState - 1));

				if(tempTransition.getOriginElement() != null && tempTransition.getType().equals(TransitionType.GUI_INPUT_TEXT)){
					//Get the android node
					String inputString = (String) currentTransition.get("inputString");
					tempTransition.setInputString(inputString);
					System.out.println("INPUT STRING: " + inputString);
				}

				if (currentTransition.containsKey("androidNode")) {
					JSONObject androidNode = (JSONObject) currentTransition.get("androidNode");
					String resourceID = (String) androidNode.get("resourceID");
					String xpath = (String) androidNode.get("xpath");
					String text = (String) androidNode.get("text");
					tempTransition.setOriginElement(oldStates.get(originState - 1).getAndroidNode(resourceID, xpath, text));
				}

				oldTransitions.add(tempTransition);
			}
			JSONObject allTransitions = (JSONObject) obj.get("allTransitions");

			for (int i = 1; i < allTransitions.keySet().size(); i++) {
				JSONObject currentTransition = (JSONObject) allTransitions.get(i + "");
				int originState = Math.toIntExact((long) currentTransition.get("stState"));
				TransitionType tType = TransitionType.valueOf((String) currentTransition.get("tranType"));
				int valTrans = Math.toIntExact((long) currentTransition.get("valTrans"));
				boolean outside = (boolean) currentTransition.get("outside");
				Transition tempTransition = new Transition(oldStates.get(originState - 1), tType);
				tempTransition.setLeavesAppCore(outside);
				tempTransition.setValuableTransNumber(valTrans);
				allOldTransitions.add(tempTransition);
			}
			for (int i = 0; i < oldTransitions.size(); i++) {
				System.out.println(oldTransitions.get(i).getOrigin().getId()+" - "+oldTransitions.get(i).getDestination().getId()+" - "+oldTransitions.get(i).getType().name());
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void explore(State previousState, Transition executedTransition){
		System.out.println("NEW STATE EXPLORATION STARTED");
		currentState = new State(hybridApp,contextualExploration);
		try{
			//Process the current state to discover whether is an existing state or a new one
			processState(previousState,executedTransition);
			//End execution if the old transitions are already done
			if(oldTransitions.size()==0) {
				System.out.println("OLD TRANSITIONS EMPTY");
				throw new RipException("OLD TRANSITIONS EMPTY");
			}

			//Get the next old transition to be executed and the node where it is expected to be done
			Transition transToBeExec = oldTransitions.get(0);
			transToBeExecAN = transToBeExec.getOriginNode();

			System.out.println(currentState.getId());
			//Print all the remaining transitions
			for (int i = 0; i < oldTransitions.size(); i++) {
				System.out.println((i+1)+": "+oldTransitions.get(i).getOrigin().getId()+" - "+oldTransitions.get(i).getDestination().getId()+" - "+oldTransitions.get(i).getType().name());
			}

			//Ending execution due to current node has a different id to the next transition id expected to be executed
			if(transToBeExec.getOrigin().getId()!=currentState.getId()) {
				System.out.println("EXITING EXECUTION. START STATE != CURRENT STATE");
				System.out.println(transToBeExec.getOrigin().getId()+" - "+currentState.getId());
				throw new RipException("EXITING EXECUTION. START STATE != CURRENT STATE");
			}
			////Take the next transition in the current state
			//Check 1. same type between the transition and the expected transition
			//	2. same android id source 3. same android node xPath
			Transition tempTrans = currentState.popTransition();
			AndroidNode tempTransAN = tempTrans.getOriginNode();
			Transition teempTransition = allOldTransitions.get(0);
			boolean sameType = tempTrans.getType().equals(transToBeExec.getType());
			boolean sameID = tempTransAN.getResourceID().equals(transToBeExecAN.getResourceID());
			boolean sameXpath = tempTransAN.getxPath().equals(transToBeExecAN.getxPath());



			if(tempTrans.getType() != TransitionType.BUTTON_BACK && transToBeExec.getType() != TransitionType.BUTTON_BACK){
				//TODO it is possible there is a bug here. The tempTrans could not be of type BUTTON_BACK at the first iteration but could not be the case in the next iterations
				while( !sameType
						|| !sameID
						|| !sameXpath) {
					//If one or more of those conditions are false get the next possible transition in the current state
					if(!teempTransition.isLeavesAppCore()) {
						executeTransition(tempTrans);
					}
					allOldTransitions.remove(0);
					teempTransition = allOldTransitions.get(0);
					ifKeyboardHideKeyboard();
					EmulatorHelper.isEventIdle();
					tempTrans = currentState.popTransition();
					tempTransAN = tempTrans.getOriginNode();
					sameType = tempTrans.getType().equals(transToBeExec.getType());
					sameID = tempTransAN.getResourceID().equals(transToBeExecAN.getResourceID());
					sameXpath = tempTransAN.getxPath().equals(transToBeExecAN.getxPath());
				}
			}else{
				//TODO it is possible there is a bug here. The tempTrans could be of the type BUTTON_BACK at the first iteration but could not be the case in the next iterations
				while(!tempTrans.getType().equals(transToBeExec.getType())) {
					//If the condition is false get the next possible transition in the current state
					if(!teempTransition.isLeavesAppCore()) {
						executeTransition(tempTrans);
					}
					allOldTransitions.remove(0);
					teempTransition = allOldTransitions.get(0);
					ifKeyboardHideKeyboard();
					EmulatorHelper.isEventIdle();
					tempTrans = currentState.popTransition();
					tempTransAN = tempTrans.getOriginNode();
				}
			}
			if (tempTrans.getType() == TransitionType.GUI_INPUT_TEXT) {
				tempTrans.setInputString(transToBeExec.getInputString());
			}
			executeTransition(tempTrans);
			allOldTransitions.remove(0);
			teempTransition = allOldTransitions.get(0);
			ifKeyboardHideKeyboard();

			//Remove the transition
			oldTransitions.remove(0);
			//Add one to the iteration counter
			executedIterations++;
			// Waits until the executed transition changes the application current state
			ifKeyboardHideKeyboard();
			EmulatorHelper.isEventIdle();
			String tranScreenshot = ImageHelper.takeTransitionScreenshot(tempTrans, transitions.size());
			tempTrans.setScreenshot(tranScreenshot);
			//explore recursively
			explore(currentState, tempTrans);
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
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	@Override
	public void printRIPInitialMessage() {
		System.out.println("\n 2018, Universidad de los Andes\n The Software Design Lab\n");
		System.out.println("https://thesoftwaredesignlab.github.io/\n");
		String s = String.join("\n",
				"🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥   🔥🔥🔥🔥🔥🔥   🔥🔥🔥🔥🔥🔥"
				, "🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥  🔥🔥     🔥🔥  🔥🔥     🔥🔥",
				"🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥  🔥🔥     🔥🔥  🔥🔥     🔥🔥  "
				, "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥   🔥🔥🔥🔥🔥🔥   🔥🔥🔥🔥🔥🔥   ",
				"🔥🔥   🔥🔥    🔥🔥  🔥🔥           🔥🔥   🔥🔥    🔥🔥   🔥🔥         "
				, "🔥🔥    🔥🔥   🔥🔥  🔥🔥           🔥🔥    🔥🔥   🔥🔥    🔥🔥   "
				,"🔥🔥     🔥🔥  🔥🔥  🔥🔥           🔥🔥     🔥🔥  🔥🔥     🔥🔥  ", " ");
		System.out.println(s);
	}

	public static void main(String[] args) {
		if(args.length<1) {
			System.err.println("Please provide config file location");
		} else {
			try {
				//Always shutdown the emulators to allow a clean start. RIPBase will start a new emulator with wiped data
				EmulatorHelper.shutdownEmulators();
				new RIPRR(args[0]);
			} catch (RipException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


}
