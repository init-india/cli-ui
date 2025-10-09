package com.cliui.launcher;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Start CLIUI Python code
        new Thread(() -> {
            try {
                // This would start the Python interpreter
                // For now, just show a message
                runOnUiThread(() -> {
                    // You would initialize Python here
                    // Python.start(new AndroidPlatform(this));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
