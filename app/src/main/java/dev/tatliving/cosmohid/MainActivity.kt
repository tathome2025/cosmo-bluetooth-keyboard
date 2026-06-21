package dev.tatliving.cosmohid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var deviceList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBtPermissions()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 56, 40, 56)
        }

        root.addView(header("CosmoHID — keyboard relay"))

        status = TextView(this).apply {
            text = "Idle"
            setPadding(0, 0, 0, 24)
        }
        root.addView(status)

        root.addView(button("1. Start HID service (register)") {
            HidService.start(this)
            refreshSoon()
        })

        root.addView(button("2. Refresh paired devices") { refreshDevices() })

        deviceList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(deviceList)

        root.addView(button("Disconnect") {
            HidManager.disconnect(); refreshSoon()
        })

        root.addView(button("Reduce lag: disable battery optimization") {
            requestIgnoreBatteryOptimization()
        })

        root.addView(header("Verify"))
        root.addView(button("Send TEST: Space (play/pause)") {
            HidManager.sendKeyDown(KeyEvent.KEYCODE_SPACE)
            HidManager.sendKeyUp(KeyEvent.KEYCODE_SPACE)
        })

        root.addView(header("Input method"))
        root.addView(button("Enable CosmoHID Relay in settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(button("Switch to CosmoHID Relay (picker)") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        })

        root.addView(header("Capture box"))
        root.addView(TextView(this).apply {
            text = "Tap below and pick CosmoHID Relay as the keyboard. " +
                    "While this box is focused, the Cosmo's physical keys are relayed to the host."
            setPadding(0, 0, 0, 16)
        })
        root.addView(EditText(this).apply {
            hint = "focus here, then type on the Cosmo keyboard"
            minLines = 2
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        HidManager.onStatus = { runOnUiThread { status.text = HidManager.status() } }
        refreshSoon()
    }

    private fun refreshSoon() = status.postDelayed({ status.text = HidManager.status() }, 300)

    private fun refreshDevices() {
        deviceList.removeAllViews()
        val devices = HidManager.bondedDevices()
        if (devices.isEmpty()) {
            deviceList.addView(TextView(this).apply {
                text = "No paired devices. Pair the Find N2 with this Cosmo in Bluetooth settings first."
            })
            return
        }
        for (d in devices) {
            val name = try { d.name } catch (e: SecurityException) { null } ?: d.address
            deviceList.addView(button("Connect: $name") {
                HidManager.connect(d); refreshSoon()
            })
        }
    }

    @Suppress("BatteryLife")
    private fun requestIgnoreBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            status.text = "Already exempt from battery optimization"
            return
        }
        try {
            startActivity(Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            ))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun requestBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val needed = listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
            if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 1)
        }
        // Android 9 (the Cosmo): BLUETOOTH/BLUETOOTH_ADMIN are install-time perms, nothing to request.
    }

    // ---- tiny view helpers ----
    private fun header(t: String) = TextView(this).apply {
        text = t
        textSize = 18f
        gravity = Gravity.START
        setPadding(0, 32, 0, 12)
    }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setOnClickListener { onClick() }
    }
}
