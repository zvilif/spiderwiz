package org.spiderwiz.plugins;

/**
 * Various constants used in the package
  * @author zvil
*/
public class PluginConsts {
    static final String TYPES[] = {"import", "producer", "consumer"};
    public static final String EXCEPTION_IN_ZBUFFER = "Exception when using ZBuffer";
    
    public static class WebSocket {
        public static final String WEBSOCKET_SCHEMA = "ws:";
        public static final String WSS_SCHEMA = "wss:";
        public static final String WEBSOCKET_CLIENT = "WebSocket client";
        public static final String WEBSOCKET = "websocket";
        static final String NO_URI = "No URI specified for WebSocket %1$s-%2$d";
        static final String DOUBLE_SERVER = "%1$s server-%2$d - multiple WebSocket servers of the same type are not allowed";
    }

    public static class TcpSocket {
        public static final String IP = "ip";
        public static final String PORT = "port";
        static final String PORT_ID = "port ";
        static final String NO_TCP_PARAMS = "No IP address or port number specified for %1$s-%2$d";
        static final String NO_PORT = "No port number specified for %1$s server-%2$d";
    }
    
    public static class FileChannel {
        public static final String INFILE = "infile";
        public static final String OUTFILE = "outfile";
        static final String NO_FILES = "Both input and output file names missing from configuration of %1$s-%2$d";
    }
    
    public static class SpiderAdmin {
        public static final String SPIDERADMIN_CLASS = "org.spiderwiz.admin.imp.SpiderAdminSocket";
        public static final String CLASS_NOT_FOUND = "Missing spiderwiz-admin dependency";
    }
}
