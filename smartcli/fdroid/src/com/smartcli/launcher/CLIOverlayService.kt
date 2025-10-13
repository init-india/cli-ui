package com.smartcli.launcher

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.isVisible

class CLIOverlayService : Service() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createOverlay() {
        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            gravity = Gravity.BOTTOM or Gravity.START
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = 0
            y = 0
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_cli, null)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(overlayView, layoutParams)

        setupOverlayInteraction()
    }

    private fun setupOverlayInteraction() {
        overlayView?.findViewById<EditText>(R.id.overlayInput)?.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                val command = overlayView?.findViewById<EditText>(R.id.overlayInput)?.text.toString()
                if (command.isNotEmpty()) {
                    // Send command to main activity
                    val intent = Intent(this, FdroidMainActivity::class.java).apply {
                        putExtra("COMMAND", command)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    
                    overlayView?.findViewById<EditText>(R.id.overlayInput)?.text?.clear()
                    hideOverlay()
                }
                true
            } else false
        }

        // Toggle overlay visibility on tap
        overlayView?.setOnClickListener {
            val input = overlayView?.findViewById<EditText>(R.id.overlayInput)
            input?.isVisible = !(input?.isVisible ?: false)
            if (input?.isVisible == true) {
                input.requestFocus()
            }
        }
    }

    private fun hideOverlay() {
        overlayView?.findViewById<EditText>(R.id.overlayInput)?.isVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            windowManager?.removeView(it)
        }
    }
}
