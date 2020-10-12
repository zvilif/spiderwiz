package org.spiderwiz.core;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.spiderwiz.annotation.WizField;
import org.spiderwiz.annotation.WizObject;
import org.spiderwiz.annotation.WizSerializable;
import org.spiderwiz.zutils.ZDate;
import org.spiderwiz.zutils.ZDictionary;

/**
 * Do data object serialization and deserialization
 * @author Zvi 
 */
class Serializer {
    private static Serializer myInstance = null;
    private DatatypeFactory factory = null;
    
    static synchronized Serializer getInstance() {
        if (myInstance == null)
            myInstance = new Serializer();
        return myInstance;
    }
    
    private static final char ESCAPER = '\\';
    static final char FIELD_SEPARATOR = ',';
    private static final char NULL_INDICATOR = '*';
    private static final char EMPTY_OBJECT = '^';
    static final char BAR_SEPARATOR = '|';
    private static final char LIST_SEPARATOR = ';';
    private static final char ASSIGMENT = '=';
    private static final char LIST_START = '[';
    private static final char LIST_END = ']';
    private static final char MAP_START = '<';
    private static final char MAP_END = '>';
    private static final char OBJECT_START = '{';
    private static final char OBJECT_END = '}';
    private static final char REMOVE_INDICATOR = '~';
    private static final char NUMBER_DIFFERENCE = '#';
    private static final char SUBSTRING_MATCH = ':';
    private static final String OPEN_AGGREGATORS = "[<{";
    private static final String CLOSE_AGGREGATORS = "]>}";
    private static final int MAX_STRING_COMPRESSION_ITERATIONS = 30000;
    
    private static final HashMap<Character, Character> escapeMap = new HashMap<Character, Character>() {
        {
            put(ESCAPER, '\\');
            put(FIELD_SEPARATOR, 'c');
            put(NULL_INDICATOR, 'n');
            put(EMPTY_OBJECT, 'e');
            put(BAR_SEPARATOR, 'b');
            put(LIST_SEPARATOR, 's');
            put(ASSIGMENT, 'a');
            put(LIST_START, 'l');
            put(LIST_END, 'r');
            put(MAP_START, 'm');
            put(MAP_END, 'p');
            put(OBJECT_START, 'o');
            put(OBJECT_END, 't');
            put(REMOVE_INDICATOR, 'd');
            put(NUMBER_DIFFERENCE, 'i');
            put(SUBSTRING_MATCH, 'u');
            // Add escapes for unprintable characters
            for (int i = 0; i <= 9; i++)
                put((char)i, (char)('0' + i));
            for (int i = 10; i <= 0x1f; i++)
                put((char)i, (char)('A' + i - 10));
        }
    };
    
    /**
     * Declare and assign the reverse map
     */
    private static final HashMap<Character, Character> unscapeMap = new HashMap<Character, Character>() {
        {
            escapeMap.entrySet().forEach((e) -> {
                put(e.getValue(), e.getKey());
            });
        }  
    };
    
    /**
     * Declare a base64 conversion array
     */
    private static final char base64Map[] = new char[64];
    
    /**
     * Declare and assign reverse base64 conversion map, after assigning the base64 conversion array
     */
    private static final HashMap<Character,Integer> reverseBase64Map = new HashMap<Character,Integer>() {
        {
            int i = 0;
            char c;
            for (c = 'A'; c <= 'Z'; c++)
                base64Map[i++] = c;
            for (c = 'a'; c <= 'z'; c++)
                base64Map[i++] = c;
            for (c = '0'; c <= '9'; c++)
                base64Map[i++] = c;
            base64Map[i++] = '+';
            base64Map[i] = '/';
            for (i = 0; i < 64; i++)
                put(base64Map[i], i);
        }
    };
    
    private static final double pow10[] = calcPow10();
    
    private static double[] calcPow10 () {
        double p[] = new double[20];
        int i = 0;
        for (double d = 1; d < 1e20; d *= 10)
            p[i++] = d;
        return p;
    }
    
    /**
     * Serialize a DataObject
     * @param obj the data object
     * @return a string that is comma-delimit concatenation of all object field that are annotated as "WizField"
     * @throws java.lang.Exception
     */
    public String serialize(DataObject obj) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        serializeClass(sb, obj, obj.getClass(), false);
        return sb.toString();
    }
    
    /**
     * Serialize an object by its class
     * @param sb
     * @param obj
     * @param cl
     * @param enclose   when true, object is enclosed by []
     * @return if the object is serializable return the class that was used for serialization, otherwise return null
     * @throws Exception 
     */
    private Class serializeClass(StringBuilder sb, Object obj, Class cl, boolean enclose) throws Exception
    {
        cl = getSerializableClass(cl);
        if (cl == null)
            return null;
        boolean xml = isXmlType(cl);
        
        if (enclose)
            sb.append(OBJECT_START);
        int startLength = sb.length();
        // First serialize fields up the class hierarchy
        Class superClass = cl.getSuperclass();
        if (superClass != null && !superClass.equals(DataObject.class))
            serializeClass(sb, obj, superClass, false);
        
        // Go through all class fields annotated with org.spiderwiz.annotation.Field and chain their string value in a
        // comma separated string
        Field[] fields = cl.getDeclaredFields();
        for (Field f : fields) {
            String format = "";
            int precision = -1;
            boolean ignoreZone = false;
            WizField an = f.getAnnotation(WizField.class);
            if (an != null) {
                format = an.format();
                precision = an.precision();
                ignoreZone = an.ignoreZoneDifference();
            }
            if (xml || an != null){      // if the object is annotated with XmlType we serialize all fields
                if (sb.length() != startLength)
                    sb.append(FIELD_SEPARATOR);
                serializeField(sb, obj, f, format, precision, ignoreZone);
            }
        }
        if (enclose)
            sb.append(OBJECT_END);
        return cl;
    }
    
    private Class getSerializableClass(Class cl) {
        // We can serialize objects annotated with WizObject, WizSerializable or XmlType (which, unlike the others, is not inherited).
        while (
            cl.isAnonymousClass() ||
            !cl.isAnnotationPresent(WizObject.class) && !cl.isAnnotationPresent(WizSerializable.class) &&
            !isXmlType(cl)
        ) {
            cl = cl.getSuperclass();
            if (cl == null)
                return null;
        }
        return cl;
    }
    
    private void serializeField(StringBuilder sb, Object obj, Field f, String format, int precision, boolean ignoreZone)
        throws Exception
    {
        f.setAccessible(true);
        Object val = f.get(obj);
        Class cl = f.getType();
        Type type = f.getGenericType();
        serializeFieldObject(sb, val, cl, type, format, precision, ignoreZone);
    }
    
    /**
     * serialize a field object
     * @param sb
     * @param val
     * @param cl
     * @param type
     * @param format
     * @param precision
     * @param ignoreZone
     * @return the class that was used for serialization. It can be a super class of 'cl' in some cases
     * @throws Exception 
     */
    private Class serializeFieldObject(StringBuilder sb, Object val, Class cl, Type type, String format, int precision,
        boolean ignoreZone) throws Exception
    {
        if (val == null) {
            sb.append(String.valueOf(NULL_INDICATOR));
            return cl;
        }

        if (cl.equals(Object.class))
            serializeGenericObject(sb, val, type, format, precision, ignoreZone);
        else if (val instanceof String)
           serializeString(sb, val);
        else if (cl.isEnum())
            sb.append(String.valueOf(((Enum)val).ordinal()));
        else if (val instanceof Boolean)
            sb.append(serializeBoolean(val));
        else if (val instanceof Date)
            sb.append(serializeDate(new ZDate((Date)val), format, ignoreZone));
        else if (val instanceof XMLGregorianCalendar)
            sb.append(serializeDate(ZDate.fromXMLGregorianCalendar((XMLGregorianCalendar)val), format, ignoreZone));
        else if (val instanceof Number)
            sb.append(serializeNumber(val, precision));
        else if (cl.isArray())
            serializeArray(sb, val, cl.getComponentType(), format, precision, ignoreZone);
        else if ((cl.getModifiers() & (Modifier.ABSTRACT | Modifier.INTERFACE)) != 0)
            serializeGenericObject(sb, val, type, format, precision, ignoreZone);
        else if (val instanceof List)
            serializeList(sb, (List)val, cl, type, format, precision, ignoreZone);
        else if (val instanceof Set)
            serializeSet(sb, (Set)val, cl, type, format, precision, ignoreZone);
        else if (val instanceof Map)
            serializeMap(sb, (Map)val, cl, type, format, precision, ignoreZone);
        else {
            Class actualClass = serializeClassField(sb, val);
            if (actualClass != null)
                return actualClass;
            if (!serializeUsingFormatMethod(sb, val, format))
                serializeUsingToStringMethod(sb, val);
        }
        return cl;
    }
    
    /**
     * If the field is declared as generic object (Object), we need to code the actual class of the object
     * @param sb
     * @param val
     * @param format
     * @param precision
     * @param ignoreZone 
     */
    private void serializeGenericObject(StringBuilder sb, Object val, Type type, String format, int precision,
        boolean ignoreZone
    )
        throws Exception
    {
        Class cl = val.getClass();
        while (cl.isAnonymousClass()) {
            cl = cl.getSuperclass();
            if (cl == null)
                return;
        }
        
        sb.append(OBJECT_START);
        // Serialize the class name. The name might change later if the class used for serialization is a superclass fo the given class
        int sbl = sb.length();
        sb.append(cl.getName()).append(ASSIGMENT);
        Class actualClass = serializeFieldObject(sb, val, cl, type, format, precision, ignoreZone);
        if (!actualClass.equals(cl))
            sb.replace(sbl, sbl + cl.getName().length(), actualClass.getName());
        sb.append(OBJECT_END);
    }
    
    private void serializeString(StringBuilder sb, Object val) {
        String s = (String)val;
        if (s.isEmpty())
            sb.append(EMPTY_OBJECT);
        else
            escapeDelimiters(sb, s);
    }
    
    private String serializeBoolean(Object val) {
        Boolean b = (Boolean)val;
        return b ? "1" : "0";
    }
    
    private String serializeDate(ZDate zd, String format, boolean ignoreZone) {
        String f = format == null || format.isEmpty() ? ZDate.TIMESTAMP : format;
        return escapeDelimiters(ignoreZone ? zd.format(f) : zd.formatGMT(f));
    }
    
    private String serializeNumber(Object val, int precision) throws Exception {
        val.getClass().getConstructor(String.class);  // to make sure we can deserialize it with a String constructor
        Number n = (Number)val;
        if (precision >= 0) {
            double pow = pow10[precision];
            n = (double)Math.round(n.doubleValue() * pow) / pow;
        }
        return n.toString();
    }
    
    private void serializeArray(StringBuilder sb, Object vals, Class cl, String format, int precision, boolean ignoreZone)
        throws Exception
    {
        for (int i = 0; i < Array.getLength(vals); i++) {
            sb.append(i == 0 ? LIST_START : LIST_SEPARATOR);
            serializeFieldObject(sb, Array.get(vals, i), cl, cl, format, precision, ignoreZone);
        }
        sb.append(LIST_END);
    }
    
    private void serializeList(StringBuilder sb, List vals, Class cl, Type type, String format, int precision,
        boolean ignoreZone) throws Exception
    {
        Type[] args = getTypeArguments(type, cl);
        Class parameterClass = (Class)args[0];
        sb.append(LIST_START);
        int startLength = sb.length();
        for (Object val : vals) {
            if (sb.length() > startLength)
                sb.append(LIST_SEPARATOR);
            serializeFieldObject(sb, val, parameterClass, parameterClass, format, precision, ignoreZone);
        }
        sb.append(LIST_END);
    }
    
    private void serializeSet(StringBuilder sb, Set vals, Class cl, Type type, String format, int precision, boolean ignoreZone)
        throws Exception
    {
        Type[] args = getTypeArguments(type, cl);
        Class parameterClass = (Class)args[0];
        sb.append(MAP_START);
        int startLength = sb.length();
        for (Object val : vals) {
            if (sb.length() > startLength)
                sb.append(LIST_SEPARATOR);
            serializeFieldObject(sb, val, parameterClass, parameterClass, format, precision, ignoreZone);
        }
        sb.append(MAP_END);
    }
    
    private void serializeMap(StringBuilder sb, Map<Object, Object> vals, Class cl, Type type, String format, int precision,
        boolean ignoreZone) throws Exception
    {
        Type[] args = getTypeArguments(type, cl);
        Class keyClass = (Class)args[0];
        Class valClass = (Class)args[1];
        sb.append(MAP_START);
        int startLength = sb.length();
        for (Entry entry : vals.entrySet()) {
            if (sb.length() > startLength)
                sb.append(LIST_SEPARATOR);
            serializeFieldObject(sb, entry.getKey(), keyClass, keyClass, format, precision, ignoreZone);
            sb.append(ASSIGMENT);
            serializeFieldObject(sb, entry.getValue(), valClass, valClass, format, precision, ignoreZone);
        }
        sb.append(MAP_END);
    }
    
    private Class serializeClassField(StringBuilder sb, Object obj) throws Exception {
         return serializeClass(sb, obj, obj.getClass(), true);
    }
    
    private boolean serializeUsingFormatMethod(StringBuilder sb, Object val, String format)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Method method;
        try {
          method = val.getClass().getMethod("format", String.class);
        } catch (NoSuchMethodException | SecurityException e) {
          return false;
        }
        if (method == null)
            return false;
        serializeString(sb, (String)method.invoke(val, format));
        return true;
    }

    private boolean serializeUsingToStringMethod(StringBuilder sb, Object val) throws Exception
    {
        Method method;
        try {
          method = val.getClass().getMethod("toString");
        } catch (NoSuchMethodException | SecurityException e) {
          return false;
        }
        if (method == null)
            return false;
        serializeString(sb, (String)method.invoke(val));
        return true;
    }

    /**
     * Deserialize the values of a DataObject fields that are annotated as "WizField"
     * @param obj       the data object
     * @param fields            the object field values as one string separated by commas
     * @param offset    the offset in the array that desrialization shall start
     * @return          If object has been modified by the command return the object itself, otherwise return null.
     * @throws java.lang.Exception
     */
    public DataObject deserialize(DataObject obj, String fields) throws Exception
    {
        String[] vals = splitFields(fields, FIELD_SEPARATOR);
        int length = deserializeClass(obj, obj.getClass(), vals, 0);
        return length == 0 ? null : obj;
    }
    
    private boolean isXmlType(Class cl)  {
        try {
            return cl.isAnnotationPresent(XmlType.class);
        } catch (Exception ex) {
            return false;
        }
    }
    
    private int deserializeClass(Object obj, Class objClass, String[] vals, int offset) throws Exception
    {
        if (objClass == null)
            return offset;
        boolean xml = isXmlType(objClass);
        if (!xml && !objClass.isAnnotationPresent(WizObject.class) && !objClass.isAnnotationPresent(WizSerializable.class))
            return offset;
        // First deserialize fields up the class hierarchy
        offset = deserializeClass(obj, objClass.getSuperclass(), vals, offset);
        
        // Go through all class fields annotated with org.spiderwiz.annotation.Field and deserialize them
        Field[] fields = objClass.getDeclaredFields();
        for (Field f : fields) {
            if (offset >= vals.length)
                return offset;
            String format = "";
            boolean ignoreZone = false;
            WizField an = f.getAnnotation(WizField.class);
            if (an != null) {
                format = an.format();
                ignoreZone = an.ignoreZoneDifference();
            }
            if (xml || an != null)
                deserializeField(obj, vals[offset++], f, format, ignoreZone);
        }
        return offset;
    }
    
    private void deserializeField(Object obj, String val, Field f, String format, boolean ignoreZone) throws Exception
    {
        f.setAccessible(true);
        Class cl = f.getType();
        Type type = f.getGenericType();
        if (cl.isPrimitive()) {
            deserializePrimitive(val, obj, f);
            return;
        }
        f.set(obj, cl.cast(deserializeFieldObject(val, cl, type, obj, format, ignoreZone)));
    }
    
    private Object deserializeFieldObject(String val, Class cl, Type type, Object outer, String format, boolean ignoreZone)
        throws Exception
    {
        Object result;
        if (isNullIndicator(val))
            return null;
        if ((result = deserializeClassField(val, cl, type, outer, format, ignoreZone)) != null)
            return result;
        if (String.class.isAssignableFrom(cl))
            return deserializeString(val);
        if (cl.isEnum())
            return deserializeEnum(val, cl);
        if (Boolean.class.isAssignableFrom(cl))
            return deserializeBoolean(val);
        if (Date.class.isAssignableFrom(cl))
            return deserializeDate(val, format, ignoreZone);
        if (XMLGregorianCalendar.class.isAssignableFrom(cl))
            return deserializeXmlDate(val, format, ignoreZone);
        if (Number.class.isAssignableFrom(cl))
            return deserializeNumber(val, cl);
        if (cl.isArray())
            return deserializeArray (val, cl.getComponentType(), outer, format, ignoreZone);
        if (List.class.isAssignableFrom(cl))
            return deserializeList(val, cl, type, outer, format, ignoreZone);
        if (Set.class.isAssignableFrom(cl))
            return deserializeSet(val, cl, type, outer, format, ignoreZone);
        if (Map.class.isAssignableFrom(cl))
            return deserializeMap(val, cl, type, outer, format, ignoreZone);
        if ((result = deserializeUsingConstructor(val, cl)) == null &&
            (result = deserializeUsingParseWithFormatMethod(val, format, cl)) == null
        )
            return deserializeUsingFromStringMethod(val, cl);
        return result;
    }
    
    /**
     * Check if 'val' is encoded as a generic object. If it is, deserialize the object, otherwise return null.
     * @param val
     * @param format
     * @param ignoreZone
     * @return the deserialized object
     */
    private Object deserializeGenericObject(String val, Type type, Object outer, String format, boolean ignoreZone) throws Exception {
        String[] parts = splitFields(val, ASSIGMENT);
        if (parts.length != 2)
            return null;
        Class cl = Class.forName(parts[0]);
        return deserializeFieldObject(parts[1], cl, type, outer, format, ignoreZone);
    }
    
    private void deserializePrimitive(String val, Object obj, Field f) throws Exception {
        Class cl = f.getType();
        if (cl.equals(int.class))
            f.set(obj, Integer.parseInt(val));
        else if (cl.equals(byte.class))
            f.set(obj, Byte.parseByte(val));
        else if (cl.equals(boolean.class))
            f.set(obj, deserializeBoolean(val));
        else if (cl.equals(double.class))
            f.set (obj, Double.parseDouble(val));
        else if (cl.equals(float.class))
            f.set(obj, Float.parseFloat(val));
        else if (cl.equals(long.class))
            f.set(obj, Long.parseLong(val));
        else if (cl.equals(short.class))
            f.set(obj, Short.parseShort(val));
    }
    
    private String deserializeString(String val) {
        return String.valueOf(EMPTY_OBJECT).equals(val) ? "" : unescapeDelimiters(val);
    }
    
    static Enum deserializeEnum(String val, Class type) {
        return (Enum)type.getEnumConstants()[Integer.parseInt(val)];
    }
    
    private Boolean deserializeBoolean(String val) {
        return !val.equals("0");
    }
    
    private Date deserializeDate(String val, String format, boolean ignoreZone) throws ParseException {
        return (Date)deserializeZDate(val, format, ignoreZone);
    }
    
    private synchronized XMLGregorianCalendar deserializeXmlDate(String val, String format, boolean ignoreZone) throws Exception {
        if (factory == null)
            factory = DatatypeFactory.newInstance();
        return deserializeZDate(val, format, ignoreZone).toXMLGregorianCalendar(factory);
    }

    private ZDate deserializeZDate(String val, String format, boolean ignoreZone) throws ParseException {
        String f = format.isEmpty() ? ZDate.TIMESTAMP : format;
        val = unescapeDelimiters(val);
        return ignoreZone ? ZDate.parseTime(val, format, null) : ZDate.parseGMTtime(val, f);
    }
    
    private Number deserializeNumber(String val, Class cl) throws Exception
    {
        Constructor<?> ctor;
        try {
            ctor = cl.getConstructor(String.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
        if (ctor == null)
            return null;
        ctor.setAccessible(true);
        return (Number)ctor.newInstance(val);
    }
    
    private Object deserializeArray(String val, Class cl, Object outer, String format, boolean ignoreZone)
        throws Exception
    {
        if (String.valueOf(EMPTY_OBJECT).equals(val))
            val = "";
        else if (!val.isEmpty()) {
            int end = val.length() - 1;
            if (val.charAt(0) != LIST_START || val.charAt(end) != LIST_END)
                throw new ParseException(String.format(CoreConsts.Serializer.LIST_PARSE_ERROR, val), 0);
            val = val.substring(1, end);
        }
        String[] elements = splitFields(val, LIST_SEPARATOR);
        int size = elements.length;
        Object result = Array.newInstance(cl, size);
        for (int i = 0; i < size; i++) {
            String element = elements[i];
            if (!cl.isPrimitive())
                Array.set(result, i, deserializeFieldObject(element, cl, cl, outer, format, ignoreZone));
            else if (cl.equals(int.class))
                Array.set(result, i, Integer.parseInt(element));
            else if (cl.equals(byte.class))
                Array.set(result, i, Byte.parseByte(element));
            else if (cl.equals(double.class))
                Array.set (result, i, Double.parseDouble(element));
            else if (cl.equals(float.class))
                Array.set(result, i, Float.parseFloat(element));
            else if (cl.equals(long.class))
                Array.set(result, i, Long.parseLong(element));
            else if (cl.equals(short.class))
                Array.set(result, i, Short.parseShort(element));
        }
        return result;
    }
    
    private List deserializeList(String val, Class cl, Type type, Object outer, String format, boolean ignoreZone)
        throws Exception
    {
        if (String.valueOf(EMPTY_OBJECT).equals(val))
            val = "";
        else if (!val.isEmpty()) {
            int end = val.length() - 1;
            if (val.charAt(0) != LIST_START || val.charAt(end) != LIST_END)
                throw new ParseException(String.format(CoreConsts.Serializer.LIST_PARSE_ERROR, val), 0);
            val = val.substring(1, end);
    }
        return (List)deserializeCollection(val, cl, type, outer, format, ignoreZone);
    }
    
    private Set deserializeSet(String val, Class cl, Type type, Object outer, String format, boolean ignoreZone)
        throws Exception
    {
        if (String.valueOf(EMPTY_OBJECT).equals(val))
            val = "";
        else if (!val.isEmpty()) {
            int end = val.length() - 1;
            if (val.charAt(0) != MAP_START || val.charAt(end) != MAP_END)
                throw new ParseException(String.format(CoreConsts.Serializer.SET_PARSE_ERROR, val), 0);
            val = val.substring(1, end);
    }
        return (Set)deserializeCollection(val, cl, type, outer, format, ignoreZone);
    }

    private Collection deserializeCollection(String val, Class cl, Type type, Object outer, String format,
        boolean ignoreZone
    )
        throws Exception
    {
        Type[] args = getTypeArguments(type, cl);
        Class parameterClass = (Class)args[0];
        Collection result = (Collection)instantiateClass(cl, outer);
        String[] elements = splitFields(val, LIST_SEPARATOR);
        for (String element : elements)
            result.add(deserializeFieldObject(element, parameterClass, parameterClass, outer, format, ignoreZone));
        return result;
    }
    
    private Type[] getTypeArguments(Type type, Class cl) throws ParseException {
        Type t = type;
        Class c = cl;
        while (!(t instanceof ParameterizedType)) {
            t = c.getGenericSuperclass();
            c = c.getSuperclass();
        }
        Type[] result = ((ParameterizedType)t).getActualTypeArguments();
        // Check if type parameters are valid classes
        for (int i = 0; i < result.length; i++)
            if (!(result[i] instanceof Class))
                result[i] = Object.class;
        return result;
    }

    private Map deserializeMap(String val, Class cl, Type type, Object outer, String format, boolean ignoreZone)
        throws Exception
    {
        if (String.valueOf(EMPTY_OBJECT).equals(val))
            val = "";
        else if (!val.isEmpty()) {
            int end = val.length() - 1;
            if (val.charAt(0) != MAP_START || val.charAt(end) != MAP_END)
                throw new ParseException(String.format(CoreConsts.Serializer.MAP_PARSE_ERROR, val), 0);
            val = val.substring(1, end);
        }
        Type[] args = getTypeArguments(type, cl);
        Class keyClass = (Class)args[0];
        Class valClass = (Class)args[1];
        String[] elements = splitFields(val, LIST_SEPARATOR);
        Map result = (Map)instantiateClass(cl, outer);
        for (String element : elements) {
            String entry[] = splitFields(element, ASSIGMENT);
            Object key = deserializeFieldObject(entry[0], keyClass, keyClass, outer, format, ignoreZone);
            Object value = entry.length < 2 ? null :
                deserializeFieldObject(entry[1], valClass, valClass, outer, format, ignoreZone);
            result.put(key, value);
        }
        return result;
    }
    
    private Object deserializeClassField(String val, Class cl, Type type, Object outer, String format, boolean ignoreZone)
        throws Exception
    {
        int end = val.length() - 1;
        if (val.charAt(0) != OBJECT_START || val.charAt(end) != OBJECT_END)
            return null;
        val = val.substring(1, end);
        
        // try first if this is serialized as a generic object
        Object obj = deserializeGenericObject(val, type, outer, format, ignoreZone);
        if (obj == null) {
            cl = getSerializableClass(cl);
            if (cl == null)
                return null;
            obj = instantiateClass(cl, outer);
            deserializeClass(obj, cl, splitFields(val, FIELD_SEPARATOR), 0);
        }
        return obj;
    }
    
    private Object instantiateClass(Class cl, Object outer) throws NoSuchMethodException, InstantiationException,
        IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        Constructor<?>ctor;
        if (outer != null && cl.getEnclosingClass() != null && !Modifier.isStatic(cl.getModifiers())) {
            ctor = cl.getDeclaredConstructor(cl.getEnclosingClass());
            ctor.setAccessible(true);
            return ctor.newInstance(outer);
        }
        ctor = cl.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private Object deserializeUsingConstructor(String val, Class cl) throws Exception {
        Constructor<?> ctor;
        try {
            ctor = cl.getConstructor(String.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
        if (ctor == null)
            return null;
        ctor.setAccessible(true);
        return ctor.newInstance(deserializeString(val));
    }
    
    private Object deserializeUsingParseWithFormatMethod(String val, String format, Class cl) throws Exception
    {
        Method method;
        try {
          method = cl.getMethod("parse", String.class, String.class);
        } catch (NoSuchMethodException | SecurityException e) {
          return null;
        }
        if (method == null)
            return null;
        return method.invoke(null, deserializeString(val), format);
    }

    private Object deserializeUsingFromStringMethod(String val, Class cl) throws Exception
    {
        Method method;
        try {
          method = cl.getMethod("fromString", String.class);
        } catch (NoSuchMethodException | SecurityException e) {
          return null;
        }
        if (method == null)
            return null;
        return method.invoke(null, deserializeString(val));
    }

    /**
     * Return a string that is a concatenation of the escaped version of all the parameters with 'delimit' as a
     * delimiter. Non-trailing nulls are encoded as empty strings. Trailing nulls are omitted all together.
     * @param delimiter   the character that delimits the concatenated values
     * @param args
     * @return
     */
    static String escapeAndConcatenate(String delimiter, String ... args) throws Exception {
        StringBuilder result = new StringBuilder();
        int delimiters = 0;
        for (String arg : args) {
            if (arg != null) {
                while (delimiters > 0) {
                    result.append(delimiter);
                    --delimiters;
                }
                result.append(escapeDelimiters(arg));
            }
            ++delimiters;
        }
        return result.toString();
    }
    
    /**
     * Split a string value to an array of strings, using the specified delimiter, and then delimiter-unescape every
     * element in the list.
     * @param val       source string
     * @param delimiter the delimiter (as is, without the regex "\" prefix)
     * @param limit     the split result threshold
     * @return the split array
     */
    static String[] splitAndUnescape(String val, String delimiter, int limit) {
        if (val == null || val.isEmpty())
            return new String[0];
        String fields[] = val.split("\\" + delimiter, limit);
        for (int i = 0; i < fields.length; i++)
            fields[i] = unescapeDelimiters(fields[i]);
        return fields;
    }
    
    /**
     * Split a string which is a delimiter-concatenation of data object fields. Take into consideration that fields may
     * contain aggregates, in which case the delimiter should not affect the split.
     * @param val
     * @return 
     */
    static String[] splitFields(String val, char delimiter) {
        ArrayList<String> flds = new ArrayList<>();
        int offset;
        char c;
        for (offset = 0; offset < val.length();) {
            int level = 0, i;
            for (i = offset; i < val.length(); i++) {
                 c = val.charAt(i);
                 if (c == delimiter && level == 0)
                     break;
                if (OPEN_AGGREGATORS.indexOf(c) >= 0)
                    ++level;
                else if (CLOSE_AGGREGATORS.indexOf(c) >= 0)
                    --level;
            }
            flds.add(val.substring(offset, i++));
            // if the delimiter is the last character of the string then there is one more empty field
            if (i == val.length())
                flds.add("");
            offset = i;
        }
        return flds.toArray(new String[flds.size()]);
    }

    /**
     * Escape a String containing delimiters so it can be serialized in delimiter-dependent context
     * @param val   the original string
     * @return      the escaped string after replacing all known delimiters by escape sequences.
     */
    static String escapeDelimiters(String val) {
        if (val == null)
            return null;
        StringBuilder sb = new StringBuilder();
        getInstance().escapeDelimiters(sb, val);
        return sb.toString();
    }
    
    /**
     * Escape delimiters into a given StringBuilder
     * @param sb
     * @param val 
     */
    private void escapeDelimiters(StringBuilder sb, String val) {
        for (int i = 0; i < val.length();) {
            char c = val.charAt(i++);
            Character esc = escapeMap.get(c);
            if (esc == null)
                sb.append(c);
            else
                sb.append(ESCAPER).append(esc);
        }
    }

    /**
     * Unescape an delimiter-dependent string back to its original value
     * @param val   the escaped field
     * @return      the original string after restoring all escaped delimiters to their original values.
     */
    static String unescapeDelimiters(String val) {
        if (val == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < val.length();) {
            char c = val.charAt(i++);
            sb.append((char)(c == ESCAPER ? unscapeMap.get(val.charAt(i++)) : c));
        }
        String result = sb.toString();
        return result.isEmpty() ? null : result;
    }

    /**
     * Get a parameter list encoded string and return a parameter map
     * @param parList   parameter list in the format "name1=value1;name2=value2;..."
     * @return a Map that maps every parameter name to its value. Keys in the map are converted to lowercase.
     */
    static ZDictionary parseParameterList(String parList) {
        if (parList == null || parList.isEmpty())
            return null;
        ZDictionary parMap = new ZDictionary();
        String pars[] = parList.split(String.valueOf(LIST_SEPARATOR));
        for (String par : pars) {
            String pair[] = par.split(String.valueOf(ASSIGMENT));
            String key = unescapeDelimiters(pair[0]);
            if (key != null && !key.isEmpty()) {
                String val = pair.length < 2 ? null : unescapeDelimiters(pair[1]);
                parMap.put(key.toLowerCase(), val);
            }
        }
        return parMap;
    }
    
    /**
     * Treat the object as a parameter map and return all parameters encoded into a single string in the format
     * "name1=value1;name2=value2;...".
     * @return the encoded parameters as a String
     */
    static String encodeParameterList(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        map.entrySet().forEach((entry) -> {
            if (sb.length() > 0)
                sb.append(";");
            String name = entry.getKey();
            String val = entry.getValue();
            sb.append(escapeDelimiters(name));
            if (val != null) {
                sb.append("=").append(escapeDelimiters(val));
            }
        });
        return sb.toString();
    }
    
    /**
     * Check if this is a null indicator
     * @param value
     * @return 
     */
    static boolean isNullIndicator(String value) {
        return value.length() == 1 && value.charAt(0) == NULL_INDICATOR;
    }
    
    /**
     * Receive two strings that are the serialized values of previous and current Data Object and return a string array that
     * is the delta difference of the second from the first
     * @param previous
     * @param current
     * @return delta string
     */
    String compress(String previous, String current) {
        StringBuilder sb = new StringBuilder();
        compressValues(sb, previous, current, FIELD_SEPARATOR);
        return sb.toString();
    }
    
    /**
     * Exposed version of compressValues
     */
    String compressValues(String previous, String current, char delimiter) {
        StringBuilder sb = new StringBuilder();
        compressValues(sb, previous, current, delimiter);
        return sb.toString();
    }
    
    /**
     * Receive two strings that are the concatenated serialization of previous and current values of the fields of the same
     * class object and return a string array that is the concatenated delta differences of the second from the first
     * @param previous
     * @param current
     * @param delimiter   the character that delimits the concatenated values
     * @return delta string
     */
    private void compressValues(StringBuilder sb, String previous, String current, char delimiter) {
        String pre[] = splitFields(previous, delimiter);
        String cur[] = splitFields(current, delimiter);
        int length = Math.max(pre.length, cur.length);
        int sbl = sb.length();
        for (int i = 0; i < length; i++) {
            if (i > 0)
                sb.append(delimiter);
            if (i >= cur.length)
                sb.append(REMOVE_INDICATOR);
            else if (i >= pre.length)
                sb.append(cur[i]);
            else
                compressField(sb, pre[i], cur[i]);
        }
        removeTrailingDelimiters(sb, sbl, delimiter);
    }
    
    /**
     * Remove trailing delimiters from the given StringBuilder
     * @param sb            the StringBuilder
     * @param indexFrom     index to start search from in sb
     * @param delimiter     the delimiter
     */
    static void removeTrailingDelimiters(StringBuilder sb, int indexFrom, char delimiter) {
        int i;
        for (i = sb.length() - 1; i >= indexFrom; i--) {
            if (sb.charAt(i) != delimiter)
                break;
        }
        sb.delete(i + 1, sb.length());
    }
    
    /**
     * Receive two strings that are the serialization of previous and current values of the same field and return a string
     * that is the delta difference of the second from the first.
     * @param sb        compress into this StringBuilder
     * @param previous
     * @param current
     * @return true if anything was added to sb
     */
    private boolean compressField(StringBuilder sb, String previous, String current) {
        int sbl = sb.length();
        if (previous == null || previous.isEmpty())
            sb.append(current);
        else if (current.isEmpty())
            sb.append(EMPTY_OBJECT);
        else if (!current.equals(previous)) {
            switch (current.charAt(0)) {
            case LIST_START:
                compressList(sb, previous, current, LIST_START, LIST_SEPARATOR, LIST_END);
                break;
            case MAP_START:
                compressMapField(sb, previous, current);
                break;
            case OBJECT_START:
                compressList(sb, previous, current, OBJECT_START, FIELD_SEPARATOR, OBJECT_END);
                break;
            default:
                compressString(sb, previous, current);
            }
        }
        return sb.length() > sbl;
    }
    
    /**
     * Compare a previous and current version of a string and try to compress the current one by calculating a delta value
     * @param previous
     * @param current
     * @return 
     */
    private void compressString(StringBuilder sb, String previous, String current) {
        String delta;
        if (current.length() < 5)   // 5 is the minimum that can be compressed
            sb.append(current);
        else if ((delta = compressNumber(previous, current)) != null)
            sb.append(delta);
        else {
            String p = unescapeDelimiters(previous);
            String c = unescapeDelimiters(current);
            compressSubstring(sb, p, 0, p.length(), c, 0, c.length());
        }
    }
    
    /**
     * Compress a general object, consisted of the assignment class-name=object-values
     * @param currentParts  the current value, split by the assignment character
     * @param previous      the previous value, split
     * @return 
     */
    private void compressGeneralObject(StringBuilder sb, String[] previousParts, String[] currentParts) {
        compressValues(sb, previousParts[0], currentParts[0], '.');
        sb.append(ASSIGMENT);
        // Don't compress object if its not of the same class as the previous
        if (previousParts[0].equals(currentParts[0]))
            compressField(sb, previousParts[1], currentParts[1]);
        else
            sb.append(currentParts[1]);
    }
    
    /**
     * Compress a substring by matching it with a previous substring and compress out the matches
     * @param sb        StringBuilder to build the result into
     * @param p         previous string
     * @param pFrom     start offset in previous string
     * @param pTo       end offset in previous string
     * @param c         current string
     * @param cFrom     start offset in current string
     * @param cTo       end offset in current string
     * @return the number of characters at the end of the previous substring that have no match in the current substring
     */
    private int compressSubstring(StringBuilder sb, String p, int pFrom, int pTo, String c, int cFrom, int cTo) {
        // Find the best match between the two substrings
        int maxLen = 0;
        int pMatchStart = 0, cMatchStart = 0;

        // If compression requires more than the limit number of iterations, split the strings and compress the two parts separately.
        int pActualTo;
        int cActualTo;
        int d1 = pTo - pFrom;
        int d2 = cTo - cFrom;
        if (d1 * d2 > MAX_STRING_COMPRESSION_ITERATIONS) {
            double f = Math.sqrt((double)MAX_STRING_COMPRESSION_ITERATIONS / (d1 * d2));
            d1 *= f;
            d2 *= f;
            pActualTo = pFrom + d1;
            cActualTo = cFrom + d2;
        } else {
            pActualTo = pTo;
            cActualTo = cTo;
        }
        int[][] seqCounter = new int[2][d2];

        // Loop through the previous substring then through the current substring. Stop either loop when there is no
        // chance to yield a better match.
        for (int i = pFrom; i < pActualTo; i++) {
            for (int j = cFrom; j < cActualTo; j++) {
                int b = i & 1;              // We can use binary table with modulu index
                int matchLen = 0;
                if (p.charAt(i) == c.charAt(j)) {
                    matchLen = j == cFrom ? 1 : seqCounter[1 - b][j - cFrom - 1] + 1;
                    // Check if current iteration produced a better match
                    if (matchLen > maxLen) {
                        maxLen = matchLen;
                        pMatchStart = i - matchLen + 1;
                        cMatchStart = j - matchLen + 1;
                    }
                }
                seqCounter[b][j - cFrom] = matchLen;
            }
        }

        // If the best match is shorter than 5 characters, encode and append the entire c substring to sb and return the length of the
        // entire p substring and the result of the function.
        int skip;
        if (maxLen < 5) {
            sb.append(escapeDelimiters(c.substring(cFrom, cActualTo)));
            if (cActualTo >= c.length())
                return 0;
            maxLen = 0;
            skip = pActualTo - pFrom;
            if (cActualTo >= cTo && pActualTo >= pTo)
                return skip;
        } else {
            // We hae a match!
            // Compress the substring to the left of the match and store it in sb
            pActualTo = pMatchStart + maxLen;
            cActualTo = cMatchStart + maxLen;
            skip = compressSubstring(sb, p, pFrom, pMatchStart, c, cFrom, cMatchStart);
        }
        
        
        // Encode substring copy escape sequence, made of the character : followed by number of characters to skip and number of characters
        // to copy in base64 format. If skip or copy > 63 then repeat the escape sequence as many time as needed.
        if (skip + maxLen > 0) {
            sb.append(SUBSTRING_MATCH);
            while (skip > 63) {
                sb.append(base64Map[63]).append(base64Map[0]).append(SUBSTRING_MATCH);
                skip -= 63;
            }
            sb.append(base64Map[skip]);
            int n = maxLen;
            while (n > 63) {
                sb.append(base64Map[63]).append(SUBSTRING_MATCH).append(base64Map[0]);
                n -= 63;
            }
            sb.append(base64Map[n]);
        }
        
        // Compress the substring to the right of the match and store it in sb
        return compressSubstring(sb, p, pActualTo, pTo, c, cActualTo, cTo);
    }
    
    /**
     * Try to compress a value by assuming it's a number and calculate the difference from previous value
     * @param previous
     * @param current
     * @return the compressed (or not) value if it's a number, null if it isn't
     */
    private String compressNumber(String previous, String current) {
        final String LONG_REGEX = "-?[1-9]\\d*";
        final String DOUBLE_REGEX = "-?[1-9]\\d*(\\.\\d+)?(E-?\\d+)?";
        
        // Check if any type of number
        if (!current.matches(DOUBLE_REGEX) || !previous.matches(DOUBLE_REGEX))
            return null;

        // Try long
        if (current.matches(LONG_REGEX) && previous.matches(LONG_REGEX)) {
            try {
                long pl = Long.parseLong(previous);
                long cl = Long.parseLong(current);
                // Make sure compression/decompression makes the round trip and value was indeed compressed
                long dl = cl - pl;
                if (!String.valueOf(pl + dl).equals(current))
                    return null;
                String d = NUMBER_DIFFERENCE + String.valueOf(dl);
                return d.length() < current.length() ? d : current;
            } catch (NumberFormatException ex) {
            }
        }
        
        // Try to caculate difference of two doubles
        try {
            double pl = Double.parseDouble(previous);
            double cl = Double.parseDouble(current);
            double dl = cl - pl;
            // round dl to the precision of pl and cl
            int pp = previous.lastIndexOf('.');
            pp = pp < 0 ? 0 : previous.length() - pp - 1;
            int cp = current.lastIndexOf('.');
            cp = cp < 0 ? 0 : current.length() - cp - 1;
            double pow = pow10[Math.max(pp, cp)];
            dl = Math.round(dl * pow) / pow;
            // Make sure compression/decompression makes the round trip and value was indeed compressed
            if (!String.valueOf(Math.round((pl + dl) * pow) / pow).equals(current))
                return null;
            String d = NUMBER_DIFFERENCE + String.valueOf(dl);
            return d.length() < current.length() ? d : current;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    /**
     * Receive two strings that are the serialization of previous and current values of the same array or List and return a
     * string that is the delta difference of the second from the first
     * @param previous
     * @param current
     * @return delta string
     */
    private void compressList(StringBuilder sb, String previous, String current, char listStart, char listSeparator,
        char listEnd)
    {
        if (previous.charAt(0) != listStart)
            sb.append(current);
        else {
            previous = previous.substring(1, previous.length() - 1);
            current = current.substring(1, current.length() - 1);
            sb.append(listStart);
            int sbl = sb.length();
            
            // Check if this is general object assignment
            String[] currentParts;
            if (listStart == OBJECT_START && (currentParts = splitFields(current, ASSIGMENT)).length == 2) {
                String[] previousParts = splitFields(previous, ASSIGMENT);
                if (previousParts.length != 2)
                    sb.append(current);
                else
                    compressGeneralObject(sb, previousParts, currentParts);
            } else
                compressValues(sb, previous, current, listSeparator);
            // if values where compressed out entirely we can remove the list-start indicator
            if (sb.length() == sbl)
                sb.deleteCharAt(sbl - 1);
            else
                sb.append(listEnd);
        }
    }

    /**
     * Receive a string that that is the serialization of a map or set and return a map populated with the parsed string
     * @param vals   encoded map value
     * @return a ZDictionary object with the parsed content of 'vals'
     */
    private ZDictionary parseMap(String vals) {
        ZDictionary map = new ZDictionary();
        String[] es = splitFields(vals, LIST_SEPARATOR);
        for (String par : es) {
            String entry[] = splitFields(par, ASSIGMENT);
            String key = entry[0];
            String val = entry.length < 2 ? null : entry[1];
            map.put(key, val);
        }
        return map;
    }

    /**
     * Receive two strings that are the serialization of previous and current values of the map or set and return a string
     * that is the delta difference of the second from the first
     * @param previous
     * @param current
     * @return delta string
     */
    private void compressMapField(StringBuilder sb, String previous, String current) {
        if (previous.charAt(0) != MAP_START)
            sb.append(current);
        else {
            sb.append(MAP_START);
            int sbl = sb.length();
            compressMap(sb, previous.substring(1, previous.length() - 1), current.substring(1, current.length() - 1));
            // if values where compressed out entirely we can remove the list-start indicator
            if (sb.length() == sbl)
                sb.deleteCharAt(sbl - 1);
            else
                sb.append(MAP_END);
        }
    }
    
    /**
     * exposed version of compressMap
     */
    String compressMap(String previous, String current) {
        StringBuilder sb = new StringBuilder();
        compressMap(sb, previous, current);
        return sb.toString();
    }
    
    /**
     * Compare string representation of two maps or sets and return a string which is the difference between the two
     * @param previous
     * @param current
     * @return 
     */
    private void compressMap(StringBuilder sb, String previous, String current) {
        if (isNullIndicator(previous) || isNullIndicator(current)) {
            sb.append(current);
            return;
        }
        ZDictionary pm = parseMap(previous);
        ZDictionary cm = parseMap(current);
        
        // compare the maps and create delta string
        int sbl = sb.length();
        for (Entry<String,String> e : cm.entrySet()) {
            String key = e.getKey();
            String cv = e.getValue();
            String pv = pm.get(key);
            if (cv == null) {
                String ds;
                if (!pm.keySet().contains(key))
                   ds = key;
                else if (pv != null)
                    ds = key + ASSIGMENT + NULL_INDICATOR;
                else
                    continue;
                if (sb.length() > sbl)
                    sb.append(LIST_SEPARATOR);
                sb.append(ds);
            } else if (!cv.equals(pv)) {
                int sbLengthBeforeArg = sb.length();
                if (sb.length() > sbl)
                    sb.append(LIST_SEPARATOR);
                sb.append(key).append(ASSIGMENT);
                // Add the value. If it turns out to be empty, remove the entire assignment
                if (!compressField(sb, pv, cv))
                    sb.delete(sbLengthBeforeArg, sb.length());
            }
        }
        // add remove indicators for all keys that are in previous by not in current
        pm.keySet().removeAll(cm.keySet());
        pm.keySet().forEach((pk) -> {
            if (sb.length() > sbl)
                sb.append(LIST_SEPARATOR);
            sb.append(pk).append(ASSIGMENT).append(REMOVE_INDICATOR);
        });
    }

    /**
     * Receive two strings, the first which is a serialized Data Object, while the second is the delta values between this
     * and a subsequent values of the same object, and return a string that is the second after applying the delta on the
     * first.
     * @param previous
     * @param delta
     * @return 
     */
    String decompress(String previous, String delta) {
        StringBuilder sb = new StringBuilder();
        decompressValues(sb, previous, delta, FIELD_SEPARATOR);
        return sb.toString();
    }
    
    /**
     * Exposed version of decompressValues
     * @param previous
     * @param delta
     * @param delimiter
     * @return the decompressed values
     */
    String decompressValues(String previous, String delta, char delimiter) {
        StringBuilder sb = new StringBuilder();
        decompressValues(sb, previous, delta, delimiter);
        return sb.toString();
    }    
    
    /**
     * Receive two strings, the first which is a concatenated serialization of object fields, while the second is
     * the delta values between this and a subsequent field values of the same object, and return a string that is the
     * serialized values of the second after applying the delta on the first.
     * @param previous
     * @param current
     * @param delimiter   the character that delimits the concatenated values
     * @return delta string
     */
    private void decompressValues(StringBuilder sb, String previous, String delta, char delimiter) {
        if (delta == null || delta.isEmpty()) {
            sb.append(previous);
            return;
        }
        String ps[] = splitFields(previous, delimiter);
        String ds[] = splitFields(delta, delimiter);
        int length = Math.max(ps.length, ds.length);
        int sbl = sb.length();
        for (int i = 0; i < length; i++) {
            if (i > 0)
                sb.append(delimiter);
            if (i >= ps.length)
                sb.append(ds[i]);
            else if (i >= ds.length)
                sb.append(ps[i]);
            else
                decompressField(sb, ps[i], ds[i]);
        }
        removeTrailingDelimiters(sb, sbl, delimiter);
    }
    
    /**
     * Receive two strings, the first which is the serialized value of a field , while the second is the delta between this
     * and a second value of the field, and return a string that is the serialized value of the second field after applying
     * the delta on the first.
     * @param previous
     * @param delta
     * @return 
     */
    private void decompressField(StringBuilder sb, String previous, String delta) {
        if (delta.isEmpty())
            sb.append(previous);
        else if (previous == null || previous.isEmpty())
            sb.append(delta);
        else switch (delta.charAt(0)) {
        case REMOVE_INDICATOR:
            break;
        case LIST_START:
            decompressList(sb, previous, delta, LIST_START, LIST_SEPARATOR, LIST_END);
            break;
        case MAP_START:
            decompressMapField(sb, previous, delta);
            break;
        case OBJECT_START:
            decompressList(sb, previous, delta, OBJECT_START, FIELD_SEPARATOR, OBJECT_END);
            break;
        default:
            decompressString(sb, previous, delta);
        }
    }
    
    /**
     * Calculate value of current string from the values of a previous string and a delta string
     * @param previous
     * @param delta
     * @return 
     */
    private void decompressString(StringBuilder sb, String previous, String delta) {
        // Check if this is a number compression
        if (delta.charAt(0) == NUMBER_DIFFERENCE) {
            sb.append(decompressNumber(previous, delta));
            return;
        }
        
        // expand overlap indicators
        int n = delta.indexOf(SUBSTRING_MATCH);
        if (n < 0)
            sb.append(delta);
        else {
            int mp = 0, md = 0;
            String p = unescapeDelimiters(previous);
            do {
                sb.append(delta.substring(md, n++));
                int skip = reverseBase64Map.get(delta.charAt(n++));
                int copy = reverseBase64Map.get(delta.charAt(n++));
                sb.append(escapeDelimiters(p.substring(mp + skip, mp + skip + copy)));
                mp += skip + copy;
                md = n;
                n = delta.indexOf(SUBSTRING_MATCH, md);
            } while (n >= 0);
            sb.append(delta.substring(md)).toString();
        }
    }

    /**
     * Decompress a general object, consisted of the assignment class-name=object-values
     * @param previous      the delta value, split
     * @param deltaParts    the current value, split by the assignment character
     * @return 
     */
    private void decompressGeneralObject(StringBuilder sb, String[] previousParts, String[] deltaParts) {
        decompressValues(sb, previousParts[0], deltaParts[0], '.');
        sb.append(ASSIGMENT);
        // If classes of previous and current are not the same (delta is not empty), object is not compressed
        if (deltaParts[0].isEmpty())
            decompressField(sb, previousParts[1], deltaParts[1]);
        else
            sb.append(deltaParts[1]);
    }
    
    /**
     * Decompress a value marked as a number difference
     * @param previous
     * @param delta
     * @return 
     */
    private String decompressNumber(String previous, String delta) {
        // try long values
        String dl = delta.substring(1);
        try {
            return String.valueOf(Long.parseLong(previous) + Long.parseLong(dl));
        } catch (NumberFormatException ex) {
        }

        // try double values
        return String.valueOf(Double.parseDouble(previous) + Double.parseDouble(dl));
    }
    
    /**
     * Receive two strings, the first which is the serialized value of an array or a list, while the second is the delta
     * between this and a second list, and return a string that is the serialized value of the second list after applying the
     * delta on the first.
     * @param previous
     * @param delta
     * @param listStart         the character that precedes the serialized list
     * @param listSeparator     the character that delimits the list elements
     * @param listEnd           the character that ends the serialized list
     */
    private void decompressList(StringBuilder sb, String previous, String delta, char listStart, char listSeparator,
        char listEnd)
    {
        if (previous.charAt(0) != listStart)
            sb.append(delta);
        else {
            sb.append(listStart);
            int sbl = sb.length();
            previous = previous.substring(1, previous.length() - 1);
            delta = delta.substring(1, delta.length() - 1);
            
            // Try if this is a general object
            String[] deltaParts;
            if (listStart == OBJECT_START && (deltaParts = splitFields(delta, ASSIGMENT)).length == 2) {
                String[] previousParts = splitFields(previous, ASSIGMENT);
                if (previousParts.length != 2)
                    sb.append(delta);
                else
                    decompressGeneralObject(sb, previousParts, deltaParts);
            } else
                decompressValues(sb, previous, delta, listSeparator);
            
            if (sb.length() > sbl)
                sb.append(listEnd);
            else
                sb.replace(sbl - 1, sbl, String.valueOf(EMPTY_OBJECT));
        }
    }

    /**
     * Receive two strings, the first which is the serialized value of a map or set field, while the second is the delta between
     * this and a second map or set, and return a string that is the serialized value of the second after applying the delta
     * on the first.
     * @param previous
     * @param delta
     * @return the reconstructed updated string
     */
    private void decompressMapField(StringBuilder sb, String previous, String delta) {
        if (previous.charAt(0) != MAP_START)
            sb.append(delta);
        else {
            sb.append(MAP_START);
            int sbl = sb.length();
            decompressMap(sb, previous.substring(1, previous.length() - 1), delta.substring(1, delta.length() - 1));
            if (sb.length() > sbl)
                sb.append(MAP_END);
            else
                sb.deleteCharAt(sbl - 1);
        }
    }
    
    /**
     * Exposed version of decompressMap
     * @param previous
     * @param delta
     * @return the decompressed string
     */
    String decompressMap(String previous, String delta) {
        StringBuilder sb = new StringBuilder();
        decompressMap(sb, previous, delta);
        return sb.toString();
    }
    
    /**
     * Receive two strings, the first which is the serialized value of a map or set, while the second is the delta between
     * this and a second map or set, and return a string that is the serialized value of the second after applying the delta
     * on the first.
     * @param previous
     * @param delta
     * @return the reconstructed updated string
     */
    private void decompressMap(StringBuilder sb, String previous, String delta) {
        if (isNullIndicator(previous) || isNullIndicator(delta)) {
            sb.append(delta);
            return;
        }
        ZDictionary pm = parseMap(previous);
        ZDictionary dm = parseMap(delta);
        
        // compare the maps and create the updated array
        int sbl = sb.length();
        dm.entrySet().forEach((e) -> {
            String key = e.getKey();
            String dv = e.getValue();
            if (String.valueOf(REMOVE_INDICATOR).equals(dv))
                pm.remove(key);
            else {
                if (sb.length() > sbl)
                    sb.append(LIST_SEPARATOR);
                sb.append(key);
                String pv = pm.get(key);
                if (dv == null) {
                    if (pv != null)
                        sb.append(ASSIGMENT).append(NULL_INDICATOR);
                } else {
                    sb.append(ASSIGMENT);
                    decompressField(sb, pv, dv);
                }
            }
        });
        // Add all previous entries that do not exist in the delta map
        pm.keySet().removeAll(dm.keySet());
        for (Entry<String,String> e : pm.entrySet()) {
            if (sb.length() > sbl)
                sb.append(LIST_SEPARATOR);
            sb.append(e.getKey());
            String pv = e.getValue();
            if (pv != null)
                sb.append(ASSIGMENT).append(pv);
        }
    }
}
