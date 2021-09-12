package org.spiderwiz.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.spiderwiz.plugins.FileChannel;
import org.spiderwiz.plugins.PluginConsts;
import org.spiderwiz.plugins.TcpSocket;
import org.spiderwiz.plugins.Websocket;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZDictionary;
import org.spiderwiz.zutils.ZHashMap;
import org.spiderwiz.zutils.ZHashSet;
import org.spiderwiz.zutils.ZLog;
import org.spiderwiz.zutils.ZModInteger;
import org.spiderwiz.zutils.ZThread;
import org.spiderwiz.zutils.ZUtilities;
import org.spiderwiz.zutils.Ztrings;

/**
 * Handles the socket by which a data server is connected to the Gateway Server.
 * @author @author  zvil
 */
final class DataHandler extends ChannelHandler {
    /**
     * An inner class that performs socket monitoring
     */
    private class ChannelMonitor extends ZThread {
        
        private ZDate idleSince = null;
        
        /**
         * Check if channel is active
         * @return 
         */
        private boolean checkChannel() {
            synchronized(DataHandler.this) {
                if (channel == null)
                    return false;
                channel.ping();
                if (channel.isConnected())
                    idleSince = null;
                if (needsRelogin())
                    login();
                return true;
            }
        }
        
        @Override
        protected void doLoop() {
            try {
                if (!checkChannel()) {
                    // Check if there is a need to relogin
                    if (idleSince == null)
                        idleSince = ZDate.now();
                    if (idleSince.elapsed() >= Main.getMyConfig().getObsolescenceTime()) {
                        DataHandler.this.cleanup();
                        initiatingNode.removeChannel(DataHandler.this);
                        return;
                    }
                }

                if (getSequenceManager() != null)
                    sequenceManager.monitor();
            } catch (Exception ex) {
                Main.getInstance().sendExceptionMail(ex,
                    String.format(CoreConsts.AlertMail.EXCEPTION_CHANNEL_MONITORING, getRemoteAddress()), null, false);
            }
        }

        @Override
        protected long getLoopInterval() {
            // If relogin may be needed, the time to wait should reflect that
            return getLoginSent() != null ? RELOGIN_DEFAULT : pingRate;
        }
    }
    
    /**
     * A class that represents a peer data node connected through this socket;
     */
    private final class PeerNode {
        private final ZModInteger nextResetSeq;
        private ZDate lastRequestTs = null;

        public PeerNode() {
            nextResetSeq = new ZModInteger(CoreConsts.SEQUENCE_MODULU);
        }
        
        /**
         * Check the order of a $Reset request from this service
         * @param ts    timestamp of the request
         * @param seq   sequential number of the request
         * @return true if this is a new request that should be handled
         */
        synchronized boolean checkResetOrder (ZDate ts, int seq) {
            if (lastRequestTs != null) {
                if (ts.before(lastRequestTs))
                    return false;
                if (ts.equals(lastRequestTs) && nextResetSeq.compareTo(seq) > 0)
                    return false;
            }
            lastRequestTs = ts;
            nextResetSeq.setValue(++seq);
            return true;
        }
    }
    
    private class PeerNodeMap extends ZHashMap<UUID, PeerNode> {
        /**
         * Find the specified element in the map. If it doesn't exist create it and insert it to the map
         * @param uuid
         * @return 
         */
        PeerNode create(UUID uuid) {
            PeerNode node = get(uuid);
            if (node == null) {
                // Use putIfAbsent because the sequence get-put is not sychronized in this method
                node = putIfAbsentReturnNew(uuid, new PeerNode());
            }
            return node;
        }
    }
    
    class CommandOffsets {
        static final int COMMAND_CODE = 0;
        static final int TIMESTAMP = 1;
        static final int SEQUENCE_NUMBER = 2;
        static final int SUBHEADER = 3;
        static final int KEYS = 4;
        static final int VALUES = 5;
        static final int LENGTH = 6;
        class SubheaderOffsets {
            static final int ORIGIN = 0;
            static final int DESTINATIONS = 1;
            static final int OBJ_SEQ = 2;
            static final int ACK_SEQ = 3;
        }
    }
    
    static class CommandComponents {
        private final ZDate timestamp;
        private final UUID origin;
        private final HashSet<UUID> destinations;
        private final int objSeq;
        private final Integer ackSeq;
        private final String keys;
        private final String fields;
        private final String expandedCommand;

        public CommandComponents(ZDate timestamp, UUID origin, HashSet<UUID> destinations, int objSeq, Integer ackSeq, String keys,
            String fields, String expandedCommand)
        {
            this.timestamp = timestamp;
            this.origin = origin;
            this.destinations = destinations;
            this.objSeq = objSeq;
            this.ackSeq = ackSeq;
            this.keys = keys;
            this.fields = fields;
            this.expandedCommand = expandedCommand;
        }
    }
    
    private Channel channel = null;
    private ZLog logger;
    private String appName, appVersion, coreVersion;
    private final boolean producer;
    private boolean idle = false, alertByMe = false, alertByOtherSize = false;
    private int logInput = CoreConsts.DataChannel.LOG_UNDEFINED;
    private int logOutput = CoreConsts.DataChannel.LOG_UNDEFINED;
    private boolean flushLogs = false;
    private UUID appUUID = null;
    private String userID = null, appUser;
    private String defString = null;
    private final DataNode initiatingNode;
    private ServerChannelHandler channelServer = null;
    private SequenceManager sequenceManager = null;
    private final DataNodeInfo info;
    private ZDate lastInput = null, connectedSince = null;
    private final ChannelMonitor channelMonitor;
    private final PeerNodeMap peerNodeMap;
    private final ZHashSet<UUID>connectedNodes;
    private ZDate loginSent = null;
    private boolean loginReceived = false;
    private boolean gateway = false;
    private static final int RELOGIN_DEFAULT = 10 * ZDate.SECOND;
    private int compress;
    static final int NO_COMPRESSION = 0;
    static final int ZIP_COMPRESSION = 1;
    static final int LOGICAL_COMPRESSION = 2;
    static final int FULL_COMPRESSION = ZIP_COMPRESSION | LOGICAL_COMPRESSION;
    private long pingRate;

    DataHandler(DataNode initiatingNode) {
        this.initiatingNode = initiatingNode;
        this.producer = initiatingNode.isProducer();
        this.logger = Main.getLogger();
        info = new DataNodeInfo(this);
        appName = appVersion = coreVersion = "?";
        channelMonitor = new ChannelMonitor();
        peerNodeMap = new PeerNodeMap();
        connectedNodes = new ZHashSet<>();
    }

    private synchronized Channel getChannel() {
        return channel;
    }

    private synchronized void setChannel(Channel channel) {
        this.channel = channel;
    }

    synchronized ZDate getLastInput() {
        return lastInput;
    }

    private synchronized void setLastInput(ZDate lastInput) {
        this.lastInput = lastInput;
    }

    boolean isSame(DataHandler other) {
        return appUUID != null ? appUUID.equals(other.appUUID) : getAppFolderName().equals(other.getAppFolderName());
    }

    public String getAppUser() {
        return appUser;
    }

    int getLogOutput() {
        return logOutput;
    }

    public void setLogInput(int logInput) {
        this.logInput = logInput;
    }

    public void setLogOutput(int logOutput) {
        this.logOutput = logOutput;
    }

    public void setFlushLogs(boolean flushLogs) {
        this.flushLogs = flushLogs;
    }

    public void setGateway(boolean gateway) {
        this.gateway = gateway;
    }

    public void setCompress(int compress) {
        this.compress = compress;
    }

    public void setPingRate(long pingRate) {
        this.pingRate = pingRate;
    }
    
    /**
     * Initialize a server-side channel.
     * @param channel       the channel object created by a class extending ZServerChannel
     * @param channelServer the event handler for the the class that created channel
     */
    void initServerChannel(Channel channel, ServerChannelHandler channelServer) throws Exception {
        setChannel(channel);
        this.channelServer = channelServer;
        channel.init(this);
        channel.setMonitorInterval(0);      // no need because we use our own monitoring
        startLog(!channel.isFileChannel(), null);
        channelMonitor.execute();
    }

    /**
     * Initializes a client channel when the connection info is taken from the configuration file.
     * @param defString     connection information as appears in the configuration file
     * @return true if the channel was established successfully.
     */
    boolean initClientChannel(String defString, int n) {
        try {
            this.defString = defString;
            String customClass;
            ZDictionary configParams = ZDictionary.parseParameterList(defString);
            userID = null;
            if ((customClass = configParams.get(CoreConsts.Channel.CLASS)) != null) {
                try {
                    channel = (Channel)Class.forName(customClass).getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    String msg = String.format(CoreConsts.AlertMail.CHANNEL_CLASS_FAILED, customClass, ex.toString());
                    Main.getLogger().logEvent(msg);
                    Main.getInstance().sendNotificationMail(msg, null, null, true);
                    return false;
                }
            } else if (configParams.get(PluginConsts.TcpSocket.IP) !=  null && configParams.get(PluginConsts.TcpSocket.PORT) != null)
                channel = new TcpSocket();
            else if (configParams.get(PluginConsts.FileChannel.INFILE) != null &&
                configParams.get(PluginConsts.FileChannel.OUTFILE) != null)
                channel = new FileChannel();
            else if (configParams.get(PluginConsts.WebSocket.WEBSOCKET) != null)
                channel = new Websocket();
            else if ((userID = configParams.get(MyConfig.SPIDERADMIN)) != null) {
                try {
                    channel = (Channel)Class.forName(PluginConsts.SpiderAdmin.SPIDERADMIN_CLASS).getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    Main.getLogger().logEvent(PluginConsts.SpiderAdmin.CLASS_NOT_FOUND);
                    return false;
                }
            } else
                return false;
                
            String alert = configParams.get(CoreConsts.DataChannel.ALERT);
            if (alert != null) {
                switch (alert) {
                case CoreConsts.DataChannel.CLIENT:
                    alertByMe = true;
                    break;
                case CoreConsts.DataChannel.SERVER:
                    alertByOtherSize = true;
                    break;
                case CoreConsts.DataChannel.BOTH:
                    alertByMe = alertByOtherSize = true;
                    break;
                }
            }
            logInput = Main.getMyConfig().interpretLogParam(configParams, CoreConsts.DataChannel.LOGINPUT);
            logOutput = Main.getMyConfig().interpretLogParam(configParams, CoreConsts.DataChannel.LOGOUTPUT);
            flushLogs = configParams.containsKey(CoreConsts.DataChannel.FLUSHLOGS);
            compress = Main.getMyConfig().
                interpretCompressParam(configParams, channel.isCompressable() ? FULL_COMPRESSION : NO_COMPRESSION);
            if (userID == null)
                userID = configParams.get(CoreConsts.DataChannel.USER);
            pingRate = MyConfig.getPingRate(configParams);

            if (channel != null) {
                channel.init(this);
                if (channel.configure(configParams, producer ? 1 : 2, n)) {
                    channel.setReconnectWait(Main.getMyConfig().getReconnectTime());
                    channel.setBufferFile(configParams.get(CoreConsts.Channel.BUFFERFILE));
                    channel.setMonitorInterval(0);      // no need because we use our own monitoring
                    channelMonitor.execute();
                    channel.execute();
                    return true;
                }
            }
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex,
                String.format(CoreConsts.AlertMail.EXCEPTION_INIT_CLIENT_CHANNEL, getRemoteAddress()), null, false);
        }
        return false;
    }

    public synchronized String getAppName() {
        return appName;
    }

    public synchronized String getAppVersion() {
        return appVersion;
    }

    public synchronized String getCoreVersion() {
        return coreVersion;
    }

    public UUID getAppUUID() {
        return appUUID;
    }

    ZHashSet<UUID> getConnectedNodes() {
        return connectedNodes;
    }

    /**
     * get the data node context path
     * @return 
     */
    public synchronized String getContextPath() {
        return getChannel() == null ? null : "xadmin" + ":" + appUUID;
    }

    /**
     * Get application folder name (for logs and backup).
     * @return the application name, as received from the peer, followed by a period then the IP of the peer.
     */
    String getAppFolderName() {
        String address = getRemoteAddress();
        int n = address.lastIndexOf(":");
        String result = appName + "." + (n < 0 ? address : address.substring(0, n));
        if (appUser != null)
            result += "." + appUser;
        return result;
    }

    ZLog getLogger() {
        return logger;
    }

    synchronized SequenceManager getSequenceManager() {
        return sequenceManager;
    }

    synchronized void setSequenceManager(SequenceManager sequenceManager) {
        this.sequenceManager = sequenceManager;
    }

    DataNodeInfo getInfo() {
        return info;
    }

    public synchronized ZDate getConnectedSince() {
        return connectedSince;
    }

    private synchronized void setConnectedSince(ZDate connectedSince) {
        this.connectedSince = connectedSince;
    }

    private synchronized ZDate getLoginSent() {
        return loginSent;
    }

    private synchronized boolean needsRelogin() {
        return loginSent != null && loginSent.elapsed() >= RELOGIN_DEFAULT;
    }

    private synchronized void setLoginSent(ZDate loginSent) {
        this.loginSent = loginSent;
    }

    private synchronized boolean isLoginReceived() {
        return loginReceived;
    }

    private synchronized void setLoginReceived(boolean loginReceived) {
        this.loginReceived = loginReceived;
    }
    
    /**
     * Flush the log file attached to this socket
     */
    void flushLog() {
        if (logger != null)
            logger.flush();
    }

    private void startLog(boolean append, String name) throws Exception {
        // If name is not null each socket has its own log
        if (name != null) {
            if (logger != null)
                logger.cleanup();
            logger = new ZLog(Main.getInstance().getRootFolder(), Main.getMyConfig(), append);
            logger.init(MyConfig.LOGFOLDER);
            String subRoot = producer ? "Consumers/" : "Producers/";
            logger.setRootPath(logger.getRootPath() + subRoot + MyUtilities.escapeNonAlphanumeric("-", name) + "/");
        }
    }
    
    /**
     * Process a ^L command
     * @param s command elements (separated by commas in the original command). s[0] is the $L itself.
     */
    private void processLogin(String s[]) throws Exception {
        if (isLoginReceived())      // ignore repeating logins
            return;
        setLoginReceived(true);
        int i = 1;
        boolean isConsumer = s[i++].equals(CoreConsts.DataChannel.CONSUMER);
        
        // Ignore producer to consumer log
        if (!isConsumer && !producer)
            return;
        
        // parse rest of parameters
        appName = Serializer.unescapeDelimiters(s[i++]);
        appVersion = Serializer.unescapeDelimiters(s[i++]);
        coreVersion = Serializer.unescapeDelimiters(s[i++]);
        compress &= ZUtilities.parseInt(s[i++]);
        alertByMe |= s[i++].equals(CoreConsts.DataChannel.ALERT);
        appUUID = UUID.fromString(s[i++]);
        if (i < s.length) {
            appUser = Serializer.unescapeDelimiters(s[i++]);
            if (appUser.isEmpty())
                appUser = null;
        }
        
        // Login can be done only from consumers to producers
        Main main = Main.getInstance();
        String ackLoginCommand = ZUtilities.concatAll(
            ",", CoreConsts.DataChannel.ACK_LOGIN_COMMAND,
            producer && isConsumer ? CoreConsts.DataChannel.LOGIN_OK : CoreConsts.DataChannel.LOGIN_FAILED,
            compress, main.getLoginParams(), alertByOtherSize ? CoreConsts.DataChannel.ALERT : "", main.getAppUUID(),
            Serializer.escapeDelimiters(userID));
        transmit(ackLoginCommand, true);
        String logResult =
            producer ? isConsumer ? CoreConsts.DataChannel.LOGGED_IN : CoreConsts.DataChannel.BOTH_PRODUCERS :
            CoreConsts.DataChannel.BOTH_CONSUMERS;
        Main.getLogger().logNow(logResult, getRemoteAddress(), appName, appVersion, coreVersion);
        if (producer && isConsumer) {
            if ((compress & ZIP_COMPRESSION) > 0)
                channel.compressOutput();
            postLogin();
        }
    }
    
    private void processLoginAck(String s[]) throws Exception {
        setLoginSent(null);     // no need to send login any more
        int i = 1;
        boolean ok = s[i++].equals(CoreConsts.DataChannel.LOGIN_OK);
        compress &= ZUtilities.parseInt(s[i++]);
        appName = Serializer.unescapeDelimiters(s[i++]);
        appVersion = Serializer.unescapeDelimiters(s[i++]);
        coreVersion = Serializer.unescapeDelimiters(s[i++]);
        alertByMe |= s[i++].equals(CoreConsts.DataChannel.ALERT);
        appUUID = UUID.fromString(s[i++]);
        if (i < s.length) {
            appUser = Serializer.unescapeDelimiters(s[i++]);
            if (appUser.isEmpty())
                appUser = null;
        }
        if (ok) {
            Main.getLogger().logNow(CoreConsts.DataChannel.LOG_ACKED, getRemoteAddress(), appName, appVersion, coreVersion);
            if ((compress & ZIP_COMPRESSION) > 0)
                channel.compressOutput();
            postLogin();
        } else
            channel.disconnect(CoreConsts.DataChannel.LOGIN_REFUSED);
    }
    
    private void postLogin() throws Exception {
        startLog(!channel.isFileChannel(), getAppFolderName());
        if (getSequenceManager() == null) {
            SequenceManager sm = initiatingNode.getSequenceManager(this);
            if (sm == null)
                sm = new SequenceManager(this);
            setSequenceManager(sm);
        } else
            sequenceManager.resetAll();
        Hub.getInstance().resetChannelObjects(this);
        boolean logicalCompression = (compress & LOGICAL_COMPRESSION) > 0;
        boolean fullLog =
            (Main.getMyConfig().getLogMode(CoreConsts.DataChannel.LOGOUTPUT, getLogOutput()) & CoreConsts.DataChannel.LOG_FULL)
            > 0;
        boolean rawLog =
            (Main.getMyConfig().getLogMode(CoreConsts.DataChannel.LOGOUTPUT, getLogOutput()) & CoreConsts.DataChannel.LOG_RAW)
            > 0;
        sequenceManager.onLogin(logicalCompression, fullLog, rawLog, flushLogs);
    }
    
    private void processReset(String s[]) throws Exception {
        if (s.length < 5)
            return;
        int i = 1;
        
        // Parse parameters
        String objectList = s[i++];
        ZDate ts = ZDate.parseFullTimestamp(s[i++]);
        int seq = ZUtilities.parseInt(s[i++]);
        UUID requestorUUID = UUID.fromString(s[i++]);
        UUID destUUID = null;
        if (i < s.length) {
            String dest = s[i++];
            if (!dest.equals("*"))
                destUUID = UUID.fromString(dest);
        }
        ZDate deployTime = i < s.length ? ZDate.parseFullTimestamp(s[i++]) : null;
        UUID originUUID = null;
        if (i < s.length) {
            String uuid = s[i++];
            if (!uuid.isEmpty())
                originUUID = UUID.fromString(uuid);
        }
        if (Hub.getInstance().isMe(originUUID) > 0)     // Ignore boomerangs
            return;
        if (originUUID != null)
            Hub.getInstance().addUserID(originUUID, getAppUser());
        if (!isResetInOrder(ts, seq, requestorUUID))
            return;
        ZDictionary applicationParams = i < s.length ? Serializer.parseParameterList(s[i++]) : null;
        String requestingAppName = i < s.length ? Serializer.unescapeDelimiters(s[i++]) : null;
        String requestingAppVersion = i < s.length ? Serializer.unescapeDelimiters(s[i++]) : null;
        String requestingCoreVesion = i < s.length ? Serializer.unescapeDelimiters(s[i++]) : null;
        String requestingRemoteAddress = i < s.length ? Serializer.unescapeDelimiters(s[i++]) : null;
        if (requestingAppName != null && !requestingAppName.isEmpty()) {
            if (gateway || requestingRemoteAddress == null || requestingRemoteAddress.isEmpty())
                requestingRemoteAddress = getRemoteAddress();
        }

        // Execute the request
        ConsumedObjectMap requestedObjects = new ConsumedObjectMap(){
            @Override
            Boolean getLosslessController(String objCode) {
                return true;
            }
        };
        // If we are not in hub mode retain only the object codes that we produce.
        requestedObjects.fromString(objectList, null);
        objectList = requestedObjects.getAsString();

        // Log the request
        logger.logNow(requestingAppName != null && !requestingAppName.isEmpty() ?
            CoreConsts.DataChannel.REQ_APP_RESET : CoreConsts.DataChannel.REQ_RESET,
            objectList, requestingAppName, requestingRemoteAddress);

        // If this is a full reset request we need to store application data and add the origin node to the connected node
        // list.
        if (originUUID != null && Hub.getInstance().isMe(originUUID) < 0) {
            connectedNodes.add(originUUID);
            Hub.getInstance().handleResetRequest(originUUID, objectList, requestingAppName, requestingAppVersion,
                requestingCoreVesion, requestingRemoteAddress, applicationParams);
        }

        // Process the request if it is for us
        if (getSequenceManager() != null && Hub.getInstance().isMe(destUUID) >= 0)
            sequenceManager.resetOutput(requestedObjects.keySet(), this);

        if (deployTime != null && originUUID != null)
            Hub.getInstance().checkDeployTime(originUUID, deployTime);
        
        Hub.getInstance().broadcastResetRequest(
            objectList, requestorUUID, destUUID, ts, seq, deployTime, originUUID, applicationParams,
            requestingAppName, requestingAppVersion, requestingCoreVesion, requestingRemoteAddress, this
        );
    }
    
    /**
     * Process a ^RemoveNodes command
     * @param s     command fields
     */
    private void processRemoveNodes(String s[]) {
        ZHashSet<UUID> removedNodes = Ztrings.split(s[1]).toUUIDs();
        if (connectedNodes.removeAll(removedNodes) && getSequenceManager() != null)
            sequenceManager.stopSending(Hub.getInstance().onChannelDisconnection(this, removedNodes, connectedNodes));
    }
    
    /**
     * Check if the request is a duplicate. This is important for avoiding looping requests.
     * @param ts            timestamp of the original request
     * @param seq           sequential number at the originator of the request
     * @param originUUID    origin service UUID
     */
    private boolean isResetInOrder (ZDate ts, int seq, UUID originUUID) {
        if (Hub.getInstance().isMe(originUUID) > 0)   // Ignore boomerangs
            return false;
        return peerNodeMap.create(originUUID).checkResetOrder(ts, seq);
    }

    /**
     * Transmit a ^RESET command
     * @param objectCodes       a list of codes of objects that need reset, separated by ";"
     * @param requestorUUID     UUID of the node requesting the reset. Null if not known.
     * @param targetUUID        UUID of the node from which the reset is requested, or null if all
     * @param ts                time stamp of the request
     * @param sequence          sequence number of the request (counted at the origin service).
     * @param deployTime        deploy time of origin node. used to control sequence handling.
     * @param originUUID        UUID of the node that originally requested the objects included in the request request
     * @param applicationParams application parameters (returned from the override of Main.getAppParams) of the origin node
     * @param appName           application name of the origin node
     * @param appVersion        version of the origin node
     * @param coreVersion       Spiderwiz versin of the origin node
     * @param remoteAddress     remote address of the origin node
     * @param userID            User ID the destination application used when connecting to the network (by [consumer-n] or [producer-n]
     *                          property in the configuration file), or null if not defined.
     */
    void transmitResetRequest(String objectCodes, UUID requestorUUID, UUID targetUUID, ZDate ts, int sequence,
        ZDate deployTime, UUID originUUID, Map<String, String> applicationParams, String appName, String appVersion,
        String coreVersion, String remoteAddress
    )
    {
        transmit(ZUtilities.concatAll(",",
                CoreConsts.DataChannel.RESET_COMMAND,
                objectCodes,
                ts.formatFullTimestamp(),
                sequence,
                requestorUUID,
                targetUUID == null ? "*" : targetUUID,
                deployTime == null ? "" : deployTime.formatFullTimestamp(),
                originUUID,
                applicationParams == null ? null : Serializer.encodeParameterList(applicationParams),
                Serializer.escapeDelimiters(appName),
                Serializer.escapeDelimiters(appVersion),
                Serializer.escapeDelimiters(coreVersion),
                Serializer.escapeDelimiters(remoteAddress)
            ),
            true);
    }
    
    /**
     * Transmit a ^RemoveNodes
     * @param connectedNodes 
     */
    void transmitDropNodesRequest(Collection<UUID> connectedNodes) {
        transmit(ZUtilities.concatAll(",", CoreConsts.DataChannel.REMOVE_NODES,
        ZUtilities.concatAll(";", connectedNodes)), true);
    }
    
    /**
     * Process a serialized command arriving from a data node
     * @param fields    The command split to components
     * @param command   The complete command
     * return true if processed successfully
     * @throws Exception 
     */
    private boolean processCommand(String command) throws Exception {
        if (getSequenceManager() == null)
            return false;
        
        // Parse command header
        String[] fields = command.split(String.valueOf(Serializer.FIELD_SEPARATOR), CommandOffsets.LENGTH);
        if (fields.length < 4 || fields[0].length() < 2)
            return false;
        String prefix = fields[CommandOffsets.COMMAND_CODE].substring(0, 1);
        if (prefix.equals("^"))
            return false;                 // ignore unprocessed control commands
        String objCode = fields[CommandOffsets.COMMAND_CODE].substring(1);         // strip the prefix
        CommandComponents components = sequenceManager.parseCommandComponents(prefix, objCode, fields);
        if (components == null) {
            info.updateErrors();
            return false;
        }
        info.updateActivity(components.timestamp, components.expandedCommand.length());
        
        int logMode = Main.getMyConfig().getLogMode(CoreConsts.DataChannel.LOGINPUT, logInput);
        if ((logMode & CoreConsts.DataChannel.LOG_RAW) > 0)
            logger.log("<- " + command, false);
        if ((logMode & CoreConsts.DataChannel.LOG_FULL) > 0)
            logger.log("<= " + components.expandedCommand, false);
        if (flushLogs)
            logger.flush();
        
        // Ignore the message if 'objSeq' is not in order
        if (!Hub.getInstance().isObjectInOrder(components.origin, objCode, components.objSeq))
            return true;
        
        // Pass to data manager (if message is for us) then, if necessary, forward the object to other data nodes.
        try {
            DataObject obj;
            if ((   Hub.getInstance().isForMe(components.destinations) < 0 ||
                    !DataManager.getInstance().isConsumingObject(objCode) && !QueryObject.isQueryReply(prefix, components.fields) ||
                    ((obj = DataManager.getInstance().processCommand(
                        prefix, objCode, components.keys, components.fields, components.origin, components.destinations, this,
                        components.expandedCommand, components.timestamp, components.ackSeq, command.length()
                    )) == null || !obj.onlyForMe()) &&
                Hub.getInstance().isForMe(components.destinations) == 0
                ) &&
                (   Main.getMyConfig().isHubMode() ||
                    DataManager.getInstance().isProducingObject(objCode)
                )
            ) { 
                Hub.getInstance().propogateCommand(
                    prefix, objCode, components.timestamp, components.keys, components.fields, components.origin,
                    components.destinations, components.objSeq, components.ackSeq, this
                );
            }
        } catch (Exception ex) {
            info.updateErrors();
            Main.getInstance().sendExceptionMail(ex,
                String.format(CoreConsts.AlertMail.OBJECT_PARSING_ERROR_ALERT, appName, getRemoteAddress()),
                String.format(CoreConsts.AlertMail.WHEN_PARSING_COMMAND, command, components.expandedCommand),
                false);
        }
        return true;
    }

    /**
     * Send a ^ACK command (if necessary) for the given 'objCode' and 'seq' values
     * @param objCode           code of the acknowledged object
     * @param originUUID        acknowledging node
     * @param destinationUUID   acknowledged node
     * @param seq               acknowledged sequence number
     */
    void sendAck(String objCode, UUID originUUID, UUID destinationUUID, int seq) {
        if (getConnectedNodes().contains(destinationUUID))
            transmit(String.format(CoreConsts.ACK_COMMAND, objCode, originUUID, destinationUUID, seq), false);
    }
        
    /**
     * Echo a command received from a data node to other nodes that need it
     * @param prefix        The character that shall prefix the command (normally $)
     * @param objCode       The command code. This is checked against the commands required by the peer of this socket. The
     *                      command is sent only if it is indeed required. If 'cmdCode' is null they command will be always
     *                      sent without counter resequence.
     * @param ts            Timestamp of the command
     * @param objectValues  Serialized object fields
     * @param objectKeys    Serialized object key values
     * @param originUUID    UUID of the originating data node
     * @param destinations  UUIDs of the destination data nodes, or null if shall be broadcast to all
     * @param objSeq        Sequence number given to the object by the originating data node
     * @param ackSequence   Sequence number for acknowledging lossless objects
     * @param moderated     True if transmission of the object is moderated. If it is not, the moderator shall count
     *                      the object. If it is moderated, moderation was done before calling this method.
     * @param reply         True if this a query reply
     */
    void transmitCommand(String prefix, String objCode, ZDate ts, String objectKeys, String objectValues, UUID originUUID,
        Collection<UUID> destinations, int objSeq, Integer ackSequence, boolean moderated, boolean reply
    ) {
        if (getSequenceManager() != null)
            sequenceManager.transmitCommand(
                prefix, objCode, ts, objectKeys, objectValues, originUUID, destinations, objSeq, ackSequence, moderated, reply);
    }
    
    String getDefString() {
        return defString;
    }
    
    public CoreConsts.DataNodeInfo.StatusCode getStatus() {
        return getChannel() != null && channel.isConnected() ? !idle ? CoreConsts.DataNodeInfo.StatusCode.OK :
            CoreConsts.DataNodeInfo.StatusCode.IDLE : CoreConsts.DataNodeInfo.StatusCode.DISCONNECTED;
    }
    
    private void setAlerted(boolean alerted){
        if (getSequenceManager() != null)
            sequenceManager.setAlerted(alerted);
    }
    
    public String getRemoteAddress() {
        return getChannel() == null ? "?" : channel.getRemoteAddress();
    }
    
    synchronized boolean isFileChannel() {
        return channel == null ? false : channel.isFileChannel();
    }
    
    synchronized boolean isConnected() {
        return channel == null ? false : channel.isConnected();
    }
    
    void sendDisconnectionMail(){
        if (!alertByMe)
            return;
        setAlerted(true);
        Main.getInstance().sendNotificationMail(
            String.format(CoreConsts.AlertMail.DISCONNECT_ALERT, appName, getRemoteAddress()), null, ZDate.now(), true);
    }
    
    void sendIdleMail(boolean reading, ZDate idleSience){
        if (!alertByMe)
            return;
        setAlerted(true);
        Main.getInstance().sendNotificationMail(
            String.format(CoreConsts.AlertMail.IDLE_ALERT, appName, getRemoteAddress()), null, idleSience, true);
    }
    
    boolean nodeExistsInRemoteNodeList(UUID uuid) {
        return connectedNodes.contains(uuid);
    }
    
    void resetCounters() {
        info.reset();
    }

    protected void onIdle() {
        idle = true;
        Main.getLogger().logEvent(CoreConsts.DataChannel.IDLE_MESSAGE, getAppName(), getRemoteAddress(),
            CoreConsts.DataChannel.IDLE_READING);
        sendIdleMail(true, null);
    }

    public boolean transmit(String line, boolean urgent) {
        if (getChannel() == null)
            return false;
        if (line != null && channel.isConnected())
            info.updateOutputActivity(line.length());
        boolean result = channel.transmit(line, urgent);
        if (!result)
            Main.getLogger().logf(CoreConsts.DataChannel.SEND_ERROR, getRemoteAddress());
        return result;
    }

    private void onConnectFailed(Exception e) {
        Main.getLogger().logEvent(CoreConsts.DataChannel.CONNECTION_FAIL, getRemoteAddress(), e);
        Main.getInstance().onConnectFailed(getRemoteAddress(), e);
    }
    
    private void onError(Throwable e) {
        Main.getInstance().sendExceptionMail(e, String.format(CoreConsts.DataChannel.CHANNEL_ERROR, getRemoteAddress()), null, false);
    }
    
    private void onCompressAck() {
        logger.logNow(CoreConsts.DataChannel.COMPRESSION_ACK, getRemoteAddress());
    }

    private void onCompressReq() {
        logger.logNow(CoreConsts.DataChannel.COMPRESSION_REQ, getRemoteAddress());
    }

    private void onPong(long diff) {
        info.setClockDiff(diff);
    }

    /**
     * Send a login request. A consumer must send this request and get acknowledge. A producer sends it to let the other
     * side to activate a procedure for conflict alert.
     */
    private void login() {
        // No need for sending a login request if this is a file channel
        if (channel.isFileChannel()) {
            try {
                postLogin();
            } catch (Exception ex) {
                Main.getInstance().sendExceptionMail(ex, CoreConsts.DataChannel.FILE_LOGIN_EXCEPTION, null, true);
            }
            return;
        }
        if (!producer) {
            setLoginSent(ZDate.now());
            channelMonitor.activate();      // to trigger relogin on time
        }
        Main main = Main.getInstance();
        String loginLine = ZUtilities.concatAll(",", CoreConsts.DataChannel.LOGIN_COMMAND,
            producer ? CoreConsts.DataChannel.PRODUCER : CoreConsts.DataChannel.CONSUMER,
            main.getLoginParams(), compress,
            alertByOtherSize ? CoreConsts.DataChannel.ALERT : "", main.getAppUUID(), Serializer.escapeDelimiters(userID));
        transmit(loginLine, true);
        channel.ping();
    }
    
    @Override
    public void processLine(String line, ZDate ts) {
        try {
            idle = false;
            setLastInput(ZDate.now());
            setAlerted(false);
            if (line.isEmpty())
                return;
            String s[] = line.split(String.valueOf(Serializer.FIELD_SEPARATOR));
            switch (s[0]) {
            case CoreConsts.DataChannel.LOGIN_COMMAND:
                processLogin(s);
                break;
            case CoreConsts.DataChannel.ACK_LOGIN_COMMAND:
                processLoginAck(s);
                break;
            case CoreConsts.DataChannel.RESET_COMMAND:
                processReset(s);
                break;
            case CoreConsts.DataChannel.REMOVE_NODES:
                processRemoveNodes(s);
                break;
            case CoreConsts.DataChannel.ACK:
                Hub.getInstance().processAck(s[1], UUID.fromString(s[2]), UUID.fromString(s[3]), Integer.parseInt(s[4]), this);
                break;
            default:
                if (processCommand(line));
                    return;
            }
        } catch (Exception ex) {
            if (getChannel() == null || !getChannel().isConnected())
                return;
            info.updateErrors();
            Main.getInstance().sendExceptionMail(ex,
                String.format(CoreConsts.AlertMail.PARSING_ERROR_ALERT, appName, getRemoteAddress()),
                String.format(CoreConsts.AlertMail.WHEN_PARSING_LINE, line),
                false);
        }
        // called if processDataNodeCommand() is not called (that function has its own logging)
        if ((Main.getMyConfig().getLogMode(CoreConsts.DataChannel.LOGINPUT, logInput) & CoreConsts.DataChannel.LOG_RAW) > 0)
            logger.log("<- " + line, false);
        if (flushLogs)
            logger.flush();
        info.updateActivity(null, line.length());
    }

    @Override
    public void onEvent(Channel.EventCode eventCode, Object additionalInfo) {
        switch (eventCode) {
        case CONNECT_FAILED:
            onConnectFailed((Exception)additionalInfo);
            break;
        case COMPRESS_ACK:
            onCompressAck();
            break;
        case COMPRESS_REQ:
            onCompressReq();
            break;
        case PONG:
            onPong((long)additionalInfo);
            break;
        case ERROR:
            onError((Throwable)additionalInfo);
            break;
        case PHYSICAL_READ:
            info.updateCompressedInput((int)additionalInfo);
            break;
        }
    }

    @Override
    public void onDisconnect(String reason) {
        setLastInput(null);
        setLoginSent(null);
        setLoginReceived(false);
        Hub.getInstance().onChannelDisconnection(this, connectedNodes, null);
        Main.getLogger().logEvent(CoreConsts.DataChannel.DISCONNECT_CHANNEL, getRemoteAddress(), reason);
        if (getSequenceManager() != null)
            sequenceManager.onDisconnect();
        connectedNodes.clear();
        peerNodeMap.clear();
        // A disconnected server-side channel can be removed
        if (channelServer != null) {
            channelServer.removeChannel(getChannel());
            setChannel(null);
        }
    }

    @Override
    public void onConnect() {
        setConnectedSince(ZDate.now());
        Main.getLogger().logEvent(CoreConsts.DataChannel.CONNECTION_OK, getRemoteAddress());
        login();
    }

    @Override
    public void monitor() {}

    @Override
    public boolean isProducer() {
        return producer;
    }

    public void cleanup() {
        if (sequenceManager != null)
            sequenceManager.cleanup();
        channelMonitor.cleanup();
        if (logger != null)
            logger.cleanup();
        if (getChannel() != null)
            channel.cleanup();
    }
}