package com.smartcli.launcher

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope

import com.smartcli.core.cli.CommandProcessor
import com.smartcli.core.cli.TUIFormatter
import com.smartcli.core.system.AuthManager
import com.smartcli.core.system.InterruptionHandler
import com.smartcli.core.system.LinuxCommandExecutor




class FdroidMainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // Views
    private lateinit var cliContainer: LinearLayout
    private lateinit var lockScreen: LinearLayout
    private lateinit var cliInput: EditText
    private lateinit var cliOutput: TextView
    private lateinit var statusBar: TextView
    private lateinit var notificationBar: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var authMessage: TextView
    private lateinit var fingerprintIcon: ImageView

    // Core components
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var authManager: AuthManager
    private lateinit var interruptionHandler: InterruptionHandler

    // System state
    private var isAuthenticated = false
    private var lastCommandTime = 0L
    private val commandHistory = mutableListOf<String>()
    private var currentHistoryIndex = -1

    // Broadcast receivers
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateStatusBar()
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateStatusBar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupWindowFlags()
        initializeComponents()
        initializeViews()
        setupBiometricAuth()
        registerReceivers()
        setupInterruptionHandler()
        checkPermissions()

        // Start with locked state
        showLockScreen()
    }

    private fun setupWindowFlags() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Make sure we're the home launcher
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun initializeComponents() {
        commandProcessor = CommandProcessor()
        authManager = AuthManager(this)
        interruptionHandler = InterruptionHandler(this)
        executor = ContextCompat.getMainExecutor(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeViews() {
        cliContainer = findViewById(R.id.cliContainer)
        lockScreen = findViewById(R.id.lockScreen)
        cliInput = findViewById(R.id.cliInput)
        cliOutput = findViewById(R.id.cliOutput)
        statusBar = findViewById(R.id.statusBar)
        notificationBar = findViewById(R.id.notificationBar)
        scrollView = findViewById(R.id.cliScrollView)
        authMessage = findViewById(R.id.authMessage)
        fingerprintIcon = findViewById(R.id.fingerprintIcon)

        // Setup CLI input handler
        cliInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_ENTER -> {
                        executeCommand(cliInput.text.toString())
                        cliInput.text.clear()
                        currentHistoryIndex = -1
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        navigateCommandHistory(-1)
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        navigateCommandHistory(1)
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_TAB -> {
                        autoCompleteCommand()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }

        // Double tap to lock
        cliContainer.setOnTouchListener(object : View.OnTouchListener {
            private var lastTouchTime: Long = 0
            override fun onTouch(v: View?, event: android.view.MotionEvent): Boolean {
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTouchTime < 300) { // Double tap
                        lockDevice()
                        return true
                    }
                    lastTouchTime = currentTime
                }
                return false
            }
        })

        // Focus management
        cliInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard()
            }
        }
    }

    private fun setupBiometricAuth() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            authMessage.text = "Too many attempts. Use PIN instead."
                            showPinAuth()
                        }
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            // User canceled, keep showing auth
                        }
                        else -> {
                            authMessage.text = "Authentication error: $errString"
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleAuthenticationSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    authMessage.text = "Authentication failed. Try again."
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("SmartCLI Authentication")
            .setSubtitle("Authenticate to access SmartCLI")
            .setDescription("Use fingerprint or device credentials to unlock")
            .setAllowedAuthenticators(
                BiometricPrompt.Authenticators.BIOMETRIC_STRONG or
                        BiometricPrompt.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    private fun showPinAuth() {
        // For F-Droid version, use simple PIN auth
        val pinDialog = PinAuthDialog(this) { pin ->
            if (authManager.validatePin(pin)) {
                handleAuthenticationSuccess()
            } else {
                authMessage.text = "Invalid PIN. Try again."
                showBiometricAuth()
            }
        }
        pinDialog.show()
    }

    private fun handleAuthenticationSuccess() {
        isAuthenticated = true
        authManager.saveSessionToken(generateSessionToken())
        lifecycleScope.launch {
            unlockDevice()
            startSystemMonitoring()
        }
    }

    private fun generateSessionToken(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun showLockScreen() {
        lockScreen.isVisible = true
        cliContainer.isVisible = false
        showBiometricAuth()
    }

    private fun showBiometricAuth() {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                authMessage.text = "Biometric not available. Using PIN."
                showPinAuth()
            }
        }, 500)
    }

    private suspend fun unlockDevice() {
        withContext(Dispatchers.Main) {
            lockScreen.isVisible = false
            cliContainer.isVisible = true
            cliInput.requestFocus()

            val welcomeMsg = """
            ┌─────────────────────────────────────────────────────┐
            │                SMARTCLI LAUNCHER                    │
            ├─────────────────────────────────────────────────────┤
            │ # Authentication successful                        │
            │ # Welcome to SmartCLI v1.0                         │
            │ # System ready - Type 'help' for commands          │
            │ # F-Droid Edition - Pure & Open Source             │
            └─────────────────────────────────────────────────────┘
            """.trimIndent()

            appendToOutput(welcomeMsg)
            updateStatusBar()
        }
    }

    private fun lockDevice() {
        isAuthenticated = false
        authManager.clearSession()
        showLockScreen()
    }

    private suspend fun executeCommand(command: String) {
        if (!isAuthenticated) return

        val trimmedCommand = command.trim()
        if (trimmedCommand.isEmpty()) return

        // Add to history
        commandHistory.add(trimmedCommand)
        if (commandHistory.size > 100) {
            commandHistory.removeFirst()
        }

        lastCommandTime = System.currentTimeMillis()

        appendToOutput("user@smartcli:~$ $trimmedCommand")

        val result = withContext(Dispatchers.IO) {
            commandProcessor.processCommand(trimmedCommand, this@FdroidMainActivity)
        }

        appendToOutput(result)
        updateNotificationBar(trimmedCommand)
    }

    private suspend fun appendToOutput(text: String) {
        withContext(Dispatchers.Main) {
            cliOutput.append("$text\n\n")
            // Auto-scroll to bottom
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun navigateCommandHistory(direction: Int) {
        if (commandHistory.isEmpty()) return

        currentHistoryIndex += direction
        when {
            currentHistoryIndex < 0 -> currentHistoryIndex = -1
            currentHistoryIndex >= commandHistory.size -> currentHistoryIndex = commandHistory.size - 1
        }

        cliInput.setText(
            if (currentHistoryIndex >= 0) commandHistory[currentHistoryIndex] else ""
        )
        cliInput.setSelection(cliInput.text.length)
    }

    private fun autoCompleteCommand() {
        val currentText = cliInput.text.toString()
        val suggestions = listOf(
            "help", "lock", "exit", "map", "sms", "call", "contact",
            "mail", "wifi", "bluetooth", "app", "ps", "kill", "settings"
        )

        val match = suggestions.find { it.startsWith(currentText, true) }
        match?.let {
            cliInput.setText(it)
            cliInput.setSelection(it.length)
        }
    }

    private fun startSystemMonitoring() {
        lifecycleScope.launch {
            while (isAuthenticated) {
                updateStatusBar()
                delay(2000) // Update every 2 seconds
            }
        }
    }

    private fun updateStatusBar() {
        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) {
                TUIFormatter.formatSystemStatus(this@FdroidMainActivity)
            }
            withContext(Dispatchers.Main) {
                statusBar.text = status
            }
        }
    }

    private fun updateNotificationBar(command: String) {
        // Update notification bar based on last command
        val notification = when {
            command.startsWith("sms") -> "✉️ SMS ready"
            command.startsWith("call") -> "📞 Calling..."
            command.startsWith("map") -> "🗺️ Navigation started"
            else -> "Ready for command"
        }
        notificationBar.text = notification
    }

    private fun registerReceivers() {
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(wifiReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
    }

    private fun setupInterruptionHandler() {
        interruptionHandler.addInterruptionListener { event ->
            lifecycleScope.launch {
                when {
                    event.startsWith("INCOMING_CALL") -> {
                        appendToOutput("🔔 Incoming call: ${event.substringAfter(":")}")
                    }
                    event == "CALL_ACTIVE" -> {
                        appendToOutput("📞 Call active")
                    }
                    event == "CALL_ENDED" -> {
                        appendToOutput("📞 Call ended")
                    }
                }
            }
        }
        interruptionHandler.register()
    }

    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.CHANGE_WIFI_STATE
        )

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }.keys
        if (deniedPermissions.isNotEmpty()) {
            lifecycleScope.launch {
                appendToOutput("⚠️  Missing permissions: ${deniedPermissions.joinToString()}")
            }
        }
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(cliInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(cliInput.windowToken, 0)
    }

    override fun onBackPressed() {
        if (isAuthenticated) {
            // Minimize instead of exit for launcher
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(wifiReceiver)
            interruptionHandler.unregister()
        } catch (e: IllegalArgumentException) {
            // Receiver not registered, ignore
        }
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    override fun onResume() {
        super.onResume()
        if (isAuthenticated) {
            updateStatusBar()
        }
    }
}

// PIN Auth Dialog for F-Droid version
class PinAuthDialog(
    context: Context,
    private val onPinEntered: (String) -> Unit
) : Dialog(context) {

    private lateinit var pinInput: EditText
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button

    init {
        setContentView(R.layout.dialog_pin_auth)
        setupViews()
    }

    private fun setupViews() {
        pinInput = findViewById(R.id.pinInput)
        submitButton = findViewById(R.id.submitButton)
        cancelButton = findViewById(R.id.cancelButton)

        submitButton.setOnClickListener {
            val pin = pinInput.text.toString()
            if (pin.length >= 4) {
                onPinEntered(pin)
                dismiss()
            } else {
                pinInput.error = "PIN must be at least 4 digits"
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        // Auto-submit on enter
        pinInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                submitButton.performClick()
                true
            } else false
        }
    }
}
