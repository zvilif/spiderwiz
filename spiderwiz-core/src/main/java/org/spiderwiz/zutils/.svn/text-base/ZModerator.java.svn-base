package org.spiderwiz.zutils;

/**
 * A class used to moderate a repeated action according to a set rate.
 */
public class ZModerator {
    private int totalModerated = 0;
    private ZDate startModeration;
    private final ZTrigger suspendTrigger;
    private int rate;
    final static int DEFAULT_ACTION_RATE = 30000;

    public ZModerator() {
        suspendTrigger = new ZTrigger();
    }
    
    /**
     * Resets the moderator for a new series of actions.
     */
    public synchronized void reset() {
        totalModerated = 0;
        startModeration = ZDate.now();
        rate = getRate();
    }
    
    /**
     * Moderates one action.
     * <p>
     * The method calculates the time that the moderated thread needs to wait until it can execute the next action and pauses
     * the calling thread for that period.
     */
    public void moderate() {
        long wait = 0;
        synchronized(this){
            if (rate > 0) {
                ZDate nextSend = startModeration.add(Math.round((double)ZDate.MINUTE / rate * totalModerated++));
                wait = nextSend.diff(ZDate.now());
            }
        }
        if (wait > 0)
            suspendTrigger.pause(wait);
    }
    
    /**
     * Counts one moderated step without pausing.
     */
    public synchronized void count() {
        ++totalModerated;
    }

    /**
     * Returns the number of actions per minute that are set for the moderator.
     * <p>
     * Override this method to set the moderator rate in actions per minute. The default is 30000 actions per minute.
     * @return the number of actions per minute that are set for the moderator.
     */
    protected int getRate() {
        return DEFAULT_ACTION_RATE;
    }
    
    /**
     * Releases any thread that is paused by the moderator.
     */
    public void cleanup() {
        suspendTrigger.activate();
    }
}
