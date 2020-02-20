package org.spiderwiz.admin.xml;

import java.util.List;

/**
 * Extends PropertySheet by providing methods to populate it
 * @author Zvi 
 */
public class PropertySheetEx extends PropertySheet{

    /**
     * Add a property (pair of 'key' and 'value') to the property sheet
     * @param key
     * @param value
     * @return
     */
    public PropertySheetEx addProperty(String key, String value) {
        if (key == null)
            return this;
        List<Property> properties = getProperty();
        Property prop = new Property();
        prop.setKey(key);
        prop.setValue(value);
        properties.add(prop);
        return this;
    }
}
