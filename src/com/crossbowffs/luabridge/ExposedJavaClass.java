package com.crossbowffs.luabridge;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps a Java object as a {@link LuaUserdata} object, and exposes
 * methods annotated with {@link ExposeToLua} as functions
 * in Lua. Static methods should be called using {@code obj.method()},
 * while instance methods should be called using {@code obj:method()}.
 */
public class ExposedJavaClass extends LuaUserdata {
    private static Map<Class<?>, LuaTable> sMetatableCache = new HashMap<Class<?>, LuaTable>();

    /**
     * Creates a Java object wrapper class from {@code this}. This
     * constructor should be used if you subclass this class. This
     * is logically equivalent to passing {@code this} to the
     * {@link #ExposedJavaClass(Object)} constructor.
     */
    protected ExposedJavaClass() {
        super(null);
        m_instance = this;
        m_metatable = getMetatable(getClass());
    }

    /**
     * Creates a Java object wrapper class from {@code obj} that
     * delegates all method calls to it. This constructor should
     * be used if you are wrapping a separate class.
     *
     * @param obj The object to delegate method calls to.
     */
    public ExposedJavaClass(Object obj) {
        super(obj, createMetatable(obj.getClass()));
    }

    private static LuaTable getMetatable(Class<?> cls) {
        LuaTable metatable = sMetatableCache.get(cls);
        if (metatable == null) {
            metatable = createMetatable(cls);
            sMetatableCache.put(cls, metatable);
        }
        return metatable;
    }

    private static LuaTable createMetatable(Class<?> cls) {
        final LuaTable methodTable = new LuaTable();

        for (Method method : cls.getMethods()) {
            ExposeToLua annotation = method.getAnnotation(ExposeToLua.class);
            if (annotation == null) {
                continue;
            }

            // Use the name provided in the annotation if specified,
            // or the Java method name otherwise
            String exposedName = annotation.value();
            if (exposedName == null || exposedName.isEmpty()) {
                exposedName = method.getName();
            }

            if (!methodTable.get(exposedName).isnil()) {
                throw new IllegalArgumentException("Duplicate method name: " + exposedName);
            }

            // Make sure our method is callable via reflection
            method.setAccessible(true);

            // Add our wrapped function into the metatable
            ExposedJavaMethod luaFunction = new ExposedJavaMethod(method, annotation);
            methodTable.set(exposedName, luaFunction);
        }

        methodTable.set(LuaValue.INDEX, new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue object, LuaValue key) {
                LuaValue value = methodTable.rawget(key);
                if (value.isnil()) {
                    throw new LuaError("Exposed method not found: " + key);
                }
                return value;
            }
        });

        return methodTable;
    }

    @Override
    public String toString() {
        return "ExposedJavaClass{" + userdata().getClass().getSimpleName() + "}";
    }
}
