package org.spiderwiz.core;

import java.util.Collection;
import java.util.Map;
import org.spiderwiz.zutils.ZHashMap;

/**
 * Mapping of a list of consumed objects to T values that are not null for lossless objects
 */
abstract class ConsumedObjectMap extends ZHashMap<String, Object> {
    /**
     * Load or update the object from a string of values concatenated by ';'
     * @param objString 
     * @param objList 
     */
    void fromString(String objString, Collection<String> subset) {
        fromArray(objString.split(";"), subset);
    }
    
    /**
     * Load or update the object from a string array
     * @param objList 
     * @param subset    if not null, include only object codes that are contained in this list
     */
    void fromArray(String[] objList, Collection<String> subset) {
        for (String obj : objList) {
            String objCode = DataObject.stripLossless(obj);
            boolean lossless = DataObject.isLossless(obj);
            boolean wasLossless = get(objCode) != null;
            if ((subset == null || subset.contains(objCode)) && (!containsKey(objCode) || lossless != wasLossless))
                put(objCode, lossless ? getLosslessController(objCode) : null);
        }
    }

    /**
     * @return the object as a semicolon separated list
     */
    String getAsString() {
        return getAsString(null);
    }
    
    /**
     * Get the values of the object that exist in 'subset' parameter as a semicolon separated list
     * @param subset    list of keys that should be included, or null if all shall.
     * @return 
     */
    String getAsString(Collection<String> subset) {
        StringBuilder sb = new StringBuilder();
        lockRead();
        try {
            for (Map.Entry<String, Object> en : entrySet()) {
                if (subset == null || subset.contains(en.getKey())) {
                    if (sb.length() > 0)
                        sb.append(';');
                    sb.append(en.getKey());
                    if (en.getValue() != null)
                        sb.append(DataObject.Lossless);
                }
            }
        } finally {
            unlockRead();
        }
        return sb.toString();
    }

    /**
     * Override this method to create the object used for lossless data objects
     * @param objCode   the object code for which the object is created
     * @return 
     */
    abstract Object getLosslessController(String objCode);
}
    

