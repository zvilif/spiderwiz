package org.spiderwiz.core;

import java.io.File;
import java.util.Scanner;

/**
 *
 * @author Zvi
 */
class MyUtilities {
    private static final char Alef = '\u05D0';
    private static final char Tav = '\u05EA';
    
    /**
     * Convert a string to a string where each non alphanumeric character in the original string is replaced
     * by an escape character followed by the hexadecimal representation of the character
     * @param esc string to use as escape character
     * @param s input string
     * @return the escaped string
     */
    public static String escapeNonAlphanumeric (String esc, String s) {
        if (s == null)
            return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= Alef && c <= Tav || c == ' ' || c == '.')
                result.append(c);
            else {
                result.append(esc);
                result.append(String.format("%04x", (int) c));
            }
        }
        return result.toString();
    }
    
    /**
     * Convert a escaped string back to the original string.
     * @param esc string to use as escape character
     * @param s input escaped string
     * @return the original string
     */
    public static String unescapeNonAlphanumeric (String esc, String s) {
        if (s == null)
            return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length();) {
            int endIndex = i + esc.length();
            if (endIndex <= s.length() && esc.equals(s.substring(i, endIndex))) {
                i = endIndex + 4;
                result.append((char)new Scanner(s.substring(endIndex, i)).nextInt(16));
            } else
                result.append(s.charAt(i++));
        }
        return result.toString();
    }

    
    /**
     * Return the full path of the file that consists of this string prefixed by first d: then c:
     * If none of them has the file return null
     * @param value
     * @return the full path with the first device that has it
     */
    public static String prefixDevice (String value) {
        if (value == null || value.isEmpty() || !value.startsWith("/") && !value.startsWith("\\"))
            return value;
        for (char c = 'c'; c <= 'z'; c--) {
            String name =  c + ":" + value;
            if (new File (name).exists())
                return name;
        }
        return null;
    }
}
