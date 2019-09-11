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
	
	public static int levenshteinDistance(String left, String right) {
		left = left.toLowerCase();
		right = right.toLowerCase();
        // i == 0
        int n = left.length(); // length of left
        int m = right.length(); // length of right

        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }

        if (n > m) {
            // swap the input strings to consume less memory
            final String tmp = left;
            left = right;
            right = tmp;
            n = m;
            m = right.length();
        }

        int[] p = new int[n + 1];

        // indexes into strings left and right
        int i; // iterates through left
        int j; // iterates through right
        int upper_left;
        int upper;

        char rightJ; // jth character of right
        int cost; // cost

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }

        for (j = 1; j <= m; j++) {
            upper_left = p[0];
            rightJ = right.charAt(j - 1);
            p[0] = j;

            for (i = 1; i <= n; i++) {
                upper = p[i];
                cost = left.charAt(i - 1) == rightJ ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                p[i] = Math.min(Math.min(p[i - 1] + 1, p[i] + 1), upper_left + cost);
                upper_left = upper;
            }
        }

        return p[n];
    }
}
