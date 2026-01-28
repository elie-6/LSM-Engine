package com.elie.lsm.engine;

import com.elie.lsm.memtable.MemTable;
import com.elie.lsm.sstable.SSTable;
import com.elie.lsm.wal.WAL;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LSMEngine {

    private static final int MEMTABLE_THRESHOLD = 10_000;

    private final MemTable memTable = new MemTable();
    private final WAL wal;
    private final List<SSTable> sstables = new ArrayList<>();

    private boolean closed = false;

    public LSMEngine(String walPath) throws IOException {
        this.wal = new WAL(walPath);

        wal.replay(memTable::put);

        loadSSTables();
    }

    public void put(String key, String value) throws IOException {
        wal.append(key, value);
        memTable.put(key, value);
        System.out.println("size=" + memTable.size()); //test


        if (memTable.size() >= MEMTABLE_THRESHOLD) {
            flushMemTable();
        }
    }

    public String get(String key) throws IOException {
        String value = memTable.get(key);
        if (value != null) return value;

        // newest SSTable first
        for (int i = sstables.size() - 1; i >= 0; i--) {
            value = sstables.get(i).get(key);
            if (value != null) return value;
        }
        return null;
    }

    private void flushMemTable() throws IOException {
        File file = new File("data/segment_" + System.currentTimeMillis() + ".dat");
        file.getParentFile().mkdirs();

        FileOutputStream fos = new FileOutputStream(file);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));

        for (var e : memTable.entries().entrySet()) {
            writer.write(e.getKey() + "=" + e.getValue());
            writer.newLine();
        }

        writer.flush();
        fos.getFD().sync();

        writer.close();
        fos.close();

        sstables.add(new SSTable(file));

        memTable.clear();
        wal.reset();
    }

    private void loadSSTables() throws IOException {
        File dir = new File("data");
        File[] files = dir.listFiles(f -> f.getName().startsWith("segment_"));
        if (files == null) return;

        for (File f : files) {
            sstables.add(new SSTable(f));
        }
    }


    public void close() throws IOException {
        if (closed) return;

        if (memTable.size() != 0) {
            flushMemTable();
        }

        wal.fsync();
        wal.close();
        closed = true;
    }
}
