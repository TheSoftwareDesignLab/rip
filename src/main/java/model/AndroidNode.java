package model;

import oldModel.OldDomainEntity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static model.AndroidNodeProperty.RESOURCE_ID;

public class AndroidNode {

	public static String TRUE = "true";
	public static String FALSE = "false";

	private State state;
	private boolean interacted;
	private boolean clickable;
	private int[] centralPoint;
	private int[] point1;
	private int[] point2;
	private String pClass;
	private boolean enabled;
	private String resourceID="";
	private String text="";
	private String name="";
	private String xPath="";
	private String index="";
	private boolean scrollable;
	private String originalText = "";

	private Domain.Type type;

	public AndroidNode(State state, Node domNode) {
		this.state = state;
		loadAttributesFromDom(domNode);
		String[] classes = pClass.split("\\.");
		xPath = domNode.getAttributes().getNamedItem("index").getNodeValue()+"_"+(!pClass.equals("")?classes[classes.length-1]:"")+(!resourceID.equals("")?"/"+resourceID:"");
		Node temp = domNode.getParentNode();
		while(!temp.getNodeName().equals("hierarchy")) {
			NamedNodeMap teemp = temp.getAttributes();
			String [] classess = teemp.getNamedItem("class").getNodeValue().split("\\.");
			String indexx = teemp.getNamedItem("index").getNodeValue();
			xPath=indexx+"_"+classess[classess.length-1]+"|"+xPath;
			temp = temp.getParentNode();
		}
	}

	public void loadAttributesFromDom(Node domNode) {
		NamedNodeMap attributes = domNode.getAttributes();
		name = domNode.getNodeName();
		String bounds;
		String attributeValue;
		AndroidNodeProperty androidNodeProperty;
		for (int j = 0; j < attributes.getLength(); j++) {
			Node attribute = attributes.item(j);
			attributeValue = attribute.getNodeValue();
			 androidNodeProperty = AndroidNodeProperty.fromName(attribute.getNodeName());
			if (androidNodeProperty != null) {
				switch (androidNodeProperty) {
				case CLICKABLE:
					clickable = attributeValue.equals(TRUE);
					break;
				case BOUNDS:
					loadBounds(attributeValue);
					break;
				case CLASS:
					pClass = attributeValue;
					break;
				case ENABLED:
					enabled = true;
					break;
				case RESOURCE_ID:
					resourceID = attributeValue;
					break;
				case TEXT:
					text = attributeValue;
					originalText = text;
					break;
				case INDEX:
					index = attributeValue;
					break;
				case SCROLLABLE:
					scrollable = Boolean.parseBoolean(attributeValue);
					break;
				}
			}
			else {
				System.out.println("IMPORTANT: Property "+ attribute.getNodeName() + " is not included in RIP");
			}
		}
	}
	
	
	
	public String getxPath() {
		return xPath;
	}

	public String getResourceID() {
		return resourceID;
	}

	public boolean isScrollable() {
		return scrollable;
	}

	public String getText() {
		return text;
	}

	public String getName() {
		return name;
	}

	/**
	 * Calculates the bounds and central point of a node
	 * @param text Raw input
	 * Initializes point1, point2 and centralPoint
	 */
	public void loadBounds(String text) {
		String bounds = text.replace("][", "/").replace("[", "").replace("]", "");
		bounds += "/0";
		String[] coords = bounds.split("/");
		String coord1 = coords[0];
		String coord2 = coords[1];
		String[] points1 = coord1.split(",");
		String[] points2 = coord2.split(",");
		int x1 = Integer.parseInt(points1[0]);
		int x2 = Integer.parseInt(points2[0]);
		int y1 = Integer.parseInt(points1[1]);
		int y2 = Integer.parseInt(points2[1]);
		point1 = new int[] {x1,y1};
		point2 = new int[] {x2,y2};
		centralPoint = new int[] {(int)((x1+x2)/2), (int)((y1+y2)/2)};
	}

	public boolean isClickable() {
		return clickable;
	}
	
	public boolean isAButton() {
		
		switch(pClass) {
		case "android.widget.Button":
			return true;
		}
		
		return pClass.toLowerCase().contains("button");
	}

	public boolean isDomainAttribute() {
		switch (pClass) {
			case "android.widget.CheckBox":
				type = Domain.Type.BOOLEAN;
				return true;

			case "android.widget.EditText":
				type = Domain.Type.STRING;
				return true;

			case "android.widget.Button":
				type = Domain.Type.BUTTON;
				return true;

			case "android.widget.RadioGroup":
				type = Domain.Type.LIST;
				return true;
		}

		return false;

	}

	public boolean isEditableText() {

		switch (pClass) {
			case "android.widget.EditText":
				return true;
		}

		return pClass.toLowerCase().contains("EditText");

	}

	public int getCentralX() {
		return centralPoint[0];
	}

	public int getCentralY() {
		return centralPoint[1];
	}
	
	public String getpClass() {
		return pClass;
	}

	public Domain.Type getType() {
		return type;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int[] getPoint1() {
		return point1;
	}

	public int[] getPoint2() {
		return point2;
	}

	public String toString(){
		return "STATE: " + state + ", INTERACTED: " + interacted + ", CLICKEABLE: " + clickable + ",\n CENTRAL POINT: " + centralPoint + ", POINT1: " + point1 +
				",\n POINT2: " + point2 + ", pCLASS: " + pClass +", ENABLED: " + enabled+",\n RESOURCEID: " +resourceID +", TEXT: " + text + ", NAME: " + name  +
				",\n xPATH: " +xPath + ", INDEX:" + index +", SCROLLABLE: " + scrollable;
	}

	public String getOriginalText(){
		return originalText;
	}
}
