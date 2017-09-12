package com.navigation;

import com.navigation.serial.GPSSerialPortManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.navigation.GPSData;
import com.navigation.algorithm.Angle;

public class RobotController implements Runnable {

	public static final double MAX_SPEED_PWM = 255; // 20 m/min
	public static final double MAX_SPEED = 0.33; // 20 m/min (0.33 m/s)
	public static final double WHEEL_TRACK = 15; // rozstaw kół, 15 cm
	
	protected GPSData previous, current, currentTarget;
	protected List<GPSData> targets = new ArrayList<GPSData>();
	
	private GPSData lastCurrents[] = new GPSData[3]; // działa na zasadzie bufora cyklicznego
	private int lastCurrentsIndex = 0;
	
	private GPSSerialPortManager spm;
	
	private volatile boolean interrupt = false;
	private double speed = MAX_SPEED_PWM;

	private Double heading = 0.0, desiredAngle;
	
	protected Logger logger = new Logger(RobotController.class, "Logs/controller.txt");
	
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
	public void addTarget(GPSData target) {
		this.targets.add(target);
		logger.info("Target added: " + target);
	}
	
	/**
	 * Czyści wszystkie cele i zatrzymuje robota
	 */
	public void clearTargets() {
		targets.clear();
		
		currentTarget = null;
		desiredAngle = null;
		previous = null;
		
		String command = "0|0"; // rozkaz zatrzymania
		spm.sendCommand(command);
	}

	/**
	 * Ustawia maksymalną prędkość pojazdu (i przycina do zakresu 160 .. 255)
	 * @param speed
	 */
	public void setSpeed(double speed) {
		this.speed = Math.min(Math.max(speed, 160), 255);
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
		
		logger.info("Robot stopped");
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
		GPSData receivedData = spm.getGps();
		
		// sprawdz czy nie ma jeszcze informacji o obecnej pozycji i czy informacja odczytana z portu szeregowego zawiera same zera
		if(current == null && receivedData.getLatitude() == 0 && receivedData.getLongitude() == 0)
			return;

		logger.info("Received data: " + receivedData);
		
		if(current != null && receivedData.getDistanceTo(current) > 5) {
			logger.info("Distance between current and received is higher than 5m!");
			return;
		}

		previous = current; // zapamietaj aktualna pozycje jako pozycje poprzednia
		
		int effectiveIndex = lastCurrentsIndex % 3;
		lastCurrentsIndex++;
		lastCurrents[effectiveIndex] = receivedData;
		
		if(lastCurrentsIndex < 3)
			current = receivedData; // zastap aktualna pozycje daną z portu szeregowego
		else {
			current = average(lastCurrents);
			logger.info("Average current: " + current);
		}
	}
	
	/**
	 * Wyznacza średnią 3 ostatnich znanych pozycji
	 * @param lastCurrents
	 * @return
	 */
	private GPSData average(GPSData[] lastCurrents) {
		double lat = 0, lon = 0;
		
		for(GPSData lastCurrent : lastCurrents) {
			lat += lastCurrent.getLatitude();
			lon += lastCurrent.getLongitude();
		}
		
		lat /= lastCurrents.length;
		lon /= lastCurrents.length;
		
		return new GPSData(lat, lon);
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
		
		if(current != null && currentTarget != null && !current.equals(currentTarget)) {
			if(previous != null) {	
				//if(heading == null)
				setHeading();
				
				logger.info("Heading: " + Math.toDegrees(heading));
				
				desiredAngle = Angle.denormalizeAngle(current.getBearingWith(currentTarget));
				logger.info("Previous: " + previous + " Current: " + current + " Target: " + currentTarget);
				logger.info("Desired angle: " + Math.toDegrees(desiredAngle));

				double angleDelta = Math.atan2(Math.sin(desiredAngle - heading), Math.cos(desiredAngle - heading));
				logger.info("Delta: " + Math.toDegrees(angleDelta) + " " + Math.toDegrees(2*Math.PI - angleDelta) + "\n");
				
				double radius = 600 / Math.toDegrees(Math.abs(angleDelta)) + 35;
				radius = Math.min(radius, 500000); // limit to 500 meters
				
				double leftVelocity = 0.0;
				double rightVelocity = 0.0;
				
				if(angleDelta < 0) { // left
					rightVelocity = speed;
					leftVelocity = speed * (radius - WHEEL_TRACK / 2) / (radius + WHEEL_TRACK / 2);
				} else if(angleDelta > 0) { // right
					leftVelocity = speed;
					rightVelocity = speed * (radius - WHEEL_TRACK / 2) / (radius + WHEEL_TRACK / 2);
				}

				String command = (int)leftVelocity + "|" + (int)rightVelocity;
				logger.info("Command: " + (int)leftVelocity + ", " + (int)rightVelocity + " Radius: " + radius + "cm / " + radius/100 + "m");
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

			if(!targets.isEmpty()) {
				currentTarget = targets.get(0);
				targets.remove(0);
			}
		}
	}

	/**
	 * Ustawia kierunek. Wydzielone do funkcji aby można było kontrolować tą wartość takeże w testach.
	 */
	protected void setHeading() {
		setHeading(Angle.denormalizeAngle(previous.getBearingWith(current)));
	}
	
	/*private double normalize(double speed) {
		return (speed - 0) * 255 / ( MAX_SPEED - 0 ) + 0;
	}*/

	/**
	 * Dokonuje próby zresetowania ustawień robota.
	 */
	public void tryRestart() {
		if(current != null && current.equals(currentTarget)) {
			currentTarget = null;
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
		logger.info("Command " + command + " was sent\r\n\r\n");
	}
}
