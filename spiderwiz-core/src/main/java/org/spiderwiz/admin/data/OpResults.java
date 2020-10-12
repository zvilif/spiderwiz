package org.spiderwiz.admin.data;

import org.spiderwiz.annotation.WizField;
import org.spiderwiz.annotation.WizSerializable;
import org.spiderwiz.zutils.ZDate;

/**
 * Describes the results of a <a href="http://spideradmin.com">SpiderAdmin</a> button operation.
 * @see PageInfo
 */
@WizSerializable
public class OpResults {

    @WizField private String status;
    @WizField private String time;

    /**
     * Represents the text "OK".
     */
    public final static String OK = "OK";

    /**
     * Represents the text "FAILED".
     */
    public final static String FAILED = "FAILED";

    /**
     * Constructs an empty object
     */
    public OpResults() {
    }

    /**
     * Constructs an object with the given status value and the current time.
     * @param status    User defined status description. It is recommended to use either {@link #OK} or {@link #FAILED}.
     */
    public OpResults(String status) {
        this.status = status;
        this.time = ZDate.now().format(ZDate.FULL_DATE);
    }

    /**
     * Gets the value of the status property.
     * <p>
     * The {@code status} property holds a user defined status description.
     * It is recommended to use either {@link #OK} or {@link #FAILED}.
     * @return the status property
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * <p>
     * The {@code status} property holds a user defined status description.
     * It is recommended to use either {@link #OK} or {@link #FAILED}.
     * @param value
     */
    public void setStatus(String value) {
        this.status = value;
    }

    /**
     * Gets the value of the time property.
     * <p>
     * The {@code time} property holds the time of the operation formatted as a String.
     * @return the time property
     */
    public String getTime() {
        return time;
    }

    /**
     * Sets the value of the time property.
     * <p>
     * The {@code time} property holds the time of the operation formatted as a String.
     * @param value
     */
    public void setTime(String value) {
        this.time = value;
    }
}
