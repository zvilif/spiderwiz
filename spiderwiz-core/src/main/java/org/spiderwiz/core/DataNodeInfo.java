/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spiderwiz.core;

import java.text.DecimalFormat;
import org.spiderwiz.admin.data.PageInfo.TableInfo;
import org.spiderwiz.admin.data.TableData;
import org.spiderwiz.zutils.ZDate;

/**
 *
 * @author @author  zvil
 */
class DataNodeInfo extends StatisticInfo {
    private final static int COMPRESSED = 2;
    private final static int MAX_NORMAL_ABS_CLOCK_DIFF = 40;

    private final DataHandler dataChannel;
    private int errors = 0;
    private long clockDiff = 0;

    DataNodeInfo(DataHandler dataSocket) {
        super(1);           // Add a column for compressed input
        this.dataChannel = dataSocket;
    }

    public static TableInfo getTableStructure(String tableTitle, String tableService) {
        return new TableInfo(tableTitle, tableService, false).
            addColumn(CoreConsts.DataNodeInfo.Name, null, TableInfo.Style.LEFT, 0).
            addColumn(CoreConsts.DataNodeInfo.Version, null, TableInfo.Style.LEFT, 0).
            addColumn(CoreConsts.DataNodeInfo.CoreVersion, null, TableInfo.Style.LEFT, 0).
            addColumn(CoreConsts.DataNodeInfo.RemoteAddress, null, TableInfo.Style.LEFT, 0).
            addColumn(CoreConsts.DataNodeInfo.Input, CoreConsts.DataNodeInfo.ActivitySubTitle, TableInfo.Style.RIGHT,
                TableInfo.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.Output, CoreConsts.DataNodeInfo.ActivitySubTitle, TableInfo.Style.RIGHT,
                TableInfo.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.LastInput, null, TableInfo.Style.CENTER, 0).
            addColumn(CoreConsts.DataNodeInfo.AverageDelay, CoreConsts.DataNodeInfo.AverageDelaySubTitle,
                TableInfo.Style.RIGHT, 0).
            addColumn(CoreConsts.DataNodeInfo.MaximumDelay, null, TableInfo.Style.RIGHT, TableInfo.Summary.MAX).
            addColumn(CoreConsts.DataNodeInfo.ClockDifference, CoreConsts.DataNodeInfo.ClockDiffSubTitle,
                TableInfo.Style.RIGHT, 0).
            addColumn(CoreConsts.DataNodeInfo.UncompressedInput, CoreConsts.DataNodeInfo.BandwidthSubTitle,
                TableInfo.Style.RIGHT, TableInfo.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.InBandwidth, CoreConsts.DataNodeInfo.BandwidthSubTitle,
                TableInfo.Style.RIGHT, TableInfo.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.OutBandwidth, CoreConsts.DataNodeInfo.BandwidthSubTitle,
                TableInfo.Style.RIGHT, TableInfo.Summary.TOTAL).
            addColumn(CoreConsts.DataNodeInfo.Status, null, TableInfo.Style.CENTER, 0).
            addColumn(CoreConsts.DataNodeInfo.ConnectedSince, null, TableInfo.Style.CENTER, 0).
            addColumn(CoreConsts.DataNodeInfo.Errors, null, TableInfo.Style.RIGHT, TableInfo.Summary.TOTAL);
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

    ZDate getConnectedSince() {
        if (dataChannel == null)
            return null;
        return dataChannel.getConnectedSince();
    }
    
    // Handle error reporting if necessary
    synchronized void updateErrors (){
        ++errors;
    }
    
    synchronized void addAdminTableRow(TableData td){
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        boolean isAlertRow = !"OK".equalsIgnoreCase(getStatus());
        td.addRow().
            addCell(CoreConsts.DataNodeInfo.Name, getDnName(), isAlertRow ? TableInfo.Style.ALERT : 0, getContextPath()).
            addCell(CoreConsts.DataNodeInfo.Version, getVersion(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.CoreVersion, getCoreVersion(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.RemoteAddress, getRemoteAddress(), isAlertRow ? TableInfo.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.Input, getActivity(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.Output, getOutputActivity(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.LastInput, getLastActivity(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.AverageDelay, df.format(getAvgDelay()), isAlertRow ? TableInfo.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.MaximumDelay, getMaxDelay(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.ClockDifference, df.format(getClockDiff()),
                isAlertRow || Math.abs(getClockDiff()) >= MAX_NORMAL_ABS_CLOCK_DIFF ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.UncompressedInput, getBandwidth(), isAlertRow ? TableInfo.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.InBandwidth, getCompressedBandwidth(), isAlertRow ? TableInfo.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.OutBandwidth, getOutputBandwidth(), isAlertRow ? TableInfo.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.Status, getStatus(), isAlertRow ? TableInfo.Style.ALERT : 0, null).
            addCell(CoreConsts.DataNodeInfo.ConnectedSince, getConnectedSince(), isAlertRow ? TableInfo.Style.ALERT : 0,
                null).
            addCell(CoreConsts.DataNodeInfo.Errors, getErrors(), isAlertRow || getErrors() > 0 ? TableInfo.Style.ALERT : 0,
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
