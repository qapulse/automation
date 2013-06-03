package com.littleinc.MessageMe.util;

public class LocationUtil {
	public static double distance(double latitudePoint1,
			double longituedPoint1, double latitudePoint2,
			double longitudePoint2) {
		double EARTH_RADIUS = 6371.0; // wikipedia
										// http://en.wikipedia.org/wiki/Earth_radius
										// median radius

		double deltalat = Math.toRadians(latitudePoint2 - latitudePoint1);
		double deltalon = Math.toRadians(longitudePoint2 - longituedPoint1);

		double a = Math.sin(deltalat / 2) * Math.sin(deltalat / 2)
				+ Math.cos(latitudePoint1) * Math.cos(latitudePoint2)
				* Math.sin(deltalon / 2) * Math.sin(deltalon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = EARTH_RADIUS * c;

		return distance;
	}
}
