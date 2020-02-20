package org.spiderwiz.admin.services;

import org.spiderwiz.admin.xml.ApplicationInfo;
import org.spiderwiz.admin.xml.PageInfoEx;
import org.spiderwiz.admin.xml.TableDataEx;
import org.spiderwiz.admin.xml.TableInfoEx;
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

    private final static String SERVER_INFO_TABLE_TITLE = "Server information";
    private final static String APPLICATION_TABLE_TITLE = "Applications";
    private final static String IMPORT_TABLE_TITLE = "Import channels";
    private final static String PRODUCER_TABLE_TITLE = "Producers";
    private final static String CONSUMER_TABLE_TITLE = "Consumers";

    private class ServerInfoColumnTitles {
        static final String deployedAt = "Deployed at";
        static final String serverLocation = "Server location";
        static final String coreVersion = "Core version";
        static final String cpuPercentage = "% of used CPU";
        static final String availableMemory = "Available JVM memory";
        static final String availableMemorySubTitle = "MB";
        static final String freeDiskSpace = "Free disk space";
        static final String freeDiskSpaceSubTitle = "GB";
    }

    public static PageInfoEx getPageInfo() {
        return new PageInfoEx().
            addTable(Main.getInstance().getServerInfoTableStructure()).
            addTable(Main.getInstance().getApplicationsTableStructure(APPLICATION_TABLE_TITLE, AdminServices.APPLICATION_TABLE_SERVICE)).
            addTable(Main.getInstance().getImportsTableStructure(IMPORT_TABLE_TITLE, AdminServices.IMPORT_TABLE_SERVICE)).
            addTable(Main.getInstance().getConnectedNodesTableStructure(PRODUCER_TABLE_TITLE, AdminServices.PRODUCER_TABLE_SERVICE)).
            addTable(Main.getInstance().getConnectedNodesTableStructure(CONSUMER_TABLE_TITLE, AdminServices.CONSUMER_TABLE_SERVICE));
    }

    public static TableInfoEx getServerInfoTableStructure() {
        return new TableInfoEx(SERVER_INFO_TABLE_TITLE, AdminServices.SERVER_INFO_TABLE_SERVICE, false).
            addColumn(ServerInfoColumnTitles.deployedAt, null, TableInfoEx.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.serverLocation, null, TableInfoEx.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.coreVersion, null, TableInfoEx.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.cpuPercentage, null, TableInfoEx.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.availableMemory, ServerInfoColumnTitles.availableMemorySubTitle, TableInfoEx.Style.CENTER, 0).
            addColumn(ServerInfoColumnTitles.freeDiskSpace, ServerInfoColumnTitles.freeDiskSpaceSubTitle, TableInfoEx.Style.CENTER, 0);
    }

    public static void addServerInfoRowData(TableDataEx.RowDataEx row) {
        ApplicationInfo info = Main.getInstance().getApplicationInfo();
        row.
            addCell(ServerInfoColumnTitles.deployedAt, info.getDeployTime(), 0, null).
            addCell(ServerInfoColumnTitles.serverLocation, info.getServerLocation(), 0, null).
            addCell(ServerInfoColumnTitles.coreVersion, info.getCoreVersion(), 0, null).
            addCell(ServerInfoColumnTitles.cpuPercentage, info.getUsedCpu(), 0, null).
            addCell(ServerInfoColumnTitles.availableMemory, info.getAvailableJvmMemory(), 0, null).
            addCell(ServerInfoColumnTitles.freeDiskSpace, info.getAvailableDiskSpace(), 0, null);
    }

}
