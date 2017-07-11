package com.navigation.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import com.navigation.GPSData;
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.VTGSentence;

public class SerialPortManager implements SentenceListener {
	private SerialPort serialPort;
	private final GPSData gps = new GPSData();
	
    /**
     * Ports specified for each OS.
     */
	private static final String PORT_NAMES[] = {
		"/dev/tty.usbserial-A9007UX1", // Mac OS X
		"/dev/ttyUSB0", // Linux
		"COM1", // Windows
	};
	
	private InputStream input;
	private OutputStream output;
	private int TIMEOUT = 2000;
	private int DATARATE = 9600;
	
	/**
	 * Uses default configuration
	 */
	public SerialPortManager() {}
	
	public SerialPortManager(int timeout, int datarate) {
		this.TIMEOUT = timeout;
		this.DATARATE = datarate;
	}

	/**
	 * Find port name for host OS, open it and create read and write buffers.
	 */
	public void initialize() {
	    CommPortIdentifier portId = null;
	    Enumeration<?> portEnum = CommPortIdentifier.getPortIdentifiers();
	
	    // first, find an instance of serial port as set in PORT_NAMES
	    while (portEnum.hasMoreElements()) {
	        CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();

	        for (String portName : PORT_NAMES) {
	            if (currPortId.getName().equals(portName)) {
	    		    System.out.println("Founded port: " + currPortId.getName());
	                portId = currPortId;
	                break;
	            }
	        }
	    }
	    if (portId == null) {
	        System.out.println("Could not find COM port.");
	        return;
	    }
	
	    try {
	        serialPort = (SerialPort) portId.open(this.getClass().getName(), TIMEOUT); // open the port with specified timeout
	        serialPort.setSerialPortParams(DATARATE,
	                SerialPort.DATABITS_8,
	                SerialPort.STOPBITS_1,
	                SerialPort.PARITY_NONE);
	        serialPort.enableReceiveThreshold(1);
	
	        // open the streams
		    input = serialPort.getInputStream();
		    output = serialPort.getOutputStream();
		    
		    SentenceReader reader = new SentenceReader(input);
			reader.addSentenceListener(this, SentenceId.VTG);
			reader.addSentenceListener(this, SentenceId.GGA);
			/*reader.setExceptionListener(new ExceptionListener() {
				
				@Override
				public void onException(Exception e) {
					if(e instanceof IOException) {
						System.err.println("Błąd we/wy (najprawdopodobniej pusty stream");
					}
				}
			});*/
			reader.start();
	    } catch(PortInUseException e) {
	    	System.err.println("Port " + portId.getName() + " is in use.");
	    } catch (Exception e) {
	        System.err.println(e.toString());
	    }
	}
	
	/**
	 * Get data gathered from serial port and decoded by Marine API.
	 * @return simplified structure for storing most important data
	 */
	public GPSData getGps() {
		return gps;
	}
	
	/** 
	 * Send message through serial port
	 * @param command
	 */
	public void sendCommand(String command) {
		if(output != null) {
			try {
				output.write(command.getBytes(Charset.forName("UTF-8")));
			} catch (IOException e) {
				System.err.println("Writing to serial port error.");
			}
		}
	}

	/**
	 * Close the port.
	 */
	public synchronized void close() {
	    if (serialPort != null)
	        serialPort.close();
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
	    SerialPortManager spm = new SerialPortManager();
	    spm.initialize();
	    
	    System.out.println("Started");
	}
}