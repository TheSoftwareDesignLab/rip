package helper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import model.AndroidNode;
import model.State;
import model.Transition;
import model.TransitionType;

public class ImageHelper {


	// In this class can be added a new method that highlight a part of an image based on coordinates.

	public static float compareImage(File fileA, File fileB) {

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

	public static String takeTransitionScreenshot(Transition stateTransition, int transitionId) throws Exception {

		State actual = stateTransition.getOrigin();
		File screen = new File(actual.getScreenShot());
		BufferedImage img;
		if(TransitionType.getUserTypeTransitions().contains(stateTransition.getType())) {

			AndroidNode toHighlight = stateTransition.getOriginNode();
			int[] p1 = toHighlight.getPoint1();
			int[] p2 = toHighlight.getPoint2();
			img = ImageIO.read(screen);
			//			[0,84][1440,2392]

			int width = p2[0] - p1[0];
			int height = p2[1] - p1[1];
			Graphics2D g2d = img.createGraphics();

			final float dash1[] = { 20.0f };
			final BasicStroke dashed = new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
					dash1, 0.0f);
			g2d.setStroke(dashed);

			int alpha = 127; // 50% transparent
			Color myColour = new Color(245, 187, 5, alpha);
			g2d.setColor(myColour);
			g2d.fillOval(p1[0], p1[1], width, height);

			g2d.setColor(Color.RED);
			g2d.drawOval(p1[0], p1[1], width, height);
			g2d.dispose();

		} else {
			int[] deviceDimensions = EmulatorHelper.getScreenSize();
			img = new BufferedImage(deviceDimensions[0], deviceDimensions[1], BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = img.createGraphics();

			g2d.setPaint ( Color.BLACK );
			g2d.fillRect ( 0, 0, deviceDimensions[0], deviceDimensions[1] );

			g2d.setPaint(Color.WHITE);
			g2d.setFont(new Font("Serif", Font.BOLD, 60));

			String s = stateTransition.getType().toString();
			FontMetrics fm = g2d.getFontMetrics();
			int x = (img.getWidth() - fm.stringWidth(s))/2;
			int y = (img.getHeight() - fm.getHeight())/2;
			g2d.drawString(s, x, y);
			g2d.dispose();
		}

		String route = screen.getParent();

		File outputfile = new File(route +File.separator+ "t"+transitionId+".png");
		ImageIO.write(img, "png", outputfile);

		return outputfile.getPath();
	}
	
	public static String getNodeImagesFromState(State state) throws IOException {
		File screenshot = new File(state.getScreenShot());
		File stateNodesFolder = new File(screenshot.getParent()+File.separator+state.getId()+File.separator);
		stateNodesFolder.mkdirs();
		
		List<AndroidNode> nodes = state.getStateNodes();
		
		for (int i = 0; i < nodes.size(); i++) {
			AndroidNode temp = nodes.get(i);
			BufferedImage img = ImageIO.read(screenshot);
			BufferedImage subimage = img.getSubimage(temp.getPoint1()[0], temp.getPoint1()[1], (temp.getPoint2()[0]-temp.getPoint1()[0]), (temp.getPoint2()[1]-temp.getPoint1()[1]));
			ImageIO.write(subimage, "png", new File(stateNodesFolder.getPath()+File.separator+i+".png"));
		}
		
		return stateNodesFolder.getPath();
	}

}
