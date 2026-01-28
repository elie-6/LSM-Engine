package com.elie.lsm;

import com.elie.lsm.engine.LSMEngine;

public class MainCrashWrite {

    public static void main(String[] args) throws Exception {
        LSMEngine db = new LSMEngine("data/wal.log");

        for (int i = 0; i < 7; i++) {
            db.put("crash-key-" + i, "value-" + i);
            System.out.println("Wrote crash-key-" + i);
        }

        System.out.println("💥 Simulating crash now");
        System.exit(1); // NO close(), NO flush
    }
}
