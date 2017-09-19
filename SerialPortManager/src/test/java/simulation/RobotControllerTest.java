package simulation;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import com.navigation.GPSData;
import com.navigation.RobotController;

public class RobotControllerTest extends RobotController {

	private RobotMock mock;
	
	public RobotControllerTest(RobotMock mock) {
		this.mock = mock;
		
		Thread thread = new Thread(this);
		thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				RobotControllerTest.this.interrupt();
				logger.info("Robot stopped");
				e.printStackTrace();
				System.exit(-1);
			}
		});
		
		thread.start();
	}
	
	/** 
	 * Ustawia aktualną pozycję robota.
	 * @param current
	 */
	public void setCurrent(GPSData newCurrent) {
		this.previous = this.current;
		this.current = newCurrent;
	}

	/**
	 * Ustawia aktualną pozycję robota (użyte podczas inicjalizacji)
	 * @param newCurrent
	 */
	public void setInitialPosition(GPSData newCurrent) {
		this.current = newCurrent;
	}
	
	
	/**
	 * Nie rób nic. Kierunek jest ustalany w wątku obracającym wirtualnego robota.
	 */
	@Override
	protected void setHeading() {

	}

	/**
	 * Pobierz współrzedne aktualnej pozycji
	 * @return
	 */
	public GPSData getCurrent() {
		return current;		
	}
	
	/**
	 * Pobierz współrzędne celu
	 * @return
	 */
	public GPSData getCurrentTarget() {
		return currentTarget;
	}
	
	/**
	 * Pobierz listę celów
	 * @return
	 */
	public List<GPSData> getTargets() {
		return targets;
	}

	/**
	 * Zatrzymuje robota
	 */
	@Override
	public void stop() {
		String command = "0|0"; // rozkaz zatrzymania
		mock.parse(command);
		
		logger.info("Robot stopped");
	}

	/**
	 * Pozycja nie jest aktualizowana automatycznie, należy to zrobić jawnie metodą setCurrent
	 */
	protected int updatePreviousAndCurrent() {
		return 0;
	}

	/**
	 * Wysyła komedę do mocka
	 * @param command
	 */
	@Override
	protected void sendCommand(String command) {
		mock.parse(command);
		logger.info("Command " + command + " was sent\r\n\r\n");
	}
}
