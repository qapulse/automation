package com.littleinc.MessageMe.bo;


public class CountryCode {

    private String countryName;

    private String countryShortName;

    private int countryCode;

    public CountryCode() { };
    
    public CountryCode(String countryName, String countryID, int countryCode) {
        this.countryName = countryName;
        this.countryShortName = countryID;
        this.countryCode = countryCode;
    }
    
    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public int getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(int countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryInitial() {
        return String.valueOf(countryName.charAt(0));
    }

    /**
     * @return the countryShortName
     */
    public String getCountryShortName() {
        return countryShortName;
    }

    /**
     * @param countryShortName the countryShortName to set
     */
    public void setCountryShortName(String countryShortName) {
        this.countryShortName = countryShortName;
    }
    
    @Override
    public String toString() {
        return countryName + " " + countryShortName + " " + countryCode;
    }
}
