package helper;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import main.RipException;

public class EmulatorHelper {
	
	
	/**
	 * Installs an apk file in a device.
	 * 
	 * @param pathAPK
	 *            is where apk is stored
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws Exception
	 *             if the app is already installed
	 */
	public static boolean installAPK(String pathAPK) throws IOException, InterruptedException {
		
		isActionIdle();
		try {
			List<String> commands = Arrays.asList("adb", "install", "-r", pathAPK);
			ExternalProcess2.executeProcess(commands, "INSTALLING APK", "Installation complete", "App could not be installed");
			Helper.logMessage("INSTALL APP",  pathAPK , null);
			return true;

		} catch (Exception e) {
			Helper.logMessage("APP ALREADY INSTALLED APP", pathAPK, e.getMessage());
			return false;
		}
	}
	
	/**
	 * Keeps the device's screen unlocked
	 */
	public static void keepUnlock() {
		try {
			List<String> commands = Arrays.asList("adb", "shell", "svc", "power", "stayon", "true");
			ExternalProcess2.executeProcess(commands, "KEEP UNLOCKED", null, null);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Verifies that ADB is currently in the system path
	 * 
	 * @return actual ADB version
	 * @throws Exception
	 *             If ADB is not found in the system path
	 */
	public static String verifyADB() throws Exception {
		List<String> commands = Arrays.asList("adb", "version");
		List<String> result = ExternalProcess2.executeProcess(commands, "CHECK ADB INSTALLATION", "Verification complete",
				"ADB is not in your path");
		String output = result.get(0);
		String[] success = output.split("\n");
		String[] version = success[0].split(" ");
		return version[4];
	}
	
	/**
	 * Uninstalls an apk with its package
	 * 
	 * @param packageName
	 *            is the name of the package that contains the app
	 * @throws Exception
	 *             if there is no package with that name
	 */
	public static void uninstallAPK(String packageName) throws Exception {

		List<String> commands = Arrays.asList("adb", "uninstall", packageName);
		ExternalProcess2.executeProcess(commands, "UNINSTALL APK", "Uninstall complete", "APK could not be uninstalled");
		System.out.println("ðŸ—‘ï¸�ï¸� UNINSTALL COMPLETE");

	}
	
	/**
	 * Starts a remote shell and calls the activity manager (am) to launch a
	 * specific activity
	 * 
	 * @param activity
	 *            is the path of the activity inside package
	 * @throws RipException
	 * @throws IOException
	 * @throws SecurityException
	 *             if permission is denied
	 * @throws Exception
	 *             if activity does not exist or warning if activity is a current
	 *             task
	 */
	public static void startActivity(String packageName, String activity) throws IOException, RipException {

		List<String> commands = Arrays.asList("adb", "shell", "am", "start", "-n", packageName + "/" + activity);
		ExternalProcess2.executeProcess(commands, "START ACTIVITY", "Activity launched", "Activity could not be started");
	}
	
	/**
	 * Stops all the processes related to the specified package. It does not
	 * uninstall
	 * 
	 * @param packageName
	 *            is the name of the package that contains the app
	 * @throws Exception
	 *             if something in the process fails, like app does not exist
	 */
	public static void stopApp(String packageName) throws Exception {

		List<String> commands = Arrays.asList("adb", "shell", "am", "force-stop", packageName);
		ExternalProcess2.executeProcess(commands, "STOP APPLICATION", "Application stopped", "Application could not be stopped");

	}
	
	/**
	 * Clears all data related to the specified package due to package manager(pm).
	 * This command will stop the app too.
	 * 
	 * @param packageName
	 *            is the name of the package that contains the app
	 * @throws Exception
	 *             if something in the process fails, like app does not exist
	 */
	public static void clearData(String packageName) throws Exception {

		List<String> commands = Arrays.asList("adb", "shell", "pm", "clear", packageName);
		ExternalProcess2.executeProcess(commands, "CLEAR DATA", "Data cleared", "Data could not be cleared");

	}
	
	/**
	 * Takes a screenshot of the actual device's screen.
	 * 
	 * @param Filename
	 *            is the path where the file will be saved.
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             if filename is wrong, not valid directory
	 */
	public static String takeAndPullScreenshot(String id, String folderName) throws IOException, RipException {

		String screenCapName = id+".png";
		String filename = "/sdcard/"+screenCapName;
		String local = folderName + File.separator + screenCapName;
		List<String> commands = Arrays.asList("adb", "shell", "screencap", filename);
		ExternalProcess2.executeProcess(commands, "TAKE SCREENSHOT", "Screenshot saved at: " + filename, "Screenshot was not captured");
		pullFile(filename, local);
		return local;
	}

	/**
	 * Extracts a file from device or emulator (remote) to computer (local).
	 * 
	 * @param RemoteFilePath
	 *            must be replaced with the path of the file in the device
	 * @param LocalFilePath
	 *            is the path where the file will be stored in the computer.
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             if any path is wrong
	 */
	public static void pullFile(String remotePath, String localPath) throws IOException, RipException {

		String remotePathEx = "/sdcard/sreen.png";
		String localPathEx = "screenPull.png";

		if (remotePath.equals("") || localPath.equals("")) {
			remotePath = remotePathEx;
			localPath = localPathEx;
		}
		List<String> commands = Arrays.asList("adb", "pull", remotePath, localPath);
		ExternalProcess2.executeProcess(commands, "PULL FILE", "File saved on PC", "File could not be pulled");
	}

//	/**
//	 * Takes a screenshot and copies it in computer. Calls methods takeScreenshot
//	 * and pullFile .
//	 * 
//	 * @param RemoteFilePath
//	 *            must be replaced with the path of the file in the device.
//	 * @param LocalFilePath
//	 *            is the path where the file will be stored in the computer.
//	 * @param Filename
//	 *            is the path where the file will be saved.
//	 * @throws RipException 
//	 * @throws IOException 
//	 * @throws Exception
//	 *             if filename is wrong, not valid directory or any path is wrong
//	 */
//	public static void pullScreenshot(String filename, String remotePath, String localPath) throws IOException, RipException {
//
//		String filenameEx = "/sdcard/screen2.png";
//		String remotePathEx = "/sdcard/screen2.png";
//		String localPathEx = "screenPull2.png ";
//
//		if (remotePath.equals("") || localPath.equals("") || filename.equals("")) {
//			remotePath = remotePathEx;
//			localPath = localPathEx;
//			filename = filenameEx;
//		}
//		takeScreenshot(filename);
//		pullFile(remotePath, localPath);
//	}

	/**
	 * Calls the window manager (wm) and gets the density/resolution of the screen
	 * 
	 * @return int Device density
	 */
	public static int getDeviceResolution() {

		int ans = 0;
		try {
			List<String> commands = Arrays.asList("adb", "shell", "wm", "density");
			List<String> results = ExternalProcess2.executeProcess(commands, "GET DEVICE RESOLUTION", null, null);
			String output = results.get(0);

			String[] result = output.split(" ");
			char[] temp = result[2].toCharArray();
			char[] fin = new char[temp.length - 1];

			for (int i = 0; i < temp.length - 1; i++) {
				fin[i] = temp[i];
			}
			String def = new String(fin);
			// Output is "Physical density: 320", but we only need the number
			ans = Integer.parseInt(def);
			System.out.println(ans);

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return ans;
	}
	
	public static void isEventIdle() throws IOException, InterruptedException {
		// "adb","shell","dumpsys","window","-a","|","grep","mAppTransitionState"
		ProcessBuilder pBB = new ProcessBuilder(new String[]{"adb","shell","dumpsys","window","-a","|","grep","mAppTransitionState"});
		Process pss;
		boolean termino = false;
		boolean running = false;
		System.out.println("waiting for emulator's event idle state");
		while (!running || !termino) {
			pss = pBB.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(pss.getInputStream()));
			String line;
			String resp = "";
			while ((line = reader.readLine())!=null) {
				resp += line;
			}
			pss.waitFor();
			
			if(running && resp.contains("IDLE")) {
				termino = true;
				Thread.sleep(200);
				System.out.println("Emulator now is in event idle state");
			} else if (resp.contains("RUNNING")){
				running = true;
				System.out.println("Emulator now is in running");
			} 
//			else {
//				Thread.sleep(200);
//			}
		}
	}
	
	public static void isActionIdle() throws IOException, InterruptedException {
		// "adb","shell","dumpsys","window","-a","|","grep","mAppTransitionState"
		ProcessBuilder pBB = new ProcessBuilder(new String[]{"adb","shell","dumpsys","window","-a","|","grep","mAppTransitionState"});
		Process pss;
		boolean termino = false;
		boolean running = false;
		System.out.println("waiting for emulator's event idle state");
		while (!termino) {
			pss = pBB.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(pss.getInputStream()));
			String line;
			String resp = "";
			while ((line = reader.readLine())!=null) {
				resp += line;
			}
			pss.waitFor();
			
			if(resp.contains("IDLE")) {
				termino = true;
				Thread.sleep(200);
				System.out.println("Emulator now is in action idle state");
			}
//			else {
//				Thread.sleep(200);
//			}
		}
	}

	/**
	 * Gets the current orientation of the accelerometer
	 * 
	 * @return 0 if it's current orientation is portrait, 1 if landscape
	 */
	public static int getCurrentOrientation() {

		int answer = 0;
		try {
			List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "input", "|", "grep",
					"'SurfaceOrientation'");
			List<String> results = ExternalProcess2.executeProcess(commands, "GET CURRENT ORIENTATION", null, null);
			String output = results.get(0);
			String[] ans = output.split(":");
			ans[1].replaceAll("\\s+", "");

			answer = Integer.parseInt(ans[1]);

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return answer;
	}

	/**
	 * Simulates a tap in the screen.
	 * 
	 * @param CoordX
	 *            X coordinate
	 * @param Coordy
	 *            Y coordinate
	 * @throws RipException
	 * @throws IOException
	 */
	public static void tap(String coordX, String coordY) throws IOException, RipException {

		List<String> commands = Arrays.asList("adb", "shell", "input", "tap", coordX, coordY);
		ExternalProcess2.executeProcess(commands, "TAP", null, null);

	}

	/**
	 * Simulates a long tap in the screen.
	 * 
	 * @param CoordX
	 *            and CoordY are the coordinates of the touch and milliseconds, the
	 *            time of it.
	 */
	public static void longTap(String coordX, String coordY, String ms) throws Exception {

		String cX = "168";
		String cY = "680";
		String milli = "1000";

		if (coordX.equals("") || coordY.equals("") || ms.equals("")) {
			coordX = cX;
			coordY = cY;
			ms = milli;
		}

		List<String> commands = Arrays.asList("adb", "shell", "input", "swipe", coordX, coordY, coordX, coordY, ms);
		ExternalProcess2.executeProcess(commands, "LONG TAP", null, null);
	}

	/**
	 * Simulates a scroll
	 * 
	 * @param coordX1
	 *            and coordY1 are the source coordinate
	 * @param coordX2
	 *            and coordY2 destination coordinate
	 * @param Milliseconds,
	 *            time of the touch.
	 */
	public static void scroll(String coordX1, String coordY1, String coordX2, String coordY2, String ms)
			throws Exception {

		String cX1 = "168";
		String cY1 = "680";
		String cX2 = "200";
		String cY2 = "700";
		String milli = "1000";

		if (coordX1.equals("") || coordY1.equals("") || ms.equals("") || coordX2.equals("") || coordY2.equals("")) {
			coordX1 = cX1;
			coordY1 = cY1;
			coordX2 = cX2;
			coordY2 = cY2;
			ms = milli;
		}

		List<String> commands = Arrays.asList("adb", "shell", "input", "swipe", coordX1, coordY1, coordX2, coordY2, ms);
		ExternalProcess2.executeProcess(commands, "SCROLL", null, null);

	}

	/**
	 * Simulates the effect of touching back soft button, return to the last
	 * activity. 4 is KEYCODE_BACK
	 * @throws RipException 
	 * @throws IOException 
	 */
	public static void goBack() throws IOException, RipException {

		List<String> commands = Arrays.asList("adb", "shell", "input", "keyevent", "4");
		ExternalProcess2.executeProcess(commands, "GO BACK", null, null);

	}

	/**
	 * Simulates the effect of touching home soft button, return home. 3 is
	 * KEYCODE_HOME
	 * @throws RipException 
	 * @throws IOException 
	 */
	public static void goHome() throws IOException, RipException {

		List<String> commands = Arrays.asList("adb", "shell", "input", "keyevent", "3");
		ExternalProcess2.executeProcess(commands, "GO HOME", null, null);

	}

	/**
	 * Simulates the effect of touching recents soft button, shows recent apps. 187
	 * is KEYCODE_APP_SWITCH
	 */
	public static void showRecents() {

		try {
			System.out.println("ðŸ“±ï¸� RECENTS \n [adb shell input keyevent 187] \n ");
			List<String> commands = Arrays.asList("adb", "shell", "input", "keyevent", "187");
			ExternalProcess2.executeProcess(commands, "RECENT", null, null);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Simulates the effect of pressing the power button. 26 is KEYCODE_POWER
	 */
	public static void turnOnScreen() throws Exception {

		List<String> commands = Arrays.asList("adb", "shell", "input", "keyevent", "26");
		ExternalProcess2.executeProcess(commands, "TURN ON/OFF SCREEN", null, null);

	}

	/**
	 * Lists available avds
	 * 
	 * @throws Exception
	 *             if cannot list emulators.
	 */
	public static void getEmulators() throws Exception {

		System.out.println("ðŸ“±ï¸� LISTING EMULATORS \n [emulator -list-avds] \n ");
		List<String> commands = Arrays.asList("~/Library/Android/sdk/tools/emulator", "-list-avds");
		ExternalProcess2.executeProcess(commands, "LIST EMULATORS", null, null);
	}

	/**
	 * Launch an existing emulator on the list
	 * 
	 * @param emulator
	 *            Name of the emulator to launch
	 * @throws Exception
	 *             if the emulator does not exist
	 */
	public static void launchEmulator(String emulator) throws Exception {

		if (emulator.equals("")) {
			emulator = "Nexus_5_API_27";
		}
		List<String> commands = Arrays.asList("~/Library/Android/sdk/tools/emulator", "-avd", emulator, "-netdelay",
				"none", "-netspeed", "full");
		ExternalProcess2.executeProcess(commands, "LAUNCH EMULATOR", null, null);
	}

	/**
	 * Shows logcat and stops.
	 * 
	 * @throws Exception
	 *             if something goes wrong
	 */
	public static void showLogcat() throws Exception {

		List<String> commands = Arrays.asList("adb", "shell", "logcat", "-d");
		ExternalProcess2.executeProcess(commands, "GET LOGCAT", null, null);

	}

	/**
	 * Cleans logcat.
	 * 
	 * @throws Exception
	 *             if something goes wrong
	 */
	public static void clearLogcat() throws Exception {

		List<String> commands = Arrays.asList("adb", "shell", "logcat", "-c");
		ExternalProcess2.executeProcess(commands, "CLEAR LOGCAT", "LOGCAT Cleared", "Error clearing LOGCAT");

	}

	/**
	 * Gets logcat and saves it in a file, then, pulled it into pc
	 * 
	 * @param path
	 *            place where logcat will be saved inside phone
	 * @param localPath
	 *            place where logcat will be saved in pc
	 * @throws Exception
	 *             if file cannot be created or pulled
	 */
	public static void saveLogcat(String path, String localPath) throws Exception {
		if (path.equals("") ) {
			path = "/sdcard/outputLogcat.txt";
		}
		if(localPath.equals("")){
			localPath = "out.txt";
		}
		createFile(path);
		List<String> commands = Arrays.asList("adb", "shell", "logcat", "-d", ">", path);
		ExternalProcess2.executeProcess(commands, "SAVE LOGCAT", null, null);
		pullFile(path, localPath);
	}

	public static void saveLogcatErrors(String path, String localPath) throws Exception{
		if (path.equals("") ) {
			path = "/sdcard/outputLogcat.txt";
		}
		if(localPath.equals("")){
			localPath = "outError.txt";
		}
		createFile(path);
			List<String> commands = Arrays.asList("adb", "shell", "logcat", "-s", "System.err", "-d", ">", path);
		ExternalProcess2.executeProcess(commands, "SAVE LOGCAT ERRORS", null, null);
		pullFile(path, localPath);
	}

	/**
	 * Creates a file to save the logcat later
	 * 
	 * @param path
	 *            the file path where you want to create the file
	 * @throws Exception
	 *             if permission is denied
	 */
	public static void createFile(String path) throws Exception {
		if (path.equals("")) {
			path = "/sdcard/outputLogcat.txt";
		}
		List<String> commands = Arrays.asList("adb", "shell", "touch", path);
		ExternalProcess2.executeProcess(commands, "CREATE A FILE", "File created at: " + path, "File could not be created");
	}

	/**
	 * Turns off automatic rotation
	 * 
	 * @throws RipException
	 * @throws IOException
	 */
	public static void turnOffRotation() throws IOException, RipException {

		List<String> commands = Arrays.asList("adb", "shell", "content", "insert", "--uri", "content://settings/system",
				"--bind", "name:s:accelerometer_rotation", "--bind", "value:i:0");
		ExternalProcess2.executeProcess(commands, "TURN OFF AUTOMATIC ROTATION", null, null);
	}

	/**
	 * Rotates to landscape. It is necessary turn off automatic rotation.
	 * 
	 * @throws RipException
	 * @throws IOException
	 */
	public static void rotateLandscape() throws IOException, RipException {

		turnOffRotation();
		List<String> commands = Arrays.asList("adb", "shell", "content", "insert", "--uri", "content://settings/system",
				"--bind", "name:s:user_rotation", "--bind", "value:i:1");
		ExternalProcess2.executeProcess(commands, "ROTATE TO LANDSCAPE", null, null);
	}

	/**
	 * Rotates to portrait. It is necessary turn off automatic rotation.
	 * 
	 * @throws RipException
	 * @throws IOException
	 */
	public static void rotatePortrait() throws IOException, RipException {

		turnOffRotation();
		List<String> commands = Arrays.asList("adb", "shell", "content", "insert", "--uri", "content://settings/system",
				"--bind", "name:s:user_rotation", "--bind", "value:i:0");
		ExternalProcess2.executeProcess(commands, "ROTATE TO PORTRAIT", null, null);
	}

	/**
	 * Open keyboard, the one that has letters
	 */
	public static void showKeyboard() {

		try {
			List<String> commands = Arrays.asList("adb", "shell", "input", "keyevent", "78");
			ExternalProcess2.executeProcess(commands, "SHOW KEYBOARD", null, null);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Open a keyboard, the one that has numbers
	 */
	public static void showNumKeyboard() throws Exception {

		List<String> commands = Arrays.asList("adb", "shell", "input", "keyevent", "58");
		ExternalProcess2.executeProcess(commands, "SHOW NUMERIC KEYBOARD", null, null);

	}

	/**
	 * Writes text in a text field
	 * 
	 * @param input
	 *            is the string that the users wants to write
	 */
	public static void enterInput(String input) throws RipException, IOException {
		String newString = input.replace(" ", "%s");
		List<String> commands = Arrays.asList("adb", "shell", "input", "text", newString);
		ExternalProcess2.executeProcess(commands, "INPUT TEXT", null, null);

	}

	/**
	 * Takes a XML snapshot of the current activity
	 * 
	 * @param destinationRoute
	 *            File destination route. If destinationRoute == null, the file
	 *            won't be saved
	 * @return XML file in String format
	 */
	public static String takeAndPullXMLSnapshot(String id, String folderName) throws Exception {

		List<String> commands = Arrays.asList("adb", "shell", "uiautomator", "dump");
		List<String> answer = ExternalProcess2.executeProcess(commands, "TAKE XML SNAPSHOT", null, null);
		String[] temp = answer.get(0).split("\n");
		String route = temp[0].split("UI hierchary dumped to: ")[1].replaceAll("(\\r)", "");
		
		String screenCapName = id+".xml";
		String local = folderName + File.separator + screenCapName;
		pullFile(route, local);

		return local;

	}

	public static String getCurrentViewHierarchy() throws IOException, RipException {
		List<String> commands = Arrays.asList("adb", "shell", "uiautomator", "dump");
		List<String> answer = ExternalProcess2.executeProcess(commands, "TAKE XML SNAPSHOT", null, null);
		String[] temp = answer.get(0).split("\n");
		String route = temp[0].split("UI hierchary dumped to: ")[1].replaceAll("(\\r)", "");
		List<String> readCommands = Arrays.asList("adb", "shell", "cat", route);
		List<String> response = ExternalProcess2.executeProcess(readCommands, "READ XML SNAPSHOT", null, null);
		return response.get(0);
	}

	/**
	 * Reads a remote file and returns its content.
	 * 
	 * @param remoteRoute
	 *            Remote file route.
	 * @return File's content
	 */
	public static String readRemoteFile(String remoteRoute) throws Exception {
		List<String> commands = Arrays.asList("adb", "shell", "cat", remoteRoute);
		List<String> answer = ExternalProcess2.executeProcess(commands, "READ REMOTE FILE", null, null);
		return answer.get(0);
	}

	/**
	 * Shows history of CPU usage with the name of the packages and the
	 * corresponding percentage
	 * 
	 * @param packageName
	 *            is the name of the package that we want to know cpu usage
	 * @return usage % of the package
	 * @throws Exception
	 *             if there is no device or emulator or if the package does not
	 *             exist
	 */
	public static double showCPUUsage(String packageName) throws Exception {

		double ans = 0.0;
		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "cpuinfo", "|", "grep", packageName);
		List<String> answer = ExternalProcess2.executeProcess(commands, "SHOW CPU USAGE", null, null);

		if (!answer.get(0).isEmpty()) {
			String cpu = answer.get(0).split("%")[0].replaceAll("\\s+", "");
			if (cpu.contains("\\+")) {
				cpu.replaceAll("\\+", "");
			}
			ans = Double.parseDouble(cpu);
		}

		System.out.println("CPU: " + ans);

		return ans;
	}

	/**
	 * Shows history of memory usage with the name of the packages and the
	 * corresponding percentage
	 * 
	 * @param packageName
	 *            is the name of the package that we want to know memory usage
	 * @return Memory usage in K.
	 * @throws Exception
	 *             if there is no device or emulator or if the package does not
	 *             exist
	 */
	public static double showMemoryUsage(String packageName) throws Exception {

		double ans = 0.0;
		if (packageName.equals("")) {
			packageName = "com.example.lanabeji.dailyexpenses";
		}
		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "meminfo", "|", "grep", packageName);
		List<String> answer = ExternalProcess2.executeProcess(commands, "SHOW MEMORY USAGE", null, null);
		String[] list = answer.get(0).split("\n");

		if (list.length != 0) {
			String temp = list[0].toLowerCase().split(":")[0].replaceAll("k", "").replaceAll("b", "")
					.replaceAll("\\s+", "").replaceAll(",", "");
			double mem = Double.parseDouble(temp);
			ans = mem;

			System.out.println("MEM: " + ans);

		}
		return ans;
	}

	/**
	 * Returns the battery level of device or emulator
	 * 
	 * @return Battery level percentage
	 * @throws Exception
	 *             if there is no device or emulator
	 */
	public static int showBatteryLevel() throws Exception {

		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "battery", "|", "grep", "level");
		List<String> answer = ExternalProcess2.executeProcess(commands, "SHOW BATTERY LEVEL", null, null);
		String list = answer.get(0);

		String level = list.split(":")[1].replaceAll("\\s+", "");
		int battery;

		try {
			battery = Integer.parseInt(level);
		} catch (Exception e) {
			battery = 101;
			System.out.println("ERROR PARSING BATTERY LEVEL: " + level);
		}
		return battery;
	}

	/**
	 * Shows the battery temperature
	 * 
	 * @return temperature of the battery in ÂªC
	 * @throws Exception
	 *             is there i no device or emulator
	 */
	public static double showBatteryTemperature() throws Exception {

		double ans = 0;
		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "battery", "|", "grep", "temperature");
		List<String> answer = ExternalProcess2.executeProcess(commands, "SHOW BATTERY TEMPERATURE", null, null);
		String list = answer.get(0);

		String temp = list.split(":")[1].replaceAll("\\s+", "");
		double temperature = Double.parseDouble(temp);
		ans = temperature / 10;

		System.out.println("TEM: " + ans);

		return ans;
	}

	/**
	 * Indicates if wifi is enabled in device or emulator
	 * 
	 * @return true if wifi is enable, false if is disabled
	 * @throws Exception
	 *             if something goes wrong in executeProcess
	 */
	public static boolean isWifiEnabled() throws Exception {

		boolean ans = false;

		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "wifi", "|", "grep", "'Wi-Fi is'");
		List<String> answer = ExternalProcess2.executeProcess(commands, "CHEKING IF WIFI IS ENABLED", null, null);
		String[] list = answer.get(0).split("\n");
		if (list[0].startsWith("Wi-Fi is enabled")) {
			ans = true;
		} else {
			ans = false;
		}

		System.out.println("WIFI: " + ans);

		return ans;
	}

	/**
	 * Indicates is screen is on
	 * 
	 * @return true if screen is on, false is screen is off
	 * @throws Exception
	 *             if there is no device or emulator
	 */
	public static boolean isScreenOn() throws Exception {
		boolean ans = false;

		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "wifi", "|", "grep", "'mScreenOff'");
		List<String> answer = ExternalProcess2.executeProcess(commands, "CHEKING IF SCREEN IS ON", null, null);
		String list = answer.get(0);
		if (list.split(" ")[1].contains("true")) {
			ans = false;
		} else {
			ans = true;
		}
		System.out.println(ans);
		return ans;
	}

	/**
	 * Indicates is airplane mode is on
	 * 
	 * @return true if airplane mode is on, false is airplane mode is off
	 * @throws Exception
	 *             if there is no device or emulator
	 */
	public static boolean isAirplaneModeOn() throws Exception {
		boolean ans = false;

		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "wifi", "|", "grep", "'mAirplaneModeOn'");
		List<String> answer = ExternalProcess2.executeProcess(commands, "CHEKING IF AIRPLANE MODE IS ON", null, null);
		String list = answer.get(0);
		if (list.split(" ")[1].contains("true")) {
			ans = true;
		} else {
			ans = false;
		}

		System.out.println("AIR: " + ans);
		return ans;
	}

	/**
	 * Verify if the android version of device or emulator is correct, kitkat and
	 * superiors.
	 * 
	 * @return true if version is kitkat or superior, false in the other case
	 * @throws Exception
	 *             if version is inferior to kitkat
	 */
	public static boolean verifyAndroidVersion() throws Exception {
		boolean ans = false;
		List<String> commands = Arrays.asList("adb", "shell", "getprop", "ro.build.version.release");
		List<String> answer = ExternalProcess2.executeProcess(commands, "CHEKING ANDROID VERSION", null, null);
		String answ = answer.get(0);
		if (answ.startsWith("1") || answ.startsWith("2") || answ.startsWith("3") || answ.startsWith("4.1")
				|| answ.startsWith("4.2") || answ.startsWith("4.3")) {
			throw new Exception("This program does not support the Android version " + answ);
		} else {
			ans = true;
		}
		System.out.println(ans);
		return ans;
	}

	/**
	 * Gets android version
	 * 
	 * @return Android version (Ex: 8.0.1)
	 * @throws RipException
	 * @throws IOException
	 * @throws Exception
	 *             if no device is connected
	 */
	public static String getAndroidVersion() throws IOException, RipException {
		List<String> commands = Arrays.asList("adb", "shell", "getprop", "ro.build.version.release");
		List<String> answer = ExternalProcess2.executeProcess(commands, "CHECKING ANDROID VERSION", null, null);
		String answ = answer.get(0).replaceAll("\\s+", "");

		return answ;
	}
	
	/**
	 * Generates a random quantity of graphic methods waiting a certain time
	 * 
	 * @param qtyEvents
	 *            is the quantity of methods to generate
	 * @param time
	 *            in milliseconds between methods
	 * @throws Exception
	 *             if any of the methods fail
	 */
	public static void generateRandomGraphicEvents(int qtyEvents, int time) throws Exception {
		// Total events qty=12

		/**
		 * 1. Tap 2. Long tap 3. Scroll 4. Go back 5. Go home 6. Show recents 7. Turn
		 * on/off screen 8. Rotate landscape 9. Rotate portrait 10. Show keyboard 11.
		 * Show numeric keyboard 12. Enter input
		 */

		ArrayList<Integer> events = new ArrayList<Integer>();
		for (int i = 0; i < qtyEvents; i++) {
			// Generates a random number between 1 and 37
			Random rm = new Random();
			int event = rm.nextInt(12 - 1 + 1) + 1;
			events.add(event);
		}

		// Starts to execute methods
		int[] size = getScreenSize();
		Random rm = new Random();
		int coordX;
		int coordY;

		for (int i = 0; i < events.size(); i++) {
			int event = events.get(i);

			if (event == 1) {
				coordX = rm.nextInt(size[0] - 0 + 1) + 0;
				coordY = rm.nextInt(size[1] - 0 + 1) + 0;
				tap(String.valueOf(coordX), String.valueOf(coordY));
			} else if (event == 2) {
				coordX = rm.nextInt(size[0] - 0 + 1) + 0;
				coordY = rm.nextInt(size[1] - 0 + 1) + 0;
				int ms = rm.nextInt(1000 - 1 + 1) + 1;
				longTap(String.valueOf(coordX), String.valueOf(coordY), String.valueOf(ms));
			} else if (event == 3) {
				coordX = rm.nextInt(size[0] - 0 + 1) + 0;
				coordY = rm.nextInt(size[1] - 0 + 1) + 0;
				int coordX1 = rm.nextInt(size[0] - 0 + 1) + 0;
				int coordY1 = rm.nextInt(size[1] - 0 + 1) + 0;
				int ms = rm.nextInt(1000 - 1 + 1) + 1;
				scroll(String.valueOf(coordX), String.valueOf(coordY), String.valueOf(coordX1), String.valueOf(coordY1),
						String.valueOf(ms));
			} else if (event == 4) {
				goBack();
			} else if (event == 5) {
				goHome();
			} else if (event == 6) {
				showRecents();
			} else if (event == 7) {
				turnOnScreen();
			} else if (event == 8) {
				rotateLandscape();
			} else if (event == 9) {
				rotatePortrait();
			} else if (event == 10) {
				showKeyboard();
			} else if (event == 11) {
				showNumKeyboard();
			} else if (event == 12) {

				enterInput("hola");
			}
			// Pause execution for a time
			Thread.sleep(time);
		}
		System.out.println("End of events");
	}

	/**
	 * Shows the current focus in screen
	 * 
	 * @return String with the actual focus and the package name
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             if there is no device
	 */
	public static String getCurrentFocus() throws IOException, RipException {
		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "window", "|", "grep", "-E",
				"'mCurrentFocus'");
		List<String> answer = ExternalProcess2.executeProcess(commands, "GETTING CURRENT FOCUS", null, null);

		String[] temp = answer.get(0).split(" ");
		String activity = temp[temp.length - 1].replace("}", "");
		return activity;
	}

	/**
	 * Shows the focused app in screen
	 * 
	 * @return String with the actual activity and the package name
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             if there is no device
	 */
	public static String getFocusedApp() throws IOException, RipException {
		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "window", "|", "grep", "-E", "'mFocusedApp'");
		List<String> answer = ExternalProcess2.executeProcess(commands, "GETTING FOCUSED APP", null, null);

		String temp = answer.get(0).split("\n")[0];
		String[] list = temp.split(" ");
		String app = list[list.length - 2];
		System.out.println(app);
		return app;
	}

	/**
	 * Gets dimensions of device to know where you can tap
	 * 
	 * @return an array with the x and y dimensions
	 * @throws Exception
	 *             if there is no device
	 */
	public static int[] getScreenSize() throws Exception {
		List<String> commands = Arrays.asList("adb", "shell", "wm", "size");
		List<String> answer = ExternalProcess2.executeProcess(commands, "CHEKING DEVICE DIMENSIONS", null, null);

		String size = answer.get(0).split(" ")[2].replaceAll("\\s+", "");
		String dimX = size.split("x")[0];
		String dimY = size.split("x")[1];

		int x = Integer.parseInt(dimX);
		int y = Integer.parseInt(dimY);

		int[] ans = new int[2];
		ans[0] = x;
		ans[1] = y;

		return ans;
	}

	/**
	 * Gets dimensions of device
	 * 
	 * @return a string with the dimensions (ex: 1080x1920)
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             if there is no device
	 */
	public static String getScreenDimensions() throws IOException, RipException {
		List<String> commands = Arrays.asList("adb", "shell", "wm", "size");
		List<String> answer = ExternalProcess2.executeProcess(commands, "CHEKING DEVICE DIMENSIONS", null, null);

		String size = answer.get(0).split(":")[1].replaceAll("\\s+", "");
		return size;
	}

	/**
	 * Checks if it is at Home
	 * 
	 * @return true if it is at home, false if it is not
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             if there is no emulator or device
	 */
	public static boolean isHome() throws IOException, RipException  {
		boolean ans = false;

		String actual = getCurrentFocus();
		System.out.println(actual);
		if (actual.contains("launcher") || actual.contains("Launcher")) {
			ans = true;
		}
		System.out.println(ans);
		return ans;
	}

	/**
	 * Starts an app if you dont know the name of the main activity
	 * 
	 * @param packageName
	 *            is the package of your app
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             is something while executing the command happens
	 */
	public static void startApp(String packageName) throws IOException, RipException {

		List<String> commands = Arrays.asList("adb", "shell", "monkey", "-p", packageName, "-v", "1");
		ExternalProcess2.executeProcess(commands, "STARTING APP", null, null);

	}

	/**
	 * Checks if any keyboard is open
	 * 
	 * @return true is a keyboard is open, false in other case
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             is there is a problem executing command
	 */
	public static boolean isKeyboardOpen() throws IOException, RipException {
		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "input_method", "|", "grep", "mInputShown");
		List<String> answer = ExternalProcess2.executeProcess(commands, "CHEKING IF KEYBOARD IS SHOWN", null, null);

		String[] ans = answer.get(0).split(" ");
		String ret = ans[ans.length - 1].split("=")[1].replaceAll("\\s+", "");

		return new Boolean(ret);
	}

	/**
	 * Verifies which is the input type of a edit text
	 * 
	 * @return 0 = no input available 1 = String 2 = Number without decimal or
	 *         negative 3 = Number with signs 4 = Number with decimal 5 = Phone
	 *         number 6 = Date and time
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 */
	public static int checkInputType() throws IOException, RipException {
		int ret = 0;
		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "input_method", "|", "grep", "-A1",
				"'mCurrentTextBoxAttribute'");
		List<String> answer = ExternalProcess2.executeProcess(commands, "CHEKING IF KEYBOARD IS SHOWN", null, null);

		String[] ans = answer.get(0).split("\n");
		String input = ans[1].substring(ans[1].indexOf("inputType="), ans[1].indexOf("ime")).replaceAll("\\s+", "");
		String number = input.split("=")[1].split("x")[1];
		if (number.equals("0") || number == null || number.equals(null)) {
			ret = 0; // No input type
		} else if (number.endsWith("1")) {
			ret = 1; // Input type equals string
		} else if (number.contains("1002") || number.contains("2002")) {
			ret = 3; // Input type equals number with negatives and symbols: -+.,/()=#
		} else if (number.endsWith("2")) {
			ret = 2; // Input type equals number with no negatives or decimal values
		} else if (number.endsWith("3")) {
			ret = 4; // Input type equals phone number, or number with the following possible
						// symbols: -+.,/()=#
		} else if (number.endsWith("4")) {
			ret = 5; // Input type equals date/time, or number with the following symbols: -/:
		}

		return ret;

	}

	/**
	 * Gets the package of the apk that you want to test
	 * 
	 * @param aapt
	 *            is the path where aapt is located
	 * @param apk
	 *            is the path where apk is located
	 * @return package name of the app
	 * @throws RipException
	 * @throws IOException
	 * @throws Exception
	 *             if any path is bad
	 */
	public static String getPackageName(String aapt, String apk) throws IOException, RipException {
		List<String> commands = Arrays.asList(aapt, "dump", "badging", apk);
		List<String> answer = ExternalProcess2.executeProcess(commands, "GETTING PACKAGE NAME", null, null);
		String ans = answer.get(0).substring(answer.get(0).indexOf("name="), answer.get(0).indexOf("versionCode"));
		String a = ans.split("=")[1].replaceAll("'", "").replaceAll("\\s+", "");
		return a;
	}

	/**
	 * Gets the main activity of the apk given
	 * 
	 * @param aapt
	 *            is the path where aapt is located
	 * @param apk
	 *            is the path where apk is located
	 * @return main activity of the app
	 * @throws RipException
	 * @throws IOException
	 * @throws Exception
	 *             if any path is bad
	 */
	public static String getMainActivity(String aapt, String apk) throws RipException, IOException {

		List<String> commands = Arrays.asList(aapt, "dump", "badging", apk);
		List<String> answer = ExternalProcess2.executeProcess(commands, "GETTING MAIN ACTIVITY", null, null);

		String[] ans = answer.get(0).split("\n");
		String a = "";
		boolean found = false;
		for (int i = 0; i < ans.length && !found; i++) {
			if (ans[i].contains("launchable-activity")) {
				a = ans[i];
				found = true;
			}
		}

		String b = "";
		int first = 0;

		if (!a.equals("")) {

			first = a.indexOf("'");
			b = a.substring(first, a.indexOf("'", first + 1)).replaceAll("'", "").replaceAll("\\s+", "");
		} else {
			throw new RipException("There is no launchable activity");
		}
		return b;

	}

	/**
	 * Lists all available sensors on device
	 * 
	 * @return a list with all the sensors
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             if there is no device connected
	 */
	public static String getSensors() throws IOException, RipException {

		List<String> commands = Arrays.asList("adb", "shell", "pm", "list", "features");
		List<String> answer = ExternalProcess2.executeProcess(commands, "GETTING AVAILABLE SENSORS", null, null);

		String ans = "";
		if (!answer.get(0).isEmpty()) {
			ans = answer.get(0).replaceAll("feature:", "");
		}
		return ans;
	}

	/**
	 * Search all the services related with the activity and service
	 * 
	 * @param packageName
	 *            is the package of the apk
	 * @return all the services related with the app
	 * @throws RipException 
	 * @throws IOException 
	 * @throws Exception
	 *             if there is no device connected
	 */
	public static String getServices(String packageName) throws IOException, RipException {

		List<String> commands = Arrays.asList("adb", "shell", "dumpsys", "activity", "services", packageName);
		List<String> answer = ExternalProcess2.executeProcess(commands, "GETTING ACTIVITY SERVICES", null, null);

		String ans = "";
		if (!answer.get(0).isEmpty()) {
			ans = answer.get(0);
		}
		return ans;
	}

	public static String turnWifi(boolean value) throws IOException, RipException {
		String sw = "OFF";
		String wifiValue = "disable";
		if (value == true) {
			wifiValue = "enable";
			sw = "ON";
		}

		List<String> commands = Arrays.asList("adb", "shell", "svc", "wifi", wifiValue);
		List<String> answer = ExternalProcess2.executeProcess(commands, "TURNING" + sw + "-> WIFI", null, null);
		String ans = "";
		if (!answer.get(0).isEmpty()) {
			ans = answer.get(0);
		}
		return ans;
	}

	public static String turnBluetooth(boolean value) throws IOException, RipException{
		String sw = "OFF";
		String bluetoothValue = "disable";
		if (value == true) {
			bluetoothValue = "enable";
			sw = "ON";
		}

		List<String> commands = Arrays.asList("adb", "shell", "svc", "bluetooth", bluetoothValue);
		List<String> answer = ExternalProcess2.executeProcess(commands, "TURNING" + sw + "-> BLUETOOTH", null, null);
		String ans = "";
		if (!answer.get(0).isEmpty()) {
			ans = answer.get(0);
		}
		return ans;
	}

	public static String turnMobileData(boolean value) throws IOException, RipException {
		String sw = "OFF";
		String dataValue = "disable";
		if (value == true) {
			dataValue = "enable";
			sw = "ON";
		}

		List<String> commands = Arrays.asList("adb", "shell", "svc", "data", dataValue);
		List<String> answer = ExternalProcess2.executeProcess(commands, "TURNING" + sw + "-> DATA", null, null);
		String ans = "";
		if (!answer.get(0).isEmpty()) {
			ans = answer.get(0);
		}
		return ans;
	}
	
	/**
	 * Rotates the device
	 * 
	 * @param value
	 *            true landscape, false portrait
	 * @throws RipException
	 * @throws IOException
	 */
	public void rotateDevice(boolean value) throws IOException, RipException {
		if (value) {
			rotateLandscape();
		} else {
			rotatePortrait();
		}
	}

	public static String turnInternet(boolean value) throws IOException, RipException {
		return turnWifi(value) + "\n" + turnMobileData(value);
	}

	public static String turnLocationServices(boolean value) throws IOException, RipException {
		return turnGPS(value) + "\n" + turnNetworkLocation(value);
	}

	public static String turnGPS(boolean value) throws IOException, RipException {
		String sw = "OFF";
		String dataValue = "-gps";
		if (value == true) {
			dataValue = "+gps";
			sw = "ON";
		}

		List<String> commands = Arrays.asList("adb", "shell", "settings", "put", "secure", "location_providers_allowed",
				dataValue);
		List<String> answer = ExternalProcess2.executeProcess(commands, "TURNING" + sw + "-> GPS", null, null);
		String ans = "";
		if (!answer.get(0).isEmpty()) {
			ans = answer.get(0);
		}
		return ans;
	}

	public static String turnNetworkLocation(boolean value) throws IOException, RipException {
		String sw = "OFF";
		String dataValue = "-network";
		if (value == true) {
			dataValue = "+network";
			sw = "ON";
		}

		List<String> commands = Arrays.asList("adb", "shell", "settings", "put", "secure", "location_providers_allowed",
				dataValue);
		List<String> answer = ExternalProcess2.executeProcess(commands, "TURNING" + sw + "-> NETWORK LOCATION SERVICES", null, null);
		String ans = "";
		if (!answer.get(0).isEmpty()) {
			ans = answer.get(0);
		}
		return ans;
	}

	public static void readWebViewConsole() throws InterruptedException {
		try {
			Process proc = Runtime.getRuntime().exec("adb logcat chromium:D SystemWebViewClient:D *:S");
			InputStream inputStream = proc.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				System.out.println(line);
			}
			proc.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void moveToEndInput() throws IOException, RipException {
		List<String> commands = Arrays.asList("adb","shell","input","keyevent","KEYCODE_MOVE_END");
		ExternalProcess2.executeProcess(commands,"MOVE TO END OF INPUT FIELD", null, null);
	}

	public static void deleteOneEntrance()throws  IOException, RipException{
		List<String> commands = Arrays.asList("adb","shell","input","keyevent","KEYCODE_DEL");
		ExternalProcess2.executeProcess(commands,"DELETING ONE ENTRANCE IN INPUT FIELD", null, null);
	}

	public static List<String> getActiveEmulators() throws  IOException, RipException{
		List<String> listaEmuladores = new ArrayList();
		List<String> commands = Arrays.asList("adb","devices");
		String lista = ExternalProcess2.executeProcess(commands,"GETTING LIST OF ACTIVE DEVICES", null,null).get(0);
		String[] listaEmu = lista.split("\\n");
		String aux = "";
		for(int i =1; i<listaEmu.length;i++){
			aux = listaEmu[i].trim().split("device")[0].trim();
			if(!aux.equals("") && aux != null){
				listaEmuladores.add(aux);
			}
		}
		return listaEmuladores;
	}

	public static void shutdownEmulators(){
		List<String> commands = Arrays.asList("adb","emu","kill");
		try{
			ExternalProcess2.executeProcess(commands,"SHUTDOWN EMULATORS", null,null);
			Thread.sleep(8000);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	public static void startEmulatorWipeData(String emulatorName)throws IOException, RipException, InterruptedException{
		if(emulatorName == null){
			emulatorName = "Nexus_6_API_27";
		}
		List<String> commands = Arrays.asList("emulator","-wipe-data","-avd",emulatorName);
		ProcessBuilder pb = new ProcessBuilder();
		List<String> emulatorList = getAvailableEmulators();
		if(emulatorList.contains(emulatorName)){
			pb.command(commands);
			pb.start().waitFor(1,TimeUnit.SECONDS);
			isEventIdle();
			//Execute adb root command
			ProcessBuilder pB1 = new ProcessBuilder();
			pB1.command("adb", "root");
			Process root = pB1.start();
			root.waitFor();
			System.out.println("Emulator ready");
			Thread.sleep(3000);
		}else{
			throw new RipException("There is no an emulator with the specified name in this system");
		}
	}

	public static void shutdownAndStartWipeDataEmulator() throws IOException, RipException, InterruptedException {
		shutdownEmulators();
		startEmulatorWipeData(null);
	}


	public static List<String> getAvailableEmulators()throws IOException, RipException{
		List<String> commands = Arrays.asList("emulator","-list-avds");
		String response = ExternalProcess2.executeProcess(commands, "GET AVAILABLE EMULATORS",null,null).get(0);
		String[] emulatorsList= response.split("\\n");
		List<String> responseList = new ArrayList();
		String aux = "";
		for(int i =0; i< emulatorsList.length;i++){
			aux = emulatorsList[i].trim();
			responseList.add(aux);
		}
		return  responseList;
	}

	public static boolean changeLanguage(String language, String expresiveLanguage, String extraPath) throws IOException, InterruptedException{
		// Change emulator language
		ProcessBuilder pB = new ProcessBuilder(new String[]{"adb","shell","setprop persist.sys.locale "+language});
		Process ps = pB.start();
		System.out.println("Emulator language changed to "+expresiveLanguage);
		ps.waitFor();
		// Restart emulator for language change to be taken into account
		pB.command(new String[]{"adb","shell","setprop ctl.restart zygote"});
		ps = pB.start();
		System.out.println("Emulator is being restarted");
		// Running command that waits emulator for idle state
		//		System.out.println(Paths.get(Helper.getInstance().getCurrentDirectory(),extraPath,"./whileCommand").toAbsolutePath().toString());
		ps.waitFor();
//		Thread.sleep(000);
		isEventIdle();
		Thread.sleep(2000);
		return true;
	}

}
