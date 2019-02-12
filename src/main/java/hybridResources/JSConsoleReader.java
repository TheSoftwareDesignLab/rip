package hybridResources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import main.Rip2;

public class JSConsoleReader extends Thread {
	
	private static Rip2 rip;
	
	private static boolean isRunning = true;
	
	public JSConsoleReader(Rip2 mainThread) {
		super();
		rip = mainThread;
	}
	
	public void run() {
		try {
			readWebViewConsole();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void readWebViewConsole() throws InterruptedException {
		try {
			Process proc = Runtime.getRuntime().exec("adb logcat chromium:D SystemWebViewClient:D *:S");
			InputStream inputStream = proc.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String line = null;
			while ((line = bufferedReader.readLine()) != null && isRunning) {
				System.out.println(line);
				if(line.contains("E chromium")) {
					
				}
			}
			proc.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void kill() {
		isRunning = false;
	}

}
