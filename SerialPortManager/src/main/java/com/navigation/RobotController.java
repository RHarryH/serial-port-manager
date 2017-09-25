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
	private static final int MAX_ATTEMPTS = 3; // maksymalna liczba sprawdzeń dystansu
	
	protected GPSData previous, current, currentTarget;
	protected List<GPSData> targets = new ArrayList<GPSData>();
	
	private GPSSerialPortManager spm;
	
	private volatile boolean interrupt = false;
	private double speed = MAX_SPEED_PWM;

	private Double heading = 0.0, desiredAngle;
	
	private int attemptsNo = MAX_ATTEMPTS;
	
	protected Logger logger = new Logger(RobotController.class, "Logs/controller");
	
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

			// jeśli ustawiono cel i odrzucono współrzędną jed prosto 
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

		if(ignoreZerosOnStart(receivedData))
			return;

		logger.info("Received data: " + receivedData);

		if(ignoreDistantResult(receivedData) || ignoreEqualResult(receivedData))
			return;

		previous = current; // zapamietaj aktualna pozycje jako pozycje poprzednia
		current = new GPSData(receivedData);
	}

	/**
	 * Sprawdz czy nie ma jeszcze informacji o obecnej pozycji i czy informacja odczytana z portu szeregowego zawiera same zera
	 * @param receivedData
	 * @return
	 */
	private boolean ignoreZerosOnStart(GPSData receivedData) {
		if(current == null && receivedData.getLatitude() == 0 && receivedData.getLongitude() == 0)
			return true;
		
		return false;
	}

	/** 
	 * Jeśli odległość między punktami jest większa niż 5 metrów - ignoruj.
	 * Jeśli 3 współrzędne pod rząd zostanie zignorowanych to następna zostanie uwzględniona.
	 * Jest to zabezpieczenie przed sytuacją kiedy robot mimo wszystko jechał w poprawnym kierunku
	 * ale GPS przez pewien okres czasu dawał błędne dane. Wtedy robot mógł się zablokować na jednym
	 * kierunku i nigdy nie odzyskać
	 * @param receivedData
	 * @return
	 */
	private boolean ignoreDistantResult(GPSData receivedData) {
		if(current != null && receivedData.getDistanceTo(current) > 5 && attemptsNo > 0) {
			attemptsNo--; // zmniejsz liczbę prób

			logger.info("Distance between current and received is higher than 5m. Ignored");
			return true;
		}
		
		attemptsNo = MAX_ATTEMPTS; // zresetuj liczbę prób
		
		return false;
	}

	/**
	 * Ignoruj wynik jeśli jest on identyczny z poprzednim
	 * @param receivedData
	 * @return
	 */
	private boolean ignoreEqualResult(GPSData receivedData) {
		if(current != null) {
			if(current.equalsPrecise(receivedData)) {

				logger.info("Received data is the same as last known value. Ignored");
				return true;
			}
		}
		
		return false;
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
		
		if(currentTarget != null) { // jeśli jest ustalony aktualny cel
			if(!currentTarget.equals(current)) { // jeśli nie jest on taki sam jak aktualna pozycja
				if(current != null && previous != null) {	
					logger.info("Distance from current to target: " + current.getDistanceTo(currentTarget) + "m");
					logger.info("Previous: " + previous + " Current: " + current + " Target: " + currentTarget);
					logger.info("---------------------");
	
					setHeading();
					logger.info("Heading: " + Math.toDegrees(heading));
					
					desiredAngle = Angle.denormalizeAngle(current.getBearingWith(currentTarget));
					logger.info("Desired angle: " + Math.toDegrees(desiredAngle));
	
					double angleDelta = Math.atan2(Math.sin(desiredAngle - heading), Math.cos(desiredAngle - heading));
					logger.info("Delta: " + Math.toDegrees(angleDelta) + " " + Math.toDegrees(2*Math.PI - angleDelta) + "\n");
					
					double radius = 600 / Math.toDegrees(Math.abs(angleDelta)) + 35;
					radius = Math.min(radius, 500000); // limit to 500 meters
					
					double leftVelocity = 0.0;
					double rightVelocity = 0.0;
					
					if(angleDelta < 0) { // left
						rightVelocity = speed;
						leftVelocity = speed * (radius - WHEEL_TRACK / 2) / (radius + WHEEL_TRACK / 2) - 70;
					} else if(angleDelta > 0) { // right
						leftVelocity = speed;
						rightVelocity = speed * (radius - WHEEL_TRACK / 2) / (radius + WHEEL_TRACK / 2) - 70;
					}
	
					String command = (int)leftVelocity + "|" + (int)rightVelocity;
					logger.info("Command: " + (int)leftVelocity + ", " + (int)rightVelocity + " Radius: " + radius + "cm / " + radius/100 + "m");
					sendCommand(command);
	
				}
			} else { // każd robotowi się zatrzymać jeśli osiągnął cel, poszukaj następnego celu
				logger.info("Target reached!");
				
				String command = "0|0";
				sendCommand(command);

				targets.remove(0);
				if(!targets.isEmpty())
					applyTarget();
				else
					currentTarget = null;

				desiredAngle = null;
			}
		} else { // każ robotowi stać jeśli nie ma zdefiniowanego celu, jeśli cel się pojawi ustaw go jako aktywny
			String command = "0|0"; 
			sendCommand(command);
			
			if(!targets.isEmpty())
				applyTarget();
		}
	}

	/**
	 * Ustawia nowy cel i dodaje 3 sekundową sekwencję rozruchową w celu aktualizacji współrzędnych
	 */
	private void applyTarget() {
		currentTarget = targets.get(0);
		logger.info("Target set: " + currentTarget);
		driveStraight();
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Rozkaz jazdy prosto
	 */
	private void driveStraight() {
		sendCommand("255|255"); // każ robotowi jechać prosto
	}

	/**
	 * Ustawia kierunek. Wydzielone do funkcji aby można było kontrolować tą wartość takeże w testach.
	 */
	protected void setHeading() {
		setHeading(Angle.denormalizeAngle(previous.getBearingWith(current)));
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
