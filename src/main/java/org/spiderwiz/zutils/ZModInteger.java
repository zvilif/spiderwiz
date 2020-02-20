package org.spiderwiz.zutils;

/**
 * Represents an integer number with a defined modulo base.
 */
public class ZModInteger implements Comparable<ZModInteger>{
    private final int base;
    private int myValue;

    /**
     * Constructs an object with the given modulo base and the initial value of zero.
     * @param base  the modulo base.
     */
    public ZModInteger(int base) {
        this(base, 0);
    }

    /**
     * Constructs an object with the given modulo base and the given initial value.
     * @param base  the modulo base.
     * @param value the initial value.
     */
    public ZModInteger(int base, int value) {
        this.base = base;
        myValue = value;
    }

    /**
     * Returns the object as an integer.
     * @return the object value as an integer.
     */
    public final int toInt() {
        return myValue;
    }

    /**
     * Sets the value of the object to the given value modulo the object defined {@code base}.
     * @param value the value to set in the object after applying modulo calculation.
     * @return the set value.
     */
    public final int setValue(int value) {
        return (myValue = value % base);
    }
    
    /**
     * Returns the modulo sum of the object and the parameter.
     * @param value the value to add.
     * @return the modulo sum of the object and the parameter.
     */
    public final int add(int value) {
        return (myValue + value) % base;
    }
    
    /**
     * Returns the modulo result of the subtraction of the object from the parameter.
     * <p>
     * The modulo result is calculated as follows:
     * <ol>
     * <li>The value of the object is subtracted from the parameter.</li>
     * <li>Modulo calculation is applied on the result.</li>
     * <li>If the absolute value of the result is less than half the modulo base then return the result as is.</li>
     * <li>Otherwise return the negation of the result.
     * </ol>
     * @param value the value to subtract the object value from.
     * @return the modulo result of the subtraction of the object from the parameter.
     */
    public final int diff(int value) {
        return -compareTo(value);
    }

    /**
     * Returns the modulo multiplication of the object by the parameter.
     * @param factor    the value to multiply by.
     * @return the modulo multiplication of the object by the parameter.
     */
    public final int multiply(int factor) {
        return (myValue * factor) % base;
    }
    
    /**
     * Returns the modulo whole division of the object by the parameter.
     * @param divisor   the value to divide by.
     * @return the modulo whole division of the object by the parameter.
     */
    public final int divide(int divisor) {
        return (myValue / divisor) % base;
    }
    
    /**
     * Increments the object by 1 modulo the defined {@code base}.
     * @return the incremented value.
     */
    public final int inc() {
        return myValue = ++myValue % base;
    }
    
    /**
     * Increments the object by 1 modulo the defined {@code base} and returns the object value before the increment.
     * @return the object value before the increment.
     */
    public final int postInc() {
        int val = myValue++;
        myValue %= base;
        return val;
    }
    
    /**
     * Increments the object by 1 modulo the defined {@code base} and returns the object value before the increment as a
     * hexadecimal value.
     * @return the object value before the increment as a hexadecimal value.
     */
    public final String postIncAsHex() {
        int val = myValue++;
        myValue %= base;
        return String.format("%x", val);
    }

    /**
     * Decrements the object by 1 modulo the defined {@code base}.
     * @return the decremented value.
     */
    public final int dec() {
        return myValue = --myValue % base;
    }
    
    /**
     * Decrements the object by 1 modulo the defined {@code base} and returns the object value before the decrement.
     * @return the object value before the decrement.
     */
    public final int postDec() {
        int val = myValue--;
        myValue %= base;
        return val;
    }
    
    /**
     * Modulo compares the object with the parameter.
     * <p>
     * The relation between the two values is determined as follows:
     * <ol>
     * <li>The parameter is subtracted from the value of the object.</li>
     * <li>Modulo calculation is applied on the result.</li>
     * <li>If the absolute value of the result is less than half the modulo base then the modulo relation of the two values
     * is identical to their arithmetic relation.</li>
     * <li>Otherwise the modulo relation of the two values is the opposite of their arithmetic relation.
     * </ol>
     * @param value the value to compare the object to.
     * @return a negative integer, zero, or a positive integer as this object is modulo less than, equal to, or greater than the
     * parameter.
     */
    public final int compareTo(int value) {
        int diff = myValue - value % base;
        return Math.abs(diff) >= base /2 ? -diff : diff;
    }
    
    /**
     * Compares this object to another modulo object.
     * <p>
     * See {@link #compareTo(int)} how to determine the relation between the values.
     * @param o     the object to compare to.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the
     * parameter.
     * @see #compareTo(int)
     */
    @Override
    public final int compareTo(ZModInteger o) {
        return compareTo(o.myValue);
    }

    /**
     * Returns the modulo maximum between the object and the parameter.
     * <p>
     * See {@link #compareTo(int)} how to determine the relation between the values.
     * @param value the value to compare with
     * @return the modulo maximum between the object and the parameter.
     * @see #compareTo(int)
     */
    public final int max(int value) {
        return compareTo(value) > 0 ? myValue : value % base;
    }

    /**
     * Returns the modulo minimum between the object and the parameter.
     * <p>
     * See {@link #compareTo(int)} how to determine the relation between the values.
     * @param value the value to compare with
     * @return the modulo minimum between the object and the parameter.
     * @see #compareTo(int)
     */
    public final int min(int value) {
        return compareTo(value) < 0 ? myValue : value % base;
    }

    /**
     * Assumes the parameter represents an hexadecimal integer value, converts it to an integer and returns the result modulo the
     * object defined {@code base}.
     * @param hex       the value to convert.
     * @return the parameter converted to an integer modulo the object defined base.
     */
    public final int fromHex(String hex) {
        return Integer.parseInt(hex, 16) % base;
    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object.
     */
    @Override
    public final String toString() {
        return String.valueOf(myValue);
    }
}
