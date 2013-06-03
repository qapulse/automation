package com.littleinc.MessageMe.util;

import java.util.Locale;

public class ConversionUtil {
	public static enum UnitLocale {
		Imperial, Metric
	}

	/**
	 * 1km is 0.621371192 miles
	 */
	public static double KILOMETERS_TO_MILES = 0.621371192;
	public static double KILOMETERS_TO_FEETS = 3280.84;

	public static double kilometerToMiles(double kilometers) {
		return kilometers * KILOMETERS_TO_MILES;
	}

	public static double kilometerToFeets(double kilometers) {
		return kilometers * KILOMETERS_TO_FEETS;
	}

	public static UnitLocale getCurrent() {
		String countryCode = Locale.getDefault().getCountry();
		if ("US".equals(countryCode))
			return UnitLocale.Imperial; // USA
		if ("LR".equals(countryCode))
			return UnitLocale.Imperial; // liberia
		if ("MM".equals(countryCode))
			return UnitLocale.Imperial; // burma
		return UnitLocale.Metric;
	}
}
