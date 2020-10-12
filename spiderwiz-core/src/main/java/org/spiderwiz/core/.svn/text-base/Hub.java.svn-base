package org.spiderwiz.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.spiderwiz.admin.data.TableData;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZDictionary;
import org.spiderwiz.zutils.ZHashMap;
import org.spiderwiz.zutils.ZHashSet;
import org.spiderwiz.zutils.ZModInteger;
import org.spiderwiz.zutils.ZUtilities;
import org.spiderwiz.zutils.Ztrings;

/**
 * This class is a singleton that handles all core communication and hub functionality of Spiderwiz.
 * @author Zvi 
 */
final class Hub {
    /**
     * A class that represents another data node on the mesh that consumes the objects that we produce or produces the objects
     * that we consume.
     */
    final class RemoteNode {
        /**
         * Override LosslessPipe to implement resending of lines that did not safeGet an ack
         */
        private class AckPipe extends LosslessPipe {
            private final String objCode;
            
            public AckPipe(String objCode) {
                super(Main.getMyConfig().getBackupPath(objCode + "@" + uuid));
                this.objCode = objCode;
            }

            @Override
            protected int getResendRate() {
                return Main.getMyConfig().getRefreshTranmsitRate();
            }

            @Override
            protected void resendSkippedLine(String line) {
                try {
                    String s[] = line.split(",", 3);
                    String cmd = s[0];
                    producedObjectMap.create(objCode).resendLosslessObject(cmd.substring(0,1), objCode, s[1], s[2], uuid, this);
                } catch (IOException ex) {
                    Main.getInstance().sendExceptionMail(ex, line, null, false);
                }
            }

            @Override
            protected void startResend(int firstSkipped, int nextReceived) {
                Main.getLogger().logNow(CoreConsts.RESENDING_LOSSESS, objCode, uuid, firstSkipped, nextReceived);
            }

            @Override
            public void handleException(Exception ex) {
                Main.getInstance().sendExceptionMail(ex, CoreConsts.EXCEPTION_IN_ZPIPE, null, false);
            }
        }
        
        private final UUID uuid;                    // UUID of the node
        private final ObjectMap producedObjects;    // map of objects produced by this node
        private final QueryMap pendingQueries;      // map of queries from this node that we are in the process of replying
        private ZDate deployTime = null;            // used for controlling input sequence
        private final ConsumedObjectMap consumedObjects; // a map of object codes consumed by this node
        private String appName;                     // application name of the node
        private String appVersion;                  // version of the node
        private String coreVersion;                 // Spiderwiz version of the node
        private String remoteAddress;               // remote address of the node
        private String userID;                      // user ID attached to the channel the node appeared from
        private ZDictionary applicationParams;      // application parameter map of the node
        private boolean connected = false;          // true when application is connected
        private ZDate since = null;                 // time of last connection/disconnection
        private final AppInfo appInfo;              // AppInfo object attached to the application

        public RemoteNode(UUID uuid) {
            this.uuid = uuid;
            producedObjects = new ObjectMap();
            pendingQueries = new QueryMap();
            appInfo = new AppInfo(this);
            consumedObjects = new ConsumedObjectMap(){
                /**
                 * We if we are producers of the given 'objCode' we return a new AckPipe, otherwise we return a counter
                 * @param objCode
                 * @return 
                 */
                @Override
                Object getLosslessController(String objCode) {
                    if (DataManager.getInstance().isProducingObject(objCode)) {
                        AckPipe pipe = new AckPipe(objCode);
                        pipe.init();
                        return pipe;
                    }
                    return new ZModInteger(LosslessPipe.DEFAULT_MODULO);
                }
            };
        }
        
        public UUID getUuid() {
            return uuid;
        }

        private synchronized ZDictionary getApplicationParams() {
            return applicationParams;
        }

        synchronized String getAppName() {
            return appName;
        }

        public String getAppVersion() {
            return appVersion;
        }

        public String getCoreVersion() {
            return coreVersion;
        }

        synchronized String getRemoteAddress() {
            return remoteAddress;
        }

        synchronized boolean isConnected() {
            return connected;
        }

        public ZDate getSince() {
            return since;
        }

        private synchronized AppInfo getAppInfo() {
            return appInfo;
        }

        private synchronized void setApplicationInfo(String appName, String appVersion, String coreVersion, String remoteAddress,
            String userID, ZDictionary applicationParams)
        {
            this.appName = appName;
            this.appVersion = appVersion;
            this.coreVersion = coreVersion;
            this.remoteAddress = remoteAddress;
            this.userID = userID;
            this.applicationParams = applicationParams;
            if (!connected) {
                connected = true;
                since = ZDate.now();
            }
            updateAppHistory();
        }

        synchronized String getUserID() {
            return userID;
        }

        /**
         * Add the codes of the objects consumed by this node to the specified collection
         * @param addTo 
         */
        void addConsumedObjects(Collection<String> addTo) {
            consumedObjects.lockRead();
            try {
                addTo.addAll(consumedObjects.keySet());
            } finally {
                consumedObjects.unlockRead();
            }
        }
        
        /**
         * Remove the codes of the objects consumed by this node from the specified collection
         * @param removeFrom 
         */
        void removeConsumedObjects(Collection<String> removeFrom) {
            consumedObjects.lockRead();
            try {
                removeFrom.removeAll(consumedObjects.keySet());
            } finally {
                consumedObjects.unlockRead();
            }
        }
        
        private ObjectSequencer createSequencer(String commandCode) {
            return producedObjects.create(commandCode);
        }
        
        private void updateInputActivity(int uncompressed, int compressed) {
            appInfo.updateActivity(ZDate.now(), uncompressed);
            appInfo.updateCompressedInput(compressed);
        }
        
        private synchronized void updateAppHistory() {
            String objectCodes = consumedObjects.getAsString(DataManager.getInstance().getProducedObjects());
            if (!objectCodes.isEmpty())
                Main.getInstance().getHistory().storeAppHistory(
                    uuid, appName, appVersion, coreVersion, remoteAddress, userID, applicationParams, objectCodes,
                    connected ? null : since);
        }
        
        private synchronized void loadAppHistory(String appName, String appVersion, String coreVersion, String remoteAddress,
            String userID, ZDictionary applicationParams, String objectCodes, ZDate lastSeen)
        {
            this.appName = appName;
            this.appVersion = appVersion;
            this.coreVersion = coreVersion;
            this.remoteAddress = remoteAddress;
            this.userID = userID;
            this.applicationParams = applicationParams;
            consumedObjects.fromString(objectCodes, null);
            since = lastSeen;
        }
        
        /**
         * Compare the specified time to the known deploy time of the node. If the given time is newer it means that
         * the node was recently redeployed and therefore we shall reset all its input counters.
         * @param newTime 
         */
        private synchronized void checkDeployTime(ZDate newTime) {
            if (deployTime == null)
                deployTime = newTime;
            else if (deployTime.before(newTime)) {
                deployTime = newTime;
                producedObjects.clear();
                pendingQueries.clear();
            }
            
        }
        
        private void pendQuery(QueryObject query) {
            pendingQueries.put(query.getQueryID(), query);
        }
        
        private QueryObject getQuery(int queryID) {
            return pendingQueries.get(queryID);
        }

        /**
         * Process a reset request for QueryObject objects. The function resends all our pending queries of the specific type
         * over the requesting channel.
         * @param resetter A Resetter object managing the reset of a specific object type.
         * @return true if stored queries contains at least one query that has been reset
         */
        boolean processQueryReset(Resetter resetter) {
            boolean didReset = false;
            pendingQueries.lockRead();
            try {
                for (QueryObject query : pendingQueries.values()) {
                    didReset |= query.resend(resetter);
                }
            } finally {
                pendingQueries.unlockRead();
            }
            return didReset;
        }
        
        /**
        * Try processing an imported object by each of the pending queries.
        * @param importObject  the object
        * @param channel       the ImportHandler object representing the sending server
        * @param commandTime   the time the object is retrieved from the import server
        * @return a query that processed the object successfully, or null.
        */
        QueryObject processImportQuery(Object importObject, ImportHandler channel, ZDate commandTime) throws Exception {
            pendingQueries.lockRead();
            try {
                for (QueryObject query : pendingQueries.values()) {
                    if (query.importObject(importObject, channel, commandTime) != null)
                       return query;
                }
            } finally {
                pendingQueries.unlockRead();
            }
            return null;
        }
        
        /**
         * Remove all completed or expired queries, then check if the node has expired
         * @return true if the node has expired.
         */
        boolean removeObsolete() {
            // Remove obsolete queries
            ArrayList<QueryObject> obsolete = new ArrayList<>();
            pendingQueries.lockRead();
            try {
                for (QueryObject query : pendingQueries.values()) {
                    if (query.isComplete() || query.hasExpired())
                        obsolete.add(query);
                }
            } finally {
                pendingQueries.unlockRead();
            }
            pendingQueries.removeAll(obsolete);
            
            // Check if the node has expired
            boolean expired = !connected && since != null && since.elapsed() >= Main.getMyConfig().getObsolescenceTime();
            if (expired) {
                Main.getInstance().getHistory().removeAppHistory(uuid);
                consumedObjects.lockRead();
                try {
                    for (Object obj : consumedObjects.values()) {
                        if (obj instanceof AckPipe) {
                            ((AckPipe)obj).remove();
                        }
                    }
                } catch (IOException ex) {
                } finally {
                    consumedObjects.unlockRead();
                }
            }
            return expired;
        }

        /**
         * Abort all the queries posted by this node.
         */
        void abortAllQueries() {
            pendingQueries.lockRead();
            try {
                for (QueryObject query : pendingQueries.values()) {
                    query.abortQuery();
                }
            } catch (Exception ex) {
                Main.getInstance().sendExceptionMail(ex, CoreConsts.AlertMail.WHEN_ABORT_ALL_QUERIES, null, false);
            } finally {
                pendingQueries.unlockRead();
            }
        }
        
        /**
         * Send a request for object reset for this node over the specified channel
         */
        void requestChannelReset(DataHandler channel) {
            if (!connected)
                return;
            // Don't send if there is user ID conflict
            if (channel.getAppUser() == null && getUserID() != null ||
                channel.getAppUser() != null && !channel.getAppUser().equalsIgnoreCase(getUserID())
            )
                return;
            // If we are not in a hub mode, we request only the objects that we also consume
            String required =
                consumedObjects.getAsString(Main.getMyConfig().isHubMode() ? null : DataManager.getInstance().getConsumedObjects());
            if (!required.isEmpty())
                requestReset(required, null, uuid, getApplicationParams(), getAppName(), getAppVersion(),
                    getCoreVersion(), getRemoteAddress(), getUserID(), channel);
        }
        
        /**
         * Handle a reset request for the node by setting the specified consumed objects and node's application parametermap.
         * @param consumedObjectsString     a concatenated string of requested object (plus lossless indication)
         * @param appName                   application name of the origin node
         * @param appVersion                version of the origin node
         * @param coreVersion               Spiderwiz version of the origin node
         * @param remoteAddress             remote address of the origin node
         * @param userID                    user ID attached to the channel the node appeared from
         * @param applicationParams
         */
        void handleResetRequest(String consumedObjectsString, String appName, String appVersion, String coreVersion,
            String remoteAddress, String userID, ZDictionary applicationParams
        ) {
            consumedObjects.fromString(consumedObjectsString, null);
            setApplicationInfo(appName, appVersion, coreVersion, remoteAddress, userID, applicationParams);
        }
        
        /**
         * Mark the node as disconnected and log the disconnection
         * @param channel   the channel through which the node was connected
         */
        void drop(DataHandler channel) {
            if (connected) {
                Main.getLogger().logNow(CoreConsts.DataChannel.NODE_DISCONNECTED, getAppName(), getRemoteAddress());
                connected = false;
                since = ZDate.now();
                Main.getInstance().getRootObject().removeTerminatedChildren(uuid);
                getImportManager().dropNode(uuid);
                updateAppHistory();
            }
        }
        
        /**
         * Called before propagating an object to determine the nodes that the specified object shall be sent to
         * @param obj               the object
         * @param dontFilter        if true, check only if this node consumes objects of this type with no other filtering
         * @param resetting         true if object is resetting
         * @param originChannel     the channel the object is arriving from (do not propagate to that channel)
         * @return                  1 if the object shall be sent to this node, 0 if it shall be filtered out, -1 if the node is not
         *                          a consumer of this object
         * @throws Exception 
         */
        synchronized int filterNode(DataObject obj, boolean dontFilter, boolean resetting, DataHandler originChannel) throws Exception
        {
            // First, check if the node is consuming this object in lossless mode. If yes, deliver it using the lossless mechanism
            AckPipe pipe = (AckPipe)consumedObjects.get(obj.getObjectCode());
            if (pipe != null) {
                // Don't reset lossless objects
                if (!resetting) {
                    if (obj.filterDestination(uuid, appName, userID, remoteAddress, applicationParams)) {
                        propagateFilteredObject(obj, false, Collections.singleton(uuid), originChannel, pipe);
                        appInfo.updateOutputActivity(0);
                    }
                }
                return 0;
            }
            // Filter out the node if it is either not connected or it is not consuming the given data object and the data object
            // is not a query reply.
            if (!connected || !consumedObjects.containsKey(obj.getObjectCode()) && !obj.isQueryReply())
                return -1;
            if (obj.filterDestination(uuid, appName, userID, remoteAddress, applicationParams)) {
                appInfo.updateOutputActivity(0);
                return 1;
            }
            return 0;
        }
        
        /**
         * Process an ^ACK command
         * @param objCode           code of the acknowledged object
         * @param destinationUUID   UUID of acknowledged node
         * @param serial            serial number of acknowledged object
         * @param channel           the channel through which the acknowledgment arrived
         */
        void processAck(String objCode, UUID destinationUUID, int serial, DataHandler channel) {
            // If destination is us process the acknowledgment, if not check 'serial' to make sure it's not a repetition and move the
            // ack over to the destination node
            Object val = consumedObjects.get(objCode);
            if (isMe(destinationUUID) > 0)
                ((AckPipe)val).acknowledge(serial);
            else {
                if (val instanceof ZModInteger) {
                    ZModInteger nextSeq = (ZModInteger)val;
                    synchronized(nextSeq) {
                        if (nextSeq.compareTo(serial) > 0)
                            return;
                        nextSeq.setValue(serial + 1);
                    }
                }
                echoAck(objCode, uuid, destinationUUID, serial, channel);
            }
        }

        void cleanup() {
            pendingQueries.lockRead();
            try {
                pendingQueries.values().forEach((query) -> {
                    query.cleanup();
                });
            } finally {
                pendingQueries.unlockRead();
            }
            consumedObjects.lockRead();
            try {
                for (Object pipe : consumedObjects.values()) {
                    if (pipe instanceof AckPipe)
                        ((AckPipe)pipe).cleanup();
                }
            } finally {
                consumedObjects.unlockRead();
            }
        }
    }
    
    private class NodeMap extends ZHashMap<UUID, RemoteNode> {
        /**
         * Find the specified element in the map. If it doesn't exist create it and insert it to the map
         * @param uuid
         * @return 
         */
        RemoteNode create(UUID uuid) {
            RemoteNode node = get(uuid);
            if (node == null) {
                // Use putIfAbsent because the sequence get-put is not sychronized in this method
                node = putIfAbsentReturnNew(uuid, new RemoteNode(uuid));
                // Store object of our own node.
                if (uuid.equals(Main.getInstance().getAppUUID()))
                    setMyNode(node);
            }
            return node;
        }
        
        /**
         * Request object reset for each of the remote nodes over the specified channel
         */
        void requestChannelReset(DataHandler channel) {
            lockRead();
            try {
                values().forEach((node) -> {
                    node.requestChannelReset(channel);
                });
            } finally {
                unlockRead();
            }
        }
        
        /**
         * Get AppInfo objects associated with all nodes, ordered by application Name + address + UUID
         * @param userID        if not null the query is only for applications connecting with this user ID.
         * @return a collection of requested objects
         */
        Collection<AppInfo> getInfos(String userID) {
            AppsInfos infos = new AppsInfos();
            try {
                lockRead();
                for (RemoteNode node : values()) {
                    if (node.getAppName() != null && (userID == null || userID.equalsIgnoreCase(node.getUserID()))) {
                        AppInfo info = node.getAppInfo();
                        if (Main.getMyConfig().isPropertySet(MyConfig.IS_ADMIN) || info.isRelevant())
                            infos.put(node.getAppName() + node.getRemoteAddress() + node.getUuid(), info);
                    }
                }
            } finally {
                unlockRead();
            }
            return infos.values();
        }
    
        /**
        * Called before propagating an object to determine the nodes that the specified object shall be sent to
        * @param object        object
        * @param destinations  list of destinations before filtering, or null if should be broadcast to all
        * @param resetting     true if object is resetting
        * @param originChannel the channel the object is arriving from (do not propagate to that channel)
        * @return a list of destinations after filtering, or null if should be broadcast to all
        */
        Collection<UUID> filterNodes(DataObject object, Collection<UUID> destinations, boolean resetting, DataHandler originChannel)
            throws Exception
        {
            lockRead();
            try {
                Collection<UUID> candidates = destinations != null ? destinations : keySet();
                Collection<UUID> result = new HashSet<>();
                boolean filtered = false;
                for (UUID uuid : candidates) {
                    RemoteNode node = get(uuid);
                    if (node == null)
                        continue;
                    int filter = node.filterNode(object, filtered, resetting, originChannel);
                    if (filter == 0)
                        filtered = true;
                    else if (filter > 0)
                        result.add(uuid);
                }
                // If 'destinations' is null (i.e. "send to all") and is not changed by the filtering, return null, otherwise return
                // the filtered list
                return destinations != null || filtered || result.isEmpty() ? result : null;
            } finally {
                unlockRead();
            }
        }
        
        /**
         * @param destinations
         * @return true if 'destinations' is null or any of the destinations is active
         */
        private boolean verifyActiveDestinations(Collection<UUID> destinations) {
            if (isForMe(destinations) >= 0)
                return true;
            for (UUID destination : destinations) {
                RemoteNode node = get(destination);
                if (node != null && node.isConnected())
                    return true;
            }
            return false;
        }
        
        void cleanup() {
            lockRead();
            try {
                values().forEach((node) -> {
                    node.cleanup();
                });
            } finally {
                unlockRead();
            }
        }
    }
    
    /**
     * A class that manages the sequential order of objects we produce or consume
     */
    private final class ObjectSequencer {
        private final ZModInteger nextSeq;

        public ObjectSequencer() {
            nextSeq = new ZModInteger(CoreConsts.SEQUENCE_MODULU);
        }
        
        /**
         * Check the order of an object of a specific type received from a specific data node.
         * @param seq   sequential number of the object
         * @param ts    timestamp of the object, usually null unless 'seq' is zero.
         * @return true if this is a new object that should be handled
         */
        boolean checkInputObjectOrder (int seq) {
            synchronized(nextSeq) {
                if (nextSeq.compareTo(seq) > 0)
                    return false;
                nextSeq.setValue(++seq);
                return true;
            }
        }

        /**
         * Get an object sequence number for the object and transmit it using the given SequenceManager. Used during reset
         * @param obj           The object to code and transmit
         * @param sm            The SequenceManager that manages sequencing over one data channel
         * @param destinations  UUIDs of the destination data node, or null if shall be broadcast to all
         * @param ackSequence   Sequence number for acknowledging lossless objects
         */
        void transmitModeratedObject(DataObject obj, SequenceManager sm, Collection<UUID> destinations) throws Exception {
            synchronized(nextSeq) {
                sm.transmitCommand(
                    obj.getTransmitPrefix(),
                    obj.getObjectCode(),
                    ZDate.now(),
                    obj.getFullKey(),
                    obj.getObjectValues(true),
                    Main.getInstance().getAppUUID(),
                    destinations,
                    nextSeq.postInc(),
                    null,
                    true,
                    false
                );
            }
        }
        
        /**
        * propagate an object to other nodes in the system.
        * @param obj                the object to propagate
        * @param resetting          true if object is resetting
        * @param destinations       UUIDs of the destination data node, or null if shall be broadcast to all
        * @param originChannel      the channel the object is arriving from (do not propagate to that channel)
        * @param pipe               if not null, the object is sent in lossless mode and is stored in the pipe for acknowledgment
        */
        void propagateObject(DataObject obj, boolean resetting, Collection<UUID> destinations, DataHandler originChannel,
            LosslessPipe pipe
        )
            throws Exception
        {
            synchronized(nextSeq) {
                int seq = nextSeq.postInc();
                String prefix = obj.getTransmitPrefix();
                String objCode = obj.getObjectCode();
                ZDate ts = ZDate.now();
                String objectKeys = obj.getFullKey();
                String objectValues = obj.getObjectValues(resetting);
                UUID myUUID = Main.getInstance().getAppUUID();
                Integer ackSeq = null;
                if (pipe != null)
                    ackSeq = pipe.put(ZUtilities.concatAll(",", prefix + objCode, objectKeys, objectValues));
                producerNode.propagateCommand(
                    prefix, objCode, ts, objectKeys, objectValues, myUUID, destinations, seq, ackSeq, originChannel);
                consumerNode.propagateCommand(
                    prefix, objCode, ts, objectKeys, objectValues, myUUID, destinations, seq, ackSeq, originChannel);
            }
        }
        
        /**
         * Resend a serialized lossless object
         * @param prefix        command prefix
         * @param objCode       object code
         * @param objKeys       concatenated object keys
         * @param objValues     serialized object values
         * @param destination   UUID of the destination node
         * @param pipe          a LosslessPipe object to store the resent command until acknowledgment
         * @throws IOException 
         */
        void resendLosslessObject(String prefix, String objCode, String objKeys, String objValues, UUID destination, LosslessPipe pipe
        ) throws IOException {
            synchronized(nextSeq) {
                int seq = nextSeq.postInc();
                ZDate ts = ZDate.now();
                UUID myUUID = Main.getInstance().getAppUUID();
                Collection<UUID> destinations = Collections.singleton(destination);
                Integer ackSeq = pipe.put(ZUtilities.concatAll(",", prefix + objCode, objKeys, objValues));
                producerNode.propagateCommand(
                    prefix, objCode, ts, objKeys, objValues, myUUID, destinations, seq, ackSeq, null);
                consumerNode.propagateCommand(
                    prefix, objCode, ts, objKeys, objValues, myUUID, destinations, seq, ackSeq, null);
            }
        }
    }
    
    /**
     * A map of all object sequencers
     */
    private final class ObjectMap extends ZHashMap<String, ObjectSequencer> {
        /**
         * Find an element with the specified command code in the map. If it doesn't exist create it and insert it to the map
         * @param commandCode
         * @return 
         */
        ObjectSequencer create(String commandCode) {
            ObjectSequencer sequencer = get(commandCode);
            if (sequencer == null) {
                // Use putIfAbsent because the sequence get-put is not sychronized in this method
                sequencer = putIfAbsentReturnNew(commandCode, new ObjectSequencer());
            }
            return sequencer;
        }
    }
    
    private final class QueryMap extends ZHashMap<Integer, QueryObject> {}
    private class AppsInfos extends TreeMap<String, AppInfo> {}
    
    private static Hub myObject;
    private final DataNode producerNode;
    private final DataNode consumerNode;
    private ImportManager importManager;
    private final ZModInteger resetCounter;
    private final ZModInteger queryCounter;
    private final NodeMap nodeMap;
    private final ObjectMap producedObjectMap;
    private RemoteNode myNode = null;
    private ZDate lastObsoleteRemoval = ZDate.now();
    private final static int OBSOLETE_REMOVAL_FREQUENCY = ZDate.MINUTE * 3;

    public Hub() {
        producerNode = new DataNode(true);
        consumerNode = new DataNode(false);
        resetCounter = new ZModInteger(CoreConsts.SEQUENCE_MODULU);
        queryCounter = new ZModInteger(CoreConsts.SEQUENCE_MODULU);
        nodeMap = new NodeMap();
        producedObjectMap = new ObjectMap();
    }

    static Hub getInstance() {
        return myObject;
    }
    
    /**
     * Initialize the object 
     */
    void init() {
        myObject = this;
        producerNode.init();
        consumerNode.init();
        importManager = new ImportManager();
}
    
    /**
     * Activate the object
     */
    void execute() {
        // Connect to import servers
        importManager.init();
    }

    ImportManager getImportManager() {
        return importManager;
    }

    private synchronized RemoteNode getMyNode() {
        return myNode;
    }

    private synchronized void setMyNode(RemoteNode myNode) {
        this.myNode = myNode;
    }

    /**
     * Reload settings from the configuration file file
     */
    void reloadConfig() {
        producerNode.reloadConfiguration();
        consumerNode.reloadConfiguration();
        importManager.reloadConfiguration();
        Main.getInstance().onConfigChange();
    }

    /**
     * Flush all the data channels logs
     */
    void flushAllLogs(){
        producerNode.flushAllLogs();
        consumerNode.flushAllLogs();
        importManager.flushAllLogs();
    }
    
    /**
     * Reset data channels counters
     */
    void reset() {
        consumerNode.resetCounters();
        producerNode.resetCounters();
        importManager.resetCounters();
}

    /**
     * return true if the node is an ISDN and it has access to the specified import server, or to any import server
     * if import Name is null.
     * @param importName    a string identifying the import server. If null, the method will return true if the node
     *                      has access to any import server.
     * @return
     */
    boolean hasImportAccess(String importName) {
        if (importManager != null)
            return importManager.hasImportAccess(importName);
        return false;
    }
    
    /**
     * return how many import channel are connected
     * @return 
     */
    int getNumberOfImports() {
        return importManager.getNumberOfImportChannels();
    }
    
    /**
     * Check if the argument is our UUID
     * @param uuid
     * @return 0 if uuid is null, 1 if it is us, -1 if it is not.
     */
    int isMe(UUID uuid) {
        return uuid == null ? 0 : uuid.equals(Main.getInstance().getAppUUID()) ? 1 : -1;
    }
    
    /**
     * Given a a set of destination UUIDs (or null), return -1 if we are not in the set, 1 if we are the only element in the
     * set, and 0 if the set is null or contains us as well as others.
     * @param destinations
     * @return 
     */
    int isForMe(Collection<UUID> destinations) {
        return destinations == null ? 0 : !destinations.contains(Main.getInstance().getAppUUID()) ? -1 :
            destinations.size() > 1 ? 0 : 1;
    }
    
    /**
     * Check if the input for the given object code arrives in order. If the origin node has already produced a greater
     * sequence number for this object code it means that we are receiving an older command that needs to be ignored in order
     * to avoid further circulation.
     * @param origUUID  UUID of origin node
     * @param objCode   object code
     * @param seq       received sequence number
     * @return          true if 'seq' is in order.
     */
    boolean isObjectInOrder(UUID origUUID, String objCode, int seq) {
        if (isMe(origUUID) > 0)     // ignore boomerangs
            return false;
        return nodeMap.create(origUUID).createSequencer(objCode).checkInputObjectOrder(seq);
    }
    
    void updateInputActivity(UUID origUUID, int uncompressed, int compressed) {
        nodeMap.create(origUUID).updateInputActivity(uncompressed, compressed);
    }
    
    /**
     * Compare the specified time to the known deploy time of the specified node. If the given time is newer it means that
 the node was recently redeployed and therefore we shall reset all its input counters.
     * @param origUUID  UUID of origin node
     * @param newTime   deploy time of the node
     */
    void checkDeployTime(UUID origUUID, ZDate newTime) {
        nodeMap.create(origUUID).checkDeployTime(newTime);
    }
    
    /**
     * propagate an object to other nodes in the system.
     * @param obj               the object to propagate
     * @param resetting         true if object is resetting
     * @param destinations      UUIDs of the destination data node, or null if shall be broadcast to all
     * @param originChannel     the channel the object is arriving from (do not propagate to that channel)
     */
    void propagateObject(DataObject obj, boolean resetting, Collection<UUID> destinations, DataHandler originChannel) throws Exception
    {
        Collection<UUID> filteredDestinations = nodeMap.filterNodes(obj, destinations, resetting, originChannel);
        if (filteredDestinations == null || !filteredDestinations.isEmpty())
            propagateFilteredObject(obj, resetting, filteredDestinations, originChannel, null);

        // If the object is propagated by commit() (channelFrom is null) it may have a message to import servers
        if (originChannel == null)
            obj.doExport(importManager);
    }
    
    /**
     * propagate an object to other nodes in the system.
     * @param obj               the object to propagate
     * @param resetting         true if object is resetting
     * @param destinations      UUIDs of the destination data node, or null if shall be broadcast to all
     * @param originChannel     the channel the object is arriving from (do not propagate to that channel)
     * @param pipe              if not null, the object is sent in lossless mode and is stored in the pipe for acknowledgment
     */
    void propagateFilteredObject(DataObject obj, boolean resetting, Collection<UUID> destinations, DataHandler originChannel,
        LosslessPipe pipe) throws Exception
    {
        producedObjectMap.create(obj.getObjectCode()).propagateObject(
            obj,
            resetting,
            destinations,
            originChannel,
            pipe
        );
    }
    
    /**
     * Echo a command line to other nodes in the system that need it
     * @param prefix        The character that prefixes the command ($ for regular command, ~ for object removal)
     * @param objCode       The code of the command to echo
     * @param ts            Timestamp of the command
     * @param objectValues  Serialized object fields
     * @param objectKeys    Serialized object key values
     * @param originUUID    UUID of the originating data node
     * @param destinations  UUIDs of the destination data nodes, or null if shall be broadcast to all
     * @param objSeq        Sequence number given to the object by the originating data node
     * @param ackSequence   Sequence number for acknowledging lossless objects
     * @param channelFroms  The channel originating the command. We shall not send back the command to where it came from.
     */
    void propogateCommand(String prefix, String objCode, ZDate ts, String objectKeys, String objectValues, UUID originUUID,
        HashSet<UUID> destinations, int objSeq, Integer ackSequence, DataHandler channelFrom
    ) throws IOException {
        producerNode.propagateCommand(
            prefix, objCode, ts, objectKeys, objectValues, originUUID, destinations, objSeq, ackSequence, channelFrom);
        consumerNode.propagateCommand(
            prefix, objCode, ts, objectKeys, objectValues, originUUID, destinations, objSeq, ackSequence, channelFrom);
    }
    
    /**
     * Echo an ^ACK command to all channels except the channel it came from
     * @param objCode           code of the acknowledged object
     * @param originUUID        acknowledging node
     * @param destinationUUID   acknowledged node
     * @param seq               acknowledged sequence number
     * @param originChannel the channel the object is arriving from (do not propagate to that channel)
     */
    void echoAck(String objCode, UUID originUUID, UUID destinationUUID, int seq, DataHandler originChannel)  {
        producerNode.echoAck(objCode, originUUID, destinationUUID, seq, originChannel);
        consumerNode.echoAck(objCode, originUUID, destinationUUID, seq, originChannel);
    }
    
    /**
     * Request a reset for all consumed objects
     */
    void resetConsumedObjects() {
        synchronized(resetCounter) {
            broadcastResetRequest(
                DataManager.getInstance().getConsumedObjectsAsString(),
                Main.getInstance().getAppUUID(),
                null,
                ZDate.now(),
                resetCounter.postInc(),
                Main.getInstance().getDeployDate(),
                Main.getInstance().getAppUUID(),
                Main.getInstance().getAppParams(),
                Main.getInstance().getAppName(),
                Main.getInstance().getAppVersion(),
                Main.getInstance().getCoreVersion(),
                null,
                null
            );
        }
    }
    
    /**
     * Request a reset for a list of objects over a specified channel. Every reset request is enumerated to prevent repeating
     * circulation.
     * @param objectCodes       A list of codes of objects that need reset separated by ;
     * @param targetUUID        UUID of the service from which the reset is requested, or null if all
     * @param originUUID        UUID of the node that originally requested the objects included in the request request
     * @param applicationParams application parameters (returned from the override of Main.getAppParams) of the origin node
     * @param appName           application name of the origin node
     * @param appVersion        version of the origin node
     * @param coreVersion       Spiderwiz versin of the origin node
     * @param remoteAddress     remote address of the origin node
     * @param userID            User ID the destination application used when connecting to the network (by [consumer-n] or [producer-n]
     *                          property in the configuration file), or null if not defined.
     * @param channel           The channel over which to send the request
     */
    void requestReset(String objectCodes, UUID targetUUID, UUID originUUID, Map<String, String> applicationParams,
        String appName, String appVersion, String coreVersion, String remoteAddress, String userID, DataHandler channel
    ) {
        synchronized(resetCounter) {
            channel.transmitResetRequest(
                objectCodes, Main.getInstance().getAppUUID(), targetUUID, ZDate.now(), resetCounter.postInc(),
                Main.getInstance().getDeployDate(), originUUID, applicationParams, appName, appVersion, coreVersion, remoteAddress
            );
        }
    }
    
    /**
     * Called when an object goes out of sequence and there is a need for an object reset, without supplying the full
     * origin node data.
     * @param objectCode
     * @param targetUUID        UUID of the service from which the reset is requested, or null if all
     * @param channel           The channel over which to send the request
     */
    void requestObectReset(String objectCode, UUID targetUUID, DataHandler channel) {
        requestReset(objectCode, targetUUID, null, null, null, null, null, null, null, channel);
    }
    
    /**
     * Broadcast a ^Reset request to all peers (except 'excludeChannel')
     * @param objectCodes       A list of object codes separated by ';'
     * @param requestorUUID    The service that originated the request
     * @param targetUUID        The service the request is asked from, or null if should be broadcast to the entire mesh
     * @param ts                Timestamp of the request
     * @param sequential        Sequential number of the request
     * @param deployTime        deploy time of origin node. used to control sequence handling.
     * @param originUUID        UUID of the node that originally requested the objects included in the request request
     * @param applicationParams application parameters (returned from the override of Main.getAppParams) of the origin node
     * @param appName           application name of the origin node
     * @param appVersion        version of the origin node
     * @param coreVersion       Spiderwiz versin of the origin node
     * @param remoteAddress     remote address of the origin node
     * @param userID            User ID the destination application used when connecting to the network (by [consumer-n] or [producer-n]
     *                          property in the configuration file), or null if not defined.
     * @param excludeChannel    Skip this channel because this is the channel the original request came from. Null if no channel
     *                          to skip.
     */
    void broadcastResetRequest(String objectCodes, UUID requestorUUID, UUID targetUUID, ZDate ts, int sequential,
        ZDate deployTime, UUID originUUID, Map<String, String> applicationParams, String appName, String appVersion,
        String coreVersion, String remoteAddress, DataHandler excludeChannel
    ) {
        producerNode.broadcastResetRequest(
            objectCodes, requestorUUID, targetUUID, ts, sequential, deployTime, originUUID, applicationParams, appName,
            appVersion, coreVersion, remoteAddress, excludeChannel
        );
        consumerNode.broadcastResetRequest(
            objectCodes, requestorUUID, targetUUID, ts, sequential, deployTime, originUUID, applicationParams, appName,
            appVersion, coreVersion, remoteAddress, excludeChannel
        );
    }
    
    /**
     * Broadcast a ^RemoveNode request to all peers (except 'excludeChannel')
     * @param connectedNodes
     * @param excludeChannel 
     */
    void broadcastDropNodesRequest(Collection<UUID> connectedNodes, DataHandler excludeChannel) {
        producerNode.broadcastDropNodesRequest(connectedNodes, excludeChannel);
        consumerNode.broadcastDropNodesRequest(connectedNodes, excludeChannel);
    }
    
    /**
     * Called after a channel is connected. Send necessary $Reset requests on the given channel
     * @param   channel the requesting DataHandler object. A channel shall not echo to itself reset requests that came from
 other channels
     */
    void resetChannelObjects(DataHandler channel) {
        // First request for ourselves
        requestReset(DataManager.getInstance().getConsumedObjectsAsString(), null, Main.getInstance().getAppUUID(),
            Main.getInstance().getAppParams(), Main.getInstance().getAppName(), Main.getInstance().getAppVersion(),
            Main.getInstance().getCoreVersion(), null, null, channel);
        
        // Now for remote nodes
        nodeMap.requestChannelReset(channel);
    }
    
    /**
     * Use the given SequenceManager for synchronized sequencing and transmission of an object during reset
     * @param obj           The object to code and transmit
     * @param sm            The SequenceManager that manages sequencing over one data channel
     * @param moderator     Transmission moderator, if shall be used. Null otherwise.
     */
    void transmitModeratedObject(DataObject obj, SequenceManager sm, TransmitModerator moderator)
        throws Exception
    {
        Collection<UUID> filteredDestinations = nodeMap.filterNodes(obj, obj.getDestinations(), true, null);
        if (filteredDestinations == null || !filteredDestinations.isEmpty()) {
            moderator.moderate();
            producedObjectMap.create(obj.getObjectCode()).transmitModeratedObject(obj, sm,  filteredDestinations);
        }
    }
    
    /**
     * Add a pending query to the query map of the specified data node.
     * @param query     the query object
     */
    void pendQuery(QueryObject query) {
        nodeMap.create(query.getOriginUUID()).pendQuery(query);
    }
    
    /**
     * Add a pending query to map of the query that we post. The method will assign a new query ID to the query.
     * @param query
     * @return the assigned query ID
     */
    boolean pendMyQuery(QueryObject query) {
        // Don't pend if no destination is active
        if (!nodeMap.verifyActiveDestinations(query.getDestinations()))
            return false;
        synchronized(queryCounter) {
            query.setQueryID(queryCounter.postInc());
        }
        pendQuery(query);
        return true;
    }
    
    /**
     * Get a pending query originated by the specified data node.
     * @param origUUID  UUID of the originator of the query
     * @param queryID   query ID
     * @return the query object
     */
    QueryObject getQuery(UUID origUUID, int queryID) {
        RemoteNode originator = nodeMap.get(origUUID);
        if (originator == null)
            return null;
        return originator.getQuery(queryID);
    }
    
    /**
     * Process a reset request for QueryObject objects. The function resends all our pending queries of the specific type
     * over the requesting channel.
     * @param resetter A Resetter object managing the reset of a specific object type.
     * @return true if stored queries contains at least one query that has been reset
     */
    boolean processQueryReset(Resetter resetter) {
        return myNode == null ? false : myNode.processQueryReset(resetter);
    }
    
    /**
    * Try processing an imported object by each of the pending queries.
    * @param importObject  the object
    * @param channel       the ImportHandler object representing the sending server
    * @param commandTime   the time the object is retrieved from the import server
    * @return a query that processed the object successfully, or null.
    */
    final QueryObject processImportQuery(Object importObject, ImportHandler channel, ZDate commandTime) throws Exception {
        QueryObject successQuery;
        nodeMap.lockRead();
        try {
            for (RemoteNode service : nodeMap.values()) {
                successQuery = service.processImportQuery(importObject, channel, commandTime);
                if (successQuery != null)
                    return successQuery;
            }
        } finally {
            nodeMap.unlockRead();
        }
        return null;
    }
        
    /**
     * Remove all completed or expired entities (do it every 3 minutes)
     */
    void removeObsolete() {
        if (lastObsoleteRemoval.elapsed() < OBSOLETE_REMOVAL_FREQUENCY)
            return;
        lastObsoleteRemoval = ZDate.now();
        ArrayList<RemoteNode> expiredNodes = new ArrayList<>();
        nodeMap.lockRead();
        try {
            for (RemoteNode node : nodeMap.values()) {
                if (node.removeObsolete())
                    expiredNodes.add(node);
            }
        } finally {
            nodeMap.unlockRead();
        }
        if (!expiredNodes.isEmpty())
            nodeMap.removeAll(expiredNodes);
    }
    
    /**
     * Abort all the queries posted by us.
     */
    void abortAllQueries() {
        if (getMyNode() != null)
            myNode.abortAllQueries();
    }

    /**
     * Handle a reset request for a node by setting the specified consumed objects and node's application parameter
 map.
     * @param uuid                      uuid of the node
     * @param consumedObjectsString     a concatenated string of requested object (plus lossless indication)
     * @param appName                   application name of the origin node
     * @param appVersion                version of the origin node
     * @param coreVersion               Spiderwiz versin of the origin node
     * @param remoteAddress             remote address of the origin node
     * @param userID                    user ID attached to the channel the node appeared from
     * @param applicationParams
     * @return true if 'consumeObjects' list length > 0 after executing the method.
     */
    void handleResetRequest(UUID uuid, String consumedObjectsString, String appName, String appVersion,
        String coreVersion, String remoteAddress, String userID, ZDictionary applicationParams
    ) {
        nodeMap.create(uuid).handleResetRequest(consumedObjectsString, appName, appVersion, coreVersion, remoteAddress, userID,
            applicationParams);
    }
    
    void loadAppHistory(UUID uuid, String appName, String appVersion, String coreVersion, String remoteAddress, String userID,
        ZDictionary applicationParams, String objectCodes, ZDate lastSeen)
    {
        nodeMap.create(uuid).
            loadAppHistory(appName, appVersion, coreVersion, remoteAddress, userID, applicationParams, objectCodes, lastSeen);
    }

    
    /**
     * Call on channel (or a channel down the way) disconnection. If any of the nodes in the given collection is not
     * connected to any other channel then we mark the node as disconnected and broadcast the disconnection to other nodes.
     * We also check here whether as result of this disconnection we can stop sending some object types on this channel
     * @param channel           the disconnected channel
     * @param droppedNodes      nodes that were dropped from that channel
     * @param remainingNodes    nodes that remained on that channel
     * @return list of object codes that channel need not send any more
     */
    Ztrings onChannelDisconnection(DataHandler channel, Collection<UUID> droppedNodes, Collection<UUID> remainingNodes) {
        ZHashSet<UUID> dropped = new ZHashSet<>();
        Ztrings objectsConsumedByRemovedNodes = new Ztrings();
        droppedNodes.forEach((nodeUUID) -> {
            RemoteNode node = nodeMap.get(nodeUUID);
            if (node != null) {
                node.addConsumedObjects(objectsConsumedByRemovedNodes);
                if (!producerNode.nodeExistsInAnotherChannel(channel, nodeUUID) &&
                    !consumerNode.nodeExistsInAnotherChannel(channel, nodeUUID)) {
                    node.drop(channel);
                    dropped.add(nodeUUID);
                }
            }
        });
        // Broadcast the delete if necessary
        if (!dropped.isEmpty())
            broadcastDropNodesRequest(droppedNodes, channel);
        
        // Find the objects that were used by the dropped nodes and are not used by the remaining nodes
        if (remainingNodes == null)
            return null;
        for (UUID nodeUUID : remainingNodes) {
            RemoteNode node = nodeMap.get(nodeUUID);
            if (node != null) {
                node.removeConsumedObjects(objectsConsumedByRemovedNodes);
            }
        }
        return objectsConsumedByRemovedNodes;
    }
    
    /**
     * Process an ^ACK command
     * @param objCode           code of the acknowledged object
     * @param originUUID        UUID of acknowledging node
     * @param destinationUUID   UUID of acknowledged node
     * @param serial            serial number of acknowledged object
     * @param channel           the channel through which the acknowledgment arrived
     */
    void processAck(String objCode, UUID originUUID, UUID destinationUUID, int serial, DataHandler channel) {
        // Ignore it if we don't have the origin node in our node map
        RemoteNode node = nodeMap.get(originUUID);
        if (node != null)
            node.processAck(objCode, destinationUUID, serial, channel);
    }

    void cleanup() {
        nodeMap.cleanup();
        importManager.cleanup();
        producerNode.cleanup();
        consumerNode.cleanup();
    }
    
    /**
     * Do the getApplicationsTableData request
     * @param userID        if not null the query is only for applications connecting with this user ID.
     * @return TableData object with the results
     */
    TableData getAppsInfo(String userID) {
        TableData td = new TableData();
        nodeMap.getInfos(userID).forEach((info) -> {
            info.addAdminTableRow(td);
        });
        return td;
    }
    
    String getAppName(UUID uuid) {
        RemoteNode node = nodeMap.get(uuid);
        return node == null ? null : node.getAppName();
    }
    
    /**
     * Do the getConnectedNodesData request
     * @param consumers
     * @param userID        if not null the query is only for nodes connecting with this user ID.
     * @return 
     */
    TableData getDnInfo(boolean consumers, String userID) {
        TableData td = new TableData();
        (consumers ? producerNode : consumerNode).getInfos(userID).forEach((info) -> {
            info.addAdminTableRow(td);
        });
        return td;
    }

    /**
     * Execute the getImporNodesData request
     * @return 
     */
    TableData getImportInfo() {
        TableData td = new TableData();
        importManager.getInfos().forEach((info) -> {
            info.addAdminTableRow(td);
        });
        return td;
    }
}
