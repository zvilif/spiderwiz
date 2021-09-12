package org.spiderwiz.zutils;

import java.util.Map;

/**
 * Equivalent of {@code ZHashMap<String,String>} with some utility methods.
 */
public class ZDictionary extends ZHashMap<String, String> {
    private static final String LIST_SEPARATOR = ";";
    private static final String ASSIGMENT = "=";
    /**
     * Constructs an empty ZDictionary with the default initial capacity and the default load factor.
     */
    public ZDictionary() {
        super();
    }

    /**
     * Constructs a new ZDictionary with the same mappings as the specified Map.
     * @param m the specified map.
     */
    public ZDictionary(Map<String, String> m) {
        super(m);
    }

    /**
     * Gets a parameter list string and returns a dictionary object that maps keys to values.
     * <p>
     * The parameter is a list of pairs <em>key</em>=<em>value</em> concatenated by a semicolon (;). The assignment is optional. The
     * list may contain keys that are not assigned to values, in which case the returned dictionary maps the keys to {@code null}.
     * <p>
     * @param parList   parameter list in the format "<em>key1</em>=<em>value1</em>;<em>key2</em>=<em>value2</em>;..."
     * @return a dictionary object that maps keys (converted to lowercase) to values.
     */
    public static ZDictionary parseParameterList(String parList) {
        if (parList == null || parList.isEmpty())
            return null;
        ZDictionary parMap = new ZDictionary();
        String pars[] = parList.split(LIST_SEPARATOR);
        for (String par : pars) {
            String pair[] = par.split(ASSIGMENT);
            String key = pair[0];
            if (key != null && !key.isEmpty()) {
                String val = pair.length < 2 ? null : pair[1];
                parMap.put(key, val);
            }
        }
        return parMap;
    }
}
