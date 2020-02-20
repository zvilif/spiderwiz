package org.spiderwiz.zutils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A synchronized implementation of {@link java.util.HashMap}.
 * <p>
 * The Java implementation of {@link java.util.HashMap} is unsynchronized. This class extends the implementation to provide
 * synchronization.
 * @param <K>   type of key
 * @param <V>   type of value
 */
public class ZHashMap<K, V> extends HashMap<K, V>{
    private final ZLock lock;

    /**
     * Constructs an empty ZHashMap with the default initial capacity (16) and the default load factor (0.75).
     */
    public ZHashMap() {
        super();
        lock = new ZLock();
    }
    
    /**
     * Constructs a new ZHashMap with the same mappings as the specified Map
     * @param m the map whose mappings are to be placed in this map.
     */
    public ZHashMap(Map<? extends K, ? extends V> m) {
        super(m);
        lock = new ZLock();
    }

    /**
     * Locks the map for reading.
     * <p>
     * This method is used by the class internally to provide synchronization, but you can use it directly when you need to perform
     * a synchronized bulk read operation on the map.
     */
    public final void lockRead() {
        lock.lockRead();
    }
    
    /**
     * Locks the map for writing.
     * <p>
     * This method is used by the class internally to provide synchronization, but you can use it directly when you need to perform
     * a synchronized bulk write operation on the map.
     */
    public final void lockWrite() {
        lock.lockWrite();
    }
    
    /**
     * Unlocks the map after reading.
     * <p>
     * This method is used by the class internally, but you should use it directly if you use {@link #lockRead()} directly.
     */
    public final void unlockRead() {
        lock.unlockRead();
    }
    
    /**
     * Unlocks the map after writing.
     * <p>
     * This method is used by the class internally, but you should use it directly if you use {@link #lockWrite()} directly.
     */
    public final void unlockWrite() {
        lock.unlockWrite();
    }

    /**
     * Synchronized implementation of {@link java.util.HashMap#get(java.lang.Object)}.
     * @param key   the key whose associated value is to be returned.
     * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key.
     */
    @Override
    public V get(Object key) {
        lock.lockRead();
        try {
            return super.get(key);
        } finally {
            lock.unlockRead();
        }
    }

    /**
     * Synchronized implementation of {@link java.util.HashMap#put(java.lang.Object, java.lang.Object)}.
     * @param key       key with which the specified value is to be associated.
     * @param value     value to be associated with the specified key.
     * @return          the previous value associated with key, or null if there was no mapping for key.
     */
    @Override
    public V put(K key, V value) {
        lock.lockWrite();
        try {
            return super.put(key, value);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link java.util.HashMap#putAll(java.util.Map)}.
     * @param m     mappings to be stored in this map.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        lock.lockWrite();
        try {
            super.putAll(m);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * This does what {@link java.util.HashMap#putIfAbsent(java.lang.Object, java.lang.Object)} does (plus synchronization),
     * but it always returns the newly stored value.
     * @param key       key with which the specified value is to be associated.
     * @param value     value to be associated with the specified key.
     * @return          the newly stored value.
     */
    public V putIfAbsentReturnNew(K key, V value) {
        lock.lockWrite();
        try {
            V old = putIfAbsent(key, value);
            return old == null ? value : old;
        } finally {
            lock.unlockWrite();
        }
    }
    
    /**
     * Synchronized implementation of {@link java.util.HashMap#remove(java.lang.Object)}.
     * @param key   key whose mapping is to be removed from the map.
     * @return      the previous value associated with key, or null if there was no mapping for key.
     */
    @Override
    public V remove(Object key) {
        lock.lockWrite();
        try {
            return super.remove(key);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Removes a collection of values from the map.
     * @param c collection containing values to be removed from this map.
     * @return true if this map changed as a result of the call.
     */
    public boolean removeAll(Collection<V> c) {
        lock.lockWrite();
        try {
            return values().removeAll(c);
        } finally {
            lock.unlockWrite();
        }
    }

    /**
     * Synchronized implementation of {@link java.util.HashMap#size()}.
     * @return  the number of key-value mappings in this map.
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
     * Synchronized implementation of {@link java.util.HashMap#containsKey(java.lang.Object)}.
     * @param key   the key whose presence in this map is to be tested.
     * @return true if this map contains a mapping for the specified key.
     */
    @Override
    public boolean containsKey(Object key) {
        lockRead();
        try {
            return super.containsKey(key);
        } finally {
            unlockRead();
        }
    }
    
    /**
     * Synchronized implementation of {@link java.util.HashMap#clear()}.
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
}
