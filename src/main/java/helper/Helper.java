package helper;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;

import javax.imageio.ImageIO;

public class Helper {

	private static Helper helper;
	private static BufferedWriter out;

	public static Helper getInstance(String folderName) {
		if(helper == null) {
			helper =  new Helper();
			try {
				out = new BufferedWriter(new FileWriter( folderName + File.separator + "./log.log"));
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return helper;
	}

	static void logMessage(String command, String parameters, String errorMessage) {

		if(out != null) {
			try {

				if (errorMessage != null && errorMessage.equals("")) {
					out.write("COMMAND: " + command +" ; PARAMETERS" + parameters.toString() +
							"\n");
					System.out.println("COMMAND: " + command +" ; PARAMETERS"  +
							parameters.toString() + "\n");
				} else {
					out.write("ERROR "+command);
					out.write("COMMAND:" + command +"; PARAMETERS" + parameters.toString() +
							"\n");
					System.out.println("COMMAND:" + command +"; PARAMETERS " +
							parameters.toString() + "\n");
				}
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		}

	
	public static void deleteFile(String file) {
		File toDelete = new File(file);
		toDelete.delete();
	}

	public static void closeStream() {
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
