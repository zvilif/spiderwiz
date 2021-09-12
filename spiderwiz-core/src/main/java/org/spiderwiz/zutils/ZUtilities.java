package org.spiderwiz.zutils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains various static utility methods.
 * @author @author  zvil
 */
public class ZUtilities {
    private static class FolderDeleter extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Constructor can be private because this class will never be instantiated.
     */
    private ZUtilities() {}

    /**
     * Returns the given string parsed into an integer, or zero if the string does not represent a valid integer number.
     * @param s the string to parse.
     * @return the given string parsed into an integer, or zero if the string does not represent a valid integer number.
     */
    public static int parseInt(String s) {
        return parseInt(s, 0);
    }
    

    /**
     * Returns the given string parsed into an integer, or the given default value if the string does not represent a valid integer
     * number.
     * @param s     the string to parse.
     * @param def   the default value.
     * @return  the given string parsed into an integer, or the given default value if the string does not represent a valid integer
     *          number.
     */
    public static int parseInt(String s, int def) {
        if (s == null)
            return def;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return def;
        }
    }
    
    /**
     * Returns the boolean value that the given string represents.
     * <p>
     * A string represents the boolean value {@code true} if it is either "1" or "true" (case insensitive), otherwise it represents
     * {@code false}.
     * @param s the string to interpret.
     * @return the boolean value that the given string represents.
     */
    public static boolean parseBoolean(String s) {
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }
    
    /**
     * Returns {@code 1} if the given boolean value is {@code true}, {@code 0} if it is {@code false}.
     * @param b the value to convert.
     * @return 1 if the given value is true, 0 if it is false.
     */
    public static int boolToInt(boolean b) {
        return b ? 1 : 0;
    }
    
    /**
     * Returns the given string parsed into a long integer, or zero if the string does not represent a valid integer number.
     * @param s the string to parse.
     * @return the given string parsed into a long integer, or zero if the string does not represent a valid integer number.
     */
    public static long parseLong(String s) {
        return parseLong(s, 0);
    }
    
    /**
     * Returns the given string parsed into a long integer, or the given default value if the string does not represent a valid integer
     * number.
     * @param s     the string to parse.
     * @param def   the default value.
     * @return  the given string parsed into a long integer, or the given default value if the string does not represent a valid integer
     *          number.
     */
    public static long parseLong(String s, long def) {
        if (s == null || s.isEmpty())
            return def;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return def;
        }
    }
    
    /**
     * Returns the given string parsed into a float number, or zero if the string does not represent a valid float number.
     * @param s the string to parse.
     * @return the given string parsed into a float number, or zero if the string does not represent a valid float number.
     */
    public static float parseFloat(String s) {
        return parseFloat(s, 0);
    }

    /**
     * Returns the given string parsed into a float number, or the given default value if the string does not represent a valid float
     * number.
     * @param s     the string to parse.
     * @param def   the default value.
     * @return  the given string parsed into a float number, or the given default value if the string does not represent a valid float
     *          number.
     */
    public static float parseFloat(String s, float def) {
        if (s == null)
            return def;
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Returns the given string parsed into a double float number, or zero if the string does not represent a valid float number.
     * @param s the string to parse.
     * @return the given string parsed into a double float number, or zero if the string does not represent a valid float number.
     */
    public static double parseDouble(String s) {
        if (s == null)
            return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Returns a concatenation of all the string representations of the given arguments with the given delimiter inserted between
     * the concatenated strings.
     * <p>
     * The given arguments may include {@code null} values. Non-trailing nulls are treated as empty strings,
     * while trailing nulls are ignored.
     * @param delimiter     the delimiter to use for concatenation.
     * @param args          the arguments to concatenate.
     * @return  a concatenation of the given arguments with the given delimiter inserted between the concatenated strings.
     */
    public static String concatAll(String delimiter, Object ... args) {
        StringBuilder result = new StringBuilder();
        int commas = 0;
        for (Object arg : args) {
            if (arg != null) {
                while (commas > 0) {
                    result.append(delimiter);
                    --commas;
                }
                result.append(arg);
            }
            ++commas;
        }
        return result.toString();
    }
    
    /**
     * Returns a concatenation of all the elements of the given collection converted to strings, with the given delimiter
     * inserted between the concatenated strings.
     * <p>
     * The collection elements may include {@code null} values. Non-trailing nulls are treated as empty strings,
     * while trailing nulls are ignored.
     * @param delimiter the delimiter to use for concatenation.
     * @param c         the collection
     * @return  the elements of the given collection concatenated by the given delimiter into one string.
     */
    public static String concatAll(String delimiter, Collection c) {
        return concatAll(delimiter, c.toArray());
    }
    
    /**
     * Returns an array of strings representing the numbers contained in the given array of integer numbers.
     * @param list the integer array.
     * @return an array of strings representing the numbers contained in the given array of integer numbers.
     */
    public static String[] intsToStrings(int list[]) {
        return Arrays.toString(list).split("[\\[\\]]")[1].split(", "); 
    }
    
    /**
     * Returns an array of integer numbers parsed from the given array of strings.
     * <p>
     * If any string in the given string array does not contain a valid number the corresponding integer array element will contain
     * zero.
     * @param list the string array.
     * @return an array of integer numbers parsed from the given array of strings.
     */
    public static int[] stringsToInts(String list[]) {
        int result[] = new int[list.length];
        int i = 0;
        for (String element : list) {
            result[i++] = parseInt(element);
        }
        return result;
    }
    
    /**
     * Converts a simple array of strings to {@link ArrayList ArrayList&lt;String&gt;}.
     * @param array the array to convert.
     * @return the given array as an ArrayList&lt;String&gt; object, or an empty list if the given array is null.
     */
    public static ArrayList<String> arrayToList(String[] array) {
        ArrayList<String> list = new ArrayList<>();
        if (array != null)
            list.addAll(Arrays.asList(array));
        return list;
    }
    
    /**
     * Returns the stack trace of the given exception object as a string.
     * @param ex    the Exception object.
     * @return      the stack trace of the given exception object as a string.
     */
    public static String stackTraceToString(Throwable ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * Returns true if the given string contains the given regular expression.
     * @param string    the string to search in. If null the method returns false;
     * @param regex     the regular expression to look for. If null the method returns false.
     * @return true if and only if the given string contains the given regular expression.
     */
    public static boolean find(String string, String regex) {
        return string == null || regex == null ? false : Pattern.compile(regex).matcher(string).find();
    }

    /**
     * Replaces a series of substrings of given string by a series of replacement strings.
     * <p>
     * The {@code pairs} argument list must be of even length. It is considered a series of pairs, where
     * the first element in each pair is a regular expression that may match a substring within the {@code source} parameter.
     * The second element in each pair is the value by which the substring matched by the first element of the pair, if exists,
     * shall be replaced.
     * <p>
     * Note that the replacement action is recursive, i.e. each search for a replacement candidate is done over the string
     * yielded from the previous replacement.
     * @param source    The source string
     * @param pairs     A series of pairs <em>regular expression</em> - <em>replacement</em>.
     * @return the result string
     */
    public static String replace(String source, String ... pairs) {
        if (source == null)
            return null;
        String result = source;
        for (int i = 0; i < pairs.length;) {
            String regex = pairs[i++];
            if (i == pairs.length)
                return result;
            result = result.replaceAll(regex, pairs[i++]);
        }
        return result;
    }
    
    /**
     * Deletes an entire folder with all sub-folders and contained files.
     * @param path the pathname of the folder to delete.
     * @throws IOException
     */
    public static void deleteFolder (String path) throws IOException {
        try {
            Files.walkFileTree(Paths.get(path), new FolderDeleter());
        }
        catch (NoSuchFileException ex) {
        }
    }
    
    /**
     * Returns a list of all files in the given folder, sorted by the lexicographic order of their names, sub folders first.
     * @param path  the folder pathname.
     * @return       a list of all files in the folder, sorted by the lexicographic order of their names, sub folders first.
     */
    public static File[] getFolderList(String path) {
        Comparator<File> comp = (File o1, File o2) -> {
            // sort by lexigoraphic order, folders first
            int type1 = o1.isDirectory() ? 0 : 1;
            int type2 = o2.isDirectory() ? 0 : 1;
            if (type1 != type2)
                return type1 - type2;
            return o1.getName().compareToIgnoreCase(o2.getName());
        };
        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files == null)
            return null;
        Arrays.sort(files, comp);
        return files;
    }

    /**
     * Returns the external IP address of the calling application or {@code "Unknown"} if the IP cannot be identified.
     * @return the external IP address of the calling application or {@code "Unknown"} if the IP cannot be identified.
     */
    public static String getMyIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            return "Unknown";
        }
    }
    
    /**
     * Extracts the filename extension from a given file path.
     * <p>
     * If the path does not have extension at all, i.e. the simple file name does not contain
     * the period (.) character, the method returns {@code null}. Otherwise it returns the extension, which may be an empty string if the
     * file name ends with a period.
     * @param path  The file path.
     * @return      filename extension or null if there is no extension.
     */
    public static String getFileExtension(String path) {
        int i = path.lastIndexOf('.');
        int p = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return i > p ? path.substring(i + 1) : null;
    }
    
    /**
     * Split {@code list} by {@code delimiter} and return the result as a {@link Set}
     * @param list          the list to split
     * @param delimiter     the delimiter to split by
     * @return s split by delimiter as a Set
     */
    public static Set<String> splitIntoSet(String list, String delimiter) {
        return list == null ? null : new HashSet<>(arrayToList(list.split(delimiter)));
    }
    
    /**
     * Split string {@code list} by delimiter {@code delimiter} and check if the split list contains {@code element}
     * @param list          the list to split
     * @param delimiter     the delimiter to split by
     * @param element       the element to look for
     * @return true if and only if element is contained in list
     */
    public static boolean contains(String list, String delimiter, String element) {
        return list != null && Arrays.asList(list.split(delimiter)).contains(element);
    }
    
    /**
     * Check if the given path is of an empty folder
     * @param path
     * @return true if and only if the given path is of an empty folder
     * @throws IOException
     */
    public static boolean isFolderEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return !entries.findFirst().isPresent();
            }
        }

        return false;
    }
}
