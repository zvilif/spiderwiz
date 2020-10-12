package org.spiderwiz.core;

import java.lang.reflect.Constructor;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZDictionary;
import org.spiderwiz.zutils.ZHtml;
import org.spiderwiz.zutils.ZHtml.Document;
import org.spiderwiz.zutils.ZMail;
import org.spiderwiz.zutils.ZSMTPMail;

/**
 * @author Zvi 
 */
class AlertMail {

    private final MyConfig config;
    private final ZMail zmail;
    
    public AlertMail() {
        ZMail obj = null;
        config = Main.getMyConfig();
        String mailConfig = config.getProperty(MyConfig.MAIL_SYSTEM);
        if (mailConfig != null) {
            String customClass;
            ZDictionary configParams = ZDictionary.parseParameterList(mailConfig);
            if ((customClass = configParams.get(CoreConsts.AlertMail.CLASS)) != null) {
                try {
                    obj = ZMail.class.cast(Class.forName(customClass).getDeclaredConstructor().newInstance());
                } catch (Exception ex) {
                    String msg = String.format(CoreConsts.AlertMail.MAIL_CLASS_FAILED, customClass, ex.toString());
                    Main.getLogger().logEvent(msg);
                }
            } else if (configParams.containsKey(CoreConsts.AlertMail.SMTP))
                obj = new ZSMTPMail();
        }
        zmail = obj;
    }

    void sendNotificationMail (String appName, String appAddress, String msg, String sendingApp, String additionalInfo,
        String stackTrace, ZDate eventTime, boolean alert)
    {
        if (zmail == null)
            return;
        String from = config.getProperty(MyConfig.ALERT_MAIL_FROM);
        String to;
        String cc;
        boolean exception = stackTrace != null;
        to = config.getProperty(exception ? MyConfig.EXCEPTION_MAIL_TO : MyConfig.ALERT_MAIL_TO);
        if (to == null)
            return;
        cc = config.getProperty(exception ? MyConfig.EXCEPTION_MAIL_CC : MyConfig.ALERT_MAIL_CC);
        String subject = String.format(
            stackTrace != null ? CoreConsts.AlertMail.APPLICATION_EXCEPTION_MESSAGE : alert ?
                CoreConsts.AlertMail.APPLICATION_ALERT_MESSAGE : CoreConsts.AlertMail.APPLICATION_NOTIFICATION_MESSAGE,
            appName, appAddress);
        Document doc = ZHtml.createDocument();
        String style = alert | exception ? CoreConsts.AlertMail.ALERT_FONT : CoreConsts.AlertMail.OK_FONT;
        doc.addHeading(subject, 1).addStyle(style);
        if (msg != null)
            doc.addHeading(msg, 1).addStyle(style);
        if (eventTime != null)
            doc.addHeading(String.format(CoreConsts.AlertMail.ALERT_MAIL_BODY2, eventTime.format(ZDate.FULL_DATE_MILLISECONDS)), 3);
        if (sendingApp != null)
            doc.addHeading(sendingApp, 2);
        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            doc.addHeading(CoreConsts.AlertMail.ADDITIONAL_INFO, 3);
            doc.addParagraph(additionalInfo);
        }
        if (stackTrace != null && !stackTrace.isEmpty()) {
            doc.addHeading(CoreConsts.AlertMail.EXCEPTION_BODY4, 3);
            doc.addParagraph(stackTrace);
        }
        try {
            String configString = config.getProperty(MyConfig.MAIL_SYSTEM);
            if (configString != null) {
                zmail.configure(ZDictionary.parseParameterList(configString));
                zmail.send(from, to, cc, subject, doc.toHtml(), true, false);
            }
        } catch (Exception ex) {
            Main.getLogger().logException(ex);
        }
    }
}
