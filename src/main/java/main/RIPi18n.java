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

//	@Override
//	public void processState(State previousState, Transition executedTransition) 
//			throws IOException, RipException, ParserConfigurationException, SAXException, Exception {
//		ifKeyboardHideKeyboard();
//		EmulatorHelper.isEventIdle();
////		currentState = new State(hybridApp, contextualExploration);
//		currentState.setId(getSequentialNumber());		
//		String rawXML = EmulatorHelper.getCurrentViewHierarchy();
//		rawXML = processXML(rawXML);
//		Document parsedXML;
//		parsedXML = loadXMLFromString(rawXML);
//		currentState.setParsedXML(parsedXML);
//		String activity = EmulatorHelper.getCurrentFocus();
//		currentState.setActivityName(activity);
//		currentState.setRawXML(rawXML);
//
//		State foundState = findStateInGraph(currentState);
//		if (foundState != null) {
//			// State already exists
//			currentState = foundState;
//			System.out.println("State Already Exists");
//		} else {
//			// New state discovered
//			String screenShot = EmulatorHelper.takeAndPullScreenshot(currentState.getId()+"", folderName);
//			String snapShot = EmulatorHelper.takeAndPullXMLSnapshot(currentState.getId()+"", folderName);
//			System.out.println("Current ST: " + currentState.getId());
//			//State sameState = compareScreenShotWithExisting(screenShot);
//			rippingOutsideApp = isRippingOutsideApp(parsedXML);
//			if (!rippingOutsideApp) {
//				statesTable.put(rawXML, currentState);
//				states.add(currentState);
//				currentState.setScreenShot(screenShot);
//				currentState.retrieveContext(packageName);
//				ImageHelper.getNodeImagesFromState(currentState);
//			} else {
//				// Discard state
//				sequentialNumber--;
//				Helper.deleteFile(screenShot);
//				Helper.deleteFile(snapShot);
//				if(EmulatorHelper.isHome()) {
//					throw new RipException("Execution closed the app");
//				}
//				if(rippingOutsideApp) {
//					currentState = previousState;
//				}
//			}
//		}
//		if (currentState.hasRemainingTransitions() && !rippingOutsideApp) {
//			previousState.addPossibleTransition(executedTransition);
//		}
//
//		if (!rippingOutsideApp) {
//			currentState.addInboundTransition(executedTransition);
//			previousState.addOutboundTransition(executedTransition);
//			executedTransition.setDestination(currentState);
//			executedTransition.setOrigin(previousState);
//			transitions.add(executedTransition);
//		}
//	}	

	@Override
	public String processXML(String rawXML) {
		return rawXML.replaceAll("(text|focused|checked|password)=\"[^\"]*\"", "");
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
				EmulatorHelper.shutdownEmulators();
				new RIPi18n(args[0]);
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
