package org.spiderwiz.core;

import org.spiderwiz.zutils.ZDate;

/**
 * Objects implementing this interface are used in conjuction with Channel to provide channel and event handling.
 * @author @author  zvil
 */
abstract class ChannelHandler {

    /**
     * Implement this to provide data handling code.
     * @param line  one line of input text
     * @param ts    timestamp attached to the line. If null, current time will be used.
     */
    abstract void processLine(String line, ZDate ts);

    /**
     * Implement this to do something when connection is established
     */
    abstract void onConnect();

    /**
     * Implement this to do something when the socket is disconnected
     * @param reason    reason of disconnection.
     */
    abstract void onDisconnect(String reason);

    /**
     * Override this method to handle various events that happen on the channel.
     * @param eventCode enum EventCode {
                    CONNECT_FAILED, // Connection failed. additionalInfo contains the reason as Exception object.
                    SEND,           // A line is physically sent. additional info contains the line as String.
                    COMPRESS_ACK,   // Data compression over the channel has been acknowledged.
                    COMPRESS_REQ,   // Data compression over the channel has been requested.
                    PING            // Ping command has been received.
                    PONG            // Pong command has been received. additionalInfo contains the clock difference
                                    // from the peer node as long
                    ERROR           // Any other exception. additionalInfo contains the exception.
                    PHYSICAL_READ   // Bytes where physically read from the channel. addtionalInfo contains number of bytes
                    PHYSICAL_WRITE  // Bytes where physically written to the channel. addtionalInfo contains number of bytes
                };
     * @param additionalInfo    additional information as explained above.
     */
    void onEvent(Channel.EventCode eventCode, Object additionalInfo) {}
    
    /**
     * Override this method to provide code for monitoring the channel at the set time interval
     */
    void monitor() {}
    
    boolean isProducer() {return false;}
}
