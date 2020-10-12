package org.spiderwiz.core;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.spiderwiz.annotation.WizField;
import org.spiderwiz.annotation.WizQuery;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZTrigger;
import org.spiderwiz.zutils.ZUtilities;
import org.spiderwiz.zutils.Ztrings;

/**
 * A base class for <em><b>Query Objects</b></em>.
 * <p>
 * <em>Query objects</em> are a special sort of <em>data objects</em> used for round-trip query and response. The inquirer sets
 * query property fields and posts the query. The responder that receives the query needs
 * just to set response property fields and the framework carries the response back to the inquirer.
 * <p>
 * Note that the inquirer side is the 
 * {@link org.spiderwiz.core.Main#getProducedObjects() producer} of the query object, while the responder is the
 * {@link org.spiderwiz.core.Main#getConsumedObjects() consumer}.
 * <p>
 * The following is a general description of the class methods. For a detailed description, see below.
 * <p>
 * At the inquirer side, post a query by calling {@link #post(long) post(expires)} or
 * {@link #post(long, java.lang.String) post(expires, destinations)}. The first version broadcasts the query to all the consumers of
 * this query type, the latter posts it to the specified destination(s).
 * <p>
 * You can handle the response synchronously or asynchronously.
 * To handle synchronously, call {@link #waitForReply()} after posting the query. When the method ends, if the query has been replied
 * properly the object response properties will be set. Asynchronous response handling is done by overriding
 * {@link #onReply()}. You may override {@link #onExpire()} to do something in case the query expires before it is replied.
 * <p>
 * At the responder side, handle the query by overriding {@link #onInquire()}. Reply by setting the appropriate property fields and
 * return {@code true} when you quit the method. Alternatively you commit the response by calling {@link #replyNow()} before you quit. If you
 * need to reply by sending back a stream of data in multiple items, set object properties and call {@link #replyNext()} for
 * each item, then finalize the response by calling {@link #replyEnd()}.
 * <p>
 * A query can be closed or open. A closed query expects only one response after which the query is discarded. An open
 * query can receive multiple responses from one or more responders and it remains active until it expires. To indicate an open query
 * override {@link #isOpenQuery()}.
 * <p>
 * You can mark the query as <em>urgent</em> by overriding {@link #isUrgent()}. Urgent queries have priority over other items
 * circulated by the framework.
 */
@WizQuery
public abstract class QueryObject extends DataObject{
    enum QueryState {QUERY, REPLIED, NEXT, END, ABORTED}
    private final static int DEFAULT_STREAMING_PER_SECOND = 100;       // Streaming rate for replyNext
    
    @WizField private int queryID = -1;
    @WizField private QueryState queryState = QueryState.QUERY;        // Possible values: 0-query, 1-reply, 2-reply next, 3-reply end
    @WizField private long expires = 0;
    
    private final ZTrigger waitForReply;
    private final ZTrigger pauseStreaming;
    private final ZTrigger busy;
    private ZDate activated = null;
    private ZDate startStreaming = null;
    private int totalStreamedRecords = 0;

    /**
     * Object constructor.
     * <p>
     * Normally you would not instantiate objects of this class directly but use
     * {@link org.spiderwiz.core.Main#createQuery(java.lang.Class) Main.createQuery()}.
     */
    public QueryObject() {
        waitForReply = new ZTrigger();
        pauseStreaming = new ZTrigger();
        busy = new ZTrigger();
        busy.activate();    // At the beginning the 'busy' trigger is released.
    }

    /**
     * Posts the query.
     * <p>
     * Posts the query to all potential responders. (You can still filter destinations by using
     * {@link org.spiderwiz.core.DataObject#filterDestination(java.util.UUID,
     * java.lang.String, java.lang.String, java.lang.String, java.util.Map) DataObject.filterDestination()}). The {@code expires}
     * parameter tells how long to wait for a reply before the query expires and discarded. A non positive value means no waiting
     * (which usually makes sense only if the query is responded from the application itself).
     * @param expires   query expiring period in milliseconds.
     */
    public final void post(long expires) {
        post(expires, null);
    }
    
    /**
     * Posts the query to specific destination.
     * <p>
     * Posts the query to the destinations specified by the {@code destinations} parameter as a list of
     * {@link org.spiderwiz.core.Main#getAppUUID() application UUIDs} concatenated by the semicolon (';') character.
     * The {@code expires} parameter tells how long to wait for a reply before the query expires and discarded. A non positive value
     * means no waiting (which usually makes sense only if the query is responded from the application itself).
     * @param expires       query expiring period in milliseconds.
     * @param destinations  a list of UUIDs concatenated by the semicolon (';') character. The null value means a general broadcast.
     */
    public final void post(long expires, String destinations) {
        try {
            synchronized(this){
                if (isBusy())     // Don't reuse an active query
                    return;
                queryState = QueryState.QUERY;
                this.expires = expires;
                activate();
            }
            setOriginUUID(Main.getInstance().getAppUUID());
            Collection<UUID> destinationUUIDs = destinations == null ? null : Ztrings.split(destinations).toUUIDs();
            setDestinations(destinationUUIDs);
            if (!Hub.getInstance().pendMyQuery(this)) {
                synchronized(this){
                    activated = null;
                }
                onExpire();
                return;
            }
            propagate(false, destinationUUIDs, null);
            if (Hub.getInstance().isForMe(destinationUUIDs) >= 0 && !notForMe())
                DataManager.getInstance().getEventDispatcher(getObjectCode()).queryEnquire(this);
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, 
                String.format(CoreConsts.AlertMail.WHEN_POSTING_A_QUERY, getObjectCode()), null, false);
        }
    }
    
    /**
     * Waits until the query is replied or expires.
     * <p>
     * Blocks the caller thread until the query is replied fully or partially, or expires. Full reply is when this is
     * a closed query (that returns {@code false} in {@link #isOpenQuery()}), and either {@link #onReply()} or {@link #onReplyEnd()}
     * are activated. Partial reply is when {@link #onReplyNext()} was activated but {@link #onReplyEnd()} was not, or a reply was
     * received for an open query (that returns {@code true} in {@link #isOpenQuery()}).
     * <p>
     * Note that in case of a partial reply, each call to {@link #waitForReply()} activates a new expiration cycle with the length
     * equal to the value set when {@link #post(long) post()} was called.
     * <p>
     * The method returns {@code true} if the query was replied, fully or partially, since the previous call to this method, and
     * false when the query expired.
     * @return true if the query has been replied, fully or partially, and false when it has expired.
     */
    public final boolean waitForReply() {
        if (isComplete())
            return true;
        if (getExpires() == 0)
            return false;
        waitForReply.pause(getExpires());
        return isReplied();
    }
    
    /**
     * Handles a query on the responder side.
     * <p>
     * Override this method to reply to a query. You would need to examine the property fields pertaining to the query, set the property
     * fields pertaining to the response, and quit the method with the value {@code true}. Replying a query by this way would fire
     * {@link #onReply()} at the inquirer side.
     * <p>
     * Alternatively you can handle the query by responding with a multi-item stream. For each item, set the appropriate response
     * properties and call {@link #replyNext()}. When you have done, call {@link #replyEnd()}. When you use this mechanism exit with the
     * value {@code false} to indicate that you have committed the response and the framework shall not take any further action.
     * <p>
     * If you want to send a single-item response before you quit the method, call {@link #replyNow()} from anywhere in the method. In
     * this case also exit with the value {@code false}.
     *
     * @return true if the query has been fully replied and the framework shall send the reply back to the inquirer, false otherwise.
     */
    protected boolean onInquire() {
        return false;
    }

    /**
     * Commits a query response.
     * <p>
     * Call this method if you want to commit the query response before you exit {@link #onInquire()}. If you do, exit that method
     * with the value {@code false} to notify the framework that no further process is needed. Replying a query by this way would fire
     * {@link #onReply()} at the inquirer side.
     * @throws Exception
     */
    protected final void replyNow() throws Exception {
        replyStep(QueryState.REPLIED);
    }

    /**
     * Commits a single item of a multi-item response stream.
     * <p>
     * Use this method to reply a query by streaming, i.e. sending a bulk of data piece by piece. For instance, when the query is
     * executed by querying a database table and the response is a record set that you want to deliver record by record. For each item
     * set appropriate property fields and call {@code replyNext()}. Replying a query by this way would fire {@link #onReplyNext()} at
     * the inquirer side. Having sent the entire response call {@link #replyEnd()}. You would usually do this by forking a new thread
     * from {@link #onInquire()} to handle the streaming in the background.
     * <p>
     * When you use this method exit {@link #onInquire()} with the value {@code false} to notify the framework that you take care of
     * the entire response mechanism by yourself.
     * <p>
     * Note that the use of this method includes rate moderation. Every call to {@code replyNext()} pauses execution for a period
     * determined by {@link #getStreamingRate()}. To stream continuously without moderation implement that method to return
     * zero.
     * <p>
     * The method returns {@code true} if the response item is committed successfully. If the query is not active anymore
     * ({@link #replyEnd()} or {@link #replyNow()} were called, or it has expired), the method returns {@code false}.
     * @return true if the item is committed successfully, false if the query had completed or expired.
     * @throws java.lang.Exception
     */
    protected final boolean replyNext() throws Exception  {
        if (startStreaming == null) {
            startStreaming = ZDate.now();
            totalStreamedRecords = 0;
        }
        if (replyStep(QueryState.NEXT)) {
            int sr = getStreamingRate();
            if (sr > 0) {
                long timeDue = (long)(++totalStreamedRecords / (sr / (double) ZDate.SECOND));
                long msWait = timeDue - startStreaming.elapsed();
                if (msWait > 0)
                    pauseStreaming.pause(msWait);
            }
            return true;
        }
        return false;
    }

    /**
     * Notifies the end of a multi-item response stream.
     * <p>
     * Call this method when you have completed a series of {@link #replyNext()} calls. {@link #onReplyEnd()} would be fired
     * at the inquirer side.
     * @throws Exception
     */
    protected final void replyEnd() throws Exception  {
        replyStep(QueryState.END);
    }
    
    /**
     * Handles a query response.
     * <p>
     * Override this method to handle a single-item query response. To handle a multiple item response
     * stream, override {@link #onReplyNext()} and {@link #onReplyEnd()}.
     */
    protected void onReply() {}

    /**
     * Handles one response item of a multi-item response stream.
     * <p>
     * Override this method to handle one response item of a multi-item response stream, committed at the responder side by
     * calling {@link #replyNext()}.
     */
    protected void onReplyNext() {}

    /**
     * Handles the notification of the end of a multi-item response stream.
     * <p>
     * Override this method to handle the notification of the end of a multi-item response stream, notified at the responder side
     * by calling {@link #replyEnd()}.
     */
    protected void onReplyEnd() {}
    
    /**
     * Handles query expiration.
     * <p>
     * Override this method to do something when the query expires or is aborted before completion.
     */
    protected void onExpire() {}
    
    /**
     * Returns {@code true} if this is an open query.
     * <p>
     * A query can be closed or open. A closed query expects only one response after which the query is discarded. An open
     * query can receive multiple responses from one or more responders and it remains active until it expires. Override this method
     * to indicate that this is an open query. The default implementation returns {@code false}.
     *
     * @return true if and only if this is an open query.
     */
    protected boolean isOpenQuery() {return false;}

    /**
     * Returns {@code true} if your application does not handle this query.
     * <p>
     * Override this method to tell the framework that your application will not handle this query. This is more
     * resource-saving than implementing {@link #onReply()} that returns {@code false}. The default implementation returns
     * {@code false}.
     * @return true if and only if your application does not handle this query.
     */
    protected boolean notForMe() {return false;}
    
    /**
     * Returns {@code true} if the query has been completely replied.
     * <p>
     * A query is considered to be completely replied if it is a closed query (see {@link #isOpenQuery()}) and either a single-item
     * response or the end of a multi-item response stream were received for it, or if the query was {@link #abort() aborted}.
     * @return true if and only if the query has been completely replied.
     */
    protected final synchronized boolean isComplete() {
        switch(queryState) {
        case REPLIED:
        case END:
            return !isOpenQuery();
        case ABORTED:
            return true;
        }
        return false;
    }
    
    /**
     * Aborts the query.
     * <p>
     * Call this method, usually from the inquirer side, to abort the query. This notifies potential responders that there is no
     * need any more to handle the query.
     * @return true if the query was active before this method is called.
     */
    public final boolean abort() {
        try {
            return abortQuery();
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, 
                String.format(CoreConsts.AlertMail.WHEN_ABORTING_A_QUERY, getObjectCode()), null, false);
            return false;
        }
    }

    /**
     * Returns the streaming rate of a multi-item stream response.
     * <p>
     * A streamed query response, i.e. when a query is replied by a series of {@link #replyNext()} calls, is moderated by default.
     * In order to avoid network congestion, {@link #replyNext()} would pause for a while after each item. The default
     * streaming rate is 100 items per second. Override this method to define a different rate. Return zero to indicate a
     * continuous streaming with no pausing.
     * @return  number of stream items per second, or zero for continuous streaming with no pausing. The default is 100
     *          items per second.
     */
    protected int getStreamingRate() {
        return DEFAULT_STREAMING_PER_SECOND;
    }

    /**
     * Returns the object code of the parent of this query.
     * <p>
     * By default, the method returns {@code null} for <em>query objects</em>.
     * @return the object code of the parent of this query, which is null by default.
     */
    @Override
    protected String getParentCode() {
        return null;
    }

    /**
     *  Returns {@code true} if the query is disposable (the default).
     * @return true if the query is disposable (the default).
     */
    @Override
    protected boolean isDisposable() {
        return true;
    }

    /**
     * Cleans up query resources.
     * <p>
     * This method overrides {@link org.spiderwiz.core.DataObject#cleanup()} to perform query object specific cleanup. If you override
     * this method do not forget to call {@code super.cleanup()}.
     */
    @Override
    public void cleanup() {
        setQueryState(QueryState.ABORTED);
        waitForReply.release();
        pauseStreaming.release();
    }

    final synchronized int getQueryID() {
        return queryID;
    }

    /**
     * Set a query id and return the previous id value
     * @param queryID
     * @return 
     */
    final synchronized int setQueryID(int queryID) {
        int id = this.queryID;
        this.queryID = queryID;
        return id;
    }

    /**
     * Check if the query has been replied.
     * @return true if the object contains the reply to the query.
     */
    private synchronized boolean isReplied() {
        return queryState != QueryState.QUERY;
    }
    
    /**
     * Check if the query is busy, i.e. in the state of query that hasn't been fully replied.
     * @return true if the query is not active or has expired.
     */
    private synchronized boolean isBusy() {
        return activated != null && activated.add(expires).elapsed() < 0;
    }

    final synchronized QueryState getQueryState() {
        return queryState;
    }

    synchronized void setQueryState(QueryState queryState) {
        this.queryState = queryState;
    }

    final synchronized long getExpires() {
        return expires;
    }
    
    final void waitBusy() {
        busy.pause(0);
    }
    
    final void releaseBusy() {
        busy.activate();
    }
    
    final void releaseWaitForReply() {
        waitForReply.release();
    }
        
    /**
     * Set activation time
     */
    final void activate() {
        synchronized(this) {
            if (isComplete()) {
                activated = null;
                return;
            }
            if (queryState != QueryState.QUERY && queryState != QueryState.NEXT)
                return;
            activated = ZDate.now();
        }
        if (Hub.getInstance().isMe(getOriginUUID()) > 0)
            QueryManager.getInstance().scheduleQuery(this, expires);
    }
    
    /**
     * Abort a query that we or others have posted. If it's our query the abort request will be propagated to other nodes.
     * @return true if was active before
     */
    final boolean abortQuery() throws Exception {
        if (isComplete())
            return false;
        synchronized(this){
            queryState = QueryState.ABORTED;
            activated = null;
        }
        releaseWaitForReply();
        if (Hub.getInstance().isMe(getOriginUUID()) > 0)
            propagate(false, getDestinations(), null);
        return true;
    }
    
    /**
     * Call onExpire() if necessary.
     */
    final void expire() {
        if (isComplete() || !hasExpired())
            return;
        onExpire();
    }

    /**
     * Check if query has expired
     * @return 
     */
    synchronized final boolean hasExpired() {
        return activated == null ? false : (activated.add(expires)).elapsed() >= 0;
    }

    /**
     * Handle the query reply. If we are the inquirer process the reply synchronously, otherwise propagate the object
     * back to the inquirer.
     * @param busy      true if this method is called when the query is in busy state, i.e. when called from inquire()
     * @throws Exception 
     */
    final void reply(boolean busy) throws Exception {
        if (hasExpired())
            return;
        if (!busy)
            waitBusy();
        activate();
        try {
            if (Hub.getInstance().isMe(getOriginUUID()) > 0)
                DataManager.getInstance().getEventDispatcher(getObjectCode()).queryReply(this, true);
            else
                Hub.getInstance().propagateObject(this, false, Collections.singleton(getOriginUUID()), null);
        } finally {
            if (!busy)
                releaseBusy();
        }
    }
    
    /**
     * Handle a replied query by firing the appropriate reply event on the query object
     */
    final void processReply() throws Exception {
        activate();
        if (!DataManager.getInstance().getEventDispatcher(getObjectCode()).queryReply(this, false)) {
            releaseWaitForReply();
            releaseBusy();
        }
    }
    
    /**
     * Fire a query event. If successful, change object state to 'replied'.
     * @return the value returned by the event.
     */
    final boolean inquire() {
        if (hasExpired())
            return false;
        waitBusy();
        try {
            if (isComplete())           // Make sure nobody is ahead of us.
                return false;
            totalStreamedRecords = 0;
            startStreaming = null;
            boolean success = onInquire();
            if (success)
                setQueryState(QueryState.REPLIED);
            return success;
        } finally {
            releaseBusy();
        }
    }
    
    private boolean replyStep(QueryState queryState) throws Exception  {
        if (isComplete())
            return false;
        setQueryState(queryState);
        reply(true);
        return true;
    }
    
    /**
     * Resend a query using a resetter
     * @param resetter A Resetter object managing the reset of a specific object type.
     */
    final boolean resend(Resetter resetter) {
        if (!hasExpired() && !isComplete())
            return resetter.resetObject(this);
        return false;
    }

    /**
     * if leading character is '?' or '!' this is a query command.
     * @param prefix
     * @return 
     */
    static boolean isQuery(String prefix) {
        return prefix.matches("\\?|\\!");
    }
    
    /**
     * Check whether the arguments represent query reply
     * @param prefix        command prefix
     * @param objectValues  field values
     * @return true if a query reply
     */
    static boolean isReply(String prefix, String objectValues) {
        if (!isQuery(prefix))
            return false;
        String vals[] = objectValues.split(",", 3);
        switch (QueryState.values()[ZUtilities.parseInt(vals[1])]) {
        case REPLIED:
        case NEXT:
        case END:
            return true;
        }
        return false;
    }
    
    /**
     * @return true if this object is in reply state
     */
    boolean isReply() {
        switch (queryState) {
        case REPLIED:
        case NEXT:
        case END:
            return true;
        }
        return false;
    }
    
    @Override
    void propagate(boolean resetting, Collection<UUID> destinations, DataHandler originChannel) throws Exception {
        if (Hub.getInstance().isForMe(destinations) <= 0 && !onlyForMe())
            super.propagate(false, destinations, originChannel);
    }

    @Override
    String myTransmitPrefix() {
        return isUrgent() ? "!" : "?";
    }
}
