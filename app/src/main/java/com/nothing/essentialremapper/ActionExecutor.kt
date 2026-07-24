
package com.nothing.essentialremapper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.Path
import android.hardware.camera2.CameraManager
import android.os.Build
import android.accessibilityservice.GestureDescription
import android.provider.MediaStore

object ActionExecutor {
    var serviceRef: AccessibilityService? = null
    private var torchOn = false

    fun execute(context: Context, actionId: String, isMediaPlaying: Boolean = false) {
        // State-based automation: if media playing, use different logic
        if(isMediaPlaying && actionId == "flashlight") {
            // Example: when media playing, single click = play/pause instead of torch
            // User can configure via stateActionKey
            // For now we handle in EssentialButtonService, this is fallback
        }

        when(actionId) {
            "flashlight" -> toggleTorch(context)
            "screenshot" -> serviceRef?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            "camera" -> launch(context, Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            "lock" -> serviceRef?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            "recents" -> serviceRef?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            "notification" -> serviceRef?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            "qs" -> serviceRef?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            "assistant" -> launch(context, Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "dnd" -> toggleDND(context)
            "media" -> dispatchMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "next" -> dispatchMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            "prev" -> dispatchMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "back" -> serviceRef?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            "home" -> serviceRef?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            "swipe_down" -> dispatchSwipeDown()
            "swipe_up" -> dispatchSwipeUp()
            "essential_space" -> launchPackage(context, "com.nothing.essential")
            "chatgpt" -> launchPackage(context, "com.openai.chatgpt") ?: launchPackage(context, "com.google.android.apps.bard")
            "none" -> {}
            else -> {
                // Try as package name OR shortcut
                if(actionId.contains(".")) {
                    if(!launchShortcut(context, actionId)) {
                        launchPackage(context, actionId)
                    }
                } else {
                    launchPackage(context, actionId)
                }
            }
        }
    }

    // Foreground Services + Intent launching + Shortcuts API
    private fun launchShortcut(context: Context, shortcutIdOrPackage: String): Boolean {
        return try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val sm = context.getSystemService(ShortcutManager::class.java)
                // Try to find shortcut by id
                val shortcuts = sm?.manifestShortcuts ?: emptyList()
                // For custom URL / deep link, treat as intent
                if(shortcutIdOrPackage.contains("://")) {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(shortcutIdOrPackage)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            }
            false
        } catch(e: Exception) { false }
    }

    private fun launch(context: Context, i: Intent) { try { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch(_: Exception){} }
    private fun launchPackage(context: Context, pkg: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch(_: Exception){ false }
    }

    private fun toggleTorch(context: Context) {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList[0]
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
        } catch(_: Exception){}
    }

    private fun toggleDND(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if(nm.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE)
            } else {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch(_: Exception){}
    }

    private fun dispatchMediaKey(context: Context, keyCode: Int) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val eventDown = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
            am.dispatchMediaKeyEvent(eventDown)
            am.dispatchMediaKeyEvent(eventUp)
        } catch(_: Exception){}
    }

    // AccessibilityService.dispatchGesture() - for swipe gestures
    private fun dispatchSwipeDown() {
        try {
            val path = Path().apply { moveTo(500f, 200f); lineTo(500f, 1200f) }
            val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 300)).build()
            serviceRef?.dispatchGesture(gesture, null, null)
        } catch(_: Exception){}
    }
    private fun dispatchSwipeUp() {
        try {
            val path = Path().apply { moveTo(500f, 1200f); lineTo(500f, 200f) }
            val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 300)).build()
            serviceRef?.dispatchGesture(gesture, null, null)
        } catch(_: Exception){}
    }
}
