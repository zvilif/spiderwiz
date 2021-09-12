package org.spiderwiz.core;

import org.spiderwiz.zutils.ZDate;

/**
 *
 * @author @author  zvil
 */
class StatisticInfo extends Statistics {
    protected final static int IN = 0;
    protected final static int OUT = 1;

    /**
     * Default column length for this object. Use it to calculate actual value of extra columns.
     */
    protected final static int COLUMN_LENGTH = 2;

    /**
     * Default constructor
     */
    public StatisticInfo() {
        super(COLUMN_LENGTH);
    }

    /**
     * Constructor that adds column over the default two
     * @param extra
     */
    public StatisticInfo(int extra) {
        super(COLUMN_LENGTH + extra);
    }
    
    public int getActivity() {
        return getActivity(IN);
    }

    public int getOutputActivity() {
        return getActivity(OUT);
    }

    public ZDate getLastActivity() {
        return  getLastActivity(IN);
    }

    public ZDate getLastOuputActivity() {
        return getLastActivity(OUT);
    }

    public int getBandwidth() {
        return getBandwidth(IN);
    }

    public int getOutputBandwidth() {
        return getBandwidth(OUT);
    }

    public float getAvgDelay() {
        return getAvgDelay(IN);
    }

    public long getMaxDelay() {
        return getMaxDelay(IN);
    }

    public void setMaxDelay(long maxDelay) {
    }

    /**
     * Update counters and accumulators in used to calculate performance. If
     * there is no timestamp associated with this transaction update only the
     * bandwidth info
     *
     * @param ts
     * @param rawSize
     */
    public void updateActivity(ZDate ts, int rawSize) {
        updateActivity(IN, ts, rawSize);
    }
    
    /**
     * Update counters and accumulators in used to calculate output rate.
     *
     * @param rawSize
     */
    public void updateOutputActivity(int rawSize) {
        updateActivity(OUT, ZDate.DAY_OF_MESSIAH, rawSize);
    }
}
