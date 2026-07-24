
package com.nothing.essentialremapper
import android.content.Context
object PrefsManager {
    fun get(c: Context) = c.getSharedPreferences("essential_remapper_ultimate", Context.MODE_PRIVATE)
    const val KEY_DETECTED_CODE = "detected_keycode"
    const val KEY_DETECTED_NAME = "detected_key_name"
    const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
    const val KEY_DOUBLE_INTERVAL = "double_interval"
    const val KEY_LONG_DURATION = "long_duration"
    const val KEY_OVERLAY_STYLE = "overlay_style"
    const val KEY_STATE_AUTOMATION = "state_automation"
    const val KEY_SCREEN_OFF_ENABLED = "screen_off_enabled"
    fun actionKey(g:String) = "action_$g"
    fun stateActionKey(g:String) = "state_action_${g}_media"
}
