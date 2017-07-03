package main.java.com.navigation;

import net.sf.marineapi.nmea.util.Position;

public class GPSData {
	private double latitude, // szerokosc geogr.
				   longitude, // dlugosc geogr.
				   altitude, // wysokosc
				   speed; // predkosc (kmh)
	
	public GPSData() {}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setPosition(Position position) {
		latitude = position.getLatitude();
		longitude = position.getLongitude();
		altitude = position.getAltitude();
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}
}
