package imageAnalyzer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;

import javax.imageio.ImageIO;

public class ImageAnalyzer {

	public float compareImage(File fileA, File fileB) {

		float percentage = 0;
		try {
			BufferedImage biA = ImageIO.read(fileA);
			DataBuffer dbA = biA.getData().getDataBuffer();
			int sizeA = dbA.getSize();
			BufferedImage biB = ImageIO.read(fileB);
			DataBuffer dbB = biB.getData().getDataBuffer();
			int sizeB = dbB.getSize();
			int count = 0;
			if (sizeA == sizeB) {
				for (int i = 0; i < sizeA; i++) {

					if (dbA.getElem(i) == dbB.getElem(i)) {
						count = count + 1;
					}

				}
				percentage = (count * 100) / sizeA;
			} else {
				System.out.println("Both the images are not of same size");
			}

		} catch (Exception e) {
			System.out.println("Failed to compare image files ...");
		}
		return percentage;
	}

}
