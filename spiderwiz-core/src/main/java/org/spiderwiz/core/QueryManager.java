package org.spiderwiz.core;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import org.spiderwiz.zutils.ZDate;

/**
 * Manages the traffic of query objects (objects derived from QueryObject).
 * This class is derived from ZThread, i.e. it runs on a loop in its own thread. 
 * @author zvil
 */
final class QueryManager {
    /**
     * Time task that calls queries OnExpire() method when necessary.
     */
    class QueryTimer extends TimerTask {
        private final QueryObject query;

        public QueryTimer(QueryObject query) {
            this.query = query;
        }

        @Override
        public void run() {
            query.expire();
        }
    }
    
    private static QueryManager myObject = null;
    private final Timer timer;

    public QueryManager() {
        timer = new Timer(true);
    }

    static QueryManager getInstance() {
        return myObject;
    }
    
    void init() {
        myObject = this;
    }
    
    /**
     * Process a serialized query object command
     *
     * @param prefix        The character that prefixes the command ($ for regular command, ~ for object removal)
     * @param code          command code (without the $)
     * @param keys          the object keys as one string separated by bar characters
     * @param fields        the object field values as one string separated by commas
     * @param origUUID      the UUID of the origin service of the command
     * @param destinations  destination UUIDs of this command or null if destined to all.
     * @param channel       the channel on which the command has been received.
     * @param rawCommand    full raw command that delivered the object
     * @param commandTs     time stamp of the command that delivered the object
     * @return              the query object. If the command should be forwarded to other data nodes return null.
     * @throws java.lang.Exception
     */
    DataObject processQueryCommand(String prefix, String code, String keys, String fields, UUID origUUID,
        Collection<UUID> destinations, DataHandler channel, String rawCommand, ZDate commandTs
    ) throws Exception {
        String[] cmd = fields.split(String.valueOf(Serializer.FIELD_SEPARATOR), 3);
        int queryID = Integer.parseInt(cmd[0]);
        QueryObject.QueryState queryState =
            (QueryObject.QueryState)Serializer.deserializeEnum(cmd[1], QueryObject.QueryState.class);
        QueryObject query;

        if (queryState == QueryObject.QueryState.ABORTED) {
            query = Hub.getInstance().getQuery(origUUID, queryID);
            if (query != null)
                query.abortQuery();
            return query;
        }

        if (queryState != QueryObject.QueryState.QUERY)
            return processRepliedQuery(queryID, queryState, fields);
        
        // Process a query object in query (non-reply) state.
        query = (QueryObject)DataManager.getInstance().deserializeObject(
            prefix, code, keys, fields, origUUID, channel, rawCommand, commandTs);
        if (query == null)
            return null;
        query.setDestinations(destinations);
        if (!query.notForMe()) {
            query.activate();
            Hub.getInstance().pendQuery(query);
            DataManager.getInstance().getEventDispatcher(code).queryEnquire(query);
        }
        return query;
    }
    
    /**
     * Parse and handle a reply to a query that we have originated.
     * @param queryID       query ID
     * @param queryState    query state (in this case a reply type)
     * @param fields        the object field values as one string separated by commas
     * @return              the query that is replied
     * @throws Exception 
     */
    QueryObject processRepliedQuery(int queryID, QueryObject.QueryState queryState, String fields) throws Exception {
        QueryObject query = Hub.getInstance().getQuery(Main.getInstance().getAppUUID(), queryID);
        if (query == null || query.hasExpired())
            return query;
        query.waitBusy();
        if (query.isComplete()) {
            query.releaseBusy();
            return query;
        }
        try {
            query.parseObject(fields, false);
        } catch (Exception ex) {
            query.releaseBusy();
            throw ex;
        }
        query.processReply();
        return query;
    }
    
    /**
     * Try processing an imported object by each of the pending queries.
     * @param importObject     the object
     * @param channel       the ImportHandler object representing the sending server
     * @param commandTime   the time the object is retrieved from the import server
     * @return true if one of the queries processed the object successfully.
     */
    final boolean processImportQuery(Object importObject, ImportHandler channel, ZDate commandTime) throws Exception {
        QueryObject query = Hub.getInstance().processImportQuery(importObject, channel, commandTime);
        if (query != null) {
            query.replyNow();
            return true;
        }
        return false;
    }
    
    /**
     * Schedule a timer task that will call the given query's onExpire() method when the query expires.
     * @param query
     * @param expiration 
     */
    void scheduleQuery(QueryObject query, long expiration) {
        timer.schedule(new QueryTimer(query), expiration);
    }

    public void cleanup() {
        if (Hub.getInstance() == null)
            return;
        Hub.getInstance().abortAllQueries();
        timer.cancel();
    }
}
