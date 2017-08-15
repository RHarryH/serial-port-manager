package simulation;

import java.awt.geom.Point2D;
import java.util.concurrent.TimeUnit;

import com.navigation.GPSData;
import com.navigation.RobotController;
import com.navigation.algorithm.Angle;
import com.navigation.algorithm.Command;

import simulation.convert.Projection;
import simulation.gui.Canvas;

/**
 * Klasa ta odpowiada za symulowanie robota. Jest pośrednikiem pomiędzy kontrolerem,
 * a canvasem.
 * @author Harry
 *
 */
public class RobotMock {
	
	/**
	 * Wątek odpowiedzialny za symulację obrotu
	 * @author Harry
	 *
	 */
	private class Rotation extends Thread {
		public double targetDirection;
		
		public void setTargetDirection(double targetDirection) {
			this.targetDirection = targetDirection;
		}
	
		@Override
		public void run() {
			//Double desiredAngle = controller.getDesiredAngle();
			while(Math.abs(controller.getDesiredAngle() - heading) > Math.toRadians(1)) {
				if(targetDirection < 0)
					setHeading(heading - Math.toRadians(1));
				else
					setHeading(heading + Math.toRadians(1));
	
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Mock(Thread): H:" + Math.toDegrees(heading) + " TD:" +  Math.round(Math.toDegrees(targetDirection)));
		}
	}

	private Canvas canvas;
	private Projection projection = new Projection(22);
	private RobotControllerTest controller;
	
	private Double heading;
	private GPSData current;

	private Rotation rotation = new Rotation();
	
	public RobotMock(GPSData initial, Canvas canvas) {
		this.canvas = canvas;
		this.controller = new RobotControllerTest(this);
		this.canvas.setMock(this);
		
		this.controller.setInitialPosition(initial);
		this.canvas.setInitialPosition(initial);
	}
	
	/**
	 * Pobierz kierunek robota z kontrolera
	 * @return
	 */
	public Double getHeading() {
		return controller.getHeading();
	}

	/**
	 * Ustaw kierunek robota w kontrolerze
	 * @param heading
	 */
	public void setHeading(double heading) {
		this.heading = Angle.denormalizeAngle(heading);
		canvas.repaint();
		controller.setHeading(this.heading);
	}
	
	/**
	 * Pobierz z kontrolera docelowy kat obrotu
	 * @return
	 */
	public Double getDesiredAngle() {
		return controller.getDesiredAngle();
	}
	
	/**
	 * Ustaw aktualną pozycję
	 * @param current
	 */
	public void setCurrent(GPSData current) {
		controller.setCurrent(current);
		canvas.setCurrent(current);
	}
	
	/**
	 * Ustaw początkową pozycję
	 * @param initial
	 */
	public void setInitialPosition(GPSData initial) {
		this.current = initial;
		controller.setInitialPosition(initial);
		canvas.setCurrent(initial);
	}
	
	/**
	 * Pobierz aktualną pozycję
	 * @return
	 */
	public GPSData getCurrent() {
		return current;
	}

	/**
	 * Ustaw cel
	 * @param target
	 */
	public void setTarget(GPSData target) {
		controller.setTarget(target);
	}

	/**
	 * Parsuje komendy i symuluje akcję robota, aktualizuje współrzędne i kierunek
	 * @param commands
	 */
	public void parse(String command) {
		String[] splitted = command.split("\\|");
		System.out.println("Mock: COMMAND: " + command);
		
		Double left = Double.parseDouble(splitted[0]);
		Double right = Double.parseDouble(splitted[1]);
		
		double bearing = getAngle(left, right); 
		
		if(bearing > 0) {
			heading = controller.getHeading();
			rotation.setTargetDirection(bearing);

			System.out.println("Mock: H:" + Math.toDegrees(heading) + " bearing:" +  Math.round(bearing));

			if(!rotation.isAlive()) {
				rotation = new Rotation();
				rotation.setTargetDirection(bearing);
				rotation.start();
			}
		}
		
		if(left > 0.0 && right > 0.0) {
			//if(controller.getHeading() != null) {
				double speed = (left + right) / 2.0;
				
				System.out.println("Mock: AvgPwmSpeed: " + speed);
				
				speed = (RobotController.MAX_SPEED * speed) / 255;
				
				System.out.println("Mock: Speed: " + speed);
				System.out.println("Mock: Current: " + current);
				
				current = current.destinationPointFromDistanceAndBearing(controller.getHeading() != null ? controller.getHeading() : 0, speed);
				
				System.out.println("Mock: New current: " + current);
				
				setCurrent(current);
				// wyznacz kierunek (z korektą kąta)
				/*Point2D.Double direction = Angle.angleToVector(controller.getHeading() - Math.PI/2);

				System.out.println("Mock: Heading: " + Math.toDegrees(controller.getHeading()));

				// ustaw prędkość
				speed /= 300000;
				
				// zaktualizuj pozycję
				Point2D.Double world = projection.fromGeoToWorld(current);
				world.x += direction.x * speed;
				world.y += direction.y * speed;
				current = projection.fromWorldToGeo(world);

				setCurrent(current);
				System.out.println("Mock: Current:" + current + " Direction:" + direction);*/
			/*} else {
				current.setLatitude(current.getLatitude() + 0.000002);
				setCurrent(current);
			}*/
		}
		
		
		/*String[] splitted = command.split("\\|");
		System.out.println("Mock: COMMANDS: " + command);
		for(String command : splitted) {
			switch(Command.valueOf(command.substring(0, 1))) {
			case T: {
				double targetDirection = Double.parseDouble(command.substring(1));
				
				heading = controller.getHeading();
				rotation.setTargetDirection(targetDirection);

				System.out.println("Mock: H:" + Math.toDegrees(heading) + " TD:" +  Math.round(Math.toDegrees(targetDirection)));

				if(!rotation.isAlive()) {
					rotation = new Rotation();
					rotation.setTargetDirection(targetDirection);
					rotation.start();
				}

				break;
			}
			case V:
				double speed = Double.parseDouble(command.substring(1));
				if(speed != 0) {
					if(controller.getHeading() != null) {
						// wyznacz kierunek (z korektą kąta)
						Point2D.Double direction = Angle.angleToVector(controller.getHeading() - Math.PI/2);

						System.out.println("Mock(V): Heading: " + Math.toDegrees(controller.getHeading()));

						// ustaw prędkość
						speed /= 300000;
						
						// zaktualizuj pozycję
						Point2D.Double world = projection.fromGeoToWorld(current);
						world.x += direction.x * speed;
						world.y += direction.y * speed;
						current = projection.fromWorldToGeo(world);

						setCurrent(current);
						System.out.println("Mock(V): Current:" + current + " Direction:" + direction);
					} else {
						current.setLatitude(current.getLatitude() + 0.000002);
						setCurrent(current);
					}
				}
				break;
			default:
				break;
			}
		}*/
	}

	/**
	 * @param left
	 * @param right
	 */
	private double getAngle(Double left, Double right) {
		double distance = controller.getTarget() != null ? 
				current.getDistanceTo(controller.getTarget()) * 100 : 0;

		if(left > right) { // skręt w lewo
			double radius = reconstructRadius(left, right);
			return -reconstructAngle(radius, distance);
			
		} else if(left < right) { // skręt w prawo
			double radius = reconstructRadius(right, left);
			return reconstructAngle(radius, distance);
		}
		
		return 0.0;
	}
	
	/*private double lerp(double a, double b, double step) {
	    return a + step * (b - a);
	}*/
	
	private double reconstructRadius(double a, double b) {
		return (-RobotController.WHEEL_TRACK / 2.0) * (b/a + 1.0) / (b/a - 1.0);
	}
	
	private double reconstructAngle(double radius, double distance) {
		return Math.toRadians((4 * distance) / (radius - 10));
	}
	
	public void tryRestart() {
		controller.tryRestart();
	}
}
