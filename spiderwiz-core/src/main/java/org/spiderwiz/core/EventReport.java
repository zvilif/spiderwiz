package org.spiderwiz.core;

import org.spiderwiz.annotation.WizField;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZUtilities;

/**
 * Handles events reported by remote nodes
 * @author zvil
 */
class EventReport extends DataObject {
    public final static String ObjectCode = Main.ObjectCodes.EventReport;
    
    @WizField private String appName = null;
    @WizField private String appAddress = null;
    @WizField private String message = null;
    @WizField private String stackTrace = null;
    @WizField private String additionalInfo = null;
    @WizField private boolean alert = true;
    @WizField(format = ZDate.FULL_TIMESTAMP) private ZDate eventTime = null;
    
    final void commitEvent(String message, String additionalInfo, String stackTrace, boolean alert)
    {
        appName = Main.getInstance().getAppName();
        appAddress = ZUtilities.getMyIpAddress();
        this.message = message;
        this.additionalInfo = additionalInfo;
        this.stackTrace = stackTrace;
        this.alert = alert;
        eventTime = ZDate.now();
        commit();
    }

    @Override
    public void commit() {
        try {
            super.commit();
        } catch (Exception ex) {
            Main.getLogger().logException(ex);
        }
    }

    /**
     * We handle events on this object by sending appropriate alert mails
     * @return 
     */
    @Override
    protected boolean onEvent() {
        Main.getInstance().getAlertMail().sendNotificationMail(
            appName, appAddress, message, 
            String.format(CoreConsts.AlertMail.SENT_BY, Main.getInstance().getAppName(),  ZUtilities.getMyIpAddress()),
            additionalInfo, stackTrace, eventTime, alert);
        return true;
    }

    @Override
    protected String getParentCode() {
        return null;
    }

    @Override
    protected boolean isDisposable() {
        return true;
    }

}
