/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spiderwiz.core;

import java.text.DecimalFormat;
import org.spiderwiz.admin.xml.TableDataEx;
import org.spiderwiz.admin.xml.TableInfoEx;
import org.spiderwiz.zutils.ZDate;

/**
 *
 * @author Zvi 
 */
class DataNodeInfo extends StatisticInfo {
    private final static int COMPRESSED = 2;
    private final static int maxNormalAbsClockDiff = 40;

    private final DataHandler dataChannel;
    private int errors = 0;
    private long clockDiff = 0;

    DataNodeInfo(DataHandler dataSocket) {
        super(1);           // Add a column for compressed input
        this.dataChannel = dataSocket;
    }

    public static TableInfoEx getTableStructure(String tableTitle, String tableService) {
        return new TableInfoEx(tableTitle, tableService, false).
            addColumn(CoreConsts.DataNodeInfo.Name, null, TableInfoEx.Style.LEFT, 0).
            addColumn(CoreConsts.DataNodeInfo.Version, null, TableInfoEx.Style.LEFT, 0).
            addColumn(CoreConsts.DataNodeInfo.CoreVersion, null, TableInfoEx.Style.LEFT, 0).
            addColumn(CoreConsts.DataNodeInfo.RemoteAddress, null, TableInfoEx.Style.LEFT, 0).
            addColumn(CoreConsts.DataNodeInfo.Input, CoreConsts.DataNodeInfo.ActivitySubTitle, TableInfoEx.Style.RIGHT,
                TableInfoEx.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.Output, CoreConsts.DataNodeInfo.ActivitySubTitle, TableInfoEx.Style.RIGHT,
                TableInfoEx.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.LastInput, null, TableInfoEx.Style.CENTER, 0).
            addColumn(CoreConsts.DataNodeInfo.AverageDelay, CoreConsts.DataNodeInfo.AverageDelaySubTitle,
                TableInfoEx.Style.RIGHT, 0).
            addColumn(CoreConsts.DataNodeInfo.MaximumDelay, null, TableInfoEx.Style.RIGHT, TableInfoEx.Summary.MAX).
            addColumn(CoreConsts.DataNodeInfo.ClockDifference, CoreConsts.DataNodeInfo.ClockDiffSubTitle,
                TableInfoEx.Style.RIGHT, 0).
            addColumn(CoreConsts.DataNodeInfo.UncompressedInput, CoreConsts.DataNodeInfo.BandwidthSubTitle,
                TableInfoEx.Style.RIGHT, TableInfoEx.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.InBandwidth, CoreConsts.DataNodeInfo.BandwidthSubTitle,
                TableInfoEx.Style.RIGHT, TableInfoEx.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.OutBandwidth, CoreConsts.DataNodeInfo.BandwidthSubTitle,
                TableInfoEx.Style.RIGHT, TableInfoEx.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.Status, null, TableInfoEx.Style.CENTER, 0).
            addColumn(CoreConsts.DataNodeInfo.ConnectedSince, null, TableInfoEx.Style.CENTER, 0).
            addColumn(CoreConsts.DataNodeInfo.Errors, null, TableInfoEx.Style.RIGHT, TableInfoEx.Summary.TOTAL);
    }

    String getDnName() {
        return dataChannel.getAppName();
    }
    
    String getVersion() {
        return dataChannel.getAppVersion();
    }
    
    String getCoreVersion() {
        return dataChannel.getCoreVersion();
    }
    
    String getRemoteAddress() {
        return dataChannel.getRemoteAddress();
    }

    synchronized int getErrors() {
        return errors;
    }

    synchronized float getClockDiff() {
        return Math.round(clockDiff / 100F) / 10F;
    }

    synchronized void setClockDiff(long clockDiff) {
        this.clockDiff = clockDiff;
    }

    String getContextPath() {
        return dataChannel.getContextPath();
    }
    
    String getStatus() {
        return CoreConsts.DataNodeInfo.statusText[dataChannel.getStatus().ordinal()];
    }

    String getConnectedSince() {
        if (dataChannel == null)
            return null;
        ZDate connectedSince = dataChannel.getConnectedSince();
        return connectedSince == null ? null : connectedSince.format(ZDate.FULL_DATE);
    }
    // Handle error reporting if necessary
    synchronized void updateErrors (){
        ++errors;
    }
    
    synchronized void addAdminTableRow(TableDataEx td){
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        boolean isAlertRow = !"OK".equalsIgnoreCase(getStatus());
        td.addRow().
            addCell(CoreConsts.DataNodeInfo.Name, getDnName(), isAlertRow ? TableInfoEx.Style.ALERT : 0, getContextPath()).
            addCell(CoreConsts.DataNodeInfo.Version, getVersion(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.CoreVersion, getCoreVersion(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.RemoteAddress, getRemoteAddress(), isAlertRow ? TableInfoEx.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.Input, getActivity(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.Output, getOutputActivity(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.LastInput, getLastActivity(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.AverageDelay, df.format(getAvgDelay()), isAlertRow ? TableInfoEx.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.MaximumDelay, getMaxDelay(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.ClockDifference, df.format(getClockDiff()),
                isAlertRow || Math.abs(getClockDiff()) >= maxNormalAbsClockDiff ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.UncompressedInput, getBandwidth(), isAlertRow ? TableInfoEx.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.InBandwidth, getCompressedBandwidth(), isAlertRow ? TableInfoEx.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.OutBandwidth, getOutputBandwidth(), isAlertRow ? TableInfoEx.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.Status, getStatus(), isAlertRow ? TableInfoEx.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.ConnectedSince, getConnectedSince(), isAlertRow ? TableInfoEx.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.Errors, getErrors(), isAlertRow || getErrors() > 0 ? TableInfoEx.Style.ALERT : 0,
                null);
    }
    
    public int getCompressedBandwidth() {
        return getBandwidth(COMPRESSED);
    }
    
    void updateCompressedInput(int rawSize) {
        updateActivity(COMPRESSED, ZDate.now(), rawSize);
    }

    @Override
    protected synchronized long getClockSync() {
        return clockDiff;
    }

    @Override
    public synchronized void reset() {
        errors = 0;
        super.reset();
    }
}
