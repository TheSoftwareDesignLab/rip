package helper;
/*
 * chrome --headless --disable-gpu --dump-dom https://www.chromestatus.com/
	adb logcat chromium:D SystemWebViewClient:D *:S
 */

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import main.RipException;

public class ExternalProcess3 {

	/**
	 * Common method. It creates a process builder with a list of commands and
	 * executes it. Each method must give appropriate commands depending on the
	 * functionality.
	 * 
	 * @param commands
	 *            List with commands to pass them as arguments to the process
	 *            builder
	 * @return answer List with inputStream and errorStream of the process builder
	 * @throws IOException
	 * @throws RipException
	 * @throws InterruptedException 
	 * @throws Exception
	 *             if it is not possible to generate the process builder
	 */
	public static List<String> executeProcess(List<String> commands,
											  String commandName, String onSuccessMessage,
											  String onErrorMessage, String dir) throws IOException, RipException, InterruptedException {
		List<String> answer = new ArrayList<String>();
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		//System.out.println(s+dir);
		ProcessBuilder pb = new ProcessBuilder(commands).directory(new File(dir));
		System.out.println(pb.directory());

	
		Process spb = pb.start();
		String output = IOUtils.toString(spb.getInputStream(), "UTF-8");
		String err = IOUtils.toString(spb.getErrorStream(), "UTF-8");
		while(err.contains("null root node")) {
			pb = new ProcessBuilder(commands).directory(new File(dir));
			spb = pb.start();
			output = IOUtils.toString(spb.getInputStream(), "UTF-8");
			err = IOUtils.toString(spb.getErrorStream(), "UTF-8");
		}
		answer.add(output);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(spb.getInputStream()));
		BufferedReader errorReader = new BufferedReader(new InputStreamReader(spb.getErrorStream()));
		String line, errorLine = "";
		while ((line = reader.readLine())!=null || (errorLine = errorReader.readLine())!= null) {
								System.out.println(line);
			System.out.print(".");
			System.out.println(reader.readLine());
			if(errorLine != null && errorLine.contains("EXITING EXECUTION. START STATE != CURRENT STATE")) {
				throw new RipException("New replay failure");
			}
		}

		spb.waitFor();
		
//		if (!output.startsWith("<?xml") || output.equals("")) {
//			System.out.println("answer added ----------------: " + output);
//		}
		
		answer.add(err);
		//System.out.println(err);
		answer.add(commandName);
		System.out.println("- - - - - - - - - - - - - - - - - - -");
		//Helper.getInstance("./").logMessage(commandName, Arrays.toString(commands.toArray(new String[]{})), err);

		if (!err.equals("")) {
			throw new RipException(err);
		}

		if (!output.startsWith("<?xml")) {
			if (output.startsWith("adb: error") || output.contains("error") || output.contains("Failure")
					|| output.contains("Error")) {
				throw new RipException(output);
			}
		}

		return answer;
	}

	
}