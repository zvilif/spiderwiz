package org.spiderwiz.core;

import java.util.UUID;
import org.spiderwiz.plugins.FileChannel;
import org.spiderwiz.plugins.PluginConsts;
import org.spiderwiz.plugins.TcpSocket;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZDictionary;
import org.spiderwiz.zutils.ZLog;

/**
 * This class provides the <em>Spiderwiz</em> mechanism for importing data from external resources into the framework and exporting data
 * from the framework to external resources. The mechanism involves the following steps:
 * <ul>
 * <li>Creating an import {@link org.spiderwiz.core.Channel} and configuring it through the <code>import-<em>n</em></code> property
 * defined in the <a href="doc-files/config.html#import">application's configuration file</a>.</li>
 * <li>Getting serialized data records from the channel.</li>
 * <li>For each record, creating an instance of each <em>data object</em> that the application
 * {@link org.spiderwiz.core.Main#getProducedObjects() produces} and letting it a chance to interpret the data by calling its 
 * {@link org.spiderwiz.core.DataObject#importObject(java.lang.String, org.spiderwiz.core.ImportHandler, org.spiderwiz.zutils.ZDate)
 * importObject()} method.</li>
 * <li>Whenever a <em>data object</em> is {@link org.spiderwiz.core.DataObject#commit() committed} its
 * {@link org.spiderwiz.core.DataObject#exportObject(org.spiderwiz.core.ImportHandler) exportObject()} method is called, and if it
 * returns a non-null data record, the record is written to the import channel.</li>
 * </ul>
 * <p>
 * In many cases the mechanism provided by {@code ImportHandler} class is sufficient. However it may happen that you want to hook into
 * the basic mechanism and provide some extra processing in one or more of its steps. You can do it by extending the class. If you do,
 * you should register your implementation with {@link org.spiderwiz.core.Main#createImportHandler(int) Main.createImportHandler()} to
 * make it a substitute to the default class.
 * <p>
 * To extend the class initialization code override {@link #init()}.
 * <p>
 * To extend the class behavior in connection and disconnection events override {@link #onConnect()},
 * {@link #onDisconnect(java.lang.String) onDisconnect()} and {@link #onConnectFailed(java.lang.Exception) onConnectFailed()}.
 * <p>
 * To extend the handling of imported records override {@link #processLine(java.lang.String, org.spiderwiz.zutils.ZDate) processLine()}
 * and for exported records override {@link #transmitCommand(java.lang.String) transmitCommand()}.
 * <p>
 * To do custom cleanup code when the handler is discarded override {@link #cleanup(boolean) cleanup()}.
 * <p>
 * Note than each instance of this class manages one {@link org.spiderwiz.core.Channel} object. You can get the object by calling
 * {@link #getChannel()}.
 *
 * @see Channel
 * @see Main.ObjectCodes#RawImport
 * @see Main.ObjectCodes#RawExport
 * @see Main#createImportHandler(int)
 */
 public class ImportHandler extends ChannelHandler {
    private final int number;
    private String defString = null;
    private String name;
    private UUID appUUID = null;
    private Channel channel = null;
    private ZLog logger = null;
    private ZDate lastDisconnect = null, lastInput = null, connectedSince = null;
    private final ImportInfo info;
    private boolean alert = false, alerted = false, logInput = false, logOutput = false;
    private DataHandler rimChannel = null;
    
    private final static String[] statusText = {"OK", "Disconnected", "Idle"};
    private final static int IDLE_TIME = ZDate.MINUTE * 3;

    /**
     * Class constructor gets a number, which identifies the connection in import-n property of the application configuration
     * file.
     * @param number    the 'n' in import-n property of the configuration file.
     */
    ImportHandler(int number) {
        this.number = number;
        info = new ImportInfo(this);
    }
    
    /**
     * Initializes the handler.
     * <p>
     * Override this method to execute custom handler initialization.
     */
    protected void init() {
    }

    /**
     * Called when the channel managed by this handler is connected.
     * <p>
     * Override this method to do extra processing when the channel managed by this handler is connected. Your implementation
     * must call {@code super.onConnect()}.
     */
    @Override
    protected void onConnect() {
        setConnectedSince(ZDate.now());
        lastDisconnect = null;
        logger.logEvent(CoreConsts.ImportChannel.CONNECTION_OK, getRemoteAddress());
        RawImport.createAndCommit(null, ZDate.now(), this);
    }

    /**
     * Called when the channel managed by this handler is disconnected.
     * <p>
     * Override this method to do extra processing when the channel managed by this handler is disconnected. Your implementation
     * must call {@code super.onDisconnect(reason)}.
     * @param reason    text describing the disconnection reason as implemented by {@link org.spiderwiz.core.Channel}.
     */
    @Override
    protected void onDisconnect(String reason) {
        lastDisconnect = ZDate.now();
        lastInput = null;
        logger.logEvent(CoreConsts.ImportChannel.DISCONNECT, getRemoteAddress(), reason);
        RawImport.createAndCommit(null, null, this);
    }

    /**
     * Called when the channel managed by this handler could not connect.
     * <p>
     * Override this method to do extra processing when the channel managed by this handler could not connect. Your implementation
     * must call {@code super.onConnectFailed(ex)}.
     * @param ex  the exception object that was thrown on the attempt to connect, if any, or null if not applicable.
     */
    protected void onConnectFailed(Exception ex) {
        logger.logEvent(CoreConsts.ImportChannel.CONNECTION_FAIL, getRemoteAddress(), ex);
    }

    /**
     * Processes an imported record.
     * <p>
     * Override this method to do extra processing on imported records before passing them over to the framework. To pass the record
     * to the framework call {@code super.processLine(line, ts)}.
     * @param line  record content.
     * @param ts    timestamp attached to the record if provided by the channel managed by this handler.
     */
    @Override
    protected void processLine(String line, ZDate ts) {
        try {
            if (alerted) {
                alerted = false;
                Main.getInstance().sendNotificationMail(String.format(
                    CoreConsts.AlertMail.IMPORT_RESUME_NOTIFICATION, getName(), getRemoteAddress()), null, ZDate.now(), false);
            }
            lastInput = ZDate.now();
            if (logInput)
                logger.log("<- " + line, false);
            info.updateActivity(null, line.length());
            if (ts == null)
                ts = ZDate.now();
            DataManager.getInstance().processImportCommand(line, this, ts);
        } catch (Exception ex) {
            info.updateErrors();
            Main.getInstance().sendExceptionMail(ex,
                String.format(CoreConsts.AlertMail.IMPORT_PARSING_ERROR_ALERT, getName(), getRemoteAddress()),
                String.format(CoreConsts.AlertMail.WHEN_PARSING_LINE, line),
                false);
        } 
    }

    /**
     * Exports a record.
     * <p>
     * Override this method to do extra processing on exported records before transmitting them over the channel managed by this
     * handler. To transmit them call {@code super.transmitCommand(line)}.
     * @param line      the exported record.
     * @return true if and only if record was transmitted successfully.
     */
    protected boolean transmitCommand(String line) {
        return channel.transmit(line, false);
    }
    
    /**
     * Returns the {@link org.spiderwiz.core.Channel} object managed by this handler.
     * @return the Channel object managed by this handler.
     */
    protected final Channel getChannel() {
        return channel;
    }

    /**
     * Cleans up object resources.
     * <p>
     * Override this method to do custom resource cleanup code when the handler is discarded.
     */
    protected void cleanup(){}
    
    /**
     * Returns the version of the server application the channel managed by this handler is connected to, or null if not applicable.
     * <p>
     * Override this method to return a string representing the version of the server application this handler is connected to.
     * @return the version of the server application the channel managed by this handler is connected to, or null if not applicable
     * or by default.
     */
    protected String getServerVersion() {
        return null;
    }

    /**
     * Initialize the object and create an appropriate Channel object to handle the physical communication.
     * @param defString 
     */
    void init(String defString, int n) {
        try {
            this.defString = defString;
            String customClass;
            name = String.valueOf(number);
            ZDictionary configParams = ZDictionary.parseParameterList(defString);
            if ((customClass = configParams.get(CoreConsts.Channel.CLASS)) != null)
                try {
                    channel = (Channel)Class.forName(customClass).newInstance();
                } catch (Exception ex) {
                    Main.getInstance().sendExceptionMail(ex,
                        String.format(CoreConsts.AlertMail.CHANNEL_CLASS_FAILED, customClass, ex.toString()), null, true);
                }
            else if (configParams.get(PluginConsts.TcpSocket.IP) !=  null && configParams.get(PluginConsts.TcpSocket.PORT) != null)
                channel = new TcpSocket();
            else if (configParams.get(PluginConsts.FileChannel.INFILE) != null &&
                configParams.get(PluginConsts.FileChannel.OUTFILE) != null)
                channel = new FileChannel();
            logInput = configParams.containsKey(CoreConsts.ImportChannel.LOGINPUT);
            logOutput = configParams.containsKey(CoreConsts.ImportChannel.LOGOUTPUT);
            alert = configParams.containsKey(CoreConsts.DataChannel.ALERT);
            if (channel != null) {
                channel.init(this).setImporting(true);
                if (channel.configure(configParams, 0, n)) {
                    String impName = configParams.get(CoreConsts.ImportChannel.NAME);
                    if (impName != null)
                        name = impName;
                    String charset = configParams.get(CoreConsts.ImportChannel.CHARSET);
                    if (charset != null)
                        channel.setCharacterSet(charset);
                    channel.execute();
                    // Set log folder
                    restartLog(!channel.isFileChannel());
                }
            }
            init();
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, CoreConsts.AlertMail.EXCEPTION_IMPORT_INIT,
                String.format(CoreConsts.AlertMail.PROPERTY_STRING, defString), false);
        }
    }
    
    String getDefString() {
        return defString;
    }

    /**
     * Returns the connection name.
     * <p>
     * Returns the name attached to the connection managed by this handler in the
     * <a href="../core/doc-files/config.html#importName">application's configuration file</a>. If a name is not attached, the
     * value of <i>n</i> used in the <code>import-<em>n</em></code> property defined in that file is returned.
     * @return the connection name.
     */
    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    UUID getAppUUID() {
        return appUUID;
    }

    void setAppUUID(UUID appUUID) {
        this.appUUID = appUUID;
    }

    /**
     * Returns the remote address of the connected entity.
     * @return the remote address of the connected entity.
     */
    public String getRemoteAddress() {
        return getChannel() == null ? "?" : channel.getRemoteAddress();
    }
    
    String getAppName() {
        return appUUID == null ? null : Hub.getInstance().getAppName(appUUID);
    }

    boolean isConnected() {
        return getChannel() != null && channel.isConnected();
    }
    
    String getStatus() {
        CoreConsts.DataNodeInfo.StatusCode status = isConnected() ? lastInput != null && lastInput.elapsed() < IDLE_TIME ?
            CoreConsts.DataNodeInfo.StatusCode.OK : CoreConsts.DataNodeInfo.StatusCode.IDLE :
            CoreConsts.DataNodeInfo.StatusCode.DISCONNECTED;
        return statusText[status.ordinal()];
    }
    
    synchronized ZDate getConnectedSince() {
        return connectedSince;
    }

    synchronized void setConnectedSince(ZDate connectedSince) {
        this.connectedSince = connectedSince;
    }

    ImportInfo getInfo() {
        return info;
    }

    /**
     * If this handler was created by a RIM set the channel where the RIM came from.
     * @param rimChannel
     */
    void setRimChannel(DataHandler rimChannel) {
        this.rimChannel = rimChannel;
    }

    /**
     * If this handler was created by a RIM return the channel where the RIM came from.
     * @return
     */
    DataHandler getRimChannel() {
        return rimChannel;
    }
    
    private void restartLog(boolean append) throws Exception {
        if (logger != null)
            logger.cleanup();
        logger = new ZLog(Main.getInstance().getRootFolder(), Main.getMyConfig(), append);
        logger.init(MyConfig.LOGFOLDER);
        logger.setRootPath(logger.getRootPath() + MyConfig.IMPORTS_SUB_FOLDER +
            MyUtilities.escapeNonAlphanumeric("-", name) + "/");
    }

    /**
     * Encode an object as an Import command and send it to the Import server. Nothing will be sent if the object's exportObject()
     * function returns null.
     * @param obj   The object to encode and send.
     * @return true if successful.
     */
    boolean transmitObject(DataObject obj){
        String line = obj.exportObject(this);
        return line == null || transmitCommand(line);
    }
    
    boolean isCaptured() {
        return getChannel() != null && channel.isFileChannel();
    }
    
    void flushLog() {
        if (logger != null)
            logger.flush();
    }

    private void monitorChannel() {
        boolean alertDisconnect = false, alertIdle = false;
        synchronized(this){
            if (!alert)
                return;
            int alertTime = Main.getMyConfig().getDisconnectionAlertTime();
            if (alertTime > 0 && lastDisconnect != null && lastDisconnect.elapsed() >= alertTime) {
                alertDisconnect = true;
                lastDisconnect = null;
            }

            alertTime = Main.getMyConfig().getIdleAlertTime();
            if (alertTime > 0 && lastInput != null && lastInput.elapsed() >= alertTime) {
                alertIdle = true;
                lastInput = null;
            }
        }
        
        if (alertDisconnect) {
            alerted = true;
            Main.getInstance().sendNotificationMail(
                String.format(CoreConsts.AlertMail.IMPORT_DISCONNECT_ALERT, getName(), getRemoteAddress()), null, ZDate.now(), true);
        }

        if (alertIdle) {
            alerted = true;
            Main.getInstance().sendNotificationMail(
                String.format(CoreConsts.AlertMail.IMPORT_IDLE_ALERT, getName(), getRemoteAddress()), null, lastInput, true);
        }
    }
    
    void resetCounters() {
        info.reset();
    }

    void cleanup(boolean flush) {
        cleanup();
        if (logger != null)
            logger.cleanup();
        if (getChannel() != null)
            channel.cleanup();
    }

    @Override
    void onEvent(Channel.EventCode eventCode, Object additionalInfo) {
        switch (eventCode) {
        case SEND:
            onSend(additionalInfo.toString());
            break;
        case CONNECT_FAILED:
            onConnectFailed((Exception)additionalInfo);
            break;
        }
    }

    private void onSend(String line) {
        if (line != null && logOutput)
            logger.log("-> " + line, false);
    }

    @Override
    void monitor() {
        monitorChannel();
    }
}
