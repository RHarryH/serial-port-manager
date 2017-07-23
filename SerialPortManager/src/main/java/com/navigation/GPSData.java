package com.navigation;

import net.sf.marineapi.nmea.util.Position;

public class GPSData {
	private static final double EPS = 0.000005; // błąd
	
	private double latitude, // szerokosc geogr.
				   longitude, // dlugosc geogr.
				   altitude, // wysokosc
				   speed; // predkosc (kmh)
	
	@Override
	public String toString() {
		return "[latitude=" + latitude + ", longitude=" + longitude + "]";
	}

	public GPSData() {}
	
	public GPSData(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public GPSData(GPSData data) {
		this.latitude = data.getLatitude();
		this.longitude = data.getLongitude();
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GPSData other = (GPSData) obj;
		
		// równanie okręgu
		// sprawdza czy punkt jest w określonym przez EPS promieniu
		double x = Math.pow(latitude - other.latitude, 2.0);
		double y = Math.pow(longitude - other.longitude, 2.0);
		
		//System.out.println("GPSData: Equals:" + (x + y) + " ? " + EPS*EPS);
		
		if(x + y < EPS*EPS)
			return true;

		return false;
	}
	
	public static void main(String args[]) {
		GPSData a = new GPSData();
		a.setPosition(new Position(50.8653772, 20.7168326));
		
		GPSData b = new GPSData();
		b.setPosition(new Position(50.8653774, 20.7168330));
		
		System.out.println(a.equals(b));
	}
}
