package org.spiderwiz.zutils;

/**
 * Controls thread execution.
 * <p>
 * A trigger is a synchronization object with the following features:
 * <ul>
 * <li>It can be waited on, with or without timeout interval.</li>
 * <li>It can be released to let the waiting thread continue.</li>
 * <li>It can be released before it is being requested so that the requesting thread will continue without pausing.</li>
 * <li>It can be released only if it is being requested, i.e. there is a thread pausing on it.</li>
 * </ul>
 * 
 */
public final class ZTrigger {
    private boolean released = false;
    private boolean requested = false;
    
    /**
     * Pauses the calling thread until the trigger is released or the given timeout interval has elapsed.
     * <p>
     * The calling thread does not pause if the trigger was released before it is requested.
     * @param timeout   the maximum time to wait in milliseconds, or zero if the calling thread shall wait unlimited until the
     *                  trigger is released.
     */
    public synchronized void pause(long timeout) {
        try {
            // Don't wait if already released.
            if (!released) {
                requested = true;
                    wait(timeout);
            }
        } catch (InterruptedException ex) {
        } finally {
            released = requested = false;
        }
    }
    
    /**
     * Releases the trigger.
     * <p>
     * This method can be used to release the trigger before it is being requested, so that a following request for the trigger
     * will cause the calling thread to continue without pausing.
     */
    public synchronized void activate() {
        notifyAll();
        released = true;
    }
    
    /**
     * Releases the trigger only if it has been requested, i.e. it is paused on by another thread.
     * <p>
     * Use this method if you want to release the thread (or threads) that is pausing on the trigger, but you don't want the trigger
     * to be released before it is requested.
     */
    public synchronized void release() {
        if (requested)
            activate();
    }
}
