package com.navigation.serial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.TooManyListenersException;

import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

/**
 * Implementacja uniwersalnego portu szeregowego, który pozwala na odczyt dowolnych danych
 * @author Harry
 *
 */
public class UniversalSerialPortManager extends SerialPortManager implements SerialPortEventListener{
	
	private BufferedReader input;
	
	/**
	 * Implementacja odczytu ze strumienia wejścia
	 */
	@Override
	protected void createInputStream() throws IOException{
		input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
	}

	/**
	 * Implementacja odczytu ze strumienia wejścia
	 */
	@Override
	protected void handleInputStream() {
		try {
			serialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			System.err.println("Za dużo listenerów");
		}
        serialPort.notifyOnDataAvailable(true);
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
	    UniversalSerialPortManager spm = new UniversalSerialPortManager();
	    spm.initialize();

	    System.out.println("Started");
	    
	    Scanner scanner = new Scanner(System.in);
	    
	    while(true) {
	    	String txt = scanner.nextLine();
	    	if(txt.equals("q"))
	    		break;
	    	else {
	    		System.out.println("Text " + txt + " został wysłany.");
	    		spm.sendCommand(txt);
	    	}
	    }
	    
	    scanner.close();
	    
	    spm.close();
	}
}