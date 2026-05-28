package com.leaf.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ScrcpyService {

    private Process process;
    private String runningDeviceId;
    private final StringBuilder recentOutput = new StringBuilder();
    private volatile boolean startConfirmed;
    private volatile boolean failureNotified;

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public String getRunningDeviceId() {
        return runningDeviceId;
    }

    public void start(String scrcpyPath, String adbPath, String deviceId, Consumer<String> onStartFailed) {
        if (isRunning()) {
            if (deviceId.equals(runningDeviceId)) {
                return;
            }
            stop();
        }

        String executable = resolveScrcpyExecutable(scrcpyPath);
        if (executable == null) {
            notifyFailure(onStartFailed, "未找到 scrcpy，请在顶部配置 scrcpy 路径");
            return;
        }

        File adbExecutable = new File(adbPath, "adb");
        if (!adbExecutable.exists()) {
            notifyFailure(onStartFailed, "ADB 不存在: " + adbExecutable.getAbsolutePath());
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-s");
        command.add(deviceId);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("ADB", adbExecutable.getAbsolutePath());
        builder.redirectErrorStream(true);

        try {
            recentOutput.setLength(0);
            startConfirmed = false;
            failureNotified = false;
            process = builder.start();
            runningDeviceId = deviceId;

            Thread outputReader = new Thread(() -> readProcessOutput(process), "scrcpy-output");
            outputReader.setDaemon(true);
            outputReader.start();

            Thread watcher = new Thread(() -> watchProcess(process, deviceId, onStartFailed), "scrcpy-watcher");
            watcher.setDaemon(true);
            watcher.start();

            Thread earlyFailureChecker = new Thread(() -> checkEarlyFailure(onStartFailed), "scrcpy-start-check");
            earlyFailureChecker.setDaemon(true);
            earlyFailureChecker.start();
        } catch (Exception e) {
            process = null;
            runningDeviceId = null;
            notifyFailure(onStartFailed, "无法启动 scrcpy: " + e.getMessage());
        }
    }

    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        process = null;
        runningDeviceId = null;
    }

    public static String resolveScrcpyExecutable(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            File file = new File(configuredPath);
            if (file.isDirectory()) {
                File inDir = new File(file, "scrcpy");
                if (inDir.isFile()) {
                    return inDir.getAbsolutePath();
                }
            } else if (file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        for (String path : new String[]{
                "/opt/homebrew/bin/scrcpy",
                "/usr/local/bin/scrcpy",
                "/opt/local/bin/scrcpy"
        }) {
            File candidate = new File(path);
            if (candidate.isFile()) {
                return candidate.getAbsolutePath();
            }
        }
        return null;
    }

    private void readProcessOutput(Process activeProcess) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(activeProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (recentOutput) {
                    if (recentOutput.length() < 4000) {
                        recentOutput.append(line).append('\n');
                    }
                }
            }
        } catch (Exception ignored) {
            // process closed
        }
    }

    private void watchProcess(Process activeProcess, String deviceId, Consumer<String> onStartFailed) {
        try {
            int exitCode = activeProcess.waitFor();
            if (!startConfirmed && exitCode != 0 && deviceId.equals(runningDeviceId)) {
                notifyFailure(onStartFailed, buildFailureMessage(exitCode));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (activeProcess == process) {
                process = null;
                runningDeviceId = null;
            }
        }
    }

    private void checkEarlyFailure(Consumer<String> onStartFailed) {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (process != null && process.isAlive()) {
            startConfirmed = true;
            return;
        }
        if (process != null) {
            notifyFailure(onStartFailed, buildFailureMessage(process.exitValue()));
        }
    }

    private String buildFailureMessage(int exitCode) {
        synchronized (recentOutput) {
            String output = recentOutput.toString().trim();
            if (output.isEmpty()) {
                return "scrcpy 启动失败，退出码: " + exitCode;
            }
            return "scrcpy 启动失败，退出码: " + exitCode + "\n" + output;
        }
    }

    private void notifyFailure(Consumer<String> onStartFailed, String message) {
        if (onStartFailed == null || failureNotified) {
            return;
        }
        failureNotified = true;
        onStartFailed.accept(message);
    }
}
