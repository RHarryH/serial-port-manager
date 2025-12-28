package simulation.convert;

import com.navigation.GPSData;

public class DataPixelConverter {

	public static final double xMin = 50.8653, 
						 xMax = 50.8656, 
						 yMin = 20.7165, 
						 yMax = 20.7170;
	
	public int convertToPixel(double value, double min, double max, int range) {
		if(inRange(value, min, max))
			return (int)normalize(value, min, max, 0, range);
		
		return -1;
	}
	
	public double convertToGPS(int value, int min, int max, double newMin, double newMax) {
		if(inRange(value, min, max))
			return normalize(value, min, max, newMin, newMax);
		
		return -1;
	}
	
	private boolean inRange(double value, double min, double max) {
	   return (value >= min) && (value <= max);
	}
	
	private double normalize(double value, double min, double max, double newMin, double newMax) {
		return (value - min)/(max - min) * (newMax - newMin) + newMin;
	}

	public static void main(String[] args) {
		GPSData data = new GPSData(50.8653750, 20.7168213);
		
		DataPixelConverter dpc = new DataPixelConverter();
		
		System.out.println("Konwersja punkt -> piksel");
		System.out.println(dpc.convertToPixel(data.getLatitude(), xMin, xMax, 800));
		System.out.println(dpc.convertToPixel(data.getLongitude(), yMin, yMax, 600));
		System.out.println("Konwersja piksel -> punkt");
		System.out.println(dpc.convertToGPS(200, 0, 800, xMin, xMax));
		System.out.println(dpc.convertToGPS(385, 0, 600, yMin, yMax));
	}

}
