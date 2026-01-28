package com.elie.lsm;

import com.elie.lsm.engine.LSMEngine;


public class MainBenchmarkFlush {

    public static void main(String[] args) throws Exception {
        int N = 100_000;// total writes
        LSMUtils.clearDataFolder();

        LSMEngine db = new LSMEngine("data/wal.log");

        long start = System.nanoTime();

        for (int i = 0; i < N; i++) {
            db.put("key-" + i, "value-" + i);
        }

        long end = System.nanoTime();

        db.close();


        double seconds = (end - start) / 1_000_000_000.0;
        double opsPerSec = N / seconds;

        System.out.println("Total writes: " + N);
        System.out.println("Time (s): " + seconds);
        System.out.println("Ops/sec: " + opsPerSec);


    }
}
