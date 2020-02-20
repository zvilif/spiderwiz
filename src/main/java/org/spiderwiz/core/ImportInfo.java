/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spiderwiz.core;

import org.spiderwiz.admin.xml.TableDataEx;
import org.spiderwiz.admin.xml.TableInfoEx;
import org.spiderwiz.zutils.ZDate;

/**
 *
 * @author Zvi 
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

    public static TableInfoEx getTableStructure(String tableTitle, String tableService) {
        return new TableInfoEx(tableTitle, tableService, false).
            addColumn(ImportColumnTitles.Name, null, TableInfoEx.Style.LEFT, 0).
            addColumn(ImportColumnTitles.Version, null, TableInfoEx.Style.LEFT, 0).
            addColumn(ImportColumnTitles.Application, null, TableInfoEx.Style.LEFT, 0).
            addColumn(ImportColumnTitles.RemoteAddress, null, TableInfoEx.Style.LEFT, 0).
            addColumn(ImportColumnTitles.Activity, ImportColumnTitles.ActivitySubTitle, TableInfoEx.Style.RIGHT,
                TableInfoEx.Summary.TOTAL).
            addColumn(ImportColumnTitles.LastUpdate, null, TableInfoEx.Style.CENTER, 0).
            addColumn(ImportColumnTitles.Bandwidth, ImportColumnTitles.BandwidthSubTitle, TableInfoEx.Style.RIGHT,
                TableInfoEx.Summary.TOTAL).
            addColumn(ImportColumnTitles.Status, null, TableInfoEx.Style.CENTER, 0).
            addColumn(ImportColumnTitles.ConnectedSince, null, TableInfoEx.Style.CENTER, 0).
            addColumn(ImportColumnTitles.Errors, null, TableInfoEx.Style.RIGHT, TableInfoEx.Summary.TOTAL);
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

    String getConnectedSince() {
        if (importChannel == null)
            return null;
        ZDate connectedSince = importChannel.getConnectedSince();
        return connectedSince == null ? null : connectedSince.format(ZDate.FULL_DATE);
    }
    // Handle error reporting if necessary
    public synchronized void updateErrors (){
        ++errors;
    }
    
    public synchronized void addAdminTableRow(TableDataEx td) {
        boolean isAlertRow = getActivity() == 0 || !"OK".equals(getStatus().toUpperCase());
        td.addRow().
            addCell(ImportColumnTitles.Name, getImportName(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Version, getVersion(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Application, getAppName(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.RemoteAddress, getRemoteAddress(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Activity, getActivity(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.LastUpdate, getLastActivity(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Bandwidth, getBandwidth(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Status, getStatus(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.ConnectedSince, getConnectedSince(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(ImportColumnTitles.Errors, getErrors(), isAlertRow || getErrors() > 0 ? TableInfoEx.Style.ALERT : 0, null);
    }
    
    @Override
    public synchronized void reset() {
        errors = 0;
        super.reset();
    }
}
