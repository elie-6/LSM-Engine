package com.elie.lsm.memtable;

import java.util.Map;
import java.util.TreeMap;

public class MemTable {

    private final TreeMap<String, String> table = new TreeMap<>();

    public void put(String key, String value) {
        table.put(key, value);
    }

    public String get(String key) {
        return table.get(key);
    }

    public int size() {
        return table.size();
    }

    public Map<String, String> entries() {
        return table;
    }

    public void clear() {
        table.clear();
    }
}
