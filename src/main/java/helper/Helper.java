package helper;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;

import javax.imageio.ImageIO;

public class Helper {

	static void logMessage(String command, String[] parameters, String errorMessage) {
		// try {
		if (errorMessage == null) {
			// out.write("COMMAND:" + command +"; PARAMETERS" + parameters.toString() +
			// "\n");
			// System.out.println("COMMAND:" + command +"; PARAMETERS" +
			// parameters.toString() + "\n");
		} else {
			// out.write("ERROR "+command);
			// out.write("COMMAND:" + command +"; PARAMETERS" + parameters.toString() +
			// "\n");
			// System.out.println("COMMAND:" + command +"; PARAMETERS" +
			// parameters.toString() + "\n");
		}
		// out.flush();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}
	
	public static void deleteFile(String file) {
		File toDelete = new File(file);
		toDelete.delete();
	}
}
