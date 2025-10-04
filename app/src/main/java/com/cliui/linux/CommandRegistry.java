package com.cliui.linux;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    public interface CommandHandler {
        String execute(String[] args, TerminalSession session);
    }
    
    private Map<String, CommandHandler> handlers;
    
    public CommandRegistry() {
        this.handlers = new HashMap<>();
    }
    
    public void register(String command, CommandHandler handler) {
        handlers.put(command, handler);
    }
    
    public CommandHandler getHandler(String command) {
        return handlers.get(command);
    }
    
    public boolean isRegistered(String command) {
        return handlers.containsKey(command);
    }
}
