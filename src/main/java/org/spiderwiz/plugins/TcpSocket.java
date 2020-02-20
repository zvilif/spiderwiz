package org.spiderwiz.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import org.spiderwiz.core.Channel;
import org.spiderwiz.core.Main;
import org.spiderwiz.zutils.ZUtilities;

/**
 * A class that handle tcp/ip communication
 *
 * @author Zvi 
 */
public class TcpSocket extends Channel{

    private Socket socket = null;
    private String ip;
    private int port;

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    /**
     * Called in a server-side socket to set the Socket value that was received from accept()
     * @param socket the socket object returned by ServerSocket.accept()
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
        ip = socket.getRemoteSocketAddress().toString().replace("/", "");
        int n = ip.indexOf(":");
        if (n > 0)
            ip = ip.substring(0, n);
        port = 0;
    }

    @Override
    protected boolean configure(Map<String, String> initParams, int type, int n) {
        return init(
            initParams.get(PluginConsts.TcpSocket.IP), ZUtilities.parseInt((initParams.get(PluginConsts.TcpSocket.PORT))), type, n);
    }

    private boolean init(String ip, int port, int type, int n) {
        if (ip == null || port <= 0) {
            Main.getInstance().sendExceptionMail(null,
                String.format(PluginConsts.TcpSocket.NO_TCP_PARAMS, PluginConsts.TYPES[type], n), null, true);
            return false;
        }
        this.ip = ip;
        this.port = port;
        socket = null;
        return true;
    }

    @Override
    protected boolean open() throws UnknownHostException, IOException {
        // create a socket only if this is a client side socket. A server side socket already exists
        if (port != 0) {
            socket = new Socket(InetAddress.getByName(ip).getHostAddress(), port);
        }
        return true;
    }

    @Override
    protected void close() throws IOException {
        if (socket != null)
            socket.close();
        socket = null;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return socket == null ? null : socket.getInputStream();
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return socket == null ? null : socket.getOutputStream();
    }

    @Override
    protected boolean dontReconnect() {
        return port == 0 || super.dontReconnect();
    }

    @Override
    public String getRemoteAddress() {
        return getIp() + (getPort() == 0 ? "" : ":" + getPort());
    }
}
