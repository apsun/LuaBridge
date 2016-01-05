package com.crossbowffs.luabridge;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.Iterator;
import java.util.NoSuchElementException;

/* package */ abstract class LuaTableIterator implements Iterator<Varargs> {
    private LuaTable mTable;
    private LuaValue mCurrentKey;

    public LuaTableIterator(LuaTable table, LuaValue initialKey) {
        mTable = table;
        mCurrentKey = initialKey;
    }

    @Override
    public boolean hasNext() {
        Varargs entry = peekNext(mTable, mCurrentKey);
        return !entry.arg1().isnil();
    }

    @Override
    public Varargs next() {
        Varargs entry = peekNext(mTable, mCurrentKey);
        mCurrentKey = entry.arg1();
        if (mCurrentKey.isnil()) {
            throw new NoSuchElementException();
        }
        return entry;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    protected abstract Varargs peekNext(LuaTable table, LuaValue currentKey);
}
