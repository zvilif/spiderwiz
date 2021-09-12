package org.spiderwiz.core;

import org.spiderwiz.annotation.WizField;
import org.spiderwiz.zutils.ZDate;

/**
 * The class supports delivery of original import command and import identifier.
 * @author @author  zvil
 */
class RawImport extends DataObject {
    public final static String ObjectCode = Main.ObjectCodes.RawImport;
    @WizField private String importName = null;
    @WizField private String remoteAddress = null;
    @WizField private String serverVersion = null;
    @WizField private String importCommand = null;
    @WizField private ZDate commandTime = null;
    
    public String getImportCommand() {
        return importCommand;
    }

    public String getImportName() {
        return importName;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public ZDate getCommandTime() {
        return commandTime;
    }

    /**
     * If we are producers of RIC then create a RIC object for the given command and commit it.
     * @param command
     * @param ts
     * @param handler 
     */
    static void createAndCommit(String command, ZDate ts, ImportHandler handler) {
        if (DataManager.getInstance().isProducingObject(ObjectCode)) {
            DataObject ric = Main.getInstance().createDataObject(ObjectCode);
            try {
                ric.importObject(command, handler, ts);
            } catch (Exception ex) {
                Main.getInstance().sendExceptionMail(ex, CoreConsts.AlertMail.WHEN_PARSING_EMPTY_RIC, handler.getName(), false);
            }
            ric.commit();
        }
    }
    
    @Override
    protected boolean onEvent() {
        // onEvent() of RIC simulates an import server. Do it only if the object is arriving from another node (and not if we are its
        // producers).
        if (getDataChannel() != null)
            Hub.getInstance().getImportManager().processRIM(this);
        return true;
    }

    @Override
    protected String[] importObject(Object cmd, ImportHandler channel, ZDate commandTime) throws Exception {
        if (channel != null) {
            importName = channel.getName();
            remoteAddress = channel.getRemoteAddress();
            serverVersion = channel.getServerVersion();
            this.commandTime = commandTime;
        }
        if (cmd != null) {
            importCommand = cmd.toString();
            return new String[0];
        }
        return null;
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
