package com.navigation.serial;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

/**
 * Podstawowa klasa obsługi portu szeregowego. Pozwala na inicjalizację i zamykanie portu oraz wysyłanie
 * przez niego danych.
 * @author Harry
 *
 */
public abstract class SerialPortManager {
	protected SerialPort serialPort;
	
    /**
     * Ports specified for each OS.
     */
	private static final String PORT_NAMES[] = {
		"/dev/tty.usbserial-A9007UX1", // Mac OS X
		"/dev/ttyUSB0", // Linux
		"COM6", // Windows
	};

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
	        //serialPort.enableReceiveThreshold(1); // powodowało błąd przy odczycie przez radio (?)
	
	        // open the streams
	        createInputStream();
		    output = serialPort.getOutputStream();
		    
		    handleInputStream();
	    } catch(PortInUseException e) {
	    	System.err.println("Port " + portId.getName() + " is in use.");
	    } catch (Exception e) {
	        System.err.println(e.toString());
	    }
	}
	
	protected abstract void createInputStream() throws IOException;
	protected abstract void handleInputStream();

	/** 
	 * Send message through serial port
	 * @param command
	 */
	public void sendCommand(String command) {
		if(output != null) {
			try {
				output.write(command.getBytes(Charset.forName("UTF-8")));
				output.flush();
			} catch (IOException e) {
				System.err.println("Writing to serial port error.");
			}
		}
	}
	
	public synchronized void close() {
		if (serialPort != null) {
	        serialPort.close();
	    }
	};
}