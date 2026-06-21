package dev.tatliving.cosmohid

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.TextView

/**
 * Input method whose only job is to grab real hardware key events from the Cosmo
 * keyboard and relay them as Bluetooth HID reports to the connected host.
 *
 * For the events to arrive here, this IME must be the active input method AND a
 * text field must be focused on the Cosmo (use the "capture box" in MainActivity).
 */
class RelayImeService : InputMethodService() {

    private var statusView: TextView? = null

    override fun onCreateInputView(): View {
        val tv = TextView(this).apply {
            setPadding(32, 24, 32, 24)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#101418"))
            setTextColor(Color.parseColor("#E6E6E6"))
            text = relayLine()
        }
        statusView = tv
        HidManager.onStatus = { runOnUi { statusView?.text = relayLine() } }
        return tv
    }

    private fun relayLine(): String {
        val mode = if (HidManager.connected) "RELAYING keys to host" else "NOT connected — keys type locally"
        return "CosmoHID • $mode\n${HidManager.status()}"
    }

    private fun runOnUi(block: () -> Unit) {
        statusView?.post(block) ?: block()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (HidManager.connected && HidManager.handles(keyCode)) {
            if (event.repeatCount == 0) HidManager.sendKeyDown(keyCode)
            return true // consume so it doesn't type into the local capture box
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (HidManager.connected && HidManager.handles(keyCode)) {
            HidManager.sendKeyUp(keyCode)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        if (HidManager.onStatus != null) HidManager.onStatus = null
        statusView = null
        super.onDestroy()
    }
}
