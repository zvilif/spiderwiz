package org.spiderwiz.admin.services;

import org.spiderwiz.admin.data.ApplicationInfo;
import org.spiderwiz.admin.data.PageInfo;
import org.spiderwiz.admin.data.PageInfo.TableInfo;
import org.spiderwiz.admin.data.TableData;
import org.spiderwiz.core.Main;

/**
 * Constants and methods used by SpiderAdmin services
 * @author Zvi 
 */
public class AdminServices {
    public final static String APPLICATION_INFO_SERVICE = "applicationInfo";
    public final static String PAGE_INFO_SERVICE = "pageInfo";
    public final static String RELOAD_SETTINGS_SERVICE = "reloadSettings";
    public final static String FLUSH_LOGS_SERVICE = "flushLogs";
    public final static String SERVER_INFO_TABLE_SERVICE = "serverInfo";
    public final static String APPLICATION_TABLE_SERVICE = "appsInfo";
    public final static String IMPORT_TABLE_SERVICE = "importInfo";
    public final static String PRODUCER_TABLE_SERVICE = "producerInfo";
    public final static String CONSUMER_TABLE_SERVICE = "consumerInfo";
    public final static String LOAD_PROPERTIES_SERVICE = "loadProperties";
    public final static String SAVE_PROPERTIES_SERVICE = "saveProperties";
    public final static String LIST_LOG_FOLDER = "listLogFolder";
    public final static String LIST_DEPLOY_FOLDER = "listDeployFolder";
    public final static String LIST_UPLOAD_FOLDER = "listUploadFolder";
    public final static String DOWNLOAD_LOG_FILE = "downloadLogFile";
    public final static String UPLOAD_DEPLOY_FILE = "uploadDeployFile";
    public final static String REQUEST_DEPLOY_FILE = "requestDeployFile";
    public final static String ABORT_UPLOAD_FILE = "abortDeployFile";
    public final static String DEPLOY_FILE = "deployFile";
    public final static String RESTART_SERVICE = "restartService";

    private final static String SERVER_INFO_TABLE_TITLE = "Server Information";
    private final static String APPLICATION_TABLE_TITLE = "Applications";
    private final static String IMPORT_TABLE_TITLE = "Import Channels";
    private final static String PRODUCER_TABLE_TITLE = "Producers";
    private final static String CONSUMER_TABLE_TITLE = "Consumers";
    
    public final static String DATE_PREFIX = "xadmin.date:";

    private class ServerInfoColumnTitles {
        static final String DEPLOYEDAT = "Deployed at";
        static final String SERVERLOCATION = "Server location";
        static final String COREVERSION = "Core version";
        static final String CPUPERCENTAGE = "% of used CPU";
        static final String AVAILABLEMEMORY = "Available JVM memory";
        static final String AVAILABLEMEMORYSUBTITLE = "MB";
        static final String FREEDISKSPACE = "Free disk space";
        static final String FREEDISKSPACESUBTITLE = "GB";
    }

    public static PageInfo getPageInfo() {
        return new PageInfo().
            addTable(Main.getInstance().getServerInfoTableStructure()).
            addTable(Main.getInstance().getApplicationsTableStructure(APPLICATION_TABLE_TITLE, AdminServices.APPLICATION_TABLE_SERVICE)).
            addTable(Main.getInstance().getImportsTableStructure(IMPORT_TABLE_TITLE, AdminServices.IMPORT_TABLE_SERVICE)).
            addTable(Main.getInstance().getConnectedNodesTableStructure(PRODUCER_TABLE_TITLE, AdminServices.PRODUCER_TABLE_SERVICE)).
            addTable(Main.getInstance().getConnectedNodesTableStructure(CONSUMER_TABLE_TITLE, AdminServices.CONSUMER_TABLE_SERVICE));
    }

    public static TableInfo getServerInfoTableStructure() {
        return new TableInfo(SERVER_INFO_TABLE_TITLE, AdminServices.SERVER_INFO_TABLE_SERVICE, false).
            addColumn(ServerInfoColumnTitles.DEPLOYEDAT, null, TableInfo.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.SERVERLOCATION, null, TableInfo.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.COREVERSION, null, TableInfo.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.CPUPERCENTAGE, null, TableInfo.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.AVAILABLEMEMORY, ServerInfoColumnTitles.AVAILABLEMEMORYSUBTITLE, TableInfo.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.FREEDISKSPACE, ServerInfoColumnTitles.FREEDISKSPACESUBTITLE, TableInfo.Style.CENTER, 0);
    }

    public static void addServerInfoRowData(TableData.RowData row) {
        ApplicationInfo info = Main.getInstance().getApplicationInfo();
        row.
            addCell(ServerInfoColumnTitles.DEPLOYEDAT, info.getDeployTime(), 0, null).
            addCell(ServerInfoColumnTitles.SERVERLOCATION, info.getServerLocation(), 0, null).
            addCell(ServerInfoColumnTitles.COREVERSION, info.getCoreVersion(), 0, null).
            addCell(ServerInfoColumnTitles.CPUPERCENTAGE, info.getUsedCpu(), 0, null).
            addCell(ServerInfoColumnTitles.AVAILABLEMEMORY, info.getAvailableJvmMemory(), 0, null).
            addCell(ServerInfoColumnTitles.FREEDISKSPACE, info.getAvailableDiskSpace(), 0, null);
    }

}
