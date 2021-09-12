package org.spiderwiz.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;
import org.spiderwiz.zutils.ZDate;

/**
 *
 * @author  zvil
 */
class MyUtilities {
    private static class OldFileFinder extends SimpleFileVisitor<Path> {
        Path oldestFile = null;
        ZDate oldestTime = null;
        
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            ZDate creationTime = new ZDate(attrs.creationTime().toMillis());
            ZDate modifTime = new ZDate(attrs.lastModifiedTime().toMillis());
            creationTime = creationTime.earliest(modifTime);
            if (oldestTime == null || creationTime.before(oldestTime)) {
                oldestFile = file;
                oldestTime = creationTime;
            }
            return FileVisitResult.CONTINUE;
        }
    }

    private static final char ALEF = '\u05D0';
    private static final char TAV = '\u05EA';
    
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
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= ALEF && c <= TAV || c == ' ' || c == '.')
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

    /**
     * Find the time of the oldest file in a folder tree
     * @param path
     * @return the time in whole seconds
     * @throws IOException 
     */
    static ZDate findDateOfOldestFile (String path) throws IOException {
        try {
            OldFileFinder finder = new OldFileFinder();
            Files.walkFileTree(Paths.get(path), finder);
            if (finder.oldestTime != null)
                return finder.oldestTime.truncate(ZDate.SECOND);
        }
        catch (NoSuchFileException ex) {}
        return null;
    }
}
