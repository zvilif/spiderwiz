package org.spiderwiz.core;

import org.spiderwiz.zutils.ZDate;

/**
 * Handle import input that arrives from a remote application through RawImport.
 */
class RemoteImportHandler extends ImportHandler {
    private final String version;
    private final String remoteAddress;
    
    public RemoteImportHandler(RawImport rim) {
        super();
        setConnectedSince(ZDate.now());
        setName(rim.getImportName());
        setAppUUID(rim.getOriginUUID());
        version = rim.getServerVersion();
        remoteAddress = rim.getRemoteAddress();
        setRimChannel(rim.getDataChannel());
    }
    
    @Override
    protected String getServerVersion() {
        return version;
    }

    @Override
    public String getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    boolean isConnected() {
        return true;
    }

    @Override
    protected boolean exportObject(Object data) {
        // Transmit using a REX
        if (DataManager.getInstance().isProducingObject(Main.ObjectCodes.RawExport))
            RawExport.createObject(data.toString(), getName()).commit(getAppUUID().toString());
        return true;
    }
}
