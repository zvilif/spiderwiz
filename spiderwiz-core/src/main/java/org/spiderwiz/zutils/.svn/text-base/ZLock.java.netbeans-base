package org.spiderwiz.zutils;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An implementation of {@link ReentrantReadWriteLock} with easier read-write lock mechanism.
 */
public final class ZLock extends ReentrantReadWriteLock{

    /**
     * Creates a new lock with default (nonfair) ordering properties.
     */
    public ZLock() {
    }

    /**
     * Creates a new lock with the given fairness policy.
     * @param fair true if this lock should use a fair ordering policy.
     */
    public ZLock(boolean fair) {
        super(fair);
    }

    /**
     * Locks this lock for reading.
     */
    public void lockRead() {
        readLock().lock();
    }
    
    /**
     * Unlocks this lock after being locked for reading.
     */
    public void unlockRead() {
        readLock().unlock();
    }
    
    /**
     * Locks this lock for writing.
     */
    public void lockWrite() {
        writeLock().lock();
    }
    
    /**
     * Unlock this lock after being locked for writing.
     */
    public void unlockWrite() {
        writeLock().unlock();
    }
}
