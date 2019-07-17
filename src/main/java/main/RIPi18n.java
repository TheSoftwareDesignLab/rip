package main;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import helper.EmulatorHelper;
import helper.Helper;
import helper.ImageHelper;
import model.State;
import model.Transition;

public class RIPi18n extends RIPBase{


	public RIPi18n(String configFilePath) throws Exception {
		super(configFilePath);
	}

	@Override
	public void explore(State previousState, Transition executedTransition) {
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
				System.out.println("State Already Exists");
			} else {
				// New state discovered
				currentState.setId(getSequentialNumber());
				String screenShot = EmulatorHelper.takeAndPullScreenshot(currentState.getId()+"", folderName);
				String snapShot = EmulatorHelper.takeAndPullXMLSnapshot(currentState.getId()+"", folderName);
				System.out.println("Current ST: " + currentState.getId());
				//State sameState = compareScreenShotWithExisting(screenShot);
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

	public String processXML(String rawXML) {
		return rawXML.replaceAll("(text|focused|checked)=\"[^\"]*\"", "");
	}

	@Override
	public boolean stateChanges() throws IOException, RipException {
		String rawXML = EmulatorHelper.getCurrentViewHierarchy();
		rawXML = processXML(rawXML);
		if (rawXML.equals(currentState.getRawXML())) {
			return false;
		}
		return true;
	}

	public static void main(String[] args) {
		if(args.length<1) {
			System.err.println("Please provide config file location");
		} else {
			try {
				new RIPi18n(args[0]);
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
