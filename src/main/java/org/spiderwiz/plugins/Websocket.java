package org.spiderwiz.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import javax.websocket.Session;
import org.spiderwiz.core.Channel;
import org.spiderwiz.core.Main;
import org.spiderwiz.endpoints.EndpointConsts;
import org.spiderwiz.endpoints.WebsocketClient;
import org.spiderwiz.zutils.ZBuffer;
import org.spiderwiz.zutils.ZDispenser;
import org.spiderwiz.zutils.ZTrigger;

/**
 * Implements WebSocket client
 * @author Zvi 
 */
public class Websocket extends Channel {
    // Implement WebSocket inputStream and outputStream
    private class WebsocketInputStream extends InputStream implements ZDispenser<byte[]>{
        private final ZTrigger waitForInput;
        private final ZTrigger waitForEmpty;
        private final ZBuffer<byte[]> buffer;
        private byte[] processedMessage;
        private int offset;
        private boolean aborted = false;

        public WebsocketInputStream() {
            waitForInput = new ZTrigger();
            waitForEmpty = new ZTrigger();
            buffer = new ZBuffer<>(this);
            buffer.setTimeout(0);
        }
        
        void execute() {
            buffer.execute();
        }

        private synchronized boolean isAborted() {
            return aborted;
        }

        private synchronized void setAborted(boolean aborted) {
            this.aborted = aborted;
        }

        void onMessage(byte[] binaryMessage) {
            buffer.add(binaryMessage);
        }

        @Override
        public int read() throws IOException {
            if (isAborted())
                return -1;
            int result;
            waitForInput.pause(0);
            if (offset >= processedMessage.length)
                return -1;
            result = processedMessage[offset++];
            if (offset >= processedMessage.length) {
                offset = 0;
                waitForEmpty.activate();
            }
            return result;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result;
            if (isAborted())
                return -1;
            waitForInput.pause(0);
            if (offset >= processedMessage.length)
                return -1;
            result = Math.min(len, processedMessage.length - offset);
            System.arraycopy(processedMessage, offset, b, off, result);
            offset += result;
            if (offset >= processedMessage.length) {
                offset = 0;
                waitForEmpty.activate();
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            setAborted(true);
            waitForInput.release();
            waitForEmpty.release();
            buffer.cleanup(false);
            super.close();
        }

        @Override
        public void dispense(byte[] binaryMessage, boolean flush) {
            processedMessage = binaryMessage;
            offset = 0;
            waitForInput.activate();
            waitForEmpty.pause(0);
        }

        @Override
        public void handleException(Exception ex) {
            Main.getInstance().sendExceptionMail(ex, PluginConsts.EXCEPTION_IN_ZBUFFER, null, false);
        }
    }
    
    private class WebsocketOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte)b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (session != null)
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(b, off, len));
        }
    }
    
    private String endPoint = null;
    private Session session = null;
    private WebsocketInputStream inputStream = null;
    private String remoteAddress = null;
    private boolean producer;

    /**
     * Called for a server-side socket
     * @param session 
     */
    public void setSession(Session session) {
        this.session = session;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }
    
    public boolean init(String uri, int type, int n){
        if (uri == null) {
            Main.getInstance().sendExceptionMail(null,
                String.format(PluginConsts.WebSocket.NO_URI, PluginConsts.TYPES[type], n), null, true);
            return false;
        }
        this.endPoint = addSchema(uri);
        return true;
    }

    private String addSchema(String uri) {
        return (uri.startsWith(PluginConsts.WebSocket.WEBSOCKET_SCHEMA) ? "" : PluginConsts.WebSocket.WEBSOCKET_SCHEMA + "//") + uri;
    }
    
    public void onMessage(byte[] binaryMessage, Session session) {
        if (inputStream == null)
            return;
        inputStream.onMessage(binaryMessage);
    }

    public void onClose(Session session) throws IOException {
        disconnect(null);
    }

    public void onError(Session session, Throwable throwable) {
        super.onError(throwable);
    }

    @Override
    protected boolean configure(Map<String, String> initParams, int type, int n) {
        producer = type == 1;
        return init(initParams.get(PluginConsts.WebSocket.WEBSOCKET), type, n);
    }

    @Override
    protected boolean open() throws Exception {
        if (session == null) {
            session = WebsocketClient.connectToServer(endPoint, producer);
            if (session != null)
                session.getUserProperties().put(EndpointConsts.ZOBJECT, this);
        }
        return session != null;
    }

    @Override
    protected void close() throws Exception {
        if (inputStream != null)
            inputStream.close();
        if (session != null) {
            session.close();
            session = null;
        }
    }

    @Override
    public String getRemoteAddress() {
        return remoteAddress != null ? PluginConsts.WebSocket.WEBSOCKET_SCHEMA + remoteAddress : endPoint;
    }

    @Override
    protected InputStream getInputStream() throws Exception {
        inputStream = new WebsocketInputStream();
        inputStream.execute();
        return inputStream;
    }

    @Override
    protected OutputStream getOutputStream() throws Exception {
        return session == null ? null : new WebsocketOutputStream();
    }

    @Override
    protected boolean dontReconnect() {
        return endPoint == null || super.dontReconnect();
    }

}
