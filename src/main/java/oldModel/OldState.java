package oldModel;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class OldState {
	@JsonIgnore
	private int id;
	@JsonIgnore
	private Document xml;
	private String currentFocus;
	private String focusedApp;
	private String screenCapture;
	private String imageName;
	private List<String> buttons;
	private List<String> clickedButtons;
	private List<OldTransition> children;
	private List<OldDomainEntity> attributes;
	private String activityName;
	private boolean webView;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<String> getClickedButtons() {
		return clickedButtons;
	}

	public void setClickedButtons(List<String> clickedButtons) {
		this.clickedButtons = clickedButtons;
	}
	
	public void addClickedButton(String clicked) {
		clickedButtons.add(clicked);
	}

	public List<String> getButtons() {
		return buttons;
	}

	public void setButtons(List<String> buttons) {
		this.buttons = buttons;
	}
	
	public void addButton(String button) {
		buttons.add(button);
	}

	public String getCurrentFocus() {
		return currentFocus;
	}

	public void setCurrentFocus(String currentFocus) {
		this.currentFocus = currentFocus;
	}

	public String getFocusedApp() {
		return focusedApp;
	}

	public void setFocusedApp(String focusedApp) {
		this.focusedApp = focusedApp;
	}

	public Document getXml() {
		return xml;
	}

	public void setXml(Document xml) {
		this.xml = xml;
		webView = containsWebView();
	}

	public List<OldTransition> getChildren() {
		return children;
	}

	public void setChildren(List<OldTransition> children) {
		this.children = children;
	}
	
	public void addTransition(OldTransition tran) {
		children.add(tran);
	}

	public String getScreenCapture() {
		return screenCapture;
	}

	public void setScreenCapture(String screenCapture) {
		this.screenCapture = screenCapture;
	}
	
	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public List<OldDomainEntity> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<OldDomainEntity> attributes) {
		this.attributes = attributes;
	}

	public void addAttribute(OldDomainEntity attr) {
		attributes.add(attr);
	}

	public String getActivityName() {return activityName;}

	public void setActivityName(String activityName) {this.activityName = activityName;	}

	public OldState ()
	{
		id = -1;
		children = new ArrayList<OldTransition>();
		buttons = new ArrayList<String>();
		clickedButtons = new ArrayList<String>();
		attributes = new ArrayList<>();
		currentFocus = "";
		focusedApp = "";
		screenCapture = "";
		imageName = "";
		webView = false;
	}
	
	public boolean containsWebView () {
		return false;
	}
	
}
