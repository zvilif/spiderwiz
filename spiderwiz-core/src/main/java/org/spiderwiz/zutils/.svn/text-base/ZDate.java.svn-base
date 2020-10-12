package org.spiderwiz.zutils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Extension of {@link Date} that provides extra utility fields and methods.
 */
public final class ZDate extends Date {

    /**
     * Represents {@code "d/M/yyyy HH:mm"} time format.
     */
    public final static String DATE_FORMAT = "d/M/yyyy HH:mm";

    /**
     * Represents {@code "dd/MM/yy-HH:mm:ss"} time format.
     */
    public static final String FULL_DATE = "dd/MM/yy-HH:mm:ss";

    /**
     * Represents {@code "dd/MM/yy-HH:mm:ss:SSS"} time format.
     */
    public static final String FULL_DATE_MILLISECONDS = "dd/MM/yy-HH:mm:ss:SSS";

    /**
     * Represents {@code "dd/MM-HH:mm"} time format.
     */
    public static final String DAY_MONTH_HOUR_MINUTE = "dd/MM-HH:mm";

    /**
     * Represents {@code "HH:mm:ss"} time format.
     */
    public static final String HOUR_MINUTE_SECOND = "HH:mm:ss";

    /**
     * Represents {@code "ddMMyyHHmmss"} time format.
     */
    public static final String TIMESTAMP = "ddMMyyHHmmss";

    /**
     * Represents {@code "ddMMyyHHmmssSSS"} time format.
     */
    public static final String FULL_TIMESTAMP = "ddMMyyHHmmssSSS";

    /**
     * Represents {@code "ddMMyyHHmm"} time format.
     */
    public static final String MINUTE_TIMESTAMP = "ddMMyyHHmm";

    /**
     * Represents {@code "yyyyMMddHHmm"} time format.
     */
    public static final String YYYYMMDDHHMM = "yyyyMMddHHmm";

    /**
     * Represents {@code "yyyyMMddHHmmss"} time format.
     */
    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    /**
     * Represents {@code "yyMMdd"} time format.
     */
    public static final String YYMMDD = "yyMMdd";

    /**
     * Represents {@code "yyMMddHHmm"} time format.
     */
    public static final String YYMMDDHHMM = "yyMMddHHmm";

    /**
     * Represents {@code "yyMMddHHmmss"} time format.
     */
    public static final String YYMMDDHHMMSS = "yyMMddHHmmss";

    /**
     * Represents {@code "MMddyyHHmmss"} time format.
     */
    public static final String MMDDYYHHMMSS = "MMddyyHHmmss";

    /**
     * Represents {@code "yyyy-MM-dd HH:mm:ss"} time format.
     */
    public static final String SQL_DATE = "yyyy-MM-dd HH:mm:ss";

    /**
     * Represents {@code "yyyy-MM-dd'T'HH:mm:ss"} time format.
     */
    public static final String SORTABLE = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Number of milliseconds in a second (1000).
     */
    public static final int SECOND = 1000;

    /**
     * Number of milliseconds in a minute (60,000).
     */
    public static final int MINUTE = SECOND * 60;

    /**
     * Number of milliseconds in an hour (360,000).
     */
    public static final int HOUR = MINUTE * 60;

    /**
     * Number of milliseconds in a day (8,640,000).
     */
    public static final int DAY = HOUR * 24;

    /**
     * The latest possible date (Long.MAX_VALUE).
     */
    public static final ZDate DAY_OF_MESSIAH = new ZDate(Long.MAX_VALUE);
    
    private static final int[] POWER_OF_10 = {1, 10, 100, 1000};

    /**
     * Constructs an object representing the current time, measured to the nearest millisecond.
     * @see #now()
     */
    public ZDate() {
    }

    /**
     * Constructs an object from a value of milliseconds since 1/1/1970 GMT.
     * @param date  the milliseconds since January 1, 1970, 00:00:00 GMT.
     */
    public ZDate(long date) {
        super(date);
    }

    /**
     * Constructs an object from the value represented by a given {@link Date} object.
     * @param date  a Date object whose value shall be copied to the constructed object.
     */
    public ZDate(Date date) {
        setTime(date.getTime());
    }

    /**
     * Constructs an object representing the date of today, with the given hour and minute in local time.
     * @param hour      hour to be set.
     * @param minute    minute to be set.
     */
    public ZDate(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        this.setTime(cal.getTimeInMillis());
    }

    /**
     * Constructs an object representing the date of today, with the given amount of seconds, possibly with a faction, from midnight
     * in local time.
     * @param seconds       amount of seconds, possibly with a faction, from midnight in local time.
     */
    public ZDate(float seconds) {
        Calendar cal = Calendar.getInstance();
        int ms = (int) Math.round(seconds * SECOND);
        int hour = ms / HOUR;
        int minute = (ms % HOUR) / MINUTE;
        int second = (ms % MINUTE) / SECOND;
        ms = ms % SECOND;
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, ms);
        this.setTime(cal.getTimeInMillis());
    }

    /**
     * Constructs an object by parsing the beginning of the given string with a given time format.
     * @param string    the string to parse
     * @param fmt       the format to use
     * @throws ParseException
     * @see SimpleDateFormat
     * @see SimpleDateFormat#parse(java.lang.String) SimpleDateFormat.parse()
     */
    public ZDate(String string, String fmt) throws ParseException {
        setTime(new SimpleDateFormat(fmt).parse(string).getTime());
    }

    /**
     * Returns a new time object set to the current time.
     * <p>
     * Calling {@code ZDate.now()} is equivalent to {@code new ZDate()}.
     *
     * @return a new time object set to the current time.
     * @see #ZDate()
     */
    public static ZDate now() {
        return new ZDate();
    }

    /**
     * Returns the current time formatted into a string.
     * <p>
     * This method is equivalent to {@code new ZDate.format(fmt)}.
     * @param fmt format string
     * @return the current time formatted into a string.
     * @see #format(java.lang.String) format()
     */
    public static String now(String fmt) {
        return now().format(fmt);
    }

    /**
     * Returns the current time as milliseconds since 1/1/1970.
     * @return the current time as milliseconds since 1/1/1970.
     */
    public static long nowAsLong() {
        return new Date().getTime();
    }

    /**
     * Returns a time object with a value equal to the time in this object plus the given amount of milliseconds.
     * @param ms    the amount of milliseconds to add.
     * @return a time object with a value equal to the time in this object plus the given amount of milliseconds.
     */
    public ZDate add(long ms) {
        ZDate date = new ZDate(getTime() + ms);
        return date;
    }
    
    /**
     * Returns a time object with a value equal to the time in this object plus the given amount of the given time units.
     * <p>
     * This method adds the specified amount of the unit defined by the {@code field} parameter to the time of this object and
     * returns the result as a new time object. The possible values of {@code field} are those defined by
     * the Field numbers in {@link Calendar} class. For instance, {@code add(Calendar.DAY_OF_MONTH, -5)} returns a time object that represents the time
     * of 5 days before the time of the object the method is called on.
     * @param field     the time unit as defined by {@link Calendar} class.
     * @param amount    the amount of units to add.
     * @return          a time object with a value equal to the time in this object plus the specified amount of the specified
     *                  time units.
     */
    public ZDate add(int field, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this);
        cal.add(field, amount);
        return new ZDate(cal.getTime());
    }

    /**
     * Returns the difference of time in milliseconds from the time represented by the {@code date} parameter till the time
     * represented by this object.
     * @param date      a time object to calculate difference from.
     * @return the difference of time in milliseconds from the time represented by the parameter till the time represented by this
     * object.
     */
    public long diff(Date date) {
        return this.getTime() - date.getTime();
    }
    
    /**
     * Returns the difference in whole months from the time represented by the {@code date} parameter till the time
     * represented by this object.
     * @param date      a time object to calculate difference from.
     * @return the difference in whole months from the time represented by the parameter till the time represented by this object,
     *          or zero if the parameter is null.
     */
    public int diffMonths(Date date) {
        if (date == null)
            return 0;
        Calendar from = getCalendar(date);
        Calendar till = getCalendar(this);
        int diff = (till.get(Calendar.YEAR) - from.get(Calendar.YEAR)) * 12 + (till.get(Calendar.MONTH) - from.get(Calendar.MONTH));
        int fromDay = from.get(Calendar.DATE);
        int tillDay = till.get(Calendar.DATE);
        if (fromDay > tillDay)
            --diff;
        else if (fromDay == tillDay) {
            // compare hour/minute/second/millisecond part
            from.set(Calendar.DATE, 1);
            till.set(Calendar.DATE, 1);
            from.set(Calendar.MONTH, 1);
            till.set(Calendar.MONTH, 1);
            from.set(Calendar.YEAR, 1970);
            till.set(Calendar.YEAR, 1970);
            if (from.after(till))
                --diff;
        }
        return diff;
    }
    
    /**
     * Returns the time that has elapsed since the time represented by this object in milliseconds.
     * @return the time that has elapsed since the time represented by this object in milliseconds.
     */
    public long elapsed() {
        return now().getTime() - this.getTime();
    }

    /**
     * Returns true if the time of this object is after the time of the given object or if the given object is {@code null}.
     * @param when  the time to compare to.
     * @return true if and only if the time of this object is after the time of the given object or if the given object is null.
     */
    @Override
    public boolean after(Date when) {
        return when == null || super.after(when);
    }

    /**
     * Returns true if the time of the given object is not null and the time of this object is before the time of the given object.
     * @param when  the time to compare to.
     * @return true if and only if the time of the given object is not null and the time of this object is before the time of the
     * given object.
     */
    @Override
    public boolean before(Date when) {
        return when != null && super.before(when);
    }
    
    /**
     * Returns true if this object is between the two given time objects, inclusive.
     * <p>
     * The method returns true if and only if the time of this object is not before the time of {@code from} parameter and not
     * after the time of {@code to} parameter. If {@code from} is {@code null} then it is considered as the time of the Big Bang,
     * i.e. ever. If {@code to} is {@code null} it is considered as the coming of Messiah, i.e. forever.
     * @param from  from time, null is considered as infinite time ago.
     * @param to    to time, null is considered as infinite time ahead.
     * @return true if and only if the object is not before <em>from</em> and not after <em>to</em>.
     */
    public boolean between(ZDate from, ZDate to){
        return (from == null || !before(from)) && (to == null || !after(to));
    }

    /**
     * Returns the earliest between this object and the given object.
     * <p>
     * If the given time object is {@code null} or this time object is strictly before {@code when} then this object is returned,
     * otherwise {@code when} is returned.
     * @param when  time object to compare to.
     * @return this object is it is before the given object or the given object is null, otherwise return the given object.
     */
    public ZDate earliest(ZDate when) {
        return when == null || before(when) ? this : when;
    }
    
    /**
     * Returns the latest between this object and the given object.
     * <p>
     * If the given time object is {@code null} or this time object is strictly after {@code when} then this object is returned,
     * otherwise {@code when} is returned.
     * @param when  time object to compare to.
     * @return this object is it is after the given object or the given object is null, otherwise return the given object.
     */
    public ZDate latest(ZDate when) {
        return when == null || after(when) ? this : when;
    }

    /**
     * Returns the hour of the day that this time object represents as an integer number between 0 and 23.
     * <p>
     * The method overrides the depreciated {@link Date#getHours()} and uses the
     * {@link Calendar#get(int) Calendar.get(Calendar.HOUR_OF_DAY)} replacement.
     * @return the hour of the day in the local time zone that this time object represents as an integer number between 0 and 23.
     */
    @Override
    public int getHours() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * Returns the minutes fraction of this time object as an integer number between 0 and 59.
     * <p>
     * The method overrides the depreciated {@link Date#getMinutes()} and uses the
     * {@link Calendar#get(int) Calendar.get(Calendar.MINUTE)} replacement.
     * @return the minutes fraction of this time object in the local time zone as an integer number between 0 and 59.
     */
    @Override
    public int getMinutes() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this);
        return cal.get(Calendar.MINUTE);
    }
    
    /**
     * Returns the seconds fraction of this time object as an integer number between 0 and 59.
     * <p>
     * The method overrides the depreciated {@link Date#getSeconds()} and uses the
     * {@link Calendar#get(int) Calendar.get(Calendar.SECOND)} replacement.
     * @return the seconds fraction of this time object as an integer number between 0 and 59.
     */
    @Override
    public int getSeconds() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this);
        return cal.get(Calendar.SECOND);
    }

    /**
     * Returns the day of the week represented by this time object.
     * <p>
     * The method overrides the depreciated {@link Date#getDay()} and uses the
     * {@link Calendar#get(int) Calendar.get(Calendar.DAY_OF_WEEK)} replacement.
     * @return the day of the week in the local time zone represented by this object.
     */
    @Override
    public int getDay() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this);
        return cal.get(Calendar.DAY_OF_WEEK);
    }
    
    /**
     * Returns a time object with the date of this object and the given hour and minute.
     * <p>
     * The method returns a new time object whose value is a combination of the date of this object with the given {@code hour}
     * and {@code minutes}. Seconds and milliseconds are set to zero.
     * @param hour      the given hour
     * @param minutes   the given minutes
     * @return an object with the date of this object and the given hour and minute.
     */
    public ZDate setTime(int hour, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new ZDate(cal.getTime());
    }

    /**
     * Formats this object into a time string.
     * @param fmt   the given format.
     * @return the formatted string.
     * @see SimpleDateFormat#format(java.util.Date) SimpleDateFormat.format()
     */
    public String format(String fmt) {
        return new SimpleDateFormat(fmt).format(this);
    }
    
    /**
     * Formats this object into a timestamp string using the format {@code "ddMMyyHHmmssSSS"}.
     * @return the formatted string.
     */
    public String formatFullTimestamp() {
        return format(ZDate.FULL_TIMESTAMP);
    }
    
    /**
     * Formats this object into a rounded timestamp string using the format {@code "ddMMyyHHmmssS}(s){@code "} when the number of 
     * {@code S}s is the given precision.
     * @param precision 0 - round by seconds, 1 - round by 10th of a second, 2 - round by 100th of a second, 3 - round by milliseconds
     * (native format).
     * @return the formatted string.
     */
    public String formatRoundTimestamp(int precision) {
        int factor = 1000 / POWER_OF_10[precision];
        ZDate rounded = new ZDate((getTime() + factor / 2) / factor * factor);
        return rounded.formatFullTimestamp().substring(0, FULL_TIMESTAMP.length() - 3 + precision);
    }
    
    /**
     * Formats this object into a time string representing the time of the object in GMT zone.
     * @param fmt   the given format.
     * @return the formatted string.
     */
    public String formatGMT(String fmt) {
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(this);
    }
    
    /**
     * Parses a formatted time string using the given date object to resolve full time data.
     * <p>
     * Parses the beginning of the {@code string} parameter assuming it contains a time value formatted with the {@code format}
     * parameter. The formatted string may contain partial information, for instance only the time of the day, in which case the method
     * uses the {@code ts} parameter to resolve the full time value. If {@code ts} is {@code null} the current time is used.
     * @param string    the string to parse.
     * @param format    the format to use.
     * @param ts        the base timestamp. If null, the current time is used.
     * @return the parsed and resolved time object.
     * @throws java.text.ParseException
     */
    public static ZDate parseTime(String string, String format, ZDate ts) throws ParseException {
        if (string == null || string.isEmpty())
            return null;
        if (ts == null) {
            ts = now();
        }
        Date t = new SimpleDateFormat(format).parse(string);

        // If the parsed time does not include date, add the date of today
        if (t.getTime() > 0 && t.before(new Date(DAY * 365)))
            return ts.setTimeOfDay(t);
        return new ZDate(t);
    }
    
    /**
     * Parses the beginning of the given string assuming it contains a formatted GMT time string.
     * @param string    the string to parse.
     * @param format    the format to use.
     * @return          the parsed time object.
     * @throws ParseException
     */
    public static ZDate parseGMTtime (String string, String format) throws ParseException {
        Date t = new SimpleDateFormat(format).parse(string);
        Calendar in = Calendar.getInstance();
        in.setTime(t);
        Calendar out = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        out.setTimeInMillis(0);
        out.set(in.get(Calendar.YEAR), in.get(Calendar.MONTH), in.get(Calendar.DAY_OF_MONTH),
                in.get(Calendar.HOUR_OF_DAY), in.get(Calendar.MINUTE), in.get(Calendar.SECOND));
        return new ZDate (out.getTime());
    }

    /**
     * Parses the beginning of the given string assuming it contains a timestamp value in the format {@code "ddMMyyHHmmssSSS"}.
     * @param string    the string to parse.
     * @return          the parsed time object.
     * @throws ParseException 
     */
    public static ZDate parseFullTimestamp(String string) throws ParseException {
        return parseTime(string, ZDate.FULL_TIMESTAMP, null);
    }
    
    /**
     * Parses the beginning of the given string assuming it contains a rounded timestamp string formatted as
     * {@code "ddMMyyHHmmssS}(s){@code "} when the number of {@code S}s is the given precision.
     * @param string    the string to parse.
     * @param precision 0 - round by seconds, 1 - round by 10th of a second, 2 - round by 100th of a second, 3 - round by milliseconds
     * (native format).
     * @return the parsed time object.
     * @throws java.text.ParseException
     */
    public static ZDate parseRoundTime(String string, int precision) throws ParseException {
        return parseFullTimestamp(string + "000".substring(0, 3 - precision));
    }
    
    /**
     * Returns a time object that represents the time of this object after shifting it to GMT time zone.
     * <p>
     * This method assumes that the time represented by this object is in the local time zone. It allocates a new time object and sets
     * it to the literal time of this object shifted to GMT. For instance if this object represents the time
     * 2pm PST and the local time zone is PST, the returned object represents the time 2pm GMT.
     * @return a time object that represents the time of this object after shifting it to GMT time zone.
     */
    public ZDate toGMT() {
        try {
            String gmtTime = formatGMT(FULL_TIMESTAMP);
            return parseFullTimestamp(gmtTime);
        } catch (ParseException ex) {
            return null;
        }
    }

    /**
     * Returns a time object that represents the time of this object after shifting it from GMT time zone.
     * <p>
     * This method assumes that the time represented by this object is in GMT time zone. It allocates a new time object and sets
     * it to the literal time of this object shifted to the local time zone. For instance if this object represents the time
     * 2pm GMT and the local time zone is PST, the returned object represents the time 2pm PST.
     * @return a time object that represents the time of this object after shifting it from GMT time zone.
     */
    public ZDate fromGMT() {
        try {
            return parseGMTtime(formatFullTimestamp(), FULL_TIMESTAMP);
        } catch (ParseException ex) {
            return null;
        }
    }
    
    /**
     * Returns a time object equal to the time represented by this object rounded to the given amount of milliseconds.
     * <p>
     * For instance, if {@code roundBy} is 1000, the returned time is the time of this object rounded to the nearest second.
     * @param roundBy   unit of round in milliseconds
     * @return a time object equal to the time represented by this object rounded to the given amount of milliseconds.
     */
    public ZDate round (long roundBy) {
        return new ZDate (Math.round(this.getTime() / (double) roundBy) * roundBy);
    }
    
    /**
     * Returns a time object equal to the time represented by this object truncated to a floor value that is a whole
     * multiply of the given amount of milliseconds.
     * <p>
     * For instance, if {@code truncateBy} is 1000, the returned time is the latest time in whole seconds that is 
     * not after this object.
     * @param truncateBy   unit of round in milliseconds
     * @return a time object equal to the time represented by this object rounded to the given amount of milliseconds.
     */
    public ZDate truncate (long truncateBy) {
        return new ZDate ((this.getTime() / truncateBy) * truncateBy);
    }
    
    /**
     * Returns true if since the time of the object until now a certain point of time was crossed.
     * <p>
     * The method accepts a parameter that is an amount of milliseconds and checks whether since the time of the object until now
     * the point of time equal to last midnight plus the given milliseconds was crossed, i.e. if that point of time is
     * between the time of the object (exclusive) and the current time (inclusive).
     * @param ms milliseconds since last midnight.
     * @return true if and only if the point of time equal to last midnight plus the given amount of milliseconds is between
     * the time of the object (exclusive) and the current time (inclusive).
     */
    public boolean hasCrossed(long ms) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        Date crossTime = new Date(date.getTimeInMillis() + ms);
        Date now = new Date();
        return this.before(crossTime) && !now.before(crossTime);
    }

    /**
     * Returns a time object representing the latest midnight that precedes or equals this object.
     * @return  a time object representing the latest midnight that precedes or equals this object.
     */
    public ZDate getMidnight() {
        return this.add(-this.getTimeSinceMidnight());
    }
    
    /**
     * Converts an {@link XMLGregorianCalendar} object to a {@code ZDate} object.
     * @param xDate     the given XMLGregorianCalendar object, or null, in which case the method returns null.
     * @return the date as a ZDate object or null if the given XMLGregorianCalendar object is null.
     */
    public static ZDate fromXMLGregorianCalendar(XMLGregorianCalendar xDate) {
        return xDate == null ? null : new ZDate(xDate.toGregorianCalendar().getTime());
    }

    /**
     * Returns the time of this object as an {@link XMLGregorianCalendar} object, using the given factory for conversion.
     * <p>
     * Converts the current object to an {@code XMLGregorianCalendar} object. Since the conversion requires the use of a
     * {@link DatatypeFactory} object whose instantiation consumes heavy resources, the method has a {@code factory} parameter that
     * lets you reusing the same object in multiple calls to this method. If you call the method with the value {@code null} in this
     * parameter, the method creates a new instance and uses it for the conversion.
     * @param factory   a factory object used for the conversion. If the value of this parameter is null the method instantiates
     *                  a new factory object.
     * @return the time of this object as an XMLGregorianCalendar object.
     * @throws Exception
     */
    public XMLGregorianCalendar toXMLGregorianCalendar(DatatypeFactory factory) throws Exception {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(this);
        if (factory == null)
            factory = DatatypeFactory.newInstance();
        return factory.newXMLGregorianCalendar(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            c.get(Calendar.SECOND), 
            DatatypeConstants.FIELD_UNDEFINED, 
            (c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)) / 60000 ); // get returns in ms and newXMLGregorianCalendar expects in minutes
    }

    /**
     * Converts an {@link java.sql.Date SQL Date} object to a {@code ZDate} object.
     * @param date the given SQL Date object, or null, in which case the method returns null.
     * @return the date as a ZDate object or null if the given SQL Date object is null.
     */
    public static ZDate fromSQLdate (java.sql.Date date){
        return date == null ? null : new ZDate(date.getTime());
    }
    
    /**
     * Returns the time of this object as an {@link java.sql.Date SQL Date} object.
     * @return the time of this object as an SQL Date object.
     */
    public java.sql.Date toSQLdate() {
        return new java.sql.Date(getTime());
    }
    
    /**
     * Converts an {@link java.sql.Timestamp SQL Timestamp} object to a {@code ZDate} object.
     * @param ts the given SQL Timestamp object, or null, in which case the method returns null.
     * @return the date as a ZDate object or null if the given SQL Timestamp object is null.
     */
    public static ZDate fromSQLtime (java.sql.Timestamp ts) {
        return ts == null ? null : new ZDate(ts.getTime());
    }
    
    /**
     * Returns the time of this object as an {@link java.sql.Timestamp SQL Timestamp} object.
     * @return the time of this object as an SQL Timestamp object.
     */
    public java.sql.Timestamp toSQLtime() {
        return new java.sql.Timestamp(getTime());
    }
    
    /**
     * Return the date of the current object with the time of the day as in the parameter
     * @param time a Date object from which time of day is taken
     * @return the result as a ZDate object
     */
    private ZDate setTimeOfDay (Date time) {
        Calendar hr = Calendar.getInstance();
        hr.setTime(time);
        Calendar me = Calendar.getInstance();
        me.setTime(this);
        hr.set(me.get(Calendar.YEAR), me.get(Calendar.MONTH), me.get(Calendar.DAY_OF_MONTH));
        return new ZDate (hr.getTime());
    }
    
    /**
     * Return number of milliseconds passed since midnight of the current object
     * @return
     */
    private long getTimeSinceMidnight () {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this);
        return cal.get(Calendar.HOUR_OF_DAY) * ZDate.HOUR + cal.get(Calendar.MINUTE) * ZDate.MINUTE +
                cal.get(Calendar.SECOND) * ZDate.SECOND + cal.get(Calendar.MILLISECOND);
    }
    
    private Calendar getCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }
}
