package com.whiteboard.animator.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// ============================================================================
// ENUMS - Type-safe configuration options
// ============================================================================

/**
 * Generation mode for the app.
 * OFFLINE: All processing done locally, no internet required
 * ONLINE: AI-enhanced processing with Hugging Face, ElevenLabs, etc.
 */
enum class GenerationMode {
    OFFLINE,
    ONLINE
}

/**
 * Background style options for scenes.
 */
enum class BackgroundType {
    WHITEBOARD,    // Classic white background
    CHALKBOARD,    // Green/black chalkboard style
    PAPER,         // Textured paper look
    CUSTOM         // User-provided image
}

/**
 * Hand styles for drawing animation.
 */
enum class HandStyle {
    POINTING,      // Pointing finger
    WRITING,       // Hand holding pen
    CARTOON,       // Cartoon-style hand
    MARKER,        // Hand with marker
    NONE           // No hand shown
}

/**
 * Video aspect ratios for export.
 */
enum class AspectRatio {
    HORIZONTAL_16_9,  // YouTube, landscape
    VERTICAL_9_16,    // TikTok, Instagram Stories
    SQUARE_1_1        // Instagram feed
}

/**
 * Video resolution options.
 */
enum class Resolution(val width: Int, val height: Int) {
    SD_480P(854, 480),
    HD_720P(1280, 720),
    FHD_1080P(1920, 1080)
}

/**
 * Export format options.
 */
enum class ExportFormat {
    MP4,           // Standard video
    GIF,           // Animated GIF
    PNG_SEQUENCE   // Image sequence
}

/**
 * Animation styles for assets appearing on screen.
 */
enum class AnimationStyle {
    HAND_DRAW,     // Appears as if being drawn by hand
    FADE_IN,       // Fade from transparent
    FADE_OUT,      // Fade to transparent
    ZOOM_IN,       // Scale up from center
    ZOOM_OUT,      // Scale down from center
    SLIDE_LEFT,    // Slide from right to left
    SLIDE_RIGHT,   // Slide from left to right
    SLIDE_UP,      // Slide from bottom to top
    SLIDE_DOWN,    // Slide from top to bottom
    POP,           // Quick scale pop effect
    NONE           // Instant appearance
}

/**
 * Image placement positions on canvas.
 */
enum class ImagePlacement {
    CENTER,
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    LEFT_CENTER,
    RIGHT_CENTER,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    CUSTOM         // User-defined position
}

/**
 * Scene transition styles.
 */
enum class TransitionStyle {
    NONE,          // Instant cut
    FADE,          // Cross-fade
    SLIDE,         // Slide horizontally
    ZOOM,          // Zoom transition
    WIPE           // Wipe effect
}

/**
 * Asset types in the library.
 */
enum class AssetType {
    HAND,          // Hand images for drawing effect
    BACKGROUND,    // Background images
    DOODLE,        // Doodles and illustrations
    ICON,          // Icons and symbols
    PERSON,        // People and characters
    SHAPE,         // Basic shapes
    USER_IMAGE,    // User-imported images
    AI_GENERATED   // AI-generated images
}

/**
 * TTS voice source.
 */
enum class VoiceSource {
    DEVICE_TTS,    // Android built-in TTS
    ELEVENLABS,    // ElevenLabs API
    AZURE,         // Microsoft Azure TTS
    GOOGLE_CLOUD   // Google Cloud TTS
}

/**
 * Video visual style presets.
 */
enum class VideoStyle {
    WHITEBOARD,     // Classic white background with black lines
    CARTOON,        // Colorful cartoon style
    REALISTIC,      // Realistic doodle/sketch style
    CHALKBOARD,     // Chalkboard green with white lines
    MINIMAL         // Minimal line art
}

/**
 * API providers for online features.
 */
enum class ApiProvider {
    GEMINI,         // Google Gemini API
    OPENAI,         // OpenAI GPT
    HUGGINGFACE,    // Hugging Face Inference
    ELEVENLABS,     // ElevenLabs TTS
    AZURE_TTS       // Microsoft Azure TTS
}

/**
 * Character reference types for consistency.
 */
enum class CharacterType {
    PERSON,         // Human character
    ANIMAL,         // Animal character
    MASCOT,         // Brand mascot
    OBJECT          // Recurring object
}

/**
 * Scene generation/processing status.
 */
enum class SceneStatus {
    PENDING,        // Awaiting generation
    GENERATING,     // Currently being processed
    COMPLETE,       // Ready for preview/export
    ERROR           // Generation failed
}

// ============================================================================
// ROOM ENTITIES - Database tables
// ============================================================================

/**
 * Project entity - Top-level container for animation projects.
 * 
 * Contains all project metadata and default settings.
 * Scenes are linked via foreign key.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Basic info
    val name: String,
    val description: String = "",
    val script: String = "",
    
    // Settings
    val mode: GenerationMode = GenerationMode.OFFLINE,
    val backgroundType: BackgroundType = BackgroundType.WHITEBOARD,
    val customBackgroundPath: String? = null,
    val handStyle: HandStyle = HandStyle.WRITING,
    val defaultTransition: TransitionStyle = TransitionStyle.FADE,
    
    // Export defaults
    val aspectRatio: AspectRatio = AspectRatio.HORIZONTAL_16_9,
    val resolution: Resolution = Resolution.HD_720P,
    val fps: Int = 24,
    
    // Voice settings
    val voiceSource: VoiceSource = VoiceSource.DEVICE_TTS,
    val voiceId: String? = null,  // Device TTS voice name or API voice ID
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    
    // Video style
    val videoStyle: VideoStyle = VideoStyle.WHITEBOARD,
    
    // Template support
    val isTemplate: Boolean = false,
    val templateCategory: String? = null,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Thumbnail (generated after first scene)
    val thumbnailPath: String? = null
)

/**
 * Scene entity - Individual animated scene within a project.
 * 
 * Each scene contains text, timing, and references to assets.
 * Ordered by 'orderIndex' for timeline display.
 */
@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class Scene(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val projectId: Long,
    
    // Scene content
    val text: String,
    val orderIndex: Int,
    
    // Timing (milliseconds)
    val durationMs: Long = 5000,
    val transitionDurationMs: Long = 500,
    val transitionStyle: TransitionStyle = TransitionStyle.FADE,
    
    // Voice
    val voiceoverPath: String? = null,
    val voiceoverDurationMs: Long = 0,
    val showSubtitles: Boolean = true,
    
    // Visual settings
    val backgroundColor: Int? = null,  // ARGB color, null = use project default
    val backgroundImagePath: String? = null,
    val handStyle: HandStyle? = null,  // null = use project default
    
    // Animation settings
    val textAnimationStyle: AnimationStyle = AnimationStyle.HAND_DRAW,
    val textRevealSpeed: Float = 1.0f,  // Characters per second multiplier
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * SceneAsset entity - Assets (images, doodles) placed in a scene.
 * 
 * Defines position, animation, and timing for each asset.
 */
@Entity(
    tableName = "scene_assets",
    foreignKeys = [
        ForeignKey(
            entity = Scene::class,
            parentColumns = ["id"],
            childColumns = ["sceneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sceneId")]
)
data class SceneAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sceneId: Long,
    
    // Asset reference
    val assetType: AssetType,
    val assetPath: String,  // Drawable resource name, file path, or URL
    val isBuiltIn: Boolean = true,  // true = bundled asset, false = user/AI generated
    
    // Position and size
    val placement: ImagePlacement = ImagePlacement.CENTER,
    val customX: Float = 0f,  // Only used if placement == CUSTOM
    val customY: Float = 0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    
    // Animation
    val animationStyle: AnimationStyle = AnimationStyle.FADE_IN,
    val animationDelayMs: Long = 0,  // Delay from scene start
    val animationDurationMs: Long = 1000,
    
    // Layer ordering (higher = on top)
    val layerOrder: Int = 0,
    
    // Visibility
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * CachedAsset entity - Metadata for cached online assets.
 * 
 * Tracks downloaded AI-generated images, TTS audio, etc.
 * Used for offline access to previously generated content.
 */
@Entity(
    tableName = "cached_assets",
    indices = [Index("sourceUrl", unique = true)]
)
data class CachedAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Source info
    val sourceType: String,  // "huggingface", "elevenlabs", etc.
    val sourceUrl: String,   // Original API endpoint or identifier
    val prompt: String? = null,  // For AI-generated content
    
    // Local storage
    val localPath: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    
    // Metadata
    val keywords: String = "",  // Comma-separated keywords for search
    val metadata: String = "",  // JSON metadata
    
    // Usage tracking
    val useCount: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis(),
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * DrawingPath entity - Freehand drawing strokes on canvas.
 * 
 * Stores vector paths for user-drawn content.
 */
@Entity(
    tableName = "drawing_paths",
    foreignKeys = [
        ForeignKey(
            entity = Scene::class,
            parentColumns = ["id"],
            childColumns = ["sceneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sceneId")]
)
data class DrawingPath(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sceneId: Long,
    
    // Path data (serialized as JSON)
    val pathData: String,  // JSON array of points: [{x, y, pressure}]
    
    // Style
    val strokeColor: Int,      // ARGB color
    val strokeWidth: Float,
    val isEraser: Boolean = false,
    
    // Layer ordering
    val layerOrder: Int = 0,
    
    // Animation
    val animationStyle: AnimationStyle = AnimationStyle.HAND_DRAW,
    val animationDelayMs: Long = 0,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * TextOverlay entity - Text elements placed on canvas.
 */
@Entity(
    tableName = "text_overlays",
    foreignKeys = [
        ForeignKey(
            entity = Scene::class,
            parentColumns = ["id"],
            childColumns = ["sceneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sceneId")]
)
data class TextOverlay(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sceneId: Long,
    
    // Content
    val text: String,
    
    // Position
    val positionX: Float,
    val positionY: Float,
    val rotation: Float = 0f,
    
    // Style
    val fontFamily: String = "sans-serif",
    val fontSize: Float = 24f,
    val fontColor: Int = 0xFF212121.toInt(),  // Dark gray
    val fontWeight: Int = 400,  // 400 = normal, 700 = bold
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    
    // Background
    val hasBackground: Boolean = false,
    val backgroundColor: Int = 0x80FFFFFF.toInt(),
    val backgroundPadding: Float = 8f,
    val backgroundCornerRadius: Float = 4f,
    
    // Animation
    val animationStyle: AnimationStyle = AnimationStyle.HAND_DRAW,
    val animationDelayMs: Long = 0,
    
    // Layer ordering
    val layerOrder: Int = 0,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================================================
// DATA CLASSES - Non-entity models
// ============================================================================

/**
 * Settings stored in DataStore.
 */
@Serializable
data class AppSettings(
    val mode: GenerationMode = GenerationMode.OFFLINE,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val defaultResolution: Resolution = Resolution.HD_720P,
    val defaultFps: Int = 24,
    val defaultVoiceId: String? = null,
    val huggingFaceApiKey: String = "",
    val elevenLabsApiKey: String = "",
    val azureTtsKey: String = "",
    val azureTtsRegion: String = "",
    val hasSeenOnboarding: Boolean = false
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Generation progress state for UI.
 */
data class GenerationProgress(
    val stage: GenerationStage = GenerationStage.IDLE,
    val progress: Float = 0f,
    val message: String = "",
    val error: String? = null
)

enum class GenerationStage {
    IDLE,
    SPLITTING_SCRIPT,
    GENERATING_ILLUSTRATIONS,
    GENERATING_VOICEOVER,
    ASSEMBLING_TIMELINE,
    COMPLETE,
    ERROR
}

/**
 * Export progress state for UI.
 */
data class ExportProgress(
    val status: ExportStatus = ExportStatus.IDLE,
    val progress: Float = 0f,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val outputPath: String? = null,
    val error: String? = null
)

enum class ExportStatus {
    IDLE,
    PREPARING,
    ENCODING_VIDEO,
    ENCODING_AUDIO,
    MUXING,
    COMPLETE,
    ERROR,
    CANCELLED
}

/**
 * Built-in asset metadata.
 */
data class BuiltInAsset(
    val id: String,
    val name: String,
    val type: AssetType,
    val resourceName: String,
    val keywords: List<String>,
    val category: String
)

/**
 * Point data for drawing paths.
 */
@Serializable
data class PathPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

/**
 * Project with all related data for export/preview.
 */
data class ProjectWithScenes(
    val project: Project,
    val scenes: List<SceneWithAssets>
)

/**
 * Scene with all its assets and drawings.
 */
data class SceneWithAssets(
    val scene: Scene,
    val assets: List<SceneAsset>,
    val drawings: List<DrawingPath>,
    val textOverlays: List<TextOverlay>
)

// ============================================================================
// CHARACTER REFERENCE - For consistency across scenes
// ============================================================================

/**
 * Character reference for maintaining consistency.
 * User uploads a reference image that is reused across scenes.
 */
@Entity(
    tableName = "character_references",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class CharacterReference(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val name: String,                    // e.g., "Main Character", "Robot Mascot"
    val type: CharacterType,
    val imagePath: String,               // Local path to uploaded reference image
    val description: String = "",        // Used in AI prompts
    val createdAt: Long = System.currentTimeMillis()
)
