import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.VTGSentence;

public class MarineAPITest implements SentenceListener {

	private SentenceReader reader;

	/**
	 * Creates a new instance of FileExample
	 * 
	 * @param file File containing NMEA data
	 */
	public MarineAPITest(String fileName) throws IOException {

		ClassLoader classLoader = getClass().getClassLoader();
		
		// create sentence reader and provide input stream
		URL resource = classLoader.getResource(fileName);
		if(resource == null) {
			System.err.println("Resource does not exist");
			return;
		}
			
		InputStream stream = new FileInputStream(new File(resource.getFile()));
		reader = new SentenceReader(stream);

		// register self as a listener for GGA sentences
		reader.addSentenceListener(this, SentenceId.RMC);
		reader.addSentenceListener(this, SentenceId.VTG);
		reader.addSentenceListener(this, SentenceId.GGA);
		reader.addSentenceListener(this, SentenceId.GSA);
		reader.addSentenceListener(this, SentenceId.GSV);
		reader.addSentenceListener(this, SentenceId.GLL);
		reader.start();
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		reader.stop();
	}

	public void readingPaused() {
		System.out.println("-- Paused --");
	}

	public void readingStarted() {
		System.out.println("-- Started --");
	}

	public void readingStopped() {
		System.out.println("-- Stopped --");
	}

	public void sentenceRead(SentenceEvent event) {
		try {
			switch(event.getSentence().getSentenceId()) {
			// recommended minimum data for gps 
			/*case "RMC":
				RMCSentence rmc = (RMCSentence) event.getSentence();
	
				System.out.println("RMC: " + rmc.getPosition());
	
				break;*/
			// vector track an Speed over the Ground  (verify for speed)
			case "VTG":
				VTGSentence vtg = (VTGSentence) event.getSentence();
	
				System.out.println("VTG: " + vtg.getSpeedKmh());
				break;
			// fix information (most important!) 
			case "GGA":
				GGASentence gga = (GGASentence) event.getSentence();
	
				System.out.println("GGA: " + gga.getPosition());
				break;
			// overall Satellite data 
			/*case "GSA":
				GSASentence gsa = (GSASentence) event.getSentence();
	
				//System.out.println(gsa.getPosition());
				break;*/
			// detailed Satellite data 
			/*case "GSV":
				GSVSentence gsv = (GSVSentence) event.getSentence();
	
				//System.out.println(gsv.getPosition());
				break;*/
			// lat/lon data
			/*case "GLL":
				GLLSentence gll = (GLLSentence) event.getSentence();
				
				System.out.println("GLL: " + gll.getPosition());
				break;*/
			}
		}catch(DataNotAvailableException e) {
			// this exception is ignored, if data isn't available we print nothing
		}
	}

	/**
	 * Main method takes one command-line argument, the name of the file to
	 * read.
	 * 
	 * @param args Command-line arguments
	 */
	public static void main(String[] args) {

		if (args.length < 1) {
			System.out.println("Wrong number of parameters.");
			System.exit(1);
		}

		for(int i = 0; i < args.length; i++) {
			try {
				System.out.println("Running " + args[i]);
				new MarineAPITest(args[i]);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
