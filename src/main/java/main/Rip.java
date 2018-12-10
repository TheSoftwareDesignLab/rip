package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import helper.ExternalProcess;
import oldModel.*;
import webapp.App;

import static sun.misc.Version.println;

public class Rip {

	public static final String APPS_FOLDER = "androidApps";
	public static final String ADB_PATH = "";
	public static String DESTINATION_FOLDER = "./generated";

	public static final String MODE_1 = "1. DFS (Depth-first search mode)";
	public static final String MODE_2 = "2. Limited events (explore until a number of events occur)";
	public static final String MODE_3 = "3. Limited time (explore during a determined time)";

	private FileWriter out;
	private FileWriter meta;
	private int index;
	private List<OldState> sequentialStates;
	private List<OldContext> contexts;
	private List<OldTransition> sequentialTransitions;
	private List<OldTransition> transitions;
	private String pathAPK;
	private String packageName;
	private String mainActivity;
	private String aapt;
	private HashMap<String, String> metaInfo;

	private int maxIterations = 10000;
	private int maxTime = 1000;

	public Rip() {
		try {

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			// Ask for the folder name where all the screenshots and xml will be saved

			String noVis = System.getenv("NO_VIS");
			boolean noVisualization = false;
			if (noVis != null) {
				if (noVis.equals("True") || noVis.equals("true") || noVis.equals("on")) {
					noVisualization = true;
				}
			}
			pathAPK = System.getenv("APK_LOCATION");
			if (pathAPK.equals("") || pathAPK == null) {
				throw new Exception("Please set the environmental variable APK_LOCATION");
			}

			String folderName = pathAPK;
			File apkFile = new File(folderName);
			Date nowFolder = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("hhmmss");
			String time = dateFormat.format(nowFolder);
			folderName = apkFile.getName()+"_"+time;
			
			if (!noVisualization) {
				System.out.println("Enter a name for your project or press intro to start WEB visualization tool:");
				folderName = br.readLine();
			}

			if (folderName == null || folderName.equals("")) {
				System.out.println("Default port is 8080, press INTRO to start or enter the port");
				folderName = br.readLine();
				if (folderName == null || folderName.equals("")) {
					new App("8080", true);
				} else {
					new App(folderName.trim(), true);
				}
			} else {

				index = 0;
				contexts = new ArrayList<>();
				if (System.getenv("DESTINATION_FOLDER") != null) {
					DESTINATION_FOLDER = System.getenv("DESTINATION_FOLDER");
				}
				String basePath = DESTINATION_FOLDER + "/" + folderName;
				new File(basePath).mkdirs();

				// Create a new folder
				createNewFolder(folderName);

				aapt = System.getenv("AAPT_LOCATION");

				System.out.println("AAPT Location " + aapt + " APK Location " + pathAPK);

				if (aapt.length() > 3) {
					packageName = ExternalProcess.getPackageName(aapt, pathAPK);
					mainActivity = ExternalProcess.getMainActivity(aapt, pathAPK);
				} else {
					System.out.println("Please set the environmental variable AAPT_LOCATION");
				}

				// Gets execution mode based in user input
				getExecutionMode(br);

				String version = ExternalProcess.getAndroidVersion();
				String aV = "command: GET ANDROID VERSION; parameters: none; "
						+ new Timestamp(System.currentTimeMillis());
				out.write(aV + "\n");
				out.flush();
				meta.write("ANDROID VERSION: " + version + "\n");
				metaInfo.put("androidVersion", version);
				meta.flush();

				meta.write("APK: " + pathAPK + "\n");
				metaInfo.put("apk", pathAPK);
				Timestamp now = new Timestamp(System.currentTimeMillis());
				meta.write("STARTING DATE: " + now.toString() + "\n");
				metaInfo.put("startingDate", now.toString());
				meta.flush();

				// Install chosen apk
				installAPK();

				// Launch the main activity

				if (packageName == null || packageName.equals("") || mainActivity == null || mainActivity.equals("")) {
					/*
					 * packageName = "com.ppg.spunky_java"; mainActivity =
					 * "com.ppg.spunky_java.MainActivity";
					 */

					/*
					 * packageName = "me.kuehle.carreport"; mainActivity = ".gui.MainActivity";
					 */
					System.out.println("Please set the environmental variable AAPT_LOCATION");

				}

				String activityP = packageName + "/" + mainActivity;

				// activityP = "com.ppg.spunky_java/com.ppg.spunky_java.MainActivity";
				// activityP = "me.kuehle.carreport/.gui.MainActivity";
				// activityP = "org.tasks/com.todoroo.astrid.activity.TaskListActivity";

				ExternalProcess.startActivity(activityP);

				String activity = "command: START APP; parameters: " + activityP + "; "
						+ new Timestamp(System.currentTimeMillis());
				out.write(activity + "\n");
				out.flush();

				getDeviceData();

				int numButtons = 1;

				long currentT = System.currentTimeMillis();
				long after = System.currentTimeMillis();
				long initial = maxTime * 60000;
				long difference = after - currentT;
				List<OldState> states = new ArrayList<OldState>();
				List<String> boundsStates = new ArrayList<String>();
				List<Integer> st = new ArrayList<>();
				List<String> sta = new ArrayList<>();
				List<Integer> seqTran = new ArrayList<>();
				int transitionId = 0;

				try {
					run(numButtons, initial, difference, transitionId, maxIterations, folderName, states, boundsStates,
							after, currentT, out, st, sta, seqTran);
					out.close();
					meta.close();
				} catch (Exception e) {
					System.out.println("FATAL ERROR 1");
					System.out.println(e.getMessage());
					drawTransitions(boundsStates, states, folderName, st, sta, seqTran);
					e.printStackTrace();
				}
				drawTransitions(boundsStates, states, folderName, st, sta, seqTran);

				if (!noVisualization) {
					System.out.println("Do you want to open web visualization tool? (y/n)");
					String response = br.readLine().trim();

					if (response.toUpperCase().trim().startsWith("Y")) {
						System.out.println("Default port is 8080, press INTRO to start or enter the port");
						response = br.readLine();
						if (response == null || response.equals("")) {
							new App("8080", true);
						} else {
							new App(response.trim(), true);
						}

					}
				}
				System.out.println("Shutting down RIP...");
			}
		} catch (Exception e) {
			System.out.println("FATAL ERROR 2");
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				out.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Saves in the meta file data of the device
	 * (orientation,resolution,dimensions,sensors,services)
	 * 
	 * @throws Exception
	 */
	private void getDeviceData() throws Exception {
		int orientation = ExternalProcess.getCurrentOrientation();
		String ori = "";
		if (orientation == 0) {
			ori = "Portrait";
		} else {
			ori = "Landscape";
		}
		String or = "command: GET CURRENT ORIENTATION; parameters: none; " + new Timestamp(System.currentTimeMillis());
		out.write(or + "\n");
		out.flush();
		meta.write("CURRENT ORIENTATION: " + ori + "\n");
		metaInfo.put("currentOrientation", ori);
		meta.flush();

		int resol = ExternalProcess.getDeviceResolution();
		String re = "command: GET DEVICE RESOLUTION; parameters: none; " + new Timestamp(System.currentTimeMillis());
		out.write(re + "\n");
		out.flush();
		meta.write("DEVICE RESOLUTION: " + resol + "\n");
		metaInfo.put("deviceResolution", resol + "");
		meta.flush();

		String dimensions = ExternalProcess.getScreenDimensions();
		String dim = "command: GET DEVICE DIMENSIONS; parameters: none; " + new Timestamp(System.currentTimeMillis());
		out.write(dim + "\n");
		out.flush();
		meta.write("DEVICE DIMENSIONS: " + dimensions + "\n");
		metaInfo.put("deviceDimensions", dimensions + "");
		meta.flush();

		String sensors = ExternalProcess.getSensors();
		meta.write("SENSORS: \n");
		meta.write(sensors);
		metaInfo.put("sensors", sensors + "");
		meta.flush();

		String services = ExternalProcess.getServices("'" + packageName + "'");
		meta.write("SERVICES: \n");
		meta.write(services);
		metaInfo.put("services", services + "");
		meta.flush();

	}

	/**
	 * Installs apk on emulator/device
	 * 
	 * @throws IOException
	 */
	private void installAPK() throws IOException {

		try {

			ExternalProcess.installAPK(pathAPK);

			String install = "command: INSTALL APP; parameters: " + pathAPK + "; "
					+ new Timestamp(System.currentTimeMillis());
			out.write(install + "\n");
			out.flush();

		} catch (Exception e) {
			System.out.println("ERROR INSTALLING APK");
			System.out.println(e.getMessage());
			String install = "command: INSTALL APP; parameters: " + pathAPK + "; "
					+ new Timestamp(System.currentTimeMillis());
			out.write(install + "\n");
			out.flush();
		}
	}

	/**
	 * Gets execution mode (1, 2 or 3)
	 * 
	 * @param br
	 * @throws IOException
	 */
	private void getExecutionMode(BufferedReader br) throws IOException {

		int executionMode = -1;
		String mode = System.getenv("EXECUTION_MODE");
		if (mode != null && mode != "") {
			executionMode = Integer.parseInt(mode);
		} else {
			System.out.println("Choose an execution mode:");
			System.out.println(MODE_1);
			System.out.println(MODE_2);
			System.out.println(MODE_3);
			executionMode = Integer.parseInt(br.readLine());
		}

		while (executionMode > 3 || executionMode < 1) {
			System.out.println("Please enter a valid execution mode");

			System.out.println("Choose an execution mode:");
			System.out.println(MODE_1);
			System.out.println(MODE_2);
			System.out.println(MODE_3);

			executionMode = Integer.parseInt(br.readLine());
		}

		switch (executionMode) {
		case 2:
			System.out.println("How many events will be generated? (positive integer):");
			maxIterations = Integer.parseInt(br.readLine());

			while (maxIterations <= 0) {
				System.out.println("Please enter a valid number (positive integer)");
				System.out.println("How many events will be generated? (positive integer):");
				maxIterations = Integer.parseInt(br.readLine());
			}

			meta.write("EXECUTION METHOD: " + MODE_2 + "\n");
			metaInfo.put("executionMethod", MODE_2);
			meta.write("NUMBER OF EVENTS: " + maxIterations + "\n");
			metaInfo.put("numberOfEvents", maxIterations + "");
			meta.flush();
			break;

		case 3:
			System.out.println("How much time will take the exploration? (minutes):");
			maxTime = Integer.parseInt(br.readLine());

			while (maxTime <= 0) {
				System.out.println("Please enter a positive integer number");
				System.out.println("How much time will take the exploration? (minutes):");
				maxTime = Integer.parseInt(br.readLine());
			}
			meta.write("EXECUTION METHOD: " + MODE_3 + "\n");
			metaInfo.put("executionMethod", MODE_3);
			meta.write("MINUTES: " + maxTime + "\n");
			metaInfo.put("minutes", maxIterations + "");
			meta.flush();
			break;

		default:
			meta.write("EXECUTION METHOD: " + MODE_1 + "\n");
			metaInfo.put("executionMethod", MODE_1 + "");
			meta.flush();
		}
	}

	/**
	 * Creates a new folder to save all results
	 * 
	 * @param folderName
	 *            String
	 * @throws Exception
	 *             if the folder could not be created
	 */
	private void createNewFolder(String folderName) throws IOException {
		String basePath = DESTINATION_FOLDER + "/" + folderName;
		new File(basePath).mkdirs();
		// Create a text file that contains all commands and crashes in RIP
		String path = basePath + "/" + "log.log";
		String path2 = basePath + "/" + "meta.log";
		System.out.println(path);
		out = new FileWriter(path, true);
		meta = new FileWriter(path2, true);
		metaInfo = new HashMap<String, String>();
		meta.write("PROJECT NAME: " + folderName + "\n");
		metaInfo.put("projectName", folderName);
		meta.flush();

	}

	/**
	 * Creates a XML document from a String
	 *
	 * @param xml
	 *            String
	 * @return DOM Document
	 * @throws Exception
	 *             if the document could not be created
	 */
	public static Document loadXMLFromString(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}

	/**
	 * Enters an input in nodes that have EditText class
	 *
	 * @param cNodes
	 *            is the list of nodes of current activity
	 * @throws Exception
	 */
	public static ArrayList<Node> inputText(ArrayList<Node> cNodes, FileWriter out) throws Exception {

		ArrayList<Node> editables = new ArrayList<Node>();
		ArrayList<Node> checks = new ArrayList<Node>();
		ArrayList<Node> toErase = new ArrayList<Node>();

		Node current;
		NamedNodeMap attributes;
		Node attribute;

		for (int i = 0; i < cNodes.size(); i++) {

			current = cNodes.get(i);
			attributes = current.getAttributes();
			attribute = attributes.getNamedItem("class");

			switch (attribute.getNodeValue()) {
			case "android.widget.EditText":
				System.out.println("-----------------");
				editables.add(current);
				toErase.add(current);
				for (int j = 0; j < attributes.getLength(); j++) {
					System.out.println(attributes.item(j));
				}
				break;

			case "android.widget.CheckBox":
				System.out.println("-----------------");
				checks.add(current);
				toErase.add(current);
				for (int j = 0; j < attributes.getLength(); j++) {
					System.out.println(attributes.item(j));
				}
				break;

			case "android.widget.RadioButton":
				System.out.println("-----------------");
				checks.add(current);
				toErase.add(current);
				for (int j = 0; j < attributes.getLength(); j++) {
					System.out.println(attributes.item(j));
				}
				break;
			}
		}
		if (!toErase.isEmpty()) {
			Node erase;
			for (int i = 0; i < toErase.size(); i++) {
				erase = toErase.get(i);
				cNodes.remove(erase);
			}
		}

		if (!editables.isEmpty()) {
			Random rm = new Random();

			for (int i = 0; i < editables.size(); i++) {

				auxEnterInput(editables.get(i), out);

				int type = ExternalProcess.checkInputType();

				if (type == 1) {
					String input = "" + (char) (rm.nextInt(26) + 'A') + (char) (rm.nextInt(26) + 'a')
							+ (char) (rm.nextInt(26) + 'a');
					ExternalProcess.enterInput(input);

					String in = "command: ENTER INPUT; parameters: " + input + "; "
							+ new Timestamp(System.currentTimeMillis());
					out.write(in + "\n");
					out.flush();
				} else {

					String numInput = String.valueOf(rm.nextInt(100));

					ExternalProcess.enterInput(numInput);
					String num = "command: ENTER INPUT; parameters: " + numInput + "; "
							+ new Timestamp(System.currentTimeMillis());
					out.write(num + "\n");
					out.flush();
				}

				ExternalProcess.goBack();
				String back = "command: GO BACK; parameters: none; " + new Timestamp(System.currentTimeMillis());
				out.write(back + "\n");
				out.flush();
			}
		}

		if (!checks.isEmpty()) {
			Random random = new Random();

			for (int i = 0; i < checks.size(); i++) {
				if (random.nextBoolean()) {
					auxEnterInput(checks.get(i), out);

				}
			}
		}
		return cNodes;
	}

	private static void auxEnterInput(Node node, FileWriter out) throws Exception {
		String bounds = node.getAttributes().getNamedItem("bounds").getNodeValue().replace("][", "/").replace("[", "")
				.replace("]", "");
		String[] coords = bounds.split("/");
		String coord1 = coords[0];
		String coord2 = coords[1];
		String[] points1 = coord1.split(",");
		String[] points2 = coord2.split(",");

		int x1 = Integer.parseInt(points1[0]);
		int x2 = Integer.parseInt(points2[0]);

		int y1 = Integer.parseInt(points1[1]);
		int y2 = Integer.parseInt(points2[1]);

		Random rm = new Random();
		int tapX = rm.nextInt(x2 - x1 + 1) + x1;
		int tapY = rm.nextInt(y2 - y1 + 1) + y1;

		ExternalProcess.tap(String.valueOf(tapX), String.valueOf(tapY));
		String n = node.getAttributes().getNamedItem("resource-id").getNodeValue();
		String tapI = "command: TAP; parameters: " + tapX + ", " + tapY + "; "
				+ new Timestamp(System.currentTimeMillis()) + "; " + "id: " + n;
		out.write(tapI + "\n");
		out.flush();
	}

	/**
	 * Method that swipes screen depending if it is ScrollView or ViewPager
	 *
	 * @param sNodes
	 *            is the list of nodes that has scrollable in true
	 */
	public void scroll(ArrayList<Node> sNodes, FileWriter out) {

		String bounds, coord1, coord2;
		String[] coords, points1, points2;
		int x1, x2, y1, y2, tapX, tapX2, tapY, tapY2;
		String tX, tX2, tY, tY2;

		Node current, attribute;
		NamedNodeMap attributes;

		for (int i = 0; i < sNodes.size(); i++) {

			bounds = sNodes.get(i).getAttributes().getNamedItem("bounds").getNodeValue().replace("][", "/")
					.replace("[", "").replace("]", "");
			coords = bounds.split("/");
			coord1 = coords[0];
			coord2 = coords[1];
			points1 = coord1.split(",");
			points2 = coord2.split(",");

			x1 = Integer.parseInt(points1[0]);
			x2 = Integer.parseInt(points2[0]);
			y1 = Integer.parseInt(points1[1]);
			y2 = Integer.parseInt(points2[1]);

			tapX = x1;
			tapX2 = (int) (x2 / 3) * 2;

			tapY = y1;
			tapY2 = (int) (y2 / 3) * 2;

			tX = String.valueOf(tapX);
			tX2 = String.valueOf(tapX2);
			tY = String.valueOf(tapY);
			tY2 = String.valueOf(tapY2);

			current = sNodes.get(i);
			attributes = current.getAttributes();
			attribute = attributes.getNamedItem("class");

			try {
				// Is vertical swipe
				if (attribute.getNodeValue().contains("ScrollView") || attribute.getNodeValue().contains("RecyclerView")
						|| attribute.getNodeValue().contains("ListView")) {
					ExternalProcess.scroll(tX2, tY2, tX2, tY, "1000");

					String vScroll = "command: VERTICAL SCROLL; parameters: " + tX2 + ", " + tY2 + ", " + tX2 + ", "
							+ tY + ", " + "1000" + "; " + new Timestamp(System.currentTimeMillis());
					out.write(vScroll + "\n");
					out.flush();

					System.out.println("SWIPE VERTICAL");
				}
				// Is horizontal swipe
				else if (attribute.getNodeValue().contains("ViewPager")) {
					ExternalProcess.scroll(tX2, tY2, tX, tY2, "1000");

					String hScroll = "command: HORIZONTAL SCROLL; parameters: " + tX2 + ", " + tY2 + ", " + tX + ", "
							+ tY2 + ", " + "1000" + "; " + new Timestamp(System.currentTimeMillis());
					out.write(hScroll + "\n");
					out.flush();
					System.out.println("SWIPE HORIZONTAL");
				}
			} catch (Exception e) {
				System.out.println("CANNOT SCROLL");
			}

		}
	}

	public void drawTransitions(List<String> boundsStates, List<OldState> states, String folderName, List<Integer> st,
			List<String> sta, List<Integer> seqTran) throws Exception {

		transitions = new ArrayList<>();
		String bound, origin, x, y, fin, x2, y2, transition;
		int xInt, yInt, tran, ac, x2Int, y2Int, width, height;

		for (int i = 0; i < boundsStates.size(); i++) {

			bound = boundsStates.get(i);
			OldState actual;
			if (!bound.contains("0,0/0,0")) {
				origin = bound.split("/")[0];
				x = origin.split(",")[0];
				y = origin.split(",")[1];
				xInt = Integer.parseInt(x);
				yInt = Integer.parseInt(y);

				fin = bound.split("/")[1];
				x2 = fin.split(",")[0];
				y2 = fin.split(",")[1];

				transition = bound.split("/")[2];
				tran = Integer.parseInt(transition);

				ac = Integer.parseInt(bound.split("/")[3]);
				actual = states.get(ac);

				x2Int = Integer.parseInt(x2);
				y2Int = Integer.parseInt(y2);

				width = x2Int - xInt;
				height = y2Int - yInt;
				File screen = new File(actual.getScreenCapture());
				BufferedImage img = ImageIO.read(screen);
				Graphics2D g2d = img.createGraphics();

				final float dash1[] = { 20.0f };
				final BasicStroke dashed = new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
						dash1, 0.0f);
				g2d.setStroke(dashed);

				int alpha = 127; // 50% transparent
				Color myColour = new Color(245, 187, 5, alpha);
				g2d.setColor(myColour);
				g2d.fillOval(xInt, yInt, width, height);

				g2d.setColor(Color.RED);
				g2d.drawOval(xInt, yInt, width, height);
				g2d.dispose();

				String route = bound.split("!!")[1];

				File outputfile = new File(route + "T.png");
				ImageIO.write(img, "png", outputfile);

				actual.getChildren().get(tran).setScreenCapture(route + "T.png");
				transitions.add(actual.getChildren().get(tran));
			} else {
				ac = Integer.parseInt(bound.split("/")[3]);
				actual = states.get(ac);
				transitions.add(actual.getChildren().get(0));
			}
		}

		sequentialStates = new ArrayList<>();
		OldState actual;
		int id;
		// int counter;
		for (int i = 0; i < st.size(); i++) {
			id = st.get(i);
			actual = states.get(id);
			String asd = transitions.get(id).getScreenCapture();

			OldState dc = new OldState();
			dc.setXml(actual.getXml());
			dc.setCurrentFocus(actual.getCurrentFocus());
			dc.setFocusedApp(actual.getFocusedApp());
			// dc.setScreenCapture(actual.getScreenCapture());
			dc.setScreenCapture(transitions.get(id).getScreenCapture());

			dc.setImageName(actual.getImageName());
			dc.setChildren(actual.getChildren());
			dc.setButtons(actual.getButtons());
			dc.setClickedButtons(actual.getClickedButtons());
			dc.setAttributes(actual.getAttributes());

			sequentialStates.add(dc);
		}

		sequentialTransitions = new ArrayList<>();
		OldTransition currentTransition = new OldTransition();
		int idTran;

		for (int i = 0; i < seqTran.size(); i++) {
			idTran = seqTran.get(i);

			for (int j = 0; j < transitions.size(); j++) {
				if (transitions.get(j).getId() == idTran) {
					currentTransition = transitions.get(j);
					break;
				}
			}

			OldTransition newTran = new OldTransition();
			newTran.setActions(currentTransition.getActions());
			newTran.setId(currentTransition.getId());
			newTran.setNode(currentTransition.getNode());
			newTran.setScreenCapture(currentTransition.getScreenCapture());

			sequentialTransitions.add(newTran);
		}

		String a;
		String b;
		int tran2;
		int s;
		int s1;
		OldState cur;
		for (int i = 0; i < sta.size(); i++) {

			if (i + 1 < sta.size()) {
				a = sta.get(i);
				b = sta.get(i + 1);
				tran2 = Integer.parseInt(a.split("/")[0]);
				s = Integer.parseInt(a.split("/")[1]);
				s1 = Integer.parseInt(b.split("/")[1]);

				if (a.split("/").length == 2) {
					cur = states.get(s);
					cur.getChildren().get(tran2).setDestination(states.get(s1));
				}
			}
		}

		// Create a tree with the states
		if (states.size() > 0) {
			String tree = createGraph(states);
			String d3File = createD3File(states);
			String seque = createSequentialFile(sequentialStates, sequentialTransitions);
			String metaFileJSON = createMetaJSON();

			/*
			 * String domainModel = DomainModelBuilder.createDomainModel(states);
			 * DomainModelBuilder.createDomainModel(sequentialStates);
			 */

			String pathFile1 = DESTINATION_FOLDER + "/" + folderName + "/tree.json";
			String pathFile2 = DESTINATION_FOLDER + "/" + folderName + "/d3.json";
			String pathFile3 = DESTINATION_FOLDER + "/" + folderName + "/sequential.json";
			String pathFile4 = DESTINATION_FOLDER + "/" + folderName + "/meta.json";

			FileUtils.writeStringToFile(new File(pathFile1), tree);
			FileUtils.writeStringToFile(new File(pathFile2), d3File);
			FileUtils.writeStringToFile(new File(pathFile3), seque);
			FileUtils.writeStringToFile(new File(pathFile4), metaFileJSON);

		} else {
			throw new Exception("No states have been created. States list is empty");
		}
	}

	public String createD3File(List<OldState> states) {
		JSONObject graph = new JSONObject();

		// Nodes creation
		JSONArray nodes = new JSONArray();
		for (int i = 0; i < states.size(); i++) {
			OldState currentState = states.get(i);
			JSONObject node = new JSONObject();
			node.put("name", currentState.getCurrentFocus() + " - " + currentState.getFocusedApp());
			node.put("group", i);
			node.put("image", currentState.getImageName());
			nodes.add(node);
		}

		// Links creation
		JSONArray links = new JSONArray();
		for (int i = 0; i < states.size() - 1; i++) {
			JSONObject link = new JSONObject();
			link.put("source", i);
			link.put("target", i + 1);
			link.put("value", 4);
			links.add(link);
		}

		graph.put("nodes", nodes);
		graph.put("links", links);

		String d3Text = graph.toString();
		return d3Text;
	}

	public String createSequentialFile(List<OldState> states, List<OldTransition> transi) {
		JSONObject graph = new JSONObject();
		JSONArray nodes = new JSONArray();
		JSONArray links = new JSONArray();
		for (int i = 0; i < states.size(); i++) {
			OldState currentState = states.get(i);
			OldContext currentContext = contexts.get(i);
			JSONObject node = new JSONObject();
			node.put("name", currentState.getCurrentFocus() + " - " + currentState.getFocusedApp());
			node.put("image", currentState.getImageName());

			node.put("cpu", currentContext.getCpu());
			node.put("memory", currentContext.getMemory());
			node.put("battery", currentContext.getBattery());
			node.put("temperature", currentContext.getTemperature());
			node.put("wifi", currentContext.isWifi());
			node.put("airplane", currentContext.isAirplaneMode());

			nodes.add(node);
		}

		for (int i = 0; i < transi.size(); i++) {
			OldTransition currentTransition = transi.get(i);
			JSONObject link = new JSONObject();
			link.put("id", currentTransition.getId());
			link.put("actions", currentTransition.getActions());

			String imageName = "";
			if (!currentTransition.getScreenCapture().equals("")) {
				imageName = currentTransition.getScreenCapture().split("/")[3];
			}

			link.put("image", imageName);
			links.add(link);
		}
		graph.put("nodes", nodes);
		graph.put("links", links);
		return graph.toString();

	}

	public String createMetaJSON() {
		JSONObject info = new JSONObject();
		info.putAll(metaInfo);
		return info.toString();
	}

	public String createGraph(List<OldState> states) throws JsonProcessingException {
		JSONObject graph = new JSONObject();
		JSONArray nodes = new JSONArray();
		JSONArray links = new JSONArray();
		int maxId = -1;

		// Alternativa a nodes

		ObjectMapper mapper = new ObjectMapper();
		String jsonInString = "";

		for (int i = 0; i < states.size(); i++) {
			OldState currentState = states.get(i);
			JSONObject node = new JSONObject();
			String[] activityarray = currentState.getFocusedApp().split(packageName + "/.");
			String activityName = "";
			if (activityarray.length > 1) {
				activityName = activityarray[1];
			}

			// Se deben considerar los casos de crash

			node.put("id", currentState.getId());
			if (currentState.getId() > maxId) {
				maxId = currentState.getId();
			}
			node.put("currentFocus", currentState.getCurrentFocus());
			node.put("focusedApp", currentState.getFocusedApp());
			node.put("screenCapture", currentState.getScreenCapture());
			node.put("imageName", currentState.getImageName());
			node.put("buttons", currentState.getButtons());
			node.put("clickedButtons", currentState.getClickedButtons());
			node.put("name", "(" + currentState.getId() + ") " + currentState.getCurrentFocus());
			node.put("activityName", activityName);

			JSONArray model = new JSONArray();
			for (OldDomainEntity attribute : currentState.getAttributes()) {
				JSONObject attributeJSON = new JSONObject();

				attributeJSON.put("name", attribute.getName());
				attributeJSON.put("field", attribute.getField());
				attributeJSON.put("type", attribute.getType().toString());
				if (attribute.getValues() != null) {
					attributeJSON.put("values", attribute.getValues());
				}
				model.add(attributeJSON);

			}
			node.put("model", model);
			nodes.add(node);

		}
		maxId++;

		// Agregar todos los links
		for (int i = 0; i < states.size(); i++) {

			OldState currentState = states.get(i);
			for (int j = 0; j < currentState.getChildren().size(); j++) {

				JSONObject link = new JSONObject();
				OldTransition currentTransition = currentState.getChildren().get(j);
				link.put("id", currentTransition.getId());
				link.put("source", i);

				if (currentTransition.getDestination() != null) {
					link.put("target", currentTransition.getDestination().getId());
				} else {
					link.put("target", maxId);
				}

				link.put("actions", currentTransition.getActions());
				link.put("screenCapture", currentTransition.getScreenCapture());

				if (!currentTransition.getScreenCapture().equals("")) {
					String[] divisions = currentTransition.getScreenCapture().split("/");
					String image = divisions[divisions.length - 1];

					link.put("imageName", image);
				} else {
					link.put("imageName", "");
				}

				link.put("value", 2);

				links.add(link);
			}
		}

		// Creates End Of Execution node
		JSONObject finalNode = new JSONObject();
		finalNode.put("currentFocus", "NA");
		finalNode.put("focusedApp", "NA");
		finalNode.put("screenCapture", "NA");
		finalNode.put("imageName", "NA");
		finalNode.put("buttons", "NA");
		finalNode.put("clickedButtons", "NA");
		finalNode.put("id", maxId);
		finalNode.put("name", "(" + maxId + ") " + "End of execution");
		nodes.add(finalNode);

		graph.put("nodes", nodes);
		graph.put("links", links);

		return graph.toString();
	}

	private void run(int numButtons, long initial, long difference, int transitionId, int maxIterations,
			String folderName, List<OldState> states, List<String> boundsStates, long after, long currentT, FileWriter out,
			List<Integer> st, List<String> sta, List<Integer> seqTran) throws Exception {

		OldState state = new OldState();
		OldContext context = new OldContext();
		// Loop until there are not more buttons to press, or events, or time is over
		while (numButtons != -1 && maxIterations > 0 && initial > difference) {

			try {

				ArrayList<String> actions = new ArrayList<String>();
				state = new OldState();
				context = new OldContext();

				double cpu = ExternalProcess.showCPUUsage("'" + packageName + "'");
				double memory = ExternalProcess.showMemoryUsage("'" + packageName + "'");
				int battery = ExternalProcess.showBatteryLevel();
				double temperature = ExternalProcess.showBatteryTemperature();
				boolean wifi = ExternalProcess.isWifiEnabled();
				boolean airplane = ExternalProcess.isAirplaneModeOn();

				context.setCpu(cpu);
				context.setMemory(memory);
				context.setBattery(battery);
				context.setTemperature(temperature);
				context.setWifi(wifi);
				context.setAirplaneMode(airplane);

				contexts.add(context);

				Timestamp time = new Timestamp(System.currentTimeMillis());
				String currentTime = time.toString().replaceAll("\\s+", "");
				String destinationRoute = DESTINATION_FOLDER + "/" + folderName + "/" + currentTime + ".xml";

				String remote = null;
				Document nDoc = null;

				try {
					remote = ExternalProcess.takeXMLSnapshot(destinationRoute);
					nDoc = loadXMLFromString(remote);
					String xmlSnapshot = "command: TAKE XML SNAPSHOT; parameters: " + destinationRoute + "; "
							+ new Timestamp(System.currentTimeMillis());
					actions.add(xmlSnapshot);
					out.write(xmlSnapshot + "\n");
					out.flush();
				} catch (Exception e) {
					int attempt = 1;

					while (attempt <= 3) {
						System.out.println("TRYING TO TAKE XML SNAPSHOT. ATTEMPT " + attempt);
						try {
							remote = ExternalProcess.takeXMLSnapshot(destinationRoute);
							nDoc = loadXMLFromString(remote);
							String xmlSnapshot = "command: TAKE XML SNAPSHOT; parameters: " + destinationRoute + "; "
									+ new Timestamp(System.currentTimeMillis());
							actions.add(xmlSnapshot);
							out.write(xmlSnapshot + "\n");
							out.flush();
							break;
						} catch (Exception e1) {
							attempt++;
						}
					}
				}

				String currentFocus = ExternalProcess.getCurrentFocus();
				String focus = "command: GETTING CURRENT FOCUS; parameters: none; "
						+ new Timestamp(System.currentTimeMillis());
				actions.add(focus);
				out.write(focus + "\n");
				out.flush();
				out.write("CURRENT FOCUS: " + currentFocus);
				out.flush();

				String focusedApp = ExternalProcess.getFocusedApp();
				String app = "command: GETTING FOCUSED APP; parameters: none; "
						+ new Timestamp(System.currentTimeMillis());
				actions.add(app);
				out.write(app + "\n");
				out.flush();
				out.write("FOCUSED ACTIVITY: " + focusedApp + "\n");
				out.flush();

				state.setXml(nDoc);
				state.setCurrentFocus(currentFocus);
				state.setFocusedApp(focusedApp);

				boolean cont = false;

				if (states.size() == 0) {
					cont = false;
				} else {
					cont = stateExists(state, states);
				}

				if (!cont) {

					String screencap = "/sdcard/" + currentTime + ".png";
					String screenCapName = currentTime + ".png";
					String local = DESTINATION_FOLDER + "/" + folderName + "/" + screenCapName;
					ExternalProcess.pullScreenshot(screencap, screencap, local);

					String pull = "command: PULL SCREENSHOT; parameters: " + screencap + ", " + screencap + ", " + local
							+ "; " + new Timestamp(System.currentTimeMillis());
					actions.add(pull);
					out.write(pull + "\n");
					out.flush();

					state.setScreenCapture(local);
					state.setImageName(screenCapName);

					NodeList nodes;
					try {
						nodes = nDoc.getElementsByTagName("node");
					} catch (Exception e) {
						nodes = new NodeList() {

							@Override
							public Node item(int index) {
								return null;
							}

							@Override
							public int getLength() {
								return 0;
							}
						};
					}

					ArrayList<Node> cNodes = new ArrayList<Node>();
					ArrayList<Node> sNodes = new ArrayList<Node>();

					for (int i = 0; i < nodes.getLength(); i++) {
						Node current = nodes.item(i);
						NamedNodeMap attributes = current.getAttributes();
						Node attribute = attributes.getNamedItem("clickable");
						Node scroll = attributes.getNamedItem("scrollable");

						// Is clickable
						if (attribute.getNodeValue().equals("true")) {
							System.out.println("-----------------");
							cNodes.add(current);
							for (int j = 0; j < attributes.getLength(); j++) {
								System.out.println(attributes.item(j));

							}
						}

						// Is scrollable
						if (scroll.getNodeValue().equals("true")) {
							System.out.println("-----------------");
							sNodes.add(current);
							for (int j = 0; j < attributes.getLength(); j++) {
								System.out.println(attributes.item(j));

							}
						}

					}

					OldTransition transition = new OldTransition();
					OldNodeT node = new OldNodeT();
					node.setCurrentFocus(currentFocus);
					node.setFocusedApp(focusedApp);
					transition.setId(transitionId);
					transitionId++;

					if (!cNodes.isEmpty()) {

						System.out.println("TAMAÑO ANTES: " + cNodes.size());

						if (!sNodes.isEmpty()) {
							maxIterations = maxIterations - sNodes.size();
							scroll(sNodes, out);

							if (ExternalProcess.isHome()) {
								numButtons = -1;
							}
						}

						if (ExternalProcess.isHome()) {
							numButtons = -1;
						}

						for (Node cnode : cNodes) {

							NamedNodeMap attributes = cnode.getAttributes();
							Node attribute = attributes.getNamedItem("class");

							String field = attributes.getNamedItem("resource-id").getNodeValue();
							String name = attributes.getNamedItem("content-desc").getNodeValue();
							if (name.equals(""))
								name = attributes.getNamedItem("text").getNodeValue();
							OldDomainEntity.Type type = OldDomainEntity.Type.OTHER;
							switch (attribute.getNodeValue()) {
							case "android.widget.CheckBox":
								type = OldDomainEntity.Type.BOOLEAN;
								break;

							case "android.widget.TextView":
							case "android.widget.EditText":
								type = OldDomainEntity.Type.STRING;
								break;

							case "android.widget.Button":
								type = OldDomainEntity.Type.BUTTON;
								break;

							case "android.widget.RadioGroup":
								type = OldDomainEntity.Type.LIST;

							}

							// Special password case
							if (attributes.getNamedItem("password").getNodeValue().equals("true")) {
								type = OldDomainEntity.Type.PASSWORD;
							}

							OldDomainEntity domain = new OldDomainEntity(name, type, field);
							state.addAttribute(domain);

						}

						int tam = cNodes.size();
						ArrayList<Node> oNodes = inputText(cNodes, out);
						int dif = tam - oNodes.size();
						maxIterations = maxIterations - dif;

						if (!oNodes.isEmpty()) {

							// Adding clickables in state
							for (int i = 0; i < oNodes.size(); i++) {
								String field = oNodes.get(i).getAttributes().getNamedItem("resource-id").getNodeValue();
								state.addButton(field);
							}

							Random random = new Random();
							int numNode = random.nextInt((oNodes.size() - 1) + 1);

							if (states.size() == 0) {
								numNode = 0;
							}
							/*
							 * if(states.size()==2) { numNode=0; }
							 */

							String chosenNodeBounds = oNodes.get(numNode).getAttributes().getNamedItem("bounds")
									.getNodeValue();
							String bounds = chosenNodeBounds.replace("][", "/").replace("[", "").replace("]", "");
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

							Random rm = new Random();
							int tapX = rm.nextInt(x2 - x1 + 1) + x1;
							int tapY = rm.nextInt(y2 - y1 + 1) + y1;

							Node temp = cNodes.get(numNode);
							DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
							factory.setNamespaceAware(true);
							DocumentBuilder builder = factory.newDocumentBuilder();
							Document newDocument = builder.newDocument();
							Node importedNode = newDocument.importNode(temp, true);
							newDocument.appendChild(importedNode);

							node.setXml(newDocument);
							transition.setNode(node);

							String n = oNodes.get(numNode).getAttributes().getNamedItem("resource-id").getNodeValue();
							state.addClickedButton(n);
							String tap = "command: TAP; parameters: " + tapX + ", " + tapY + "; "
									+ new Timestamp(System.currentTimeMillis()) + "; id: " + n;
							actions.add(tap);
							out.write(tap + "\n");
							out.flush();
							transition.setActions(actions);

							seqTran.add(transition.getId());

							List<OldTransition> children = new ArrayList<OldTransition>();
							children.add(transition);
							state.setChildren(children);
							states.add(state);

							if (states.get(states.indexOf(state)).getId() == -1) {
								states.get(states.indexOf(state)).setId(states.indexOf(state));
							}

							String d = "/" + String.valueOf(states.indexOf(state));
							String s = "0" + d;
							sta.add(s);
							bounds += d;

							st.add(states.indexOf(state));

							String cT = new Timestamp(System.currentTimeMillis()).toString().replaceAll("\\s+", "");
							String screenCName = cT + ".png";
							String lo = DESTINATION_FOLDER + "/" + folderName + "/" + screenCName;

							String tm = "/!!" + lo;
							bounds += tm;
							boundsStates.add(bounds);

							after = System.currentTimeMillis();
							difference = after - currentT;

							maxIterations--;
							ExternalProcess.tap(String.valueOf(tapX), String.valueOf(tapY));

							if (ExternalProcess.isHome()) {
								numButtons = -1;
							}
						} else {
							// numButtons=-1;
							System.out.println("There are no more buttons");

							String bounds = "0,0/0,0/0";

							seqTran.add(transition.getId());
							List<OldTransition> children = new ArrayList<OldTransition>();
							children.add(transition);

							state.setChildren(children);
							states.add(state);

							if (states.get(states.indexOf(state)).getId() == -1) {
								states.get(states.indexOf(state)).setId(states.indexOf(state));
							}

							String d = "/" + String.valueOf(states.indexOf(state)) + "/0";
							String s = "0" + d;
							sta.add(s);

							bounds += d;
							boundsStates.add(bounds);

							st.add(states.indexOf(state));

							maxIterations--;
							ExternalProcess.goBack();
							String back = "command: GO BACK; parameters: none; "
									+ new Timestamp(System.currentTimeMillis());
							out.write(back + "\n");
							out.flush();
							System.out.println("Going back because there are no more buttons");

							after = System.currentTimeMillis();
							difference = after - currentT;

							if (ExternalProcess.isHome()) {
								numButtons = -1;
							}
						}

					} else {
						// numButtons=-1;
						System.out.println("There are no more buttons");

						String bounds = "0,0/0,0/0";
						seqTran.add(transition.getId());

						List<OldTransition> children = new ArrayList<OldTransition>();
						children.add(transition);
						state.setChildren(children);
						states.add(state);

						if (states.get(states.indexOf(state)).getId() == -1) {
							states.get(states.indexOf(state)).setId(states.indexOf(state));
						}

						String d = "/" + String.valueOf(states.indexOf(state)) + "/0";
						String s = "0" + d;
						sta.add(s);

						bounds += d;
						boundsStates.add(bounds);

						st.add(states.indexOf(state));

						maxIterations--;
						ExternalProcess.goBack();
						String back = "command: GO BACK; parameters: none; "
								+ new Timestamp(System.currentTimeMillis());
						actions.add(back);
						transition.setActions(actions);
						out.write(back + "\n");
						out.flush();
						System.out.println("Going back because there are no more buttons");

						after = System.currentTimeMillis();
						difference = after - currentT;

						if (ExternalProcess.isHome()) {
							numButtons = -1;
						}
					}
				} else {
					String repeated = "THE STATE HAS BEEN VISITED EARLIER";
					out.write(repeated + "\n");
					out.flush();

					System.out.println("CHEKING IF THERE IS A BUTTON THAT HAS NOT BEEN CLICKED");

					state = states.get(index);
					if (state.getId() == -1) {
						state.setId(index);
					}
					int n1 = index;
					st.add(n1);

					if (isAllClicked(state) || state.getButtons().size() == 0) {
						maxIterations--;

						goBack(out);
						if (ExternalProcess.isHome()) {
							numButtons--;
						}
					} else {

						Document xml = state.getXml();
						NodeList nodes = xml.getElementsByTagName("node");
						System.out.println(nodes.getLength());

						List<String> clicked = state.getClickedButtons();

						// Checkable/radio/edit text buttons
						ArrayList<Node> cNodes = new ArrayList<Node>();

						// Scrollabe node list
						ArrayList<Node> sNodes = new ArrayList<Node>();

						// Clickable node list
						ArrayList<Node> oNodes = new ArrayList<Node>();

						Node current, attribute, scroll, button, clickable;
						NamedNodeMap attributes;

						for (int i = 0; i < nodes.getLength(); i++) {
							current = nodes.item(i);
							attributes = current.getAttributes();
							attribute = attributes.getNamedItem("class");
							scroll = attributes.getNamedItem("scrollable");
							button = attributes.getNamedItem("resource-id");
							clickable = attributes.getNamedItem("clickable");

							if (attribute.getNodeValue().equals("android.widget.EditText")
									|| attribute.getNodeValue().equals("android.widget.CheckBox")
									|| attribute.getNodeValue().equals("android.widget.RadioButton")) {
								cNodes.add(current);
							} else if (scroll.getNodeValue().equals("true")) {
								sNodes.add(current);
							} else if (clickable.getNodeValue().equals("true")
									&& !clicked.contains(button.getNodeValue())) {

								oNodes.add(current);
							}
						}

						OldTransition transition = new OldTransition();
						OldNodeT node = new OldNodeT();
						node.setCurrentFocus(currentFocus);
						node.setFocusedApp(focusedApp);
						transition.setId(transitionId);
						transitionId++;

						if (!sNodes.isEmpty()) {
							maxIterations = maxIterations - sNodes.size();
							scroll(sNodes, out);

							if (ExternalProcess.isHome()) {
								numButtons = -1;
							}
						}

						if (ExternalProcess.isHome()) {
							numButtons = -1;
						}

						int tam = cNodes.size();
						inputText(cNodes, out);
						maxIterations = maxIterations - tam;

						Random random = new Random();

						if (oNodes.size() > 0) {
							int numNode = random.nextInt((oNodes.size() - 1) + 1);

							String chosenNodeBounds = oNodes.get(numNode).getAttributes().getNamedItem("bounds")
									.getNodeValue();
							String bounds = chosenNodeBounds.replace("][", "/").replace("[", "").replace("]", "");
							int tr = state.getChildren().size();
							String trN = "/" + String.valueOf(tr) + "/" + String.valueOf(index);
							bounds += trN;

							String d = "/" + String.valueOf(index);
							String s = tr + d;
							sta.add(s);

							String cT = new Timestamp(System.currentTimeMillis()).toString().replaceAll("\\s+", "");
							String screenCName = cT + ".png";
							String lo = DESTINATION_FOLDER + "/" + folderName + "/" + screenCName;

							String tm = "/!!" + lo;
							bounds += tm;

							boundsStates.add(bounds);

							String[] coords = bounds.split("/");
							String coord1 = coords[0];
							String coord2 = coords[1];
							String[] points1 = coord1.split(",");
							String[] points2 = coord2.split(",");

							int x1 = Integer.parseInt(points1[0]);
							int x2 = Integer.parseInt(points2[0]);

							int y1 = Integer.parseInt(points1[1]);
							int y2 = Integer.parseInt(points2[1]);

							Random rm = new Random();
							int tapX = rm.nextInt(x2 - x1 + 1) + x1;
							int tapY = rm.nextInt(y2 - y1 + 1) + y1;

							Node temp = oNodes.get(numNode);
							DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
							factory.setNamespaceAware(true);
							DocumentBuilder builder = factory.newDocumentBuilder();
							Document newDocument = builder.newDocument();
							Node importedNode = newDocument.importNode(temp, true);
							newDocument.appendChild(importedNode);

							node.setXml(newDocument);
							transition.setNode(node);

							String n = oNodes.get(numNode).getAttributes().getNamedItem("resource-id").getNodeValue();
							String tap = "command: TAP; parameters: " + tapX + ", " + tapY + "; "
									+ new Timestamp(System.currentTimeMillis()) + "; id: " + n;
							actions.add(tap);
							out.write(tap + "\n");
							out.flush();
							transition.setActions(actions);

							seqTran.add(transition.getId());

							state.addTransition(transition);
							state.addClickedButton(n);

							after = System.currentTimeMillis();
							difference = after - currentT;

							maxIterations--;
							ExternalProcess.tap(String.valueOf(tapX), String.valueOf(tapY));

							if (ExternalProcess.isHome()) {
								numButtons = -1;
							}
						} else {

							maxIterations--;
							ExternalProcess.goBack();
							String back = "command: GO BACK; parameters: none; "
									+ new Timestamp(System.currentTimeMillis());
							out.write(back + "\n");
							out.flush();
							if (ExternalProcess.isHome()) {
								numButtons = -1;
							}
						}
					}
					states.set(index, state);
				}

				after = System.currentTimeMillis();
				difference = after - currentT;
			} catch (Exception e) {

				System.out.println("ERROR DURING EXECUTION");
				System.out.println("Message: " + e.getMessage());
				e.printStackTrace();

				if (e.getMessage().contains("Error")) {

					String focus = "Crash after state: " + state.getId();
					state = new OldState();
					state.setId(states.size());

					context = new OldContext();
					double cpu = ExternalProcess.showCPUUsage("'" + packageName + "'");
					double memory = ExternalProcess.showMemoryUsage("'" + packageName + "'");
					int battery = ExternalProcess.showBatteryLevel();
					double temperature = ExternalProcess.showBatteryTemperature();
					boolean wifi = ExternalProcess.isWifiEnabled();
					boolean airplane = ExternalProcess.isAirplaneModeOn();

					context.setCpu(cpu);
					context.setMemory(memory);
					context.setBattery(battery);
					context.setTemperature(temperature);
					context.setWifi(wifi);
					context.setAirplaneMode(airplane);

					contexts.add(context);

					Thread.sleep(1000);

					Timestamp time = new Timestamp(System.currentTimeMillis());
					String currentTime = time.toString().replaceAll("\\s+", "");

					String screencap = "/sdcard/" + currentTime + ".png";
					String screenCapName = currentTime + ".png";
					String local = DESTINATION_FOLDER + "/" + folderName + "/" + screenCapName;
					ExternalProcess.pullScreenshot(screencap, screencap, local);

					state.setScreenCapture(local);
					state.setImageName(screenCapName);
					state.setCurrentFocus(focus);
					state.setFocusedApp(focus);
					// Problema índice negativo
					state.setId(states.size());

					OldTransition transition = new OldTransition();
					OldNodeT node = new OldNodeT();
					transition.setId(transitionId);
					transitionId++;
					transition.setNode(node);

					List<String> actions = new ArrayList<>();

					String crash = "APP CRASHED: " + e.getMessage() + "; " + new Timestamp(System.currentTimeMillis());
					out.write(crash + "\n");
					out.flush();
					maxIterations--;
					ExternalProcess.clearData(packageName);
					String clear = "command: CLEAR DATA; parameters:" + packageName + "; "
							+ new Timestamp(System.currentTimeMillis());
					actions.add(clear);
					out.write(clear + "\n");
					out.flush();

					String activityName = packageName + "/" + mainActivity;
					maxIterations--;
					ExternalProcess.startActivity(activityName);
					String start = "command: START APP; parameters:" + activityName + "; "
							+ new Timestamp(System.currentTimeMillis());
					actions.add(start);
					out.write(start + "\n");
					out.flush();

					transition.setActions(actions);
					seqTran.add(transition.getId());

					List<OldTransition> children = new ArrayList<OldTransition>();
					children.add(transition);
					state.setChildren(children);
					states.add(state);
					st.add(states.indexOf(state));

					String d = "/" + String.valueOf(states.indexOf(state)) + "/0";
					String s = "0" + d;
					sta.add(s);

					String bounds = "0,0/0,0/0";
					bounds += d;
					boundsStates.add(bounds);

					after = System.currentTimeMillis();
					difference = after - currentT;
				} else {
					throw new Exception(e);
				}
			}
		}
		out.write("END OF EXECUTION");
		out.flush();
		Timestamp fin = new Timestamp(System.currentTimeMillis());
		meta.write("FINISH DATE: " + fin.toString() + "\n");
		metaInfo.put("finishDate", fin.toString());
		meta.flush();
		System.out.println("End of execution");
		System.out.println("Starting WEB Visualization tool...");

	}

	private void goBack(FileWriter out) throws Exception {

		ExternalProcess.goBack();
		String back = "command: GO BACK; parameters: none; " + new Timestamp(System.currentTimeMillis());
		out.write(back + "\n");
		out.flush();

	}

	/**
	 * Indicates if the current state has been visited
	 * 
	 * @param current
	 * @param states
	 * @return
	 */
	private boolean stateExists(OldState current, List<OldState> states) {

		// State in i
		OldState cur;
		// Current focus of cur
		String cfCur;
		// Focused app of cur
		String faCur;
		// XML of cur
		Document xmlCur;
		// Nodes of cur
		NodeList nodesCur;

		// Current focus of current state (the one that we are checking)
		String currentFocus = current.getCurrentFocus();
		// Focused app of current state (the one that we are checking)
		String focusedApp = current.getFocusedApp();
		// XML of current state (the one that we are checking)
		Document xml = current.getXml();
		// Nodes of current state
		NodeList nodes = xml.getElementsByTagName("node");

		boolean end = false;
		boolean answer = false;

		for (int i = 0; i < states.size() && !end; i++) {

			cur = states.get(i);
			cfCur = cur.getCurrentFocus();
			faCur = cur.getFocusedApp();

			if (cur.getXml() != null) {
				xmlCur = cur.getXml();

				NodeList nodesCur1 = xml.getElementsByTagName("node");
				Node current1;

				NodeList nodesCur2 = xmlCur.getElementsByTagName("node");
				Node current2;

				for (int k = 0; k < nodesCur1.getLength(); k++) {
					current1 = nodesCur1.item(k);
					if (current1.getAttributes().getNamedItem("NAF") != null) {
						current1.getAttributes().removeNamedItem("NAF");
					}
				}

				for (int l = 0; l < nodesCur2.getLength(); l++) {
					current2 = nodesCur2.item(l);
					if (current2.getAttributes().getNamedItem("NAF") != null) {
						current2.getAttributes().removeNamedItem("NAF");
					}
				}

				nodesCur = xmlCur.getElementsByTagName("node");

				if (cfCur.equals(currentFocus) && faCur.equals(focusedApp) && !cfCur.equals("") && !faCur.equals("")) {

					if (nodesCur.getLength() == nodes.getLength()) {

						// if one node is different, loop ends
						boolean end2 = false;
						int counter = 0;
						// Checks if xmls are equal
						for (int j = 0; j < nodesCur.getLength() && !end2; j++) {
							Node a = nodesCur.item(j);
							Node b = nodes.item(j);
							//
							// String textA = a.getAttributes().getNamedItem("text").getTextContent();
							// String textB = b.getAttributes().getNamedItem("text").getTextContent();
							//
							// System.out.println("TEXTO DE A: "+textA);
							// System.out.println("TEXTO DE B: "+textB);
							//
							// System.out.println("con el value A:
							// "+a.getAttributes().getNamedItem("text").getNodeValue());
							// System.out.println("con el value B:
							// "+b.getAttributes().getNamedItem("text").getNodeValue());
							//
							// a.getAttributes().getNamedItem("text").setTextContent("a");
							// b.getAttributes().getNamedItem("text").setTextContent("a");

							if (!a.isEqualNode(b)) {
								end2 = true;
							} else {
								counter++;
							}
							// a.getAttributes().getNamedItem("text").setTextContent(textA);
							// b.getAttributes().getNamedItem("text").setTextContent(textB);
						}

						if (counter == nodesCur.getLength()) {
							end = true;
							answer = true;
							index = i;
						}
					}
				}
			}
		}
		return answer;
	}

	/**
	 * Checks if it there are buttons to tap
	 *
	 * @param state
	 *            is the current state
	 * @return true if there are no more buttons to tap, false if there are buttons
	 */
	public static boolean isAllClicked(OldState state) {

		if (state.getButtons().size() == state.getClickedButtons().size()) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		System.out.println("\n 2018, Universidad de los Andes\n The software design lab\n");

		String s = String.join("\n", "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥", "🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥",
				"🔥🔥     🔥🔥  🔥🔥  🔥🔥     🔥🔥", "🔥🔥🔥🔥🔥🔥   🔥🔥  🔥🔥🔥🔥🔥🔥 ",
				"🔥🔥   🔥🔥    🔥🔥  🔥🔥          ", "🔥🔥    🔥🔥   🔥🔥  🔥🔥          ",
				"🔥🔥     🔥🔥  🔥🔥  🔥🔥          ", " ");

		System.out.println(s);

		/*
		 * try {
		 *
		 * ExternalProcess.getServices("'com.ppg.spunky_java'");
		 *
		 * } catch (Exception e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */

		new Rip();
	}
}
