package model;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class AndroidNode {

	public static String TRUE = "true";
	public static String FALSE = "false";

	private State state;
	private boolean interacted;
	public boolean clickable;
	private int[] centralPoint;
	private int[] point1;
	private int[] point2;
	private String pClass;

	public AndroidNode(State state, Node domNode) {
		this.state = state;
		loadAttributesFromDom(domNode);
	}

	public void loadAttributesFromDom(Node domNode) {
		NamedNodeMap attributes = domNode.getAttributes();
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
					
				}
			}
			else {
				System.out.println("IMPORTANT: Property "+ attribute.getNodeName() + " is not included in RIP");
			}
		}
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

	public int getCentralX() {
		return centralPoint[0];
	}

	public int getCentralY() {
		return centralPoint[1];
	}
	
	public String getpClass() {
		return pClass;
	}

}
