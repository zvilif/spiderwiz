package org.spiderwiz.core;

import org.spiderwiz.zutils.ZDate;

/**
 * Manages statistics calculation for a table with several columns
 * @author Zvi
 */
class Statistics {
    private static final int STAT_WINDOW = ZDate.MINUTE * 5;
    private class Column {
        private int curActions = 0, prevActions = 0;
        private ZDate lastAct = null;
        private long curMmaxDelay = 0, prevMaxDelay = 0, curDelays = 0, prevDelays = 0;
        private ZDate curHour = null, prevHour = null;
        private int curTsActions = 0, prevTsActions = 0;
        private long curSize = 0, prevSize = 0;

        public Column() {
            curHour = prevHour = ZDate.now();
        }

        private synchronized void resetOnNoActivity() {
            if (lastAct != null && lastAct.elapsed() >= STAT_WINDOW) {
                reset();
            }
        }

        synchronized void reset() {
            prevActions = curActions = curTsActions = prevTsActions = 0;
            curMmaxDelay = prevMaxDelay = curDelays = prevDelays = curSize = prevSize = 0;
            prevHour = ZDate.now();
            curHour = ZDate.now();
        }

        synchronized int getActivity() {
            resetOnNoActivity();
            int actions = curActions + prevActions;
            float elapsed = prevHour.elapsed() / (float) ZDate.MINUTE;
            if (elapsed == 0) {
                elapsed = 1;
            }
            return (int) Math.round(actions / elapsed);
        }

        synchronized ZDate getLastActivity() {
            return lastAct;
        }

        float getAvgDelay() {
            resetOnNoActivity();
            long delays = curDelays + prevDelays;
            float actions = curTsActions + prevTsActions;
            if (actions == 0) {
                actions = 1;
            }
            float avgDelay = delays / actions / 1000F;
            return Math.round(avgDelay * 100) / 100F;
        }

        long getMaxDelay() {
            resetOnNoActivity();
            return (long) Math.round(Math.max(curMmaxDelay, prevMaxDelay) / 1000F);
        }

        void updateActivity(ZDate ts, int rawSize) {
            resetOnNoActivity();
            long clockSync = getClockSync();
            synchronized(this){
                //determine the time span in which statistics are calculated
                lastAct = ZDate.now();
                if (curHour.elapsed() >= STAT_WINDOW)
                    shiftTimeWidnow();
                if (ts != null) {
                    ++curTsActions;
                    long delay = getHourFraction(ts.elapsed() - clockSync);
                    curDelays += delay;
                    if (delay > curMmaxDelay) {
                        curMmaxDelay = delay;
                    }
                }
                ++curActions;
                curSize += rawSize;
            }
        }
    
        private long getHourFraction(long ms) {
            long hr = Math.round((double) ms / ZDate.HOUR) * ZDate.HOUR;
            return ms - hr;
        }
        private void shiftTimeWidnow() {
            prevHour = curHour;
            curHour = ZDate.now();
            prevActions = curActions;
            prevTsActions = curTsActions;
            curActions = curTsActions = 0;
            prevDelays = curDelays;
            prevMaxDelay = curMmaxDelay;
            curDelays = curMmaxDelay = 0;
            prevSize = curSize;
            curSize = 0;
        }

        int getBandwidth() {
            resetOnNoActivity();
            long size = curSize + prevSize;
            float elapsed = prevHour.elapsed() / (float) ZDate.SECOND;
            if (elapsed == 0) {
                elapsed = 1;
            }
            return (int) Math.round(size / elapsed);
        }
    }
    
    private final Column columns[];

    /**
     * Statistics table constructor
     * @param length number of columns in the table
     */
    public Statistics(int length) {
        columns = new Column[length];
        for (int i = 0; i < length; i++)
            columns[i] = new Column();
    }
    
    /**
     * Reset the entire table
     */
    public void reset() {
        for (Column col : columns)
            col.reset();
    }

    /**
     * Get average actions per minute in the given column
     * @param column
     * @return
     */
    public int getActivity(int column) {
        return columns[column].getActivity();
    }

    /**
     * Get the time of the last activity in the given column
     * @param column
     * @return
     */
    public ZDate getLastActivity(int column) {
        return columns[column].getLastActivity();
    }

    /**
     * Get average delay in the given column
     * @param column
     * @return
     */
    public float getAvgDelay(int column) {
        return columns[column].getAvgDelay();
    }

    /**
     * Get the maximum delay in a 5-minute window in the given column
     * @param column
     * @return
     */
    public long getMaxDelay(int column) {
        return columns[column].getMaxDelay();
    }

    /**
     * Get average bytes per second traffic in the given column
     * @param column
     * @return
     */
    public int getBandwidth(int column) {
        return columns[column].getBandwidth();
    }

    /**
     * Update counters and accumulators in used to calculate performance. If
     * there is no timestamp associated with this action update only the
     * traffic info
     *
     * @param column    relevant statistics column number
     * @param ts        timestamp of the action
     * @param rawSize   size of the data included in the action
     */
    public void updateActivity(int column, ZDate ts, int rawSize) {
        columns[column].updateActivity(ts, rawSize);
    }

    /**
     * Override this if you want to shift clock for synchronization
     * @return
     */
    protected long getClockSync() {
        return 0;
    }
}
