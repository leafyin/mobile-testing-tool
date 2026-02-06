package com.leaf.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class CmdUtil {

    public static ArrayList<String> execCMD(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            ArrayList<String> cmdOutput = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                cmdOutput.add(line);
            }
            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("退出码: " + exitCode);
            return cmdOutput;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

}
