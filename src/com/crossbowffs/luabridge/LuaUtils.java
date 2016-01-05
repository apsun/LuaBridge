package com.crossbowffs.luabridge;

import org.luaj.vm2.*;

import java.lang.reflect.Array;

public final class LuaUtils {
    private LuaUtils() { }

    /**
     * Converts a Java object to its Lua equivalent. The logic
     * is as follows:
     * <p>
     * <ol>
     *     <li>{@code null} -> {@link LuaNil}</li>
     *     <li>{@link String} -> {@link LuaString}</li>
     *     <li>{@link Integer} -> {@link LuaInteger}</li>
     *     <li>(other primitive wrapper classes...)</li>
     *     <li>{@link LuaValue} -> {@link LuaValue}</li>
     *     <li>{@link Object} -> {@link LuaUserdata}</li>
     * </ol>
     *
     * <p>
     * This method does *not* handle {@link Varargs} objects,
     * use {@link #bridgeJavaToLuaOut(Object)} if you need to handle
     * them. This is intended to be used when calling Lua functions
     * from Java.
     *
     * @param javaValue The value to convert.
     */
    public static LuaValue bridgeJavaToLuaIn(Object javaValue) {
        if (javaValue == null) {
            return LuaValue.NIL;
        } else if (javaValue instanceof LuaValue) {
            return (LuaValue)javaValue;
        } else if (javaValue instanceof String) {
            return LuaString.valueOf((String)javaValue);
        } else if (javaValue instanceof Integer) {
            return LuaInteger.valueOf((Integer)javaValue);
        } else if (javaValue instanceof Float) {
            return LuaDouble.valueOf((Float)javaValue);
        } else if (javaValue instanceof Boolean) {
            return LuaBoolean.valueOf((Boolean)javaValue);
        } else if (javaValue instanceof Double) {
            return LuaDouble.valueOf((Double)javaValue);
        } else if (javaValue instanceof Long) {
            return LuaInteger.valueOf((Long)javaValue);
        } else if (javaValue instanceof Short) {
            return LuaInteger.valueOf((Short)javaValue);
        } else if (javaValue instanceof Byte) {
            return LuaInteger.valueOf((Byte)javaValue);
        } else if (javaValue instanceof Character) {
            return LuaInteger.valueOf((Character)javaValue);
        } else if (javaValue.getClass().isArray()) {
            return toTable(javaValue);
        } else {
            return new LuaUserdata(javaValue);
        }
    }

    /**
     * The same as {@link #bridgeJavaToLuaIn(Object)}, but also
     * handles {@link Varargs} objects. This is intended to be used
     * when returning values from a Java method to Lua.
     *
     * @param javaValue The value to convert.
     */
    public static Varargs bridgeJavaToLuaOut(Object javaValue) {
        if (javaValue instanceof Varargs) {
            return (Varargs)javaValue;
        } else {
            return bridgeJavaToLuaIn(javaValue);
        }
    }

    /**
     * Converts a Lua object to its Java equivalent.
     *
     * <p>
     * If {@code luaValue} is {@code nil} and {@code expectedType}
     * does not refer to a primitive type, {@code null} will be returned.
     * If {@code expectedType} does refer to a primitive type, an
     * exception will be thrown.
     *
     * <p>
     * If {@code expectedType} refers to a Java primitive type or its
     * corresponding wrapper type, {@code luaValue} will be converted
     * to the wrapper type.
     *
     * <p>
     * If {@code expectedType} refers to a subclass of {@link LuaValue},
     * the Lua object will be directly returned.
     *
     * <p>
     * If {@code expectedType} refers to any other Java class,
     * {@code luaValue} must be an instance of {@link LuaUserdata} that
     * wraps that type.
     *
     * @param luaValue The value to convert.
     * @param expectedType The Java class to convert the value to.
     */
    public static Object bridgeLuaToJava(LuaValue luaValue, Class<?> expectedType) {
        if (luaValue.isnil() && !expectedType.isPrimitive()) {
            return null;
        } else if (String.class.equals(expectedType)) {
            return luaValue.checkjstring();
        } else if (int.class.equals(expectedType) || Integer.class.equals(expectedType)) {
            return luaValue.checkint();
        } else if (float.class.equals(expectedType) || Float.class.equals(expectedType)) {
            return (float)luaValue.checkdouble();
        } else if (boolean.class.equals(expectedType) || Boolean.class.equals(expectedType)) {
            return luaValue.checkboolean();
        } else if (double.class.equals(expectedType) || Double.class.equals(expectedType)) {
            return luaValue.checkdouble();
        } else if (long.class.equals(expectedType) || Long.class.equals(expectedType)) {
            return luaValue.checklong();
        } else if (short.class.equals(expectedType) || Short.class.equals(expectedType)) {
            return (short)luaValue.checkint();
        } else if (byte.class.equals(expectedType) || Byte.class.equals(expectedType)) {
            return (byte)luaValue.checkint();
        } else if (char.class.equals(expectedType) || Character.class.equals(expectedType)) {
            return (char)luaValue.checkint();
        } else if (LuaValue.class.isAssignableFrom(expectedType)) {
            if (!expectedType.isAssignableFrom(luaValue.getClass())) {
                throw new LuaError("Cannot convert " + luaValue.typename() +
                    " to " + expectedType.getName());
            }
            return luaValue;
        } else if (expectedType.isArray()) {
            return toArray(luaValue.checktable(), expectedType.getComponentType());
        } else {
            return luaValue.checkuserdata(expectedType);
        }
    }

    /**
     * Converts a Lua object to its Java equivalent. Automatically
     * determines the output object type based on the type of the Lua
     * object. Can only convert {@link LuaNil}, {@link LuaString},
     * {@link LuaBoolean}, {@link LuaInteger}, {@link LuaDouble}, and
     * {@link LuaUserdata}. All other values will pass through
     * unconverted.
     *
     * @param luaValue The value to convert.
     */
    public static Object bridgeLuaToJava(LuaValue luaValue) {
        switch (luaValue.type()) {
        case LuaValue.TNIL:
            return null;
        case LuaValue.TSTRING:
            return luaValue.tojstring();
        case LuaValue.TBOOLEAN:
            return luaValue.toboolean();
        case LuaValue.TINT:
            return luaValue.toint();
        case LuaValue.TNUMBER:
            return luaValue.todouble();
        case LuaValue.TUSERDATA:
            return luaValue.touserdata();
        default:
            return luaValue;
        }
    }

    /**
     * Checks whether the specified table is an array - that is,
     * all keys are integers and in the sequence {1, 2, ..., N},
     * where N is the number of elements in the table.
     *
     * @param table The table to check.
     */
    public static boolean isArray(LuaTable table) {
        // Algorithm from http://stackoverflow.com/a/6080274/1808989
        // Basically, we just check whether the number of items
        // returned from ipairs() is the same as the number of items
        // returned from pairs(). If true, then the table is an array.
        int i = 0;
        LuaMapIterator iterator = new LuaMapIterator(table);
        while (iterator.hasNext()) {
            iterator.next();
            if (table.get(++i).isnil()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts the specified table to a Java array, converting
     * {@link LuaValue} objects to their corresponding Java
     * equivalents using {@link #bridgeLuaToJava(LuaValue, Class)}.
     *
     * @param table The table to convert.
     * @param expectedType The type of the elements within the array.
     */
    public static Object toArray(LuaTable table, Class<?> expectedType) {
        int length = table.length();
        Object array = Array.newInstance(expectedType, length);
        for (int i = 0; i < length; ++i) {
            LuaValue value = table.get(i + 1);
            Array.set(array, i, bridgeLuaToJava(value, expectedType));
        }
        return array;
    }

    /**
     * Converts the specified table to a Java array, converting
     * {@link LuaValue} objects to their corresponding Java
     * equivalents using {@link #bridgeLuaToJava(LuaValue)}.
     *
     * @param table The table to convert.
     */
    public static Object[] toArray(LuaTable table) {
        int length = table.length();
        Object[] array = new Object[length];
        for (int i = 0; i < length; ++i) {
            LuaValue value = table.get(i + 1);
            array[i] = bridgeLuaToJava(value);
        }
        return array;
    }

    /**
     * Converts the specified {@link Varargs} instance to a Java
     * array, converting {@link LuaValue} objects to their corresponding
     * Java equivalents using {@link #bridgeLuaToJava(LuaValue)}.
     *
     * <p>
     * This method is intended to be used for converting Lua {@link Varargs}
     * to be used in Java methods that take {@code Object...} as a parameter,
     * such as {@link String#format(String, Object...)}.
     *
     * @param args The varargs to convert.
     */
    public static Object[] toArray(Varargs args) {
        int length = args.narg();
        Object[] array = new Object[length];
        for (int i = 0; i < length; ++i) {
            LuaValue value = args.arg(i + 1);
            array[i] = bridgeLuaToJava(value);
        }
        return array;
    }

    /**
     * Converts the specified Java array to a Lua table, converting
     * Java objects to their corresponding Lua equivalents using
     * {@link #bridgeJavaToLuaIn(Object)}.
     *
     * @param array The array to convert.
     */
    public static LuaTable toTable(Object array) {
        int length = Array.getLength(array);
        LuaTable table = new LuaTable(length, 0);
        for (int i = 0; i < length; ++i) {
            LuaValue value = bridgeJavaToLuaIn(Array.get(array, i));
            table.set(i + 1, value);
        }
        return table;
    }
}
