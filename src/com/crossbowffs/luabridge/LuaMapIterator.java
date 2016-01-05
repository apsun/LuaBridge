package com.crossbowffs.luabridge;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

/**
 * Iterates over the elements of a {@link LuaTable}.
 * This has the same behavior as {@code pairs()} within Lua.
 * If you want to iterate over the array portion of the table
 * only, use {@link LuaArrayIterator}.
 */
public class LuaMapIterator extends LuaTableIterator {
    public LuaMapIterator(LuaTable table) {
        super(table, LuaValue.NIL);
    }

    @Override
    protected Varargs peekNext(LuaTable table, LuaValue currentKey) {
        return table.next(currentKey);
    }
}
