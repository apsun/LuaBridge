package com.crossbowffs.luabridge;

import java.lang.annotation.*;

/**
 * Marks a Java method to be exposed to Lua. Only methods
 * marked with this annotation will be callable from
 * Lua code. Only public methods will be exposed - this
 * annotation has no effect on private methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExposeToLua {
    /**
     * The name of the method when imported into Lua.
     * If this is not provided, the name of the Java method
     * will be used.
     */
    String value() default "";
}
