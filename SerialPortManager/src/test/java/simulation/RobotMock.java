package simulation;

import java.awt.geom.Point2D;
import java.util.concurrent.TimeUnit;

import com.navigation.GPSData;
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
	
	private Canvas canvas;
	private Projection projection = new Projection(20);
	private RobotControllerTest controller;
	
	private Double heading;
	private double counter;
	
	private GPSData current;

	private boolean rotating = false;

	public RobotMock(GPSData initial, Canvas canvas) {
		this.canvas = canvas;
		this.controller = new RobotControllerTest(this);
		this.canvas.setMock(this);
		
		this.controller.setCurrentInitial(initial);
		this.canvas.setInitialCurrent(initial);
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
		controller.setHeading(heading);
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
		this.current = current;
		canvas.setCurrent(current);
		controller.setCurrent(current);
	}
	
	/**
	 * Ustaw początkową pozycję
	 * @param initial
	 */
	public void setCurrentInitial(GPSData initial) {
		this.current = initial;
		canvas.setCurrent(initial);
		controller.setCurrentInitial(initial);
	}

	/**
	 * Ustaw cel
	 * @param target
	 */
	public void setTarget(GPSData target) {
		controller.setTarget(target);
	}

	/**
	 * Sprawdź czy robot jest w trakcie obracania
	 * @return
	 */
	private synchronized boolean isRotating() {
		return rotating;
	}
	
	/**
	 * Ustaw czy robot aktualnie się obraca
	 * @param rotating
	 */
	private synchronized void setRotating(boolean rotating) {
		this.rotating = rotating;
	}
	
	/**
	 * Parsuje komendy i symuluje akcję robota, aktualizuje współrzędne i kierunek
	 * @param commands
	 */
	public void parse(String commands) {
		String[] splitted = commands.split("\\|");
		for(String command : splitted) {
			switch(Command.valueOf(command.substring(0, 1))) {
			case T: {
				double targetDirection = Double.parseDouble(command.substring(1));

				if(!isRotating()) {
					heading = controller.getHeading();
					
					System.out.println("Mock: H:" + Math.toDegrees(heading) + " TD:" +  Math.round(Math.toDegrees(targetDirection)));
					
					// jeśli robot obrócił się o zadany kąt, nie rób tego ponownie
					if(controller.getDesiredAngle() == heading)return;

					setRotating(true);
					counter = 0;
					
					// uruchamia wątek odpowiedzialny za symulację obrotu
					new Thread() {
	
						@Override
						public void run() {
							while(counter < 1) {
								counter += 0.1f;
								if(counter > 1)counter = 1;
								//System.out.println("Mock(Thread): C: " + counter);
	
								canvas.setHeading(heading + lerp(0, targetDirection, counter));

								try {
									TimeUnit.MILLISECONDS.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							heading += targetDirection;
							canvas.setHeading(heading);
							System.out.println("Mock(Thread): H:" + Math.toDegrees(heading) + " TD:" +  Math.round(Math.toDegrees(targetDirection)));
							setRotating(false);
						}
	
					}.start();
				}

				break;
			}
			case V:
				double speed = Double.parseDouble(command.substring(1));
				if(speed != 0) {
					if(controller.getHeading() != null) {
						// wyznacz kierunek (z korektą kąta)
						Point2D.Double direction = Angle.angleToVector(Angle.normalizeAngle(controller.getHeading() - Math.PI/2));

						System.out.println("Mock(V): Heading: " + Math.toDegrees(controller.getHeading()));

						// ustaw prędkość
						speed /= 100000;
						
						// zaktualizuj pozycję
						Point2D.Double world = projection.fromGeoToWorld(current);
						world.x += direction.x * speed;
						world.y += direction.y * speed;
						current = projection.fromWorldToGeo(world);

						setCurrent(current);
						System.out.println("Mock(V): Current:" + current + " Direction:" + direction);
					} else {
						current.setLatitude(current.getLatitude() + 0.00001);
						setCurrent(current);
					}
				}
				break;
			default:
				break;
			}
		}
	}
	
	private double lerp(double a, double b, double step) {
	    return a + step * (b - a);
	}
	
	public void tryRestart() {
		controller.tryRestart();
	}
}
