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
 * @author Zvi 
 */
final class DataManager {
    /**
     * Use this class to determine object hierarchy for parsing.
     */
    private class ParsingHierarchyMap extends HashMap<String, String[]> {
        void init(Collection<String> codes) {
            if (codes == null)
                return;
            for (String code : codes) {
                DataObject obj = Main.getInstance().createDataObject(code);               
                if (obj != null)
                    put(code, obj.getParsingHierarchy());
            }
        }
    }
    
    private static DataManager myObject = null;
    private final EventDispatcher ed;
    private final QueryManager queryManager;
    private final Ztrings producedObjects;
    private final ConsumedObjectMap consumedObjects;
    private final ParsingHierarchyMap producerParsingHierarchy;
    private final ParsingHierarchyMap consumerParsingHierarchy;
    private final RootObject rootObject;

    DataManager(EventDispatcher ed, QueryManager queryManager) {
        this.ed = ed;
        this.queryManager = queryManager;
        producedObjects = new Ztrings();
        producerParsingHierarchy = new ParsingHierarchyMap();
        consumerParsingHierarchy = new ParsingHierarchyMap();
        rootObject = new RootObject();
        consumedObjects = new ConsumedObjectMap() {
            @Override
            Boolean getLosslessController(String objCode) {
                return true;
            }
        };
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
    void init() {
        myObject = this;
        producedObjects.addAll(ZUtilities.arrayToList(Main.getInstance().getProducedObjects()));
        consumedObjects.fromArray(Main.getInstance().getConsumedObjects(), null);
        String xAdminCode;
        if ((xAdminCode = Main.getInstance().getxAdminQueryCode()) != null) {
            producedObjects.add(xAdminCode);
            consumedObjects.put(xAdminCode, null);
        }
        producerParsingHierarchy.init(producedObjects);
        consumerParsingHierarchy.init(consumedObjects.keySet());
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
     * Parse a command arriving from an imported Server
     *
     * @param cmd           the command
     * @param importChannel       the channel object representing the sending server
     * @param ricObject     If not null the function is called from RIC and this is the RIC object.
     * @param commandTime   the time the command is read from the import server
     * @param ricChannel    in case the import command arrives through RawImport (RIC) this contains the DataHandler
                      object originating the RIC.
     * @throws java.text.ParseException
     */
    void processImportCommand(String cmd, ImportHandler importChannel, ZDate commandTime) throws Exception
    {
        // Try first processing the command by each of the pending queries.
        if (queryManager.processImportQuery(cmd, importChannel, commandTime))
            return;
        
        // Try to use producer objects.
        if (producedObjects == null)
            return;
        for (String code : producedObjects.asCollection()) {
            code = DataObject.stripLossless(code);
            // RIC shall not create another RIC.
            if(code.equals(RawImport.ObjectCode) && importChannel.getRimChannel() != null)
                continue;
                
            // For each produced object create the object and parse the command into it. If the command is relevant for the object
            // process the object and then if necessary propagate the object to other nodes.
            DataObject obj = Main.getInstance().createDataObject(code);
            if (obj != null) {
                String keyList[] = obj.importObject(cmd, importChannel, commandTime);
                if (keyList != null) {
                    String keys = Serializer.escapeAndConcatenate("|", keyList);
                    String vals = obj.serialize();
                    if (!processCommand(
                        "$", obj.getObjectCode(), keys, vals, null, null, importChannel.getRimChannel(), cmd, commandTime, null, true,
                        0)
                    )
                        obj.propagate(false, null, null);
                }
            }
        }
    }

    /**
     * Process a dailyReset command
     *
     * @param resetter A Resetter object managing the dailyReset of a specific object type.
     * @return true if the running node is a producer of the objects dailyReset has
 been requested for.
     */
    boolean processReset(Resetter resetter) {
        // Try first the query list
        if (Hub.getInstance().processQueryReset(resetter))
            return true;
        
        // If we are a consumer of the dailyReset object (we get it from another node), then we have to dailyReset it only if we are also
        // a producer of it. If not, we will not dailyReset it but also will not echo the dailyReset request to other nodes, so we will return true
        // here.
        // If we are a producer of this node, then we try to dailyReset it from the root object map. If no object is dailyReset by this
        // way then only if we are the sole producer of the object (i.e. we are not a consumer of it) we will generate a dailyReset event
        // for the application to take care of the dailyReset request.
        String code = resetter.getObjectCode();
        boolean producing = isProducingObject(code);
        boolean consuming = isConsumingObject(code);
        if (!producing)
            return consuming;
        boolean reset = rootObject.resetObject(resetter);
        if (consuming)
            return reset;
        ed.objectReset(resetter);
        return true;
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
     * @return              true if the command was processed and shall not be forwarded to other data nodes, otherwise,
     *                      i.e. if either the command was not processed or it should be forwarded even though it was
     *                      processed, return false.
     * @throws java.lang.Exception
     */
    boolean processCommand(String prefix, String code, String keys, String fields, UUID origUUID, HashSet<UUID> destinations,
        DataHandler channel, String rawCommand, ZDate commandTs, Integer seq, boolean producing,
        int rawLength) throws Exception
    {
        // Update statistics
        if (origUUID != null)
            Hub.getInstance().updateInputActivity(origUUID, rawCommand.length(), rawLength);
        
        // if leading character is '?' or '!' this is a query command.
        boolean query = prefix.matches("\\?|\\!");
        DataObject obj;
        if (query) {
            obj = queryManager.processQueryCommand(
                prefix, code, keys, fields, origUUID, destinations, channel, rawCommand, commandTs, producing);
            if (obj == null)
                return false;
        } else {
            obj = deserializeObject(prefix, code, keys, fields, origUUID, channel, rawCommand, commandTs, producing);
            if (obj == null)
                return false;
            ed.objectEvent(obj, seq);
        }
        return obj.onlyForMe();
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
     * @param producing     true if we arrived here as producer of the object (read from import or RIC).
     * @return              the deserialized object
     */
    DataObject deserializeObject(String prefix, String code, String keys, String fields, UUID origUUID, DataHandler channel,
        String rawCommand, ZDate commandTs, boolean producing
    ) throws Exception {
        DataObject obj = parseObject(prefix, code, keys, fields, channel, producing);
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
     * @param producing     True if we are going to produce this object, false if we consume it
     * @return the parsed object
     * @throws Exception 
     */
    DataObject parseObject(String prefix, String code, String keys, String fields, DataHandler channel,
        boolean producing) throws Exception {
        String[] objectHierarchy =
            producing ? producerParsingHierarchy.get(code) : consumerParsingHierarchy.get(code);
        if (objectHierarchy == null)
            return null;
        boolean remove = DataObject.RemoveIndicator.equals(prefix);
        String keyArray[] = Serializer.splitAndUnescape(keys, "|", 0);
        return rootObject.processChild(keyArray, fields, objectHierarchy, remove, channel);
    }
    
    void objectEvent(DataObject obj) {
        ed.objectEvent(obj, null);
    }

    void cleanup() {
        rootObject.cleanup();
    }
}
