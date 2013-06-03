package com.coredroid.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;

import com.coredroid.core.CoreObject;
import com.coredroid.util.LogIt;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.littleinc.MessageMe.util.LocationDeserializer;
import com.littleinc.MessageMe.util.LocationSerializer;

/**
 * Save the objects in shared preferences
 */
public class PreferencesDataStore implements DataStore {

    private static final String CLASS_SUFFIX = "-Class";

    private static final String PREFS_NAME = "AppState";

    private static final String PERSISTENT_PREFS_NAME = "PersistentPrefs";

    private SharedPreferences settings;

    private SharedPreferences persistentSettings;

    private Gson gson;

    public PreferencesDataStore(Context context) {
        settings = context.getSharedPreferences(PREFS_NAME, 0);
        persistentSettings = context.getSharedPreferences(
                PERSISTENT_PREFS_NAME, 0);

        gson = createGson();
    }

    @Override
    public void clear() {
        settings.edit().clear().commit();
    }

    protected Gson createGson() {
        GsonBuilder gsonBuilder = 
                new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC,
                                                             Modifier.TRANSIENT, 
                                                             Modifier.VOLATILE);
        
        // The Android Location class is not Serializable, so we have to 
        // do some custom serialization to ensure we only serialize fields
        // which will be safe to deserialize.               
        gsonBuilder.registerTypeAdapter(Location.class, new LocationDeserializer());
        gsonBuilder.registerTypeAdapter(Location.class, new LocationSerializer());

        return gsonBuilder.create();
    }

    @Override
    public void dump(OutputStream out) throws IOException {
        PrintStream writer = new PrintStream(out);

        writer.append("Persistent\n");
        for (Entry entry : persistentSettings.getAll().entrySet()) {
            writer.append("\t").append(entry.getKey().toString()).append(":")
                    .append(gson.toJson(entry.getValue())).append("\n");
        }
        writer.append("\nSettings\n");
        for (Entry entry : settings.getAll().entrySet()) {
            writer.append("\t").append(entry.getKey().toString()).append(":")
                    .append(gson.toJson(entry.getValue())).append("\n");
        }
    }

    @Override
    public void save(String key, CoreObject obj) {
        long start = System.currentTimeMillis();
        if (obj == null) {
            SharedPreferences prefs = persistentSettings.contains(key) ? persistentSettings
                    : settings;
            Editor editor = prefs.edit();
            editor.remove(key);
            editor.commit();
        } else {
            SharedPreferences prefs = obj.isPersistent() ? persistentSettings
                    : settings;
            
            LogIt.d(this, "Save application preferences");
            Editor editor = prefs.edit();
            editor.putString(key, gson.toJson(obj));
            editor.putString(key + CLASS_SUFFIX, obj != null ? obj.getClass()
                    .getName() : null);
            editor.commit();
        }
        LogIt.d(this, "TIMER " + key + ": "
                + (System.currentTimeMillis() - start));
    }

    @Override
    public CoreObject get(String key) {
        SharedPreferences prefs = settings;
        String objString = prefs.getString(key, null);
        
        if (objString == null) {
            prefs = persistentSettings;
            objString = prefs.getString(key, null);
        }
        
        if (objString != null) {
            String classStr = prefs.getString(key + CLASS_SUFFIX, null);
            
            if (classStr != null) {
                try {
                    Class c = Class.forName(classStr);
                    LogIt.d(this, "Load application preferences");
                    return (CoreObject) gson.fromJson(objString, c);
                } catch (JsonSyntaxException e) {
                    LogIt.e(this, e, "Could not parse entry", 
                            key, e.getMessage(), objString);
                } catch (ClassNotFoundException e) {
                    LogIt.e(this, e, "Could not find class for entry", 
                            key, e.getMessage(), objString);
                } catch (Exception e) {
                    // If we are unable to load the preferences then return null
                    // to force the app to recreate them, and will make the user
                    // login again.  This is necessary as if something goes wrong
                    // here then the app will crash every time it tries to launch.
                    //
                    // Unfortunately Crittercism is not initialized by this point,
                    // so it isn't easy to send a crash report about this.
                    //
                    // We've previously seen this parsing fail because the Location
                    // object included an extra for the ClassLoader, which the GSON
                    // library tries to instantiate - which fails because the 
                    // constructor is not public (it is protected).
                    //   https://www.pivotaltracker.com/story/show/40813131
                    LogIt.e(this, e, "Exception loading saved application preferences, discarding and recreating them - the user will be forced to login again", 
                            e.getMessage(), objString);
                }
            } else {
                LogIt.w(this, "Could not find class type for entry", key, classStr);
            }
        }
        return null;
    }

}
