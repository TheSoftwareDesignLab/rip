package model;

public class Transition {
	
	private State origin;
	private State destination;
	private TransitionType type;
	private AndroidNode originElement;
	private String screenshot;
	private String inputString;
	private boolean leavesAppCore;
	private int valuableTransNumber;
	/**
	 * Creates a transition with a known origin state and a type
	 * @param origin
	 * @param type
	 */
	public Transition(State origin, TransitionType type) {
		this.origin = origin;
		this.type = type;
		leavesAppCore = false;
	}
	
	/**
	 * Creates a transition with a known origin, a known element and a type
	 * @param origin
	 * @param originElement
	 * @param type
	 */
	public Transition(State origin, TransitionType type, AndroidNode originElement) {
		this.origin = origin;
		this.type = type;
		this.originElement = originElement;
		this.inputString = "";
	}

	public String getInputString(){
		return this.inputString;
	}

	/**
	 * @return the leavesAppCore
	 */
	public boolean isLeavesAppCore() {
		return leavesAppCore;
	}

	/**
	 * @param leavesAppCore the leavesAppCore to set
	 */
	public void setLeavesAppCore(boolean leavesAppCore) {
		this.leavesAppCore = leavesAppCore;
	}

	public void setInputString(String inputString){
		this.inputString = inputString;
	}

	public State getOrigin() {
		return origin;
	}

	public void setOrigin(State origin) {
		this.origin = origin;
	}

	public State getDestination() {
		return destination;
	}

	public void setDestination(State destination) {
		this.destination = destination;
	}

	public TransitionType getType() {
		return type;
	}

	public void setType(TransitionType type) {
		this.type = type;
	}

	public AndroidNode getOriginNode() {
		return originElement;
	}

	public void setOriginElement(AndroidNode originElement) {
		this.originElement = originElement;
	}
	
	public String toString() {
		if(origin != null && destination != null){
			return origin.getId()+";"+destination.getId()+";"+type;
		} else if(origin != null){
			return origin.getId() + ";" + destination +";" + type;
		}
		else{
			return  origin + ";" + destination.getId()+ ";" + type;
		}
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

	public AndroidNode getOriginElement() {
		return originElement;
	}

	public void setValuableTransNumber(int valuableTransNumber) {
		this.valuableTransNumber = valuableTransNumber;
	}

	/**
	 * @return the valuableTransNumber
	 */
	public int getValuableTransNumber() {
		return valuableTransNumber;
	}

}
