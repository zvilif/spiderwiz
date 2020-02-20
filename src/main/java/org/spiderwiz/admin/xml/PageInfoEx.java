package org.spiderwiz.admin.xml;

import java.util.List;
import org.spiderwiz.admin.xml.PageInfo.TableInfo;

/**
 * Extends PageInfo by providing methods to populate it with content.
 *
 * @author Zvi 
 */
public class PageInfoEx extends PageInfo {
    public final static String RELOAD_SETTINGS = "reloadSettings";
    public final static String FLUSH_LOGS = "flushLogs";

    /**
     * Construct a default PageInfo object
     */
    public PageInfoEx() {
        this(true, true);
    }

    /**
     * Construct a PageInfo object with initial values
     *
     * @param showUpdateSettings true if page should show 'Reload settings'
     * button
     * @param showFlushLogs true if page should show 'Flush logs' button
     */
    public PageInfoEx(boolean showUpdateSettings, boolean showFlushLogs) {
        setShowUpdateSettings(showUpdateSettings);
        setShowFlushLogs(showFlushLogs);
        setShowReloadSettings(false);
    }

    /**
     * Add a custom button to page
     *
     * @param text Text to show on button
     * @param service Service to call on server (<service> part of the URI) when
     * button is clicked
     * @param verify notify if browser should verify the user button click,
     * before sending the request to server.
     * @return the object
     */
    public PageInfoEx addButton(String text, String service, boolean verify) {
        List<ButtonInfo> buttonList = getButtonInfo();
        ButtonInfo grButton = new ButtonInfo();
        grButton.setText(text);
        grButton.setService(service);
        grButton.setVerify(verify);
        buttonList.add(grButton);
        return this;
    }

    /**
     * Add a custom button to page with default "false" value for "verify"
     *
     * @param text Text to show on button
     * @param service Service to call on server (<service> part of the URI) when
     * button is clicked
     * before sending the request to server.
     * @return the object
     */
    public PageInfoEx addButton(String text, String service) {
        return addButton(text, service, false);
    }
    /**
     * Add a table to the page
     *
     * @param tableInfo Provides information about the table
     * @return the object
     */
    public PageInfoEx addTable(TableInfo tableInfo) {
        getTableInfo().add(tableInfo);
        return this;
    }

}
