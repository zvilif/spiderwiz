package org.spiderwiz.plugins;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import org.spiderwiz.core.Channel;
import org.spiderwiz.core.Main;
import org.spiderwiz.core.ServerChannel;
import org.spiderwiz.zutils.ZUtilities;

/**
 * Base class for implementation of ServerSocket
 *
 * @author Zvi 
 */
public class TcpServerSocket extends ServerChannel {
    private ServerSocket serverSocket = null;
    private int port;

    @Override
    protected boolean configure(Map<String, String> configParams, int type, int n) {
        port = ZUtilities.parseInt(configParams.get(PluginConsts.TcpSocket.PORT));
        if (port == 0)
            Main.getInstance().sendExceptionMail(null, String.format(
                PluginConsts.TcpSocket.NO_TCP_PARAMS, PluginConsts.TYPES[type], n), null, true);
        return port > 0;
    }
    
    @Override
    public String getGateID() {
        return PluginConsts.TcpSocket.PORT_ID + port;
    }

    @Override
    protected void open() throws IOException {
        serverSocket = new ServerSocket(port);
    }

    @Override
    protected Channel accept() throws IOException {
        if (serverSocket == null)
            return null;
        Socket socket = serverSocket.accept();
        if (socket == null)
            return null;
        TcpSocket channel = new TcpSocket();
        channel.setSocket(socket);
        return channel;
    }

    @Override
    protected void close() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
    }
}
