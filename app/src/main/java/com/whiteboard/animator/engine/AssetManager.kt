package com.whiteboard.animator.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.whiteboard.animator.data.model.Project
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun loadBuiltInBitmap(name: String): Bitmap? {
        // Map simplified name to drawable resource
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId == 0) return null
        return BitmapFactory.decodeResource(context.resources, resId)
    }

    fun loadBitmapFromPath(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                // Try as URI if applicable, though usually path is absolute path for local files
                // Fallback for demonstration
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getProjectVoiceoverDir(projectId: Long): File {
        val dir = File(context.filesDir, "projects/$projectId/voiceovers")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun getProjectImagesDir(projectId: Long): File {
        val dir = File(context.filesDir, "projects/$projectId/images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
