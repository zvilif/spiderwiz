package org.spiderwiz.zutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Manages the processing of a queue of elements.
 * <p>
 * The class uses a {@link java.util.LinkedList} to hold a queue of elements and manage their processing. Typically the user of the
 * class uses {@link #add(java.lang.Object) add()} to push elements to the head of the buffer, and the buffer itself, running on its own
 * thread(s), pulls the elements from its bottom and dispatches them for processing using a {@link org.spiderwiz.zutils.ZDispenser}
 * object, which is provided to the class {@link #ZBuffer(org.spiderwiz.zutils.ZDispenser) constructor}. A buffer that operates this way
 * is a <em>self-dispensing buffer</em>. The mechanism of this buffer shall be activated by calling {@link #execute()}. Alternatively,
 * buffers can be <em>user-dispensed</em>, in which case the user calls {@link #pull(boolean) pull()} to pull elements from the bottom
 * of the buffer and process them.
 * <p>
 * There is a {@link #ZBuffer(org.spiderwiz.zutils.ZDispenser, int) constructor} version that lets you specify the
 * number of threads that are allocated for processing buffer items. If the
 * {@link #ZBuffer(org.spiderwiz.zutils.ZDispenser) other constructor} is used and this is not a <em>user-dispensed</em> buffer
 * then one thread is allocated.
 * <p>
 * You can call {@link #addUrgent(java.lang.Object) addUrgent()} to push an urgent element. That would place the element at the bottom
 * of the buffer (on top of previous urgent elements).
 * <p>
 * Other features include setting timeout for pulling from an empty buffer by calling {@link #setTimeout(long) setTimeout()},
 * setting buffer capacity by calling {@link #setMaxCapacity(int) setMaxCapacity()}, requesting to empty the buffer when it is full
 * with {@link #setEmptyOnFull(boolean) setEmptyOnFull()}, requesting that when adding an item to a full buffer the bottom element
 * of the buffer will be discarded without processing by calling {@link #setDiscardOnFull(boolean) setDiscardOnFull()}
 * and marking the buffer as <em>lossless</em> by calling
 * {@link #setLossless(boolean) setLossless()} in which case pushing elements to the buffer when it is full would block the calling
 * thread until some space is freed.
 * <p>
 * You can also assign a backup file to the buffer, in which case when the buffer is full excess items will be stored in the given
 * disc file and retrieved for pushing into the main buffer when space is freed. To do that activate the buffer with
 * {@link #execute(java.lang.String) execute(filename)}. If you use this option, the buffer's elements will be serialized to the
 * file through their {@link Object#toString()} method, and, unless the element type is {@link String}, you should override
 * {@link #fromString(java.lang.String) fromString()} to provide deserialization code.
 * @param <T> type of elements managed by the buffer.
 * @see ZDispenser
 */
public class ZBuffer<T> implements Runnable {
    private static final int MAX_ZBUFFER_CAPACITY = 60000;
    private static final long DEFAULT_FLUSH_INTERVAL = ZDate.SECOND;

    private final ZDispenser<T> dispenser;
    private final LinkedList<T> buffer;
    private int maxCapacity = MAX_ZBUFFER_CAPACITY;
    private boolean abort = false;
    private int urgent = 0;     // count number of urgent items that require flushing after dispension
    private boolean needsFlush = false, bufferFull = false, hadException = false;
    private long timeout = DEFAULT_FLUSH_INTERVAL;
    private boolean discardOnFull = false, emptyOnFull = true, lossless = false;
    private BufferedReader inFile = null;
    private PrintWriter outFile = null;
    private String backupFile = null;
    private final int threads;      // Number of threads used for dispensing buffer items

    /**
     * Constructs a single-thread buffer.
     * <p>
     * Constructs a single-thread buffer with a given {@link org.spiderwiz.zutils.ZDispenser dispenser}. If {@code dispenser} is not
     * {@code null} a <em>self-disposing buffer</em> is constructed. If it is {@code null} the constructed buffer is
     * <em>user-dispensed</em>, i.e. the user needs to call {@link #pull(boolean) pull()} to process buffer elements.
     * @param dispenser the buffer's dispenser, or null if this is a <em>user-dispensed buffer</em>.
     */
    public ZBuffer(ZDispenser<T> dispenser) {
        this(dispenser, 1);
    }

    /**
     * Constructs a buffer.
     * <p>
     * Constructs a buffer with a given {@link org.spiderwiz.zutils.ZDispenser dispenser}. If {@code dispenser} is not
     * {@code null} a <em>self-disposing buffer</em> is constructed. If it is {@code null} the constructed buffer is
     * <em>user-dispensed</em>, i.e. the user needs to call {@link #pull(boolean) pull()} to process buffer elements.
     * <p>
     * The {@code threads} parameter determines the number of the threads to spawn for processing buffer items. If negative,
     * as many as available CPUs on this machine will be spawned. If Zero, this is not a <em>user-dispensed buffer</em>.
     * Note that a multi-thread buffer cannot guarantee processing order.
     * @param dispenser     the buffer's dispenser, or null if this is a <em>user-dispensed buffer</em>.
     * @param threads       the number of the threads to spawn for processing buffer items. If negative, as many as
     *                      available CPUs on this machine will be spawned. If Zero, this is not a user-dispensed buffer.
     */
    public ZBuffer(ZDispenser<T> dispenser, int threads) {
        this.dispenser = dispenser;
        this.threads = threads;
        buffer = new LinkedList<>();
    }

    /**
     * Sets the maximum time in milliseconds to wait for data when pulling from an empty buffer.
     * <p>
     * Sets the maximum time in milliseconds a pull operation will wait on an empty buffer until an element is available. If
     * {@code timeout} is zero, the operation will wait unlimited until an element is available or the buffer operation is
     * aborted by {@link #cleanup(boolean) cleanup()}.
     * <p>
     * If {@code timeout} is a positive value and a pull operation expires while the buffer is still empty, the pull result will be
     * {@code null}.
     * @param   timeout the maximum time in milliseconds to wait for data when pulling from an empty buffer, or zero if waiting is
     *          unlimited.
     */
    public final void setTimeout(long timeout) {
        synchronized(buffer){
            this.timeout = timeout;
        }
    }

    /**
     * Sets the maximum number of elements in the buffer.
     * <p>
     * The buffer is considered full when the defined maximum capacity is reached. The default value is 60000.
     * @param maxCapacity   maximum number of elements in the buffer.
     */
    public final void setMaxCapacity(int maxCapacity) {
        synchronized(buffer){
            this.maxCapacity = maxCapacity;
        }
    }

    /**
     * Sets discard-on-full value.
     * <p>
     * When discard-on-full is set to {@code true}, adding an element to a full buffer will cause the element at the bottom of the
     * buffer to be discarded without processing in order to make room for the new element pushed on top. The default is {@code false}.
     * @param discardOnFull true to set discard-on-full on, false to set it off.
     */
    public final void setDiscardOnFull(boolean discardOnFull) {
        synchronized(buffer){
            this.discardOnFull = discardOnFull;
        }
    }

    /**
     * Sets empty-on-full value.
     * <p>
     * When empty-on-full is set to {@code true}, adding an element to a full buffer will cause the entire buffer to be emptied,
     * except of the {@link #addUrgent(java.lang.Object) urgent} elements,
     * without processing. The added element will be placed in the empty buffer. <b>This is the default</b>.
     * @param emptyOnFull   true to set empty-on-full on (the default), false to set it off.
     */
    public final void setEmptyOnFull(boolean emptyOnFull) {
        synchronized(buffer){
            this.emptyOnFull = emptyOnFull;
        }
    }

    /**
     * Sets lossless value.
     * <p>
     * When lossless is set to true, adding elements to the buffer when it is full would block the calling thread until some space is
     * freed. The default is {@code false}.
     * @param lossless  true to set lossless on, false to set it off.
     */
    public final void setLossless(boolean lossless) {
        synchronized(buffer) {
            this.lossless = lossless;
        }
    }

    /**
     * Activates the buffer.
     * <p>
     * This method must be called if the buffer is <em>self-dispensing</em>. It has no effect if no dispenser was provided to
     * the class {@link #ZBuffer(org.spiderwiz.zutils.ZDispenser) constructor}.
     */
    public void execute() {
        execute(null);
    }
    
    /**
     * Activates the buffer with a backup file.
     * <p>
     * Activate the buffer with this method if you want to specify a backup file to store excess elements when the buffer is full.
     * When buffer space is freed, elements will be retrieved from the file and added to the buffer.
     * <p>
     * This method also activates a <em>self-dispensing</em> buffer. If no dispenser was provided to the class
     * {@link #ZBuffer(org.spiderwiz.zutils.ZDispenser) constructor}, the self-dispensing mechanism will not be activated but
     * the backup file will still be set.
     * @param backupFile    backup file pathname.
     */
    public final void execute(String backupFile) {
        this.backupFile = backupFile;
        abort = false;
        if (!discardOnFull && dispenser != null) {
            int n = threads < 0 ? Runtime.getRuntime().availableProcessors() : threads;
            while (--n >= 0)
                new Thread (this).start();
        }
    }
    
    /**
     * Appends an element to the top of the buffer.
     *<p>
     * Appends an element to the top of the buffer, i.e. to the tail of the queue. If the buffer is full the result depends on the
     * various settings of the buffer, as follow:
     * <ul>
     * <li>If {@link #setLossless(boolean) lossless} is set to {@code true} the calling thread is blocked until buffer space for one
     * element is freed.</li>
     * <li>if {@link #setEmptyOnFull(boolean) empty-on-full} is set to {@code true} (the default) the buffer will be cleared entirely,
     * except of the {@link #addUrgent(java.lang.Object) urgent} elements, before the new element is placed in it.</li>
     * <li>if {@link #setDiscardOnFull(boolean) discard-on-full} is set to {@code true} the element at the bottom of the buffer (head
     * of the queue) will be removed and discarded without being processed.</li>
     * <li>if a backup file name is supplied in the call to {@link #execute(java.lang.String) execute()} the new element will be
     * stored in the file, to be retrieved later when the buffer has space.</li>
     * <li>if none of the above is set, the element will not be appended to the buffer and the method will return {@code false}.
     * </ul>
     * @param o element to be appended to this buffer.
     * @return true if any only if the element was appended to the buffer.
     * @see #addUrgent(java.lang.Object) addUrgent()
     */
    public final boolean add(T o) {
        if (!checkSpace())
            return false;
        synchronized (buffer) {
            if (abort)
                return true;
            buffer.add(o);
            buffer.notifyAll();
        }
        return true;
    }

    /**
     * Inserts an urgent element to the buffer.
     * <p>
     * This method inserts an urgent element at the bottom of the buffer (head of the queue) on top of previously inserted urgent
     * elements. This causes the processing of urgent elements before other elements are processed.
     * <p>
     * See {@link #add(java.lang.Object) add()} for what happens if the buffer is full.
     * @param o element to be inserted to this buffer.
     * @return true if any only if the element was inserted into the buffer.
     * @see #add(java.lang.Object) add()
     */
    public final boolean addUrgent (T o) {
        if (!checkSpace())
            return false;
        synchronized (buffer) {
            if (abort)
                return true;
            buffer.add(urgent++, o);
            buffer.notifyAll();
        }
        return true;
    }
    
    /**
     * Retrieves and removes the bottom element of this buffer (head of the queue).
     * <p>
     * Used if the buffer is <em>user-dispensed</em>. <em>Self-dispensed</em> buffers process the queue by themselves and therefore
     * this method shall not be used on them.
     * @param wait      if true and the buffer is empty, wait the {@link #setTimeout(long) set time} until an element is available.
     * @return the bottom element of the buffer or null if the buffer is empty and wait time has expired.
     */
    public T pull(boolean wait) {
        return remove(wait);
    }

    /**
     * Returns true if the buffer is empty.
     * @return true if and only if the buffer is empty.
     */
    public boolean isEmpty() {
        synchronized(buffer){
            return buffer.isEmpty();
        }
    }
    
    /**
     * Deserializes a string into an object of the type managed by the buffer.
     * <p>
     * This method is used if the buffer is {@link #execute(java.lang.String) activated with a backup file}. In that case you need
     * to override this method to provide code for deserializing file records, kept as strings, into objects of the type managed
     * by the buffer. There is no need to override the method if the element type of the buffer is {@link String}.
     * @param line  a string representing a serialized buffer's element.
     * @return an object of the type managed by the buffer.
     */
    protected T fromString(String line) {
        return (T)line;
    }
    
    /**
     * Cleans up buffer resources.
     * <p>
     * Call this method when you do not need the buffer any more. If {@code flush} is {@code true} the caller thread will be
     * blocked until all remaining buffer elements are dispensed and processed.
     * @param flush true if all remaining buffer elements shall be  dispensed and processed.
     */
    public void cleanup (boolean flush) {
        abort = true;
        if (flush) {
            T o;
            while (true) {
                synchronized(buffer) {
                    if (buffer.isEmpty() && outFile == null)
                        break;
                    o = fetchNextItem();
                }
                if (o != null) {
                    if (dispenser != null)
                        dispenser.dispense(o, false);
                    synchronized(buffer){
                        if (urgent > 0)
                            --urgent;
                    }
                }
            }
            if (dispenser != null)
                dispenser.dispense(null, true);
        }
        synchronized (buffer) {
            buffer.notifyAll();
            buffer.clear();
            urgent = 0;
            try {
                if (outFile != null)
                    outFile.close();
                if (inFile != null)
                    inFile.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Implementation of {@link Runnable#run()}.
     * <p>
     * Do not call or overload.
     */
    @Override
    public final void run() {
        while (!abort) {
            T o = remove(true);
            dispenser.dispense(o, needsFlush);
        }
    }

    /**
     * Check if there is space in the buffer, making space if necessary.
     * @return 
     */
    private boolean checkSpace() {
        T o;
        synchronized(buffer) {
            do {
                // Exit normally if buffer has space
                if (buffer.size() < maxCapacity) {
                    bufferFull = false;
                    return true;
                }
                
                // If buffer doesn't have space and buffer is defined as 'lossless' wait until space is available
                if (lossless) {
                    try {
                        bufferFull = true;
                        buffer.wait();
                        if (abort)
                            return false;
                    } catch (InterruptedException ex) {
                        return false;
                    }
                }
            } while (lossless);
            
            // If buffer is not lossless and 'dispenseOnFull' was not defined then if a buffer file is used, move all non-urgent
            // lines from the buffer to the file. Otherwise return with value 'false'
            if (!discardOnFull) {
                if (backupFile == null) {
                    if (emptyOnFull) {
                        LinkedList<T> head = new LinkedList<>(buffer.subList(0, urgent));
                        buffer.clear();
                        buffer.addAll(head);
                    }
                    return false;
                }
                if (outFile == null) {
                    try {
                        File file = new File(backupFile);
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                        outFile = new PrintWriter(new FileOutputStream(file));
                    } catch (IOException ex) {
                        handleException(ex);
                        return false;
                    }
                }
                while (urgent < buffer.size())
                    outFile.println(buffer.remove(urgent).toString());
                outFile.flush();
                return true;
            }
            
            // If buffer is defined as 'dispenseOnFull' dispense the bottom element (although previous one has not been handled yet)
            // to make room for a new element
            needsFlush = false;
            o = buffer.remove();
            if (o != null && urgent > 0)
                needsFlush = --urgent == 0;
        }
        if (o != null && dispenser != null)
            dispenser.dispense(o, needsFlush);
        return false;
    }
    
    /**
     * Remove an element from the beginning of the queue. If the queue is empty wait until it is not, or the set timeout.
     * @return the element
     */
    private T remove(boolean wait) {
        T o = null;
        synchronized (buffer) {
            try {
                needsFlush = false;
                if (!abort && wait && buffer.isEmpty() && outFile == null) {
                    buffer.wait(timeout);
                    if (abort)
                        return null;
                }
                o = fetchNextItem();
                // we need to flush the dispension when we consume the last urgent item
                if (o != null && urgent > 0) {
                    needsFlush = --urgent == 0;
                }
                if (bufferFull)
                    buffer.notifyAll();
            } catch (InterruptedException | NoSuchElementException ex) {}
        }
        return o;
    }
    
    private T fetchNextItem() {
        if (urgent == 0 && outFile != null) {
            try {
                if (inFile == null)
                    inFile = new BufferedReader(new InputStreamReader(new FileInputStream(backupFile)));
                String line = inFile.readLine();
                if (line != null)
                    return fromString(line);
                inFile.close();
                outFile.close();
                inFile = null;
                outFile = null;
            } catch (IOException ex) {
                handleException(ex);
                return null;
            }
        }
        return buffer.isEmpty() ? null : buffer.remove();
    }
    
    private void handleException(Exception ex) {
        if (dispenser != null)
            dispenser.handleException(ex);
        else {
            if (!hadException)
                ex.printStackTrace();
            hadException = true;
        }
    }
}
