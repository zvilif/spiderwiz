/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spiderwiz.core;

import org.spiderwiz.admin.data.PageInfo.TableInfo;
import org.spiderwiz.admin.data.TableData;
import org.spiderwiz.zutils.ZDate;

/**
 *
 * @author zvil
 */
class ImportInfo extends StatisticInfo {
    private class ImportColumnTitles {
        static final String Name = "Name";
        static final String Version = "Version";
        static final String Application = "Application";
        static final String RemoteAddress = "Remote address";
        static final String Activity = "Activity";
        static final String ActivitySubTitle = "updates per minute";
        static final String LastUpdate = "Last update";
        static final String Bandwidth = "Bandwidth";
        static final String BandwidthSubTitle = "average bytes p/s";
        static final String Status = "Status";
        static final String ConnectedSince = "Connected since";
        static final String Errors = "Errors";
    }

    private final ImportHandler importChannel;
    private int errors = 0;

    public ImportInfo(ImportHandler importChannel) {
        this.importChannel = importChannel;
    }

    public static TableInfo getTableStructure(String tableTitle, String tableService) {
        return new TableInfo(tableTitle, tableService, false).
            addColumn(ImportColumnTitles.Name, null, TableInfo.Style.LEFT, 0).
            addColumn(ImportColumnTitles.Version, null, TableInfo.Style.LEFT, 0).
            addColumn(ImportColumnTitles.Application, null, TableInfo.Style.LEFT, 0).
            addColumn(ImportColumnTitles.RemoteAddress, null, TableInfo.Style.LEFT, 0).
            addColumn(ImportColumnTitles.Activity, ImportColumnTitles.ActivitySubTitle, TableInfo.Style.RIGHT,
                TableInfo.Summary.TOTAL).
            addColumn(ImportColumnTitles.LastUpdate, null, TableInfo.Style.CENTER, 0).
            addColumn(ImportColumnTitles.Bandwidth, ImportColumnTitles.BandwidthSubTitle, TableInfo.Style.RIGHT,
                TableInfo.Summary.TOTAL).
            addColumn(ImportColumnTitles.Status, null, TableInfo.Style.CENTER, 0).
            addColumn(ImportColumnTitles.ConnectedSince, null, TableInfo.Style.CENTER, 0).
            addColumn(ImportColumnTitles.Errors, null, TableInfo.Style.RIGHT, TableInfo.Summary.TOTAL);
    }

    String getImportName() {
        return importChannel.getName();
    }
    
    String getVersion() {
        return importChannel.getServerVersion();
    }
    
    private String getAppName() {
        return importChannel.getAppName();
    }
    
    String getRemoteAddress() {
        return importChannel.getRemoteAddress();
    }

    synchronized int getErrors() {
        return errors;
    }

    String getStatus() {
        return importChannel.getStatus();
    }

    ZDate getConnectedSince() {
        if (importChannel == null)
            return null;
        return importChannel.getConnectedSince();
    }
    
    // Handle error reporting if necessary
    public synchronized void updateErrors (){
        ++errors;
    }
    
    public synchronized void addAdminTableRow(TableData td) {
        boolean isAlertRow = getActivity() == 0 || !"OK".equals(getStatus().toUpperCase());
        td.addRow().
            addCell(ImportColumnTitles.Name, getImportName(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Version, getVersion(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Application, getAppName(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.RemoteAddress, getRemoteAddress(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Activity, getActivity(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.LastUpdate, getLastActivity(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Bandwidth, getBandwidth(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Status, getStatus(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.ConnectedSince, getConnectedSince(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Errors, getErrors(), isAlertRow || getErrors() > 0 ? TableInfo.Style.ALERT : 0, null);
    }
    
    @Override
    public synchronized void reset() {
        errors = 0;
        super.reset();
    }
}
