package com.littleinc.MessageMe.util;

import java.lang.reflect.Type;

import android.location.Location;

import com.coredroid.util.LogIt;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Perform custom serialization for the Android Location object.  This is
 * necessary because the Android Location class is not Serializable, which 
 * means it is not safe to serialize and deserialize all its fields.
 * 
 * If we serialize all its fields then we occasionally hit a RuntimeException 
 * ("Failed to invoke protected java.lang.ClassLoader() with no args"), which
 * will force us to throw away the saved application state and make the user
 * login again.
 * 
 * That happens when the Location object includes a ClassLoader in its
 * list of extras, e.g. 
 *
 *        "lastKnownLocation": {
 *            "mResults": [
 *                0,
 *                0
 *            ],
 *            "mProvider": "gps",
 *            "mExtras": {
 *                "mParcelledData": {
 *                    "mOwnObject": 1,
 *                    "mObject": 5525040
 *                },
 *                "mClassLoader": {
 *                    "packages": {                        
 *                    }
 *                },
 *                <snip>
 *
 */
public class LocationSerializer implements JsonSerializer<Location>
{
    public JsonElement serialize(Location t, Type type, 
                                 JsonSerializationContext jsc)
    {
        JsonObject jo = new JsonObject();
        jo.addProperty("mLatitude", t.getLatitude());
        jo.addProperty("mLongitude", t.getLongitude());
        jo.addProperty("mAltitude", t.getAltitude());
        jo.addProperty("mTime", t.getTime());
        jo.addProperty("mProvider", t.getProvider());
        jo.addProperty("mAccuracy", t.getAccuracy());
        
        LogIt.d(this, "Serialized Location object", jo);
        
        return jo;
    }

}