package org.spiderwiz.core;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.spiderwiz.zutils.ZArrayList;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZLog;

/**
 * implement server channel handling
 *
 * @author Zvi 
 */
class DataNode {
    private class DataChannels extends ZArrayList<DataHandler> {}
    private class ClientChannels extends HashMap<Integer, DataHandler> {}
    private class Servers extends HashMap<Integer, ServerChannelHandler> {}
    private class NodeInfos extends TreeMap<String, DataNodeInfo> {}

    private MyConfig config;
    private final DataChannels allChannels;
    private final ClientChannels clientChannels;    // set of channels that connect as clients
    private final Servers servers;                  // set of ServerChannelHandler objects that listen to client connections
    private ZLog logger;
    private final boolean producer;            // denote whether this object manages producer channels or consumer channels

    public DataNode(boolean producer) {
        this.producer = producer;
        allChannels = new DataChannels();
        clientChannels = new ClientChannels();
        servers = new Servers();
    }

    public boolean isProducer() {
        return producer;
    }

    /**
     * Initialize the object and use configuration data to establish servers and client connections.
     */
    public void init() {
        config = Main.getMyConfig();
        logger = Main.getLogger();
        reloadConfiguration();
    }
    
    /**
     * Remove a channel from the lists
     * @param channel
     */
    public void removeChannel (DataHandler channel) {
        allChannels.remove(channel);
    }
    
    /**
     * Called after login on a channel. Check if a channel with the same application has connected (and
     * disconnected) before. If it did, use the existing sequence manager and remove the old channel.
     * @param channel a channel executing post login.
     * @return a found sequence manager or null.
     */
    SequenceManager getSequenceManager(DataHandler channel) {
        SequenceManager sm = null;
        DataHandler oldChannel = null;
        try {
            allChannels.lockRead();
            for (DataHandler dc : allChannels) {
                if (dc != channel && dc.isSame(channel)) {
                    oldChannel = dc;
                    break;
                }
            }
        } finally {
            allChannels.unlockRead();
        }
        if (oldChannel != null) {
            if (oldChannel.isConnected())
                logger.logEvent(CoreConsts.DataNode.MULTIPLE_APP_ON_SOCKET, channel.getAppFolderName());
            else {
                sm = oldChannel.getSequenceManager();
                if (sm != null)
                    sm.setSocket(channel);
                removeChannel(oldChannel);
                oldChannel.setSequenceManager(null);
                oldChannel.cleanup();
            }
        }
        return sm;
    }

    /**
     * Flush the log file for each channel
     */
    public void flushAllLogs () {
        try {
            allChannels.lockRead();
            for (DataHandler channel : allChannels) {
                channel.flushLog();
            }
        } finally {
            allChannels.unlockRead();
        }
    }

    /**
     * propagate a command to all connected channels that need it
     * @param prefix        The character that shall prefix the command (normally $)
     * @param objCode       Command code
     * @param ts            Timestamp of the command
     * @param objectValues  Serialized object fields
     * @param objectKeys    Serialized object key values
     * @param originUUID    UUID of the originating data node
     * @param destinations  UUIDs of the destination data node, or null if shall be broadcast to all
     * @param objSeq        Sequence number given to the object by the originating data node
     * @param ackSequence   Sequence number for acknowledging lossless objects
     * @param originChannel the channel the object is arriving from (do not propagate to that channel)
     */
    void propagateCommand(String prefix, String objCode, ZDate ts, String objectKeys, String objectValues, UUID originUUID,
        Collection<UUID> destinations, int objSeq, Integer ackSequence, DataHandler originChannel
    ) throws IOException {
        try {
            allChannels.lockRead();
            for (DataHandler channel : allChannels) {
                if (compareChannel(channel, originChannel))
                    channel.transmitCommand(
                        prefix, objCode, ts, objectKeys, objectValues, originUUID, destinations, objSeq, ackSequence, false);
            }
        } finally {
            allChannels.unlockRead();
        }
    }
    
    /**
     * Echo an ^ACK command to all channels except the channel it came from
     * @param objCode           code of the acknowledged object
     * @param originUUID        acknowledging node
     * @param destinationUUID   acknowledged node
     * @param seq               acknowledged sequence number
     * @param originChannel the channel the object is arriving from (do not propagate to that channel)
     */
    void echoAck(String objCode, UUID originUUID, UUID destinationUUID, int seq, DataHandler originChannel) {
        try {
            allChannels.lockRead();
            for (DataHandler channel : allChannels) {
                if (channel != originChannel)
                    channel.sendAck(objCode, originUUID, destinationUUID, seq);
            }
        } finally {
            allChannels.unlockRead();
        }
    }
    
    /**
     * Send a ^Reset request to all channels (except 'excludeSocket')
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
     * @param originChannel     Skip this channel because this is the channel the original request came from. Null if no
     *                          channel to skip.
     */
    void broadcastResetRequest(String objectCodes, UUID requestorUUID, UUID targetUUID, ZDate ts, int sequential,
        ZDate deployTime, UUID originUUID, Map<String, String> applicationParams, String appName,  String appVersion,
        String coreVersion, String remoteAddress, DataHandler originChannel
    ) {
        allChannels.lockRead();
        try {
            for (DataHandler channel : allChannels) {
                if (compareChannel(channel, originChannel))
                    channel.transmitResetRequest(objectCodes, requestorUUID, targetUUID, ts, sequential, deployTime,
                        originUUID, applicationParams, appName, appVersion, coreVersion, remoteAddress);
            }
        } finally {
            allChannels.unlockRead();
        }
    }
    
    /**
     * Send a ^RemoveNodes request to all channels (except 'excludeSocket')
     * @param connectedNodes
     * @param originChannel 
     */
    void broadcastDropNodesRequest(Collection<UUID> connectedNodes, DataHandler originChannel) {
        allChannels.lockRead();
        try {
            for (DataHandler channel : allChannels) {
                if (compareChannel(channel, originChannel))
                    channel.transmitDropNodesRequest(connectedNodes);
            }
        } finally {
            allChannels.unlockRead();
        }
    }
    
    /**
     * Determine whether to send data to a target channel depending on properties of the origin channel
     * @param targetChannel
     * @param originChannel
     * @return true if data shall be sent
     */
    boolean compareChannel(DataHandler targetChannel, DataHandler originChannel) {
        return (targetChannel != originChannel &&
            (   originChannel == null ||
                targetChannel.getAppUser() == null && originChannel.getAppUser() == null ||
                targetChannel.getAppUser() != null && targetChannel.getAppUser().equalsIgnoreCase(originChannel.getAppUser())
            )
        );
    }
    
    void addChannel (DataHandler dataChannel) {
        if (dataChannel != null)
            allChannels.add(dataChannel);
    }

    /**
     * Establish and reestablish connections when application configuration is changed.
     */
    void reloadConfiguration() {
        reconnectClientChannels();
        reestablishServers();
    }
    
    /**
    /**
     * walk through [consumer/producer-n] properties of the configuration file, find what was changed, and act
     * accordingly.
     */
    private void reconnectClientChannels() {
        synchronized (clientChannels) {
            for (int i = 0; i < CoreConsts.DataNode.MAX_CLIENT_CONNECTIONS; i++) {
                String def = null;
                // i = 0 is the special case of 'spideradmin' property.
                if (i == 0) {
                    // spideradmin users are always producers.
                    if (producer) {
                        String user = config.getProperty(MyConfig.SPIDERADMIN);
                        if (user != null && !user.isEmpty())
                            def = MyConfig.SPIDERADMIN + '=' + user;
                    }
                } else
                    def = config.getProperty((producer ? MyConfig.PRODUCER_PREFIX : MyConfig.CONSUMER_PREFIX) + i);
                if (def != null && def.isEmpty())
                    def = null;
                DataHandler channel = clientChannels.get(i);
                if (channel != null && (def == null || !def.equals(channel.getDefString()))) {
                    channel.cleanup();
                    clientChannels.remove(i);
                    removeChannel(channel);
                    channel = null;
                }
                if (def != null && channel == null) {
                    channel = new DataHandler(this);
                    if (channel.initClientChannel(def, i))
                        addChannel(channel);
                    clientChannels.put(i, channel);
                }
            }
        }
    }
    
    /**
     * walk through [consumer server/producer server-n] properties of the configuration file, find what was changed, and act
     * accordingly.
     */
    private void reestablishServers() {
        synchronized(servers) {
            for (int i = 1; i < CoreConsts.DataNode.MAX_SERVER_LISTENERS; i++) {
                String def =
                    config.getProperty((producer ? MyConfig.PRODUCER_SERVER_PREFIX : MyConfig.CONSUMER_SERVER_PREFIX) + i);
                if (def != null && def.isEmpty())
                    def = null;
                ServerChannelHandler server = servers.get(i);
                if (server != null && (def == null || !def.equals(server.getDefString()))) {
                    server.cleanup();
                    servers.remove(i);
                    server = null;
                }
                if (def != null && server == null) {
                    server = new ServerChannelHandler(this);
                    server.init(def, i);
                    servers.put(i, server);
                }
            }
        }
    }
    
    /**
     * Get DataNodeInfo objects associated with all channels, ordered by application Name + address
     * @return a collection of requested objects
     */
    Collection<DataNodeInfo> getInfos(String userID) {
        NodeInfos infos = new NodeInfos();
        try {
            allChannels.lockRead();
            for (DataHandler channel : allChannels) {
                if (channel.getAppUUID() != null &&
                    (userID == null || userID.equalsIgnoreCase(channel.getAppUser()))
                )
                    infos.put(channel.getAppName() + channel.getRemoteAddress() + channel.getAppUUID(), channel.getInfo());
            }
        } finally {
            allChannels.unlockRead();
        }
        return infos.values();
    }
    
    void resetCounters() {
        try {
            allChannels.lockRead();
            for (DataHandler channel : allChannels) {
                channel.resetCounters();
            }
        } finally {
            allChannels.unlockRead();
        }
    }

    /**
     * Call on channel disconnection. Check if the given node is connected to any another channel
     * then we shall remove the node from our node map.
     * @param disconnectedChannel   the disconnected channel
     * @param node                  a node that was connected to that channel
     * @return true if found the node in another channel
     */
    boolean nodeExistsInAnotherChannel(DataHandler disconnectedChannel, UUID node) {
        allChannels.lockRead();
        try {
            for (DataHandler channel : allChannels) {
                if (channel != disconnectedChannel && channel.nodeExistsInRemoteNodeList(node))
                    return true;
            }
        } finally {
            allChannels.unlockRead();
        }
        return false;
    }
    
    /**
     * Disconnect all channels and close all servers when the object is closed
     */
    public void cleanup() {
        try {
            allChannels.lockRead();
            for (DataHandler channel : allChannels)
                channel.cleanup();
        } finally {
            allChannels.unlockRead();
        }
        synchronized(servers){
            for (ServerChannelHandler server : servers.values())
                server.cleanup();
        }
    }
}