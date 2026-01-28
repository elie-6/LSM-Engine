package com.elie.lsm;

import com.elie.lsm.engine.LSMEngine;

public class MainCrashRead {

    public static void main(String[] args) throws Exception {
        LSMEngine db = new LSMEngine("data/wal.log");

        for (int i = 0; i < 7; i++) {
            String key = "crash-key-" + i;
            String value = db.get(key);
            System.out.println(key + " => " + value);
        }

        db.close(); // clean shutdown this time
    }
}
