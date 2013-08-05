package testendpoints.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * <tt>PropertiesPlaceholder</tt> :- content the list properties on csg.properties file
 * </p>
 */

/**
 * @author : pmontano
 * Date: 5/15/13
 * Time: 12:06 PM
 */
public class PropertiesPlaceholder {


    private static final Log LOG = LogFactory.getLog(PropertiesPlaceholder.class);

    public static final String CSG_PROP_FILE_NAME = "csg.properties";

    private static PropertiesPlaceholder instance = null;

    private Properties properties = new Properties();


    /**
     * Constructor to create the single instance
     */
    private PropertiesPlaceholder(){
        try {
            properties.load(PropertiesPlaceholder.class.getClassLoader().getResourceAsStream(CSG_PROP_FILE_NAME));
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    /**
     * Gets the properties by key
     * @param key identifier the properties
     * @return the value of key properties.
     */
    public String getProperty(String key){
        return properties.getProperty(key);
    }

    /**
     * Gets the instance the PropertiesPlaceholder
     * @return return the   PropertiesPlaceholder
     */
    public static PropertiesPlaceholder getInstance(){
        if(instance == null){
            instance = new PropertiesPlaceholder();
        }

        return instance;
    }
}
