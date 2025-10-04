package com.cliui.utils;

import rikka.shizuku.Shizuku;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShizukuManager {

    public static boolean executeCommand(String cmd) {
        try {
            Process process = Shizuku.newProcess(
                new String[]{"/system/bin/sh", "-c", cmd},
                null,
                null
            );
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String executeCommandWithOutput(String cmd) {
        try {
            Process process = Shizuku.newProcess(
                new String[]{"/system/bin/sh", "-c", cmd},
                null,
                null
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            process.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
