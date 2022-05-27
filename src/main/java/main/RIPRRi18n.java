package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import helper.EmulatorHelper;
import helper.Helper;
import helper.ImageHelper;
import model.AndroidNode;
import model.State;
import model.Transition;
import model.TransitionType;

public class RIPRRi18n extends RIPRR {

	public RIPRRi18n(String configFilePath) throws Exception {
		super(configFilePath);
		// TODO Auto-generated constructor stub
	}
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
				new RIPRRi18n(args[0]);
			} catch (RipException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


}
