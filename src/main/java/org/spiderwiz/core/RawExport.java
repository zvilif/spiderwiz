package org.spiderwiz.core;

import org.spiderwiz.annotation.WizField;
import org.spiderwiz.zutils.Ztrings;

/**
 * Handles upward messages to import servers originated by remote data nodes.
 * @author Zvi 
 */
class RawExport extends DataObject {
    public final static String ObjectCode = Main.ObjectCodes.RawExport;
    
    @WizField private String message = null;          // The message to send to an import server
    @WizField private String importName = null;       // The import server name to send to
    
    /**
     * Create a RawExport object with the given parameter as its 'message' field
     * @param message
     * @return 
     */
    static RawExport createObject(String message, String importName) {
        RawExport obj = (RawExport)Main.getInstance().createDataObject(ObjectCode);
        obj.message = message;
        obj.importName = importName;
        return obj;
    }

    @Override
    protected String getParentCode() {
        return null;
    }

    /**
     * Send the message to the appropriate import server.
     * @param channel
     * @return 
     */
    @Override
    protected String exportObject(ImportHandler channel) {
        // Check if the message shall be exported to the given channel.
        return channel.getAppUUID() == null && channel.getName().equals(importName) ? message : null;
    }

    /**
     * REX's onEvent() activates the import manager's transmitObject() method, which will cause the message to be sent to
     * all imports.
     * @return 
     */
    @Override
    protected boolean onEvent() {
        try {
            return Hub.getInstance().getImportManager().transmitObject(this);
        } catch (Exception ex) {
            Main.getInstance().sendExceptionMail(ex, CoreConsts.AlertMail.EXCEPTION_TRANSMIT_RIM,
                String.format(CoreConsts.AlertMail.MESSAGE_TO_SEND, message), false);
            return false;
        }
    }

    @Override
    protected boolean isDisposable() {
        return true;
    }
}
