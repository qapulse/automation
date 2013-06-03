package com.littleinc.MessageMe.net;

import java.util.Comparator;

import android.location.Location;

import com.littleinc.MessageMe.util.LocationUtil;

public class FacebookPlaceComparator implements Comparator<FacebookPlace> {
	Location from;

	public FacebookPlaceComparator(Location from) {
		this.from = from;
	}

	@Override
	public int compare(FacebookPlace lhs, FacebookPlace rhs) {
		Double leftDistance = LocationUtil.distance(
				from.getLatitude(),
				from.getLongitude(),
				lhs.getLocation().latitude,
				lhs.getLocation().longitude);
		
		Double rightDistance = LocationUtil.distance(
				from.getLatitude(),
				from.getLongitude(),
				rhs.getLocation().latitude,
				rhs.getLocation().longitude);
		
		return leftDistance.compareTo(rightDistance);
	}

}
