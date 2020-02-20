package org.spiderwiz.core;

import java.io.IOException;
import java.util.UUID;
import org.spiderwiz.zutils.ZConfig;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZDictionary;
import org.spiderwiz.zutils.ZUtilities;

/**
 *
 * @author Zvi 
 */
class MyConfig extends ZConfig{
    public static final String APP_UUID = "application uuid";
    public static final String APP_NAME = "application name";
    final static String LOGFOLDER = "log folder";
    static final String PRODUCER_PREFIX = "producer-";
    static final String CONSUMER_PREFIX = "consumer-";
    static final String IMPORT_PREFIX = "import-";
    static final String SPIDERADMIN = "spideradmin";
    static final String PRODUCER_SERVER_PREFIX = "producer server-";
    static final String CONSUMER_SERVER_PREFIX = "consumer server-";
    private static final String BACKUP_FOLDER = "backup folder";
    private static final String DEFAULT_BACKUP_FOLDER = "backup";
    private static final String ARCHIVE_FOLDER = "archive folder";
    private static final String DEFAULT_ARCHIVE_FOLDER = "archive";
    private static final String DISCONNECTION_ALERT_MINUTES = "disconnection alert minutes";
    private static final String IDLE_ALERT_MINUTES = "idle alert minutes";
    private static final String RECONNECT_TIME = "reconnection seconds";
    private static final int ALERT_DEFAULT = 3;
    private static final int RECONNECT_DEFAULT = 60;
    static final String MAIL_SYSTEM = "mail system";
    static final String ALERT_MAIL_FROM = "from address";
    static final String ALERT_MAIL_TO = "to email";
    static final String ALERT_MAIL_CC = "cc email";
    static final String EXCEPTION_MAIL_TO = "to exception email";
    static final String EXCEPTION_MAIL_CC = "cc exception email";
    static final String MINIMUM_DISK_SPACE = "minimum disk space in gb";
    private static final String HUB_MODE = "hub mode";
    static final String IS_ADMIN = "is admin";
    static final String PASS_THROUGH = "pass through";
    private static final String EXCEPTION_ALERT_RATE = "exception alert rate";
    private static final String REFERSH_TRANSMIT_RATE = "stream rate";
    private static final int DEFAULT_RATE = 30000;
    private static final String OBSOLESCENCE_HOURS = "obsolescence hours";
    private static final int DEFAULT_OBSOLESCENCE_HOURS = 24;
    static final String IMPORTS_SUB_FOLDER = "Imports/";
    private static final String HISTORY_FILE_NAME = "history file";
    private static final String HISTORYFILE = "history.dat";
    private static final String START_OF_DAY = "start of day";
    private static final String DEFAULT_START_OF_DAY = "04:00";
    
    /**
     * Check if we are configured to run in Hub mode
     * @return true if we are.
     */
    boolean isHubMode() {
        return isPropertySet(HUB_MODE);
    }
    
    String getBackupPath(String name) {
        String folder = getProperty(BACKUP_FOLDER);
        if (folder == null)
            folder = DEFAULT_BACKUP_FOLDER;
        return Main.getInstance().getRootFolder() + folder + (name != null ? "/" + name : "");
    }
    
    String getArchiveFolder() {
        String folder = getProperty(ARCHIVE_FOLDER);
        if (folder == null)
            folder = DEFAULT_ARCHIVE_FOLDER;
        return Main.getInstance().getRootFolder() + folder;
    }
    
    /**
     * Return the time the system will wait after disconnection before it issues an alert mail
     * @return time in milliseconds
     */
    int getDisconnectionAlertTime() {
        int result;
        return ((result = getIntProperty(DISCONNECTION_ALERT_MINUTES)) != 0 ? result : ALERT_DEFAULT) * ZDate.MINUTE;
    }

    /**
     * Return the time the system will wait after getting into idle state before it issues an alert mail
     * @return time in milliseconds
     */
    int getIdleAlertTime() {
        int result;
        return ((result = getIntProperty(IDLE_ALERT_MINUTES)) != 0 ? result : ALERT_DEFAULT) * ZDate.MINUTE;
    }

    /**
     * Return the time a client channel that has been disconnected should wait before trying to reconnect.
     * @return time in seconds
     */
    int getReconnectTime() {
        int result;
        return (result = getIntProperty(RECONNECT_TIME)) != 0 ? result : RECONNECT_DEFAULT;
    }
    
    int getExceptionAlertRate() {
        int minutes = getIntProperty(EXCEPTION_ALERT_RATE);
        return minutes == 0 ? ZDate.HOUR : minutes * ZDate.MINUTE;
    }

    /**
     * Get maximum transmission rate when refreshing all predictions following a $ResetTRP request.
     * @return the rate as transmission per minute value
     */
    int getRefreshTranmsitRate() {
        int n = getIntProperty(REFERSH_TRANSMIT_RATE);
        return n > 0 ? n : DEFAULT_RATE;
    }
    
    /**
     * Get obsolescence time after which idle applications will be removed
     * @return time in milliseconds
     */
    int getObsolescenceTime() {
        int result;
        return ((result = getIntProperty(OBSOLESCENCE_HOURS)) > 0 ? result : result < 0 ? 0 : DEFAULT_OBSOLESCENCE_HOURS) * ZDate.HOUR;
    }

    synchronized void storeAppHistory(UUID uuid, String appName, String appVersion, String coreVersion, String remoteAddress,
       String userID, ZDictionary applicationParams, String objectCodes, ZDate lastSeen)
    {
        setProperty(uuid.toString(), ZUtilities.concatAll(",",
            Serializer.escapeDelimiters(appName),
            Serializer.escapeDelimiters(appVersion),
            Serializer.escapeDelimiters(coreVersion),
            Serializer.escapeDelimiters(remoteAddress),
            Serializer.escapeDelimiters(userID),
            applicationParams == null ? null : Serializer.encodeParameterList(applicationParams),
            objectCodes,
            lastSeen == null ? null : lastSeen.format(ZDate.TIMESTAMP)
        ));
        saveConfiguration();
    }
    
    void removeAppHistory(UUID uuid) {
        setProperty(uuid.toString(), null);
        saveConfiguration();
    }
    
    /**
     * Load the setting file, add or replace a property, and save it back
     * @param key           Property key to add
     * @param value         Property value
     * @param onHead        if true the property will be added (if doesn't already exist) at the head of the file
     * (after the "modified by" line
     * @throws java.io.IOException
     */
    void addProperty (String key, String value, boolean onHead) throws IOException {
        if (key == null)
            return;
        PropertySheet ps = getPropertySheet();
        // walk through the properties, save position of the property to replace and the "modified by" property if exist
        int iKey = -1, iModifiedBy = -1;
        for (int i = 0; i < ps.size(); i++) {
            String propertyKey = ps.get(i).getKey();
            if (propertyKey.equals(key))
                iKey = i;
            if (propertyKey.toLowerCase().startsWith(CoreConsts.MODIFIED_BY))
                iModifiedBy = i;
        }
        
        // process the property sheet
        if (iKey >= 0)
            ps.get(iKey).setValue(value);
        else {
            Property prop = new Property(key, value);
            if (onHead)
                ps.add(iModifiedBy == 0 ? 1 : 0, prop);
            else
                ps.add(prop);
        }
        savePropertySheet(ps);
    }
    
    /**
     * Get log mode according to [loginput] or [logoutput] in configuration file
     * @param property          either "input log" or "output log"
     * @param channelDefined    the value defined for the channel connection string
     * @return log mode as an integer number
     */
    int getLogMode(String property, int channelDefined) {
        if (channelDefined != CoreConsts.DataChannel.LOG_UNDEFINED)
            return channelDefined;
        String prop = getProperty(property);
        if (prop == null || prop.isEmpty())
            return CoreConsts.DataChannel.LOG_NONE;
        prop = prop.toLowerCase();
        switch (prop) {
        case CoreConsts.DataChannel.FULL:
            return CoreConsts.DataChannel.LOG_FULL;
        case CoreConsts.DataChannel.RAW:
            return CoreConsts.DataChannel.LOG_RAW;
        case CoreConsts.DataChannel.VERBOSE:
            return CoreConsts.DataChannel.LOG_VERBOSE;
        }
        return CoreConsts.DataChannel.LOG_NONE;
    }
    
    /**
     * Lookup in the given ZDictionary object if it contains a logging parameter with the given label and interpret it
     * @param configParams
     * @param label
     * @return the value of the interpreted parameter
     */
    int interpretLogParam(ZDictionary configParams, String label) {
        String log = configParams.get(label);
        if (log != null) {
            if (log.startsWith(CoreConsts.DataChannel.NO))
                return CoreConsts.DataChannel.LOG_NONE;
            else {
                switch(log) {
                case CoreConsts.DataChannel.RAW:
                    return CoreConsts.DataChannel.LOG_RAW;
                case CoreConsts.DataChannel.FULL:
                    return  CoreConsts.DataChannel.LOG_FULL;
                case CoreConsts.DataChannel.VERBOSE:
                    return CoreConsts.DataChannel.LOG_VERBOSE;
                }
            }
        }
        return CoreConsts.DataChannel.LOG_UNDEFINED;
    }
    
    /**
     * Interpret the 'compress' configuration parameter.
     * @param configParams
     * @param defaultCompression
     * @return 
     */
    int interpretCompressParam(ZDictionary configParams, int defaultCompression) {
        String comp = configParams.get(CoreConsts.DataChannel.COMPRESS);
        if (comp != null) {
            switch (comp) {
            case CoreConsts.DataChannel.ZIP:
                return DataHandler.ZIP_COMPRESSION;
            case CoreConsts.DataChannel.LOGICAL:
                return DataHandler.LOGICAL_COMPRESSION;
            case CoreConsts.DataChannel.FULL:
                return DataHandler.FULL_COMPRESSION;
            default:
                if (comp.toLowerCase().startsWith(CoreConsts.DataChannel.NO))
                    return DataHandler.NO_COMPRESSION;
            }
        }
        return defaultCompression;
    }
    
    /**
     * Process an entry in connections.dat file upon startup
     * @param key
     * @param value 
     */
    @Override
    protected void processProperty(String key, String value) {
        // If key is not a UUID, ignore it
        try {
            UUID uuid = UUID.fromString(key);
            String[] s = value.split(",");
            int i = 0;
            String appName = Serializer.unescapeDelimiters(s[i++]);
            String appVersion = Serializer.unescapeDelimiters(s[i++]);
            String coreVersion = Serializer.unescapeDelimiters(s[i++]); 
            String remoteAddress = i >= s.length ? "" : Serializer.unescapeDelimiters(s[i++]);
            String userID = i >= s.length ? null : Serializer.unescapeDelimiters(s[i++]);
            ZDictionary applicationParams = null;
            if (i < s.length) {
                String params = s[i++];
                if (!params.isEmpty())
                    applicationParams = Serializer.parseParameterList(params);
            }
            String objectCodes = i >= s.length ? "" : s[i++];
            ZDate disconnected = null;
            if (i < s.length) {
                String ts = s[i++];
                if (!ts.isEmpty())
                    disconnected = ZDate.parseTime(ts, ZDate.TIMESTAMP, null);
            }
            Hub.getInstance().loadAppHistory(
                uuid, appName, appVersion, coreVersion, remoteAddress, userID, applicationParams, objectCodes, disconnected);
        } catch (Exception ex) {}
    }
    
    String getHistoryFileName() {
        String result = getProperty(HISTORY_FILE_NAME);
        return result == null ? HISTORYFILE : result;
    }
    /**
     * Return the time considered "start of day" in milliseconds;
     * @return
     */

    public long getStartOfDay () {
        return getDailyTime(START_OF_DAY);
    }    
    
    /**
     * Return the value in milliseconds of a property of the format hh:mm
     * @param prop property name
     * @return the value of the property if exists, otherwise return a default "start of day" value
     */
    private long getDailyTime(String prop) {
        String sod = getProperty(prop);
        if (sod == null || sod.isEmpty())
            sod = DEFAULT_START_OF_DAY;
        do {
            try {
                String s[] = sod.split("\\:");
                if (s.length >= 2)
                    return Long.parseLong(s[0]) * ZDate.HOUR + Long.parseLong(s[1]) * ZDate.MINUTE;
            } catch (Exception ex) {
            }
            sod = DEFAULT_START_OF_DAY;
        } while (true);
    }
}
