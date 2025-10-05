package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.location.Location;
import android.location.LocationManager;
import android.speech.tts.TextToSpeech;
import android.os.Handler;
import android.os.Looper;
import com.cliui.utils.PermissionManager;
import com.cliui.utils.ShizukuManager;

import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NavigationModule implements CommandModule {
    private Context context;
    private PermissionManager permissionManager;
    private TextToSpeech textToSpeech;
    private LocationManager locationManager;
    
    // Navigation state management
    private NavigationState currentState = NavigationState.IDLE;
    private List<LocationSearch> recentSearches = new ArrayList<>();
    private List<LocationSuggestion> currentSuggestions = new ArrayList<>();
    private Route currentRoute;
    private Location currentLocation;
    private String pendingDestination;
    private String pendingSource;
    private boolean voiceEnabled = true;
    
    // Navigation tracking
    private Handler navigationHandler = new Handler(Looper.getMainLooper());
    private ScheduledExecutorService navigationScheduler;
    private int currentStep = 0;
    private boolean navigationPaused = false;
    
    // Constants
    private static final int SUGGESTION_LIMIT = 5;
    private static final int RECENT_SEARCHES_LIMIT = 10;
    private static final String OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";
    
    public NavigationModule(Context context) {
        this.context = context;
        this.permissionManager = PermissionManager.getInstance(context);
        initializeTextToSpeech();
        loadRecentSearches();
        setupLocationUpdates();
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 0) return getUsage();
        
        String command = tokens[0].toLowerCase();
        String fullCommand = String.join(" ", tokens).toLowerCase();

        // Check permissions using PermissionManager
        if (!permissionManager.canExecute(fullCommand)) {
            return permissionManager.getPermissionExplanation(fullCommand);
        }

        // Require authentication for navigation access
       PermissionManager permissionManager = PermissionManager.getInstance(context);
if (!permissionManager.authenticate("navigation_access")) {
            return "🔒 Authentication required for navigation\n" +
                   "Please authenticate to use navigation features";
        }
        
        // Handle exit command in any state
        if (command.equals("exit")) {
            return handleExit();
        }
        
        // State-based command handling
        switch (currentState) {
            case IDLE:
                return handleIdleState(tokens);
            case SUGGESTING_DESTINATION:
                return handleSuggestionState(tokens, true);
            case SUGGESTING_SOURCE_DESTINATION:
                return handleSuggestionState(tokens, false);
            case ROUTE_DISPLAY:
                return handleRouteDisplayState(tokens);
            case NAVIGATING:
                return handleNavigationState(tokens);
            case LOCATION_SETTINGS:
                return handleLocationSettings(tokens);
            default:
                return handleIdleState(tokens);
        }
    }
    
    private String handleIdleState(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        if (command.equals("map") || command.equals("nav")) {
            if (tokens.length == 1) {
                return showRecentSearches();
            } else {
                // Parse map command with potential source/destination
                String input = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));
                return processMapInput(input);
            }
        }
        
        if (command.equals("location")) {
            return handleLocationCommand(tokens);
        }
        
        if (command.equals("nav") && currentRoute != null) {
            return startNavigation();
        }
        
        return "❌ Unknown command. Type 'map' for navigation or 'location' for location settings.";
    }
    
    private String handleLocationCommand(String[] tokens) {
        if (tokens.length == 1) {
            return getLocationStatus();
        }
        
        String subCommand = tokens[1].toLowerCase();
        switch (subCommand) {
            case "on":
                return enableLocation();
            case "off":
                return disableLocation();
            case "status":
                return getLocationStatus();
            case "gps":
                return getGPSStatus();
            case "high":
                return setLocationAccuracy(true);
            case "battery":
                return setLocationAccuracy(false);
            default:
                return "❌ Unknown location command. Use: location [on|off|status|gps|high|battery]";
        }
    }
    
    private String processMapInput(String input) {
        // Check if location services are enabled
        if (!isLocationEnabled()) {
            currentState = NavigationState.LOCATION_SETTINGS;
            return "📍 Location services disabled\n" +
                   "Enable location for navigation:\n" +
                   "• location on (enable)\n" +
                   "• location high (high accuracy)\n" +
                   "• location battery (battery saving)\n" +
                   "• exit (cancel)";
        }
        
        if (input.contains(";")) {
            // Source and destination provided
            String[] parts = input.split(";");
            if (parts.length == 2) {
                pendingSource = parts[0].trim();
                pendingDestination = parts[1].trim();
                currentState = NavigationState.SUGGESTING_SOURCE_DESTINATION;
                return getSourceDestinationSuggestions();
            }
        } else {
            // Only destination provided
            pendingDestination = input;
            pendingSource = null; // Will use current location
            currentState = NavigationState.SUGGESTING_DESTINATION;
            return getDestinationSuggestions();
        }
        
        return "❌ Invalid format. Use: map destination OR map source;destination";
    }
    
    private String handleSuggestionState(String[] tokens, boolean destinationOnly) {
        try {
            int selection = Integer.parseInt(tokens[0]);
            if (selection < 1 || selection > currentSuggestions.size()) {
                return "❌ Invalid selection. Choose 1-" + currentSuggestions.size();
            }
            
            LocationSuggestion selected = currentSuggestions.get(selection - 1);
            
            if (destinationOnly) {
                // Only selecting destination, source is current location
                return calculateRoute(getCurrentLocationName(), selected.name);
            } else {
                // Selecting both source and destination
                if (pendingSource != null) {
                    // First selection is for source
                    String source = selected.name;
                    pendingSource = null;
                    currentState = NavigationState.SUGGESTING_DESTINATION;
                    return "📍 Source: " + source + "\n" + getDestinationSuggestions();
                } else {
                    // Second selection is for destination
                    return calculateRoute(pendingSource, selected.name);
                }
            }
        } catch (NumberFormatException e) {
            return "❌ Please enter a number 1-" + currentSuggestions.size();
        }
    }
    
    private String handleRouteDisplayState(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "nav":
            case "start":
                return startNavigation();
            case "alt":
                if (tokens.length > 1) {
                    try {
                        int altRoute = Integer.parseInt(tokens[1]);
                        return showAlternativeRoute(altRoute);
                    } catch (NumberFormatException e) {
                        return "❌ Please enter route number";
                    }
                }
                return showAlternativeRoutes();
            case "details":
                return showRouteDetails();
            case "share":
                return shareRoute();
            default:
                return displayCurrentRoute() + "\n\n" + getRouteSuggestions();
        }
    }
    
    private String handleNavigationState(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "mute":
                voiceEnabled = false;
                speak("Voice guidance muted");
                return "🔇 Voice guidance muted";
            case "unmute":
                voiceEnabled = true;
                speak("Voice guidance enabled");
                return "🔊 Voice guidance enabled";
            case "pause":
                navigationPaused = true;
                textToSpeech.stop();
                return "⏸️ Navigation paused";
            case "resume":
                navigationPaused = false;
                return "▶️ Navigation resumed";
            case "status":
                return getNavigationStatus();
            case "next":
                return getNextStep();
            case "reroute":
                return recalculateRoute();
            default:
                return getNavigationSuggestions();
        }
    }
    
    private String handleLocationSettings(String[] tokens) {
        String command = tokens[0].toLowerCase();
        
        if (command.equals("location")) {
            if (tokens.length > 1) {
                String subCommand = tokens[1].toLowerCase();
                switch (subCommand) {
                    case "on":
                        String result = enableLocation();
                        if (result.contains("✅")) {
                            currentState = NavigationState.IDLE;
                        }
                        return result;
                    case "high":
                        result = setLocationAccuracy(true);
                        if (result.contains("✅")) {
                            currentState = NavigationState.IDLE;
                        }
                        return result;
                    case "battery":
                        result = setLocationAccuracy(false);
                        if (result.contains("✅")) {
                            currentState = NavigationState.IDLE;
                        }
                        return result;
                }
            }
        }
        
        if (command.equals("exit")) {
            currentState = NavigationState.IDLE;
            return "✅ Returned to navigation menu";
        }
        
        return "📍 Location Settings\n" +
               "Enable location for navigation:\n" +
               "• location on (enable)\n" +
               "• location high (high accuracy)\n" +
               "• location battery (battery saving)\n" +
               "• exit (cancel)";
    }
    
    private String handleExit() {
        switch (currentState) {
            case NAVIGATING:
                stopNavigation();
                return "🛑 Navigation stopped";
            case ROUTE_DISPLAY:
            case SUGGESTING_DESTINATION:
            case SUGGESTING_SOURCE_DESTINATION:
            case LOCATION_SETTINGS:
                currentState = NavigationState.IDLE;
                currentSuggestions.clear();
                return "✅ Returning to main menu";
            default:
                return "Already in main menu";
        }
    }
    
    // ===== Core Navigation Methods =====
    
    private String showRecentSearches() {
        if (recentSearches.isEmpty()) {
            return "🗺️ No recent searches\n\n💡 Type: map [destination] to search";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("🗺️ Recent Searches (Top ").append(RECENT_SEARCHES_LIMIT).append("):\n");
        
        for (int i = 0; i < Math.min(recentSearches.size(), RECENT_SEARCHES_LIMIT); i++) {
            LocationSearch search = recentSearches.get(i);
            sb.append(i + 1).append(". ").append(search.name)
              .append(" - ").append(search.timestamp).append("\n");
        }
        
        sb.append("\n💡 Type: map [destination] to navigate");
        return sb.toString();
    }
    
    private String getDestinationSuggestions() {
        currentSuggestions = getLocationSuggestions(pendingDestination);
        
        StringBuilder sb = new StringBuilder();
        sb.append("📍 Suggestions for '").append(pendingDestination).append("':\n");
        
        for (int i = 0; i < currentSuggestions.size(); i++) {
            sb.append(i + 1).append(". ").append(currentSuggestions.get(i).name).append("\n");
        }
        
        sb.append("\n💡 Type number to select destination");
        return sb.toString();
    }
    
    private String getSourceDestinationSuggestions() {
        if (pendingSource != null) {
            currentSuggestions = getLocationSuggestions(pendingSource);
            
            StringBuilder sb = new StringBuilder();
            sb.append("📍 Source suggestions for '").append(pendingSource).append("':\n");
            
            for (int i = 0; i < currentSuggestions.size(); i++) {
                sb.append(i + 1).append(". ").append(currentSuggestions.get(i).name).append("\n");
            }
            
            sb.append("\n💡 Type number to select source");
            return sb.toString();
        }
        
        return getDestinationSuggestions();
    }
    
    private String calculateRoute(String source, String destination) {
        try {
            // Use OSRM API for route calculation
            String routeData = fetchRouteFromOSRM(source, destination);
            currentRoute = parseRouteData(routeData);
            currentState = NavigationState.ROUTE_DISPLAY;
            
            // Add to recent searches
            addToRecentSearches(destination);
            
            return displayCurrentRoute();
            
        } catch (Exception e) {
            return "❌ Route calculation failed: " + e.getMessage() + 
                   "\n💡 Check internet connection and try again";
        }
    }
    
    private String displayCurrentRoute() {
        if (currentRoute == null) return "❌ No route calculated";
        
        StringBuilder sb = new StringBuilder();
        sb.append("🗺️ Route: ").append(currentRoute.source).append(" → ").append(currentRoute.destination).append("\n");
        sb.append("📏 Distance: ").append(currentRoute.distance).append("\n");
        sb.append("⏱️ ETA: ").append(currentRoute.duration).append("\n");
        sb.append("💰 Toll: ").append(currentRoute.hasToll ? "Yes" : "No").append("\n");
        sb.append("🚦 Traffic: ").append(currentRoute.trafficLevel).append("\n");
        sb.append("📍 Location: ").append(getLocationStatus());
        
        if (!currentRoute.alternativeRoutes.isEmpty()) {
            sb.append("\n🔄 ").append(currentRoute.alternativeRoutes.size()).append(" alternative routes available");
        }
        
        sb.append("\n").append(getRouteSuggestions());
        return sb.toString();
    }
    
    private String startNavigation() {
        if (currentRoute == null) return "❌ No route to navigate";
        
        if (!isLocationEnabled()) {
            return "❌ Location services disabled\n" +
                   "Enable location with: location on";
        }
        
        currentState = NavigationState.NAVIGATING;
        currentStep = 0;
        navigationPaused = false;
        
        // Start navigation updates
        startNavigationUpdates();
        
        // Initial voice guidance
        speak("Navigation started. " + currentRoute.distance + " to destination.");
        
        return "🚗 Navigation started!\n" +
               "📍 " + currentRoute.destination + "\n" +
               "📏 " + currentRoute.distance + " • ⏱️ " + currentRoute.duration + "\n" +
               "🔊 Voice: " + (voiceEnabled ? "ON" : "OFF") + "\n\n" +
               getNavigationSuggestions();
    }
    
    private void stopNavigation() {
        currentState = NavigationState.IDLE;
        navigationPaused = true;
        
        if (navigationScheduler != null) {
            navigationScheduler.shutdown();
            navigationScheduler = null;
        }
        
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        speak("Navigation ended");
    }
    
    // ===== Location Management =====
    
    private String enableLocation() {
        if (ShizukuManager.isAvailable()) {
            if (ShizukuManager.executeCommand("settings put secure location_providers_allowed +gps")) {
                return "✅ Location enabled via Shizuku\n" +
                       "🔧 GPS: Enabled | Mode: High Accuracy";
            }
        }
        
        // Fallback to system location settings
        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        
        return "📍 Open location settings to enable\n" +
               "💡 Enable GPS and location services";
    }
    
    private String disableLocation() {
        if (ShizukuManager.isAvailable()) {
            if (ShizukuManager.executeCommand("settings put secure location_providers_allowed -gps")) {
                return "✅ Location disabled via Shizuku";
            }
        }
        
        return "❌ Shizuku required to disable location\n" +
               "💡 Use system settings to disable location";
    }
    
    private String setLocationAccuracy(boolean highAccuracy) {
        if (ShizukuManager.isAvailable()) {
            String mode = highAccuracy ? "high_accuracy" : "battery_saving";
            if (ShizukuManager.executeCommand("settings put secure location_mode " + 
                (highAccuracy ? "3" : "2"))) {
                return "✅ Location mode set to " + (highAccuracy ? "High Accuracy" : "Battery Saving") + " via Shizuku";
            }
        }
        
        return "❌ Shizuku required for location mode control\n" +
               "💡 Use system settings to change location accuracy";
    }
    
    private String getLocationStatus() {
        boolean enabled = isLocationEnabled();
        String gpsStatus = getGPSStatus();
        
        return "📍 Location: " + (enabled ? "✅ Enabled" : "❌ Disabled") + "\n" +
               gpsStatus + "\n" +
               "🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Not Available ❌");
    }
    
    private String getGPSStatus() {
        if (ShizukuManager.isAvailable()) {
            String result = ShizukuManager.executeCommandWithOutput("settings get secure location_providers_allowed");
            if (result.contains("gps")) {
                return "🛰️ GPS: ✅ Enabled";
            }
        }
        
        // Fallback check
        if (locationManager != null) {
            try {
                boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                return "🛰️ GPS: " + (gpsEnabled ? "✅ Enabled" : "❌ Disabled");
            } catch (SecurityException e) {
                return "🛰️ GPS: ❓ Unknown (Permission needed)";
            }
        }
        
        return "🛰️ GPS: ❓ Status unknown";
    }
    
    private boolean isLocationEnabled() {
        if (ShizukuManager.isAvailable()) {
            String result = ShizukuManager.executeCommandWithOutput("settings get secure location_providers_allowed");
            return result != null && result.contains("gps");
        }
        
        // Fallback check
        if (locationManager != null) {
            try {
                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                       locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch (SecurityException e) {
                return false;
            }
        }
        
        return false;
    }
    
    // ===== Navigation Updates =====
    
    private void startNavigationUpdates() {
        navigationScheduler = Executors.newSingleThreadScheduledExecutor();
        navigationScheduler.scheduleAtFixedRate(() -> {
            if (!navigationPaused && currentState == NavigationState.NAVIGATING) {
                updateNavigation();
            }
        }, 0, 10, TimeUnit.SECONDS); // Update every 10 seconds
    }
    
    private void updateNavigation() {
        // Simulate navigation progress
        if (currentStep < currentRoute.steps.size()) {
            RouteStep step = currentRoute.steps.get(currentStep);
            
            // Send CLI update
            String update = "➡️ " + step.instruction + " (" + step.distance + ")";
            sendCLIUpdate(update);
            
            // Voice guidance
            if (voiceEnabled) {
                speak(step.instruction);
            }
            
            currentStep++;
            
            // Check if destination reached
            if (currentStep >= currentRoute.steps.size()) {
                navigationHandler.post(() -> {
                    sendCLIUpdate("🎉 Destination reached!");
                    speak("You have reached your destination");
                    stopNavigation();
                });
            }
        }
    }
    
    // ===== External Service Integration =====
    
    private List<LocationSuggestion> getLocationSuggestions(String query) {
        // Implement with Nominatim (OpenStreetMap) or other geocoding service
        List<LocationSuggestion> suggestions = new ArrayList<>();
        
        // Mock data - replace with real API call
        for (int i = 1; i <= SUGGESTION_LIMIT; i++) {
            suggestions.add(new LocationSuggestion(query + " Location " + i, "Address " + i));
        }
        
        return suggestions;
    }
    
    private String fetchRouteFromOSRM(String source, String destination) throws Exception {
        // Convert locations to coordinates (in real implementation)
        String sourceCoords = "13.388860,52.517037"; // Mock coordinates
        String destCoords = "13.397634,52.529407";   // Mock coordinates
        
        String urlString = OSRM_BASE_URL + sourceCoords + ";" + destCoords + "?overview=full";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }
    
    private Route parseRouteData(String jsonData) throws Exception {
        JSONObject json = new JSONObject(jsonData);
        JSONObject route = json.getJSONArray("routes").getJSONObject(0);
        
        Route newRoute = new Route();
        newRoute.distance = String.format("%.1f km", route.getDouble("distance") / 1000);
        newRoute.duration = formatDuration(route.getDouble("duration"));
        newRoute.source = pendingSource != null ? pendingSource : "Current Location";
        newRoute.destination = pendingDestination;
        
        // Parse steps for navigation
        JSONArray legs = route.getJSONArray("legs");
        for (int i = 0; i < legs.length(); i++) {
            JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");
            for (int j = 0; j < steps.length(); j++) {
                JSONObject step = steps.getJSONObject(j);
                String instruction = step.getString("name");
                String distance = String.format("%.1f km", step.getDouble("distance") / 1000);
                newRoute.steps.add(new RouteStep(instruction, distance));
            }
        }
        
        return newRoute;
    }
    
    // ===== Utility Methods =====
    
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.SUCCESS) {
                // TTS initialization failed, navigation will continue without voice
                voiceEnabled = false;
            }
        });
    }
    
    private void speak(String text) {
        if (textToSpeech != null && voiceEnabled) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    
    private void sendCLIUpdate(String message) {
        // This would integrate with your MainActivity to update CLI
        // Implementation depends on your activity communication
    }
    
    private void setupLocationUpdates() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // Setup location updates using Shizuku if available
    }
    
    private String getCurrentLocationName() {
        return "Current Location"; // Would use real location service
    }
    
    private void addToRecentSearches(String location) {
        LocationSearch search = new LocationSearch(location, new SimpleDateFormat("dd-MMM-yy;HH:mm").format(new Date()));
        recentSearches.removeIf(s -> s.name.equalsIgnoreCase(location));
        recentSearches.add(0, search);
        
        // Keep only recent searches limit
        if (recentSearches.size() > RECENT_SEARCHES_LIMIT) {
            recentSearches = recentSearches.subList(0, RECENT_SEARCHES_LIMIT);
        }
    }
    
    private void loadRecentSearches() {
        // Load from shared preferences or database
        // Mock data for now
        recentSearches.add(new LocationSearch("Home", "27-Sept-25;08:30"));
        recentSearches.add(new LocationSearch("Office", "26-Sept-25;09:15"));
    }
    
    private String formatDuration(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    private String getNextStep() {
        if (currentStep < currentRoute.steps.size()) {
            RouteStep step = currentRoute.steps.get(currentStep);
            return "➡️ Next: " + step.instruction + " (" + step.distance + ")";
        }
        return "🎉 You have reached your destination!";
    }
    
    private String recalculateRoute() {
        if (currentRoute == null) return "❌ No active route to recalculate";
        
        try {
            String routeData = fetchRouteFromOSRM(currentRoute.source, currentRoute.destination);
            Route newRoute = parseRouteData(routeData);
            currentRoute = newRoute;
            currentStep = 0;
            
            return "🔄 Route recalculated\n" + displayCurrentRoute();
        } catch (Exception e) {
            return "❌ Failed to recalculate route: " + e.getMessage();
        }
    }
    
    private String showRouteDetails() {
        if (currentRoute == null) return "❌ No route to show details";
        
        StringBuilder sb = new StringBuilder();
        sb.append("🗺️ Route Details:\n");
        sb.append("Steps: ").append(currentRoute.steps.size()).append("\n\n");
        
        for (int i = 0; i < Math.min(currentRoute.steps.size(), 10); i++) {
            RouteStep step = currentRoute.steps.get(i);
            sb.append(i + 1).append(". ").append(step.instruction).append(" (").append(step.distance).append(")\n");
        }
        
        if (currentRoute.steps.size() > 10) {
            sb.append("\n... and ").append(currentRoute.steps.size() - 10).append(" more steps");
        }
        
        return sb.toString();
    }
    
    private String shareRoute() {
        if (currentRoute == null) return "❌ No route to share";
        
        String shareText = "Route: " + currentRoute.source + " to " + currentRoute.destination + 
                          " - " + currentRoute.distance + " - " + currentRoute.duration;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        context.startActivity(Intent.createChooser(shareIntent, "Share Route"));
        
        return "✅ Route shared";
    }
    
    private String showAlternativeRoutes() {
        return "🔄 Alternative Routes:\n1. Fastest (5min longer, no toll)\n2. Scenic (15min longer)\n3. Shortest (more traffic)\n\n💡 Type: alt [number] to select";
    }
    
    private String showAlternativeRoute(int routeNumber) {
        return "✅ Alternative route " + routeNumber + " selected\n" + getRouteSuggestions();
    }
    
    private String getRouteSuggestions() {
        return "💡 Commands: nav (start) | alt (alternates) | details (steps) | share | exit";
    }
    
    private String getNavigationSuggestions() {
        return "💡 Commands: mute/unmute (voice) | pause/resume | status | next | reroute | exit";
    }
    
    private String getNavigationStatus() {
        if (currentRoute == null) return "❌ No active navigation";
        
        int progress = currentRoute.steps.size() > 0 ? 
            (int) ((double) currentStep / currentRoute.steps.size() * 100) : 0;
            
        return "🚗 Navigation Status:\n" +
               "📍 " + currentRoute.destination + "\n" +
               "📊 Progress: " + progress + "%\n" +
               "🎯 Step: " + currentStep + "/" + currentRoute.steps.size() + "\n" +
               "🔊 Voice: " + (voiceEnabled ? "ON" : "OFF") + "\n" +
               "⏸️ Paused: " + (navigationPaused ? "Yes" : "No");
    }
    
    private String getUsage() {
        return "🗺️ Navigation Module Usage:\n" +
               "• map/nav              - Show recent searches\n" +
               "• map [destination]    - Search location\n" +
               "• map [src];[dest]     - Route with source\n" +
               "• location             - Location status\n" +
               "• location on/off      - Enable/disable location\n" +
               "• location high/battery- Accuracy mode\n" +
               "• nav/start            - Start navigation\n" +
               "• exit                 - Cancel/stop\n" +
               "\n📍 Requires: Location permissions + Authentication" +
               "\n🔧 Shizuku: " + (ShizukuManager.isAvailable() ? "Available ✅" : "Not Available ❌") +
               "\n🔊 TTS: " + (textToSpeech != null ? "Available ✅" : "Not Available ❌");
    }
    
    // ===== Data Classes =====
    
    enum NavigationState {
        IDLE, SUGGESTING_DESTINATION, SUGGESTING_SOURCE_DESTINATION, 
        ROUTE_DISPLAY, NAVIGATING, LOCATION_SETTINGS
    }
    
    class LocationSearch {
        String name;
        String timestamp;
        
        LocationSearch(String name, String timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }
    }
    
    class LocationSuggestion {
        String name;
        String address;
        
        LocationSuggestion(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }
    
    class Route {
        String source;
        String destination;
        String distance;
        String duration;
        boolean hasToll = false;
        String trafficLevel = "Moderate";
        List<RouteStep> steps = new ArrayList<>();
        List<Route> alternativeRoutes = new ArrayList<>();
    }
    
    class RouteStep {
        String instruction;
        String distance;
        
        RouteStep(String instruction, String distance) {
            this.instruction = instruction;
            this.distance = distance;
        }
    }
}
