package main;

import java.util.List;

import org.w3c.dom.Document;

import oldModel.OldState;

public class DomainModelBuilder {

	public static String createDomainModel(List<OldState> states) {
		Document xml;
		for (OldState state : states) {
/*
			xml = state.getXml();
*/
				System.out.println("el xml "+state.getFocusedApp());
			}
		
		return "TODO";
	}

}
