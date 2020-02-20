package org.spiderwiz.core;

import java.util.Map;

/**
 * This is a base class for implementations of server channels. A <em>server channel</em> waits for requests arriving over the
 * network from implementations of {@link org.spiderwiz.core.Channel}. Based on the request, it creates the server side of the
 * channel and opens it for communication.
 * <p>
 * <em>Spiderwiz</em> framework comes out of the box with implementations of predefined servers, such as a TCP/IP
 * server and a WebSockets server. The type of the server to use is configured in the
 * <a href="doc-files/config.html">application's configuration file</a>. These are all implemented by extending the
 * {@code ServerChannel} class.
 * <p>
 * In addition to the predefined servers users can write plugins with their own custom server implementations. You develop a
 * custom server by extending {@code ServerChannel}, and use your implementation class by assigning its fully qualified name to
 * the <a href="doc-files/config.html#ServerClassTag">class</a> tag in the configuration file.
 * <p>
 * All implementations of this class shall implement its {@link #configure(java.util.Map, int, int) configure()} method to configure
 * the server, {@link #open()} to activate its mechanism, {@link #accept()} to listen to and establish connections and
 * {@link #close()} to close the server when its not needed any more. Also implement {@link #getGateID()} to identify the server
 * for logging and administration purposes.
  * <p>
 * <a name=example></a>
 * Here is a full example taken from Spiderwiz implementation of a TCP/IP server channel:
     * <pre>
public class TcpServerSocket extends ServerChannel {
    private ServerSocket serverSocket = null;
    private int port;

    &#64;Override
    protected boolean configure(Map&lt;String, String&gt; configParams, int type, int n) {
        final String NO_TCP_PARAMS = "No port number specified for %1$s-%2$d";
        final String TYPES[] = {"import", "producer", "consumer"};
        port = ZUtilities.parseInt("port");
        if (port == 0)
            Main.getInstance().sendExceptionMail(null, String.format(NO_TCP_PARAMS, TYPES[type], n), null, true);
        return port &gt; 0;
    }
    
    &#64;Override
    public String getGateID() {
        return "port:" + port;
    }

    &#64;Override
    protected void open() throws IOException {
        serverSocket = new ServerSocket(port);
    }

    &#64;Override
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

    &#64;Override
    protected void close() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
    }
}</pre>
* @see Channel
*/
public abstract class ServerChannel implements Runnable {
    private boolean abort = false;
    private Thread myThread = null;
    private ServerChannelHandler handler;
    
    enum EventCode {LISTENING, LISTENING_FAILED, ACCEPTING_FAILED, STOPPED_LISTENING};

    /**
     * Configures the server.
     * <p>
     * An abstract method that you must implement to configure a server channel from parameters specified by either
     * <code>[producer&nbsp;server-<em>n</em>]</code> or <code>[consumer&nbsp;server-<em>n</em>]</code> properties in the
     * <a href="doc-files/config.html">application's configuration file</a>. The values of these properties must be a list of pairs
     * <em>key=value</em> concatenated by a semicolon. For instance, let's say you implement a custom TCP/IP server socket, then its
     * configuration may look something like:
     * <pre>
     class=com.mydomain.myplugins.MyTcpIpServer;port=31415
     </pre>
     * The framework will then instantiate your class {@code com.mydomain.myplugins.MyTcpIpServer} and call its {@code configure()}
     * method with a map in {@code configParams} containing the following pairs (order not guarantee):
     * <pre>
     class=com.mydomain.myplugins.MyTcpIpServer
     port=31415
     </pre>
     * @param configParams      a map of key=value configuration parameters.
     * @param type              type of server - 1:producer, 2:consumer.
     * @param n                 the <em>n</em> value of <em>producer&nbsp;server-n</em> or <em>consumer&nbsp;server-n</em>.
     * @return true if and only if configuration is successful.
     */
    protected abstract boolean configure(Map<String, String> configParams, int type, int n);
    
    /**
     * Opens the server for arriving connection requests.
     * <p>
     * An abstract method that you must implement to start listening to clients.
     * @throws java.lang.Exception
     */
    protected abstract void open() throws Exception;

    /**
     * Listens for a connection to be made to this server and accepts it.
     * <p>
     * An abstract method that you must implement to listen for a connection to be made to this server and accept it. Your
     * implementation should block until a connection is made. When it is, you need to create an instance of your
     * {@link org.spiderwiz.core.Channel} implementation that relates to this server and return it.
     * @return a Channel object that represents the established connection, or null if this could not be done.
     * @throws java.lang.Exception
     */
    protected abstract Channel accept() throws Exception;
    
    /**
     * Closes the server.
     * <p>
     * An abstract method that you must implement to to stop listening to clients and close the server.
     * @throws java.lang.Exception
     */
    protected abstract void close() throws Exception;

    /**
     * Returns the identification of this server.
     * <p>
     * An abstract method that you must implement to return server identification, used for logging and administration purposes.
     * @return the identification of this server.
     */
    public abstract String getGateID();

    ServerChannelHandler getHandler() {
        return handler;
    }
    
    /**
     * Initialize the object
     * @param handler           A ZServerChannelHandler implementation that handles object events.
     * @return                  true if initializing succeeded.
     */
    boolean init(ServerChannelHandler handler) {
        try {
            this.handler = handler;
            open(); 
            handler.onEvent(EventCode.LISTENING, getGateID(), null);
        } catch (Exception ex) {
            handler.onEvent(EventCode.LISTENING_FAILED, getGateID(), ex);
            return false;
        }
        return true;
    }
    
    synchronized void execute () {
        myThread = new Thread(this);
        myThread.setDaemon(true);
        myThread.start();
    }
    
    /**
     * Implementation of {@link Runnable#run()}.
     * <p>
     * Used internally by the framework. Do not call or override.
     */
    @Override
    public final void run() {
        Channel channel;
        while (!abort) {
            try {
                channel = accept();
                if (channel != null) {
                    if (abort)
                        channel.close();
                    else
                        handler.onConnect(channel);
                }
            } catch (Exception ex) {
                if (!abort)
                    handler.onEvent(EventCode.ACCEPTING_FAILED, getGateID(), ex);
            }
        }
    }
    
    /**
     * Cleans up object resources on exit.
     * <p>
     * You may override this method if you need to do some cleanup tasks when the object is not used any more. Do not forget to
     * call {@code super.cleanup()}.
     */
    public synchronized void cleanup() {
        try {
            this.abort = true;
            if (myThread != null)
                myThread.interrupt();
            close();
            if (handler != null)
                handler.onEvent(EventCode.STOPPED_LISTENING, getGateID(), null);
        } catch (Exception e) {
        }
    }
}
