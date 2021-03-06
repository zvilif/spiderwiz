package org.spiderwiz.core;

import org.spiderwiz.zutils.ZBuffer;
import org.spiderwiz.zutils.ZDate;

/**
 * <em>Data Object Resetting</em> is the operation that takes place when a <em>consumer</em> of a specific <em>data object type</em>
 * detects loss of data. This may happen when the application starts or when data is lost due to a communication problem. In this
 * case, the consumer broadcasts a <em>reset request</em>, asking the producers of that object type to reset all missing objects to
 * their most up-to-date state.
 * <p>
 * When receiving a reset request, the <em>Spiderwiz framework</em> creates a {@code Resetter} object and calls
 * {@link Main#onObjectReset(org.spiderwiz.core.Resetter) Main.onObjectReset()} to give a chance to the application to fulfill the
 * request programmatically. The method returns {@code true} when done this way.
 * <p>
 * If {@code onObjectReset()} returns {@code false} then the framework does automatic reset by fulfilling the request from the
 * governed <em>data object tree</em>. 
 * <p>
 * That parameter to {@code onObjectReset()} is an object of type {@code Resetter} that is documented here.
 * It contains information about the requested object type. It also serves as a
 * carrier to deliver objects back to the requester. As such, it facilitates efficient
 * delivery by directing the reset items to the channel over which they were requested, as well as providing a mechanism for
 * buffering the data and moderate its delivery to avoid network congestion in case of bulk delivery of a large amount of items.
 * The class has few methods that can be used to control the delivery process.
 * <p>
 * Note that even if {@code onObjectReset()} returns {@code false}, the framework uses the same {@code Resetter} object that
 * was provided to it for automatic object reset, therefore giving the method a chance to fine tune the {@code Resetter} object
 * used for automatic reset.
 */
public final class Resetter {
    private class Streamer extends Dispenser<DataObject> {
        @Override
        public void dispense(DataObject object, boolean flush) {
            if (object == null) {
                if (isEndOfData() && buffer.isEmpty()) {
                    abort();
                    DataManager.getInstance().getEventDispatcher(objectCode).resetCompleted(Resetter.this);
                }
                return;
            }
            try {
                if (sequenceManager == null) {
                    moderator.moderate();
                    object.propagate(true, null, null);
                }
                else
                    sequenceManager.transmitModeratedObject(object);
            } catch (Exception ex) {
                Main.getInstance().sendExceptionMail(ex, String.format(CoreConsts.AlertMail.EXCEPTION_RESET, object.getObjectCode()),
                    null, false);
            }
            count();
        }
    }
    
    private final SequenceManager sequenceManager;
    private final String objectCode;
    private final ZBuffer<DataObject> buffer;
    private boolean initialized = false, endOfData = false, aborted = false;
    private final TransmitModerator moderator;
    private int resetCount;
    private ZDate startReset;

    Resetter(SequenceManager sequenceManager, String commandCode) {
        this.sequenceManager = sequenceManager;
        this.objectCode = commandCode;
        buffer = new ZBuffer<>(new Streamer());
        buffer.setTimeout(0);
        buffer.setMaxCapacity(200000);
        moderator = new TransmitModerator();
    }

    /**
     * Returns the <a href='../core/DataObject.html#ObjectCode'>{@code Object Code}</a> of the data objects requested by this
     * resetter.
     * @return the Object Code of the data objects requested by this resetter.
     */
    public String getObjectCode() {
        return objectCode;
    }
    
    /**
     * Resets one item.
     * <p>
     * Your reset procedure shall call this method for each item you want to reset. The method accepts as a parameter one <em>data
     * object</em> that shall be of the same
     * <a href='../core/DataObject.html#ObjectCode'>{@code Object Code}</a> as the resetter object and buffers it for delivery to the
     * reset requester.
     * <p>
     * The method returns {@code true} if the operation is successful. It returns {@code false} in the following cases:
     * <ul>
     * <li>The type of the item to reset differs from this Resetter type.</li>
     * <li>The buffer is full and is not defined {@link #setLossless(boolean) lossless}.</li>
     * <li>This Resetter has been {@link #isAborted() aborted}.</li>
     * </ul>
     * @param object    the item to reset.
     * @return true if and only if the reset object has been buffered for delivery successfully.
     */
    public boolean resetObject(DataObject object) {
        if (object.getObjectCode() == null || !object.getObjectCode().equals(objectCode))
            return false;
        // Do not reset objects received from other nodes
        if (object.getOriginUUID() != null && !object.getOriginUUID().equals(Main.getInstance().getAppUUID()))
            return true;
        synchronized(this){
            if (aborted)
                return false;
            if (!initialized) {
                initialized = true;
                endOfData = false;
                resetCount = 0;
                startReset = ZDate.now();
                if (sequenceManager != null)
                    sequenceManager.resetModerator();
                else
                    moderator.reset();
                buffer.execute();
            }
        }
        return buffer.add(object);
    }
    
    /**
     * Marks end of data.
     * <p>
     * Call this method to mark the end of reset data. When the reset buffer is fully flushed for delivery
     * {@link org.spiderwiz.core.Main#onResetCompleted(org.spiderwiz.core.Resetter) Main.onResetCompleted()} is called.
     */
    public synchronized void endOfData() {
        endOfData = true;
        // Add a null object to the buffer so that we will know when the buffer is empty.
        if (initialized)
            buffer.add(null);
    }

    /**
     * Sets the maximum capacity of the resetter buffer.
     * <p>
     * The resetter mechanism uses a buffer to moderate delivery rate. When the buffer gets full, if the resetter is set to
     * {@link #setLossless(boolean) lossless} then further calls to {@link #resetObject(org.spiderwiz.core.DataObject) resetObject()}
     * block until buffer space is freed, otherwise excess data is discarded and data is lost.
     * <p>
     * Call this method to set the resetter buffer capacity. The default is 200K items.
     * @param capacity  Resetter buffer capacity in number of reset items.
     */
    public void setMaxCapacity(int capacity) {
        buffer.setMaxCapacity(capacity);
    }

    /**
     * Sets lossless mode.
     * <p>
     * The resetter mechanism uses a buffer to moderate delivery rate. When the buffer gets full, if the resetter is set to
     * {@link #setLossless(boolean) lossless} then further calls to {@link #resetObject(org.spiderwiz.core.DataObject) resetObject()}
     * block until buffer space is freed, otherwise excess data is discarded and data is lost.
     * <p>
     * By default resetters are lossy. Call this method with {@code lossless} value set to {@code true} to make it lossless.
     * @param lossless  true to set a lossless resetter, false to set a lossy resetter.
     */
    public void setLossless(boolean lossless) {
        buffer.setLossless(lossless);
    }
    
    /**
     * Sets the reset rate in elements per minute.
     * <p>
     * Calls this method to set reset rate to the given amount of elements per minute. A zero
     * value eliminates the moderation effect, causing every element to be sent as soon as possible. A negative value (the default)
     * tells the resetter to take the value from {@code [stream rate]} property of the
     * <a href="../core/doc-files/config.html">application's configuration file</a>. The default value, if that property does not
     * exist, is 30,000 elements per minute.
     * @param rate reset rate in elements per minute, zero to eliminate moderation and negative value to use the configured value.
     */
    public void setResetRate(int rate) {
        moderator.setExplicitRate(rate);
    }

    /**
     * Returns the number of items that were delivered by this resetter since it restarted.
     * @return the number of items that were delivered by this resetter since it restarted.
     */
    public synchronized final int getResetCount() {
        return resetCount;
    }

    /**
     * Returns the time reset was restarted.
     * @return the time reset was restarted.
     */
    public synchronized final ZDate getStartReset() {
        return startReset;
    }

    /**
     * Returns true if the current Resetter object is aborted by an overriding reset request.
     * <p>
     * This happens when a new object reset request arrives from the same channel that previously requested a reset for the same
     * object type and processing of that request has not finished yet. In this case the current object stops processing further
     * {@link #resetObject(org.spiderwiz.core.DataObject) resetObject()} requests, but your implementation of
     * {@link Main#onObjectReset(org.spiderwiz.core.Resetter) onObjectReset()} would not know it. You can call this method to
     * check if resetting has been aborted and do something accordingly.
     * @return true if and only if the current Resetter object is aborted by an overriding reset request.
     */
    public synchronized boolean isAborted() {
        return aborted;
    }
    
    SequenceManager getSequenceManager() {
        return sequenceManager;
    }
    
    private synchronized boolean isEndOfData() {
        return endOfData;
    }

    /**
     * Increment the object reset counter
     */
    private synchronized void count() {
        ++resetCount;
    }
    
    /**
     * Abort resetting.
     */
    synchronized void abort() {
        aborted = true;
        initialized = false;
        cleanup(false);
    }
    
    /**
     * Cleanup the object
     * @param flush true if buffer shall be flush before exiting
     */
    void cleanup(boolean flush) {
        buffer.cleanup(flush);
        moderator.cleanup();
    }
}
