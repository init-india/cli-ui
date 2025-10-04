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
import com.cliui.utils.ShizukuManager;

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
        if (tokens.length == 0) return getCallUsage();
        
        String command = tokens[0].toLowerCase();
        String fullCommand = String.join(" ", tokens).toLowerCase();

        // Check permissions using PermissionManager
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

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
            if (command.equals("call") && tokens[1].equals("history")) {
                if (tokens.length >= 3) {
                    return displayCallHistoryForContact(tokens[2]);
                } else {
                    return displayCallHistory();
                }
            }
        }
        
        return getCallUsage();
    }
    
    private String handleRingingState(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        // Check permissions for call control
        if (!permissionManager.canExecute(command)) {
            return permissionManager.getPermissionExplanation(command);
        }

        switch (command) {
            case "ans":
            case "answer":
                return answerCall();
            case "rej":
            case "reject":
                return rejectCall();
            case "ignore":
                return ignoreCall();
            default:
                return "📞 Incoming: " + (incomingCallName != null ? incomingCallName : "Unknown") + 
                       " (" + incomingCallNumber + ")\n" +
                       "Type: ans (answer) | rej (reject) | ignore";
        }
    }
    
    private String handleActiveCallState(String[] tokens) {
        String command = tokens[0].toLowerCase();
        String fullCommand = String.join(" ", tokens).toLowerCase();
        
        // Check permissions for call control
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

        switch (command) {
            case "end":
            case "hangup":
                return endCall();
            case "hold":
                return holdCall();
            case "unhold":
            case "resume":
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
            case "swap":
                return swapCalls();
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
        // Check if we have call permissions
        if (!permissionManager.canMakeCalls()) {
            return "❌ Call permission required\n" +
                   "This app needs phone call permissions to make calls";
        }

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
            return "❌ Call permission denied\n" +
                   "Please grant phone call permissions in app settings";
        } catch (Exception e) {
            return "❌ Call failed: " + e.getMessage();
        }
    }
    
    private String makeCallToContact(String contactName) {
        // Check contact permissions
        if (!permissionManager.areAllPermissionsGranted(PermissionManager.CONTACT_PERMISSIONS)) {
            return "❌ Contact permission required\n" +
                   "Need contact access to find phone numbers";
        }

        String number = getPhoneNumber(contactName);
        if (number != null) {
            return makeCall(number);
        } else {
            return "❌ Contact not found: " + contactName + "\n" +
                   "💡 Make sure contacts are synced and accessible";
        }
    }
    
    private String answerCall() {
        if (incomingCallNumber == null) {
            return "❌ No incoming call to answer";
        }
        
        // Check if Shizuku is available for advanced call control
        if (ShizukuManager.isAvailable()) {
            if (ShizukuManager.executeCommand("input keyevent KEYCODE_CALL")) {
                currentState = CallState.IN_CALL;
                activeCalls.add(new ActiveCall(incomingCallNumber, incomingCallName));
                addToCallHistory(incomingCallNumber, incomingCallName, "answered");
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
            addToCallHistory(incomingCallNumber, incomingCallName, "answered");
            incomingCallNumber = null;
            incomingCallName = null;
            return "✅ Call answered\n" + getInCallSuggestions();
        } catch (Exception e) {
            return "❌ Failed to answer call\n" +
                   "💡 Shizuku recommended for direct call control";
        }
    }
    
    private String rejectCall() {
        if (incomingCallNumber == null) {
            return "❌ No incoming call to reject";
        }
        
        // Use Shizuku for direct reject
        if (ShizukuManager.isAvailable()) {
            if (ShizukuManager.executeCommand("input keyevent KEYCODE_ENDCALL")) {
                addToCallHistory(incomingCallNumber, incomingCallName, "rejected");
                incomingCallNumber = null;
                incomingCallName = null;
                currentState = CallState.IDLE;
                return "✅ Call rejected";
            }
        }
        
        // Fallback: End call to reject
        if (endCall().contains("✅")) {
            addToCallHistory(incomingCallNumber, incomingCallName, "rejected");
            incomingCallNumber = null;
            incomingCallName = null;
            return "✅ Call rejected";
        } else {
            return "❌ Failed to reject call\n" +
                   "💡 Shizuku required for reliable call rejection";
        }
    }
    
    private String ignoreCall() {
        if (incomingCallNumber == null) {
            return "❌ No incoming call to ignore";
        }
        
        addToCallHistory(incomingCallNumber, incomingCallName, "ignored");
        incomingCallNumber = null;
        incomingCallName = null;
        currentState = CallState.IDLE;
        return "✅ Call ignored (sent to voicemail)";
    }
    
    private String endCall() {
        if (ShizukuManager.isAvailable()) {
            if (ShizukuManager.executeCommand("input keyevent KEYCODE_ENDCALL")) {
                // Log ended calls to history
                for (ActiveCall call : activeCalls) {
                    addToCallHistory(call.number, call.name, "ended");
                }
                
                activeCalls.clear();
                currentState = CallState.IDLE;
                return "✅ Call ended";
            }
        }
        
        return "❌ Failed to end call\n" +
               "💡 Shizuku required for direct call control\n" +
               "🔧 Use phone hardware or system dialer instead";
    }
    
    private String holdCall() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Call hold requires system-level access via Shizuku";
        }
        
        if (ShizukuManager.executeCommand("service call phone 5 i32 1")) {
            return "✅ Call on hold";
        } else {
            return "❌ Hold failed\n" +
                   "💡 This device may not support direct call control";
        }
    }
    
    private String unholdCall() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Call unhold requires system-level access via Shizuku";
        }
        
        if (ShizukuManager.executeCommand("service call phone 5 i32 0")) {
            return "✅ Call resumed";
        } else {
            return "❌ Unhold failed\n" +
                   "💡 This device may not support direct call control";
        }
    }
    
    private String muteCall() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Call mute requires system-level access via Shizuku";
        }
        
        if (ShizukuManager.executeCommand("input keyevent KEYCODE_MUTE")) {
            return "✅ Microphone muted";
        } else {
            return "❌ Mute failed";
        }
    }
    
    private String unmuteCall() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Call unmute requires system-level access via Shizuku";
        }
        
        if (ShizukuManager.executeCommand("input keyevent KEYCODE_MUTE")) {
            return "✅ Microphone unmuted";
        } else {
            return "❌ Unmute failed";
        }
    }
    
    private String setSpeakerOn() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Speaker control requires system-level access via Shizuku";
        }
        
        if (ShizukuManager.executeCommand("media volume --stream 0 --set 15")) {
            return "✅ Speaker on";
        } else {
            return "❌ Speaker control failed";
        }
    }
    
    private String setSpeakerOff() {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Speaker control requires system-level access via Shizuku";
        }
        
        if (ShizukuManager.executeCommand("media volume --stream 0 --set 5")) {
            return "✅ Speaker off";
        } else {
            return "❌ Speaker control failed";
        }
    }
    
    private String mergeCalls() {
        if (activeCalls.size() < 2) {
            return "❌ Need at least 2 active calls to merge";
        }
        
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Call merge requires system-level access via Shizuku";
        }
        
        if (ShizukuManager.executeCommand("service call phone 16 i32 1")) {
            currentState = CallState.CONFERENCE;
            return "✅ Calls merged - Conference active\n" + getConferenceSuggestions();
        } else {
            return "❌ Merge failed\n" +
                   "💡 This device may not support conference calls";
        }
    }
    
    private String swapCalls() {
        if (activeCalls.size() < 2) {
            return "❌ Need 2 active calls to swap";
        }
        
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Call swap requires system-level access via Shizuku";
        }
        
        if (ShizukuManager.executeCommand("service call phone 16 i32 2")) {
            return "✅ Calls swapped";
        } else {
            return "❌ Swap failed";
        }
    }
    
    private String dropParticipant(String target) {
        if (!ShizukuManager.isAvailable()) {
            return "❌ Shizuku not available\n" +
                   "Conference control requires system-level access via Shizuku";
        }
        
        if (ShizukuManager.executeCommand("service call phone 17 i32 1")) {
            return "✅ Participant dropped: " + target;
        } else {
            return "❌ Drop failed\n" +
                   "💡 This device may not support conference control";
        }
    }
    
    private String makeAdditionalCall(String number) {
        // Check if we're already in a call that supports adding more
        if (currentState != CallState.IN_CALL && currentState != CallState.MULTIPLE_CALLS) {
            return "❌ No active call to add to";
        }
        
        String result = makeCall(number);
        if (result.contains("Calling:")) {
            currentState = CallState.MULTIPLE_CALLS;
            return result + "\n💡 Type 'merge' to combine calls or 'swap' to switch";
        }
        return result;
    }
    
    private String getCallStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== 📞 Call Status ===\n");
        status.append("State: ").append(currentState).append("\n");
        status.append("Active Calls: ").append(activeCalls.size()).append("\n");
        
        for (int i = 0; i < activeCalls.size(); i++) {
            ActiveCall call = activeCalls.get(i);
            status.append(i+1).append(". ").append(call.name)
                  .append(" (").append(call.number).append(")\n");
        }
        
        status.append("\nPermissions:\n");
        status.append("• Phone: ").append(permissionManager.canMakeCalls() ? "✅" : "❌").append("\n");
        status.append("• Contacts: ").append(permissionManager.areAllPermissionsGranted(PermissionManager.CONTACT_PERMISSIONS) ? "✅" : "❌").append("\n");
        status.append("• Shizuku: ").append(ShizukuManager.isAvailable() ? "✅" : "❌");
        
        if (ShizukuManager.isAvailable()) {
            status.append("\n\n💡 Advanced controls available via Shizuku");
        } else {
            status.append("\n\n⚠️  Install Shizuku for call control features");
        }
        
        return status.toString();
    }
    
    private String getCallUsage() {
        return "📞 Call Module Usage:\n" +
               "• call [number]          - Make a call\n" +
               "• call [contact]         - Call a contact\n" +
               "• call history           - Show call history\n" +
               "• call history [name]    - History for specific contact\n" +
               "\n📞 During calls:\n" +
               "• end/hangup            - End current call\n" +
               "• hold/unhold           - Hold/resume call" + (ShizukuManager.isAvailable() ? " ✅" : " ❌") + "\n" +
               "• mute/unmute           - Mute controls" + (ShizukuManager.isAvailable() ? " ✅" : " ❌") + "\n" +
               "• speakeron/speakeroff  - Speaker toggle" + (ShizukuManager.isAvailable() ? " ✅" : " ❌") + "\n" +
               "• merge/swap            - Multi-call controls" + (ShizukuManager.isAvailable() ? " ✅" : " ❌") + "\n" +
               "\n🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available" : "Not Available");
    }
    
    private String getInCallSuggestions() {
        String shizukuStatus = ShizukuManager.isAvailable() ? "✅" : "❌ (Shizuku required)";
        return "\n💡 Available commands:\n" +
               "end | hold " + shizukuStatus + " | mute " + shizukuStatus + " | speakeron " + shizukuStatus + "\n" +
               "call [number] (add call) | status (call info)";
    }
    
    private String getConferenceSuggestions() {
        String shizukuStatus = ShizukuManager.isAvailable() ? "✅" : "❌ (Shizuku required)";
        return "\n💡 Conference commands " + shizukuStatus + ":\n" +
               "drop [number] | mute | hold | swap | end";
    }
    
    private String getActiveCallSuggestions() {
        switch (currentState) {
            case IN_CALL:
                return getInCallSuggestions();
            case MULTIPLE_CALLS:
                return "💡 Multiple calls active. Type 'merge' to combine, 'swap' to switch, or 'status' for details";
            case CONFERENCE:
                return getConferenceSuggestions();
            default:
                return getCallUsage();
        }
    }
    
    // Call history methods
    private String displayCallHistory() {
        if (!permissionManager.areAllPermissionsGranted(PermissionManager.PHONE_PERMISSIONS)) {
            return "❌ Phone permission required\n" +
                   "Need call log access to show call history";
        }

        StringBuilder history = new StringBuilder();
        history.append("📞 Recent Calls:\n");
        
        if (callHistory.isEmpty()) {
            history.append("No call history found\n");
            history.append("💡 Make some calls or check permissions");
        } else {
            for (int i = 0; i < Math.min(callHistory.size(), 10); i++) {
                CallHistoryItem item = callHistory.get(i);
                history.append(i+1).append(". ").append(item.display()).append("\n");
            }
        }
        
        if (callHistory.size() > 10) {
            history.append("\n💡 Type 'call history [contact]' for specific history");
        }
        
        return history.toString();
    }
    
    private String displayCallHistoryForContact(String contact) {
        if (!permissionManager.areAllPermissionsGranted(PermissionManager.PHONE_PERMISSIONS)) {
            return "❌ Phone permission required\n" +
                   "Need call log access to show call history";
        }

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
        // Load real call history from CallLog if permissions are granted
        if (permissionManager.areAllPermissionsGranted(PermissionManager.PHONE_PERMISSIONS)) {
            // Implementation for real call log access would go here
            callHistory.clear();
            // Add some sample data for demonstration
            callHistory.add(new CallHistoryItem("+1234567890", "John Doe", "outgoing", new Date()));
            callHistory.add(new CallHistoryItem("+0987654321", "Jane Smith", "incoming", new Date(System.currentTimeMillis() - 3600000)));
        }
    }
    
    private void addToCallHistory(String number, String name, String status) {
        CallHistoryItem item = new CallHistoryItem(number, name, status, new Date());
        callHistory.add(0, item); // Add to beginning
    }
    
    // Utility methods
    private boolean isNumeric(String str) {
        return str.matches("\\d+");
    }
    
    private String getContactName(String number) {
        // In real implementation, query contacts database
        return number; // Fallback to number if no name found
    }
    
    private String getPhoneNumber(String contactName) {
        // In real implementation, query contacts database
        // Return null if not found
        return null;
    }
    
    private void setupCallStateListener() {
        // Setup TelephonyManager listener for incoming calls
        // This would require additional permissions and setup
    }
    
    // Inner classes
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
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm");
            String statusIcon = "📞";
            if ("incoming".equals(status)) statusIcon = "📥";
            if ("outgoing".equals(status)) statusIcon = "📤";
            if ("missed".equals(status)) statusIcon = "❌";
            if ("rejected".equals(status)) statusIcon = "🚫";
            
            return statusIcon + " " + name + " (" + number + ") - " + sdf.format(timestamp);
        }
    }
}
