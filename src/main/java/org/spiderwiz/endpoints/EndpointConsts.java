/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spiderwiz.endpoints;

import java.net.URI;

/**
 *
 * @author Zvi 
 */
public class EndpointConsts {
    static final String WEBSOCKET_URI = "/spiderwizWebSocket/{role}";
    static final String FILTER_URI = "/spiderwizWebSocket/*";
    static final String WEBSOCKET_GATEID = "WebSockets";
    static final String SESSION_ADDRESS = "SessionAddress";
    public static final String IS_PRODUCER = "IsProducer";
    public static final String ZOBJECT = "zobject";
    
    /**
     * Get the full Websocket URI
     * @param producer  true if the requestor is a producer, false if a consumer
     * @return the URI after replacing the {role} parameter by "consumer" if the requestor is a producer, or by "producer"
     * if the requestor is a consumer.
     */
    static String getWebsocketUri(boolean producer) {
        return WEBSOCKET_URI.replace("{role}", producer ? "consumer" : "producer");
    }
    
    /**
     * Find if the given URI is for producer or a consumer
     * @param uri
     * @return true if producer
     */
    static boolean isProducer(URI uri) {
        return uri.toString().contains("producer");
    }
}
