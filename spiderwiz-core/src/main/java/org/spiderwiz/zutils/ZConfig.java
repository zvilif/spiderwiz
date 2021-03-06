package org.spiderwiz.zutils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A class that manages application configuration files.
 * <p>
 * An application configuration file is a text file that contains mapping of property names (the keys) to property values.
 * Each line in the file has the format:
 * <p style="padding-left:2em;">
 * <code>[<em>property name</em>]<em>property value</em></code>
 * <p>
 * This class has methods for reading configuration files into a map object, retrieving and
 * interpreting configuration properties, modifying properties, updating configuration files and some other tools.
 * <p>
 * Note that a configuration map allows only one value per property. If the configuration file contains multiple definitions of the
 * same property, then when loading the file into a configuration map the latter in the file will step over the earlier. Also lines
 * that do not have the structure of a configuration property will be ignored.
 */
public class ZConfig {
    private static class ConfigMap extends HashMap<String, String> {}

    /**
     * Represents one mapping of <em>property name</em> -&gt; <em>property value</em>.
     */
    public static class Property {
        private final String key;
        private String value;

        /**
         * Class constructor.
         * @param key       property name.
         * @param value     property value.
         */
        public Property(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the property name.
         * @return the property name.
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns the property value.
         * @return the property value.
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets property value.
         * @param value property value to set.
         */
        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Represents all the properties defined in a configuration file as an {@link ArrayList}.
     */
    public static class PropertySheet extends ArrayList<Property> {}

    private String charset;
    private String pathname;
    private final ConfigMap config;

    /**
     * Class constructor.
     */
    public ZConfig() {
        config = new ConfigMap();
    }

    /**
     * Initializes the object with a file pathname and the default character set (UTF-8).
     * <p>
     * The method opens the specified file that is supposed to contain UTF-8 text,
     * reads its content, parses the configuration properties into the map managed by this object, then closes the file.
     * @param pathname      the system-dependent file pathname.
     * @return  true if the file exists and the object has been initialized successfully, false otherwise.
     * @see #init(java.lang.String, java.lang.String) init(pathname, charset)
     * @see #getPropertySheet()
     */
    public boolean init(String pathname) {
        return init(pathname, "UTF-8");
    }

    /**
     * Initializes the object with a file pathname and the specified character set.
     * <p>
     * The method opens the specified file that is supposed to contain text in the specified character set,
     * reads its content, parses the configuration properties into the map managed by this object, then closes the file.
     * @param pathname      the system-dependent file pathname.
     * @param charset       the character set used for the text in the file.
     * @return  true if the file exists and the object has been initialized successfully, false otherwise.
     * @see #init(java.lang.String) init(pathname)
     */
    public boolean init(String pathname, String charset) {
        this.pathname = pathname;
        this.charset = charset;
        config.clear();
        try {
            readSettingFile(null);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Reloads the properties from the file used to initialize the object.
     * <p>
     * Call this method if the configuration file was modified externally and you want to reload it into the map managed by this
     * object.
     * @return  true if the file used for initializing the object exists and its content has been reloaded successfully,
     * false otherwise.
     * @see #init(java.lang.String) init(pathname)
     * @see #init(java.lang.String, java.lang.String) init(pathname, charset)
     */
    public boolean reload() {
        return init(pathname, charset);
    }
    
    /**
     * Returns the value to which the specified property is mapped.
     * <p>
     * Returns the value to which the specified property is mapped, or null if the configuration contains no mapping for the property.
     * @param prop  the property whose associated value is to be returned.
     * @return  the value to which the specified property is mapped, or null if the configuration contains no mapping for the property.
     * @see #getIntProperty(java.lang.String) getIntProperty()
     */
    public synchronized final String getProperty(String prop) {
        String property = config.get(prop);
        return property == null || property.isEmpty() ? null : property;
    }

    /**
     * Returns the value to which the specified property is mapped converted to an integer.
     * <p>
     * Returns the value to which the specified property is mapped converted to an integer, or zero if the property is not an integer
     * number or the configuration contains no mapping for the property.
     * @param prop  the property whose associated value is to be returned.
     * @return  the value to which the specified property is mapped converted to an integer, or zero if the property is not an integer
     *          number or the configuration contains no mapping for the property.
     * @see #getProperty(java.lang.String) getProperty()
     */
    public int getIntProperty(String prop){
        return ZUtilities.parseInt(getProperty(prop));
    }
    
    /**
     * Returns true if at least one property from a given list is set on.
     * <p>
     * A property is considered to be set on if it exists in the configuration map and its value is neither an empty string nor does
     * it start with the letters "no", case insensitive. This method accepts as parameters a list of property names and returns true
     * if and only if at least one of the properties is set on.
     * @param props     zero or more property names.
     * @return true if and only if at least one of the specified properties is set on.
     */
    public final boolean isPropertySet (String ... props) {
        for (String prop : props) {
            String s = getProperty(prop);
            if (s != null && !s.isEmpty() && !s.toLowerCase().startsWith("no"))
                return true;
        }
        return false;
    }

    /**
     * Sets a property value or remove it.
     * <p>
     * If {@code value} is not null the method updates the  value of the property if it exists and will add it if it
     * does not. If {@code value} is {@code null} the property is deleted if it exists.
     * @param prop      the property to be set.
     * @param value     the value to set. If null then the property is removed if it exists.
     * @see #setProperties(java.lang.String) setProperties()
     */
    public synchronized final void setProperty(String prop, String value) {
        if (value == null)
            config.remove(prop);
        else
            config.put(prop, value);
    }
    
    /**
     * Sets values in a list of properties.
     * <p>
     * The {@code assignments} parameter is a list assignments <code><em>property</em>=<em>value</em></code> concatenated by a
     * semicolon, i.e.:
     * <p style="padding-left:2em;">
     * <code><em>property1</em>=<em>value1</em>;<em>property2</em>=<em>value2</em>;...</code>
     * <p>
     * The method sets each of the properties in the list to the specified value.
     * @param assignments  a list of properties in the format <em>property1</em>=<em>value1</em>;<em>property2</em>=<em>value2</em>;...
     * @see #setProperty(java.lang.String, java.lang.String) setProperty()
     */
    public synchronized final void setProperties (String assignments) {
        String s[] = assignments.split(",");
        for (String prop : s) {
            String p[] = prop.split("=");
            config.put(p[0], p[1]);
        }
    }
    
    /**
     * Saves the loaded configuration map in the configuration file.
     * <p>
     * Saves the current configuration map image in the file provided in the call to {@link #init(java.lang.String) init()}. The
     * order of the properties in the saved file is not guaranteed, and if the original file contains multiple definitions of the
     * same property, only the last one is stored in the newly written file. Lines that do not have the configuration
     * structure are written to the new file.
     */
    public synchronized final void saveConfiguration() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter (new FileOutputStream(pathname), charset))) {
            for (String key : config.keySet()) {
                writer.printf("[%1$s]%2$s", key, config.get(key));
                writer.println();
            }
        } catch (UnsupportedEncodingException | FileNotFoundException ex) {
        }
    }
    
    /**
     * Returns a list that contains the content of the configuration file.
     * <p>
     * This method loads the configuration file into a {@link PropertySheet PropertySheet} object that lists all the properties
     * contained in the configuration file, in the order they appear in the file. If there are multiple definitions of the same
     * property, they are copied to the returned object as they appear in the file.
     * <p>
     * You can use this method in conjunction with
     * {@link #savePropertySheet(org.spiderwiz.zutils.ZConfig.PropertySheet) savePropertySheet()} in order to edit the file without
     * shuffling its content.
     * @return a list that contains the content of the configuration file.
     * @throws IOException
     * @see #savePropertySheet(org.spiderwiz.zutils.ZConfig.PropertySheet) savePropertySheet()
     */
    public PropertySheet getPropertySheet() throws IOException{
        PropertySheet properties = new PropertySheet();
        readSettingFile(properties);
        return properties;
    }
    
    /**
     * Uses a list of properties to rewrite the configuration file and reloads the running application configuration.
     * <p>
     * This method gets a {@link PropertySheet PropertySheet} object that contains a list of properties and saves them in the
     * configuration file, in the order they appear in the list, rewriting over the file's previous content. If the list contains
     * multiple definitions of the same property, they are all written to the file as they appear in the list.
     * <p>
     * Besides writing to the file, the method reloads the property map of the running application with the values contained in the
     * list. If the list contains multiple definitions of the same property, the value of the latter steps over the earlier.
     * <p>
     * You can use this method in conjunction with {@link #getPropertySheet()} in order to edit the file without shuffling its content.
     * @param properties    a list of properties to store in the file.
     * @throws java.io.FileNotFoundException
     * @throws java.io.UnsupportedEncodingException
     * @see #getPropertySheet()
     */
    public synchronized void savePropertySheet(PropertySheet properties)
        throws FileNotFoundException, UnsupportedEncodingException {
        config.clear();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter (new FileOutputStream(pathname), charset))) {
            for (Property property : properties) {
                writer.printf("[%1$s]%2$s", property.key, property.value);
                writer.println();
                config.put(property.key, property.value);
            }
        }
    }
    
    /**
     * Performs custom processing for each property included in the configuration map.
     * <p>
     * This method activates {@link #processProperty(java.lang.String, java.lang.String) processProperty()} for each property
     * included in the configuration map. You can override that method in order to perform custom processing for each property.
     * @see #processProperty(java.lang.String, java.lang.String) processProperty()
     */
    public void processAllProperties() {
        HashMap<String, String> properties;
        synchronized(this) {
            properties = new HashMap<> (config);
        }
        for (String prop : properties.keySet()) {
            processProperty(prop, properties.get(prop));
        }
    }
    
    /**
     * Processes the given property.
     * <p>
     * This method is activated from {@link #processAllProperties()}. Override it to perform custom processing on the given property.
     * @param prop      name of the property to be processed.
     * @param value     value of the property to be processed.
     * @see #processAllProperties()
     */
    protected void processProperty(String prop, String value) {}
    
    /**
     * If 'properties' is null read the settings file into the settings map, otherwise fill out the properties sheet.
     * @param properties
     * @throws IOException 
     */
    private void readSettingFile(PropertySheet properties) throws IOException {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(
                new FileInputStream(pathname), charset))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] splitLine = line.split("]", -1);
                if (splitLine.length > 1) {
                    int i = splitLine[0].indexOf("[");
                    if (i >= 0) {
                        String key = splitLine[0].substring(i+1).trim();
                        String value = splitLine[1].trim();
                        if (properties == null)
                            config.put(key, value);
                        else
                            properties.add(new Property(key, value));
                    }
                }
            }
        }
    }
}
