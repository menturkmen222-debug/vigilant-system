package com.whiteboard.animator.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.whiteboard.animator.data.model.*

/**
 * Main Room Database for Whiteboard Animator Pro.
 * Version 2: Added CharacterReference entity for V2 features.
 */
@Database(
    entities = [
        Project::class,
        Scene::class,
        SceneAsset::class,
        CachedAsset::class,
        DrawingPath::class,
        TextOverlay::class,
        CharacterReference::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // DAOs
    abstract fun projectDao(): ProjectDao
    abstract fun sceneDao(): SceneDao
    abstract fun sceneAssetDao(): SceneAssetDao
    abstract fun cachedAssetDao(): CachedAssetDao
    abstract fun drawingPathDao(): DrawingPathDao
    abstract fun textOverlayDao(): TextOverlayDao
    abstract fun characterRefDao(): CharacterRefDao
    
    companion object {
        const val DATABASE_NAME = "whiteboard_animator_db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Migration from version 1 to 2: Add character_references table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS character_references (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        imagePath TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_character_references_projectId ON character_references(projectId)")
            }
        }
    }
}

/**
 * Type Converters for Room to handle enum types and other complex objects.
 */
class Converters {
    // ---- GenerationMode ----
    @TypeConverter
    fun fromGenerationMode(value: GenerationMode): String = value.name
    
    @TypeConverter
    fun toGenerationMode(value: String): GenerationMode = 
        try { GenerationMode.valueOf(value) } catch (e: Exception) { GenerationMode.OFFLINE }
    
    // ---- BackgroundType ----
    @TypeConverter
    fun fromBackgroundType(value: BackgroundType): String = value.name
    
    @TypeConverter
    fun toBackgroundType(value: String): BackgroundType = 
        try { BackgroundType.valueOf(value) } catch (e: Exception) { BackgroundType.WHITEBOARD }
    
    // ---- HandStyle ----
    @TypeConverter
    fun fromHandStyle(value: HandStyle): String = value.name
    
    @TypeConverter
    fun toHandStyle(value: String): HandStyle = 
        try { HandStyle.valueOf(value) } catch (e: Exception) { HandStyle.WRITING }
    
    // ---- AspectRatio ----
    @TypeConverter
    fun fromAspectRatio(value: AspectRatio): String = value.name
    
    @TypeConverter
    fun toAspectRatio(value: String): AspectRatio = 
        try { AspectRatio.valueOf(value) } catch (e: Exception) { AspectRatio.HORIZONTAL_16_9 }
    
    // ---- Resolution ----
    @TypeConverter
    fun fromResolution(value: Resolution): String = value.name
    
    @TypeConverter
    fun toResolution(value: String): Resolution = 
        try { Resolution.valueOf(value) } catch (e: Exception) { Resolution.HD_720P }
    
    // ---- VoiceSource ----
    @TypeConverter
    fun fromVoiceSource(value: VoiceSource): String = value.name
    
    @TypeConverter
    fun toVoiceSource(value: String): VoiceSource = 
        try { VoiceSource.valueOf(value) } catch (e: Exception) { VoiceSource.DEVICE_TTS }
    
    // ---- TransitionStyle ----
    @TypeConverter
    fun fromTransitionStyle(value: TransitionStyle): String = value.name
    
    @TypeConverter
    fun toTransitionStyle(value: String): TransitionStyle = 
        try { TransitionStyle.valueOf(value) } catch (e: Exception) { TransitionStyle.FADE }
    
    // ---- SceneStatus ----
    @TypeConverter
    fun fromSceneStatus(value: SceneStatus): String = value.name
    
    @TypeConverter
    fun toSceneStatus(value: String): SceneStatus = 
        try { SceneStatus.valueOf(value) } catch (e: Exception) { SceneStatus.PENDING }
    
    // ---- AssetType ----
    @TypeConverter
    fun fromAssetType(value: AssetType): String = value.name
    
    @TypeConverter
    fun toAssetType(value: String): AssetType = 
        try { AssetType.valueOf(value) } catch (e: Exception) { AssetType.DOODLE }
    
    // ---- AnimationStyle ----
    @TypeConverter
    fun fromAnimationStyle(value: AnimationStyle): String = value.name
    
    @TypeConverter
    fun toAnimationStyle(value: String): AnimationStyle = 
        try { AnimationStyle.valueOf(value) } catch (e: Exception) { AnimationStyle.HAND_DRAW }
    
    // ---- VideoStyle ----
    @TypeConverter
    fun fromVideoStyle(value: VideoStyle): String = value.name
    
    @TypeConverter
    fun toVideoStyle(value: String): VideoStyle = 
        try { VideoStyle.valueOf(value) } catch (e: Exception) { VideoStyle.WHITEBOARD }
    
    // ---- CharacterType ----
    @TypeConverter
    fun fromCharacterType(value: CharacterType): String = value.name
    
    @TypeConverter
    fun toCharacterType(value: String): CharacterType = 
        try { CharacterType.valueOf(value) } catch (e: Exception) { CharacterType.PERSON }
    
    // ---- List<Float> for drawing points ----
    @TypeConverter
    fun fromFloatList(value: List<Float>): String = value.joinToString(",")
    
    @TypeConverter
    fun toFloatList(value: String): List<Float> = 
        if (value.isEmpty()) emptyList() else value.split(",").map { it.toFloat() }
}
