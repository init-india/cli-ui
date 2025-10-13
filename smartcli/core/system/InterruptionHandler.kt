package com.smartcli.core.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import android.util.Log

class InterruptionHandler(private val context: Context) {
    
    private var callState: String = "IDLE"
    private var interruptionListeners = mutableListOf<(String) -> Unit>()
    
    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    callState = "RINGING"
                    val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    notifyInterruption("INCOMING_CALL:$number")
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    callState = "ACTIVE"
                    notifyInterruption("CALL_ACTIVE")
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (callState == "ACTIVE") {
                        notifyInterruption("CALL_ENDED")
                    }
                    callState = "IDLE"
                }
            }
        }
    }
    
    fun register() {
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        context.registerReceiver(phoneStateReceiver, filter)
    }
    
    fun unregister() {
        try {
            context.unregisterReceiver(phoneStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("InterruptionHandler", "Receiver not registered")
        }
    }
    
    fun addInterruptionListener(listener: (String) -> Unit) {
        interruptionListeners.add(listener)
    }
    
    fun removeInterruptionListener(listener: (String) -> Unit) {
        interruptionListeners.remove(listener)
    }
    
    private fun notifyInterruption(event: String) {
        interruptionListeners.forEach { listener ->
            try {
                listener(event)
            } catch (e: Exception) {
                Log.e("InterruptionHandler", "Error in interruption listener", e)
            }
        }
    }
    
    fun getCurrentState(): String = callState
}
