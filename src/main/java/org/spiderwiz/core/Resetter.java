package org.spiderwiz.core;

import org.spiderwiz.zutils.ZBuffer;
import org.spiderwiz.zutils.ZDate;

/**
 * <em>Data Object Resetting</em> is the operation that takes place when a <em>consumer</em> of a specific <em>data object type</em>
 * detects loss of data. This may happen when the application starts up or when data is lost due to a communication problem. In this
 * case, the consumer broadcasts a <em>reset request</em>, asking the producers of that object type to reset all missing objects to
 * their most up-to-date state.
 * <p>
 * When receiving a reset request, <em>Spiderwiz</em> framework first tries to fulfill the request from the data governed by the
 * framework. If the requested data objects are not {@link org.spiderwiz.core.DataObject#isDisposable() disposable} and therefore kept
 * in the <em>data object tree</em> of the producer, the framework automatically resets the objects from the memory image of the
 * tree. Whether this is done or not, the framework also calls
 * {@link org.spiderwiz.core.Main#onObjectReset(org.spiderwiz.core.Resetter) Main.onObjectReset()}. This method can be implemented to
 * reset the requested objects by another mechanism, such as a database.
 * <p>
 * {@link org.spiderwiz.core.Main#onObjectReset(org.spiderwiz.core.Resetter) Main.onObjectReset()} has a parameter of type
 * {@link org.spiderwiz.core.Resetter} that contains information about the requested object type. It also facilitates efficient object
 * delivery by directing the reset items to the data channel over which they were requested, as well as providing a mechanism for
 * buffering the data and moderate its delivery to avoid network congestion due to a bulk delivery of large amount of items. This class
 * is documented here.
 *
 */
public final class Resetter {
    private class Streamer extends Dispenser<DataObject> {
        @Override
        public void dispense(DataObject object, boolean flush) {
            if (object == null) {
                if (isEndOfData() && buffer.isEmpty()) {
                    restart();
                    EventDispatcher.getInstance().resetCompleted(Resetter.this);
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
    private boolean initialized = false, endOfData;
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
     * reset requestor.
     * @param object    the item to reset.
     * @return true if and only if the object code of the parameter is the same as of this resetter.
     */
    public boolean resetObject(DataObject object) {
        if (object.getObjectCode() == null || !object.getObjectCode().equals(objectCode))
            return false;
        synchronized(this){
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
     * Restarts resetting.
     * <p>
     * Call this method when you want to stop the reset procedure, clear the resetter buffer and restart the entire reset procedure.
     */
    public synchronized void restart() {
        if (initialized) {
            initialized = false;
            cleanup(false);
        }
    }
    
    /**
     * Marks end of data.
     * <p>
     * Call this method to mark end of reset data. When the reset buffer is fully flushed for delivery
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
     * Sets the reset rate in elements per per minute.
     * <p>
     * Calls this method to set reset rate to the given amount of elements per minute. A negative
     * value eliminates the moderation effect, causing every element to be sent as soon as possible. If this method is not called,
     * the resetter takes the value from {@code [stream rate]} property of the
     * <a href="../core/doc-files/config.html">application's configuration file</a>. The default value, if that property does not
     * exist, is 30,000 elements per minute.
     * @param rate reset rate in elements per per minute.
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
    
    SequenceManager getSequenceManager() {
        return sequenceManager;
    }
    
    private synchronized boolean isEndOfData() {
        return endOfData;
    }

    /**
     * Increment the object dailyReset counter
     */
    private synchronized void count() {
        ++resetCount;
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
