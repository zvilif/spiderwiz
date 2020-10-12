package org.spiderwiz.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a serializable field of a serializable class.
 * <p>
 * Use this annotation to decorate member fields of a data object class (a class that extends {@link org.spiderwiz.core.DataObject
 * DataObject}) or a serializable class (a class that is annotated by or inherits {@link org.spiderwiz.annotation.WizSerializable
 * WizSerializable} annotation) that you want to be serialized and shared across the network.
 * <p>
 * A class field annotated as <strong>WizField</strong> must be <em>serializable</em>. Any of the following field types is
 * serializable:
 * <ul>
 * <li>{@link javax.lang.model.type.PrimitiveType Primitive} types</li>
 * <li>{@link java.lang.String String} or {@link java.lang.Number Number} classes </li>
 * <li>{@link java.lang.Enum enum} classes </li>
 * <li>{@link java.util.Date Date}, {@link javax.xml.datatype.XMLGregorianCalendar XMLGregorianCalendar} and their derivatives</li>
 * <li>A class that extends {@link org.spiderwiz.core.DataObject DataObject}
 * <li>A class that is annotated by or inherits the {@link org.spiderwiz.annotation.WizSerializable WizSerializable} annotation</li>
 * <li>A class that is annotated by {@link javax.xml.bind.annotation.XmlType XmlType}</li>
 * <li>A class that implements <a href="#SerializationMethods">serialization methods</a></li>
 * <li>Aggregates (array, {@link java.util.List List}, {@link java.util.Set Set} or {@link java.util.Map Map}) of the above types (in
 * case of a Map both the key and the value need to be serializable). </li>
 * </ul>
 * <div class="summary">
 * <div style="font-family: 'DejaVu Sans', Arial, Helvetica, sans-serif">
 * <ul class="blockList">
 * <li class="blockList">
 * <!--   -->
 * <h2><a id="SerializationMethods"></a>Serialization and Deserialization Methods</h2>
 * <div class=block>
 * Other than the classes mentioned above, a class is considered serializable if it implements a<em> serialization method </em>and a
 * <em> deserialization method</em>. A serialization method is any of the following:
 * </div>
 * <table class="memberSummary">
 * <caption><span>Serialization Methods</span><span class="tabEnd">&nbsp;</span></caption>
 * <tr>
 * <th class="colFirst" scope="col" style="width:27em">Method</th>
 * <th class="colLast" scope="col">Serialization Type</th>
 * </tr>
 * <tr class="altColor">
 * <td class="colFirst"> {@code String toString()}
 * </td>
 * <td class="colLast"><div class=block>
 * Simple string conversion.
 * </div></td>
 * </tr>
 * <tr class="rowColor">
 * <td class="colFirst"> {@code String format(String format)}
 * </td>
 * <td class="colLast"><div class=block>
 * String conversion using a format specifier. The format to use shall be specified by the {@link #format() format} element of
 * {@code WizField} annotation.
 * </div>
 * </td>
 * </tr>
 * </table>
 * <div class=block>
 * <p>&nbsp;</p>
 * Classes that implement a serialization method must also implement a deserialization method which is one of the
 * following:
 * </div>
 * <table class="memberSummary">
 * <caption><span>Deserialization Methods</span><span class="tabEnd">&nbsp;</span></caption>
 * <tr>
 * <th class="colFirst" scope="col" style="width:27em">Method</th>
 * <th class="colLast" scope="col">Deserialization Type</th>
 * </tr>
 * <tr class="altColor">
 * <td class="colFirst">
 * <div class=block>Class constructor with a {@code String} argument</div>
 * </td>
 * <td class="colLast"><div class=block>
 * Converts from a string by the class constructor
 * </div></td>
 * </tr>
 * <tr class="rowColor">
 * <td class="colFirst">
 * {@code static <T> fromString(String val)}
 * </td>
 * <td class="colLast"><div class=block>
 * Converts from a string by a static method
 * </div>
 * </td>
 * </tr>
 * <tr class="altColor">
 * <td class="colFirst">
 * {@code static <T> parse(String val, String format)}
 * </td>
 * <td class="colLast"><div class=block>
 * Converts from a string using a format specifier. The format to use shall be specified by the {@link #format() format}
 * element of {@code WizField} annotation.
 * </div>
 * </td>
 * </tr>
 * </table>
 * </li>
 * </ul>
 * </div>
 * </div>
 */
@Inherited
@Target(value = {ElementType.FIELD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface WizField {
    /**
     * A format string to be used when the annotated field is serialized.
     * <p>
     * This element is mandatory when the annotated field is serialized using the {@code format()} method. It specifies the format
     * specifier that the method gets as its argument.
     * <p>
     * The element is optional when the annotated field is of type {@link java.util.Date Date} or
     * {@link javax.xml.datatype.XMLGregorianCalendar XMLGregorianCalendar}. If used, it specifies the pattern used for serialization
     * as defined in {@link java.text.SimpleDateFormat SimpleDateFormat}. If not used, the pattern {@code ddMMyyHHmmss} will be used,
     * which means that milliseconds are truncated to centiseconds.
     * @return a format string or an empty string that indicates default serialization pattern.
     */
    String format() default "";
    
    /**
     * Specifies the precision to be used when serializing a float or double value.
     * <p>
     * The field value is interpreted as the number of digits after the decimal point to retain in the serialized string. A negative
     * value indicates that the exact value shall be retained.
     *
     * @return the precision to be used when serializing a float or double value.
     */
    int precision() default -1;
    
    /**
     * Specifies whether to ignore time zone differences when transferring a {@link java.util.Date Date} or
     * {@link javax.xml.datatype.XMLGregorianCalendar XMLGregorianCalendar} value across different time zones.
     * <p>
     * By default when a {@link java.util.Date Date} or {@link javax.xml.datatype.XMLGregorianCalendar XMLGregorianCalendar} value is
     * serialized, the value is converted to GMT time so that the receiving node will convert it back to local time zone and get it
     * right even if the peer nodes are deployed in different time zones. By setting this element to {@code true} you can instruct
     * the serializer (and the deserializer) to ignore time zone differences and transfer the local time value without conversion.
     * @return true if local time shall be transferred without conversion.
     */
    boolean ignoreZoneDifference() default false;
}
