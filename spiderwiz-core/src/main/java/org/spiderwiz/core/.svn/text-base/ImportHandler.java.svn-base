package org.spiderwiz.core;

import java.util.Map;
import java.util.UUID;
import org.spiderwiz.plugins.FileChannel;
import org.spiderwiz.plugins.PluginConsts;
import org.spiderwiz.plugins.TcpSocket;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZLog;

/**
 * This class provides a mechanism for importing data from external resources into the framework and exporting data
 * from the framework to external resources. The mechanism involves the following steps:
 * <ul>
 * <li>Configuring an import channel through the <code>import-<em>n</em></code> property
 * defined in the <a href="doc-files/config.html#import">application's configuration file</a>.</li>
 * <li>Retrieving imported objects from the channel.</li>
 * <li>For each object, creating an instance of each <em>data object</em> that the application
 * {@link org.spiderwiz.core.Main#getProducedObjects() produces} and letting it have a chance to interpret the data by calling its 
 * {@link org.spiderwiz.core.DataObject#importObject(java.lang.Object, org.spiderwiz.core.ImportHandler, org.spiderwiz.zutils.ZDate) 
 * importObject()} method.</li>
 * <li>Whenever a <em>data object</em> is {@link org.spiderwiz.core.DataObject#commit() committed} its
 * {@link org.spiderwiz.core.DataObject#exportObject(org.spiderwiz.core.ImportHandler, java.lang.String)  exportObject()}
 * method is called, and if it returns a non-null object, the object is exported to the import channel.</li>
 * </ul>
 * <p>
 * The mechanism provided by this class supports importing and exporting of serialized data through one of the communication channels
 * supported by the framework (or a communication plugin). It may happen that you want to hook into
 * the basic mechanism and provide some extra processing in one or more of its steps. You can do it by extending the class. If you do,
 * you should register your implementation with {@link Main#createImportHandler() Main.createImportHandler()} to
 * make it a substitute to the default class. This is called a serialize Import handler.
 * <p>
 * You can also implement a general object Import handler, in which case you would write the code for retrieving and delivering
 * import/export objects directly.
 * <p>
 * Override {@link #configure(java.util.Map, int) configure()} to customize your handler configuration.
 * <p>
 * To modify the handler behavior in connection and disconnection events override {@link #onConnect()},
 * {@link #onDisconnect(java.lang.String) onDisconnect()} and {@link #onConnectFailed(java.lang.Exception) onConnectFailed()}.
 * <p>
 * To extend the handling of imported serialized data override
 * {@link #processLine(java.lang.String, org.spiderwiz.zutils.ZDate) processLine()}.
 * <p>
 * General object handlers should call {@link #processObject(java.lang.Object, org.spiderwiz.zutils.ZDate, int) processObject()}
 * to feed the framework with imported objects.
 * <p>
 * Override {@link #exportObject(java.lang.Object) exportObject()} to handle exported objects in the case of a general object handler,
 * or to extend the default implementation in the case of serialize handlers.
 * <p>
 * To do custom cleanup code when the handler is discarded override {@link #cleanup(boolean) cleanup()}.
 * <p>
 * Note that each instance of this class manages one {@link org.spiderwiz.core.Channel} object (in the case of serialize handler).
 * You can get the object by calling {@link #getChannel()}.
 *
 * @see Channel
 * @see Main.ObjectCodes#RawImport
 * @see Main.ObjectCodes#RawExport
 * @see Main#createImportHandler()
 */
 public class ImportHandler extends ChannelHandler {
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
     * Class constructor.
     */
    public ImportHandler() {
        info = new ImportInfo(this);
    }
    
    /**
     * Configures and initializes the handler.
     * <p>
     * configures and initializes the handler from from parameters specified by
     * <code>import-<em>n</em></code> properties in the
     * <a href="doc-files/config.html#ImportConnection">application's configuration file</a>.
     * The values of these properties must be a list of pairs <em>key=value</em> concatenated by a semicolon.
     * <p>
     * The default implementation of the method initializes a serialized data channel as documented there. Implementations of
     * serialize Import handlers can override this method to add custom initialization code after calling {@code super.configure()}.
     * Implementations of general object handlers can override it to provide their own configuration and initialization code.
     * 
     * @param configParams      a map of key=value configuration parameters.
     * @param n                 the <em>n</em> value of the <em>import-n</em> property.
     * @return true on success.
     */
    protected boolean configure(Map<String, String> configParams, int n) {
        try {
            String customClass;
            name = String.valueOf(n);
            if ((customClass = configParams.get(CoreConsts.Channel.CLASS)) != null)
                try {
                    channel = (Channel)Class.forName(customClass).getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    String msg = String.format(CoreConsts.AlertMail.CHANNEL_CLASS_FAILED, customClass, ex.toString());
                    Main.getLogger().logEvent(msg);
                    Main.getInstance().sendNotificationMail(msg, null, null, true);
                    return false;
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
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, CoreConsts.AlertMail.EXCEPTION_IMPORT_INIT,
                String.format(CoreConsts.AlertMail.PROPERTY_STRING, configParams), false);
            return false;
        }
        return true;
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
     * Processes an imported serialized data line.
     * <p>
     * Override this method if you implement a serialize importer and you want to do extra processing on imported lines before
     * passing them over to the framework. To pass the line to the framework call {@code super.processLine(line, ts)}.
     * @param line  record content.
     * @param ts    timestamp attached to the line  if provided by the channel managed by this handler.
     */
    @Override
    protected void processLine(String line, ZDate ts) {
        try {
            processObject(line, ts, line.length());
        } catch (Exception ex) {
            info.updateErrors();
            Main.getInstance().sendExceptionMail(ex,
                String.format(CoreConsts.AlertMail.IMPORT_PARSING_ERROR_ALERT, getName(), getRemoteAddress()),
                String.format(CoreConsts.AlertMail.WHEN_PARSING_LINE, line),
                false);
        } 
    }
    
    /**
     * Processes an imported object.
     * <p>
     * You can call this method when you implement a general object import handler in order to process one import object.
     * @param importObject  the imported object
     * @param ts            timestamp of the import. If null, the current time is used.
     * @param size          size of the object in bytes, for statistic purposes (logging and
     *                      <a href="http://spideradmin.com">SpiderAdmin</a>).
     * @throws Exception
     */
    protected void processObject(Object importObject, ZDate ts, int size) throws Exception {
        if (alerted) {
            alerted = false;
            Main.getInstance().sendNotificationMail(String.format(
                CoreConsts.AlertMail.IMPORT_RESUME_NOTIFICATION, getName(), getRemoteAddress()), null, ZDate.now(), false);
        }
        lastInput = ZDate.now();
        if (logInput)
            logger.log("<- " + importObject.toString(), false);
        info.updateActivity(null, size);
        if (ts == null)
            ts = ZDate.now();
        DataManager.getInstance().processImportCommand(importObject, this, ts);
    }

    /**
     * Exports an object.
     * <p>
     * Override this method to do extra processing on exported records before transmitting them over the channel managed by this
     * handler (if you implement a serialize importer), or to export it in a form other than a serialized string (if you implement
     * a general object importer). In the first case,
     * call {@code super.exportObject(data)} to transmit the data as a string.
     * @param data  the exported record.
     * @return true if and only if record was transmitted successfully.
     */
    protected boolean exportObject(Object data) {
        return channel.transmit(data.toString(), false);
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

    void setDefString(String defString) {
        this.defString = defString;
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
     * @param obj       The object to encode and send.
     * @param newID     null if the object is active, empty string if it has been removed, non-empty string if it has been renamed.
     * @return true if successful.
     */
    boolean transmitObject(DataObject obj, String newID){
        Object data = obj.exportObject(this, newID);
        return data == null || exportObject(data);
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
