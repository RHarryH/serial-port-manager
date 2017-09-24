package com.navigation;

import com.navigation.serial.GPSSerialPortManager;

import java.util.ArrayList;
import java.util.Arrays;
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
	
	private GPSData lastCurrents[] = new GPSData[3]; // działa na zasadzie bufora cyklicznego
	private int lastCurrentsIndex = 0;
	
	private GPSSerialPortManager spm;
	
	private volatile boolean interrupt = false;
	private double speed = MAX_SPEED_PWM;

	private Double heading = 0.0, desiredAngle;
	
	private int attemptsNo = MAX_ATTEMPTS;
	private boolean ignored = false;
	
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
			if(updatePreviousAndCurrent() == -2  && currentTarget != null) {
				driveStraight();
				continue;
			}

	    	sendCommands();
	    }
		
		stop();
	}

	/**
	 * Aktualizuje poprzedni i aktualny punkt
	 */
	protected int updatePreviousAndCurrent() {
		GPSData receivedData = spm.getGps();

		if(ignoreZerosOnStart(receivedData))
			return -1;

		logger.info("Received data: " + receivedData);

		if(ignoreDistantResult(receivedData) || ignoreEqualResult(receivedData))
			return -2;

		/* jeśli jakakolwiek współrzędna została zignorowana
		 * wyczyść bufor aby nie mieć nieaktualnych danych
		 */
		if(ignored) {
			clearBuffer();
			
			ignored = false;
		}

		previous = current; // zapamietaj aktualna pozycje jako pozycje poprzednia
		
		assignToCurrent(receivedData);
		
		return 0;
	}

	/**
	 * Czyści bufor cykliczny
	 */
	private void clearBuffer() {
		lastCurrents = new GPSData[3];
		lastCurrentsIndex = 0;
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
			ignored = true;
			
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
		if(lastCurrentsIndex > 0) {
			int previousEffectiveIndex = (lastCurrentsIndex - 1) % 3;
			GPSData lastCurrent = lastCurrents[previousEffectiveIndex];

			// jeśli ostatnia znana wartość jest identyczne z otrzymaną to ignorujemy
			// pozwoli to zachować ostatni znany prawidłowy kierunek jazdy robota
			if(lastCurrent != null && lastCurrent.equalsPrecise(receivedData)) {
				ignored = true;
				
				logger.info("Received data is the same as last known value. Ignored");
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Przypisz otrzymaną wartość do zmiennej przechowującą aktualną wartość. Wartość ta jest też
	 * wpisywana do bufora cyklicznego. Jeśli bufor będzie pełny, aktualną wartością będzie średnia
	 * wartości z bufora.
	 * @param receivedData
	 */
	private void assignToCurrent(GPSData receivedData) {
		int effectiveIndex = lastCurrentsIndex % 3;
		lastCurrentsIndex++;
		lastCurrents[effectiveIndex] = new GPSData(receivedData);
		
		if(lastCurrentsIndex < 3)
			current = new GPSData(receivedData); // zastap aktualna pozycje daną z portu szeregowego
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
				logger.info("Distance from current to target: " + current.getDistanceTo(currentTarget) + "m");
				logger.info("Previous: " + previous + " Current: " + current + " Target: " + currentTarget);
				logger.info("Buffer content: " + Arrays.toString(lastCurrents));
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

			} else {
				driveStraight();

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
