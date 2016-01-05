package com.crossbowffs.luabridge;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

/**
 * Iterates over the array part of a {@link LuaTable}.
 * This has the same behavior as {@code ipairs()} within Lua.
 * If you want to iterate over all items within the table,
 * use {@link LuaMapIterator}.
 */
public class LuaArrayIterator extends LuaTableIterator {
    public LuaArrayIterator(LuaTable table) {
        super(table, LuaValue.ZERO);
    }

    @Override
    protected Varargs peekNext(LuaTable table, LuaValue currentKey) {
        return table.inext(currentKey);
    }
}
