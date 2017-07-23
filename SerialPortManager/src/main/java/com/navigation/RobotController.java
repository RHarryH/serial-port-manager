package com.navigation;

import com.navigation.serial.SerialPortManager;

import static com.navigation.algorithm.Command.*;

import java.util.concurrent.TimeUnit;

import com.navigation.GPSData;
import com.navigation.algorithm.Angle;

public class RobotController implements Runnable {

	protected static final double SPEED = 1.0; // prędkość domyślna w kmh
	protected GPSData previous, current, target;
	private SerialPortManager spm;
	private volatile boolean interrupt = false;

	private Double heading, desiredAngle;
	
	public RobotController() {
		initSerialPort();
	}

	/**
	 * Inicjalizuje port szeregowy
	 */
	private void initSerialPort() {
		spm = new SerialPortManager();
		spm.initialize();
	}
	
	/**
	 * Ustawia nowy punkt docelowy. Musi on być znany przed uruchomieniem metody run.
	 * @param target
	 */
	public void setTarget(GPSData target) {
		this.target = target;
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
		String command = V.getMemonic() + "0"; // rozkaz zatrzymania
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
					setHeading(Angle.denormalizeAngle(Angle.getBearing(previous, current)));
				
				System.out.println("\nController: Heading: " + Math.toDegrees(heading));
				
				desiredAngle = Angle.denormalizeAngle(Angle.getBearing(current, target));
				System.out.println("Controller: Previous: " + previous + " Current: " + current + " Target: " + target);
				System.out.println("Controller: Desired angle: " + Math.toDegrees(desiredAngle));

				double angleDelta = Math.atan2(Math.sin(desiredAngle - heading), Math.cos(desiredAngle - heading));
				System.out.println("Controller: Delta: " + Math.toDegrees(angleDelta) + " " + Math.toDegrees(2*Math.PI - angleDelta) + "\n");
				
				String commands = T.getMemonic() + angleDelta + "|" + V.getMemonic() + SPEED;
				sendCommand(commands);

			} else {
				String command = V.getMemonic() + SPEED; // każ robotowi jechać prosto
				sendCommand(command);

				tryRestart();
			}
		} else {
			String command = V.getMemonic() + "0"; // każ robotowi sie zatrzymac
			sendCommand(command);
			
			tryRestart();
		}
	}

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
