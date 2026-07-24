
package com.nothing.essentialremapper
import android.os.Build
import android.content.Context
import android.provider.Settings
object DeviceDetector {
    data class DeviceInfo(val manufacturer:String, val model:String, val isNothingFamily:Boolean, val displayName:String, val osVersion:String, val hasEssentialKey:Boolean, val hasGlyph:Boolean, val detectedCode:Int, val detectedName:String, val accessibilityEnabled:Boolean, val notificationEnabled:Boolean)
    fun detect(context: Context): DeviceInfo {
        val manuf = Build.MANUFACTURER ?: "Unknown"; val model = Build.MODEL ?: "Unknown"; val display = Build.DISPLAY ?: ""
        val prefs = PrefsManager.get(context)
        val isNothingFamily = manuf.equals("Nothing", true) || model.contains("Nothing", true) || listOf("A063","A065","A142","A059","A061","A062","A147","A148","B094","A015").any{ model.contains(it) } || display.contains("Nothing", true) || model.contains("CMF", true)
        val displayName = when {
            model.contains("A147") || model.contains("Phone (4)") && !model.contains("4a") -> "Nothing Phone (4)"
            model.contains("A148") || model.contains("Phone (4a)") -> "Nothing Phone (4a)"
            model.contains("A062") || (model.contains("Phone (3)") && !model.contains("3a")) -> "Nothing Phone (3)"
            model.contains("A059") -> "Nothing Phone (3a)"; model.contains("A061") -> "Nothing Phone (3a) Pro"
            model.contains("A065") -> "Nothing Phone (2)"; model.contains("A142") -> "Nothing Phone (2a)"; model.contains("A063") -> "Nothing Phone (1)"
            model.contains("B094") -> "CMF Phone 2 Pro"; model.contains("CMF") || model.contains("A015") -> "CMF Phone 1/2"
            isNothingFamily -> "Nothing Device ($model)"; else -> model
        }
        val hasEssentialKey = displayName.contains("Phone (3)") || displayName.contains("Phone (4)") || displayName.contains("CMF Phone 2 Pro")
        val hasGlyph = !displayName.contains("CMF")
        val osVer = try { val c=Class.forName("android.os.SystemProperties"); val m=c.getMethod("get", String::class.java); (m.invoke(null,"ro.nothing.version") as? String)?.takeIf{it.isNotBlank()} ?: display } catch(e:Exception){ display }
        val detectedCode = prefs.getInt(PrefsManager.KEY_DETECTED_CODE, -1)
        val detectedName = prefs.getString(PrefsManager.KEY_DETECTED_NAME, "none") ?: "none"
        val accEnabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains("${context.packageName}/${context.packageName}.EssentialButtonService") == true
        val notifEnabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
        return DeviceInfo(manuf, model, isNothingFamily, displayName, osVer, hasEssentialKey, hasGlyph, detectedCode, detectedName, accEnabled, notifEnabled)
    }
}
