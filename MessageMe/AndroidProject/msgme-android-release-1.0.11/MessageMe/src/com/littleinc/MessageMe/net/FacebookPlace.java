package com.littleinc.MessageMe.net;

import java.io.Serializable;

import com.google.gson.Gson;
import com.littleinc.MessageMe.util.StringUtil;

public class FacebookPlace implements Serializable {
    /*
     * { "name": "Los Pozos, San Jose, Costa Rica", "location": { "latitude":
     * 9.95, "longitude": -84.1833 }, "category": "City", "id":
     * "108657535834202" }
     */

    /**
     * 
     */
    private static final long serialVersionUID = -8965553597826302589L;

    private String name = "";

    private Location location;

    private String category = "";

    private String id = "";

    public static class Location implements Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public double latitude;

        public double longitude;

        public String country;

        public String city;

        public Location() {
        }

        public Location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public String toString() {
            return "Location [latitude=" + latitude + ", longitude=" + longitude + "]";
        }

        public String getAddress() {
            StringBuffer buff = new StringBuffer();
            if (!StringUtil.isEmpty(city) && !StringUtil.isEmpty(country)) {
                buff.append(city);
                buff.append(", ");
                buff.append(country);
            } else {
                if (!StringUtil.isEmpty(country)) {
                    buff.append(country);
                }
            }

            return buff.toString();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static FacebookPlace parseFromJson(String json) {
        FacebookPlace place = null;
        Gson gson = new Gson();
        place = gson.fromJson(json, FacebookPlace.class);
        return place;
    }

    @Override
    public String toString() {
        return "FacebookPlace [name=" + name + ", location=" + location + ", category=" + category + ", id=" + id + "]";
    }

}
