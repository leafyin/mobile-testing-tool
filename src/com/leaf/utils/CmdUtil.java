package com.leaf.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdUtil {

    public static class ExecResult {
        public final int exitCode;
        public final List<String> stdout;
        public final List<String> stderr;

        public ExecResult(int exitCode, List<String> stdout, List<String> stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public static ExecResult exec(String... command) {
        return exec(Arrays.asList(command));
    }

    public static ExecResult exec(List<String> command) {
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(false)
                    .start();
            stdout.addAll(readLines(process.getInputStream()));
            stderr.addAll(readLines(process.getErrorStream()));
            int exitCode = process.waitFor();
            return new ExecResult(exitCode, stdout, stderr);
        } catch (Exception e) {
            e.printStackTrace();
            stderr.add(e.getMessage());
            return new ExecResult(-1, stdout, stderr);
        }
    }

    /** @deprecated use {@link #exec(String...)} */
    @Deprecated
    public static ArrayList<String> execCMD(String cmd) {
        ExecResult result = exec("sh", "-c", cmd);
        return new ArrayList<>(result.stdout);
    }

    private static List<String> readLines(java.io.InputStream stream) throws Exception {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
