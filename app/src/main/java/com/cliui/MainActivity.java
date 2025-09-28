package com.cliui;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private TextView terminalOutput;
    private EditText commandInput;
    private ScrollView terminalScroll;
    private CommandParser commandParser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeComponents();
        setupCommandParser();
        showWelcomeMessage();
    }
    
    private void initializeComponents() {
        terminalOutput = findViewById(R.id.terminalOutput);
        commandInput = findViewById(R.id.commandInput);
        terminalScroll = findViewById(R.id.terminalScroll);
        
        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                processCommand();
                return true;
            }
            return false;
        });
    }
    
    private void setupCommandParser() {
        commandParser = new CommandParser(this);
    }
    
    private void showWelcomeMessage() {
        appendToTerminal("SmartCLI v2.0 - F-Droid Edition", "#00FF00");
        appendToTerminal("Type 'help' for commands", "#FFFFFF");
        appendToTerminal("cli> ", "#00FF00");
    }
    
    private void processCommand() {
        String command = commandInput.getText().toString().trim();
        if (!command.isEmpty()) {
            appendToTerminal("cli> " + command, "#00FF00");
            String result = commandParser.parse(command);
            
            if ("CLEAR_SCREEN".equals(result)) {
                clearTerminal();
            } else {
                appendToTerminal(result, "#FFFFFF");
                appendToTerminal("cli> ", "#00FF00");
            }
            
            commandInput.setText("");
            scrollToBottom();
        }
    }
    
    private void appendToTerminal(String text, String color) {
        String html = "<font color='" + color + "'>" + text + "</font><br>";
        terminalOutput.append(android.text.Html.fromHtml(html));
    }
    
    private void clearTerminal() {
        terminalOutput.setText("");
        appendToTerminal("Terminal cleared", "#FFFFFF");
        appendToTerminal("cli> ", "#00FF00");
    }
    
    private void scrollToBottom() {
        terminalScroll.post(() -> terminalScroll.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
