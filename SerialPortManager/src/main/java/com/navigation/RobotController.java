package com.navigation;

import com.navigation.serial.GPSSerialPortManager;

import java.util.concurrent.TimeUnit;

import com.navigation.GPSData;
import com.navigation.algorithm.Angle;

public class RobotController implements Runnable {

	public static final double MAX_SPEED_PWM = 255; // 20 m/min
	public static final double MAX_SPEED = 0.33; // 20 m/min (0.33 m/s)
	public static final double WHEEL_TRACK = 15; // rozstaw kół, 15 cm
	
	protected GPSData previous, current, target;
	private GPSSerialPortManager spm;
	private volatile boolean interrupt = false;
	private double speed = MAX_SPEED_PWM;

	private Double heading, desiredAngle;
	
	public RobotController() {
		initSerialPort();
	}

	/**
	 * Inicjalizuje port szeregowy
	 */
	private void initSerialPort() {
		spm = new GPSSerialPortManager();
		spm.initialize();
	}
	
	/**
	 * Pobiera pozycję 
	 * @return
	 */
	public GPSData getCurrent() {
		return current;
	}
	
	/**
	 * Ustawia nowy punkt docelowy. Musi on być znany przed uruchomieniem metody run.
	 * @param target
	 */
	public void setTarget(GPSData target) {
		this.target = target;
	}

	/**
	 * Ustawia maksymalną prędkość pojazdu (i przycina do zakresu 120 .. 255)
	 * @param speed
	 */
	public void setSpeed(double speed) {
		this.speed = Math.min(Math.max(speed, 120), 255);
	}

	/**
	 * Przerywa pracę
	 */
	public void interrupt() {
		interrupt = true;
	}
	
	/**
	 * Zatrzymuje robota
	 */
	protected void stop() {
		String command = "0|0"; // rozkaz zatrzymania
		spm.sendCommand(command);
		// zamknij port
		spm.close();
		
		System.out.println("Robot stopped");
	}
	
	/**
	 * Główna metoda wątku
	 * @throws InterruptedException
	 */
	public void run() {
		sendCommands();
		
		while(!interrupt) {
			try {
				TimeUnit.MILLISECONDS.sleep(1000); // odczekaj sekundę
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 

			updatePreviousAndCurrent();

	    	sendCommands();
	    }
		
		stop();
	}

	/**
	 * Aktualizuje poprzedni i aktualny punkt
	 */
	protected void updatePreviousAndCurrent() {
		previous = current; // zapamietaj aktualna pozycje jako pozycje poprzednia
		current = spm.getGps(); // zastap aktualna pozycje daną z portu szeregowego
	}
	
	public Double getHeading() {
		return heading;
	}

	public void setHeading(Double heading) {
		this.heading = heading;
	}

	public Double getDesiredAngle() {
		return desiredAngle;
	}

	/**
	 * Dokonuje obliczeń, buduje komendy i wysyła je do robota
	 */
	private void sendCommands() {
		
		if(target != null && !current.equals(target)) {
			if(previous != null) {	
				if(heading == null)
					setHeading(Angle.denormalizeAngle(previous.getBearingWith(current)));
				
				System.out.println("\nController: Heading: " + Math.toDegrees(heading));
				
				desiredAngle = Angle.denormalizeAngle(current.getBearingWith(target));
				System.out.println("Controller: Previous: " + previous + " Current: " + current + " Target: " + target);
				System.out.println("Controller: Desired angle: " + Math.toDegrees(desiredAngle));

				double angleDelta = Math.atan2(Math.sin(desiredAngle - heading), Math.cos(desiredAngle - heading));
				System.out.println("Controller: Delta: " + Math.toDegrees(angleDelta) + " " + Math.toDegrees(2*Math.PI - angleDelta) + "\n");
				
				double radius = (4 * (current.getDistanceTo(target) * 100) / Math.toDegrees(Math.abs(angleDelta))) + 10;
				radius = Math.min(radius, 300000); // limit to 300 meters
				
				double leftVelocity = 0.0;
				double rightVelocity = 0.0;
				
				if(angleDelta < 0) { // left
					rightVelocity = speed;
					leftVelocity = speed * (1.0 - WHEEL_TRACK / (2 * radius));
					
					leftVelocity = Math.max(leftVelocity, 120);
				} else if(angleDelta > 0) { // right
					leftVelocity = speed;
					rightVelocity = speed * (1.0 - WHEEL_TRACK / (2 * radius));
					
					rightVelocity = Math.max(rightVelocity, 120);
				}

				//String command = T.getMemonic() + angleDelta + "|" + V.getMemonic() + MAX_SPEED;
				String command = leftVelocity + "|" + rightVelocity;
				System.out.println("Controller: Command: " + leftVelocity + ", " + rightVelocity + " Radius: " + radius + "cm / " + radius/100 + "m");
				sendCommand(command);

			} else {
				String command = "255|255"; // każ robotowi jechać prosto
				sendCommand(command);

				tryRestart();
			}
		} else {
			String command = "0|0"; // każ robotowi sie zatrzymac
			sendCommand(command);
			
			tryRestart();
		}
	}
	
	/*private double normalize(double speed) {
		return (speed - 0) * 255 / ( MAX_SPEED - 0 ) + 0;
	}*/

	/**
	 * Dokonuje próby zresetowania ustawień robota.
	 */
	public void tryRestart() {
		if(current.equals(target)) {
			target = null;
			desiredAngle = null;
			previous = null;
		}
	}

	/**
	 * Wysyła komendę przez port szeregowy
	 * @param command
	 */
	protected void sendCommand(String command) {
		spm.sendCommand(command);
	}
}
