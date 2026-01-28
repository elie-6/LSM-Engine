package com.elie.lsm;

import java.io.File;

public class LSMUtils {


     // Delete all WAL and SSTable files in data/ folder

    public static void clearDataFolder() {
        File dir = new File("data");
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            boolean deleted = f.delete();
            if (!deleted) {
                System.err.println("Failed to delete: " + f.getAbsolutePath());
            }
        }
    }
}
