
package com.nothing.essentialremapper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.content.ComponentName

// Notification Access (optional) - for state-based automation & media detection
class MediaNotificationService : NotificationListenerService() {
    companion object {
        var isMediaPlaying = false
        var currentPackage = ""
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateMediaState(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, ComponentName(this, this::class.java))
            updateMediaState(mediaSessionManager?.getActiveSessions(ComponentName(this, this::class.java)))
        } catch(e: Exception) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Used for notification-based triggers if needed
    }

    private fun updateMediaState(controllers: List<MediaController>?) {
        isMediaPlaying = controllers?.any { it.playbackState?.isActive == true || it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING } == true
        currentPackage = controllers?.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }?.packageName ?: ""
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try { mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener) } catch(e: Exception) {}
    }
}
