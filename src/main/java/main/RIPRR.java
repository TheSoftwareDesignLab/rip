package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.NoSuchElementException;

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

		//JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();

		try (FileReader reader = new FileReader(scriptPath)) {

			//Read JSON file
			JSONObject obj = (JSONObject) jsonParser.parse(reader);

			int amountStates = Math.toIntExact((long) obj.get(AMOUNT_STATES));
			int amountTransitions = Math.toIntExact((long) obj.get(AMOUNT_TRANSITIONS));
			System.out.println("AMOUNT STATES: " +amountStates);
			System.out.println("AMOUNT TRANSITIONS: " +amountTransitions);
			JSONObject states = (JSONObject) obj.get(STATES);

			for (int i = 0; i < amountStates; i++) {
				State tempState = new State(hybridApp, contextualExploration);
				JSONObject currentState = (JSONObject) states.get((i + 1) + "");
				tempState.setRawXML((String) currentState.get("rawXML"));
				tempState.setActivityName((String) currentState.get("activityName"));
				tempState.setId(Math.toIntExact((long) currentState.get("id")));
				Document parsedXML = loadXMLFromString(tempState.getRawXML());
				tempState.setParsedXML(parsedXML);
				oldStates.add(tempState);
				oldStatesTable.put(tempState.getRawXML(), tempState);
			}
			System.out.println("AMOUNT STATES OLDSTATES: " +oldStates.size());
			System.out.println("AMOUNT STATES OLDSTATESTABLE: " +oldStatesTable.size());

			JSONObject transitions = (JSONObject) obj.get(TRANSITIONS);

			for (int i = 1; i < amountTransitions; i++) {
				JSONObject currentTransition = (JSONObject) transitions.get(i + "");
				int originState = Math.toIntExact((long) currentTransition.get("stState"));
				TransitionType tType = TransitionType.valueOf((String) currentTransition.get("tranType"));
				int destState = Math.toIntExact((long) currentTransition.get("dsState"));
				Transition tempTransition = new Transition(oldStates.get(originState - 1), tType);
				tempTransition.setDestination(oldStates.get(destState - 1));
				if (currentTransition.containsKey("androidNode")) {
					JSONObject androidNode = (JSONObject) currentTransition.get("androidNode");
					String resourceID = (String) androidNode.get("resourceID");
					String xpath = (String) androidNode.get("xpath");
					String text = (String) androidNode.get("text");
					tempTransition.setOriginElement(oldStates.get(originState - 1).getAndroidNode(resourceID, xpath, text));
				}
				oldTransitions.add(tempTransition);
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
			ifKeyboardHideKeyboard();
			EmulatorHelper.isEventIdle();
			currentState.setId(getSequentialNumber());
			String rawXML = EmulatorHelper.getCurrentViewHierarchy();
			Document parsedXML = loadXMLFromString(rawXML);
			String screenShot = EmulatorHelper.takeAndPullScreenshot(currentState.getId()+"", folderName);
			currentState.setRawXML(rawXML);
			currentState.setParsedXML(parsedXML);
			//Conditions for find a new state
			rippingOutsideApp = isRippingOutsideApp(parsedXML);
			State foundState = findStateInGraph(currentState);
			State sameState = compareScreenShotWithExisting(screenShot);

			if (foundState != null || sameState != null || rippingOutsideApp) {
				// State already exists
				String reason = "";
				if(foundState != null){
					currentState = foundState;
					Helper.deleteFile(screenShot);
					reason = "Found state in graph";
				}else if(sameState != null){
					System.out.println("SAME STATE FOUND BY IMAGE COMPARISON");
					Helper.deleteFile(sameState.getScreenShot());
					File newScreen = new File(screenShot);
					newScreen.renameTo(new File(sameState.getScreenShot()));
					currentState = sameState;
					reason = "Found state by images";
				}else{
					Helper.deleteFile(screenShot);
					currentState = previousState;
					reason = "Ripping out side the app";
				}
				sequentialNumber--;
				if(EmulatorHelper.isHome()) {
					throw new RipException("Execution closed the app");
				}
				System.out.println("State Already Exists: " + reason);
			} else {
				//New State
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
			//End execution if the old transitions are already done
			if(oldTransitions.size()==0) {
				System.out.println("OLD TRANSITIONS EMPTY");
				return;
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
				System.out.println("SALIENDO DE EJECUCIÓN. ESTADO DE INICIO != AL ACTUAL");
				System.out.println(transToBeExec.getOrigin().getId()+" - "+currentState.getId());
				return ;
			} else {
				////Take the next transition in the current state
				Transition tempTrans = currentState.popTransition();
				AndroidNode tempTransAN = tempTrans.getOriginNode();
				//TODO aquí cambié un compareTo por el equals
				//Check 1. same type between the transition and the expected transition
				//	2. same android id source 3. same android node xPath 4. same android node text
				if(tempTrans.getType() != TransitionType.BUTTON_BACK && transToBeExec.getType() != TransitionType.BUTTON_BACK){
					while(!(tempTrans.getType().equals(transToBeExec.getType()))
							|| !tempTransAN.getResourceID().equals(transToBeExecAN.getResourceID())
							|| !tempTransAN.getxPath().equals(transToBeExecAN.getxPath())
							) {
						//If just one of those conditions is false get the next possible transition in the current state
                        executeTransition(tempTrans);
						ifKeyboardHideKeyboard();
						EmulatorHelper.isEventIdle();
						tempTrans = currentState.popTransition();
						tempTransAN = tempTrans.getOriginNode();
					}
				}else{
					while(!(tempTrans.getType().equals(transToBeExec.getType()))) {
						//If just one of those conditions is false get the next possible transition in the current state
                        executeTransition(tempTrans);
						ifKeyboardHideKeyboard();
						EmulatorHelper.isEventIdle();
						tempTrans = currentState.popTransition();
						tempTransAN = tempTrans.getOriginNode();
					}
				}
				//Execute the transition
				executeTransition(tempTrans);
				ifKeyboardHideKeyboard();
				EmulatorHelper.isEventIdle();
				//Remove the transition
				oldTransitions.remove(0);
				//Add one to the iteration counter
				executedIterations++;
				// Waits until the executed transition changes the application current state
				EmulatorHelper.isEventIdle();
				String tranScreenshot = ImageHelper.takeTransitionScreenshot(tempTrans, transitions.size());
				tempTrans.setScreenshot(tranScreenshot);
				//explore recursively
				explore(currentState, tempTrans);

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
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	public static void main(String[] args) {
		if(args.length<1) {
			System.err.println("Please provide config file location");
		} else {
			try {
				new RIPRR(args[0]);
			} catch (RipException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


}
