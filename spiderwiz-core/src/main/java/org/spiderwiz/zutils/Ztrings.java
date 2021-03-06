package org.spiderwiz.zutils;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Extension of {@link ZHashSet ZHashSet&lt;String&gt;} with some utility methods.
 */
public class Ztrings extends ZHashSet<String> {
    /**
     * Casts the object to a generic collection of strings.
     * @return the object as a generic collection of strings.
     */
    public Collection<String> asCollection() {
        return this;
    }
    
    /**
     * Returns a new set that contains the elements of the given collection.
     * @param c the given collection.
     * @return a new set that contains the elements of the given collection.
     */
    public static Ztrings fromList(Collection<String> c) {
        Ztrings set = new Ztrings();
        set.addAll(c);
        return set;
    }
    
    /**
     * Returns all set elements concatenated by the given delimiter into one string.
     * @param delimiter the delimiter to use for concatenation.
     * @return all set elements concatenated by the given delimiter into one string.
     */
    public String concatenate(String delimiter) {
        lockRead();
        try {
            return ZUtilities.concatAll(delimiter, toArray());
        } finally {
            unlockRead();
        }
    }
    
    /**
     * Returns all set elements concatenated by a semicolon (;) into one string.
     * @return all set elements concatenated by a semicolon (;) into one string.
     */
    public String concatenate() {
        return concatenate(";");
    }
    
    /**
     * Returns a new set containing the components of the given string after being split by the given delimiter.
     * @param string        the string to split.
     * @param delimiter     the delimiter to use from splitting.
     * @return a new set containing the components of the given string after being split by the given delimiter.
     */
    public static Ztrings split(String string, String delimiter) {
        return string == null || string.isEmpty() ? new Ztrings() : Ztrings.fromList(Arrays.asList(string.split(delimiter)));
    }
    
    /**
     * Returns a new set containing the components of the given string after being split by a semicolon (;).
     * @param string        the string to split.
     * @return a new set containing the components of the given string after being split by a semicolon (;).
     */
    public static Ztrings split(String string) {
        return split(string, ";");
    }
    
    /**
     * Returns a new set containing all elements of both this set and the given collection.
     * @param c the collection to unite with this set.
     * @return a new set containing all elements of both this set and the given collection.
     */
    @Override
    public Ztrings union(Collection<String> c) {
        return (Ztrings)super.union(c);
    }
    
    /**
     * Returns a new set containing the elements that are contained in both this set and the given collection.
     * @param c the collection to intersect with this set.
     * @return a new set containing the elements that are contained in both this set and the given collection.
     */
    @Override
    public Ztrings intersection(Collection<String> c) {
        return (Ztrings)super.intersection(c);
    }
    
    /**
     * Converts a set of strings to a set of {@link UUID} elements.
     * @return all the elements of this set as a set of UUID elements.
     */
    public ZHashSet<UUID> toUUIDs() {
        ZHashSet<UUID> set = new ZHashSet<>();
        for (String uuid : this.asCollection())
            set.add(UUID.fromString(uuid));
        return set;
    }
}
