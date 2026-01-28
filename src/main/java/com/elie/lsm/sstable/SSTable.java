package com.elie.lsm.sstable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SSTable {

    private final File file;
    private final List<Long> offsets = new ArrayList<>();
    private final List<String> keys = new ArrayList<>();

    public SSTable(File file) throws IOException {
        this.file = file;
        buildIndex();
    }

    private void buildIndex() throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        long pos = 0;
        String line;

        while ((line = raf.readLine()) != null) {
            offsets.add(pos);
            keys.add(line.split("=", 2)[0]);
            pos = raf.getFilePointer();
        }
        raf.close();
    }

    public String get(String key) throws IOException {
        int idx = binarySearch(key);
        if (idx < 0) return null;

        RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(offsets.get(idx));
        String line = raf.readLine();
        raf.close();

        return line.split("=", 2)[1];
    }

    private int binarySearch(String key) {
        int lo = 0, hi = keys.size() - 1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = keys.get(mid).compareTo(key);

            if (cmp == 0) return mid;
            if (cmp < 0) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1;
    }
}
