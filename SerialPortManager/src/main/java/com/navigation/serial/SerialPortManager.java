package main.java.com.navigation.serial;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener;
/*import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;*/

import java.util.Enumeration;


public class SerialPortManager implements SerialPortEventListener/*, SentenceListener*/ {
	private SerialPort serialPort;
	
    /**
     * Ports specified for each OS.
     */
	private static final String PORT_NAMES[] = {
		"/dev/tty.usbserial-A9007UX1", // Mac OS X
		"/dev/ttyUSB0", // Linux
		"COM1", // Windows
	};
	
	//private InputStream input;
	private BufferedReader input;
	private OutputStream output;
	private static final int TIMEOUT = 2000;
	private static final int DATARATE = 9600;

	/**
	 * Find port name for host OS, open it and create read and write buffers.
	 */
	public void initialize() {
	    CommPortIdentifier portId = null;
	    Enumeration<?> portEnum = CommPortIdentifier.getPortIdentifiers();
	
	    // first, find an instance of serial port as set in PORT_NAMES
	    while (portEnum.hasMoreElements()) {
	        CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
		    
		    System.out.println(currPortId.getName());
	        for (String portName : PORT_NAMES) {
	            if (currPortId.getName().equals(portName)) {
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
	
	        // open the streams
	        input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
	        output = serialPort.getOutputStream();
	
	        serialPort.addEventListener(this);
	        serialPort.notifyOnDataAvailable(true);
	        serialPort.enableReceiveThreshold(1);
	        
		    /*input = serialPort.getInputStream();
		    SentenceReader sr = new SentenceReader(input);
			sr.addSentenceListener(this);
			sr.start();*/
	    } catch (Exception e) {
	        System.err.println(e.toString());
	    }
	}

	/**
	 * Close the port.
	 */
	public synchronized void close() {
	    if (serialPort != null) {
	        serialPort.removeEventListener();
	        serialPort.close();
	    }
	}

	public SerialPort getSerialPort() {
		return serialPort;
	}

	/*@Override
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
		System.out.println(event.getSentence());
	}*/

	/**
	 * This event is responsible for reading the buffer every time when
	 * data is available.
	 */
	public synchronized void serialEvent(SerialPortEvent event) {
	    if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
	        try {
	            String inputLine = null;
	            if (input.ready()) {
	                inputLine = input.readLine();
	                System.out.println(inputLine);
	            }
	
	        } catch (Exception e) {
	            System.err.println(e.toString());
	        }
	    }
	    // ignore all the other eventTypes, but the other ones should be considered (see rxtx docs)
	}

	/**
	 * Test class
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
	    SerialPortManager spm = new SerialPortManager();
	    spm.initialize();
	    
	    // create new thread
	    Thread t = new Thread() {
	        public void run() {
	            // The following line will keep this app alive for 1000 seconds,
	            // waiting for events to occur and responding to them (printing incoming messages to console).
	            try {
	            	Thread.sleep(1000000);
	            } catch (InterruptedException ie) {
	            	System.err.println("Thread was interrupted");
	            }
	        }
	    };
	    t.start();
	    
	    System.out.println("Started");
	}
}