package org.spiderwiz.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import org.spiderwiz.zutils.ZBuffer;
import org.spiderwiz.zutils.ZConfig;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZDispenser;
import org.spiderwiz.zutils.ZModInteger;
import org.spiderwiz.zutils.ZModerator;
import org.spiderwiz.zutils.ZThread;
import org.spiderwiz.zutils.ZUtilities;

/**
 * Implement a pipe of Strings between two processes kept in memory up to a size limit and backed by the file system for unlimited size.
 * The pipe is persistent through sessions.
 * @author Zvi 
 */
class LosslessPipe implements ZDispenser<LosslessPipe.PipeBlock>{
    public class PipeBlock {
        private final int first;
        private int nextPut;
        private int nextGet;
        private final String buffer[];
        private final ZModInteger startFrom;

        private PipeBlock(int first) {
            nextPut = nextGet = this.first = first;
            buffer = new String[bufferSize];
            startFrom = new ZModInteger(modulo);
            startFrom.setValue(first);
        }

        /**
         * Add an item to the buffer if not full.
         * @param item
         * @return true if successful, false if buffer is full
         */
        private boolean putItem(String item) {
            if (nextPut / bufferSize - first / bufferSize > 0)
                return false;
            buffer[nextPut++ - first] = item;
            return true;
        }
        
        /**
         * Get an item from the buffer.
         * @return the item or null if there are no more items in the buffer
         */
        private String getItem() {
            return getItem(nextGet++);
        }
        
        /**
         * Get the n-th line from the buffer
         * @param n
         * @return the item or null if n is beyond buffer size
         */
        private String getItem(int n) {
            return n >= nextPut ? null : buffer[n - first];
        }
    }
    
    /**
     * Manages range of acks that were skipped
     */
    private class SkippedAckResender {
        private final ZModInteger firstSkippedAck, nextReceivedAck;

        public SkippedAckResender(int firstSkippedAck, int nextReceivedAck) {
            this.firstSkippedAck = new ZModInteger(modulo, firstSkippedAck);
            this.nextReceivedAck = new ZModInteger(modulo, nextReceivedAck);
        }
        
        void resend() throws IOException {
            setNextUnskippedAck(nextReceivedAck.toInt());
            int nGet;
            moderator.reset();
            startResend(firstSkippedAck.toInt(), nextReceivedAck.toInt());
            while (!isAbort() && nextReceivedAck.compareTo(nGet = getNextGet()) > 0) {
                String line = get();
                if (line != null && firstSkippedAck.compareTo(nGet) <= 0) {
                    moderator.moderate();
                    resendSkippedLine(line);
                }
            }
            setNextUnskippedAck(-1);
            if (!isAbort())
                removeSkippedRange(nextReceivedAck.toInt());
        }
    }
    
    /**
     * Used as the dispenser in a ZBuffer that manages skipped acks.
     * If the object is null, i.e. there are no more objects in the buffer, then get all lines in the pipe until the next expected
     * ack number.
     */
    private class AckDispenser implements ZDispenser<SkippedAckResender> {
        @Override
        public void dispense(SkippedAckResender object, boolean flush) {
            try {
                if (object != null)
                    object.resend();
                else
                    getAllAcked();
            } catch (IOException ex) {
            }
        }

        @Override
        public void handleException(Exception ex) {
            LosslessPipe.this.handleException(ex);
        }
    }
    
    /**
     * This class is activated if we want to implement an auto-getter on the pipe.
     */
    private class AutoGetter extends ZThread {
        private boolean abort = false;

        private synchronized boolean isAbort() {
            return abort;
        }

        private synchronized void setAbort(boolean abort) {
            this.abort = abort;
        }

        @Override
        protected void doLoop() {
            try {
                // In the thread iterations we get all lines until there are no more and call a protected method to handle each of them
                String line;
                while (!isAbort() && (line = get()) != null) {
                    processGotLine(line);
                }
            } catch (IOException ex) {
                handleAutoGetterException(ex);
            }
        }

        @Override
        protected long getLoopInterval() {
            return 0;       // iteration is activated when there are new lines for getting
        }

        @Override
        public void cleanup() {
            setAbort(true);
            super.cleanup();
        }
        
    }
    
    private class PipeModerator extends ZModerator {
        @Override
        protected int getRate() {
            return LosslessPipe.this.getResendRate();
        }
    }
    
    private final static int BUFFER_SIZE = 1000;
    private final static int BUFFERS_PER_FILE = 10;
    private final static int MAXIMUM_NUMBER_OF_FILES = 100000;
    private final static String HISTORY_FILE_NAME = "history.txt";
    private final static String NEXTGET = "nextGet";
    private final static String NEXTPUT = "nextPut";
    private final static String SKIPPED_ACKS = "skipped acks";
    private final static int DEFAULT_ACTION_RATE = 30000;
    
    private int bufferSize = BUFFER_SIZE;
    private int buffersPerFile = BUFFERS_PER_FILE;
    private int maxNumberOfFiles = MAXIMUM_NUMBER_OF_FILES;
    public static final int DEFAULT_MODULO = BUFFER_SIZE * BUFFERS_PER_FILE * MAXIMUM_NUMBER_OF_FILES;
    private int fileSize;
    private int modulo;
    private final String backingFolder;
    private final ZBuffer<PipeBlock> pipeBuffer;
    private final LinkedList<PipeBlock> list;
    private final ZModInteger nextPut, nextGet, nextSave, nextAck;
    private PipeBlock currentPutBlock = null, currentGetBlock = null;
    private int firstReceivedAck = -1;
    private BufferedReader reader = null;
    private int currentReadFileSeq;
    private PrintWriter writer = null;
    private int currentWriteFileSeq;
    private boolean appendToCurrentFile = false;
    private final ZConfig history;
    private final AutoGetter autoGetter;
    private boolean abort = false;
    private final ZBuffer<SkippedAckResender> ackBuffer;
    private int nextUnskippedAck;
    private final PipeModerator moderator;

    /**
     * Constructor of a non auto-getter pipe.
     * @param backingFolder
     */
    public LosslessPipe(String backingFolder) {
        this(backingFolder, false);
    }

    /**
     * Constructor of a pipe with an option for auto-getter.
     * @param backingFolder
     * @param useAutoGetter
     */
    public LosslessPipe(String backingFolder, boolean useAutoGetter) {
        this.backingFolder = backingFolder;
        this.autoGetter = useAutoGetter ? new AutoGetter() : null;
        setSizeValues();
        pipeBuffer = new ZBuffer<>(this);
        pipeBuffer.setTimeout(0);
        list = new LinkedList<>();
        nextPut = new ZModInteger(modulo);
        nextGet = new ZModInteger(modulo);
        nextSave = new ZModInteger(modulo);
        nextAck = new ZModInteger(modulo);
        history = new ZConfig();
        ackBuffer = new ZBuffer<>(new AckDispenser());
        ackBuffer.setTimeout(ZDate.SECOND * 2);
        moderator = new PipeModerator();
    }
    
    private void setSizeValues() {
        fileSize = bufferSize * buffersPerFile;
        modulo = fileSize * maxNumberOfFiles;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        setSizeValues();
    }

    public void setBuffersPerFile(int buffersPerFile) {
        this.buffersPerFile = buffersPerFile;
        setSizeValues();
    }

    public void setMaxNumberOfFiles(int maxNumberOfFiles) {
        this.maxNumberOfFiles = maxNumberOfFiles;
        setSizeValues();
    }

    private synchronized boolean isAbort() {
        return abort;
    }

    private synchronized void setAbort(boolean abort) {
        this.abort = abort;
    }

    public synchronized int getNextUnskippedAck() {
        return nextUnskippedAck;
    }

    private synchronized void setNextUnskippedAck(int nextUnskippedAck) {
        this.nextUnskippedAck = nextUnskippedAck;
    }
    
    /**
     * Initialize the pipe
     */
    public void init() {
        // If the folder already has a settings file then this session shall preserve the previuos one.
        new File(backingFolder).mkdirs();
        if (history.init(backingFolder + "/" + HISTORY_FILE_NAME))
            appendToCurrentFile = true;
        nextSave.setValue(nextPut.setValue(history.getIntProperty(NEXTPUT)));
        nextGet.setValue(history.getIntProperty(NEXTGET));
        nextAck.setValue(nextGet.toInt());
        pipeBuffer.execute();
        if (autoGetter != null) {
            autoGetter.execute();
            autoGetter.activate();
        }
        ackBuffer.execute();
        loadSkippedRange();
    }

    /**
     * Put an item at the tail of the pipe
     * @param item
     * @return the serial number of the added item
     */
    public synchronized int put(String item) {
        // Try to put in current buffer. If buffer is full create a new one and send old one to be written on file
        while (currentPutBlock == null || !currentPutBlock.putItem(item)) {
            if (currentPutBlock != null)
                pipeBuffer.add(currentPutBlock);
            currentPutBlock = new PipeBlock(nextPut.toInt());
            list.push(currentPutBlock);
            history.saveConfiguration();
        }
        int result = nextPut.postInc();
        history.setProperty(NEXTPUT, nextPut.toString());
        if (autoGetter != null)
            autoGetter.activate();
        return result;
    }
    
    /**
     * Get an item from the head of the pipe
     * @return the item at the head or null if the pipe is empty
     * @throws java.io.IOException
     */
    public synchronized String get() throws IOException {
        if (isAbort())
            return null;
        if (nextGet.compareTo(nextPut) >= 0)
            return null;

        // Delete obsolete files and get the sequence of the next file to read
        int fileSeq = nextGet.divide(fileSize);
        if (fileSeq != currentReadFileSeq) {
            if (reader != null){
                reader.close();
                reader = null;
            }
            deleteFile(currentReadFileSeq);
            currentReadFileSeq = fileSeq;
        }

        // Try first current buffer. If it's not there take from next buffer or a backing file
        String item = null;
        if (currentGetBlock == null && nextGet.compareTo(nextSave) >= 0 && !list.isEmpty())
            currentGetBlock = list.remove();
        if (currentGetBlock != null && currentGetBlock.startFrom.compareTo(nextGet) <= 0) {
            while ((item = currentGetBlock.getItem()) == null) {
                if (nextGet.compareTo(nextSave) < 0) {
                    currentGetBlock = null;
                    break;
                }
                else {
                    if (list.isEmpty())
                        return null;
                    currentGetBlock = list.remove();
                }
            }
        }
        if (currentGetBlock == null && (item = getFromFile()) == null)
            return null;
        nextGet.inc();
        history.setProperty(NEXTGET, nextGet.toString());
        return item;
    }
    
    private File getFile(int basis) {
        return new File(backingFolder + "/" + basis * fileSize + ".txt");
    }
    
    private void deleteFile(int basis) {
        getFile(basis).delete();
    }
    
    private String getFromFile() throws IOException {
        while (nextGet.compareTo(nextPut) < 0) {
            while (reader == null && nextGet.compareTo(nextPut) < 0) {
                currentReadFileSeq = nextGet.divide(fileSize);
                File file = getFile(currentReadFileSeq);
                if (file.exists())
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), CoreConsts.DEFAULT_CHARSET));
                else {
                    nextGet.setValue(nextPut.min(++currentReadFileSeq * fileSize));
                }
            }
            if (reader == null)
                return null;
            String line;
            while ((line = reader.readLine()) != null) {
                // each stored line consists of ^,<serial number>,<item>
                String s[] = line.split(",", 3);
                if (s.length == 3 && s[0].equals("^")) {
                     int seq = ZUtilities.parseInt(s[1]);
                     if (seq >= nextGet.toInt()) {
                        nextGet.setValue(seq);
                        history.setProperty(NEXTGET, nextGet.toString());
                        return s[2];
                     }
                }
            }
            reader.close();
            reader = null;
            deleteFile(currentReadFileSeq);
        }
        return null;
    }
    
    /**
     * Call this if you want LosslessPipe to manage acknowledgments and resend.
     * @param ackNo line ID an ACK was received for
     */
    public synchronized void acknowledge(int ackNo) {
        // ignore old acks
        if (nextAck.compareTo(ackNo) > 0)
            return;
        
        // if the received ack number is not what expected (one after the previous one) then create an object to handle the skipped
        // lines and queue them in the buffer
        if (nextAck.compareTo(ackNo) < 0) {
            addSkippedRange(nextAck.toInt(), ackNo);
            firstReceivedAck = ackNo;
            ackBuffer.add(new SkippedAckResender(nextAck.toInt(), ackNo));
            nextAck.setValue(ackNo);
        }
        nextAck.inc();
    }
    
    private synchronized void getAllAcked() throws IOException {
        if (nextGet.compareTo(firstReceivedAck) < 0)
            return;
        while (!isAbort() && nextGet.compareTo(nextAck) < 0) {
            get();
            firstReceivedAck = nextGet.toInt();
        }
    }
    
    /**
     * Get a line from the pipe. If its serial number is less than 'end' then return the line, otherwise return null
     * @param end
     * @return
     * @throws java.io.IOException
     */
    public String getGapLine(int end) throws IOException {
        if (nextGet.compareTo(end) > 0)
            return null;
        String result = get();
        while (nextGet.compareTo(end) < 0) {
            if (result != null)
                return result;
            result = get();
        }
        return null;
    }
    
    /**
     * Remove the entire pipe with all the files
     * @throws java.io.IOException
     */
    public void remove() throws IOException {
        ZUtilities.deleteFolder(backingFolder);
    }
    
    /**
     * Return the sequence of the next line to put
     * @return
     */
    public synchronized int getNextGet() {
        return nextGet == null ? -1 : nextGet.toInt();
    }

    /**
     * Return the sequence of the next line to put
     * @return
     */
    public synchronized int getNextPut() {
        return nextPut == null ? -1 : nextPut.toInt();
    }
    
    public synchronized boolean isDataAvailable() {
        return nextGet != null && nextPut != null && nextGet.compareTo(nextPut) < 0;
    }
    
    private void addSkippedRange(int from, int to) {
        String ranges = history.getProperty(SKIPPED_ACKS);
        if (ranges == null)
            ranges = "";
        if (!ranges.isEmpty())
            ranges += ",";
        ranges += from + "-" + to;
        history.setProperty(SKIPPED_ACKS, ranges);
        history.saveConfiguration();
    }

    private void removeSkippedRange(int to) {
        ZModInteger rangeTo = new ZModInteger(modulo, to);
        String ranges = history.getProperty(SKIPPED_ACKS);
        if (ranges == null || ranges.isEmpty())
            return;
        String s[] = ranges.split(",");
        int i;
        for (i = 0; i < s.length; i++) {
            String range[] = s[i].split("\\-");
            if (range.length == 2 && rangeTo.compareTo(ZUtilities.parseInt(range[1])) > 0)
                break;
        }
        if (i == 0)
            return;
        ranges = "";
        for (; i < s.length; i++) {
            if (!ranges.isEmpty())
                ranges += ",";
            ranges += s[i];
        }
        history.setProperty(SKIPPED_ACKS, ranges);
        history.saveConfiguration();
    }
    
    private void loadSkippedRange() {
        String ranges = history.getProperty(SKIPPED_ACKS);
        if (ranges == null || ranges.isEmpty())
            return;
        String s[] = ranges.split(",");
        for (String fromTo : s) {
            String range[] = fromTo.split("\\-");
            if (range.length == 2) {
                int from = ZUtilities.parseInt(range[0]);
                int to = ZUtilities.parseInt(range[1]);
                ackBuffer.add(new SkippedAckResender(from, to));
            }
        }
    }
    
    @Override
    public void dispense(PipeBlock block, boolean flush) {
        if (block == null)
            return;
        try {
            boolean needsSave;
            // Do not write a block that get() passed it already
            synchronized(this){
                needsSave = nextGet.compareTo(block.nextPut) < 0;
            }
            // Write the buffer to a file
            if (needsSave) {
                int fileSeq = block.first / fileSize;
                if (writer == null || fileSeq != currentWriteFileSeq) {
                    if (writer != null)
                        writer.close();
                    currentWriteFileSeq = fileSeq;
                    writer =new PrintWriter(new OutputStreamWriter (
                        new FileOutputStream(getFile(fileSeq), appendToCurrentFile), CoreConsts.DEFAULT_CHARSET)
                    );
                    appendToCurrentFile = false;
                }
                int i;
                synchronized(this){
                    i = nextGet.max(block.first);
                    nextSave.setValue(block.nextPut);
                }
                for (; i < block.nextPut; i++)
                    writer.println("^," + i + "," + block.getItem(i));
                writer.flush();
            }
            
            // Remove the block from the list of memory buffers.
            // Set 'nextSave' value so that when get() finishes with its current block it will read the next one from file.
            // If file is not needed any more, delete it
            synchronized(this){
                list.remove(block);
                nextSave.setValue(block.nextPut);
                if (nextSave.divide(fileSize) != currentWriteFileSeq) {
                    if (writer != null) {
                        writer.close();
                        writer = null;
                    }
                    if (nextGet.compareTo(block.nextPut) >= 0)
                        deleteFile(currentWriteFileSeq);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Override this to implement an auto-getter pipe. Note that an auto getter pipe must be instantiated with autoGetter=true
     * @param line
     */
    protected void processGotLine(String line){}
    
    /**
     * Override this to do something when there is an exception in the auto getter
     * @param ex
     */
    protected void handleAutoGetterException(Exception ex){}
    
    /**
     * Override this to set the pipe's resend rate (rate skipped lines are resent)
     * @return number of lines to send per minute. The default resend rate is 30000 lines per minute
     */
    protected int getResendRate() {
        return DEFAULT_ACTION_RATE;
    }
    
    /**
     * Override this to implement the sending of a line that did not receive an ACK
     * @param line
     */
    protected void resendSkippedLine(String line){}
    
    /**
     * Override this method to do something (logging, for instance) when the pipe starts to resend a block of skipped lines
     * @param firstSkipped  the first line that was skipped and therefore will be resent
     * @param nextReceived  the next line that was acked. all lines until and excluding this will be resent.
     */
    protected void startResend(int firstSkipped, int nextReceived) {}
    
    /**
     * Flush all buffers to files
     */
    public void cleanup() {
        setAbort(true);
        moderator.cleanup();
        try {
            getAllAcked();
            ackBuffer.cleanup(false);
            if (autoGetter != null)
                autoGetter.cleanup();
            synchronized(this){
                if (currentPutBlock != null)
                    pipeBuffer.add(currentPutBlock);
            }
            pipeBuffer.cleanup(true);
            synchronized(this){
                if (reader != null)
                    reader.close();
                if (writer != null)
                    writer.close();
            }
            history.saveConfiguration();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void handleException(Exception ex) {
        ex.printStackTrace();
    }
}
