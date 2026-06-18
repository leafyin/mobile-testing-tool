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
    private static final String KEY_EXPORT_PATH = "export.path";
    private static final String KEY_INPUT_HISTORY_PREFIX = "input.history.";
    public static final int MAX_INPUT_HISTORY = 20;

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

    public static String loadExportPath() {
        return loadProperty(KEY_EXPORT_PATH);
    }

    public static void saveExportPath(String path) {
        saveProperty(KEY_EXPORT_PATH, path);
    }

    public static java.util.List<String> loadInputHistory() {
        java.util.List<String> history = new java.util.ArrayList<>();
        Properties props = loadAllProperties();
        for (int i = 0; i < MAX_INPUT_HISTORY; i++) {
            String value = props.getProperty(KEY_INPUT_HISTORY_PREFIX + i);
            if (value == null || value.isBlank()) {
                break;
            }
            history.add(value);
        }
        return history;
    }

    public static void saveInputHistory(java.util.List<String> history) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Properties props = loadAllProperties();
            for (int i = 0; i < MAX_INPUT_HISTORY; i++) {
                props.remove(KEY_INPUT_HISTORY_PREFIX + i);
            }
            int count = Math.min(history.size(), MAX_INPUT_HISTORY);
            for (int i = 0; i < count; i++) {
                props.setProperty(KEY_INPUT_HISTORY_PREFIX + i, history.get(i));
            }
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "Logan App Config");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Properties loadAllProperties() {
        Properties props = new Properties();
        if (!Files.exists(CONFIG_FILE)) {
            return props;
        }
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    private static String loadProperty(String key) {
        return loadAllProperties().getProperty(key);
    }

    private static void saveProperty(String key, String value) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Properties props = loadAllProperties();
            props.setProperty(key, value);
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "Logan App Config");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}