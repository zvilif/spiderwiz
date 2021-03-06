package org.spiderwiz.zutils;

/**
 * Used by {@link org.spiderwiz.zutils.ZBuffer} to dispense buffered objects.
 * <p>
 * A {@link org.spiderwiz.zutils.ZBuffer} that runs on its own thread uses an implementation of this interface for dispensing the
 * pulled objects.
 * @param <T> type of the buffered objects.
 * @see ZBuffer
 */
public interface ZDispenser<T> {

    /**
     * Dispenses the objects pulled from the associated {@link org.spiderwiz.zutils.ZBuffer} object.
     * <p>
     * Implement this method to handle objects that are pulled from the associated buffer. The {@code object} parameter is the
     * dispensed object. It can be {@code null} if the buffer's pull waiting time is limited and there are no objects in the buffer.
     * The {@code urgent} parameter tells that the object was pushed to the bottom of the buffer because of its urgency, and therefore
     * it should be handled accordingly. For instance if the dispensed objects are written to a communication socket, the socket
     * should be flushed after writing urgent objects.
     * @param object    the object pulled from the associated buffer, or null if the buffer is empty.
     * @param urgent    marks an urgent object. 
     */
    void dispense (T object, boolean urgent);
    
    /**
     * Handles an exception occurred while processing the associated {@link org.spiderwiz.zutils.ZBuffer} object.
     * <p>
     * Implement this method to handle exceptions in the associated buffer.
     * @param ex the exception.
     */
    void handleException(Exception ex);
}
