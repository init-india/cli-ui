package com.cliui.linux;

import android.content.Context;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

public class TerminalSession {
    private Context context;
    private String currentDirectory;
    private Map<String, String> environment;
    private Map<String, String> aliases;
    private List<String> commandHistory;
    private CommandRegistry commandRegistry;
    
    public TerminalSession(Context context) {
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
    
    public String executeCommand(String command) {
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
        
        // Execute via registry
        CommandHandler handler = commandRegistry.getHandler(baseCommand);
        if (handler != null) {
            return handler.execute(args, this);
        }
        
        return "Command not found: " + baseCommand + "\nType 'help' for available commands";
    }
    
    // CORE COMMAND REGISTRATION
    private void registerCoreCommands() {
        // FILE OPERATIONS (Chapter 3,4)
        commandRegistry.register("ls", this::listFiles);
        commandRegistry.register("cd", this::changeDirectory);
        commandRegistry.register("pwd", this::printWorkingDirectory);
        commandRegistry.register("cp", this::copyFile);
        commandRegistry.register("mv", this::moveFile);
        commandRegistry.register("rm", this::deleteFile);
        commandRegistry.register("mkdir", this::createDirectory);
        commandRegistry.register("touch", this::createFile);
        commandRegistry.register("ln", this::createLink);
        
        // VIEWING FILES (Chapter 5)
        commandRegistry.register("cat", this::readFile);
        commandRegistry.register("less", this::pageFile);
        commandRegistry.register("head", this::showFileHead);
        commandRegistry.register("tail", this::showFileTail);
        commandRegistry.register("file", this::fileType);
        
        // MANIPULATING FILES (Chapter 6)
        commandRegistry.register("sort", this::sortLines);
        commandRegistry.register("uniq", this::uniqueLines);
        commandRegistry.register("wc", this::wordCount);
        commandRegistry.register("grep", this::searchInFiles);
        commandRegistry.register("cut", this::cutColumns);
        commandRegistry.register("paste", this::pasteFiles);
        
        // REDIRECTION (Chapter 7)
        commandRegistry.register(">", this::redirectOutput);
        commandRegistry.register(">>", this::appendOutput);
        commandRegistry.register("<", this::redirectInput);
        commandRegistry.register("|", this::pipeCommands);
        
        // PERMISSIONS (Chapter 9)
        commandRegistry.register("chmod", this::changePermissions);
        commandRegistry.register("chown", this::changeOwner);
        commandRegistry.register("su", this::switchUser);
        
        // PROCESSES (Chapter 10)
        commandRegistry.register("ps", this::showProcesses);
        commandRegistry.register("top", this::showTopProcesses);
        commandRegistry.register("kill", this::killProcess);
        commandRegistry.register("jobs", this::showJobs);
        commandRegistry.register("bg", this::backgroundJob);
        commandRegistry.register("fg", this::foregroundJob);
        
        // ENVIRONMENT (Chapter 11)
        commandRegistry.register("export", this::exportVariable);
        commandRegistry.register("alias", this::manageAliases);
        commandRegistry.register("history", this::showHistory);
        
        // NETWORKING (Chapter 13)
        commandRegistry.register("ping", this::pingHost);
        commandRegistry.register("ifconfig", this::networkInterfaces);
        commandRegistry.register("netstat", this::networkStatistics);
        
        // SEARCHING (Chapter 14)
        commandRegistry.register("find", this::findFiles);
        commandRegistry.register("locate", this::locateFile);
        commandRegistry.register("which", this::whichCommand);
        
        // ARCHIVING (Chapter 15)
        commandRegistry.register("tar", this::createArchive);
        commandRegistry.register("gzip", this::compressFile);
        commandRegistry.register("zip", this::createZip);
        commandRegistry.register("unzip", this::extractZip);
        
        // REGULAR EXPRESSIONS (Chapter 17)
        commandRegistry.register("sed", this::streamEdit);
        commandRegistry.register("awk", this::awkProcessing);
        
        // TEXT PROCESSING (Chapter 18)
        commandRegistry.register("tr", this::translateChars);
        commandRegistry.register("tee", this::teeOutput);
        
        // UTILITIES
        commandRegistry.register("echo", this::echoText);
        commandRegistry.register("date", this::currentDate);
        commandRegistry.register("cal", this::showCalendar);
        commandRegistry.register("whoami", this::currentUser);
        commandRegistry.register("uname", this::systemInfo);
        commandRegistry.register("df", this::diskUsage);
        commandRegistry.register("du", this::directoryUsage);
        commandRegistry.register("diff", this::compareFiles);
        commandRegistry.register("clear", this::clearScreen);
        commandRegistry.register("help", this::showHelp);
        commandRegistry.register("man", this::showManual);
        commandRegistry.register("exit", this::exitShell);
    }
    
    // IMPLEMENTATION OF KEY COMMANDS FROM THE BOOK
    
    // Chapter 3: Navigation
    private String listFiles(String[] args, TerminalSession session) {
        boolean longFormat = false;
        boolean showAll = false;
        String targetDir = session.currentDirectory;
        
        // Parse options (-l, -a, -la)
        for (String arg : args) {
            if (arg.startsWith("-")) {
                if (arg.contains("l")) longFormat = true;
                if (arg.contains("a")) showAll = true;
            } else {
                targetDir = session.resolvePath(arg);
            }
        }
        
        File dir = new File(targetDir);
        if (!dir.exists()) return "ls: cannot access '" + targetDir + "': No such directory";
        
        File[] files = dir.listFiles();
        if (files == null) return "ls: permission denied";
        
        Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        
        StringBuilder result = new StringBuilder();
        for (File file : files) {
            if (!showAll && file.getName().startsWith(".")) continue;
            
            if (longFormat) {
                result.append(String.format("%s %8s %s %s\n",
                    session.getPermissions(file),
                    session.formatSize(file.length()),
                    new SimpleDateFormat("MMM dd HH:mm").format(new Date(file.lastModified())),
                    file.getName()));
            } else {
                result.append(file.getName()).append("\n");
            }
        }
        return result.toString();
    }
    
    // Chapter 5: Viewing Files
    private String pageFile(String[] args, TerminalSession session) {
        if (args.length == 0) return "less: missing filename";
        
        File file = new File(session.resolvePath(args[0]));
        if (!file.exists()) return "less: " + args[0] + ": No such file";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 20) {
                content.append(line).append("\n");
                lineCount++;
            }
            if (line != null) {
                content.append("--- More --- (Press Enter for more, q to quit)");
            }
            return content.toString();
        } catch (IOException e) {
            return "less: cannot read file";
        }
    }
    
    // Chapter 6: Manipulating Files
    private String sortLines(String[] args, TerminalSession session) {
        if (args.length == 0) {
            // Sort stdin
            return "sort: reading from stdin not implemented";
        }
        
        File file = new File(session.resolvePath(args[0]));
        if (!file.exists()) return "sort: " + args[0] + ": No such file";
        
        try {
            List<String> lines = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
            
            Collections.sort(lines);
            return String.join("\n", lines);
        } catch (IOException e) {
            return "sort: cannot read file";
        }
    }
    
    private String uniqueLines(String[] args, TerminalSession session) {
        if (args.length == 0) return "uniq: missing filename";
        
        File file = new File(session.resolvePath(args[0]));
        if (!file.exists()) return "uniq: " + args[0] + ": No such file";
        
        try {
            Set<String> uniqueLines = new LinkedHashSet<>();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                uniqueLines.add(line);
            }
            reader.close();
            
            return String.join("\n", uniqueLines);
        } catch (IOException e) {
            return "uniq: cannot read file";
        }
    }
    
    private String wordCount(String[] args, TerminalSession session) {
        if (args.length == 0) return "wc: missing filename";
        
        File file = new File(session.resolvePath(args[0]));
        if (!file.exists()) return "wc: " + args[0] + ": No such file";
        
        try {
            int lines = 0, words = 0, chars = 0;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                lines++;
                words += line.split("\\s+").length;
                chars += line.length() + 1; // +1 for newline
            }
            reader.close();
            
            return String.format("%d %d %d %s", lines, words, chars, args[0]);
        } catch (IOException e) {
            return "wc: cannot read file";
        }
    }
    
    // Chapter 7: Redirection
    private String redirectOutput(String[] args, TerminalSession session) {
        if (args.length < 2) return "syntax: command > file";
        // This would be handled by command parser for real redirection
        return "Redirect output to: " + args[1];
    }
    
    // Chapter 9: Permissions
    private String changePermissions(String[] args, TerminalSession session) {
        if (args.length < 2) return "chmod: missing mode and file";
        
        String mode = args[0];
        File file = new File(session.resolvePath(args[1]));
        if (!file.exists()) return "chmod: cannot access '" + args[1] + "': No such file";
        
        return "chmod: changed permissions of '" + args[1] + "' to " + mode;
    }
    
    // Chapter 10: Processes
    private String showProcesses(String[] args, TerminalSession session) {
        return "PID\tTTY\tTIME\tCMD\n" +
               "1\t?\t00:00:01\tinit\n" +
               "2\t?\t00:00:00\tkthreadd\n" +
               "3\t?\t00:00:00\tksoftirqd/0\n" +
               "... (process list simulated)";
    }
    
    // Chapter 13: Networking
    private String pingHost(String[] args, TerminalSession session) {
        if (args.length == 0) return "ping: missing host";
        return "PING " + args[0] + " (127.0.0.1): 56 data bytes\n" +
               "64 bytes from 127.0.0.1: icmp_seq=0 ttl=64 time=0.100 ms\n" +
               "64 bytes from 127.0.0.1: icmp_seq=1 ttl=64 time=0.150 ms\n" +
               "\n--- " + args[0] + " ping statistics ---\n" +
               "2 packets transmitted, 2 packets received, 0.0% packet loss";
    }
    
    // Chapter 14: Searching
    private String locateFile(String[] args, TerminalSession session) {
        if (args.length == 0) return "locate: missing pattern";
        
        List<String> results = new ArrayList<>();
        searchAllFiles(new File("/"), args[0], results);
        
        if (results.isEmpty()) {
            return "No files found matching pattern: " + args[0];
        }
        
        return String.join("\n", results.subList(0, Math.min(results.size(), 20)));
    }
    
    // Chapter 17: Regular Expressions
    private String streamEdit(String[] args, TerminalSession session) {
        if (args.length < 2) return "sed: missing expression and file";
        
        String expression = args[0];
        File file = new File(session.resolvePath(args[1]));
        if (!file.exists()) return "sed: " + args[1] + ": No such file";
        
        // Basic s/foo/bar/ replacement
        if (expression.startsWith("s/") && expression.endsWith("/")) {
            String[] parts = expression.substring(2, expression.length() - 1).split("/");
            if (parts.length == 2) {
                try {
                    String content = session.readFileContent(file);
                    content = content.replaceAll(parts[0], parts[1]);
                    return content;
                } catch (IOException e) {
                    return "sed: cannot read file";
                }
            }
        }
        
        return "sed: complex expressions not implemented";
    }
    
    // UTILITY METHODS
    private String resolvePath(String path) {
        if (path.startsWith("/")) return path;
        if (path.equals("..")) {
            File parent = new File(currentDirectory).getParentFile();
            return parent != null ? parent.getAbsolutePath() : currentDirectory;
        }
        if (path.equals(".")) return currentDirectory;
        return currentDirectory + File.separator + path;
    }
    
    private String getPermissions(File file) {
        return file.isDirectory() ? "drwxr-xr-x" : "-rw-r--r--";
    }
    
    private String formatSize(long size) {
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return (size / 1024) + "K";
        return (size / (1024 * 1024)) + "M";
    }
    
    private void searchAllFiles(File dir, String pattern, List<String> results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.getName().toLowerCase().contains(pattern.toLowerCase())) {
                results.add(file.getAbsolutePath());
            }
            if (file.isDirectory() && results.size() < 100) {
                searchAllFiles(file, pattern, results);
            }
        }
    }
    
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        return content.toString();
    }
    
    // PLACEHOLDER IMPLEMENTATIONS
    private String changeDirectory(String[] args, TerminalSession session) {
        if (args.length == 0) {
            session.currentDirectory = session.context.getFilesDir().getAbsolutePath();
            return "";
        }
        String newPath = session.resolvePath(args[0]);
        session.currentDirectory = newPath;
        return "";
    }
    
    private String printWorkingDirectory(String[] args, TerminalSession session) {
        return session.currentDirectory;
    }
    
    private String copyFile(String[] args, TerminalSession session) { 
        return "cp: copy files"; 
    }
    
    private String showFileHead(String[] args, TerminalSession session) { 
        return "head: first 10 lines"; 
    }
    
    private String showHelp(String[] args, TerminalSession session) {
        return "Essential Linux Commands (from 'The Linux Command Line'):\n\n" +
               "NAVIGATION: ls, cd, pwd\n" +
               "FILE OPS: cp, mv, rm, mkdir, touch, ln\n" +
               "VIEWING: cat, less, head, tail, file\n" +
               "PROCESSING: sort, uniq, wc, grep, cut, paste\n" +
               "PERMISSIONS: chmod, chown\n" +
               "PROCESSES: ps, top, kill, jobs\n" +
               "NETWORKING: ping, ifconfig, netstat\n" +
               "SEARCHING: find, locate, which\n" +
               "ARCHIVING: tar, gzip, zip, unzip\n" +
               "REGEX: sed, awk\n" +
               "TEXT: tr, tee\n" +
               "UTILITIES: echo, date, cal, whoami, df, du\n\n" +
               "Type 'man [command]' for detailed help";
    }
    
    // ... Add other placeholder implementations
    
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
    
    // Getters for command handlers
    public String getCurrentDirectory() { return currentDirectory; }
    public Context getContext() { return context; }
    public Map<String, String> getEnvironment() { return environment; }
}
