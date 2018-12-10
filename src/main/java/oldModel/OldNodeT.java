package oldModel;

import org.w3c.dom.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class OldNodeT {	
	@JsonIgnore
	private Document xml;
	private String currentFocus;
	private String focusedApp;
	
	
	
	public OldNodeT() {
		this.currentFocus = "";
		this.focusedApp = "";
	}
	public Document getXml() {
		return xml;
	}
	public void setXml(Document xml) {
		this.xml = xml;
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

}
