package com.leaf.service;

import com.leaf.model.RemoteEntry;
import com.leaf.utils.CmdUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdbService {

    public static final String SD_CARD_PATH = "/sdcard";

    private final String adbPath;
    private final String deviceId;

    public AdbService(String adbPath, String deviceId) {
        this.adbPath = adbPath;
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getAdbPath() {
        return adbPath;
    }

    public static List<String> listDevices(String adbPath) {
        List<String> devices = new ArrayList<>();
        CmdUtil.ExecResult result = execAdb(adbPath, null, "devices");
        for (String line : result.stdout) {
            if (line.contains("\t")) {
                String[] parts = line.split("\t");
                if (parts.length == 2 && "device".equals(parts[1])) {
                    devices.add(parts[0]);
                }
            }
        }
        return devices;
    }

    public static class ListDirectoryResult {
        public final List<RemoteEntry> entries;
        public final boolean success;

        public ListDirectoryResult(List<RemoteEntry> entries, boolean success) {
            this.entries = entries;
            this.success = success;
        }
    }

    public ListDirectoryResult listDirectory(String remotePath) {
        List<RemoteEntry> entries = new ArrayList<>();
        CmdUtil.ExecResult result = execShell("ls", "-1p", remotePath);
        if (!result.isSuccess()) {
            return new ListDirectoryResult(entries, false);
        }
        for (String line : result.stdout) {
            if (line.isBlank()) {
                continue;
            }
            boolean directory = line.endsWith("/");
            String name = directory ? line.substring(0, line.length() - 1) : line;
            if (".".equals(name) || "..".equals(name)) {
                continue;
            }
            String fullPath = joinRemotePath(remotePath, name);
            entries.add(new RemoteEntry(name, fullPath, directory));
        }
        entries.sort((a, b) -> {
            if (a.isDirectory() != b.isDirectory()) {
                return a.isDirectory() ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return new ListDirectoryResult(entries, true);
    }

    public boolean pull(String remotePath, String localDir) {
        File localFile = new File(localDir, new File(remotePath).getName());
        CmdUtil.ExecResult result = execAdb(adbPath, deviceId, "pull", remotePath, localFile.getAbsolutePath());
        return result.isSuccess();
    }

    public int pullMultiple(List<String> remotePaths, String localDir) {
        int successCount = 0;
        for (String remotePath : remotePaths) {
            if (pull(remotePath, localDir)) {
                successCount++;
            }
        }
        return successCount;
    }

    public int deleteFiles(List<String> remotePaths) {
        int successCount = 0;
        for (String remotePath : remotePaths) {
            CmdUtil.ExecResult result = execShell("rm", remotePath);
            if (result.isSuccess()) {
                successCount++;
            }
        }
        return successCount;
    }

    public void keyEvent(int keyCode) {
        execShell("input", "keyevent", String.valueOf(keyCode));
    }

    public void inputText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (isAsciiInputText(text)) {
            execShell("input", "text", escapeAsciiInputText(text));
            return;
        }
        CmdUtil.ExecResult clipResult = execShell("cmd", "clipboard", "set-text", text);
        if (clipResult.isSuccess()) {
            execShell("input", "keycombination", "113", "50");
        }
    }

    private static boolean isAsciiInputText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 127) {
                return false;
            }
        }
        return true;
    }

    private static String escapeAsciiInputText(String text) {
        return text.replace(" ", "%s")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("&", "\\&")
                .replace("<", "\\<")
                .replace(">", "\\>")
                .replace("|", "\\|")
                .replace(";", "\\;")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    public static String parentPath(String path) {
        if (path == null || path.equals("/") || path.equals(SD_CARD_PATH)) {
            return SD_CARD_PATH;
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return SD_CARD_PATH;
        }
        return path.substring(0, lastSlash);
    }

    public static String joinRemotePath(String base, String name) {
        if (base.endsWith("/")) {
            return base + name;
        }
        return base + "/" + name;
    }

    private CmdUtil.ExecResult execShell(String... shellArgs) {
        List<String> command = new ArrayList<>();
        command.add(adbExecutable());
        command.add("-s");
        command.add(deviceId);
        command.add("shell");
        command.addAll(Arrays.asList(shellArgs));
        return CmdUtil.exec(command);
    }

    private static CmdUtil.ExecResult execAdb(String adbPath, String deviceId, String... adbArgs) {
        List<String> command = new ArrayList<>();
        command.add(new File(adbPath, "adb").getAbsolutePath());
        if (deviceId != null && !deviceId.isBlank()) {
            command.add("-s");
            command.add(deviceId);
        }
        command.addAll(Arrays.asList(adbArgs));
        return CmdUtil.exec(command);
    }

    private String adbExecutable() {
        return new File(adbPath, "adb").getAbsolutePath();
    }
}
