package com.navigation.algorithm;

import java.awt.geom.Point2D;

import com.navigation.GPSData;

public class Angle {
	
	/**
	 * Ustala kąt (w radianach) pomiędzy dwoma lokacjami
	 * @param a
	 * @param b
	 * @return
	 */
	public static double getBearing(GPSData a, GPSData b) {
		double lat1 = Math.toRadians(a.getLatitude());
		double lat2 = Math.toRadians(b.getLatitude());
		
		double longitudeDelta = Math.toRadians(b.getLongitude() - a.getLongitude());
		
		double y = Math.sin(longitudeDelta) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - 
				Math.sin(lat1) * Math.cos(lat2) * Math.cos(longitudeDelta);

		double direction = Math.atan2(y, x);
		
		return direction;
	}
	
	/**
	 * Zamienia kat (w radianach) na wektor
	 * @param angle
	 * @return
	 */
	public static Point2D.Double angleToVector(double angle) {
		return new Point2D.Double(Math.cos(angle), Math.sin(angle));
	}
	
	/**
	 * Konwertuje kat do zakresu 0 .. 2PI
	 * @param angle kat podany w radianach (w zakresie -PI .. PI)
	 * @return
	 */
	public static double denormalizeAngle(double angle) {
		return (angle + 2 * Math.PI) % (2 * Math.PI);
	}

	/**
	 * Konwertuje kat do zakresu 0 .. 360
	 * @param angle kat podany w stopniach (w zakresie -180 .. 180)
	 * @return
	 */
	public static double denormalizeAngleDeg(double angle) {
		//angle = 360 - angle;
		
		return (angle + 360) % 360;
	}
	
	/**
	 * Konwertuje kat do zakresu -PI .. PI
	 * @param angle kat podany w radianach (w zakresie 0 .. 2PI)
	 * @return
	 */
	public static double normalizeAngle(double angle) {
		double newAngle = angle;
	    while (newAngle <= -Math.PI) newAngle += 2 * Math.PI;
	    while (newAngle > Math.PI) newAngle -= 2 * Math.PI;
	    return newAngle;
	}

	/**
	 * Konwertuje kat do zakresu -180 .. 180
	 * @param angle kat podany w stopniach (w zakresie 0 .. 360)
	 * @return
	 */
	public static double normalizeAngleDeg(double angle) {
		double newAngle = angle;
	    while (newAngle <= -180) newAngle += 360;
	    while (newAngle > 180) newAngle -= 360;
	    return newAngle;
	}
}
