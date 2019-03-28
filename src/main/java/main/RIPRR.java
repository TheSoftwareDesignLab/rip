package main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import model.State;
import model.Transition;
import model.TransitionType;

public class RIPRR extends RIPi18n {

	public String scriptPath;

	public Hashtable<String, State> oldStatesTable;

	public ArrayList<State> oldStates;

	public ArrayList<Transition> oldTransitions;

	public RIPRR(String apkPath, String outputFolder, String isHybrid, String scriptPath) throws RipException, IOException {
		super(apkPath, outputFolder, isHybrid, new String[] {scriptPath});
	}

	@Override
	public void preProcess(String[] preProcArgs) {

		scriptPath = preProcArgs[0];

		//JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();

		try (FileReader reader = new FileReader(scriptPath))
		{
			//Read JSON file
			JSONObject obj = (JSONObject) jsonParser.parse(reader);

			int amountStates = (int)obj.get(AMOUNT_STATES);
			int amountTransitions = (int)obj.get(AMOUNT_TRANSITIONS);

			JSONObject states = (JSONObject) obj.get(STATES);

			for (int i = 0; i < amountStates; i++) {
				State tempState = new State(hybridApp, contextualExploration);
				JSONObject currentState = (JSONObject) states.get((i+1)+"");
				tempState.setRawXML((String)currentState.get("rawXML"));
				tempState.setActivityName((String) currentState.get("activityName"));
				tempState.setId(Integer.parseInt((String)currentState.get("id")));
				Document parsedXML;
				parsedXML = loadXMLFromString(tempState.getRawXML());
				tempState.setParsedXML(parsedXML);
				oldStates.add(tempState);
				oldStatesTable.put(tempState.getRawXML(), tempState);
			}
			
			JSONObject transitions = (JSONObject) obj.get(TRANSITIONS);
			
			State initialState = new State(hybridApp, contextualExploration);
			initialState.setId(0);
			Transition initialTransition = new Transition(initialState, TransitionType.FIRST_INTERACTION);
			initialTransition.setDestination(oldStates.get(0));
			oldTransitions.add(initialTransition);

			for (int i = 1; i < amountTransitions; i++) {
				JSONObject currentTransition = (JSONObject) transitions.get(i+"");
				int originState = Integer.parseInt((String)currentTransition.get("stState"));
				TransitionType tType = TransitionType.valueOf((String)currentTransition.get("tranType"));
				int destState = Integer.parseInt((String)currentTransition.get("dsState"));
				Transition tempTransition = new Transition(oldStates.get(originState-1), tType);
				tempTransition.setDestination(oldStates.get(destState-1));
				if(currentTransition.containsKey("androidNode")) {
					JSONObject androidNode = (JSONObject) currentTransition.get("androidNode");
					String resourceID = (String) androidNode.get("resourceID");
					String xpath = (String) androidNode.get("xpath");
					String text = (String) androidNode.get("text");
					tempTransition.setOriginElement(oldStates.get(originState-1).getAndroidNode(resourceID, xpath, text));
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void explore(State previousState, Transition executedTransition) {


	}

	public static void main(String[] args) {
		if(args.length<4) {
			System.err.println("Some arguments are missing, please provide apk location, outputfolder, boolean value if AUT is hybrid and executionScript from RIP");
		} else {
			try {
				new RIPRR(args[0], args[1], args[2], args[4]);
			} catch (RipException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


}
