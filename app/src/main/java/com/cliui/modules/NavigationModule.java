package com.cliui.modules;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.location.LocationManager;
import android.content.pm.PackageManager;

public class NavigationModule implements CommandModule {
    private Context context;
    
    public NavigationModule(Context context) {
        this.context = context;
    }
    
    @Override
    public String execute(String[] tokens) {
        if (tokens.length == 1) {
            return "🗺️ Recent locations:\n1. Home\n2. Office\n3. Starbucks\n\nUsing OpenStreetMap";
        }
        
        String input = String.join(" ", tokens).substring(4); // Remove "map "
        
        if (input.contains(";")) {
            String[] parts = input.split(";");
            String source = parts[0].trim();
            String destination = parts[1].trim();
            
            // Open OpenStreetMap with route
            String osmUrl = "https://www.openstreetmap.org/directions?engine=osrm_car&route=" + 
                           Uri.encode(source) + ";" + Uri.encode(destination);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(osmUrl));
            context.startActivity(intent);
            
            return "📍 Opening OpenStreetMap route:\n" + source + " → " + destination;
        } else {
            // Open OpenStreetMap with destination
            String osmUrl = "https://www.openstreetmap.org/search?query=" + Uri.encode(input);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(osmUrl));
            context.startActivity(intent);
            
            return "📍 Opening OpenStreetMap for: " + input;
        }
    }
    
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && 
               (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }
}
