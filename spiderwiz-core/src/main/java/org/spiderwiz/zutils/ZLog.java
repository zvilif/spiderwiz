package org.spiderwiz.zutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Provides logging mechanism.
 * <p>
 * The class provides a framework for a logging mechanism that includes the following features:
 * <ul>
 * <li>Management an hierarchy of log folders and files under a root log folder.</li>
 * <li>Automatic creation of log folders for every calendar day and log files for every hour of the day.</li>
 * <li>Optional appending of timestamps and headers to each log line.</li>
 * <li>Various log methods with and without formatting.</li>
 * </ul>
 * <p>
 * For details about the framework logging system see <a href="../core/doc-files/logging.html">Spiderwiz Logging System</a>.
 * @author @author  zvil
 */
public class ZLog {
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String DEFAULT_LOG_FOLDER = "logs";

    private final String rootFolder;
    private final ZConfig config;
    private String rootPath;
    private String logFileName;
    private PrintWriter logFile;
    private boolean append;

    /**
     * Constructs an appending logger.
     * <p>
     * The log folder managed by the logger is determined by the given root folder name and a property taken from the given
     * configuration file. The label of the property that defines the log sub-folder name is provided when
     * {@link #init(java.lang.String) init()} is called.
     * <p>
     * If the log folder already contains hourly files for an hour that is logged by this logger then new logs are appended to them.
     * @param rootFolder    the root folder name.
     * @param config        the configuration file containing the property that determines the log sub-folder name.
     */
    public ZLog(String rootFolder, ZConfig config) {
        this(rootFolder, config, true);
    }

    /**
     * Constructs an appending or overwriting logger.
     * <p>
     * The log folder managed by the logger is determined by the given root folder name and a property taken from the given
     * configuration object. The label of the property that defines the log sub-folder name is provided when
     * {@link #init(java.lang.String) init()} is called.
     * <p>
     * The {@code append} parameter determines whether this is an appending or overwriting logger.
     * If the log folder already contains hourly files for an hour that is logged by this logger, then an appending logger
     * appends to them while an overwriting logger overwrites them.
     * @param rootFolder    the root folder name.
     * @param config        the configuration object containing the property that determines the log sub-folder name.
     * @param append        true for an appending logger, false for an overwriting logger.
     */
    public ZLog(String rootFolder, ZConfig config, boolean append) {
        this.rootFolder = rootFolder;
        this.config = config;
        this.logFileName = null;
        this.logFile = null;
        this.append = append;
        this.rootPath = null;
    }

    /**
     * Initializes the logger.
     * <p>
     * The {@code label} parameter identifies the property in the configuration file attached by the
     * class {@link #ZLog(java.lang.String, org.spiderwiz.zutils.ZConfig) constructor} that specifies the log sub-folder name.
     * @param label   identifies the property in the configuration file attached to this logger that specifies the log sub-folder name.
     */
    public synchronized void init(String label) {
        this.cleanup();
        String name = config.getProperty(label);
        if (name == null)
            name = DEFAULT_LOG_FOLDER;
        rootPath = this.rootFolder + name + "/";
    }

    /**
     * Writes a line to the current log file.
     * <p>
     * Writes the {@code line} string to the current log file. If {@code time} is not {@code null} then the method:
     * <ol>
     * <li>Precedes the line written to the log file with a timestamp in the format {@code hh:mm:ss:SSS} whose value is specified by
     * the parameter.</li>
     * <li>Writes the line to a file under the current log sub-folder named {@code am}<em>hh</em> or {@code pm}<em>hh</em>, reflecting
     * the given time.</li>
     * <li>If {@code keepSame} parameter is {@code false}, then if {@code time} spans into a new hour since the last {@code commit}
     * creates a new file (and also a new folder if the date has changed), or appends to an existing file if a file with that names
     * exists and this is an appending logger.</li>
     * </ol>
     * @param time      if not null the line is preceded by this timestamp and written to a file whose name is determined by this
     * time.
     * @param header    if not null the timestamp (if given) is followed by this header.
     * @param line      the line to write to the file.
     * @param keepSame  if true writes to the same file as the previous commit even though the hour has changed since then.
     */
    public synchronized void commit(Date time, String header, String line, boolean keepSame) {
        try {
            if (rootPath == null) {
                return;
            }
            String ts = "", name;
            if (time != null) {
                ts = String.format("%1$tH:%1$tM:%1$tS:%1$tL ", time);
            }
            name = keepSame ? null : String.format("%1$ty-%1$tm-%1$td/%1$tp%1$tI", time == null ? new Date() : time);
            File file = null;
            if (name != null && !name.equals(this.logFileName)) {
                this.cleanup();
                this.logFileName = name;
                file = new File(this.rootPath + name + ".txt");
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            if (logFile == null) {
                logFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, append), DEFAULT_CHARSET));
            }
            logFile.println(ts + header + line);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Logs one line in a log file with the current time as a timestamp.
     * @param line      the line to log.
     * @param keepSame  if true writes to the same file as the previous commit even though the hour has changed since then.
     * @return this object
     * @see #commit(java.util.Date, java.lang.String, java.lang.String, boolean) commit()
     */
    public ZLog log (String line, boolean keepSame) {
        try {
            commit (new Date(), "", line, false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return this;
    }
    
    /**
     * Formats a line using the given format string and arguments and writes it to a log file with the current time as a timestamp.
     * @param fmt   the given format string.
     * @param args  the given arguments.
     * @return this object
     * @see #log(java.lang.String, boolean) log()
     */
    public synchronized ZLog logf (String fmt, Object ... args) {
        try {
            commit(new Date(), "", String.format(fmt, args), false);
        } catch (Exception ex) {
            ex.printStackTrace();

        }
        return this;
    }

    /**
     * Formats a line using the given format string and arguments, writes it to a log file with the current time as a timestamp
     * and flushes the file.
     * @param fmt   the given format string.
     * @param args  the given arguments.
     * @return this object
     * @see #logf(java.lang.String, java.lang.Object...) logf()
     */
    public synchronized ZLog logNow (String fmt, Object ... args) {
        try {
            commit(new Date(), "", String.format(fmt, args), false);
            if (logFile != null)
                logFile.flush();
        } catch (Exception ex) {
            ex.printStackTrace();

        }
        return this;
    }

    /**
     * Formats a line using the given format string and arguments, writes it to a log file with the current time as a timestamp,
     * flushes the file and also writes the line to the console.
     * @param fmt   the given format string.
     * @param args  the given arguments.
     * @return this object
     * @see #logNow(java.lang.String, java.lang.Object...) logNow()
     */
    public synchronized ZLog logEvent (String fmt, Object ... args) {
        try {
            System.out.printf(fmt, args);
            System.out.println();
            commit(new Date(), "", String.format(fmt, args), false);
            if (logFile != null)
                logFile.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return this;
    }
    
    /**
     * Calls {@link #logEvent(java.lang.String, java.lang.Object...) logEvent()} to log an event with the given exception as text,
     * then follows it in the file and on the console by the stack trace of the exception.
     * @param ex    the given exception.
     * @return this object
     * @see #logEvent(java.lang.String, java.lang.Object...) logEvent()
     */
    public synchronized ZLog logException (Throwable ex) {
        logEvent(ex.toString());
        ex.printStackTrace();
        if (logFile != null) {
            ex.printStackTrace(logFile);
            logFile.flush();
        }
        return this;
    }

    /**
     * Flushes the current log file.
     */
    public synchronized void flush() {
        if (logFile != null) {
            logFile.flush();
        }
    }

    /**
     * Flushes and closes the current log file.
     */
    public synchronized void cleanup() {
        if (logFile != null) {
            logFile.flush();
            logFile.close();
        }
        this.logFileName = null;
        this.logFile = null;
    }

    /**
     * Returns the root pathname assigned to the logger.
     * <p>
     * A root path is assigned by the class {@link #ZLog(java.lang.String, org.spiderwiz.zutils.ZConfig) consructor} or by
     * {@link #setRootPath(java.lang.String) setRootPath()}.
     * @return the root pathname assigned to the logger.
     * @see #setRootPath(java.lang.String) setRootPath()
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * Sets a new root pathname to the logger.
     * @param rootPath  root pathname to set.
     * @see #getRootPath()
     */
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
}
