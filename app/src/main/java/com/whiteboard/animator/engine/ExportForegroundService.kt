package com.whiteboard.animator.engine

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.whiteboard.animator.MainActivity
import com.whiteboard.animator.R
import com.whiteboard.animator.WhiteboardApp

/**
 * Foreground Service for video export.
 * 
 * Keeps the app alive during long export operations and shows progress notification.
 */
class ExportForegroundService : Service() {
    
    companion object {
        const val ACTION_START = "com.whiteboard.animator.action.START_EXPORT"
        const val ACTION_STOP = "com.whiteboard.animator.action.STOP_EXPORT"
        const val ACTION_UPDATE_PROGRESS = "com.whiteboard.animator.action.UPDATE_PROGRESS"
        
        const val EXTRA_PROJECT_NAME = "project_name"
        const val EXTRA_PROGRESS = "progress"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val projectName = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: "Video"
                startForeground(
                    WhiteboardApp.NOTIFICATION_EXPORT_PROGRESS,
                    createProgressNotification(projectName, 0)
                )
            }
            ACTION_UPDATE_PROGRESS -> {
                val projectName = intent.getStringExtra(EXTRA_PROJECT_NAME) ?: "Video"
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                updateNotification(projectName, progress)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun createProgressNotification(projectName: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, WhiteboardApp.CHANNEL_EXPORT_PROGRESS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Exporting $projectName")
            .setContentText("$progress% complete")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun updateNotification(projectName: String, progress: Int) {
        val notification = createProgressNotification(projectName, progress)
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager.notify(WhiteboardApp.NOTIFICATION_EXPORT_PROGRESS, notification)
    }
}
