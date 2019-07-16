package helper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
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
		String screenShot = actual.getScreenShot();
		//TODO eliminar sysout
		System.out.println("ID: " + actual.getId());
		System.out.println("SCREENSHOT: " + screenShot);
		File screen = new File(screenShot);
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

		} else if (TransitionType.getScrollTransitions().contains(stateTransition.getType())) {
			
			AndroidNode toHighlight = stateTransition.getOriginNode();
			
			int[] p1 = toHighlight.getPoint1();
			int[] p2 = toHighlight.getPoint2();
			
			int tapX = p1[0];
			int tapX2 = (int) (p2[0] / 3) * 2;

			int tapY = p1[1];
			int tapY2 = (int) (p2[1] / 3) * 2;
			
			img = ImageIO.read(screen);
			Graphics2D g2d = img.createGraphics();

			final float dash1[] = { 20.0f };
			final BasicStroke dashed = new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
					dash1, 0.0f);
			g2d.setStroke(dashed);
			
			Point initial = new Point(tapX2, tapY2);
			Point finalP;
			if(stateTransition.getType()==TransitionType.SCROLL) {
				finalP = new Point(tapX2, tapY);
			} else {
				finalP = new Point(tapX, tapY2);
			}
			
			Shape arrow = createArrowShape(initial, finalP);
			int alpha = 127; // 50% transparent
			Color myColour = new Color(245, 187, 5, alpha);
			g2d.setColor(myColour);
			g2d.fill(arrow);
			
			g2d.setColor(Color.RED);
			g2d.draw(arrow);
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
	
	public static Shape createArrowShape(Point fromPt, Point toPt) {
	    Polygon arrowPolygon = new Polygon();
	    arrowPolygon.addPoint(-6,1);
	    arrowPolygon.addPoint(3,1);
	    arrowPolygon.addPoint(3,3);
	    arrowPolygon.addPoint(6,0);
	    arrowPolygon.addPoint(3,-3);
	    arrowPolygon.addPoint(3,-1);
	    arrowPolygon.addPoint(-6,-1);


	    Point midPoint = midpoint(fromPt, toPt);

	    double rotate = Math.atan2(toPt.y - fromPt.y, toPt.x - fromPt.x);

	    AffineTransform transform = new AffineTransform();
	    transform.translate(midPoint.x, midPoint.y);
	    double ptDistance = fromPt.distance(toPt);
	    double scale = ptDistance / 12.0; // 12 because it's the length of the arrow polygon.
	    transform.scale(scale, scale);
	    transform.rotate(rotate);

	    return transform.createTransformedShape(arrowPolygon);
	}

	private static Point midpoint(Point p1, Point p2) {
	    return new Point((int)((p1.x + p2.x)/2.0), 
	                     (int)((p1.y + p2.y)/2.0));
	}
	
	public static String getNodeImagesFromState(State state) throws IOException {
		File screenshot = new File(state.getScreenShot());
		File stateNodesFolder = new File(screenshot.getParent()+File.separator+state.getId()+File.separator);
		stateNodesFolder.mkdirs();
		
		List<AndroidNode> nodes = state.getStateNodes();
		
		for (int i = 0; i < nodes.size(); i++) {
			AndroidNode temp = nodes.get(i);
			BufferedImage img = ImageIO.read(screenshot);
			int w = (temp.getPoint2()[0]-temp.getPoint1()[0]);
			int h = (temp.getPoint2()[1]-temp.getPoint1()[1]);
			BufferedImage subimage = img.getSubimage(temp.getPoint1()[0], temp.getPoint1()[1], w==0?30:w, h==0?30:h);
			ImageIO.write(subimage, "png", new File(stateNodesFolder.getPath()+File.separator+state.getId()+"_"+i+".png"));
		}
		
		return stateNodesFolder.getPath();
	}

}
