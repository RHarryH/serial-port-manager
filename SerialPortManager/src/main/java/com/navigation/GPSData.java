package com.navigation;

import net.sf.marineapi.nmea.util.Position;

public class GPSData {
	private static final double EPS_PRECISE = 0.00000001; // błąd przy dokładnym porównaniu
	private static final double R = 6371e3; // promien Ziemi w metrach
	
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

	public GPSData(Position data) {
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

	/**
	 * Ustala odległość (w metrach) pomiędzy dwoma lokacjami
	 * @param target współrzędna GPS, z którą mamy wyznaczyć odległość
	 * @return
	 */
    public double getDistanceTo(GPSData target) {
    	double lat1 = Math.toRadians(this.getLatitude());
		double lat2 = Math.toRadians(target.getLatitude());
    
		double latitudeDelta = lat2 - lat1;
		double longitudeDelta =  Math.toRadians(target.getLongitude() - this.getLongitude());
    
		double a = Math.pow(Math.sin(latitudeDelta / 2.0), 2.0) + 
				Math.cos(lat1) * Math.cos(lat2) * 
				Math.pow(Math.sin(longitudeDelta) / 2.0, 2.0);
		
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		
		return R * c;
    }
    
    /**
     * Współrzędne punktu bazując na kierunku i odległości
     * @param bearing
     * @param distance odległość w km
     * @return
     */
    public GPSData destinationPointFromDistanceAndBearing(double bearing, double distance) {
    	double lat1 = Math.toRadians(this.getLatitude());
    	double lon1 = Math.toRadians(this.getLongitude());
    	
    	double dR = distance/R;
    	
		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dR) + 
    						Math.cos(lat1) * Math.sin(dR) * Math.cos(bearing));
    	
    	double lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(dR) * Math.cos(lat1), 
    			Math.cos(dR) - Math.sin(lat1) * Math.sin(lat2));
    	
    	return new GPSData(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }
    
    /**
	 * Ustala kąt (w radianach) pomiędzy dwoma lokacjami
	 * @param second współrzędna GPS, z którą mamy wyznaczyć kąt
	 * @return
	 */
    public double getBearingWith(GPSData second) {
		double lat1 = Math.toRadians(this.getLatitude());
		double lat2 = Math.toRadians(second.getLatitude());
		
		double longitudeDelta = Math.toRadians(second.getLongitude() - this.getLongitude());
		
		double y = Math.sin(longitudeDelta) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - 
				Math.sin(lat1) * Math.cos(lat2) * Math.cos(longitudeDelta);

		double direction = Math.atan2(y, x);
		
		return direction;
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

		// jeśli odległośc między punktami jest mniejsza niż 3 metry
		// zakładamy, że punkty są równe
		if(this.getDistanceTo(other) < 3)
			return true;

		return false;
	}
	
	public boolean equalsPrecise(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GPSData other = (GPSData) obj;
		
		if(Math.abs(this.latitude - other.latitude) < EPS_PRECISE &&
		   Math.abs(this.longitude - other.longitude) < EPS_PRECISE)
			return true;

		return false;
	}
	
	public static void main(String args[]) {
		GPSData a = new GPSData();
		a.setPosition(new Position(50.8653772, 20.7168326));
		
		GPSData b = new GPSData();
		b.setPosition(new Position(50.8653774, 20.7168330));
		
		GPSData c = new GPSData();
		c.setPosition(new Position(50.8653772, 20.668126666666666));
		
		GPSData d = new GPSData();
		d.setPosition(new Position(50.8653772, 20.668125));
		
		System.out.println(a.equals(b));
		System.out.println(c + "==" + d);
		System.out.println(c.equalsPrecise(d));
	}
}
