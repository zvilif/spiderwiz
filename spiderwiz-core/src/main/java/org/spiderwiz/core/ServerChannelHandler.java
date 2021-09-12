package org.spiderwiz.core;

import java.util.ArrayList;
import org.spiderwiz.plugins.PluginConsts;
import org.spiderwiz.plugins.ServerWebsocket;
import org.spiderwiz.plugins.TcpServerSocket;
import org.spiderwiz.zutils.ZDictionary;
import org.spiderwiz.zutils.ZLog;

/**
 * Acts as a server that accepts all kind of connections from clients.
 * @author zvil
 */
final class ServerChannelHandler {
    private final DataNode initiatingNode;
    private final boolean producer;
    private final ArrayList<Channel> channels;
    private final ZLog logger;
    private boolean aborted = false;
    private String defString = null;
    private ServerChannel server = null;
    private int logInput = CoreConsts.DataChannel.LOG_UNDEFINED;
    private int logOutput = CoreConsts.DataChannel.LOG_UNDEFINED;
    private boolean flushLogs = false;
    private boolean gateway = false;
    private int compress;
    private long pingRate;

    public ServerChannelHandler(DataNode initiatingNode) {
        this.initiatingNode = initiatingNode;
        producer = initiatingNode.isProducer();
        logger = Main.getLogger();
        channels = new ArrayList<>();
    }

    private synchronized ServerChannel getServer() {
        return server;
    }

    private synchronized void setServer(ServerChannel server) {
        this.server = server;
    }

    /**
     * Add a newly connected channel
     * @param channel 
     */
    synchronized void addChannel(Channel channel) {
        if (!aborted)
            channels.add(channel);
    }
    
    /**
     * Remove a channel that was disconnected
     * @param channel 
     */
    synchronized void removeChannel(Channel channel) {
        if (channel != null && !aborted)
            channels.remove(channel);
    }

    String getDefString() {
        return defString;
    }
    
    /**
     * Initialize and start the server
     * @param defString     connection information as appears in the configuration file
     * @return              true if server was established successfully.
     */
    boolean init(String defString, int n) {
        try {
            this.defString = defString;
            String customClass;
            setServer(null);
            ZDictionary configParams = ZDictionary.parseParameterList(defString);
            if ((customClass = configParams.get(CoreConsts.Channel.CLASS)) != null)
                try {
                    server = (ServerChannel)Class.forName(customClass).getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    String msg = String.format(CoreConsts.AlertMail.SERVER_CLASS_FAILED, customClass, ex.toString());
                    Main.getLogger().logEvent(msg);
                    Main.getInstance().sendNotificationMail(msg, null, null, true);
                    return false;
                }
            else if (configParams.get(PluginConsts.TcpSocket.PORT) != null)
                server = new TcpServerSocket();
            else if (configParams.containsKey(PluginConsts.WebSocket.WEBSOCKET))
                server = new ServerWebsocket();
            logInput = Main.getMyConfig().interpretLogParam(configParams, CoreConsts.DataChannel.LOGINPUT);
            logOutput = Main.getMyConfig().interpretLogParam(configParams, CoreConsts.DataChannel.LOGOUTPUT);
            flushLogs = configParams.containsKey(CoreConsts.DataChannel.FLUSHLOGS);
            gateway = configParams.containsKey(CoreConsts.DataChannel.GATEWAY);
            compress = Main.getMyConfig().interpretCompressParam(configParams, DataHandler.FULL_COMPRESSION);
            pingRate = MyConfig.getPingRate(configParams);
            if (server != null && server.configure(configParams, producer ? 1 : 2, n)) {
                server.init(this);
                server.execute();
                return true;
            }
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, CoreConsts.AlertMail.EXCEPTION_INIT,
                String.format(CoreConsts.AlertMail.PROPERTY_STRING, defString), false);
        }
        return false;
    }
    
    /**
     * Close all channels and clean up the list
     */
    synchronized void cleanup() {
        aborted = true;
        channels.forEach((channel) -> {
            channel.cleanup();
        });
        channels.clear();
        if (server != null)
            server.cleanup();
    }
    
    public void onConnect(Channel channel) {
        try {
            DataHandler dataChannel = new DataHandler(initiatingNode);
            addChannel(channel);
            initiatingNode.addChannel(dataChannel);
            dataChannel.initServerChannel(channel, this);
            dataChannel.setLogInput(logInput);
            dataChannel.setLogOutput(logOutput);
            dataChannel.setFlushLogs(flushLogs);
            dataChannel.setGateway(gateway);
            dataChannel.setCompress(compress);
            dataChannel.setPingRate(pingRate);
            channel.execute();
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, String.format(CoreConsts.AlertMail.WHILE_CONNECTING, channel.getRemoteAddress()),
                null, false);
        }
    }

    public void onEvent(ServerChannel.EventCode eventCode, String gateID, Object additionalInfo) {
        switch (eventCode) {
        case LISTENING:
            logger.logEvent(producer ? CoreConsts.DataNode.PRODUCER_LISTENING : 
                CoreConsts.DataNode.CONSUMER_LISTENING, gateID);
            break;
        case LISTENING_FAILED:
            logger.logEvent(producer ? CoreConsts.DataNode.PRODUCER_LISTENING_FAILED : 
                CoreConsts.DataNode.CONSUMER_LISTENING_FAILED, gateID, additionalInfo.toString());
            break;
        case ACCEPTING_FAILED:
            Main.getInstance().sendExceptionMail(
                (Exception)additionalInfo, String.format(CoreConsts.AlertMail.WHILE_CONNECTING, gateID), null, false);
            break;
        case STOPPED_LISTENING:
            logger.logEvent(producer ? CoreConsts.DataNode.PRODUCER_STOPPED_LISTENING : 
                CoreConsts.DataNode.CONSUMER_STOPPED_LISTENING, gateID);
            break;
        }
    }

    public boolean isProducer() {
        return producer;
    }
}
