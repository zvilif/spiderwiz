package org.spiderwiz.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.spiderwiz.zutils.ZBuffer;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZUtilities;

/**
 * Handles data object archiving and restoration
 * @author Zvi 
 */
class Archiver {
    private class ObjectRecord {
        final String prefix;
        final String objectCode;
        final String keyValues;
        final String fieldValues;
        final ZDate time;

        public ObjectRecord(String prefix, String objectCode, String keyValues, String fieldValues, ZDate time) {
            this.prefix = prefix;
            this.objectCode = objectCode;
            this.keyValues = keyValues;
            this.fieldValues = fieldValues;
            this.time = time;
        }

        String resolveObjectPath(String path) {
            return resolvePath(path, time);
        }
        
        void writeToArchive(PrintWriter writer) {
            writer.println(ZUtilities.concatAll(",", prefix + objectCode, time == null ? null : time.format(ZDate.TIMESTAMP),
                keyValues, fieldValues));
        }
    }
    
    private class ObjectBuffer extends ArrayList<ObjectRecord> {
        /**
         * Add the values of an object to the buffer.
         * @param path          The path that determines the location of the object in the archive hierarchy
         * @param time          Time of the object
         * @param prefix        Prefix character of the object (normally $ but can be ~ if obsolete)
         * @param objectCode    Code of the object
         * @param keyValues     The object keys encoded to string
         * @param fieldValues   The object encoded to string
         */
        synchronized void archive(String path, ZDate time, String prefix, String objectCode, String keyValues, String fieldValues) {
            boolean needsActivation = isEmpty();
            add(new ObjectRecord(prefix, objectCode, keyValues, fieldValues, time));
            if (needsActivation)
                activationBuffer.add(path);
        }
        
        /**
         * Flush the buffer to disk
         * @param path          The path that determines the location of the object in the archive hierarchy
         * @return true if there was data to flush, false if the buffer is empty
         */
        synchronized boolean flush(String path) {
            if (isEmpty())
                return false;
            String filePath = null;
            PrintWriter writer = null;
            try {
                for (ObjectRecord obj : this) {
                    // check if the path for the object differs from previuos (or is the first one)
                    String objectPath = obj.resolveObjectPath(path);
                    if (objectPath == null)
                        continue;
                    if (!objectPath.equals(filePath)) {
                        // A new path requires opening a new file
                        if (writer != null)
                            writer.close();
                        filePath = objectPath;
                        File file = new File(filePath);
                        if (!file.exists()) {
                                file.getParentFile().mkdirs();
                                file.createNewFile();
                        }
                        OutputStream stream = new FileOutputStream(file, true);
                        if (isZipped(file))
                            stream = new GZIPOutputStream(stream);
                        writer = new PrintWriter (stream);
                    }
                    obj.writeToArchive(writer);
                }
            } catch (IOException ex) {
                Main.getInstance().sendExceptionMail(ex, null, filePath, false);
            }
            if (writer != null)
                writer.close();
            // Clear the ObjectRecord list and add the path to the activation buffer so that next time this object is flushed,
            // if the list is empty the object will be removed.
            clear();
            activationBuffer.add(path);
            return true;
        }
    }
    
    private class AllObjectBuffers extends HashMap<String, ObjectBuffer> {}
    
    private class ArchiveWriter extends Dispenser<String> {
        @Override
        public void dispense(String path, boolean flush) {
            if (path == null)
                return;
            ObjectBuffer objBuffer;
            synchronized(allBuffers){
                objBuffer = allBuffers.get(path);
            }
            
            // Flush the buffer. If it was empty delete it from allBuffers list.
            if (objBuffer != null && !objBuffer.flush(path))
                removeObjectBuffer(path);
        }
    }
    
    private final AllObjectBuffers allBuffers;
    private final ZBuffer<String> activationBuffer;
    private boolean initiated = false;

    public Archiver() {
        allBuffers = new AllObjectBuffers();
        activationBuffer = new ZBuffer<>(new ArchiveWriter());
        activationBuffer.setTimeout(0);
    }
    
    private synchronized void init() {
        if (!initiated) {
            initiated = true;
            activationBuffer.execute();
        }
    }
    
    private void removeObjectBuffer(String path) {
        synchronized(allBuffers){
            allBuffers.remove(path);
        }
    }
    
    /**
     * Archive an object encoded to a string
     * @param path          The path that determines the location of the object in the archive hierarchy
     * @param time          Time of the object
     * @param prefix        Prefix character of the object (normally $ but can be ~ if obsolete)
     * @param objectCode    Code of the object
     * @param keyValues     The object keys encoded to string
     * @param fieldValues   The object encoded to string
     */
    void archive(String path, ZDate time, String prefix, String objectCode, String keyValues, String fieldValues) {
        init();
        ObjectBuffer objBuffer;
        synchronized(allBuffers){
            objBuffer = allBuffers.get(path);
            if (objBuffer == null) {
                objBuffer = new ObjectBuffer();
                allBuffers.put(path, objBuffer);
            }
        }
        objBuffer.archive(path, time, prefix, objectCode, keyValues, fieldValues);
    }
    
    /**
     * Resolve a file path by replacing all time symbols with actual time values.
     * @param path
     * @param time
     * @return the resolved path or null if the path contains time symbols and time value is null
     */
    private String resolvePath(String path, ZDate time) {
        // Check first whether the path contains any time symbol
        if (!ZUtilities.find(path, "\\#[ymdh]"))
            return path;
        if (time == null)
            return null;
        return ZUtilities.replace(path,
            "\\#y", time.format("yy"),
            "\\#m", time.format("MM"),
            "\\#d", time.format("dd"),
            "\\#h", time.format("HH"));
    }
    
    private boolean isZipped(File file) {
        String name = file.getName().toLowerCase();
        return !name.endsWith(".txt") && !name.endsWith(".text");
    }

    /**
     * Restore from archive when a path defines the exact file to restore
     * @param objectCode    Restore only objects of this object code
     * @param from      if not null limit the object to restore
     * @param until     if not null limit the object to restore
     * @param path      path of the file hierarchy to restore
     * @return          The number of objects that were restored by this call.
     */
    int restorePath(String objectCode, Object associated, ZDate from, ZDate until, String path) throws Exception {
        int count = 0;
        File file = new File(path);
        if (!file.exists())
            return 0;
        InputStream stream = new FileInputStream(file);
        if (isZipped(file))
            stream = new GZIPInputStream(stream);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = in.readLine()) != null) {
                String fields[] = line.split(",", 4);
                if (fields.length < 2 || fields[0].length() < 2)
                    continue;
                ZDate ts = ZDate.parseTime(fields[1], ZDate.TIMESTAMP, null);
                if (from != null && from.after(ts) || until != null && (ts == null || until.before(ts)))
                    continue;
                String prefix = fields[0].substring(0, 1);
                String code = fields[0].substring(1);         // strip the prefix
                if (!code.equals(objectCode))
                    continue;
                DataObject obj = DataManager.getInstance().parseObject(prefix, code, fields.length < 2 ? null : fields[2],
                    fields.length < 3 ? null : fields[3], null, Main.getInstance().getAppUUID());
                if (obj != null) {
                    ++count;
                    obj.setCommandTs(ts);
                    if (!fireRestoreEvent(obj, associated))
                        return -1;
                }
            }
        } catch (IOException ex) {
        }
        return count;
    }
    
    /**
     * Fire a restore event. Normally this will activate the object's onRestore(), but if the object has been removed or renamed it will
     * fire onRemoval() or onRename() accordingly.
     * @param obj
     * @return 
     */
    private boolean fireRestoreEvent(DataObject obj, Object associated) {
        if (obj.isObsolete()) {
            if (obj.getRename() != null) {
                DataObject newObj = obj.getParent().getChild(obj.getObjectCode(), obj.getRename());
                if (newObj != null)
                    newObj.onRename(obj.getObjectID());
            } else
                obj.onRemoval();
            return true;
        }
        return obj.onRestore(associated);
    }
    
    /**
     * Restore from archive when a path defines a set of files depending of the value of 'from'
     * @param objectCode    Restore only objects of this object code
     * @param from          if not null limit the object to restore
     * @param until         if not null limit the object to restore
     * @param path          path of the file hierarchy to restore
     * @return              The number of objects that were restored by this call.
     */
    int restoreByTime(String objectCode, Object associated, ZDate from, ZDate until, String path) throws Exception {
        int field = determineTimeSteps(path);
        int count = 0;
        if (from == null)
            from = MyUtilities.findDateOfOldestFile(Main.getMyConfig().getArchiveFolder());
        if (from == null)
            return 0;
        if (until == null)
            until = ZDate.now();
        for (ZDate date = from; !date.after(until); date = date.add(field, 1)) {
            int n = restorePath(objectCode, associated, from, until, resolvePath(path, date));
            if (n < 0)
                return n;
            count += n;
        }
        return count;
    }
    
    int restoreAll(String objectCode, ZDate until, String path) {
        return 0;
    }
    
    /**
     * Determine the smallest unit of time contained in a path as a time parameter
     * @param path
     * @return 
     */
    private int determineTimeSteps(String path) {
        return
            path.contains("#h") ? Calendar.HOUR_OF_DAY : path.contains("#d") ? Calendar.DAY_OF_MONTH :
            path.contains("#m") ? Calendar.MONTH : Calendar.YEAR;
    }
    
    /**
     * Delete a single archive file
     * @param path 
     * @return true if file successfully deleted
     */
    boolean deleteArchiveFile(String path) {
        return new File(path).delete();
    }

    /**
     * Delete from archive when a path defines a set of files depending of the value of 'from'
     * @param objectCode    Restore only objects of this object code
     * @param from          if not null limit the object to restore
     * @param until         if not null limit the object to restore
     * @param path          path of the file hierarchy to restore
     * @return              true if all files were successfully deleted
     */
    boolean deleteByTime(String objectCode, ZDate from, ZDate until, String path) throws Exception {
        int field = determineTimeSteps(path);
        boolean result = true;
        if (until == null)
            until = ZDate.now();
        for (ZDate date = from; date.before(until); date = date.add(field, 1)) {
            result &= deleteArchiveFile(resolvePath(path, date));
        }
        return result;
    }

    void cleanup() {
        activationBuffer.cleanup(true);
    }
}
