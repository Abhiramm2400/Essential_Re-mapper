
package com.nothing.essentialremapper

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.loadUrl("file:///android_asset/index.html")
        // Start keep alive for screen-off support
        try { if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) startForegroundService(Intent(this, KeepAliveService::class.java)) else startService(Intent(this, KeepAliveService::class.java)) } catch(_:Exception){}
    }

    inner class AndroidBridge {
        @JavascriptInterface fun getDeviceInfo(): String {
            val info = DeviceDetector.detect(this@MainActivity)
            val json = JSONObject()
            json.put("manufacturer", info.manufacturer); json.put("model", info.model); json.put("isNothingFamily", info.isNothingFamily)
            json.put("displayName", info.displayName); json.put("osVersion", info.osVersion); json.put("hasEssentialKey", info.hasEssentialKey)
            json.put("hasGlyph", info.hasGlyph); json.put("detectedCode", info.detectedCode); json.put("detectedName", info.detectedName)
            json.put("accessibilityEnabled", info.accessibilityEnabled); json.put("notificationEnabled", info.notificationEnabled)
            return json.toString()
        }
        @JavascriptInterface fun saveAction(gesture:String, action:String){ PrefsManager.get(this@MainActivity).edit().putString(PrefsManager.actionKey(gesture), action).apply() }
        @JavascriptInterface fun saveTiming(type:String, value:Int){
            val prefs = PrefsManager.get(this@MainActivity)
            when(type){ "double"->prefs.edit().putInt(PrefsManager.KEY_DOUBLE_INTERVAL, value).apply(); "long"->prefs.edit().putInt(PrefsManager.KEY_LONG_DURATION, value).apply(); "triple"->prefs.edit().putInt("triple_interval", value).apply() }
        }
        @JavascriptInterface fun saveOverlayStyle(style:String){ PrefsManager.get(this@MainActivity).edit().putString(PrefsManager.KEY_OVERLAY_STYLE, style).apply() }
        @JavascriptInterface fun setStateAutomation(enabled:Boolean){ PrefsManager.get(this@MainActivity).edit().putBoolean(PrefsManager.KEY_STATE_AUTOMATION, enabled).apply() }
        @JavascriptInterface fun vibrate(duration:Int){ HapticEngine.vibrate(this@MainActivity, 70, HapticEngine.Type.MEDIUM) }
        @JavascriptInterface fun startLearningMode(){ PrefsManager.get(this@MainActivity).edit().putBoolean("learning_mode", true).apply() }
        @JavascriptInterface fun openAccessibility():Boolean{ try{ startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); return true } catch(e:Exception){ return false } }
        @JavascriptInterface fun openNotificationAccess():Boolean{ try{ startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); return true } catch(e:Exception){ return false } }
        @JavascriptInterface fun openOverlayPermission():Boolean{ try{ startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)); return true } catch(e:Exception){ return false } }
    }
    override fun onBackPressed(){ if(webView.canGoBack()) webView.goBack() else super.onBackPressed() }
}
