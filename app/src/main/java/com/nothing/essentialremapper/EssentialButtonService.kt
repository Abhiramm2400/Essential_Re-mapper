
package com.nothing.essentialremapper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.*
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class EssentialButtonService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var pressCount = 0
    private var lastDown = 0L
    private var isLong = false
    private var longRunnable: Runnable? = null
    private var detectedCode = -1
    private var learningMode = false
    private var learningPresses = mutableListOf<Int>()
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val prefs = PrefsManager.get(this)
        detectedCode = prefs.getInt(PrefsManager.KEY_DETECTED_CODE, -1)
        ActionExecutor.serviceRef = this
        // Start keep alive service for screen-off support
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(Intent(this, KeepAliveService::class.java))
            else startService(Intent(this, KeepAliveService::class.java))
        } catch(_:Exception){}
        // Wake lock for screen off
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EssentialRemapper:Service")
            wakeLock?.acquire()
        } catch(_:Exception){}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { wakeLock?.release() }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val prefs = PrefsManager.get(this)
        learningMode = prefs.getBoolean("learning_mode", false)

        if(event.action == KeyEvent.ACTION_DOWN) {
            val code = event.keyCode

            // LEARNING MODE - auto essential button detecting
            if(learningMode && !isExcluded(code)) {
                learningPresses.add(code)
                if(learningPresses.size >= 3) {
                    val mostCommon = learningPresses.groupingBy{it}.eachCount().maxByOrNull{it.value}?.key ?: code
                    detectedCode = mostCommon
                    prefs.edit().putInt(PrefsManager.KEY_DETECTED_CODE, mostCommon).putString(PrefsManager.KEY_DETECTED_NAME, KeyEvent.keyCodeToString(mostCommon)).putBoolean("learning_mode", false).apply()
                    learningPresses.clear()
                    showOverlay("SAVED: ${KeyEvent.keyCodeToString(mostCommon)}")
                    HapticEngine.vibrate(this,90,HapticEngine.Type.GLYPH)
                    return true
                } else {
                    showOverlay("Learning ${learningPresses.size}/3: ${KeyEvent.keyCodeToString(code)}")
                    HapticEngine.vibrate(this,60,HapticEngine.Type.LIGHT)
                    return true
                }
            }

            // If we have detected code, only handle that. Else accept common essential codes
            if(detectedCode != -1 && code != detectedCode) {
                val common = setOf(1018,1019,1020,1021,KeyEvent.KEYCODE_BOOKMARK,KeyEvent.KEYCODE_UNKNOWN,KeyEvent.KEYCODE_VOICE_ASSIST, 292) // 292 = CMF key
                if(code !in common) return false
                if(detectedCode == -1 && code in common) { detectedCode = code; prefs.edit().putInt(PrefsManager.KEY_DETECTED_CODE, code).apply() }
            }

            // Long press detection
            if(pressCount == 0) {
                isLong = false
                longRunnable?.let{handler.removeCallbacks(it)}
                val longDur = prefs.getInt(PrefsManager.KEY_LONG_DURATION, 600).toLong()
                longRunnable = Runnable {
                    if(!isLong) { isLong = true; handleGesture("long"); pressCount=0; pending?.let{handler.removeCallbacks(it)} }
                }
                handler.postDelayed(longRunnable!!, longDur)
            }
            return true // Block Essential Space
        }

        if(event.action == KeyEvent.ACTION_UP) {
            val code = event.keyCode
            if(detectedCode != -1 && code != detectedCode) {
                val common = setOf(1018,1019,1020,1021,KeyEvent.KEYCODE_BOOKMARK,KeyEvent.KEYCODE_UNKNOWN,KeyEvent.KEYCODE_VOICE_ASSIST, 292)
                if(code !in common) return false
            }
            if(learningMode) return true

            longRunnable?.let{handler.removeCallbacks(it)}
            if(isLong) { isLong = false; return true }

            val now = System.currentTimeMillis()
            val doubleInterval = prefs.getInt(PrefsManager.KEY_DOUBLE_INTERVAL, 300).toLong()
            if(now - lastDown > doubleInterval + 400) pressCount = 0
            pressCount++
            lastDown = now

            pending?.let{handler.removeCallbacks(it)}
            val r = Runnable {
                if(!isLong) {
                    val gesture = when(pressCount) { 1->"single"; 2->"double"; 3->"triple"; else->"triple" }
                    handleGesture(gesture)
                }
                pressCount = 0
            }
            pending = r
            handler.postDelayed(r, doubleInterval + 60)
            return true
        }
        return false
    }

    private fun isExcluded(code:Int) = code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_POWER

    private fun handleGesture(gesture:String) {
        val prefs = PrefsManager.get(this)
        val stateAutomation = prefs.getBoolean(PrefsManager.KEY_STATE_AUTOMATION, false)
        var action = prefs.getString(PrefsManager.actionKey(gesture), defaultFor(gesture)) ?: defaultFor(gesture)

        // STATE-BASED AUTOMATION: different action when media playing
        if(stateAutomation && MediaNotificationService.isMediaPlaying) {
            val stateAction = prefs.getString(PrefsManager.stateActionKey(gesture), null)
            if(stateAction != null && stateAction != "same") {
                action = stateAction
            } else {
                // Default state behavior: if media playing, single = play/pause
                if(gesture == "single") action = "media"
            }
        }

        val intensity = prefs.getInt(PrefsManager.KEY_HAPTIC_INTENSITY, 70)
        HapticEngine.vibrate(this, intensity, HapticEngine.Type.MEDIUM)
        showOverlay("${gesture.uppercase()}: $action${if(MediaNotificationService.isMediaPlaying)" (Media)" else ""}")

        // Ensure wake lock for screen-off execution
        try { if(wakeLock?.isHeld == false) wakeLock?.acquire(5000) } catch(_:Exception){}

        ActionExecutor.execute(this, action, MediaNotificationService.isMediaPlaying)
    }

    private fun defaultFor(g:String) = when(g){ "single"->"flashlight"; "double"->"camera"; "triple"->"screenshot"; else->"assistant" }

    private fun showOverlay(text:String){
        val style = PrefsManager.get(this).getString(PrefsManager.KEY_OVERLAY_STYLE,"dot") ?: "dot"
        val intent = Intent(this, OverlayService::class.java).apply{ putExtra("text", text); putExtra("style", style) }
        try { if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent) } catch(_:Exception){}
    }
}
