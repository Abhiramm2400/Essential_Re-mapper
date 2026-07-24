
package com.nothing.essentialremapper
import android.app.*, android.content.Intent, android.graphics.PixelFormat, android.os.*, android.view.*, android.widget.TextView, androidx.core.app.NotificationCompat
class OverlayService: Service(){
    private var wm:WindowManager?=null; private var view:TextView?=null; private val handler=Handler(Looper.getMainLooper())
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onCreate(){
        super.onCreate()
        val ch=NotificationChannel("overlay","Gesture Overlay",NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        startForeground(1, NotificationCompat.Builder(this,"overlay").setContentTitle("Essential Remapper Pro").setContentText("Active - Screen off supported").setSmallIcon(android.R.drawable.ic_media_play).setOngoing(true).build())
    }
    override fun onStartCommand(intent:Intent?, flags:Int, startId:Int):Int{
        val text=intent?.getStringExtra("text")?:"GESTURE"; val style=intent?.getStringExtra("style")?:"dot"
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) return START_NOT_STICKY
        wm=getSystemService(WINDOW_SERVICE) as WindowManager; view?.let{try{wm?.removeView(it)}catch(_:Exception){}}
        val tv=TextView(this).apply{ this.text=when(style){ "nothing"->"▎ $text"; else->"● $text" }; textSize=14f; setPadding(48,28,48,28); setTextColor(0xFF000000.toInt()); setBackgroundColor(0xE6FFFFFF.toInt()) }
        val params=WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL; y=180}
        try{wm?.addView(tv,params); view=tv; handler.postDelayed({try{wm?.removeView(tv)}catch(_:Exception){}; stopSelf()},1100)}catch(_:Exception){}
        return START_NOT_STICKY
    }
}
