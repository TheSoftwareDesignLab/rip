package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.NoSuchElementException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import helper.EmulatorHelper;
import helper.ExternalProcess2;
import helper.Helper;
import model.State;
import model.Transition;
import model.TransitionType;

public class RIPi18n extends RIPBase{


	private RIPi18n(String apkPath, String outputFolder, String isHybrid) throws RipException, IOException {
		super(apkPath, outputFolder, isHybrid);
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
			currentState.setRawXML(rawXML);

			State foundState = findStateInGraph(currentState);
			if (foundState != null) {
				// State already exists
				currentState = foundState;

			} else {
				// New state discovered
				currentState.setId(getSequentialNumber());
				String screenShot = EmulatorHelper.takeAndPullScreenshot(currentState.getId()+"", folderName);
				String snapShot = EmulatorHelper.takeAndPullXMLSnapshot(currentState.getId()+"", folderName);
				System.out.println("Current ST: " + currentState.getId());
				//					State sameState = compareScreenShotWithExisting(screenShot);
				rippingOutsideApp = isRippingOutsideApp(parsedXML);
				if (!rippingOutsideApp) {
					statesTable.put(rawXML, currentState);
					states.add(currentState);
					currentState.setScreenShot(screenShot);
				} else {
					// Discard state
					sequentialNumber--;
					Helper.deleteFile(screenShot);
					Helper.deleteFile(snapShot);
					//						currentState = sameState;
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
			while (!stateChanges) {
				stateTransition = currentState.popTransition();
				executeTransition(stateTransition);
				// Waits until the executed transition changes the application current state
				Thread.sleep(waitingTime);
				// Checks if the application changes due to the executed transition
				stateChanges = stateChanges();
			}

			// If the state changes, recursively explores the application
			if (stateChanges) {
				explore(currentState, stateTransition);
			}

		} catch (NoSuchElementException e) {
			// There are no more possible transitions in the current state
		} catch (ParserConfigurationException | SAXException e) {
			// Error parsing the XML DOM
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException | RipException e) {
			// Error getting the current view hierarchy
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		}
	}

	@Override
	public void buildFiles() throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(folderName + File.separator + "rstGraph.csv"));
		writer.write("strNodeID;dstNodeID;edgeType");
		writer.newLine();
		transitions.forEach(t -> {
			try {
				writer.write(t.toString());
				writer.newLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		writer.close();

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
		if(args.length<3) {
			System.err.println("Some arguments are missing, please provide apk location and outputfolder");
		} else {
			try {
				new RIPi18n(args[0], args[1], args[2]);
			} catch (RipException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}



}
