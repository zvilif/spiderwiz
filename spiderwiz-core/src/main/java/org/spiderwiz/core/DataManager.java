package org.spiderwiz.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZUtilities;
import org.spiderwiz.zutils.Ztrings;

/**
 * Top object that manages or redirect data.
 * @author zvil
 */
final class DataManager {
    /**
     * Use this class to determine object hierarchy for parsing.
     */
    private class ParsingHierarchyMap extends HashMap<String, String[]> {
        void init(Collection<String> codes) {
            if (codes == null)
                return;
            codes.forEach((code) -> {
                DataObject obj = Main.getInstance().createDataObject(code);
                if (obj != null) {
                    // Add parsing hierarchy and an EventDispatcher for this object type 
                    put(code, obj.getParsingHierarchy());
                    eds.addDispatcher(code, obj);
                }
            });
        }
    }
    
    /**
     * A map of EventDispatcher objects for each data object type.
     */
    private class EventDispatchers extends HashMap<String, EventDispatcher> {
        /**
         * Add a dispatcher for the given data object, after determining whether the object is consumed in lossless mode.
         * @param objCode   given object code
         * @param obj       object instance
         */
        void addDispatcher(String objCode, DataObject obj) {
            put(objCode, obj.createEventDispatcher(consumedObjects.get(objCode) != null));
        }
        
        void cleanup() {
            values().forEach((ed) -> {
                ed.cleanup();
            });
        }
    }
    
    private static DataManager myObject = null;
    private final QueryManager queryManager;
    private final Ztrings producedObjects;
    private final ConsumedObjectMap consumedObjects;
    private final ParsingHierarchyMap parsingHierarchy;
    private final RootObject rootObject;
    private final EventDispatchers eds;

    DataManager(QueryManager queryManager) {
        this.queryManager = queryManager;
        producedObjects = new Ztrings();
        parsingHierarchy = new ParsingHierarchyMap();
        rootObject = new RootObject();
        consumedObjects = new ConsumedObjectMap() {
            @Override
            Boolean getLosslessController(String objCode) {
                return true;
            }
        };
        eds = new EventDispatchers();
    }

    static DataManager getInstance() {
        return myObject;
    }
    
    RootObject getRootObject() {
        return rootObject;
    }
    
    /**
     * Initialize the object
     */
    void init(Collection<String> allObjectCodes) {
        myObject = this;
        producedObjects.addAll(ZUtilities.arrayToList(Main.getInstance().getProducedObjects()));
        consumedObjects.fromArray(Main.getInstance().getConsumedObjects(), null);
        String xAdminCode;
        if ((xAdminCode = Main.getInstance().getxAdminQueryCode()) != null) {
            producedObjects.add(xAdminCode);
            consumedObjects.put(xAdminCode, null);
        }
        parsingHierarchy.init(allObjectCodes);
    }

    Collection<String> getConsumedObjects() {
        return consumedObjects.keySet();
    }

    /**
     * @return the consumed object codes as a semicolon separated list
     */
    String getConsumedObjectsAsString() {
        return consumedObjects.getAsString();
    }

    Ztrings getProducedObjects() {
        return producedObjects;
    }
    
    /**
     * Informs whether this node is a producer of the given object code.
     * @param cmd   The object code to inform about.
     * @return      True if this node is a producer of objects of type 'objCode'.
     */
    boolean isProducingObject(String cmd){
        return producedObjects.contains(cmd) || producedObjects.contains(cmd + DataObject.Lossless);
    }
    
    /**
     * Informs whether this node is a consumer of the given object code.
     * @param cmd   The object code to inform about.
     * @return      True if this node is a producer of objects of type 'objCode'.
     */
    boolean isConsumingObject(String cmd){
        return consumedObjects.containsKey(cmd);
    }
    
    /**
     * Get EventDispatcher for a specific object type.
     * @param objCode
     * @return 
     */
    EventDispatcher getEventDispatcher(String objCode) {
        return eds.get(objCode);
    }

    /**
     * Process an object arriving from an imported Server
     *
     * @param importObject      the object
     * @param importChannel     the channel object representing the sending server
     * @param ricObject         If not null the function is called from RIC and this is the RIC object.
     * @param commandTime       the time the object is retrieved from the import server
     * @param ricChannel        in case the import object arrives through RawImport (RIC) this contains the DataHandler
                                object originating the RIC.
     * @throws java.text.ParseException
     */
    void processImportCommand(Object importObject, ImportHandler importChannel, ZDate commandTime) throws Exception
    {
        // Try first processing the command by each of the pending queries.
        if (queryManager.processImportQuery(importObject, importChannel, commandTime))
            return;
        
        // Try to use produced objects.
        for (String code : Main.getInstance().getProducedObjects()) {
            code = DataObject.stripLossless(code);
            // RIC shall not create another RIC.
            if(code.equals(RawImport.ObjectCode) && importChannel.getRimChannel() != null)
                continue;
                
            // For each produced object create the object and parse the command into it. If the command is relevant for the object
            // process the object and then if necessary propagate the object to other nodes.
            DataObject obj = Main.getInstance().createDataObject(code);
            if (obj != null) {
                String keyList[] = obj.importObject(importObject, importChannel, commandTime);
                if (keyList != null) {
                    String keys = Serializer.escapeAndConcatenate("|", keyList);
                    String vals = obj.serialize();
                    boolean removed = obj.isObsolete(); // is obsolete if importObject() removed it.
                    obj = processCommand(
                        removed ? DataObject.RemoveIndicator : "$",
                        obj.getObjectCode(),
                        keys,
                        removed ? null : vals,
                        Main.getInstance().getAppUUID(),
                        null,
                        importChannel.getRimChannel(), importObject.toString(), commandTime, null, 0
                    );
                    if (obj != null)
                        obj.propagate(false, null, null);
                }
            }
        }
    }

    /**
     * Process a reset command
     *
     * @param resetter A Resetter object managing the reset of a specific object type.
     */
    void processReset(Resetter resetter) {
        // Try first the query list
        if (Hub.getInstance().processQueryReset(resetter))
            return;

        // Reset the object if it is produced by this application.
        if (isProducingObject(resetter.getObjectCode()))
            getEventDispatcher(resetter.getObjectCode()).objectReset(resetter);
    }

    /**
     * Process a serialized data object command
     *
     * @param prefix        The character that prefixes the command ($ for regular command, ~ for object removal)
     * @param code          command code (without the $)
     * @param cmd           the command after breaking to its comma-separated components.
     * @param keys          the object keys as one string separated by bar characters
     * @param fields        the object field values as one string separated by commas
     * @param origUUID      the UUID of the origin service of the command
     * @param destinations  destination UUIDs of this command or null if destined to all.
     * @param channel        the socket on which the command has been received.
     * @param rawCommand    full raw command that delivered the object
     * @param commandTs     time stamp of the command that delivered the object
     * @param seq           sequential number of the command carrying the serialized object (for acknowledging a lossless object)
     * @param producing     true if we arrived here as producer of the object (read from import or RIC).
     * @return              the process object or null if it could not be deserialized
     * @throws java.lang.Exception
     */
    DataObject processCommand(String prefix, String code, String keys, String fields, UUID origUUID, HashSet<UUID> destinations,
        DataHandler channel, String rawCommand, ZDate commandTs, Integer seq, int rawLength) throws Exception
    {
        // Update statistics
        if (origUUID != null)
            Hub.getInstance().updateInputActivity(origUUID, rawCommand.length(), rawLength);
        
        boolean query = QueryObject.isQuery(prefix);
        DataObject obj;
        if (query) {
            obj = queryManager.processQueryCommand(
                prefix, code, keys, fields, origUUID, destinations, channel, rawCommand, commandTs);
            if (obj == null)
                return null;
        } else {
            obj = deserializeObject(prefix, code, keys, fields, origUUID, channel, rawCommand, commandTs);
            if (obj == null)
                return null;
            getEventDispatcher(code).objectEvent(obj, seq);
        }
        return obj;
    }
    
    /**
     * Deserialize an object from its network command
     * @param prefix        The character that prefixes the command ($ for regular command, ~ for object removal, ? and !
     *                      for queries)
     * @param code          command code (without the prefix)
     * @param keys          the object keys as one string separated by bar characters
     * @param fields        the object field values as one string separated by commas
     * @param origUUID      the UUID of the origin service of the command
     * @param channel       the channel on which the command has been received.
     * @param rawCommand    full raw command that delivered the object
     * @param commandTs     time stamp of the command that delivered the object
     * @return              the deserialized object
     */
    DataObject deserializeObject(String prefix, String code, String keys, String fields, UUID origUUID, DataHandler channel,
        String rawCommand, ZDate commandTs
    ) throws Exception {
        DataObject obj = parseObject(prefix, code, keys, fields, channel, origUUID);
        if (obj != null)
            obj.setRawCommand(rawCommand).setCommandTs(commandTs).setOriginUUID(origUUID);
        return obj;
    }
    
    /**
     * Parse the data object with the entire of its hierarchy.
     * @param prefix        The character that prefixes the command ($ for regular command, ~ for object removal)
     * @param code          command code (without the $)
     * @param keys          the object keys as one string separated by bar characters
     * @param fields        the object field values as one string separated by commas
     * @param channel       the channel on which the command has been received.
     * @param commandUUID   UUID of the origin of the command
     * @return the parsed object
     * @throws Exception 
     */
    DataObject parseObject(String prefix, String code, String keys, String fields, DataHandler channel, UUID commandUUID)
        throws Exception
    {
        String[] objectHierarchy = parsingHierarchy.get(code);
        if (objectHierarchy == null)
            return null;
        boolean remove = DataObject.RemoveIndicator.equals(prefix);
        String keyArray[] = Serializer.splitAndUnescape(keys, "|", 0);
        return rootObject.processChild(keyArray, fields, objectHierarchy, remove, channel, commandUUID);
    }
    
    void objectEvent(DataObject obj) {
        getEventDispatcher(obj.getObjectCode()).objectEvent(obj, null);
    }

    void cleanup() {
        rootObject.cleanup();
        eds.cleanup();
    }
}
