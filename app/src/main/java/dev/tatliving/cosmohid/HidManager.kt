package dev.tatliving.cosmohid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import java.util.concurrent.Executors

/**
 * Singleton that turns this phone (the Cosmo) into a Bluetooth HID keyboard and
 * relays key presses to a connected host (the Find N2).
 *
 * Lifecycle: [start] grabs the HID_DEVICE profile proxy and registers our SDP
 * record. Once registered we can [connect] to a bonded host and [sendKeyDown]/
 * [sendKeyUp] real keystrokes. [stop] tears everything down.
 */
@SuppressLint("MissingPermission") // perms are requested in MainActivity; calls are guarded by start()
object HidManager {

    private const val TAG = "CosmoHID"

    private val executor = Executors.newSingleThreadExecutor()
    private val pressedUsages = LinkedHashSet<Int>() // current non-modifier keys (max 6)
    private var modifierByte = 0

    private var adapter: BluetoothAdapter? = null
    private var proxy: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null

    @Volatile var registered = false; private set
    @Volatile var connected = false; private set

    /** UI/IME observer. Called on any state change with a human-readable status line. */
    var onStatus: ((String) -> Unit)? = null

    private fun notify(msg: String) {
        Log.d(TAG, msg)
        onStatus?.invoke(status())
    }

    fun status(): String {
        val regTxt = if (registered) "registered" else "not registered"
        val conTxt = when {
            connected -> "CONNECTED to ${host?.name ?: host?.address ?: "?"}"
            host != null -> "connecting/idle (${host?.name ?: host?.address})"
            else -> "no host"
        }
        return "HID: $regTxt | $conTxt"
    }

    fun isSupported(context: Context): Boolean {
        val a = (context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager?)?.adapter
        return a != null
    }

    /** Acquire the HID_DEVICE proxy and register the keyboard SDP record. Idempotent. */
    fun start(context: Context) {
        if (proxy != null) { notify("already started"); return }
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE)
                as? android.bluetooth.BluetoothManager
        adapter = mgr?.adapter
        val a = adapter
        if (a == null || !a.isEnabled) {
            notify("Bluetooth off/unavailable")
            return
        }
        a.getProfileProxy(context.applicationContext, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, p: BluetoothProfile) {
                if (profile != BluetoothProfile.HID_DEVICE) return
                proxy = p as BluetoothHidDevice
                registerApp()
            }
            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    proxy = null
                    registered = false
                    connected = false
                    notify("HID service disconnected")
                }
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    private fun registerApp() {
        val p = proxy ?: return
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "CosmoHID Keyboard",
            "Relay physical keyboard over Bluetooth",
            "tatliving",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            HidConstants.REPORT_DESCRIPTOR
        )
        val ok = p.registerApp(sdp, null, null, executor, object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registeredNow: Boolean) {
                registered = registeredNow
                notify("app status: registered=$registeredNow")
            }

            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                connected = state == BluetoothProfile.STATE_CONNECTED
                if (connected) host = device
                notify("conn state=$state")
            }
        })
        notify("registerApp() returned $ok")
    }

    fun bondedDevices(): List<BluetoothDevice> =
        adapter?.bondedDevices?.toList() ?: emptyList()

    fun connect(device: BluetoothDevice) {
        val p = proxy
        if (p == null || !registered) { notify("can't connect: not registered"); return }
        host = device
        val ok = p.connect(device)
        notify("connect(${device.name ?: device.address}) -> $ok")
    }

    fun disconnect() {
        val p = proxy ?: return
        host?.let { p.disconnect(it) }
        notify("disconnect()")
    }

    fun stop() {
        try {
            proxy?.let { p ->
                host?.let { p.disconnect(it) }
                p.unregisterApp()
                adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, p)
            }
        } catch (_: Exception) {}
        proxy = null
        registered = false
        connected = false
        host = null
        pressedUsages.clear()
        modifierByte = 0
        notify("stopped")
    }

    // ---- key relay -------------------------------------------------------

    /** Returns true if the keycode is something we can forward (and thus consume). */
    fun handles(keyCode: Int): Boolean =
        HidConstants.KEYCODE_TO_USAGE.containsKey(keyCode) ||
        HidConstants.KEYCODE_TO_MODBIT.containsKey(keyCode)

    /** Forward a key-down. Returns true if it was sent (consumed). */
    fun sendKeyDown(keyCode: Int): Boolean {
        HidConstants.KEYCODE_TO_MODBIT[keyCode]?.let {
            modifierByte = modifierByte or it
            return sendReport()
        }
        val usage = HidConstants.KEYCODE_TO_USAGE[keyCode] ?: return false
        if (pressedUsages.size < 6) pressedUsages.add(usage)
        return sendReport()
    }

    /** Forward a key-up. Returns true if it was sent (consumed). */
    fun sendKeyUp(keyCode: Int): Boolean {
        HidConstants.KEYCODE_TO_MODBIT[keyCode]?.let {
            modifierByte = modifierByte and it.inv()
            return sendReport()
        }
        val usage = HidConstants.KEYCODE_TO_USAGE[keyCode] ?: return false
        pressedUsages.remove(usage)
        return sendReport()
    }

    private fun sendReport(): Boolean {
        val p = proxy ?: return false
        val h = host ?: return false
        if (!connected) return false
        val report = ByteArray(8)
        report[0] = modifierByte.toByte()
        report[1] = 0
        var i = 2
        for (u in pressedUsages) {
            if (i > 7) break
            report[i++] = u.toByte()
        }
        return try {
            p.sendReport(h, 0, report)
        } catch (e: Exception) {
            Log.e(TAG, "sendReport failed", e)
            false
        }
    }
}
