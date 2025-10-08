package com.cliui.launcher;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Python for Android
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        
        // Start CLIUI
        Python python = Python.getInstance();
        python.getModule("main").callAttr("main");
        
        // Keep the app running
        finish(); // Close Android UI, keep Python running
    }
}
