package org.spiderwiz.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;
import org.spiderwiz.zutils.ZBuffer;
import org.spiderwiz.zutils.ZDate;

/**
 * A base class for classes that handle data communication.
 * <p>
 * <em>Spiderwiz</em> framework comes out of the box with implementations of predefined communication channels, such as TCP/IP
 * sockets, WebSockets and disc files (the latter is helpful for offline debugging). The type of the channel to use is configured in the
 * <a href="doc-files/config.html">application's configuration file</a>. These are all implemented by extending the {@code Channel}
 * class.
 * <p>
 * In addition to the predefined channels users can write plugins with their own custom channel implementation. You develop a custom
 * channel by extending {@code Channel}, and use your implementation class by assigning its fully qualified name to the
 * <a href="doc-files/config.html#ClassTag">class</a> tag in the configuration file.
 * <p>
 * There are two ways to use this base class for implementing a custom channel. The first relies on its builtin mechanism of using
 * {@link java.io.InputStream} and {@link java.io.OutputStream}. If you go this way all you have to do is provide your extensions to
 * these classes in {@link #getInputStream()} and {@link #getOutputStream()}. The other way is to provide your own i/o access code.
 * In this case you will need to override {@link #open()}, {@link #readLine()}, {@link #writeLine(java.lang.String) writeLine()},
 * {@link #close()} and maybe also {@link #flush()}.
 * <p>
 * In many cases channels are used in a <em>client-server architecture</em>, in which the client side of the channel applies
 * for a connection to a configured server address, while the server accepts the requests and opens the server side of the channel.
 * The {@code Channel} class should be used for both ends of the connection. At the client side, the channel is configured by
 * {@link #configure(java.util.Map, int, int) configure()} method that you must implement.
 * <p>
 * If you implement a custom {@code Channel} in client-server architecture, you shall also provide a custom implementation of
 * {@link org.spiderwiz.core.ServerChannel} that will accept requests from your client implementation and open its server side.
 * <p>
 * Here <a id=example></a>is a full example taken from Spiderwiz implementation of a TCP/IP socket:
     * <pre>
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import org.spiderwiz.core.Channel;
import org.spiderwiz.core.Main;
import org.spiderwiz.zutils.ZUtilities;

public class TcpSocket extends Channel{
    private Socket socket = null;
    private String ip;
    private int port;

    &#64;Override
    protected boolean configure(Map&lt;String, String&gt; initParams, int type, int n) {
        return init(
            initParams.get("ip"), ZUtilities.parseInt((initParams.get("port"))), type, n);
    }

    private boolean init(String ip, int port, int type, int n) {
        final String NO_TCP_PARAMS = "No IP address or port number specified for %1$s-%2$d";
        final String TYPES[] = {"import", "producer", "consumer"};
        if (ip == null || port == 0) {
            Main.getInstance().sendExceptionMail(null, String.format(NO_TCP_PARAMS, TYPES[type], n), null, true);
            return false;
        }
        this.ip = ip;
        this.port = port;
        return true;
    }

    &#64;Override
    protected boolean open() throws UnknownHostException, IOException {
        socket = new Socket(InetAddress.getByName(ip).getHostAddress(), port);
        return true;
    }

    &#64;Override
    protected void close() throws IOException {
        if (socket != null)
            socket.close();
        socket = null;
    }

    &#64;Override
    protected InputStream getInputStream() throws IOException {
        return socket == null ? null : socket.getInputStream();
    }

    &#64;Override
    protected OutputStream getOutputStream() throws IOException {
        return socket == null ? null : socket.getOutputStream();
    }

    &#64;Override
    public String getRemoteAddress() {
        return ip + (port == 0 ? "" : ":" + getPort());
    }
 }</pre>
 * @see ServerChannel
 */
public abstract class Channel extends Dispenser<String> implements Runnable {
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final long MAX_FLUSH_INTERVAL = ZDate.SECOND;
    private static final long MIN_FLUSH_INTERVAL = 20;
    private static final long MED_FLUSH_INTERVAL = 40;
    private static final int LOW_ACTIVITY = 10;
    private static final int MED_ACTIVITY = 25;
    private static final int HIGH_ACTIVITY = 50;
    private static final int COUNT_INTERVAL = 30;

    private boolean abort = false;
    private ZDate lastFlush = null;
    private Thread myThread = null;
    private boolean connected = false, reset = false, channelOpen = false;
    private final ZBuffer<String> buffer;
    private final Object sync = new Object();
    private long pongOffset = 0;
    private ZDate lastMonitor = null;
    private ZDate lastPing;
    private String bufferFile = null;
    private ZipWriter zipOut = null;
    private ZipReader zipIn = null;
    private ChannelHandler handler = null;
    private BufferedReader in = null;
    private PrintWriter out = null;
    private long monitorInterval = ZDate.MINUTE;
    private String characterSet = DEFAULT_CHARSET;
    private int reconnectWait = CoreConsts.Channel.DEFAULT_RECONNECT_WAIT;
    private boolean importing = false;
    private int writeCount = 0;
    private ZDate lastCount;
    private long flushInterval = MIN_FLUSH_INTERVAL;
    
    enum EventCode
        {CONNECT_FAILED, SEND, COMPRESS_ACK, COMPRESS_REQ, PING, PONG, ERROR, PHYSICAL_READ, PHYSICAL_WRITE};
    private static final int BUFFER_CAPACITY = 20000;

    /**
     * Class constructor.
     */
    public Channel() {
        // Use a threaded buffer to write to the channel
        buffer = new ZBuffer<>(this);
        buffer.setEmptyOnFull(true);
        buffer.setMaxCapacity(BUFFER_CAPACITY);
    }

    /**
     * Configures the channel.
     * <p>
     * An abstract method that you must implement to configure a client channel from parameters specified by
     * <code>producer-<em>n</em></code>, <code>consumer-<em>n</em></code> or <code>import-<em>n</em></code> properties in the
     * <a href="doc-files/config.html">application's configuration file</a>. The values of these properties must be a list of pairs
     * <em>key=value</em> concatenated by a semicolon. For instance, let's say you implement a custom TCP/IP socket, then its
     * configuration may look something like:
     * <pre>
     class=com.mydomain.myplugins.MyTcpIp;ip=192.185.6.24;port=31415
     </pre>
     * The framework will then instantiate your class {@code com.mydomain.myplugins.MyTcpIp} and call its {@code configure()} method
     * with a map in {@code configParams} containing the following pairs (order not guarantee):
     * <pre>
     class=com.mydomain.myplugins.MyTcpIp
     ip=192.185.6.24
     port=31415
     </pre>
     * @param configParams      a map of key=value configuration parameters.
     * @param type              type of channel - 0: import, 1:producer, 2:consumer.
     * @param n                 the <em>n</em> value of <em>import-n</em>, <em>producer-n</em> or <em>consumer-n</em>.
     * @return true if and only if configuration is successful.
     */
    protected abstract boolean configure(Map<String, String> configParams, int type, int n);

    /**
     * Returns the channel input stream.
     * <p>
     * An abstract method you must implement to provide the input stream of the channel. If you do, this is the only method you need
     * to implement for that purpose. Alternatively you can return {@code null} in this method and implement direct input by overriding
     * {@link #readLine()}, {@link #open()} and {@link #close()}.
     * @return  the {@link java.io.InputStream} object used by your implementation, or null if you implement direct input.
     * @throws java.lang.Exception
     */
    protected abstract InputStream getInputStream() throws Exception;
    
    /**
     * Returns the channel output stream.
     * <p>
     * An abstract method you must implement to provide the output stream of the channel. If you do, this is the only method you need
     * to implement for that purpose. Alternatively you can return {@code null} in this method and implement direct output by overriding
     * {@link #writeLine(java.lang.String) writeLine()}, {@link #open()} and {@link #close()}.
     * @return  the {@link java.io.OutputStream} object used by your implementation, or null if you implement direct input.
     * @throws java.lang.Exception
     */
    protected abstract OutputStream getOutputStream() throws Exception;
    
    /**
     * Opens the channel for communication.
     * <p>
     * Override this method to provide code for opening the channel, if needed. This method is called before {@link #getInputStream()}
     * and {@link #getOutputStream()} are called. The default implementation of this method does nothing and returns {@code true}.
     * <p>
     * See the <a href=#example>example</a> in the class description.
     * @return true if and only if the operation is successful.
     * @throws java.lang.Exception
     */
    protected boolean open() throws Exception {return true;}
    
    /**
     * Reads a line of text.
     * <p>
     * Override this method if and only if your implementation of {@link #getInputStream()} returns {@code null}.
     * @return  the read line, not including any line-termination characters, or null if the end of the data has been reached.
     * @throws java.io.IOException
     */
    protected String readLine() throws IOException {
        return in == null ? null : in.readLine();
    }
    
    /**
     * Writes a line of text.
     * <p>
     * Override this method if and only if your implementation of {@link #getOutputStream()} returns {@code null}.
     * @param line  the line to be written.
     */
    protected void writeLine(String line) {
        if (out != null)
            out.println(line);
    }
    
    /**
     * Flushes any remaining data in the output buffer.
     * <p>
     * Override this method if and only if your implementation of {@link #getOutputStream()} returns {@code null}.
     */
    protected void flush() {
        if (out != null)
            out.flush();
    }
  
    /**
     * Closes this channel.
     * <p>
     * Override this method to provide code for closing the channel, if needed. This method is called after the streams
     * returned by {@link #getInputStream()} {@link #getOutputStream()} are closed. The default implementation of this method
     * does nothing.
     * <p>
     * See the <a href=#example>example</a> in the class description.
     * @throws java.lang.Exception
     */
    protected void close() throws Exception {}
    
    /**
     * Returns the address of the endpoint this channel is connected to, or null if it is unknown.
     * <p>
     * This is an abstract method that you must implement to provide the endpoint address of this channel, for instance a remote IP
     * address.
     * <p>
     * See the <a href=#example>example</a> in the class description.
     * @return an address representing the remote endpoint of this channel, or null if it is unknown.
     */
    public abstract String getRemoteAddress();

    /**
     * Returns {@code true} if this channel reads from a disk file.
     * <p>
     * Override this method if this is a file channel. The default implementation returns {@code false}.
     * @return  true if any only if this is a file channel.
     */
    public boolean isFileChannel() {return false;}

    /**
     * Returns {@code true} if {@link java.util.zip ZIP} algorithm shall be used to compress the data on this channel.
     * <p>
     * This method is relevant only if you implement {@link #getInputStream()} and {@link #getOutputStream()} to return
     * non-null values. If the method returns {@code true} data written to the output stream will be compressed before, and
     * data read from the input stream will be decompressed after. The method has no effect if those methods return {@code null}.
     * <p>
     * The default implementation returns {@code true} if any only if this is not a file channel. Override the method to provide
     * a different implementation.
     * @return  true if any only if ZIP algorithm shall be used to compress the data on this channel.
     */
    protected boolean isCompressable() {return !isFileChannel();}
    
    /**
     * Returns {@code true} if this is a client channel that shall not try to reconnect automatically if disconnected.
     * <p>
     * If this method returns {@code false}, the framework will try repeatedly to reconnect the channel whenever it is disconnected.
     * The method affects only client channels. If a server channel is disconnected, the server would just
     * wait for the client to reconnect.
     * <p>
     * By default the method returns the value returned by {@link #isFileChannel()}. Override it to provide a different behavior.
     * @return  true if this is a client channel that shall not try to reconnect automatically if disconnected.
     */
    protected boolean dontReconnect() {return isFileChannel();}
    
    /**
     * Returns true when it's time to flush
     */
    private boolean timeToFlush() {
        // Calcualte new flush interval if necessary
        ZDate now = ZDate.now();
        if (now.diff(lastCount) >= COUNT_INTERVAL * ZDate.SECOND) {
            int writePerSecond = writeCount / COUNT_INTERVAL;
            long newFlushInterval =
                writePerSecond <= LOW_ACTIVITY ? MIN_FLUSH_INTERVAL :
                writePerSecond <= MED_ACTIVITY ? MED_FLUSH_INTERVAL :
                writePerSecond >= HIGH_ACTIVITY ? MAX_FLUSH_INTERVAL :
                (long)((writePerSecond - MED_ACTIVITY) / (float)(HIGH_ACTIVITY - MED_ACTIVITY) *
                (MAX_FLUSH_INTERVAL - MED_FLUSH_INTERVAL) + MED_FLUSH_INTERVAL);
            if (newFlushInterval != flushInterval) {
                flushInterval = newFlushInterval;
                buffer.setTimeout(flushInterval);
            }
            lastCount = now;
            writePerSecond = 0;
        }
        
        // Check if flush interval has arrived
        return now.diff(lastFlush) >= flushInterval;
    }

    /**
     * Returns the time interval that the framework writes an empty string to the channel to keep it alive.
     * <p>
     * There are scenarios in which if no output is written to a communication channel for a certain amount of time the system
     * treats the channel as idle and disconnects it. If your implementation involves this scenario, you can use this method to set
     * the time interval that the framework checks whether the channel is idle and sends an empty string to it to keep it alive.
     * <p>
     * The default value that the method returns is one minute. Override it to provide a different value in milliseconds, or zero if
     * "keep alive" is not needed.
     * @return the time interval in milliseconds that the framework will write an empty string to the channel to keep it alive, or
     * zero if this is not needed.
     */
    protected long getKeepAliveInterval() {
        return isFileChannel() ? 0 : ZDate.MINUTE;
    }

    /**
     * Returns {@code true} if the channel should be considered connected after the end of data has been reached.
     * <p>
     * In some cases there is a need to consider the channel connected even though reading from it has reached the end of data.
     * For instance,
     * if this is a file channel that uses one file for input and one file for output, even though the end of data from the input file
     * has been reached the channel should stay connected so that output to the output file shall continue to be written.
     * <p>
     * By default the method returns the value returned from {@link #isFileChannel()}. Override it to provide a different behavior.
     * @return  true if and only if the channel should be considered connected after the end of data has been reached.
     */
    protected boolean keepAliveOnEndOfData() {return isFileChannel();}

    /**
     * Disconnects the channel.
     * <p>
     * Call this method if your implementation detects a disconnection state that cannot be detected by the framework. The
     * {@code reason} parameter should be a free textual description of the reason that caused the disconnection, used for logging
     * purposes. A {@code null} value will yield the generic text {@code "Channel closed by other peer"}.
     * @param reason    a textual description of the reason that caused the disconnection.
     */
    public final void disconnect(String reason) {
        synchronized (sync) {
            if (reason == null)
                reason = abort ? CoreConsts.Channel.CLOSED_BY_US : CoreConsts.Channel.CLOSED_BY_OTHER;
            if (!connected) {
                return;
            }
            connected = false;
            closeChannel();
        }
        if (buffer != null) {
            buffer.cleanup(false);
        }
        handler.onDisconnect(reason);
    }
    
    /**
     * Reports an exception.
     * <p>
     * Call this method if your implementation catches an exception that could not be caught by the framework.
     * @param ex    the exception object caught by your implementation.
     */
    protected final void onError(Throwable ex) {
        if (handler != null)
            handler.onEvent(EventCode.ERROR, ex);
    }
    
    /**
     * Implementation of {@link Runnable#run()}.
     * <p>
     * Used internally by the framework. Do not call or override.
     */
    @Override
    public final void run() {
        String s[];
        try {
            while (!abort) {
                String reason = null;
                if (connect()) {
                    try {
                        while (reason == null) {
                            String scLine = readLine();
                            if (abort) {
                                reason = CoreConsts.Channel.CLOSED_BY_US;
                            } else if (!connected || scLine == null) {
                                reason = CoreConsts.Channel.CLOSED_BY_OTHER;
                            } else {
                                // Handle the line
                                if (scLine.startsWith(CoreConsts.Channel.PONG_COMMAND)) {
                                    s = scLine.split(",");
                                    ZDate pongTime = new ZDate(s[1], ZDate.FULL_TIMESTAMP);
                                    ZDate pingTime = new ZDate(s[2], ZDate.FULL_TIMESTAMP);
                                    long roundTrip = pingTime.elapsed();
                                    handler.onEvent(EventCode.PONG, pingTime.diff(pongTime) + roundTrip / 2);
                                } else if (scLine.startsWith(CoreConsts.Channel.PING_COMMAND)) {
                                    s = scLine.split(",");
                                    transmit(CoreConsts.Channel.PONG_COMMAND + "," +
                                            ZDate.now().add(getPongOffset()).formatFullTimestamp() + "," +
                                            s[1], true);
                                    handler.onEvent(EventCode.PING, null);
                                } else if (scLine.equals(CoreConsts.Channel.COMPRESS_REQ) && zipIn != null) {
                                    zipIn.startDecompression();
                                    transmit(CoreConsts.Channel.COMPRESS_ACK, true);
                                    handler.onEvent(EventCode.COMPRESS_REQ, null);
                                } else if (scLine.equals(CoreConsts.Channel.COMPRESS_ACK) && zipOut != null) {
                                    zipOut.startCompression();
                                    handler.onEvent(EventCode.COMPRESS_ACK, sync);
                                } else {
                                    handler.processLine(scLine, null);
                                }
                            }
                        }
                    } catch (Exception e) {
                        reason = e.toString();
                    }
                    if (!keepAliveOnEndOfData())  // do no disconnect if the 'socket' is a file whose end has been reached.
                        disconnect(reason);
                }
                // A server side socket is executed only once without attempts to reconnect
                if (dontReconnect() || getReconnectWait() < 0) {
                    abort = true;
                }
                // if connection failed or socket was disconnected by the other side wait set amount of time before reattempt
                if (!abort & !reset) {
                    try {
                        waitForReattempt();
                    } catch (InterruptedException e) {
                    }
                }
                reset = false;
            }
        } catch (Exception e) {
            disconnect(e.getMessage());
        }
    }

    /**
     * Implementation of {@link org.spiderwiz.zutils.ZDispenser#dispense(java.lang.Object, boolean) ZDispenser.dispense()}.
     * <p>
     * Used internally by the framework. Do not call or override.
     * @param line      Line to dispense.
     * @param flush     Flush buffered data if set to {@code true}.
     */
    @Override
    public final void dispense(String line, boolean flush) {
        send(line, flush);
    }

    /**
     * Return the time in seconds after which a failed connection attempt will be repeated.
     * @return  time in seconds. The default is 60 seconds.
     */
    private synchronized int getReconnectWait() {
        return reconnectWait;
    }

    synchronized void setReconnectWait(int reconnectWait) {
        this.reconnectWait = reconnectWait;
    }

    private String getCharacterSet() {
        return characterSet;
    }

    void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    /**
     * Call this method to define monitoring interval time in milliseconds. Zero values means that monitoring is not
     * necessary. The default is one-minute interval.
     * @param monitorInterval   time in millisecond or zero.
     */
    synchronized void setMonitorInterval(long monitorInterval) {
        this.monitorInterval = monitorInterval;
    }

    private synchronized long getMonitorInterval() {
        return monitorInterval;
    }

    final boolean isConnected() {
        return connected;
    }

    final ChannelHandler getHandler() {
        return handler;
    }

    Channel init(ChannelHandler handler) {
        this.handler = handler;
        return this;
    }

    void setBufferFile(String bufferFile) {
        this.bufferFile = bufferFile;
    }

    void setImporting(boolean importing) {
        this.importing = importing;
    }

    /**
     * Call this method when you want to offset the current machine's clock when sending $PONG messages
     * in response to $PING. Usually called when there is a need to pong the time of another machine that was
     * previously synchronized with the current machine
     * @param pongOffset    value to add to the current machine's clock
     */
    final synchronized void setPongOffset(long pongOffset) {
        this.pongOffset = pongOffset;
    }

    final synchronized long getPongOffset() {
        return pongOffset;
    }

    /**
     * Establish client channel connection
     *
     * @return true if succeeded
     */
    private boolean connect() {
        try {
            synchronized (sync) {
                if (!openChannel())
                    return false;
                lastFlush = null;
                lastCount = ZDate.now();
                connected = channelOpen = true;
                reset = false;
                buffer.execute(bufferFile);
            }
            handler.onConnect();
        } catch (Exception e) {
            handler.onEvent(EventCode.CONNECT_FAILED, e);
            return false;
        }
        return true;
    }
    
    private boolean openChannel() throws Exception {
        if (!open())
            return false;
        OutputStream output = getOutputStream();
        if (output != null) {
            if (isCompressable() && !importing)
                output = zipOut = new ZipWriter(output);
            out = new PrintWriter(new OutputStreamWriter(output, getCharacterSet()));
        }
        InputStream input = getInputStream();
        if (input != null) {
            if (isCompressable() && !importing)
                input = zipIn = new ZipReader(input, this);
            in = new BufferedReader(new InputStreamReader(input, getCharacterSet()));
        }
        return true;
    }
    
    /**
     * Execute connection and channel reading on a new thread
     */
    final void execute() {
        abort = connected = channelOpen = false;
        try {
            myThread = new Thread(this);
            myThread.setDaemon(true);
            myThread.start();
        } catch (Exception e) {
            handler.onEvent(EventCode.CONNECT_FAILED, e);
        }
    }

    private void closeChannel() {
        if (!channelOpen)
            return;
        channelOpen = false;
        try {
            close();
        } catch (Exception ex) {
        }
        try {
            if (in != null)
                in.close();
            if (out != null) {
                out.flush();
                out.close();
            }
        } catch (IOException ex) {
        }
        in = null;
        out = null;
    }

    /**
     * Send a line on the output socket
     *
     * @param line line to send
     * @param flush force a flush after sending the line
     * @return true if sending the line was followed by flushing
     */
    private boolean send(String line, boolean flush) {
        if (line != null)
            handler.onEvent(EventCode.SEND, line);
        boolean doMonitor = false, doKeepAlive = false;
        synchronized (sync) {
            if (!connected) {
                return false;
            }
            // as this method is called frequenly whether or not there is data to send, it's a good place
            // to do monitoring and keep alive pinging
            if (lastMonitor == null)
                lastMonitor = ZDate.now();
            if (getMonitorInterval() > 0 && lastMonitor.elapsed() >= getMonitorInterval()) {
                lastMonitor = ZDate.now();
                doMonitor = true;
            }
            if (lastPing == null)
                lastPing = ZDate.now();
            if (getKeepAliveInterval() > 0 && lastPing.elapsed() >= getKeepAliveInterval()) {
                lastPing = ZDate.now();
                doKeepAlive = true;
            }
        }
        if (doMonitor)
            handler.monitor();
        if (doKeepAlive && isConnected())
            transmit("", false);
        
        boolean flushed = false;
        if (lastFlush == null) {
            lastFlush = ZDate.now();
        }
        try {
            if (line != null) {
                writeLine(line);
                ++writeCount;
            }
            if (flush | timeToFlush()) {
                flush();
                lastFlush = ZDate.now();
                flushed = true;
            }
        } catch (Exception ex) {
            disconnect(ex.getMessage());
        }
        return flushed;
    }
    
    /**
     * Write a text line to the channel through the threaded buffer.
     *
     * @param line the text line
     * @param urgent true if needs to be inserted into the head of the buffer
     * and be flushed to the socket
     * @return false if the buffer is full and had to clear it
     */
    final boolean transmit(String line, boolean urgent) {
        synchronized (sync) {
            if (!isConnected() || buffer == null) {
                return true;
            }
        }
        return urgent ? buffer.addUrgent(line) : buffer.add(line);
    }
    
    /**
     * Format a text string and transmit it to the socket
     * @param fmt       format string
     * @param urgent    true if needs to be inserted into the head of the buffer
     * @param args      argument to insert in the formatted string
     * @return 
     */
    final boolean transmitF(String fmt, boolean urgent, Object ... args) {
        return transmit(String.format(fmt, args), urgent);
    }

    /**
     * Called to signal that the output of this socket should be compressed (if the other peer approves)
     */
    void compressOutput () {
        transmit(CoreConsts.Channel.COMPRESS_REQ, true);
    }

    /**
     * If connection failed wait the specified amount of seconds and try again
     *
     * @throws InterruptedException
     */
    private void waitForReattempt() throws InterruptedException {
        Thread.sleep(getReconnectWait() * ZDate.SECOND);
    }

    void ping() {
        transmit(CoreConsts.Channel.PING_COMMAND + "," + ZDate.now(ZDate.FULL_TIMESTAMP), true);
    }
    
    /**
     * Called from ZipReader to inform actual input length
     * @param length number of bytes read
     */
    void onPhysicalRead(int length) {
        if (handler != null)
            handler.onEvent(EventCode.PHYSICAL_READ, length);
    }

    /**
     * Abort everything on cleanup
     */
    void cleanup() {
        if (buffer != null) {
            buffer.cleanup(false);
        }
        try {
            synchronized(sync){
                this.abort = true;
            }
            if (myThread != null)
                myThread.interrupt();
            disconnect(null);
        } catch (Exception e) {
        }
    }
}
