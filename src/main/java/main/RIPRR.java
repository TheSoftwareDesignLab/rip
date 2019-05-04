package main;

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

public class RIPRR extends RIPi18n {

	public String scriptPath;

	public Hashtable<String, State> oldStatesTable;

	public ArrayList<State> oldStates;

	public ArrayList<Transition> oldTransitions;

	public RIPRR(String configFilePath) throws RipException, IOException {
		super(configFilePath);
	}

	@Override
	public void preProcess(JSONObject preProcArgs) {

		scriptPath = (String) preProcArgs.get("scriptPath");
		oldStatesTable = new Hashtable<String, State>();
		oldStates = new ArrayList<State>();
		oldTransitions = new ArrayList<Transition>();

		//JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();

		try (FileReader reader = new FileReader(scriptPath))
		{
			//Read JSON file
			JSONObject obj = (JSONObject) jsonParser.parse(reader);

			int amountStates = Math.toIntExact((long) obj.get(AMOUNT_STATES));
			int amountTransitions = Math.toIntExact((long) obj.get(AMOUNT_TRANSITIONS));

			JSONObject states = (JSONObject) obj.get(STATES);

			for (int i = 0; i < amountStates; i++) {
				State tempState = new State(hybridApp, contextualExploration);
				JSONObject currentState = (JSONObject) states.get((i+1)+"");
				tempState.setRawXML((String)currentState.get("rawXML"));
				tempState.setActivityName((String) currentState.get("activityName"));
				tempState.setId(Math.toIntExact((long) currentState.get("id")));
				Document parsedXML;
				parsedXML = loadXMLFromString(tempState.getRawXML());
				tempState.setParsedXML(parsedXML);
				oldStates.add(tempState);
				oldStatesTable.put(tempState.getRawXML(), tempState);
			}

			JSONObject transitions = (JSONObject) obj.get(TRANSITIONS);

			for (int i = 1; i < amountTransitions; i++) {
				JSONObject currentTransition = (JSONObject) transitions.get(i+"");
				int originState = Math.toIntExact((long) currentTransition.get("stState"));
				TransitionType tType = TransitionType.valueOf((String)currentTransition.get("tranType"));
				int destState = Math.toIntExact((long) currentTransition.get("dsState"));
				Transition tempTransition = new Transition(oldStates.get(originState-1), tType);
				tempTransition.setDestination(oldStates.get(destState-1));
				if(currentTransition.containsKey("androidNode")) {
					JSONObject androidNode = (JSONObject) currentTransition.get("androidNode");
					String resourceID = (String) androidNode.get("resourceID");
					String xpath = (String) androidNode.get("xpath");
					String text = (String) androidNode.get("text");
					tempTransition.setOriginElement(oldStates.get(originState-1).getAndroidNode(resourceID, xpath, text));
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
	public void explore(State previousState, Transition executedTransition) {
		if(oldTransitions.size()==0) {
			return;
		}
		currentState = new State(hybridApp, contextualExploration);
		try {
			String rawXML = EmulatorHelper.getCurrentViewHierarchy();
			rawXML = processXML(rawXML);
			Document parsedXML;
			parsedXML = loadXMLFromString(rawXML);

			currentState.setParsedXML(parsedXML);
			String activity = EmulatorHelper.getCurrentFocus();
			currentState.setActivityName(activity);
			currentState.setRawXML(rawXML);

			State foundState = findStateInGraph(currentState);
			if (foundState != null) {
				// State already exists
				currentState = foundState;
			} else {
				// New state discovered
				int currentStateId = getSequentialNumber();
				currentState.setId(currentStateId);
				String screenShot = EmulatorHelper.takeAndPullScreenshot(currentState.getId()+"", folderName);
				String snapShot = EmulatorHelper.takeAndPullXMLSnapshot(currentState.getId()+"", folderName);
				System.out.println("Current ST: " + currentState.getId());
				rippingOutsideApp = isRippingOutsideApp(parsedXML);
				if (!rippingOutsideApp) {
					statesTable.put(rawXML, currentState);
					states.add(currentState);
					currentState.setScreenShot(screenShot);
					currentState.retrieveContext(packageName);
					ImageHelper.getNodeImagesFromState(currentState);
				} else {
					// Discard state
					sequentialNumber--;
					Helper.deleteFile(screenShot);
					Helper.deleteFile(snapShot);
					//						currentState = sameState;
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
				executedTransition.setDestination(currentState);
				executedTransition.setOrigin(previousState);
				currentState.addInboundTransition(executedTransition);
				previousState.addOutboundTransition(executedTransition);
				transitions.add(executedTransition);
			}

			Transition transToBeExec = oldTransitions.get(0);
			AndroidNode transToBeExecAN = transToBeExec.getOriginNode();

			System.out.println(currentState.getId());
			for (int i = 0; i < oldTransitions.size(); i++) {
				System.out.println((i+1)+": "+oldTransitions.get(i).getOrigin().getId()+" - "+oldTransitions.get(i).getDestination().getId()+" - "+oldTransitions.get(i).getType().name());
			}

			if(transToBeExec.getOrigin().getId()!=currentState.getId()) {
				System.out.println(transToBeExec.getOrigin().getId()+" - "+currentState.getId());
				return ;
			} else {
				Transition tempTrans = currentState.popTransition();
				AndroidNode tempTransAN = tempTrans.getOriginNode();
				while(tempTrans.getType().compareTo(transToBeExec.getType())!=0
						|| !tempTransAN.getResourceID().equals(transToBeExecAN.getResourceID())
						|| !tempTransAN.getxPath().equals(transToBeExecAN.getxPath())
						|| !tempTransAN.getText().equals(transToBeExecAN.getText())) {
					tempTrans = currentState.popTransition();
					tempTransAN = tempTrans.getOriginNode();
				}
				oldTransitions.remove(0);
				executeTransition(tempTrans);
				Thread.sleep(waitingTime);
				String tranScreenshot = ImageHelper.takeTransitionScreenshot(tempTrans, transitions.size());
				tempTrans.setScreenshot(tranScreenshot);
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
			}
		}
	}


}
