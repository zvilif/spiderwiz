package org.spiderwiz.core;

/**
 * Global constants
 * @author Zvi 
 */
class CoreConsts {
    public static final String NO_CONFIG_FILE = "Could not open setting file %1$s";
    public static final String WELCOME = "%1$s ver. %2$s (core version %3$s) has been initiated successfully";
    public static final String INVALID_UUID = "Invalid application UUID property %s. A new value has been generated";
    public static final String GENERAL_RESET = "Performing general application reset";
    public static final String INVALID_OBJECT_TYPE = "Could not create a data object of type '%s'";
    public static final String MODIFIED_BY = "modified by";
    public static final String EXCEPTION_IN_ZBUFFER = "Exception when using ZBuffer";
    public static final String EXCEPTION_IN_ZPIPE = "Exception when using ZPipe";

    public static final String RESEND_NON_ACKED = "Resending lines %1$d through %2$d due to a gap in ACK number";
    public static final String HAS_RESET = "$%1$s counter has been reset to zero. Previous value was %2$x";
    public static final String ALL = "all";
    public static final String LOST_LINES = "%1$d $%2$s (%3$x-%4$x) lines have been lost";
    public static final String ACK_COMMAND = "^ACK,%1$s,%2$s,%3$s,%4$d";
    public final static String EVENT_BUFFER_FULL = "Event buffer full when firing an event for a data object of type %s";
    public final static String NULL_OBJECT_CODE = "ObjectCode cannot be null";
    public final static String RESENDING_LOSSESS =
        "Object:%1$s, target:%2$s, resending lines %3$d through %4$d due to a gap in ACK number";
    static final String DEFAULT_ARCHIVE_FILE_EXTENSION = ".arc";
    static final String DEFAULT_CHARSET = "UTF-8";
    
    class AlertMail {
        static final String CLASS = "class";
        static final String SMTP = "smtp";
        public static final String ALERT_MAIL_BODY2 = "Event time: %1$s";
        public static final String SENT_BY = "Sent by %1$s running at %2$s";
        public static final String NO_DISKSPACE_BODY = "server has less than %1$dGB free disk space";
        public static final String DISCONNECT_ALERT = "%1$s at %2$s has been disconneted!";
        public static final String IMPORT_DISCONNECT_ALERT = "Import Server %1$s at %2$s has been disconnected!";
        public static final String RESUME_NOTIFICATION = "%1$s at %2$s is back to normal operation";
        public static final String IMPORT_RESUME_NOTIFICATION = "Import channel %1$s at %2$s is back to normal operation";
        public static final String IDLE_ALERT = "%1$s at %2$s is idle!";
        public static final String IMPORT_IDLE_ALERT = "Import channel %1$s at %2$s is idle!";
        public static final String WHEN_PARSING_LINE = "When processing the following line:\\n%s";
        public static final String WHEN_PARSING_COMMAND = "Raw input line:\n%1$s\nExpanded line:\n%2$s";
        public static final String ADDITIONAL_INFO = "Additional information:";
        public static final String EXCEPTION_BODY4 = "Exception details:";
        public static final String PARSING_ERROR_ALERT = "Exception when processing a line from %1$s running at %2$s";
        public static final String EXCEPTION_MAIN_INIT = "Exception while initializing Main class";
        public static final String EXCEPTION_CHANNEL_MONITORING = "Exception while monitoring channel %s";
        public static final String EXCEPTION_INIT_CLIENT_CHANNEL = "Exception while initializing client channel %s";
        public static final String IMPORT_PARSING_ERROR_ALERT = "Exception when parsing a line from Import Server %1$s on %2$s";
        public static final String COMMIT_ALERT = "Exception when committing object %s";
        public static final String OBJECT_PARSING_ERROR_ALERT = "Exception when processing an object from %1$s running at %2$s";
        public static final String UNKNOWN_OBJECT_ERROR = "Exception when tyring to create an unknown object %s";
        public static final String APPLICATION_EXCEPTION_MESSAGE = "An application exception in %1$s running at %2$s";
        public static final String APPLICATION_ALERT_MESSAGE = "An application alert from %1$s running at %2$s";
        public static final String APPLICATION_NOTIFICATION_MESSAGE = "An application notification from %1$s running at %2$s";
        public static final String WHILE_CONNECTING = "while trying to connect to %s";
        public static final String DATABASE_EXCEPTION = "Database exception";
        final static String WHEN_RESTORING = "Exception in archive processing when restoring object codee %s";
        final static String WHEN_DELETING = "Exception in archive processing when restoring object code %s";
        final static String EXCEPTION_RESET = "Exception when resetting object with object code %s";
        final static String WHEN_ABORT_ALL_QUERIES = "Exception when aborting all queries";
        final static String WHEN_REMOVING_OBSOLETE_QUERIES = "Exception when removing obsolete queries";
        final static String WHEN_POSTING_A_QUERY = "Exception when posting a query with object code %s";
        final static String WHEN_ABORTING_A_QUERY = "Exception when aborting a query with object code %s";
        final static String WHEN_HANDLING_A_QUERY = "Exception when handling a query with object code %s";
        final static String WHEN_PARSING_EMPTY_RIC = "Exception when parsing an empty RIC";
        final static String EXCEPTION_INIT = "Exception when trying to initialize a channel";
        final static String EXCEPTION_IMPORT_INIT = "Exception when trying to initialize an import channel";
        final static String PROPERTY_STRING = "Property string: %s";
        public static final String MAIL_CLASS_FAILED = "Failed to instantiate a mail class %1$s because %2$s";
        public static final String CHANNEL_CLASS_FAILED = "Failed to instantiate a channel class %1$s because %2$s";
        public static final String SERVER_CLASS_FAILED = "Failed to instantiate a server class %1$s because %2$s";
        final static String EXCEPTION_TRANSMIT_RIM = "Exception when transmiting a message to an import channel";
        final static String MESSAGE_TO_SEND = "Message to send: %s";
        public static final String ALERT_FONT = "color:red";
        public static final String OK_FONT = "font-size:x-large;color:green";
    }
    
    static final String XADMIN_CLASSPATH = "org.spiderwiz.admin.imp.XAdminQuery";
    static final String NEED_INCLUDE_XADMIN =
        "Include spiderwiz-admin.jar in your project in order to use www.spiderwiz.org/SpiderAdmin";

    public class DataChannel {
        public static final String LOGIN_COMMAND = "^L";
        public static final String ACK_LOGIN_COMMAND = "^LACK";
        public static final String ACK = "^ACK";
        public static final String RESET_COMMAND = "^Reset";
        public static final String REMOVE_NODES = "^RemoveNode";
        public static final String PRODUCER = "producer";
        public static final String CONSUMER = "consumer";
        public static final String LOGIN_OK = "OK";
        public static final String LOGIN_FAILED = "FAILED";
        public static final String ALERT = "alert";
        public static final String CLIENT = "client";
        public static final String SERVER = "server";
        public static final String BOTH = "both";
        public static final String LOGINPUT = "loginput";
        public static final String LOGOUTPUT = "logoutput";
        public static final String NO = "no";
        public static final String RAW = "raw";
        public static final String FULL = "full";
        public static final String COMPRESS = "compress";
        public static final String VERBOSE = "verbose";
        public static final String ZIP = "zip";
        public static final String LOGICAL = "logical";
        public static final int LOG_NONE = 0, LOG_RAW = 1, LOG_FULL = 0x10, LOG_VERBOSE = 0x11, LOG_UNDEFINED = 0xf000;
        public static final String USER = "user";
        public static final String GATEWAY = "gateway";
        public static final String PORT_CLOSED = "port closed";
        public static final String LOGGED_IN = "%2$s version %3$s (core version %4$s) has logged in from %1$s";
        public static final String BOTH_CONSUMERS  =
            "Handshare with %2$s version %3$s (core version %4$s) logging from %1$s failed because both sides are consumers";
        public static final String BOTH_PRODUCERS  =
            "Handshare with %2$s version %3$s (core version %4$s) logging from %1$s failed because both sides are producers";
        public static final String LOG_ACKED =
            "Login to %1$s has been acknowledged by %2$s version %3$s (core version %4$s)";
        public static final String LOGIN_REFUSED = "Handshake failed";
        public static final String REQ_APP_RESET = "Received a request from %2$s running at %3$s to reset objects: %1$s";
        public static final String REQ_RESET = "Received a request to reset objects: %1$s";
        public static final String NODE_DISCONNECTED = "%1$s running at %2$s dropped";
        public static final String COMPRESSION_ACK = "Compression has been approved by %1$s";
        public static final String COMPRESSION_REQ = "Compression approval has been requested by %1$s";
        public static final String IDLE_MESSAGE = "%1$s at %2$s is %3$s!";
        public static final String IDLE_READING = "idle";
        public static final String IDLE_WRITING = "not reading from the socket";
        public static final String SEND_ERROR =
            "Channel buffer full when writing to %1$s. All pending lines have been lost";
        public static final String CONNECTION_FAIL = "Connection to %1$s failed on %2$s";
        public static final String CHANNEL_ERROR = "Exception while handling channel %1$s";
        public static final String DISCONNECT_CHANNEL = "A connection from %1$s has been dropped. Reason: %2$s";
        public static final String CONNECTION_OK = "A connection from %1$s has been established";
    }
    
    static class OpResults {
        public static class StatusCodes {
            static final String OK = "OK";
            static final String FAILED = "failed";
            static final String DISCONNECTED = "disconnected";
        }
    }
    
    static class Channel {
        static final int DEFAULT_RECONNECT_WAIT = 60;
        static final String CLOSED_BY_OTHER = "Channel closed by other peer";
        static final String CLOSED_BY_US = "Channel closed";
        static final String PING_COMMAND = "$Ping";
        static final String PONG_COMMAND = "$Pong";
        static final String COMPRESS_REQ = "$CompressReq";
        static final String COMPRESS_ACK = "$CompressAck";
        static final String INFILE = "infile";
        static final String OUTFILE = "outfile";
        static final String BUFFERFILE = "bufferfile";
        static final String CLASS = "class";
    }
    
    static class Zip {
        final static int COMPRESSION_SIGNAL = 3;
    }
    
    static class DataNode {
        static final String PRODUCER_LISTENING = "Producer is listening to consumers on %1$s";
        static final String CONSUMER_LISTENING = "Consumer is listening to producers on %1$s";
        static final String PRODUCER_LISTENING_FAILED = "Producer has failed to listen on %1$s. Reason:%2$s";
        static final String CONSUMER_LISTENING_FAILED = "Consumer has failed to listen on %1$s. Reason:%2$s";
        static final String PRODUCER_STOPPED_LISTENING = "Producer stopped listening to consumers on %1$s";
        static final String CONSUMER_STOPPED_LISTENING = "Consumer stopped listening to producers on %1$s";
        static final String MULTIPLE_APP_ON_SOCKET  =
            "Warning: multiple data channels with the same application and source: %s!";
        static final int MAX_CLIENT_CONNECTIONS = 99;
        static final int MAX_SERVER_LISTENERS = 99;
    }
    
    static class ImportChannel {
        static final String NAME = "name";
        static final String IP = "ip";
        static final String PORT = "port";
        static final String INFILE = "infile";
        static final String OUTFILE = "outfile";
        static final String CHARSET = "charset";
        static final String LOGINPUT = "loginput";
        static final String LOGOUTPUT = "logoutput";
        static final String CONNECTION_FAIL = "Connection to %1$s failed on %2$s";
        static final String DISCONNECT = "%1$s was disconnected. Reason: %2$s";
        static final String CONNECTION_OK = "Connection to %1$s succeeded";
        static final String LOGIN_ACKED = "Login to %1$s port %2$d has been acknowledged: %3$s";
    }

    static class DataNodeInfo {
        static final String Name = "Name";
        static final String Version = "Version";
        static final String CoreVersion = "Core version";
        static final String RemoteAddress = "Remote address";
        static final String Input = "Input";
        static final String Output = "Output";
        static final String ActivitySubTitle = "messages per minute";
        static final String LastInput = "Last input";
        static final String AverageDelay = "Average delay";
        static final String AverageDelaySubTitle = "in seconds";
        static final String MaximumDelay = "Maximum delay";
        static final String ClockDifference = "Clock difference";
        static final String ClockDiffSubTitle = "in seconds";
        static final String UncompressedInput = "Uncompressed input";
        static final String InBandwidth = "In-bandwidth";
        static final String OutBandwidth = "Out-bandwidth";
        static final String BandwidthSubTitle = "average bytes p/s";
        static final String Status = "Status";
        static final String ConnectedSince = "Connected since";
        static final String Errors = "Errors";
        enum StatusCode {OK, DISCONNECTED, IDLE};
        final static String[] statusText = {"OK", "Disconnected", "Idle"};
    }
    
    static class ApplicationInfo {
        static final String Name = "Name";
        static final String Version = "Version";
        static final String CoreVersion = "Core version";
        static final String RemoteAddress = "Remote address";
        static final String Input = "Input";
        static final String Output = "Output";
        static final String ActivitySubTitle = "messages per minute";
        static final String LastInput = "Last input";
        static final String LastOuput = "Last output";
        static final String UncompressedInput = "Uncompressed input";
        static final String CompressedInput = "Compressed input";
        static final String BandwidthSubTitle = "average bytes p/s";
        static final String Status = "Status";
        static final String Since = "Since";
        enum StatusCode {OK, DISCONNECTED};
        final static String[] statusText = {"Connected", "Disconnected"};
    }

    static class Serializer {
        static final String LIST_PARSE_ERROR = "Value %s is expected to encode a list but is not enclosed by []";
        static final String SET_PARSE_ERROR = "Value %s is expected to encode a set but is not enclosed by <>";
        static final String MAP_PARSE_ERROR = "Value %s is expected to encode a map but is not enclosed by <>";
        static final String CLASS_PARSE_ERROR = "Value %s is expected to encode a class field but is not enclosed by {}";
    }

    static final int SEQUENCE_MODULU = 0x10000;
}
