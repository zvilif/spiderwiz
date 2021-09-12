package org.spiderwiz.zutils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A synchronized implementation of {@link java.util.HashSet}.
 * <p>
 * The Java implementation of {@link java.util.HashSet} is unsynchronized. This class extends the implementation to provide
 * synchronization.
 * @param <T>   the type of elements maintained by this set.
 */
public class ZHashSet<T> extends HashSet<T>{
    private final ZLock lock;

    /**
     * Constructs a new, empty set; the backing HashSet instance has default initial capacity (16) and load factor (0.75).
     */
    public ZHashSet() {
        super();
        lock = new ZLock();
    }

    /**
     * Locks the set for reading.
     * <p>
     * This method is used by the class internally to provide synchronization, but you can use it directly when you need to perform
     * a synchronized bulk read operation on the set.
     */
    public final void lockRead() {
        lock.lockRead();
    }
    
    /**
     * Locks the set for writing.
     * <p>
     * This method is used by the class internally to provide synchronization, but you can use it directly when you need to perform
     * a synchronized bulk write operation on the set.
     */
    public final void lockWrite() {
        lock.lockWrite();
    }
    
    /**
     * Unlocks the set after reading.
     * <p>
     * This method is used by the class internally, but you should use it directly if you use {@link #lockRead()} directly.
     */
    public final void unlockRead() {
        lock.unlockRead();
    }
    
    /**
     * Unlocks the set after writing.a
     * <p>
     * This method is used by the class internally, but you should use it directly if you use {@link #lockWrite()} directly.
     */
    public final void unlockWrite() {
        lock.unlockWrite();
    }
    
    /**
     * Synchronized implementation of {@link HashSet#add(java.lang.Object)}.
     * @param e     element to be added to this set.
     * @return      true if this set did not already contain the specified element.
     */
    @Override
    public boolean add(T e) {
        lock.lockWrite();
        try {
            return super.add(e);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#addAll(java.util.Collection)}.
     * @param c collection containing elements to be added to this set.
     * @return true if this set changed as a result of the call.
     */
    @Override
    public boolean addAll(Collection c) {
        lock.lockWrite();
        try {
            return super.addAll(c);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#remove(java.lang.Object)}.
     * @param o object to be removed from this set, if present.
     * @return  true if the set contained the specified element.
     */
    @Override
    public boolean remove(Object o) {
        lock.lockWrite();
        try {
            return super.remove(o);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#removeAll(java.util.Collection)}.
     * @param c collection containing elements to be removed from this set.
     * @return  true if this set changed as a result of the call.
     */
    @Override
    public boolean removeAll(Collection c) {
        lock.lockWrite();
        try {
            return super.removeAll(c);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#removeIf(java.util.function.Predicate) }.
     * @param filter a predicate which returns {@code true} for elements to be removed
     * @return {@code true} if any elements were removed
     */
    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        lock.lockWrite();
        try {
            return super.removeIf(filter);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#retainAll(java.util.Collection)}.
     * @param c collection containing elements to be retained in this set.
     * @return  true if this set changed as a result of the call.
     */
    @Override
    public boolean retainAll(Collection c) {
        lock.lockWrite();
        try {
            return super.retainAll(c);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#contains(java.lang.Object)}.
     * @param o element whose presence in this set is to be tested.
     * @return  true if this set contains the specified element.
     */
    @Override
    public boolean contains(Object o) {
        lock.lockRead();
        try {
            return super.contains(o);
        } finally {
            lock.unlockRead();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#containsAll(java.util.Collection)}.
     * @param c collection to be checked for containment in this set.
     * @return  true if this set contains all of the elements in the specified collection.
     */
    @Override
    public boolean containsAll(Collection c) {
        lock.lockRead();
        try {
            return super.containsAll(c);
        } finally {
            lock.unlockRead();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#forEach(java.util.function.Consumer)}.
     * @param action The action to be performed for each element
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        lockRead();
        try {
            super.forEach(action);
        } finally {
            unlockRead();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#isEmpty()}.
     * @return true if this set contains no elements.
     */
    @Override
    public boolean isEmpty() {
        lock.lockRead();
        try {
            return super.isEmpty();
        } finally {
            lock.unlockRead();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#size()}.
     * @return the number of elements in this set.
     */
    @Override
    public int size() {
        lock.lockRead();
        try {
            return super.size();
        } finally {
            lock.unlockRead();
        }
    }

    /**
     * Synchronized implementation of {@link HashSet#clear()}.
     */
    @Override
    public void clear() {
        lock.lockWrite();
        try {
            super.clear();
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Returns a new collection containing an intersection between this set and another collection without affecting this set.
     * <p>
     * The type of the returned collection is the type of this set.
     * @param c the collection to intersect with this set.
     * @return  a new collection in the type of this set containing an intersection between this set and another collection without
     * affecting this set.
     */
    public Collection<T> intersection(Collection<T> c) {
        lockRead();
        try {
            return intersection(this, c);
        } finally {
            unlockRead();
        }
    }
    
    /**
     * Returns an intersection of all given collections.
     * <p>
     * This static method gets as arguments a variable length list of collections. If the list is empty, the method returns
     * {@code null}, otherwise it returns the intersection of all the collections. The type of the returned collection is the type
     * of the first argument.
     * @param cols    the list of collections to operate on.
     * @return  a collection containing the intersection of all given collections, in the type of the first collection, or null
     *          if there are no arguments.
     */
    public static Collection intersection(Collection ... cols) {
        Collection result = null;
        for (Collection col : cols) {
            if (result == null) {
                result = createResultClass(col);
                result.addAll(col);
            } else
                result.retainAll(col);
        }
        return result;
    }

     /**
     * Returns a new set that contains the union of this set and the given collection.
     * <p>
     * The method instantiates a new set in the type of this set and fills it with the union of this set and the given collection.
     * The current set is not affected.
     * @param c     the collection to unite with this set.
     * @return      a new set in the type of this set that contains the union of this set and the given collection.
     */
    public ZHashSet<T> union(Collection<T> c) {
        try {
            lockRead();
            ZHashSet<T> result = this.getClass().getDeclaredConstructor().newInstance();
            result.addAll(this);
            result.addAll(c);
            return result;
        } catch (Exception ex) {
            return null;
        } finally {
            unlockRead();
        }
    }
    
    /**
     * Adds the elements of this set to the given collection.
     * <p>
     * Synchronizes the access to this set and adds its elements to the given collection.
     * @param col   the collection to add to
     * @return {@code true} if the target collection changed as a result of the call
     */
    public boolean addTo(Collection<T> col) {
        lockRead();
        try {
            return col.addAll(this);
        } finally {
            unlockRead();
        }
    }

    /**
     * Converts this set to a simple array.
     * <p>
     * Synchronizes the access to this set and converts it to a simple array.
     * @param cl the {@code Class} object representing the type of the set elements.
     * @return an array containing all of the elements in this set
     */
    public T[] toArray(Class<T> cl) {
        lockRead();
        try {
            return (T[])toArray((T[])Array.newInstance(cl, size()));
        } finally {
            unlockRead();
        }
    }
    
    /**
     * Returns whether any elements of this set match the provided predicate.
     * <p>
     * Returns whether any elements of this set match the provided predicate. The method synchronizes the set for reading before
     * searching for a match.
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements of this set
     * @return {@code true} if any elements of the set match the provided predicate, otherwise {@code false}
     */
    public boolean anyMatch(Predicate<? super T> predicate) {
        lockRead();
        try {
            return stream().anyMatch(predicate);
        } finally {
            unlockRead();
        }
    }

    private static Collection createResultClass(Collection baseObject) {
        try {
            return baseObject.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            return new ZHashSet();
        }
    }
}
