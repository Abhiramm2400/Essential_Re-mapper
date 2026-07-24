
package com.nothing.essentialremapper
import android.app.*, android.content.Intent, android.os.*, androidx.core.app.NotificationCompat
// Foreground service to keep remapper alive when screen is OFF
class KeepAliveService: Service(){
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onCreate(){
        super.onCreate()
        val ch=NotificationChannel("keepalive","Keep Alive",NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        val notif = NotificationCompat.Builder(this,"keepalive").setContentTitle("Essential Remapper Active").setContentText("Works with screen on/off").setSmallIcon(android.R.drawable.ic_lock_idle_lock).setOngoing(true).build()
        startForeground(2, notif)
    }
    override fun onStartCommand(intent:Intent?, flags:Int, startId:Int):Int{
        // Acquire partial wake lock briefly to ensure key events still work screen off
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EssentialRemapper:ScreenOff")
            wl.acquire(10*60*1000L) // 10 min, will be re-acquired on next press
        } catch(_:Exception){}
        return START_STICKY
    }
}
