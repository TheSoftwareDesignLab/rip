package main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import model.State;
import model.Transition;

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
            
            JSONObject states = (JSONObject) obj.get(STATES);
            
            for (int i = 0; i < amountStates; i++) {
            	int iter = i+1;
            	State tempState = new State(hybridApp, contextualExploration);
            	JSONObject currentState = (JSONObject) states.get(iter+"");
            	tempState.setRawXML((String)currentState.get("rawXML"));
            	
            	
				
			}
 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
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
