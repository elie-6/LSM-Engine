package com.elie.lsm.wal;

import java.io.*;

public class WAL {

    private static final int FSYNC_EVERY = 100;

    private final File file;
    private final FileOutputStream fos;
    private final BufferedWriter writer;

    private int pendingWrites = 0;

    public WAL(String path) throws IOException {
        this.file = new File(path);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        this.fos = new FileOutputStream(file, true);
        this.writer = new BufferedWriter(new OutputStreamWriter(fos));
    }

    public void append(String key, String value) throws IOException {
        writer.write(key + "=" + value);
        writer.newLine();
        writer.flush();          // JVM → OS
        pendingWrites++;

        if (pendingWrites >= FSYNC_EVERY) {
            fsync();
        }
    }

    public void fsync() throws IOException {
        if (pendingWrites > 0) {
            fos.getFD().sync();      // OS → disk
            pendingWrites = 0;
        }
    }


    public void replay(WALConsumer consumer) throws IOException {
        if (!file.exists()) return;

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                consumer.apply(parts[0], parts[1]);
            }
        }
        reader.close();
    }


    public void reset() throws IOException {
        fsync();

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(0);
            raf.getFD().sync();
        }

        pendingWrites = 0;
    }


    public void close() throws IOException {
        fsync();
        writer.close();
        fos.close();
    }

    public interface WALConsumer {
        void apply(String key, String value);
    }
}
