package rip.test;

import java.io.File;
import org.junit.Test;
import helper.ExternalProcess;
import junit.framework.TestCase;

/**
 * Test class. Checks methods of ExternalProcess class.
 * @author lanabeji
 *
 */
public class RipTest extends TestCase{

	/**
	 * Initializes tests cases installing an APK 
	 */
	public void init() {
		try {
			ExternalProcess.keepUnlock();
			ExternalProcess.installAPK("android app/app-debug.apk");
			ExternalProcess.startActivity("com.example.lanabeji.dailyexpenses/com.example.lanabeji.dailyexpenses.MainActivity");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Uninstall the apk. Use it after tests cases
	 */
	public void destroy() {
		try {
			ExternalProcess.uninstallAPK("com.example.lanabeji.dailyexpenses");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the version returned by verifyABD is the same of the system
	 */
	public void testVerifyADB() {

		try {
			assertEquals("Checking ADB version", "1.0.39", ExternalProcess.verifyADB());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * First, invokes init() method to install an apk, then tries install the same apk and an exception is expected
	 */
	@Test(expected = Exception.class)
	public void testInstallException() {

		init();
		try {
			ExternalProcess.installAPK("android app/app-debug.apk");
			fail("install: expected exception was not occured.");
			destroy();
		} catch (Exception e) {
			System.out.println("Exception occurred, apk already installed");
		}
		destroy();
	}

	/**
	 * Tries to uninstall an app but an exception is expected, app is not installed.
	 */
	@Test(expected = Exception.class)
	public void testUnsintallException() {

		try {
			ExternalProcess.uninstallAPK("com.example.lanabeji.dailyexpenses");
			fail("uninstall: expected exception was not occured.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Exception occurred, apk is not installed");
		}
	}

	/**
	 * First, invokes init() method to install an apk and start one activity, then tries start the same activity and an exception is expected
	 */
	@Test(expected = Exception.class)
	public void testStartActivityException() {

		init();
		try {
			ExternalProcess.startActivity("com.example.lanabeji.dailyexpenses/com.example.lanabeji.dailyexpenses.MainActivity");
			fail("start activity: expected exception was not occured.");
			destroy();
		} catch (Exception e) {
			System.out.println("Exception occurred, activity is started");
		}
		destroy();
	}

	/**
	 * First, invokes init() method to install an apk and start one activity, then tries start a forbidden activity and a security exception is expected
	 */
	@Test(expected = Exception.class)
	public void testStartActivitySecurityException() {

		init();
		try {
			ExternalProcess.startActivity("com.example.lanabeji.dailyexpenses/com.example.lanabeji.dailyexpenses.ViewExpensesActivity");
			destroy();
			fail("start activity: expected security exception was not occured.");
		} catch (Exception e) {
			System.out.println("Exception occurred, permission denied");
		}
		destroy();
	}

	/**
	 * Installs and starts an app, then stops all related processes. 
	 * It is assumed that all processes stopped, so it is possible to start an activity without exceptions
	 */
	public void testStopApp() {
		init();
		try {
			ExternalProcess.stopApp("com.example.lanabeji.dailyexpenses");
			ExternalProcess.startActivity("com.example.lanabeji.dailyexpenses/com.example.lanabeji.dailyexpenses.MainActivity");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail("Not expected exception, the activity should have started");
		}
		destroy();
	}
	
	/**
	 * Installs and starts an app, then cleans all related data.
	 */
	public void testClearData() {
		init();
		try {
			ExternalProcess.clearData("com.example.lanabeji.dailyexpenses");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail("Not expected exception, data had to be removed");
		}
		destroy();
	}
	
	
	/**
	 * Installs and starts an app, then tries to clear data of an inexistent package. Exception is expected
	 */
	@Test(expected = Exception.class)
	public void testClearDataException() {
		init();
		try {
			ExternalProcess.clearData("com.example.lanabeji.daily");
			fail("clear data: expected exception was not occured.");
			destroy();
		} catch (Exception e) {
			System.out.println("Exception occurred, package does not exists");
		}
		destroy();
	}
	
	
	/**
	 * Tries to take an screenshot
	 */
	public void testTakeScreenshot() {
		try {
			ExternalProcess.takeScreenshot("/sdcard/screen.png");
		} catch (Exception e) {
			fail("Not expected exception, screenshot had to be taken");
		}
	}
	
	/**
	 * Tries to take an screenshot. Exception is expected because path is wrong
	 */
	@Test(expected = Exception.class)
	public void testTakeScreenshotException() {
		try {
			ExternalProcess.takeScreenshot("/screen.png");
			fail("takeScreenshot: expected exception was not occured.");
		} catch (Exception e) {
			System.out.println("Exception occurred, filename is wrong");
		}

	}
	
	/**
	 * Tries to pull a file from device
	 */
	public void testPullFile() {
		try {
			ExternalProcess.pullFile("/sdcard/screen.png", "/Users/lanabeji/Desktop/pulled.png");
			File file = new File("/Users/lanabeji/Desktop/pulled.png");
			assertTrue(file.exists());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail("Not expected exception, file had to be pulled");
		}
	}
	
	/**
	 * Takes a screenshot and pulls to computer
	 */
	public void testPullScreenshot() {
		try {
			ExternalProcess.pullScreenshot("/sdcard/testScreen.png", "/sdcard/testScreen.png", "/Users/lanabeji/Desktop/testPull.png");
			File file = new File("/Users/lanabeji/Desktop/testPull.png");
			assertTrue(file.exists());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			destroy();
			fail("Not expected exception, screenshot had to be taken and pulled");
		}
	}
	
	/**
	 * Takes a screenshot and tries to pull to computer, but an exception is expected because remotePath is wrong
	 */
	@Test(expected = Exception.class)
	public void testPullScreenshotException() {
		try {
			ExternalProcess.pullScreenshot("/sdcard/testScreen1.png", "/sdcard/test1.png", "/Users/lanabeji/Desktop/testPull1.png");
			fail("pullScreenshot: expected exception was not occured.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Exception occurred, remotePath is wrong");
		}
	}
	
	/**
	 * Gets physical density of device
	 */
	public void testGetDeviceResolution() {
		assertEquals("Checking device resolution", 420 ,ExternalProcess.getDeviceResolution());
	}
	
	/**
	 * Gets current orientation of device
	 */
	public void testGetCurrentOrientation() {
		assertEquals("Checking device orientation", 0 ,ExternalProcess.getCurrentOrientation());
	}
	
	/**
	 * Tests correct operation of touching power button, tapping the screen, scroll, go back, go home and show recents
	 */
	public void testTap() {
		
		try
		{
		//ExternalProcess.turnOnScreen();
		ExternalProcess.scroll("400", "1200", "400", "400", "500");
		ExternalProcess.tap("100", "800");
		ExternalProcess.tap("300", "800");
		ExternalProcess.tap("500", "800");
		ExternalProcess.tap("400", "1100");
		ExternalProcess.tap("550", "1100");
		ExternalProcess.longTap("200", "200", "1000");
		ExternalProcess.tap("600", "1200");
		ExternalProcess.tap("200", "200");
		ExternalProcess.goHome();
		ExternalProcess.tap("600", "1200");
		ExternalProcess.goBack();
		ExternalProcess.showRecents();
		ExternalProcess.goHome();
		ExternalProcess.tap("600", "1200");
		ExternalProcess.rotateLandscape();
		ExternalProcess.rotatePortrait();
		}
		catch (Exception e)
		{
			fail("Not expected exception");
			e.printStackTrace();
		}
	}
	
	/**
	 * Test if logcat has been shown
	 */
	public void testShowLogcat() {
		try {
			ExternalProcess.showLogcat();
		} catch (Exception e) {
			fail("Not expected exception, logcat had to be shown");
		}
	}
	
	/**
	 * Test if logcat has been cleaned
	 */
	public void testClearLogcat() {
		try {
			ExternalProcess.clearLogcat();
		} catch (Exception e) {
			fail("Not expected exception, logcat had to be cleaned");
		}
	}
	
	/**
	 * Turns off automatic rotation and check it with getCurrentOrientation
	 */
	public void testTurnOffRotation() {
		try {
			ExternalProcess.turnOffRotation();
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals("Checking that automatic rotation is off", 0, ExternalProcess.getCurrentOrientation());
	}
	
	/**
	 * Rotates to landscape and check if current orientation is 1
	 * Rotates to portrait and check if current orientation is 0
	 */
	public void testRotate() {
		ExternalProcess.rotateLandscape();
		assertEquals("Rotation to landscape", 1, ExternalProcess.getCurrentOrientation());	
		try {
			ExternalProcess.rotatePortrait();
		} catch (Exception e) {
			fail("Not expected exception");
			e.printStackTrace();
		}
		assertEquals("Rotation to portrait", 0, ExternalProcess.getCurrentOrientation());
	}
	
	/**
	 * Tries to get a list of emulators
	 */
	public void testGetEmulators() {
		try{
			ExternalProcess.getEmulators();
		}
		catch(Exception e){
			fail("Not expected exception, avd list had to be shown");
		}
	}
	
	/**
	 * Tries to launch an emulator but an exception is expected because the name is wrong(it does not exists)
	 */
	@Test(expected = Exception.class)
	public void testLaunchEmulatorException() {
		try {
			ExternalProcess.launchEmulator("Nexus_5x_API");
			fail("launchEmulator: expected exception was not occured.");
		}
		catch(Exception e) {
			System.out.println("Exception occurred, emulator name is wrong");
		}
	}
	
	/**
	 * Tests if a file is correctly created
	 */
	public void testCreateFile() {
		try {
			ExternalProcess.createFile("/sdcard/file.txt");
		}
		catch(Exception e) {
			fail("Not expected exception, file had to be created");
		}
	}
	
	/**
	 * Gets logcat, saves it in a file and pulls it to pc
	 */
	public void testSaveLogcat() {
		try {
			ExternalProcess.saveLogcat("/sdcard/log.txt", "/Users/lanabeji/Desktop/out.txt");
			File file = new File("/Users/lanabeji/Desktop/out.txt");
			assertTrue(file.exists());
		}
		catch(Exception e) {
			fail("Not expected exception, file had to be pulled");
		}
	}
	
	public void testKeyBoard() {
		try {
			ExternalProcess.showKeyboard();
			ExternalProcess.goBack();
			ExternalProcess.showNumKeyboard();
			ExternalProcess.goBack();
			ExternalProcess.enterInput("hello world");
		}
		catch(Exception e) {
			fail("Not expected exception, keyboard had to be shown");
		}
	}
}
