package org.spiderwiz.core;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.spiderwiz.core.DataHandler.CommandComponents;
import org.spiderwiz.core.DataHandler.CommandOffsets;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZHashMap;
import org.spiderwiz.zutils.ZHashSet;
import org.spiderwiz.zutils.ZLog;
import org.spiderwiz.zutils.ZModInteger;
import org.spiderwiz.zutils.ZUtilities;
import org.spiderwiz.zutils.Ztrings;

/**
 * Manages sequencing object transmission between data nodes.
 * @author zvil
 */
final class SequenceManager {
    /**
     * An internal class that handles the sequence of received messages.
     * Each command type is managed by its own object sequencer.
     */
    private class CommandSequencer {
        private final String commandCode;
        private final ZModInteger nextSeq;
        private ZDate lastReset;
        private Resetter resetter = null;
        private final HashMap<String, String> keyFrames;                    // Used to compress field values
        private final HashMap<UUID, Integer> objCounterMap;    // Used to compress application object counters
        private ZDate previousTimeStamp;
        private UUID previousOrigin;
        private String previousDestinations;
        private String previousKeys;
        private boolean inSequence = false;
        private boolean sender;         // true when this counter is for sending objects (not for query replying only!).

        public CommandSequencer(String commandCode, boolean sender) {
            this.commandCode = commandCode;
            nextSeq = new ZModInteger(CoreConsts.SEQUENCE_MODULU);
            lastReset = null;
            keyFrames = new HashMap<>();
            objCounterMap = new HashMap<>();
            this.sender = sender;
        }

        synchronized boolean isSender() {
            return sender;
        }

        synchronized void setSender(boolean sender) {
            this.sender = sender;
        }
        
        private synchronized void reset() {
            nextSeq.setValue(0);
        }

        /**
         * Get a command, split to its components. If the command is not in sequence with the previous command, log a notification,
         * request reset from peer nodes and return null. If the command is in sequence, treat it as a delta from the previous
         * command (unless this is a key frame), apply the delta on the previous command, and return the full decompressed command.
         *
         * @param prefix the command prefix
         * @param components the command split to its components
         * @return an object with the parsed decompressed components
         */
        private DataHandler.CommandComponents parseCommandComponents(String prefix, String[] components) throws ParseException
        {
            StringBuilder expandedCommand = new StringBuilder(prefix).append(commandCode);
            synchronized(nextSeq) {
                // Get the sequence number
                int seq = nextSeq.fromHex(components[CommandOffsets.SEQUENCE_NUMBER]);
                
                // A zero serial resets the sequence
                if (seq == 0) {
                    logger.logNow(CoreConsts.HAS_RESET, commandCode, nextSeq.toInt() - 1);
                    lastReset = null;
                    nextSeq.setValue(0);
                    inSequence = true;
                    keyFrames.clear();
                    objCounterMap.clear();
                }
                if (seq == 0 || !compress) {
                    previousTimeStamp = null;
                    previousOrigin = null;
                    previousDestinations = null;
                    previousKeys = null;
                }
                if (inSequence) {
                    // if was in sequence before and it is out of sequence now, log the case and set inSequence to false
                    if (seq != nextSeq.postInc()) {
                        logger.logNow(CoreConsts.LOST_LINES, nextSeq.diff(seq), commandCode, nextSeq.toInt(), seq - 1);
                        inSequence = false;
                    }
                }

                // If needed, request a reset from peer nodes. We wait 3 minutes before sending another reset request
                if (!inSequence && (lastReset == null || lastReset.elapsed() >= 3 * ZDate.MINUTE)) {
                    lastReset = ZDate.now();
                    Hub.getInstance().requestObectReset(commandCode, null, channel);
                }
                
                // If in sequence reconstruct the command from previous command and delta
                if (!inSequence)
                    return null;
                
                // Reconstruct the timestamp
                String timestamp = components[CommandOffsets.TIMESTAMP];
                ZDate ts;
                if  (previousTimeStamp == null)
                    ts = ZDate.parseRoundTime(timestamp, 1);
                else {
                    long t = timestamp.isEmpty() ? 0 : Long.parseLong(timestamp) * 100;
                    ts = previousTimeStamp.add(t);
                }
                previousTimeStamp = ts;
                expandedCommand.append(Serializer.FIELD_SEPARATOR).append(ts.formatRoundTimestamp(1)).
                    append(Serializer.FIELD_SEPARATOR).append(seq).append(Serializer.FIELD_SEPARATOR);
                
                // Reconstruct the sub header
                String subHeader[] = components[CommandOffsets.SUBHEADER].split("\\|");
                
                // Reconstruct the origin
                String originUUID = subHeader[CommandOffsets.SubheaderOffsets.ORIGIN];
                UUID origin = originUUID.isEmpty() ? null : UUID.fromString(originUUID);
                if (previousOrigin != null && origin == null)
                    origin = previousOrigin;
                previousOrigin = origin;
                expandedCommand.append(origin == null ? "" : origin.toString());
                
                // Reconstruct the destination set
                String destinationList = subHeader[CommandOffsets.SubheaderOffsets.DESTINATIONS];
                if (previousDestinations != null)
                    destinationList = destinationList.isEmpty() ? previousDestinations :
                        Serializer.getInstance().decompressMap(previousDestinations, destinationList);
                previousDestinations = destinationList;
                HashSet<UUID> destinations = Serializer.isNullIndicator(destinationList) ? null :
                    Ztrings.split(destinationList).toUUIDs();
                expandedCommand.append(Serializer.BAR_SEPARATOR).append(destinationList);
                
                // Reconstruct the application object sequence
                int objSeq = Integer.parseInt(subHeader[CommandOffsets.SubheaderOffsets.OBJ_SEQ]);
                if (compress) {
                    Integer previousObjSeq = objCounterMap.get(origin);
                    if (previousObjSeq != null)
                        objSeq += previousObjSeq;
                    objCounterMap.put(origin, objSeq);
                }
                expandedCommand.append(Serializer.BAR_SEPARATOR).append(objSeq);
                
                // Reconstruct the ack sequence
                Integer ackSeq = null;
                if (subHeader.length > CommandOffsets.SubheaderOffsets.ACK_SEQ &&
                    !subHeader[CommandOffsets.SubheaderOffsets.ACK_SEQ].isEmpty()) {
                    ackSeq = Integer.parseInt(subHeader[CommandOffsets.SubheaderOffsets.ACK_SEQ]);
                    expandedCommand.append(Serializer.BAR_SEPARATOR).append(ackSeq);
                }
                
                // Reconstrcut the keys
                String keys = components.length < CommandOffsets.KEYS ? null : components[CommandOffsets.KEYS];
                if (previousKeys != null)
                    keys = Serializer.getInstance().decompressValues(previousKeys, keys, Serializer.BAR_SEPARATOR);
                previousKeys = keys;
                expandedCommand.append(Serializer.FIELD_SEPARATOR).append(keys);
                
                // Reconstruct the object fields
                String vals = components.length <= CommandOffsets.VALUES ? null : components[CommandOffsets.VALUES];
                if (compress && !DataObject.RemoveIndicator.equals(prefix)) {
                    String previous = keyFrames.get(keys);
                    if (previous != null)
                        vals = Serializer.getInstance().decompress(previous, vals);
                    keyFrames.put(keys, vals);
                }
                expandedCommand.append(Serializer.FIELD_SEPARATOR).append(vals);
                
                return new CommandComponents(ts, origin, destinations, objSeq, ackSeq, keys, vals, expandedCommand.toString());
            }
        }
        
        /**
         * Transmit a command (serialized data object)
         * @param prefix        Prefix of the command
         * @param ts            Timestamp of the command
         * @param keys          Serialized object key values
         * @param vals          Serialized object field values
         * @param origin    UUID of the originating data node
         * @param destinations  UUIDs of the destination data node, or null if shall be broadcast to all
         * @param objSeq        Sequence number given to the object by the originating data node
         * @param ackSequence   Sequence number for acknowledging lossless objects
         * @param moderated     True if transmission of the object is moderated. If it is not, the moderator shall count
         *                      the object. If it is moderated, moderation was done before calling this method.
         */
        private void transmitCommand(String prefix, ZDate ts, String keys, String vals, UUID origin, Collection<UUID> destinations,
            int objSeq, Integer ackSequence, boolean moderated
        ) {
            // Check if this channel includes any of the specified destinations
            String destUUIDs = "*";
            if (destinations != null) {
                Collection<UUID> filter = ZHashSet.intersection(destinations, channel.getConnectedNodes());
                if (filter.isEmpty())
                    return;
                destUUIDs = ZUtilities.concatAll(";", filter);
            }
            if (!moderated)
                moderator.count();
            synchronized (nextSeq) {
                String timestamp;
                String originUUID = origin.toString();
                String dest = destUUIDs;
                String objKeys = keys;
                boolean keyframe = nextSeq.toInt() == 0;          // if nexSeq is zero we shall send keyframes for all objects
                if (keyframe) {
                    keyFrames.clear();
                    objCounterMap.clear();
                }
                if (keyframe || !compress)
                    timestamp = ts.formatRoundTimestamp(1);
                else {
                    // compress header values
                    long diff = (ts.diff(previousTimeStamp) + 50) / 100;
                    timestamp = diff == 0 ? "" : String.valueOf(diff);
                    if (previousOrigin.equals(origin)) {
                        originUUID = "";
                    }
                    dest = dest.equals(previousDestinations) ? ""
                        : Serializer.getInstance().compressMap(previousDestinations, dest);
                    objKeys = keys == null || keys.equals(previousKeys) ? ""
                        : Serializer.getInstance().compressValues(previousKeys, keys, Serializer.BAR_SEPARATOR);
                }

                previousTimeStamp = ts;
                previousOrigin = origin;
                previousDestinations = destUUIDs;
                previousKeys = keys;
                String fullVals = vals;
                int fullObjSeq = objSeq;

                if (compress) {
                    // Compress application object counter
                    Integer previousObjSeq = objCounterMap.put(origin, objSeq);
                    if (previousObjSeq != null) {
                        objSeq -= previousObjSeq;
                    }

                    // Compress field values
                    if (!DataObject.RemoveIndicator.equals(prefix)) {
                        String previousVals = keyFrames.put(keys, vals);
                        if (previousVals != null) {
                            vals = Serializer.getInstance().compress(previousVals, vals);
                        }
                    }
                }

                String subHeader = ZUtilities.concatAll("|", originUUID, dest, objSeq, ackSequence);
                if (fullLog) {
                    String fullHeader = ZUtilities.concatAll("|", origin, destUUIDs, fullObjSeq, ackSequence);
                    String fullLine = ZUtilities.concatAll(",",
                        prefix + commandCode, ts.formatRoundTimestamp(1), nextSeq.toInt(), fullHeader, keys, fullVals);
                    logger.log("=> " + fullLine, false);
                }
                String line = ZUtilities.concatAll(",",
                    prefix + commandCode, timestamp, nextSeq.postIncAsHex(), subHeader, objKeys, vals);
                if (rawLog)
                    logger.log("-> " + line, false);
                if (flushLogs)
                    logger.flush();
                getChannel().transmit(line, DataObject.isUrgentCommand(prefix));
            }
        }

        private synchronized Resetter createResetter() {
            if (resetter != null)
                resetter.abort();
            resetter = new Resetter(SequenceManager.this, commandCode);
            return resetter;
        }
        
        private synchronized void cleanup() {
            if (resetter != null)
                resetter.cleanup(false);
        }
    }

    private class SequenceMap extends ZHashMap<String, CommandSequencer> {
        /**
         * If doesn't exist yet, create a sequencer for the given command code.
         * @param cmdCode   given command code
         * @param sender    true if a normal sender sequencer, false if a receiver or responder sequencer.
         * @return          the existing or newly created sequencer
         */
        CommandSequencer create(String cmdCode, boolean sender) {
            CommandSequencer sm = get(cmdCode);
            if (sm == null) {
                CommandSequencer old = putIfAbsent(cmdCode, sm = new CommandSequencer(cmdCode, sender));
                if (old != null)
                    sm = old;
            }
            // If 'sender' is true mark the sequencer as sender for the case it was not before
            if (sender)
                sm.setSender(sender);
            return sm;
        }
        
        /**
         * Reset all counters. Called when a channel is connected.
         */
        void resetAll() {
            lockRead();
            try {
                values().forEach((os) -> {
                    os.reset();
                }); 
            } finally {
                unlockRead();
            }
        }
        
        void cleanup() {
            lockRead();
            try {
                values().forEach((os) -> {
                    os.cleanup();
                }); 
            } finally {
                unlockRead();
            }
        }
    }
    
    private DataHandler channel;
    private final SequenceMap sendCount;
    private final SequenceMap receiveCount;
    private ZDate lastDisconnect = null, lastIdle = null;
    private boolean alerted = false;
    private final TransmitModerator moderator;
    private boolean compress;
    private ZLog logger;
    private boolean fullLog;
    private boolean rawLog;
    private boolean flushLogs;

    SequenceManager(DataHandler channel) {
        sendCount = new SequenceMap();
        receiveCount = new SequenceMap();
        moderator = new TransmitModerator();
        setChannel(channel);
}

    synchronized DataHandler getChannel() {
        return channel;
    }

    /**
     * Call when a channel is connected or reconnected
     * @param channel 
     */
    synchronized void setChannel(DataHandler channel) {
        this.channel = channel;
        this.logger = channel.getLogger();
        resetAll();
    }
    
    void resetAll() {
        sendCount.resetAll();
    }

    void setAlerted(boolean alerted) {
        boolean pre;
        synchronized (this){
            pre = this.alerted;
            this.alerted = alerted;
        }
        if (pre & !alerted) {
            Main.getInstance().sendNotificationMail(
                String.format(CoreConsts.AlertMail.RESUME_NOTIFICATION, channel.getAppName(), channel.getRemoteAddress()),
                null, ZDate.now(), false);
        }
    }
    
    /**
     * Handle a request to reset objects of one or more types
     * @param cmnds     list of command codes.
     */
    void resetOutput(Collection<String> requestedObjects, DataHandler fromSocket) {
        boolean hubMode = Main.getMyConfig().isHubMode();
        for (String cmd : requestedObjects) {
            CommandSequencer sm = sendCount.get(cmd);
            if (sm == null) {
                if (!DataManager.getInstance().isProducingObject(cmd) && !hubMode)
                    continue;
                sm = sendCount.create(cmd, true);
            }
            if (sm != null) {
                sm.reset();
                DataManager.getInstance().processReset(sm.createResetter());
            }
        }
    }
    
    /**
     * Stop sending objects with the given codes
     * @param objectCodes 
     */
    void stopSending(Ztrings objectCodes) {
        if (objectCodes == null)
            return;
        for (String objectCode : objectCodes.asCollection()) {
            sendCount.remove(objectCode);
        }
    }

    /**
     * Get a command, split to its components. If the command is not in sequence with the previous command, log a
 notification, request reset from peer nodes and return null. If the command is in sequence, treat it as a delta
 from the previous command (unless this is a key frame), apply the delta on the previous command, and return the
 full decompressed command.
     * @param prefix        the command prefix
     * @param objCode       the command code
     * @param seq           serial number of the command
     * @param components    the command split to its components
     * @return an object with the parsed decompressed components
     */
    DataHandler.CommandComponents parseCommandComponents(String prefix, String objCode, String[] components)
        throws ParseException
    {
        return receiveCount.create(objCode, false).parseCommandComponents(prefix, components);
    }
    
    /**
     * Transmit a command (serialized data object)
     * @param prefix        Prefix of the command
     * @param objCode       Command (object) code
     * @param ts            Timestamp of the command
     * @param objectValues  Serialized object field values
     * @param objectKeys    Serialized object key values
     * @param originUUID    UUID of the originating data node
     * @param destinations  UUIDs of the destination data node, or null if shall be broadcast to all
     * @param objSeq        Sequence number given to the object by the originating data node
     * @param ackSequence   Sequence number for acknowledging lossless objects
     * @param moderated     True if transmission of the object is moderated. If it is not, the moderator shall count the object.
     *                      If it is moderated, moderation was done before calling this method.
     * @param reply         True if this a query reply
     */
    void transmitCommand(String prefix, String objCode, ZDate ts, String objectKeys, String objectValues, UUID originUUID,
        Collection<UUID> destinations, int objSeq, Integer ackSequence, boolean moderated, boolean reply
    ) {
        // Propagate only if requested by data server or this is a query reply
        CommandSequencer sm = reply ? sendCount.create(objCode, false) : sendCount.get(objCode);
        if (sm != null && (reply || sm.isSender()))
            sm.transmitCommand(
                prefix, ts, objectKeys, objectValues, originUUID, destinations, objSeq, ackSequence, moderated);
    }
    
    /**
     * Transmit a moderated object during reset. This causes the transmission to pause if necessary in order to retain a fixed
     * transmission rate
     * @param obj           The object to code and transmit
     */
    void transmitModeratedObject(DataObject obj) throws Exception {
        Hub.getInstance().transmitModeratedObject(obj, this, moderator);
    }
    
    /**
     * Called when a Resetter start resetting in order to synchronize all resetters
     */
    void resetModerator() {
        moderator.reset();
    }
    
    /**
     * Called after an associated channel is logged in
     */
    synchronized void onLogin(boolean compress, boolean fullLog, boolean rawLog, boolean flushLogs) {
        lastDisconnect = null;
        this.compress = compress;
        this.fullLog = fullLog;
        this.rawLog = rawLog;
        this.flushLogs = flushLogs;
    }
    
    /**
     * Called after an associated channel is disconnected
     */
    synchronized void onDisconnect() {
        lastDisconnect = ZDate.now();
    }
    
    /**
     * Monitor the associated channel.
     */
    void monitor() {
        Main.getInstance().monitorDiskSpace();
        monitorConnection();
        monitorIdle();
    }
    
    /**
     * If the associated channel is disconnected more than a set amount of time send an alert mail.
     */
    private void monitorConnection() {
        int alertTime = Main.getMyConfig().getDisconnectionAlertTime();
        synchronized(this) {
            if (alertTime < 0 || lastDisconnect == null || lastDisconnect.elapsed() < alertTime)
                return;
            lastDisconnect = null;
        }
        channel.sendDisconnectionMail();
    }
    
    /**
     * If the associated channel is idle more than a set amount of time send an alert mail.
     */
    private void monitorIdle() {
        int alertTime = Main.getMyConfig().getIdleAlertTime();
        ZDate lastInput = channel.getLastInput();
        if (alertTime < 0 || lastInput == null)
            return;
        synchronized(this) {
            if (lastInput.elapsed() < alertTime) {
                lastIdle = null;
                return;
            }
            if (lastIdle != null)
                return;
            lastIdle = ZDate.now();
        }
        channel.onIdle();
    }
    
    void cleanup() {
        moderator.cleanup();
        sendCount.cleanup();
    }
}
