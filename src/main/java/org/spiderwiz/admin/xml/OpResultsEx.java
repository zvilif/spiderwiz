package org.spiderwiz.admin.xml;

import org.spiderwiz.zutils.ZDate;

/**
 *
 * @author Zvi 
 */
public class OpResultsEx extends OpResults {
    public final static String OK = "OK";
    public final static String FAILED = "FAILED";

    public OpResultsEx(String status) {
        this.status = status;
        this.time = ZDate.now().format(ZDate.FULL_DATE);
    }
    
}
