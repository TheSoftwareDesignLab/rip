package helper;
/*
 * chrome --headless --disable-gpu --dump-dom https://www.chromestatus.com/
	adb logcat chromium:D SystemWebViewClient:D *:S
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import main.Rip;
import main.RipException;

public class ExternalProcess2 {

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
	 * @throws Exception
	 *             if it is not possible to generate the process builder
	 */
	public static List<String> executeProcess(List<String> commands, String commandName, String onSuccessMessage,
			String onErrorMessage) throws IOException, RipException {
		List<String> answer = new ArrayList<String>();
		ProcessBuilder pb = new ProcessBuilder(commands);
		System.out.println("-> " + commandName);
		System.out.println(commands.toString());
		Process spb = pb.start();
		String output = IOUtils.toString(spb.getInputStream());
		answer.add(output);
		if (!output.startsWith("<?xml")) {
//			System.out.println(output);
		}
		String err = IOUtils.toString(spb.getErrorStream());
		answer.add(err);
		System.out.println(err);
		answer.add(commandName);
		System.out.println("- - - - - - - - - - - - - - - - - - -");

		Helper.getInstance("./").logMessage(commandName, Arrays.toString(commands.toArray(new String[]{})), err);

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
