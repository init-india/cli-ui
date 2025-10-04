package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.database.Cursor;
import android.provider.CallLog;
import android.os.Handler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import com.cliui.utils.PermissionManager;

public class CallModule implements CommandModule {
    private Context context;
    private TelephonyManager telephonyManager;
    private PermissionManager permissionManager;
    
    // Call state management
    private CallState currentState = CallState.IDLE;
    private List<ActiveCall> activeCalls = new ArrayList<>();
    private String incomingCallNumber = null;
    private String incomingCallName = null;
    
    // Call history cache
    private List<CallHistoryItem> callHistory = new ArrayList<>();
    private Handler callHandler = new Handler();
    
    public CallModule(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        this.permissionManager = PermissionManager.getInstance(context);
        loadCallHistory();
        setupCallStateListener();
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return "Usage: call [number|history|command]";
        
        String command = tokens[0].toLowerCase();
        
        // Handle different call states
        switch (currentState) {
            case RINGING:
                return handleRingingState(tokens);
            case IN_CALL:
            case MULTIPLE_CALLS:
            case CONFERENCE:
                return handleActiveCallState(tokens);
            case IDLE:
            default:
                return handleIdleState(tokens);
        }
    }
    
    private String handleIdleState(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        if (tokens.length == 1) {
            if (command.equals("call")) {
                return displayCallHistory();
            }
        }
        
        if (tokens.length == 2) {
            String param = tokens[1];
            
            if (command.equals("call")) {
                // call <number> or call <contact>
                if (isNumeric(param)) {
                    return makeCall(param);
                } else {
                    return makeCallToContact(param);
                }
            }
        }
        
        if (tokens.length >= 2) {
            // call history for specific contact
            if (command.equals("call")) {
                return displayCallHistoryForContact(tokens[1]);
            }
        }
        
        return "Usage: call [number] | call [contact] | call history [contact]";
    }
    
    private String handleRingingState(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "ans":
            case "answer":
                return answerCall();
            case "rej":
            case "reject":
                return rejectCall();
            default:
                return "📞 Incoming: " + incomingCallName + " (" + incomingCallNumber + ")\n" +
                       "Type: ans (answer) | rej (reject)";
        }
    }
    
    private String handleActiveCallState(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "end":
                return endCall();
            case "hold":
                return holdCall();
            case "unhold":
                return unholdCall();
            case "mute":
                return muteCall();
            case "unmute":
                return unmuteCall();
            case "speakeron":
                return setSpeakerOn();
            case "speakeroff":
                return setSpeakerOff();
            case "merge":
                return mergeCalls();
            case "drop":
                if (tokens.length >= 2) {
                    return dropParticipant(tokens[1]);
                } else {
                    return "Usage: drop [number|contact]";
                }
            case "call":
                if (tokens.length >= 2) {
                    return makeAdditionalCall(tokens[1]);
                } else {
                    return "Usage: call [number] to add another call";
                }
            case "status":
                return getCallStatus();
            default:
                return getActiveCallSuggestions();
        }
    }
    
    private String makeCall(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
            // Update state
            currentState = CallState.IN_CALL;
            activeCalls.add(new ActiveCall(number, getContactName(number)));
            
            return "📞 Calling: " + number + "\n" + getInCallSuggestions();
            
        } catch (SecurityException e) {
            return "❌ Call permission required";
        } catch (Exception e) {
            return "❌ Call failed: " + e.getMessage();
        }
    }
    
    private String makeCallToContact(String contactName) {
        String number = getPhoneNumber(contactName);
        if (number != null) {
            return makeCall(number);
        } else {
            return "❌ Contact not found: " + contactName;
        }
    }
    
    private String answerCall() {
        if (incomingCallNumber == null) {
            return "❌ No incoming call to answer";
        }
        
        // Check if Shizuku is available for advanced call control
        if (isShizukuAvailable()) {
            if (executeShizukuCommand("input keyevent KEYCODE_CALL")) {
                currentState = CallState.IN_CALL;
                activeCalls.add(new ActiveCall(incomingCallNumber, incomingCallName));
                incomingCallNumber = null;
                incomingCallName = null;
                return "✅ Call answered\n" + getInCallSuggestions();
            }
        }
        
        // Fallback: Use system dialer intent
        Intent intent = new Intent(Intent.ACTION_ANSWER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            currentState = CallState.IN_CALL;
            activeCalls.add(new ActiveCall(incomingCallNumber, incomingCallName));
            incomingCallNumber = null;
            incomingCallName = null;
            return "✅ Call answered\n" + getInCallSuggestions();
        } catch (Exception e) {
            return "❌ Failed to answer call. Shizuku recommended for direct control.";
        }
    }
    
    private String rejectCall() {
        if (incomingCallNumber == null) {
            return "❌ No incoming call to reject";
        }
        
        // Use Shizuku for direct reject
        if (isShizukuAvailable()) {
            if (executeShizukuCommand("input keyevent KEYCODE_ENDCALL")) {
                addToCallHistory(incomingCallNumber, incomingCallName, "rejected");
                incomingCallNumber = null;
                incomingCallName = null;
                currentState = CallState.IDLE;
                return "✅ Call rejected";
            }
        }
        
        // Fallback: End call to reject
        if (executeShizukuCommand("input keyevent KEYCODE_ENDCALL")) {
            addToCallHistory(incomingCallNumber, incomingCallName, "rejected");
            incomingCallNumber = null;
            incomingCallName = null;
            currentState = CallState.IDLE;
            return "✅ Call rejected";
        } else {
            return "❌ Failed to reject call";
        }
    }
    
    private String endCall() {
        if (executeShizukuCommand("input keyevent KEYCODE_ENDCALL")) {
            // Log ended calls to history
            for (ActiveCall call : activeCalls) {
                addToCallHistory(call.number, call.name, "ended");
            }
            
            activeCalls.clear();
            currentState = CallState.IDLE;
            return "✅ Call ended";
        } else {
            return "❌ Failed to end call";
        }
    }
    
    private String holdCall() {
        if (executeShizukuCommand("service call phone 5 i32 1")) {
            return "✅ Call on hold";
        } else {
            return "❌ Hold failed. Shizuku required for call control.";
        }
    }
    
    private String unholdCall() {
        if (executeShizukuCommand("service call phone 5 i32 0")) {
            return "✅ Call resumed";
        } else {
            return "❌ Unhold failed. Shizuku required for call control.";
        }
    }
    
    private String muteCall() {
        if (executeShizukuCommand("input keyevent KEYCODE_MUTE")) {
            return "✅ Microphone muted";
        } else {
            return "❌ Mute failed. Shizuku required for call control.";
        }
    }
    
    private String unmuteCall() {
        if (executeShizukuCommand("input keyevent KEYCODE_MUTE")) {
            return "✅ Microphone unmuted";
        } else {
            return "❌ Unmute failed. Shizuku required for call control.";
        }
    }
    
    private String setSpeakerOn() {
        if (executeShizukuCommand("media volume --stream 0 --set 15")) {
            return "✅ Speaker on";
        } else {
            return "❌ Speaker control failed. Shizuku required.";
        }
    }
    
    private String setSpeakerOff() {
        if (executeShizukuCommand("media volume --stream 0 --set 5")) {
            return "✅ Speaker off";
        } else {
            return "❌ Speaker control failed. Shizuku required.";
        }
    }
    
    private String mergeCalls() {
        if (activeCalls.size() < 2) {
            return "❌ Need at least 2 active calls to merge";
        }
        
        if (executeShizukuCommand("service call phone 16 i32 1")) {
            currentState = CallState.CONFERENCE;
            return "✅ Calls merged - Conference active\n" + getConferenceSuggestions();
        } else {
            return "❌ Merge failed. Shizuku required for conference calls.";
        }
    }
    
    private String dropParticipant(String target) {
        if (executeShizukuCommand("service call phone 17 i32 1")) {
            return "✅ Participant dropped: " + target;
        } else {
            return "❌ Drop failed. Shizuku required for conference control.";
        }
    }
    
    private String makeAdditionalCall(String number) {
        String result = makeCall(number);
        if (result.contains("Calling:")) {
            currentState = CallState.MULTIPLE_CALLS;
            return result + "\n💡 Type 'merge' to combine calls";
        }
        return result;
    }
    
    // Shizuku integration methods
    private boolean isShizukuAvailable() {
        // Use PermissionManager to check Shizuku availability
        return permissionManager.canExecute("shizuku test"); // Placeholder
    }
    
    private boolean executeShizukuCommand(String command) {
        // This would integrate with your PermissionManager's Shizuku execution
        // For now, return true for simulation
        return true;
    }
    
    private String getCallStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== Call Status ===\n");
        status.append("State: ").append(currentState).append("\n");
        status.append("Active Calls: ").append(activeCalls.size()).append("\n");
        
        for (int i = 0; i < activeCalls.size(); i++) {
            status.append(i+1).append(". ").append(activeCalls.get(i).name)
                  .append(" (").append(activeCalls.get(i).number).append(")\n");
        }
        
        status.append("\nShizuku: ").append(isShizukuAvailable() ? "Available ✅" : "Not Available ❌");
        
        return status.toString();
    }
    
    private String getInCallSuggestions() {
        String shizukuStatus = isShizukuAvailable() ? "✅" : "❌ (Shizuku required)";
        return "\n💡 Available commands:\n" +
               "hold " + shizukuStatus + " | mute " + shizukuStatus + " | speakeron " + shizukuStatus + " \n" +
               "call [number] (add call) | end (hang up) | status (call info)";
    }
    
    private String getConferenceSuggestions() {
        String shizukuStatus = isShizukuAvailable() ? "✅" : "❌ (Shizuku required)";
        return "\n💡 Conference commands " + shizukuStatus + ":\n" +
               "drop [number] (remove) | mute (mute all) | hold (hold all) \n" +
               "call [number] (add more) | end (end conference)";
    }
    
    private String getActiveCallSuggestions() {
        switch (currentState) {
            case IN_CALL:
                return getInCallSuggestions();
            case MULTIPLE_CALLS:
                return "💡 Multiple calls active. Type 'merge' to combine or 'status' for details";
            case CONFERENCE:
                return getConferenceSuggestions();
            default:
                return "";
        }
    }
    
    // Call history methods (keep your existing implementation)
    private String displayCallHistory() {
        StringBuilder history = new StringBuilder();
        history.append("📞 Recent Calls:\n");
        
        for (int i = 0; i < Math.min(callHistory.size(), 10); i++) {
            CallHistoryItem item = callHistory.get(i);
            history.append(i+1).append(". ").append(item.display()).append("\n");
        }
        
        if (callHistory.size() > 10) {
            history.append("\nType 'call history [contact]' for specific history");
        }
        
        return history.toString();
    }
    
    private String displayCallHistoryForContact(String contact) {
        StringBuilder history = new StringBuilder();
        history.append("📞 Calls with ").append(contact).append(":\n");
        
        int count = 0;
        for (CallHistoryItem item : callHistory) {
            if (item.name.toLowerCase().contains(contact.toLowerCase()) || 
                item.number.contains(contact)) {
                history.append("• ").append(item.display()).append("\n");
                count++;
            }
        }
        
        if (count == 0) {
            return "❌ No call history found for: " + contact;
        }
        
        return history.toString();
    }
    
    private void loadCallHistory() {
        // Load real call history from CallLog
        callHistory.clear();
        // Implementation for real call log access
    }
    
    private void addToCallHistory(String number, String name, String status) {
        CallHistoryItem item = new CallHistoryItem(number, name, status, new Date());
        callHistory.add(0, item); // Add to beginning
    }
    
    // Utility methods (keep your existing implementation)
    private boolean isNumeric(String str) {
        return str.matches("\\d+");
    }
    
    private String getContactName(String number) {
        return number; // Fallback to number if no name found
    }
    
    private String getPhoneNumber(String contactName) {
        return null; // Would query contacts database
    }
    
    private void setupCallStateListener() {
        // Setup TelephonyManager listener for incoming calls
    }
    
    // Inner classes (keep your existing implementation)
    enum CallState {
        IDLE, RINGING, IN_CALL, MULTIPLE_CALLS, CONFERENCE
    }
    
    class ActiveCall {
        String number;
        String name;
        
        ActiveCall(String number, String name) {
            this.number = number;
            this.name = name;
        }
    }
    
    class CallHistoryItem {
        String number;
        String name;
        String status;
        Date timestamp;
        
        CallHistoryItem(String number, String name, String status, Date timestamp) {
            this.number = number;
            this.name = name;
            this.status = status;
            this.timestamp = timestamp;
        }
        
        String display() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy;HH:mm");
            return name + " (" + number + ") - " + sdf.format(timestamp) + " - " + status;
        }
    }
}
