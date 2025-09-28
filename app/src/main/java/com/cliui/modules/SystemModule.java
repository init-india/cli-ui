package com.cliui.modules;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;

public class SystemModule implements CommandModule {
    private Context context;
    private boolean flashEnabled = false;
    private boolean locationEnabled = false;
    private boolean micEnabled = true;
    private boolean cameraEnabled = false;
    
    public SystemModule(Context context) {
        this.context = context;
    }
    
    @Override
    public String execute(String[] tokens) {
        String command = tokens[0];
        
        switch (command) {
            case "flash":
                flashEnabled = !flashEnabled;
                return flashEnabled ? "🔦 Flashlight ON\n[Open camera app to use]" : "🔦 Flashlight OFF";
                
            case "location":
                locationEnabled = !locationEnabled;
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                
                return "📍 Location Services:\n" +
                       "GPS: " + (gpsEnabled ? "Enabled" : "Disabled") + "\n" +
                       "Network: " + (networkEnabled ? "Enabled" : "Disabled") + "\n" +
                       "[Open location settings to change]";
                
            case "mic":
                micEnabled = !micEnabled;
                return micEnabled ? "🎤 Microphone enabled" : "🎤 Microphone disabled\n[App permissions required]";
                
            case "camera":
                // Open device camera app
                android.content.Intent intent = new android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                context.startActivity(intent);
                return "📷 Opening camera app...";
                
            default:
                return "Unknown system command";
        }
    }
}
