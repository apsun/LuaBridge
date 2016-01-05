package com.crossbowffs.luabridge;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Wraps a Java method as a {@link LuaFunction} object, so
 * that it can be called from a Lua script.
 */
/* package */ class ExposedJavaMethod extends VarArgFunction {
    private static final Object[] EMPTY_PARAMS = new Object[0];

    private final Method mJavaMethod;
    private final ExposeToLua mAnnotation;
    private Class<?>[] mParameterTypes;
    private Object[] mParameters;

    public ExposedJavaMethod(Method method, ExposeToLua annotation) {
        mJavaMethod = method;
        mAnnotation = annotation;
    }

    private void ensureBuffers() {
        // These are lazily allocated since we don't expect every
        // method to be called, so we can reduce memory allocations
        // on startup
        if (mParameterTypes == null) {
            mParameterTypes = mJavaMethod.getParameterTypes();
        }
        if (mParameters == null) {
            if (mParameterTypes.length == 0) {
                mParameters = EMPTY_PARAMS;
            } else {
                mParameters = new Object[mParameterTypes.length];
            }
        }
    }

    private Object toJavaClass(LuaValue userdata) {
        if (userdata instanceof ExposedJavaClass) {
            return userdata.touserdata(mJavaMethod.getDeclaringClass());
        }

        String methodName = mAnnotation.value();
        if (methodName.isEmpty()) {
            methodName = mJavaMethod.getName();
        }

        String errMsg = String.format(
            "First argument is not a Java object, " +
            "did you mean :%1$s() instead of .%1$s()?", methodName);
        throw new LuaError(errMsg);
    }

    private Object invokeMethod(Object thisObject, Object[] parameters) {
        try {
            return mJavaMethod.invoke(thisObject, parameters);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw new LuaError(e.getCause());
        }
    }

    @Override
    public Varargs invoke(Varargs args) {
        int argCount = args.narg();
        Object thisObject = null;
        int destIndex = 0, srcIndex = 1;

        // Pop off the first argument if the target is an instance method
        if (!Modifier.isStatic(mJavaMethod.getModifiers())) {
            thisObject = toJavaClass(args.arg1());
            srcIndex++;
            argCount--;
        }

        ensureBuffers();
        Object[] parameters = mParameters;

        // Convert arguments to native Java objects
        while (destIndex < argCount && destIndex < parameters.length) {
            Class<?> expectedType = mParameterTypes[destIndex];
            // Allow the usage of varargs, if and only if the Java method
            // has Varargs as its last argument
            if (expectedType.equals(Varargs.class)) {
                if (destIndex == parameters.length - 1) {
                    parameters[destIndex++] = args.subargs(srcIndex++);
                } else {
                    throw new LuaError("Varargs must be the last argument in target Java method");
                }
            } else {
                parameters[destIndex++] = LuaUtils.bridgeLuaToJava(
                    args.arg(srcIndex++), expectedType);
            }
        }

        // This is necessary in case a call to this method is re-entrant -
        // the previous call might not have reached the cleanup block yet
        for (int i = destIndex; i < parameters.length; ++i) {
            parameters[i] = null;
        }

        Object returnValue = invokeMethod(thisObject, parameters);

        // Clear argument buffer for next use
        while (--destIndex >= 0) {
            parameters[destIndex] = null;
        }

        // If the method returns void, the return value
        // from invoke() will be null, which will be wrapped
        // to nil anyways, so we don't have to check the
        // method return type.
        return LuaUtils.bridgeJavaToLuaOut(returnValue);
    }
}
