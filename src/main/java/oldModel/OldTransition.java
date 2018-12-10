package oldModel;

import java.util.List;

public class OldTransition {
	
	private int id;
	private List<String> actions;
	private String screenCapture;
	private OldNodeT node;
	private OldState destination;
	
	public OldTransition() {
		screenCapture = "";
	}
	public List<String> getActions() {
		return actions;
	}
	public void setActions(List<String> actions) {
		this.actions = actions;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public OldNodeT getNode() {
		return node;
	}
	public void setNode(OldNodeT node) {
		this.node = node;
	}
	
	public String getScreenCapture() {
		return screenCapture;
	}
	public void setScreenCapture(String screenCapture) {
		this.screenCapture = screenCapture;
	}
	public OldState getDestination() {
		return destination;
	}
	public void setDestination(OldState destiny) {
		this.destination = destiny;
	}
	

}
