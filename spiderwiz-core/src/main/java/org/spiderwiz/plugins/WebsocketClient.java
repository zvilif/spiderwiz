package org.spiderwiz.plugins;

import java.io.IOException;
import java.net.URI;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * Implements WebSocket client endpoint
 * @author Zvi 
 */
public class WebsocketClient extends Endpoint{
    private class MyMessageHandler implements MessageHandler.Partial<byte[]> {
        private final Session session;

        public MyMessageHandler(Session session) {
            this.session = session;
        }
        
        @Override
        public void onMessage(byte[] binaryMessage, boolean bln) {
            if (binaryMessage.length == 0)
                return;
            Websocket socket = (Websocket)session.getUserProperties().get(EndpointConsts.ZOBJECT);
            if (socket != null)
                socket.onMessage(binaryMessage, session);
        }
    }
    
    public static Session connectToServer(String uri, boolean producer) throws DeploymentException, IOException {
        WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create().build();
        return webSocketContainer.connectToServer(
            WebsocketClient.class, config, URI.create(uri + EndpointConsts.getWebsocketUri(producer)));
    }
    
    @Override
    public void onOpen(Session session, EndpointConfig ec) {
        session.addMessageHandler(new MyMessageHandler(session));
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        try {
            Websocket socket = (Websocket)session.getUserProperties().get(EndpointConsts.ZOBJECT);
            if (socket != null)
                socket.onClose(session);
        } catch (IOException ex) {
            onError(session, ex);
        }
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        Websocket socket = (Websocket)session.getUserProperties().get(EndpointConsts.ZOBJECT);
        if (socket != null)
            socket.onError(session, throwable);
    }
}
