package org.spiderwiz.websocket.server;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import org.spiderwiz.plugins.EndpointConsts;
import org.spiderwiz.plugins.ServerWebsocket;
import org.spiderwiz.plugins.Websocket;

/**
 * Implement a WebSocket server endpoint.
 * @author Zvi 
 */
public class WebsocketServer extends Endpoint {
    private class MyMessageHandler implements MessageHandler.Partial<byte[]> {
        private final Session session;

        public MyMessageHandler(Session session) {
            this.session = session;
        }
        
        @Override
        public void onMessage(byte[] binaryMessage, boolean bln) {
            try {
                if (binaryMessage.length == 0)
                    return;
                Websocket channel = (Websocket)session.getUserProperties().get(EndpointConsts.ZOBJECT);
                if (channel != null)
                    channel.onMessage(binaryMessage, session);
                else {
                    session.close();
                }
            } catch (IOException ex) {
                onError(session, ex);
            }
        }
    }
    
    /**
     * Subclass Configurator to retrieve the remote IP address
     */
    private static class MyEndpointConfig extends ServerEndpointConfig.Configurator {
        private static MyEndpointConfig instance = null;
        static MyEndpointConfig getInstance() {
            if (instance == null)
                instance = new MyEndpointConfig();
            return instance;
        }
        
        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
            HttpSession httpSession = (HttpSession)request.getHttpSession();
            config.getUserProperties().put(
                EndpointConsts.SESSION_ADDRESS, httpSession.getAttribute(EndpointConsts.SESSION_ADDRESS));
            config.getUserProperties().put(
                EndpointConsts.IS_PRODUCER, EndpointConsts.isProducer(request.getRequestURI()));
        }
        
    }
    
    public static class MyConfig implements ServerApplicationConfig {
        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
            Set<ServerEndpointConfig> result = new HashSet<>();
            for (Class epClass : endpointClasses) {
                if (epClass.equals(WebsocketServer.class)) {
                    ServerEndpointConfig sec =
                        ServerEndpointConfig.Builder.create(epClass, EndpointConsts.WEBSOCKET_URI).
                            configurator(MyEndpointConfig.getInstance()).build();
                    result.add(sec);
                }
            }
            return result;
        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> set) {
            return Collections.EMPTY_SET;
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        try {
            ServerWebsocket server =
                ServerWebsocket.getInstance((boolean)config.getUserProperties().get(EndpointConsts.IS_PRODUCER));
            if (server == null)
                session.close();
            else {
                session.addMessageHandler(new MyMessageHandler(session));
                server.onOpen(session, (String)config.getUserProperties().get(EndpointConsts.SESSION_ADDRESS));
            }
        } catch (IOException ex) {
            onError(session, ex);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        try {
            Websocket channel = (Websocket)session.getUserProperties().get(EndpointConsts.ZOBJECT);
            if (channel != null)
                channel.onClose(session);
        } catch (IOException ex) {
            onError(session, ex);
        }
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        Websocket channel = (Websocket)session.getUserProperties().get(EndpointConsts.ZOBJECT);
        if (channel != null)
            channel.onError(session, throwable);
    }
}
