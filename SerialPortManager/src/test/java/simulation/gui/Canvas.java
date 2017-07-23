package simulation.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import com.navigation.GPSData;
import com.navigation.algorithm.Angle;

import simulation.RobotMock;
import simulation.convert.Projection;

/**
 * Canvas jest rozszerzeniem standardowego panelu oferowanego przez Swing. 
 * Odpowiada za rysowanie aktualnej pozycji robota oraz celu.
 * @author Harry
 *
 */
public class Canvas extends JPanel {

	private Projection projection = new Projection(20);
	private RobotMock mock;

	private List<Point2D.Double> points = new ArrayList<>();
	private Point2D.Double target, current;

	public Canvas(boolean b) {
        super(b);
        
		initGui();
    }
	
	/**
	 * Ustawia mocka
	 * @param mock
	 */
	public void setMock(RobotMock mock) {
		this.mock = mock;
	}

	/**
	 * Inicjalizuje GUI, obsługa eventu kliknięcia myszką (nowy cel)
	 */
	private void initGui() {
	    this.addMouseListener(new MouseAdapter() {
	    	@Override
            public void mousePressed(MouseEvent e) {

	    		target = projection.toWorldCoords(e.getX(), e.getY());
	    		//System.out.println("Target: " + e.getX() + ", " + e.getY() + ", " + target);
                System.out.println("Target GPS: " + projection.fromWorldToGeo(target));
                mock.setTarget(projection.fromWorldToGeo(target));
                repaint();
            }
	    });
	    //this.setOpaque(true);
		this.setBackground(Color.WHITE);
	}

	/**
	 * Metoda odpowiadająca za rysowanie grafik
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Point c = projection.toWindowCoords(current);
		
		Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw target
        if(target != null) {
	        g2.setColor(Color.RED);
	    	Point p = projection.toWindowCoords(target);
	        g2.fillOval(p.x - 5, p.y - 5, 10, 10);
        }
        
        // draw path
        g2.setColor(Color.GRAY);
        Iterator<Double> it = points.iterator();
        while(it.hasNext()) {
        	Point window = projection.toWindowCoords(it.next());
            g2.fillOval(window.x - 2, window.y - 2, 4, 4); 
        }
        
        // draw center
        g2.setColor(Color.BLUE);
        g2.fillOval(c.x - 5, c.y - 5, 10, 10);
        
        // draw target to center line
        if(target != null) {
    		Point t = projection.toWindowCoords(target);
    		
        	g2.setColor(Color.GREEN);
        	g2.drawLine(c.x, c.y, t.x, t.y);
        }
        
        // draw robot
        g2.setColor(Color.BLACK);
        
        g2.translate(c.x, c.y);

        AffineTransform old = g2.getTransform();
        
        g2.rotate(mock.getHeading() != null ? mock.getHeading() : 0);
        
        g2.drawLine(0, -15, 0, -30);
        g2.drawRect(-10, -15, 20, 30);
        
        g2.setTransform(old);
        
        drawCompassBearing(g2);
        
        g2.setTransform(old);

        // draw heading line
        drawHeading(g2);
        
        g2.setTransform(old);
        
        // draw desired angle line
        drawDesiredAngle(g2);
    }

	/**
	 * Rysuje docelowy kąt obrotu
	 * @param g2
	 */
	private void drawDesiredAngle(Graphics2D g2) {
		if(mock.getDesiredAngle() != null) {
	        g2.setColor(Color.BLUE);
	
	        g2.rotate(mock.getDesiredAngle() - Math.PI/2);
	        g2.drawLine(0, 0, 100, 0);
	        g2.drawString("D" + Math.round(Math.toDegrees(mock.getDesiredAngle()) * 100.0f)/100.0f, 90, 15);
        }
	}

	/**
	 * Rysuje kierunek robota
	 * @param g2
	 */
	private void drawHeading(Graphics2D g2) {
		if(mock.getHeading() != null) {
        	g2.setColor(Color.RED);
        	
	        g2.rotate(mock.getHeading() - Math.PI/2);
	        g2.drawLine(0, 0, 100, 0);
	        g2.drawString("H" + Math.round(Math.toDegrees(mock.getHeading()) * 100.0f)/100.0f, 90, 15);
        }
	}

	/**
	 * Rysuje kąty w kompasie
	 * @param g2
	 */
	private void drawCompassBearing(Graphics2D g2) {
		g2.rotate(Math.toRadians(-90));
        
        // draw compass bearing
        for(int i = 0; i < 360 ; i+=30) {
        	g2.drawLine(0, 0, 100, 0);
            g2.drawString(i + ", " + (int)Angle.normalizeAngleDeg(i), 50, 15);
        	g2.rotate(Math.toRadians(30));
        }
	}
	
	/**
	 * Ustawia pozycję
	 * @param newCurrent
	 */
	public void setCurrent(GPSData newCurrent) {
		this.current = projection.fromGeoToWorld(newCurrent);
		points.add(current);
		repaint();
	}
	
	/**
	 * Ustawia początkową pozycję
	 * @param initialCurrent
	 */
	public void setInitialCurrent(GPSData initialCurrent) {
		mock.setCurrentInitial(initialCurrent);
		setCurrent(initialCurrent);
		
		projection.setWindowOrigin(current, this.getWidth(), this.getHeight());
	}
}
