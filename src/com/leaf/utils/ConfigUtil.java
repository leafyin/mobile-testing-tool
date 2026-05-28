package com.leaf.utils;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ConfigUtil {

    private static final Path CONFIG_DIR =
            Paths.get(System.getProperty("user.home"), ".logan");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");
    private static final String KEY_ADB_PATH = "adb.path";
    private static final String KEY_SCRCPY_PATH = "scrcpy.path";

    public static String loadAdbPath() {
        return loadProperty(KEY_ADB_PATH);
    }

    public static void saveAdbPath(String path) {
        saveProperty(KEY_ADB_PATH, path);
    }

    public static String loadScrcpyPath() {
        return loadProperty(KEY_SCRCPY_PATH);
    }

    public static void saveScrcpyPath(String path) {
        saveProperty(KEY_SCRCPY_PATH, path);
    }

    private static String loadProperty(String key) {
        Properties props = new Properties();
        if (!Files.exists(CONFIG_FILE)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
            return props.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void saveProperty(String key, String value) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Properties props = new Properties();
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
            }
            props.setProperty(key, value);
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "Logan App Config");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}