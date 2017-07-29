package com.navigation.serial;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import com.navigation.GPSData;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.ExceptionListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.VTGSentence;

/**
 * Implementacja portu szeregowego pozwalającego na odczyt danych GPS (poprzez Marine API)
 * @author Harry
 *
 */
public class GPSSerialPortManager extends SerialPortManager implements SentenceListener {
	private final GPSData gps = new GPSData();
	private InputStream input;
	
	protected void createInputStream() throws IOException {
		input = serialPort.getInputStream();
	}

	/**
	 * Implementacja odczytu ze strumienia wejścia dla danych GPS
	 */
	@Override
	protected void handleInputStream() {
		SentenceReader reader = new SentenceReader(input);
		reader.addSentenceListener(this, SentenceId.VTG);
		reader.addSentenceListener(this, SentenceId.GGA);
		reader.setExceptionListener(new ExceptionListener() {
			
			@Override
			public void onException(Exception e) {
				if(e instanceof IOException) {
					System.err.println("Błąd we/wy (najprawdopodobniej pusty stream");
				}
			}
		});
		
		reader.start();
	}

	/**
	 * Get data gathered from serial port and decoded by Marine API.
	 * @return simplified structure for storing most important data
	 */
	public GPSData getGps() {
		return gps;
	}
	
	@Override
	public void readingPaused() {
		System.out.println("-- Paused --");
	}

	@Override
	public void readingStarted() {
		System.out.println("-- Started --");
	}

	@Override
	public void readingStopped() {
		System.out.println("-- Stopped --");
	}

	@Override
	public void sentenceRead(SentenceEvent event) {
		try {
			switch(event.getSentence().getSentenceId()) {
			// vector track an Speed over the Ground
			case "VTG":
				VTGSentence vtg = (VTGSentence) event.getSentence();
				
				gps.setSpeed(vtg.getSpeedKmh());
				break;
			// fix information
			case "GGA":
				GGASentence gga = (GGASentence) event.getSentence();
				
				gps.setPosition(gga.getPosition());
				break;
			}
		}catch(DataNotAvailableException e) {
			// this exception is ignored, if data isn't available we print nothing
		}
	}

	/**
	 * Test class
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
	    GPSSerialPortManager spm = new GPSSerialPortManager();
	    spm.initialize();

	    System.out.println("Started");
	    
	    // odczytuje co sekundę stan zmiennej przechowującej dane GPS
	    while(true) {
	    	System.out.println(spm.getGps());
	    	TimeUnit.SECONDS.sleep(1);
	    }
	    
	    //spm.close();
	}
}