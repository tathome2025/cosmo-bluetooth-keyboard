package dev.tatliving.cosmohid

import android.view.KeyEvent

/**
 * HID keyboard plumbing.
 *
 * We advertise a standard 8-byte boot-protocol keyboard:
 *   byte 0 = modifier bitmask, byte 1 = reserved, bytes 2..7 = up to 6 key usages.
 * No Report ID is used, so reports are sent with id 0.
 */
object HidConstants {

    /** Standard boot keyboard report descriptor (no Report ID). */
    val REPORT_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x06,             // Usage (Keyboard)
        0xA1.toByte(), 0x01,    // Collection (Application)
        0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0.toByte(),    //   Usage Minimum (Left Control)
        0x29, 0xE7.toByte(),    //   Usage Maximum (Right GUI)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95.toByte(), 0x08,    //   Report Count (8)
        0x81.toByte(), 0x02,    //   Input (Data, Var, Abs)  -> modifier byte
        0x95.toByte(), 0x01,    //   Report Count (1)
        0x75, 0x08,             //   Report Size (8)
        0x81.toByte(), 0x01,    //   Input (Const)           -> reserved byte
        0x95.toByte(), 0x06,    //   Report Count (6)
        0x75, 0x08,             //   Report Size (8)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x65,             //   Logical Maximum (101)
        0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,             //   Usage Minimum (0)
        0x29, 0x65,             //   Usage Maximum (101)
        0x81.toByte(), 0x00,    //   Input (Data, Array)     -> 6 key slots
        0xC0.toByte()           // End Collection
    )

    // Modifier bitmask values (byte 0 of the report)
    const val MOD_LCTRL = 0x01
    const val MOD_LSHIFT = 0x02
    const val MOD_LALT = 0x04
    const val MOD_LGUI = 0x08
    const val MOD_RCTRL = 0x10
    const val MOD_RSHIFT = 0x20
    const val MOD_RALT = 0x40
    const val MOD_RGUI = 0x80

    /** Android KeyEvent.KEYCODE_* -> HID usage id (Usage Page 0x07). */
    val KEYCODE_TO_USAGE: Map<Int, Int> = buildMap {
        // Letters a-z -> 0x04..0x1D
        for (i in 0..25) put(KeyEvent.KEYCODE_A + i, 0x04 + i)
        // Numbers 1-9 -> 0x1E..0x26, 0 -> 0x27
        for (i in 1..9) put(KeyEvent.KEYCODE_0 + i, 0x1E + (i - 1))
        put(KeyEvent.KEYCODE_0, 0x27)

        put(KeyEvent.KEYCODE_ENTER, 0x28)
        put(KeyEvent.KEYCODE_NUMPAD_ENTER, 0x28)
        put(KeyEvent.KEYCODE_ESCAPE, 0x29)
        put(KeyEvent.KEYCODE_DEL, 0x2A)          // Backspace
        put(KeyEvent.KEYCODE_TAB, 0x2B)
        put(KeyEvent.KEYCODE_SPACE, 0x2C)
        put(KeyEvent.KEYCODE_MINUS, 0x2D)
        put(KeyEvent.KEYCODE_EQUALS, 0x2E)
        put(KeyEvent.KEYCODE_LEFT_BRACKET, 0x2F)
        put(KeyEvent.KEYCODE_RIGHT_BRACKET, 0x30)
        put(KeyEvent.KEYCODE_BACKSLASH, 0x31)
        put(KeyEvent.KEYCODE_SEMICOLON, 0x33)
        put(KeyEvent.KEYCODE_APOSTROPHE, 0x34)
        put(KeyEvent.KEYCODE_GRAVE, 0x35)
        put(KeyEvent.KEYCODE_COMMA, 0x36)
        put(KeyEvent.KEYCODE_PERIOD, 0x37)
        put(KeyEvent.KEYCODE_SLASH, 0x38)
        put(KeyEvent.KEYCODE_CAPS_LOCK, 0x39)

        // F1-F12 -> 0x3A..0x45
        for (i in 0..11) put(KeyEvent.KEYCODE_F1 + i, 0x3A + i)

        put(KeyEvent.KEYCODE_SYSRQ, 0x46)        // PrintScreen
        put(KeyEvent.KEYCODE_SCROLL_LOCK, 0x47)
        put(KeyEvent.KEYCODE_BREAK, 0x48)        // Pause
        put(KeyEvent.KEYCODE_INSERT, 0x49)
        put(KeyEvent.KEYCODE_MOVE_HOME, 0x4A)
        put(KeyEvent.KEYCODE_PAGE_UP, 0x4B)
        put(KeyEvent.KEYCODE_FORWARD_DEL, 0x4C)  // Delete
        put(KeyEvent.KEYCODE_MOVE_END, 0x4D)
        put(KeyEvent.KEYCODE_PAGE_DOWN, 0x4E)
        put(KeyEvent.KEYCODE_DPAD_RIGHT, 0x4F)
        put(KeyEvent.KEYCODE_DPAD_LEFT, 0x50)
        put(KeyEvent.KEYCODE_DPAD_DOWN, 0x51)
        put(KeyEvent.KEYCODE_DPAD_UP, 0x52)
    }

    /** Modifier keycodes don't go in the key slots; they set bits in the modifier byte. */
    val KEYCODE_TO_MODBIT: Map<Int, Int> = mapOf(
        KeyEvent.KEYCODE_CTRL_LEFT to MOD_LCTRL,
        KeyEvent.KEYCODE_CTRL_RIGHT to MOD_RCTRL,
        KeyEvent.KEYCODE_SHIFT_LEFT to MOD_LSHIFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT to MOD_RSHIFT,
        KeyEvent.KEYCODE_ALT_LEFT to MOD_LALT,
        KeyEvent.KEYCODE_ALT_RIGHT to MOD_RALT,
        KeyEvent.KEYCODE_META_LEFT to MOD_LGUI,
        KeyEvent.KEYCODE_META_RIGHT to MOD_RGUI,
    )
}
