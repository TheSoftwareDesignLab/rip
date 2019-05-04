package model;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import helper.EmulatorHelper;

public class State {

	/**
	 * State ID
	 */
	private int id;

	/**
	 * Activity name
	 */
	private String activityName;

	/**
	 * Parsed XML
	 */
	private Document parsedXML;

	/**
	 * Raw XML
	 */
	private String rawXML;

	/**
	 * The explored application is hybrid?
	 */
	private boolean hybrid;

	/**
	 * Contextual changes will generated in the state?
	 */
	private boolean contextualChanges;
	
	private int battery;

	/**
	 * List of the state elements
	 */
	private List<AndroidNode> stateNodes;

	/**
	 * Stack of possible transitions of the state. This stack is created based on
	 * the XML properties of the GUI and the contextual changes.
	 */
	private Deque<Transition> possibleTransitions = new ArrayDeque<Transition>();

	/**
	 * Array of outbound transitions
	 */
	private List<Transition> outboundTransitions = new ArrayList<Transition>();

	/**
	 * Array of inbound transitions
	 */
	private List<Transition> inboundTransitions = new ArrayList<Transition>();

	private List<HybridError> hybridErrors;

	private String screenShot;

	private boolean wifiStatus;

	private double cpu;

	private double memory;

	private boolean airplane;


	/**
	 * Creates a new state
	 * 
	 * @param hybrid
	 *            The application is hybrid
	 * @param contextualChanges
	 *            Contextual changes must be invoked
	 */
	public State(boolean hybrid, boolean contextualChanges) {
		this.hybrid = hybrid;
		this.contextualChanges = contextualChanges;
		possibleTransitions = new ArrayDeque<Transition>();
		stateNodes = new ArrayList<AndroidNode>();
		hybridErrors = new ArrayList<HybridError>();
		outboundTransitions = new ArrayList<Transition>();
		inboundTransitions = new ArrayList<Transition>();
		

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getActivityName() {
		return activityName;
	}

	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}

	public Document getParsedXML() {
		return parsedXML;
	}

	public void setParsedXML(Document parsedXML) {
		this.parsedXML = parsedXML;
		generatePossibleTransition();
	}

	public String getRawXML() {
		return rawXML;
	}

	public void setRawXML(String rawXML) {
		this.rawXML = rawXML;
	}

	public Transition popTransition() throws NoSuchElementException {
		return possibleTransitions.pop();
	}

	public void addInboundTransition(Transition pTransition) {
		inboundTransitions.add(pTransition);
	}

	public void addOutboundTransition(Transition pTransition) {
		outboundTransitions.add(pTransition);
	}

	/**
	 * Evaluates the XML view of the file and generate the possible transitions for
	 * the state
	 */
	public void generatePossibleTransition() {

		possibleTransitions.push(new Transition(this, TransitionType.BUTTON_BACK));

		// Add the possible contextual changes in the transitions
		if (contextualChanges == true) {
			possibleTransitions.push(new Transition(this, TransitionType.CONTEXT_INTERNET_OFF));
			possibleTransitions.push(new Transition(this, TransitionType.CONTEXT_INTERNET_OFF));
			possibleTransitions.push(new Transition(this, TransitionType.CONTEXT_LOCATION_OFF));
			possibleTransitions.push(new Transition(this, TransitionType.CONTEXT_LOCATION_ON));
			possibleTransitions.push(new Transition(this, TransitionType.ROTATE_LANDSCAPE));
			possibleTransitions.push(new Transition(this, TransitionType.ROTATE_PORTRAIT));
		}

		// Interactions in hybrid applications
		if (hybrid == true) {
		}

		// GUI interactions
		NodeList allNodes = parsedXML.getElementsByTagName("node");
		Node currentNode;
		AndroidNode newAndroidNode;
		for (int i = 0; i < allNodes.getLength(); i++) {
			currentNode = allNodes.item(i);
			newAndroidNode = new AndroidNode(this, currentNode);
			stateNodes.add(newAndroidNode);
			if (newAndroidNode.isAButton() || newAndroidNode.isClickable() || (hybrid && newAndroidNode.isEnabled())) {
				if (newAndroidNode.isEditableText()) {
					possibleTransitions.push(new Transition(this, TransitionType.GUI_INPUT_TEXT, newAndroidNode));
				} else {
					possibleTransitions.push(new Transition(this, TransitionType.GUI_CLICK_BUTTON, newAndroidNode));
				}
			}
			if (newAndroidNode.isScrollable() ) {
				if(newAndroidNode.getpClass().contains("ViewPager")) {
					possibleTransitions.push(new Transition(this, TransitionType.SWIPE, newAndroidNode));
				} else {
					possibleTransitions.push(new Transition(this, TransitionType.SCROLL, newAndroidNode));
				}
			}
		}
	}

	public void addError(HybridError error) {
		hybridErrors.add(error);

	}

	public void setScreenShot(String screenShot) {
		this.screenShot = screenShot;
	}

	public String getScreenShot() {
		return screenShot;
	}
	

	public int getBattery() {
		return battery;
	}

	public void setBattery(int battery) {
		this.battery = battery;
	}

	public boolean isWifiStatus() {
		return wifiStatus;
	}

	public void setWifiStatus(boolean wifiStatus) {
		this.wifiStatus = wifiStatus;
	}

	public double getCpu() {
		return cpu;
	}

	public void setCpu(double cpu) {
		this.cpu = cpu;
	}

	public double getMemory() {
		return memory;
	}

	public void setMemory(double memory) {
		this.memory = memory;
	}

	public boolean isAirplane() {
		return airplane;
	}

	public void setAirplane(boolean airplane) {
		this.airplane = airplane;
	}

	public boolean hasRemainingTransitions() {
		return !possibleTransitions.isEmpty();
	}

	public void addPossibleTransition(Transition transition) {
		possibleTransitions.addLast(transition);
	}

	public AndroidNode getAndroidNode(String resourceID, String xpath, String text) {

		for (int i = 0; i < stateNodes.size(); i++) {
			AndroidNode temp = stateNodes.get(i);
			if(temp.getxPath().equals(xpath)&&temp.getResourceID().equals(resourceID)&&temp.getText().equals(text)) {
				return temp;
			}
		}
		return null;

	}

	public List<AndroidNode> getStateNodes() {
		return stateNodes;
	}

	public void retrieveContext(String packageName) throws Exception {
		// TODO Auto-generated method stub
		battery = EmulatorHelper.showBatteryLevel();
		wifiStatus = EmulatorHelper.isWifiEnabled();
		cpu = EmulatorHelper.showCPUUsage(packageName);
		memory = EmulatorHelper.showMemoryUsage(packageName);
		airplane = EmulatorHelper.isAirplaneModeOn();
	}

}
