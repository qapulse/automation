package com.littleinc.MessageMe.util;

import java.lang.reflect.Type;

import android.location.Location;

import com.coredroid.util.LogIt;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * @see LocationSerializer
 */
public class LocationDeserializer implements JsonDeserializer<Location>
{
    public Location deserialize(JsonElement je, Type type, 
                                JsonDeserializationContext jdc)
                           throws JsonParseException
    {
        JsonObject jo = je.getAsJsonObject();
        Location loc = new Location(jo.getAsJsonPrimitive("mProvider").getAsString());
        loc.setLatitude(jo.getAsJsonPrimitive("mLatitude").getAsDouble());
        loc.setLongitude(jo.getAsJsonPrimitive("mLongitude").getAsDouble());
        loc.setAltitude(jo.getAsJsonPrimitive("mAltitude").getAsDouble());
        loc.setTime(jo.getAsJsonPrimitive("mTime").getAsLong());
        loc.setAccuracy(jo.getAsJsonPrimitive("mAccuracy").getAsFloat());
        
        LogIt.d(this, "Parsed Location object", loc);
        
        return loc;
    }
}