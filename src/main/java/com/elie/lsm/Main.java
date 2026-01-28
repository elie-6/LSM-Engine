package com.elie.lsm;

import com.elie.lsm.engine.LSMEngine;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        // Open the LSM Engine
        LSMEngine db = new LSMEngine("data/wal.log");

        System.out.println("LSM Engine started!");




      /*  db.put("E", "5");
        db.put("F", "6");
        db.put("G", "7");
        db.put("H", "8");
        db.put("I", "9");
        db.put("J", "10");*/

        System.out.println("Inserted 5 keys!");
        // Read keys back
        String[] keys = {"J"};
        for (String key : keys) {
            String value = db.get(key);
            System.out.println("Key: " + key + ", Value: " + value);
        }

    }
}
