package org.spiderwiz.zutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A synchronized implementation of {@link java.util.ArrayList}.
 * <p>
 * The Java implementation of {@link java.util.ArrayList} is unsynchronized. This class extends the implementation to provide
 * synchronization.
 * @param <T>   Type of array elements
 */
public class ZArrayList<T> extends ArrayList<T> {
    private final ZLock lock;

    /**
     * Constructs an empty list with an initial capacity of 10.
     */
    public ZArrayList() {
        lock = new ZLock();
    }
    
    /**
     * Locks the list for reading.
     * <p>
     * This method is used by the class internally to provide synchronization, but you can use it directly when you need to perform
     * a synchronized bulk read operation on the list.
     */
    public final void lockRead() {
        lock.lockRead();
    }
    
    /**
     * Locks the list for writing.
     * <p>
     * This method is used by the class internally to provide synchronization, but you can use it directly when you need to perform
     * a synchronized bulk write operation on the list.
     */
    public final void lockWrite() {
        lock.lockWrite();
    }
    
    /**
     * Unlocks the list after reading.
     * <p>
     * This method is used by the class internally, but you should use it directly if you use {@link #lockRead()} directly.
     */
    public final void unlockRead() {
        lock.unlockRead();
    }
    
    /**
     * Unlocks the list after writing.
     * <p>
     * This method is used by the class internally, but you should use it directly if you use {@link #lockWrite()} directly.
     */
    public final void unlockWrite() {
        lock.unlockWrite();
    }

    /**
     * Synchronized implementation of {@link java.util.ArrayList#add(java.lang.Object)}.
     * @param e element to be appended to this list.
     * @return  true (as specified by {@link java.util.Collection#add(java.lang.Object)}).
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
     * Synchronized implementation of {@link java.util.ArrayList#add(int, java.lang.Object)}.
     * @param index     index at which the specified element is to be inserted.
     * @param element   element to be inserted.
     */
    @Override
    public void add(int index, T element) {
        lock.lockWrite();
        try {
            super.add(index, element);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link java.util.ArrayList#addAll(java.util.Collection)}.
     * @param c collection containing elements to be added to this list.
     * @return true if this list changed as a result of the call.
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        lock.lockWrite();
        try {
            return super.addAll(c);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link java.util.ArrayList#clear()}.
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
     * Synchronized implementation of {@link ArrayList#forEach(java.util.function.Consumer) }
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
     * Synchronized implementation of {@link java.util.ArrayList#get(int)}.
     * @param index index of the element to return.
     * @return the element at the specified position in this list.
     */
    @Override
    public T get(int index) {
        lock.lockRead();
        try {
            return super.get(index);
        } finally {
            lock.unlockRead();
        }
    }

    /**
     * Synchronized implementation of {@link java.util.ArrayList#remove(int)}.
     * @param index the index of the element to be removed.
     * @return the element that was removed from the list.
     */
    @Override
    public T remove(int index) {
        lock.lockWrite();
        try {
            return super.remove(index);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link java.util.ArrayList#remove(java.lang.Object)}.
     * @param o element to be removed from this list, if present.
     * @return  true if this list contained the specified element.
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
     * Returns whether any elements of this array match the provided predicate.
     * <p>
     * Returns whether any elements of this array match the provided predicate. The method synchronizes the set for reading before
     * searching for a match.
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements of this array
     * @return {@code true} if any elements of the array match the provided predicate, otherwise {@code false}
     */
    public boolean anyMatch(Predicate<? super T> predicate) {
        lockRead();
        try {
            return stream().anyMatch(predicate);
        } finally {
            unlockRead();
        }
    }

}
