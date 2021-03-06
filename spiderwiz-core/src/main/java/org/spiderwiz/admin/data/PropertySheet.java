package org.spiderwiz.admin.data;

import java.util.ArrayList;
import java.util.List;
import org.spiderwiz.annotation.WizField;
import org.spiderwiz.annotation.WizSerializable;

/**
 * Holds a copy of the content of an application's configuration file.
 * <p>
 * Used by <a href="http://spideradmin.com">SpiderAdmin</a> to display
 * and modify the configuration through the "Update Configuration" button.
 * <p>
 * The class is used internally by the SpiderAdmin agent and is rarely accessed directly.
 */
@WizSerializable
public class PropertySheet {

    /**
     * Holds the key to value mapping of a single configuration property.
     */
    @WizSerializable
    public static class Property {
        @WizField private String value;
        @WizField private String key;

        /**
         * Gets the value of the value property.
         * <p>
         * The {@code value} property is the value in a key to value mapping.
         * @return value of the value property.
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the value of the value property.
         * <p>
         * The {@code value} property is the value in a key to value mapping.
         * @param value
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * Gets the value of the key property.
         * <p>
         * The {@code key} property is the key in a key to value mapping.
         * @return value of the key property.
         */
        public String getKey() {
            return key;
        }

        /**
         * Sets the value of the key property.
         * <p>
         * The {@code key} property is the key in a key to value mapping.
         * @param value
         */
        public void setKey(String value) {
            this.key = value;
        }
    }
    
    @WizField private final ArrayList<PropertySheet.Property> property;

    /**
     * Constructs an empty object.
     */
    public PropertySheet() {
        property = new ArrayList<>();
    }

    /**
     * Gets the {@code property} property.
     * <p>
     * The {@code property} property lists all the properties in a configuration file.
     * @return the {@code property} property.
     */
    public List<PropertySheet.Property> getProperty() {
        return this.property;
    }

    /**
     * Adds a property (a key to value mapping) to the property sheet.
     * @param key       the key.
     * @param value     the value.
     * @return
     */
    public PropertySheet addProperty(String key, String value) {
        if (key == null)
            return this;
        Property prop = new Property();
        prop.setKey(key);
        prop.setValue(value);
        property.add(prop);
        return this;
    }
}