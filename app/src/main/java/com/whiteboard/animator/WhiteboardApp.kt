package com.whiteboard.animator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Whiteboard Animator Pro.
 * 
 * Initializes core components:
 * - Hilt dependency injection
 * - WorkManager for background video export
 * - Notification channels for export progress
 */
@HiltAndroidApp
class WhiteboardApp : Application(), Configuration.Provider {
    
    companion object {
        // Notification channel IDs
        const val CHANNEL_EXPORT_PROGRESS = "export_progress"
        const val CHANNEL_EXPORT_COMPLETE = "export_complete"
        
        // Notification IDs
        const val NOTIFICATION_EXPORT_PROGRESS = 1001
        const val NOTIFICATION_EXPORT_COMPLETE = 1002
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
    
    /**
     * WorkManager configuration with custom initialization.
     * Disables default initializer in AndroidManifest.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    
    /**
     * Creates notification channels for export progress and completion.
     * Required for Android 8.0+ (API 26+).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Export progress channel (ongoing notification)
            val progressChannel = NotificationChannel(
                CHANNEL_EXPORT_PROGRESS,
                getString(R.string.app_name) + " Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows video export progress"
                setShowBadge(false)
            }
            
            // Export complete channel (alert notification)
            val completeChannel = NotificationChannel(
                CHANNEL_EXPORT_COMPLETE,
                getString(R.string.app_name) + " Complete",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when video export is complete"
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(progressChannel, completeChannel)
            )
        }
    }
}
