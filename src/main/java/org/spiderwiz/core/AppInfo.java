package org.spiderwiz.core;

import org.spiderwiz.admin.xml.TableDataEx;
import org.spiderwiz.admin.xml.TableInfoEx;
import org.spiderwiz.zutils.ZDate;

/**
 *
 * @author Zvi <zvil@zvil.com>
 */
class AppInfo extends StatisticInfo {
    private final static int COMPRESSED = 2;
    private final Hub.RemoteNode node;

    public AppInfo(Hub.RemoteNode node) {
        super(1);           // Add a column for compressed input
        this.node = node;
    }

    public static TableInfoEx getTableStructure(String tableTitle, String tableService) {
        return new TableInfoEx(tableTitle, tableService, false).
            addColumn(CoreConsts.ApplicationInfo.Name, null, TableInfoEx.Style.LEFT, 0).
            addColumn(CoreConsts.ApplicationInfo.Version, null, TableInfoEx.Style.LEFT, 0).
            addColumn(CoreConsts.ApplicationInfo.CoreVersion, null, TableInfoEx.Style.LEFT, 0).
            addColumn(CoreConsts.ApplicationInfo.RemoteAddress, null, TableInfoEx.Style.LEFT, 0).
            addColumn(CoreConsts.ApplicationInfo.Input, CoreConsts.ApplicationInfo.ActivitySubTitle, TableInfoEx.Style.RIGHT,
                TableInfoEx.Summary.TOTAL).
            addColumn(CoreConsts.ApplicationInfo.Output, CoreConsts.ApplicationInfo.ActivitySubTitle, TableInfoEx.Style.RIGHT,
                TableInfoEx.Summary.TOTAL).
            addColumn(CoreConsts.ApplicationInfo.LastInput, null, TableInfoEx.Style.CENTER, 0).
            addColumn(CoreConsts.ApplicationInfo.LastOuput, null, TableInfoEx.Style.CENTER, 0).
            addColumn(CoreConsts.ApplicationInfo.UncompressedInput, CoreConsts.ApplicationInfo.BandwidthSubTitle,
                TableInfoEx.Style.RIGHT, TableInfoEx.Summary.TOTAL).
            addColumn(CoreConsts.ApplicationInfo.CompressedInput, CoreConsts.ApplicationInfo.BandwidthSubTitle,
                TableInfoEx.Style.RIGHT, TableInfoEx.Summary.TOTAL).
            addColumn(CoreConsts.ApplicationInfo.Status, null, TableInfoEx.Style.CENTER, 0).
            addColumn(CoreConsts.ApplicationInfo.Since, null, TableInfoEx.Style.CENTER, 0);
    }

    synchronized void addAdminTableRow(TableDataEx td){
        boolean isAlertRow = !node.isConnected();
        td.addRow().
            addCell(CoreConsts.ApplicationInfo.Name, node.getAppName(), isAlertRow ? TableInfoEx.Style.ALERT : 0, getContextPath()).
            addCell(CoreConsts.ApplicationInfo.Version, node.getAppVersion(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.CoreVersion, node.getCoreVersion(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.RemoteAddress, node.getRemoteAddress(), isAlertRow ? TableInfoEx.Style.ALERT : 0,
                null).
            addCell(CoreConsts.ApplicationInfo.Input, getActivity(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.Output, getOutputActivity(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.LastInput, getLastActivity(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.LastOuput, getLastOuputActivity(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.UncompressedInput, getBandwidth(), isAlertRow ? TableInfoEx.Style.ALERT : 0,
                null).
            addCell(CoreConsts.ApplicationInfo.CompressedInput, getCompressedBandwidth(), isAlertRow ? TableInfoEx.Style.ALERT : 0,
                null).
            addCell(CoreConsts.ApplicationInfo.Status, getStatus(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.ApplicationInfo.Since, getSince(), isAlertRow ? TableInfoEx.Style.ALERT : 0,
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

    private String getSince() {
        ZDate since = node.getSince();
        return since == null ? null : since.format(ZDate.FULL_DATE);
    }
}
