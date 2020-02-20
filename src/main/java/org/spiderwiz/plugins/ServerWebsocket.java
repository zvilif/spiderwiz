package org.spiderwiz.plugins;

import java.io.IOException;
import java.util.Map;
import javax.websocket.Session;
import org.spiderwiz.core.Channel;
import org.spiderwiz.core.Main;
import org.spiderwiz.core.ServerChannel;
import org.spiderwiz.endpoints.EndpointConsts;
import org.spiderwiz.endpoints.WebsocketServer;
import org.spiderwiz.zutils.ZBuffer;

/**
 * Implement a WebSocket server. Activated from org.spiderwiz.annotatedEndpoints.WebsocketServer
 * @author Zvi 
 */
public final class ServerWebsocket extends ServerChannel {
    private static final ServerWebsocket instances[] = {null, null};
    private final ZBuffer<Session> openSessionBuffer;       // Buffer onOpen requests
    private boolean abort = false;
    private boolean producer = false;

    public ServerWebsocket() {
        openSessionBuffer = new ZBuffer<>(null);
        openSessionBuffer.setTimeout(0);
    }

    public static synchronized ServerWebsocket getInstance(boolean producer) {
        return instances[producer ? 1 : 0];
    }

    public static synchronized void setInstance(ServerWebsocket instance, boolean producer) {
        ServerWebsocket.instances[producer ? 1 : 0] = instance;
    }

    private synchronized boolean isAbort() {
        return abort;
    }

    private synchronized void setAbort(boolean abort) {
        this.abort = abort;
    }

    @Override
    protected boolean configure(Map<String, String> configParams, int type, int n) {
        // The object runs as singleton (one instance for producers and one for consumers)
        boolean prod = type == 1;
        if (getInstance(prod) != null) {
            Main.getInstance().sendExceptionMail(null, String.format(
                PluginConsts.WebSocket.DOUBLE_SERVER, PluginConsts.TYPES[type], n), null, true);
            return false;
        }
        producer = prod;
        setInstance(this, prod);
        return true;
    }

    /**
     * Called when a new session is opened by a client
     * @param session           The Session object
     * @param remoteAddress     IP address of the client
     * @throws IOException
     */
    public void onOpen(Session session, String remoteAddress) throws IOException {
        // Create a Websocket object and store it in the session
        Websocket socket = new Websocket();
        socket.setRemoteAddress(remoteAddress);
        session.getUserProperties().put(EndpointConsts.ZOBJECT, socket);
        // Push the session to the end of the opened sessions buffer and store a trigger to be used by subsequent
        // operations on the session to pause them until the session is pulled from the buffer and full open procedure
        // is excuted.
        openSessionBuffer.add(session);
    }
 
    @Override
    public String getGateID() {
        return WebsocketServer.getGateID();
    }

    @Override
    protected void open() {
        openSessionBuffer.execute();
    }

    @Override
    protected Channel accept() throws Exception {
        if (isAbort())
            return null;
        Session session = openSessionBuffer.pull(true);
        if (session == null)
            return null;
        Websocket channel = (Websocket)session.getUserProperties().get(EndpointConsts.ZOBJECT);
        if (channel == null)
            return null;
        channel.setSession(session);
        return channel;
    }

    @Override
    protected void close() throws Exception {
        setAbort(true);
        // Clear the opened session buffer
        Session session;
        while ((session = openSessionBuffer.pull(false)) != null) {
            session.close();
        }
        openSessionBuffer.cleanup(true);
    }

    @Override
    public synchronized void cleanup() {
        super.cleanup();
        setInstance(null, producer);
    }
}
