package org.spiderwiz.zutils;

/**
 * A base class for implementations of actions running in a looping thread with set suspension time between iterations.
 * <p>
 * Extensions of this class shall at least implement two abstract methods:
 * <p>
 * {@link #doLoop()} - the action to execute in one loop iteration.
 * <p>
 * {@link #getLoopInterval()} - determines the time to wait until the next loop iteration.
 * <p>
 * Implementations should also call {@link #execute()} to spawn the thread and start the loop.
 */
public abstract class ZThread implements Runnable {
    private boolean abort = false;
    private final ZTrigger myTrigger;
    private Thread myThread = null;

    /**
     * Constructs a looping thread object.
     */
    protected ZThread() {
        myTrigger = new ZTrigger();
    }

    /**
     *  Implement this method with the code to execute at each loop iteration.
     */
    protected abstract void doLoop();
    
    /**
     * Returns a value that determines the time interval to wait between loop iterations.
     * <p>
     * Implement this method to specify the time interval to wait between iterations, as follows:
     * <ul>
     * <li>If the method returns a positive number then this is the number of milliseconds of the loop rate.</li>
     * <li>If the method returns a negative number then the thread action runs once then waits for an explicit activation by
     * {@link #activate()} for the next iteration.</li>
     * <li>If the method returns zero then the thread action runs daily at a specific time of the day. In this case you must override
     * {@link #getExecutionHour()}.</li>
     * </ul>
     * @return a positive number of milliseconds of the iteration rate, a negative value for explicitly activated iterations
     * or zero for execution at a specific time of the day.
     * @see #getExecutionHour()
     */
    protected abstract long getLoopInterval();
    
    /**
     * Returns the time of the day, as a number of milliseconds since midnight, that the thread action runs at.
     * <p>
     * Override this function if you return zero in {@link #getLoopInterval()} to set the time of the day that the thread action
     * runs at daily. The time of the day is specified as the number of seconds since midnight.
     * <p>
     * By default the method returns a negative value, which means that the thread action shall be explicitly activated by
     * {@link #activate()}.
     * @return the time of the day as number of milliseconds after midnight that the thread action runs at.
     * @see #getLoopInterval()
     */
    protected int getExecutionHour() {return -1;}
    
    /**
     * Spawns the thread and starts the thread loop.
     */
    public synchronized void execute() {
        setAbort(false);
        myThread = new Thread(this);
        myThread.setDaemon(true);
        myThread.start();
    }

    /**
     * Activates the thread action explicitly.
     * <p>
     * Calling this method is the only way to run the thread action if {@link #getLoopInterval()} returns a negative value. In
     * other cases, the method activates the action immediately regardless the set loop rate.
     */
    public void activate() {
        myTrigger.activate();
    }

    /**
     * Aborts the thread action if it runs and resets the object to its pre-execute state.
     */
    public final void kill() {
        synchronized(this) {
            if (myThread == null)
                return;
            myThread.interrupt();
            myThread = null;
        }
        cleanup();
    }
    
    /**
     * Terminates the loop and shuts down the thread.
     */
    public void cleanup() {
        setAbort(true);
        myTrigger.activate();
    }
    
    private synchronized boolean isAbort() {
        return abort;
    }

    private synchronized void setAbort(boolean abort) {
        this.abort = abort;
    }

    private synchronized void setMyThread(Thread myThread) {
        this.myThread = myThread;
    }
    
    /**
     * Internal implementation of {@link Runnable#run()}.
     */
    @Override
    public final void run() {
        while (!isAbort()) {
            long interval = getLoopInterval();
            if (interval == 0) {
                int wait = 0;
                int executionTime = getExecutionHour();
                if (executionTime >= 0) {
                    ZDate now = ZDate.now();
                    ZDate nextActivation = now.getMidnight().add(executionTime);
                    wait = (int)nextActivation.diff(now);
                    if (wait <= 0)
                        wait += ZDate.DAY;
                }
                myTrigger.pause(wait);
                if (isAbort())
                    return;
            }
            doLoop();
            if (interval < 0) {
                setMyThread(null);
                return;
            } else if (interval > 0 && !isAbort())
                myTrigger.pause(interval);
        }
    }
}
