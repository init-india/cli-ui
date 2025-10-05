package com.cliui.linux;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;
import java.util.regex.Pattern;

import com.cliui.utils.PermissionManager;

public class TerminalSession {

    private final Context context;
    private String currentDirectory;
    private final Map<String, String> environment;
    private final Map<String, String> aliases;
    private final List<String> commandHistory;
    private final CommandRegistry commandRegistry;

    public TerminalSession(@NonNull Context context) {
        this.context = context;
        this.currentDirectory = context.getFilesDir().getAbsolutePath();
        this.environment = new HashMap<>();
        this.aliases = new HashMap<>();
        this.commandHistory = new ArrayList<>();
        this.commandRegistry = new CommandRegistry();

        setupEnvironment();
        setupAliases();
        registerCoreCommands();
    }

    /**
     * Executes a command string.
     * Handles aliases, parses arguments, and calls the registered command handler.
     */
    public String executeCommand(@NonNull String command) {
        if (command.trim().isEmpty()) return "";

        // Add to history
        commandHistory.add(command);

        // Handle aliases
        String[] tokens = command.trim().split("\\s+");
        String baseCommand = tokens[0];
        if (aliases.containsKey(baseCommand)) {
            command = aliases.get(baseCommand) + command.substring(baseCommand.length());
            tokens = command.trim().split("\\s+");
            baseCommand = tokens[0];
        }

        String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);

        CommandRegistry.CommandHandler handler = commandRegistry.getHandler(baseCommand);
        if (handler != null) {
            return handler.execute(args, this);
        }

        return "Command not found: " + baseCommand + "\nType 'help' for available commands";
    }

    /**
     * Registers all core Linux-like commands.
     * Commands that require root access attempt Shizuku first.
     */
    private void registerCoreCommands() {

        // File Operations
        commandRegistry.register("ls", this::listFiles);
        commandRegistry.register("cd", this::changeDirectory);
        commandRegistry.register("pwd", this::printWorkingDirectory);
        commandRegistry.register("cp", this::copyFile);
        commandRegistry.register("mv", this::moveFile);
        commandRegistry.register("rm", this::deleteFile);
        commandRegistry.register("mkdir", this::createDirectory);
        commandRegistry.register("touch", this::createFile);
        commandRegistry.register("ln", this::createLink);

        // Viewing Files
        commandRegistry.register("cat", this::readFile);
        commandRegistry.register("less", this::pageFile);
        commandRegistry.register("head", this::showFileHead);
        commandRegistry.register("tail", this::showFileTail);
        commandRegistry.register("file", this::fileType);

        // Text Processing
        commandRegistry.register("sort", this::sortLines);
        commandRegistry.register("uniq", this::uniqueLines);
        commandRegistry.register("wc", this::wordCount);
        commandRegistry.register("grep", this::searchInFiles);
        commandRegistry.register("cut", this::cutColumns);
        commandRegistry.register("paste", this::pasteFiles);
        commandRegistry.register("tr", this::translateChars);
        commandRegistry.register("tee", this::teeOutput);
        commandRegistry.register("sed", this::streamEdit);
        commandRegistry.register("awk", this::awkProcessing);
        commandRegistry.register("diff", this::compareFiles);

        // Redirection & Pipes
        commandRegistry.register(">", this::redirectOutput);
        commandRegistry.register(">>", this::appendOutput);
        commandRegistry.register("<", this::redirectInput);
        commandRegistry.register("|", this::pipeCommands);

        // Permissions (Shizuku-aware)
        commandRegistry.register("chmod", this::changePermissions);
        commandRegistry.register("chown", this::changeOwner);
        commandRegistry.register("su", this::switchUser);

        // Processes
        commandRegistry.register("ps", this::showProcesses);
        commandRegistry.register("top", this::showTopProcesses);
        commandRegistry.register("kill", this::killProcess);
        commandRegistry.register("jobs", this::showJobs);
        commandRegistry.register("bg", this::backgroundJob);
        commandRegistry.register("fg", this::foregroundJob);

        // Environment
        commandRegistry.register("export", this::exportVariable);
        commandRegistry.register("alias", this::manageAliases);
        commandRegistry.register("history", this::showHistory);

        // Networking
        commandRegistry.register("ping", this::pingHost);
        commandRegistry.register("ifconfig", this::networkInterfaces);
        commandRegistry.register("netstat", this::networkStatistics);

        // Searching
        commandRegistry.register("find", this::findFiles);
        commandRegistry.register("locate", this::locateFile);
        commandRegistry.register("which", this::whichCommand);

        // Archiving
        commandRegistry.register("tar", this::createArchive);
        commandRegistry.register("gzip", this::compressFile);
        commandRegistry.register("gunzip", this::decompressFile);
        commandRegistry.register("zip", this::createZip);
        commandRegistry.register("unzip", this::extractZip);

        // Utilities
        commandRegistry.register("echo", this::echoText);
        commandRegistry.register("date", this::currentDate);
        commandRegistry.register("cal", this::showCalendar);
        commandRegistry.register("whoami", this::currentUser);
        commandRegistry.register("uname", this::systemInfo);
        commandRegistry.register("df", this::diskUsage);
        commandRegistry.register("du", this::directoryUsage);
        commandRegistry.register("clear", this::clearScreen);
        commandRegistry.register("help", this::showHelp);
        commandRegistry.register("man", this::showManual);
        commandRegistry.register("exit", this::exitShell);
    }

    // =================== PLACEHOLDER IMPLEMENTATIONS ===================
    // These implement the commands in a safe Android environment.
    // Shizuku is used where root access is required.

    // ------------------ File operations ------------------
    private String listFiles(String[] args, TerminalSession session) {
        File dir = new File(session.currentDirectory);
        if (!dir.exists() || !dir.isDirectory()) return "ls: cannot access " + session.currentDirectory;

        File[] files = dir.listFiles();
        if (files == null) return "ls: permission denied";

        StringBuilder sb = new StringBuilder();
        for (File f : files) sb.append(f.getName()).append("\n");
        return sb.toString();
    }

    private String changeDirectory(String[] args, TerminalSession session) {
        if (args.length == 0) {
            session.currentDirectory = session.context.getFilesDir().getAbsolutePath();
            return "";
        }
        String path = resolvePath(args[0]);
        File f = new File(path);
        if (!f.exists() || !f.isDirectory()) return "cd: no such directory: " + args[0];
        session.currentDirectory = path;
        return "";
    }

    private String printWorkingDirectory(String[] args, TerminalSession session) {
        return session.currentDirectory;
    }

    private String copyFile(String[] args, TerminalSession session) {
        if (args.length < 2) return "cp: missing source or destination";
        File src = new File(resolvePath(args[0]));
        File dest = new File(resolvePath(args[1]));
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) > 0) out.write(buf, 0, read);
            return "";
        } catch (IOException e) {
            return "cp: " + e.getMessage();
        }
    }

    private String moveFile(String[] args, TerminalSession session) {
        if (args.length < 2) return "mv: missing source or destination";
        File src = new File(resolvePath(args[0]));
        File dest = new File(resolvePath(args[1]));
        boolean success = src.renameTo(dest);
        return success ? "" : "mv: failed to move file";
    }

    private String deleteFile(String[] args, TerminalSession session) {
        if (args.length == 0) return "rm: missing file";
        File file = new File(resolvePath(args[0]));
        boolean success = file.delete();
        return success ? "" : "rm: failed to delete file";
    }

    private String createDirectory(String[] args, TerminalSession session) {
        if (args.length == 0) return "mkdir: missing directory name";
        File dir = new File(resolvePath(args[0]));
        boolean success = dir.mkdirs();
        return success ? "" : "mkdir: failed to create directory";
    }

    private String createFile(String[] args, TerminalSession session) {
        if (args.length == 0) return "touch: missing file name";
        File file = new File(resolvePath(args[0]));
        try {
            boolean success = file.createNewFile();
            return success ? "" : "touch: file already exists";
        } catch (IOException e) {
            return "touch: " + e.getMessage();
        }
    }

    private String createLink(String[] args, TerminalSession session) {
        return "ln: not implemented";
    }

    // ------------------ Viewing ------------------
    private String readFile(String[] args, TerminalSession session) {
        if (args.length == 0) return "cat: missing file";
        File file = new File(resolvePath(args[0]));
        if (!file.exists()) return "cat: no such file: " + args[0];
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } catch (IOException e) {
            return "cat: " + e.getMessage();
        }
    }

    private String pageFile(String[] args, TerminalSession session) {
        return "less: not fully implemented, showing first 20 lines\n";
    }

    private String showFileHead(String[] args, TerminalSession session) {
        return "head: first 10 lines preview";
    }

    private String showFileTail(String[] args, TerminalSession session) {
        return "tail: last 10 lines preview";
    }

    private String fileType(String[] args, TerminalSession session) {
        if (args.length == 0) return "file: missing argument";
        File f = new File(resolvePath(args[0]));
        return f.exists() ? (f.isDirectory() ? "directory" : "regular file") : "file not found";
    }

    // ------------------ Text Processing ------------------
    private String sortLines(String[] args, TerminalSession session) { return "sort: not implemented"; }
    private String uniqueLines(String[] args, TerminalSession session) { return "uniq: not implemented"; }
    private String wordCount(String[] args, TerminalSession session) { return "wc: not implemented"; }
    private String searchInFiles(String[] args, TerminalSession session) { return "grep: not implemented"; }
    private String cutColumns(String[] args, TerminalSession session) { return "cut: not implemented"; }
    private String pasteFiles(String[] args, TerminalSession session) { return "paste: not implemented"; }
    private String translateChars(String[] args, TerminalSession session) { return "tr: not implemented"; }
    private String teeOutput(String[] args, TerminalSession session) { return "tee: not implemented"; }
    private String streamEdit(String[] args, TerminalSession session) { return "sed: not implemented"; }
    private String awkProcessing(String[] args, TerminalSession session) { return "awk: not implemented"; }
    private String compareFiles(String[] args, TerminalSession session) { return "diff: not implemented"; }

    // ------------------ Permissions ------------------
    private String changePermissions(String[] args, TerminalSession session) {
        if (args.length < 2) return "chmod: missing arguments";
        String mode = args[0];
        File file = new File(resolvePath(args[1]));

        if (!file.exists()) return "chmod: file not found: " + args[1];

        if (Shizuku.checkSelfPermission() == Shizuku.PERMISSION_GRANTED) {
            try {
                // Example using Shizuku API for chmod
                java.lang.Runtime.getRuntime().exec(new String[]{"su", "-c", "chmod " + mode + " " + file.getAbsolutePath()});
                return "";
            } catch (IOException e) {
                return "chmod: failed via Shizuku";
            }
        } else {
            Toast.makeText(context, "Shizuku not available. Using sandbox permissions.", Toast.LENGTH_SHORT).show();
            return "chmod: operation limited to sandbox";
        }
    }

    private String changeOwner(String[] args, TerminalSession session) {
        return "chown: Shizuku required (not implemented in sandbox)";
    }

    private String switchUser(String[] args, TerminalSession session) {
        return "su: Shizuku required (not implemented in sandbox)";
    }

    // ------------------ Processes ------------------
    private String showProcesses(String[] args, TerminalSession session) {
        return "ps: process list not implemented";
    }

    private String showTopProcesses(String[] args, TerminalSession session) {
        return "top: not implemented";
    }

    private String killProcess(String[] args, TerminalSession session) { return "kill: not implemented"; }
    private String showJobs(String[] args, TerminalSession session) { return "jobs: not implemented"; }
    private String backgroundJob(String[] args, TerminalSession session) { return "bg: not implemented"; }
    private String foregroundJob(String[] args, TerminalSession session) { return "fg: not implemented"; }

    // ------------------ Environment ------------------
    private String exportVariable(String[] args, TerminalSession session) { return "export: not implemented"; }
    private String manageAliases(String[] args, TerminalSession session) { return "alias: not implemented"; }
    private String showHistory(String[] args, TerminalSession session) { return String.join("\n", commandHistory); }

    // ------------------ Networking ------------------
    private String pingHost(String[] args, TerminalSession session) { return "ping: simulated response"; }
    private String networkInterfaces(String[] args, TerminalSession session) { return "ifconfig: simulated"; }
    private String networkStatistics(String[] args, TerminalSession session) { return "netstat: simulated"; }

    // ------------------ Searching ------------------
    private String findFiles(String[] args, TerminalSession session) { return "find: not implemented"; }
    private String locateFile(String[] args, TerminalSession session) { return "locate: not implemented"; }
    private String whichCommand(String[] args, TerminalSession session) { return "which: not implemented"; }

    // ------------------ Archiving ------------------
    private String createArchive(String[] args, TerminalSession session) { return "tar: not implemented"; }
    private String compressFile(String[] args, TerminalSession session) { return "gzip: not implemented"; }
    private String decompressFile(String[] args, TerminalSession session) { return "gunzip: not implemented"; }
    private String createZip(String[] args, TerminalSession session) { return "zip: not implemented"; }
    private String extractZip(String[] args, TerminalSession session) { return "unzip: not implemented"; }

    // ------------------ Utilities ------------------
    private String echoText(String[] args, TerminalSession session) { return String.join(" ", args); }
    private String currentDate(String[] args, TerminalSession session) { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()); }
    private String showCalendar(String[] args, TerminalSession session) { return "cal: not implemented"; }
    private String currentUser(String[] args, TerminalSession session) { return "android-user"; }
    private String systemInfo(String[] args, TerminalSession session) { return "Android/Linux"; }
    private String diskUsage(String[] args, TerminalSession session) { return "df: not implemented"; }
    private String directoryUsage(String[] args, TerminalSession session) { return "du: not implemented"; }
    private String clearScreen(String[] args, TerminalSession session) { return "\u001b[H\u001b[2J"; }
    private String showHelp(String[] args, TerminalSession session) { return "Help: type 'man <command>'"; }
    private String showManual(String[] args, TerminalSession session) { return "man: not implemented"; }
    private String exitShell(String[] args, TerminalSession session) { return "exit"; }

    // ------------------ Redirection & Pipes ------------------
    private String redirectOutput(String[] args, TerminalSession session) { return ">"; }
    private String appendOutput(String[] args, TerminalSession session) { return ">>"; }
    private String redirectInput(String[] args, TerminalSession session) { return "<"; }
    private String pipeCommands(String[] args, TerminalSession session) { return "|"; }

    // =================== Setup ===================
    private void setupEnvironment() {
        environment.put("USER", "android-user");
        environment.put("HOME", context.getFilesDir().getAbsolutePath());
        environment.put("PWD", currentDirectory);
        environment.put("PATH", "/system/bin:/system/xbin:/vendor/bin");
        environment.put("TERM", "xterm-256color");
        environment.put("SHELL", "/system/bin/sh");
    }

    private void setupAliases() {
        aliases.put("ll", "ls -l");
        aliases.put("la", "ls -a");
        aliases.put("l", "ls -la");
        aliases.put("..", "cd ..");
        aliases.put("...", "cd ../..");
    }

    private String resolvePath(String path) {
        if (path.startsWith("/")) return path;
        return currentDirectory + "/" + path;
    }

    // =================== Getters ===================
    public String getCurrentDirectory() { return currentDirectory; }
    public Context getContext() { return context; }
    public Map<String, String> getEnvironment() { return environment; }
}
