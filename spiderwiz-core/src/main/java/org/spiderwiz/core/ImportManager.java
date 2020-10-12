package org.spiderwiz.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.UUID;
import org.spiderwiz.zutils.ZDictionary;
import org.spiderwiz.zutils.ZHashMap;

/**
 * Handles all import Server connections
 * @author Zvi 
 */
class ImportManager {
    private class ImportChannels extends ZHashMap<String, ImportHandler> {}
    private class ImportInfos extends TreeMap<String, ImportInfo> {}
    private final MyConfig settings;
    private final ImportChannels importChannels;
    
    private static final int MAX_IMPORTS = 99;

    ImportManager() {
        settings = Main.getMyConfig();
        importChannels = new ImportChannels();
    }
    
    /**
     * Initialize the object
     */
    void init() {
        reloadConfiguration();
    }
    
    /**
     * Reload Import Server info from the setting file.
     * Close existing connections that are not in the new settings or that their definitions have been changed.
     * Establish new connections or connections whose definitions have been changed.
     */
    void reloadConfiguration() {
        synchronized (importChannels) {
            for (int i = 1; i < MAX_IMPORTS; i++) {
                String key = MyConfig.IMPORT_PREFIX + i;
                String def = settings.getProperty(key);
                if (def != null && def.isEmpty())
                    def = null;
                ImportHandler channel = importChannels.get(key);
                if (channel != null && (def == null || !def.equals(channel.getDefString()))) {
                    channel.cleanup(false);
                    importChannels.remove(key);
                    channel = null;
                }
                if (def != null && channel == null) {
                    channel = Main.getInstance().createImportHandler();
                    channel.setDefString(def);
                    if (channel.configure(ZDictionary.parseParameterList(def), i))
                        importChannels.put(key, channel);
                    else
                        channel.cleanup(false);
                }
            }
        }
    }
    
    /**
     * Process import command received via RIM.
     * @param rim 
     */
    void processRIM(RawImport rim) {
        String key = rim.getOriginUUID() + rim.getImportName();
        ImportHandler channel = importChannels.get(key);
        // If a handler does not exist, create it.
        if (channel == null) {
            synchronized(importChannels){
                channel = new RemoteImportHandler(rim);
                importChannels.put(key, channel);
            }
        }
        // If command time is null this is a notification to close the handler.
        if (rim.getCommandTime() == null) {
            synchronized(importChannels){
                importChannels.remove(key);
            }
            return;
        }
        String line = rim.getImportCommand();
        if (line != null)
            channel.processLine(line, rim.getCommandTime());
    }
    
    /**
     * Remove RIM channels associated with a disconnected node.
     * @param uuid  UUID of the disconnected application.
     */
    void dropNode(UUID uuid) {
        ArrayList<ImportHandler> removed = new ArrayList<>();
        importChannels.lockRead();
        try {
            for (ImportHandler channel : importChannels.values()) {
                if (uuid.equals(channel.getAppUUID()))
                    removed.add(channel);
            }
        } finally {
            importChannels.unlockRead();
        }
        synchronized(importChannels){
            importChannels.removeAll(removed);
        }
    }
    
    /**
     * Transmit an object (i.e. the string returned by its exportObject() method to all import servers
     * @param obj
     * @param newID     null if this object is active, empty string if it has been removed, non-empty string if it has been renamed.
     * @return 
     */
    boolean transmitObject(DataObject obj, String newID) throws Exception{
        boolean sent = false;
        importChannels.lockRead();
        try {
            for (ImportHandler channel : importChannels.values()) {
                sent |= channel.transmitObject(obj, newID);
            }
        } finally {
            importChannels.unlockRead();
        }
        return sent;
    }
    
    /**
     * @return true if we are connected or set up to connect with any Import Server.
     */
    boolean isActive(){
        return importChannels.size() > 0;
    }
    
    /**
     * Flush the logs of all import channels
     */
    void flushAllLogs(){
        importChannels.lockRead();
        try {
            for (ImportHandler channel : importChannels.values())
                channel.flushLog();
        } finally {
            importChannels.unlockRead();
        }
    }
    
    /**
     * Get ImportInfo objects associated with all channels, ordered by import Name + IP address
     * @return a collection of requested objects
     */
    Collection<ImportInfo> getInfos() {
        ImportInfos infos = new ImportInfos();
        importChannels.lockRead();
        try {
            for (ImportHandler channel : importChannels.values()) {
                infos.put(channel.getName()+ channel.getRemoteAddress(), channel.getInfo());
            }
        } finally {
            importChannels.unlockRead();
        }
        return infos.values();
    }
    
    void resetCounters() {
        importChannels.lockRead();
        try {
            for (ImportHandler channel : importChannels.values()) {
                channel.resetCounters();
            }
        } finally {
            importChannels.unlockRead();
        }
    }

    /**
     * return true if the node is an ISDN and it has access to the specified import server, or to any import server
     * if importName is null.
     * @param importName   a string identifying the import server. If null, the method will return true if the node
     * has access to any import server.
     * @return
     */
    boolean hasImportAccess(String importName) {
        importChannels.lockRead();
        try {
            for (ImportHandler channel : importChannels.values()) {
                if (importName == null || importName.equalsIgnoreCase(channel.getName()))
                    return true;
            }
        } finally {
            importChannels.unlockRead();
        }
        return false;
    }

    /**
     * Cleanup object operation
     */
    void cleanup() {
        synchronized (importChannels) {
            for (ImportHandler channel : importChannels.values()) {
                channel.cleanup(false);
            }
            importChannels.clear();
        }
    }
    
    /**
     * return how many import channel are connected
     * @return 
     */
    int getNumberOfImportChannels() {
        return importChannels.size();
    }
}
