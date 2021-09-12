package org.spiderwiz.core;

import org.spiderwiz.admin.data.PageInfo.TableInfo;
import org.spiderwiz.admin.data.TableData;
import org.spiderwiz.zutils.ZDate;

/**
 *
 * @author @author  zvil<@author  zvil@@author  zvil.com>
 */
class AppInfo extends StatisticInfo {
    private final static int COMPRESSED = 2;
    private final Hub.RemoteNode node;

    public AppInfo(Hub.RemoteNode node) {
        super(1);           // Add a column for compressed input
        this.node = node;
    }

    public static TableInfo getTableStructure(String tableTitle, String tableService) {
        return new TableInfo(tableTitle, tableService, false).
            addColumn(CoreConsts.ApplicationInfo.Name, null, TableInfo.Style.LEFT, 0).
            addColumn(CoreConsts.ApplicationInfo.Version, null, TableInfo.Style.LEFT, 0).
            addColumn(CoreConsts.ApplicationInfo.CoreVersion, null, TableInfo.Style.LEFT, 0).
            addColumn(CoreConsts.ApplicationInfo.RemoteAddress, null, TableInfo.Style.LEFT, 0).
            addColumn(CoreConsts.ApplicationInfo.Input, CoreConsts.ApplicationInfo.ActivitySubTitle, TableInfo.Style.RIGHT,
                TableInfo.Summary.TOTAL).
            addColumn(CoreConsts.ApplicationInfo.Output, CoreConsts.ApplicationInfo.ActivitySubTitle, TableInfo.Style.RIGHT,
                TableInfo.Summary.TOTAL).
            addColumn(CoreConsts.ApplicationInfo.LastInput, null, TableInfo.Style.CENTER, 0).
            addColumn(CoreConsts.ApplicationInfo.LastOuput, null, TableInfo.Style.CENTER, 0).
            addColumn(CoreConsts.ApplicationInfo.UncompressedInput, CoreConsts.ApplicationInfo.BandwidthSubTitle,
                TableInfo.Style.RIGHT, TableInfo.Summary.TOTAL).
            addColumn(CoreConsts.ApplicationInfo.CompressedInput, CoreConsts.ApplicationInfo.BandwidthSubTitle,
                TableInfo.Style.RIGHT, TableInfo.Summary.TOTAL).
            addColumn(CoreConsts.ApplicationInfo.Status, null, TableInfo.Style.CENTER, 0).
            addColumn(CoreConsts.ApplicationInfo.Since, null, TableInfo.Style.CENTER, 0);
    }

    synchronized void addAdminTableRow(TableData td){
        boolean isAlertRow = !node.isConnected();
        td.addRow().
            addCell(CoreConsts.ApplicationInfo.Name, node.getAppName(), isAlertRow ? TableInfo.Style.ALERT : 0, getContextPath()).
            addCell(CoreConsts.ApplicationInfo.Version, node.getAppVersion(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.CoreVersion, node.getCoreVersion(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.RemoteAddress, node.getRemoteAddress(), isAlertRow ? TableInfo.Style.ALERT : 0,
                null).
            addCell(CoreConsts.ApplicationInfo.Input, getActivity(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.Output, getOutputActivity(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.LastInput, getLastActivity(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.LastOuput, getLastOuputActivity(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.UncompressedInput, getBandwidth(), isAlertRow ? TableInfo.Style.ALERT : 0,
                null).
            addCell(CoreConsts.ApplicationInfo.CompressedInput, getCompressedBandwidth(), isAlertRow ? TableInfo.Style.ALERT : 0,
                null).
            addCell(CoreConsts.ApplicationInfo.Status, getStatus(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.Since, getSince(), isAlertRow ? TableInfo.Style.ALERT : 0,
                null);
    }
    
    /**
     * Check if the activity on this node makes it relevant for displaying in SpiderAdmin.
     * @return 
     */
    synchronized boolean isRelevant() {
        ZDate inActivity = getLastActivity(IN);
        ZDate outActivity = getLastActivity(OUT);
        return inActivity != null && inActivity.elapsed() < ZDate.DAY || outActivity != null && outActivity.elapsed() < ZDate.DAY;
    }
    
    private String getContextPath() {
        return node.isConnected() ? "xadmin" + ":" + node.getUuid() : null;
    }

    private int getCompressedBandwidth() {
        return getBandwidth(COMPRESSED);
    }

    void updateCompressedInput(int rawSize) {
        updateActivity(COMPRESSED, ZDate.now(), rawSize);
    }

    private String getStatus() {
        return CoreConsts.ApplicationInfo.statusText[node.isConnected() ? 0 : 1];
    }

    private ZDate getSince() {
        return node.getSince();
    }
}
