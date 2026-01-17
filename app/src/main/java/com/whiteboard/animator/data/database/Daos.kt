package com.whiteboard.animator.data.database

import androidx.room.*
import com.whiteboard.animator.data.model.*
import kotlinx.coroutines.flow.Flow

// ============================================================================
// PROJECT DAO
// ============================================================================

/**
 * Data Access Object for Project entity.
 * Provides CRUD operations and queries for projects.
 */
@Dao
interface ProjectDao {
    
    // ---- Insert ----
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long
    
    // ---- Update ----
    
    @Update
    suspend fun updateProject(project: Project)
    
    @Query("UPDATE projects SET updatedAt = :timestamp WHERE id = :projectId")
    suspend fun updateTimestamp(projectId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE projects SET name = :name, updatedAt = :timestamp WHERE id = :projectId")
    suspend fun updateName(projectId: Long, name: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE projects SET script = :script, updatedAt = :timestamp WHERE id = :projectId")
    suspend fun updateScript(projectId: Long, script: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE projects SET thumbnailPath = :path WHERE id = :projectId")
    suspend fun updateThumbnail(projectId: Long, path: String?)
    
    // ---- Delete ----
    
    @Delete
    suspend fun deleteProject(project: Project)
    
    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)
    
    // ---- Query (Flow for reactive updates) ----
    
    @Query("SELECT * FROM projects WHERE isTemplate = 0 ORDER BY updatedAt DESC")
    fun getAllProjectsFlow(): Flow<List<Project>>
    
    @Query("SELECT * FROM projects WHERE isTemplate = 1 ORDER BY templateCategory, name")
    fun getAllTemplatesFlow(): Flow<List<Project>>
    
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectFlow(projectId: Long): Flow<Project?>
    
    @Query("SELECT * FROM projects WHERE name LIKE '%' || :query || '%' AND isTemplate = 0 ORDER BY updatedAt DESC")
    fun searchProjectsFlow(query: String): Flow<List<Project>>
    
    // ---- Query (Suspend for one-time fetch) ----
    
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProject(projectId: Long): Project?
    
    @Query("SELECT * FROM projects WHERE isTemplate = 0 ORDER BY updatedAt DESC")
    suspend fun getAllProjects(): List<Project>
    
    @Query("SELECT * FROM projects WHERE isTemplate = 1")
    suspend fun getAllTemplates(): List<Project>
    
    @Query("SELECT COUNT(*) FROM projects WHERE isTemplate = 0")
    suspend fun getProjectCount(): Int
}

// ============================================================================
// SCENE DAO
// ============================================================================

/**
 * Data Access Object for Scene entity.
 */
@Dao
interface SceneDao {
    
    // ---- Insert ----
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: Scene): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<Scene>): List<Long>
    
    // ---- Update ----
    
    @Update
    suspend fun updateScene(scene: Scene)
    
    @Query("UPDATE scenes SET orderIndex = :orderIndex, updatedAt = :timestamp WHERE id = :sceneId")
    suspend fun updateOrder(sceneId: Long, orderIndex: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE scenes SET text = :text, updatedAt = :timestamp WHERE id = :sceneId")
    suspend fun updateText(sceneId: Long, text: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE scenes SET durationMs = :durationMs, updatedAt = :timestamp WHERE id = :sceneId")
    suspend fun updateDuration(sceneId: Long, durationMs: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE scenes SET voiceoverPath = :path, voiceoverDurationMs = :durationMs WHERE id = :sceneId")
    suspend fun updateVoiceover(sceneId: Long, path: String?, durationMs: Long)
    
    // ---- Delete ----
    
    @Delete
    suspend fun deleteScene(scene: Scene)
    
    @Query("DELETE FROM scenes WHERE id = :sceneId")
    suspend fun deleteSceneById(sceneId: Long)
    
    @Query("DELETE FROM scenes WHERE projectId = :projectId")
    suspend fun deleteScenesByProject(projectId: Long)
    
    // ---- Query (Flow) ----
    
    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY orderIndex")
    fun getScenesFlow(projectId: Long): Flow<List<Scene>>
    
    @Query("SELECT * FROM scenes WHERE id = :sceneId")
    fun getSceneFlow(sceneId: Long): Flow<Scene?>
    
    // ---- Query (Suspend) ----
    
    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY orderIndex")
    suspend fun getScenes(projectId: Long): List<Scene>
    
    @Query("SELECT * FROM scenes WHERE id = :sceneId")
    suspend fun getScene(sceneId: Long): Scene?
    
    @Query("SELECT COUNT(*) FROM scenes WHERE projectId = :projectId")
    suspend fun getSceneCount(projectId: Long): Int
    
    @Query("SELECT MAX(orderIndex) FROM scenes WHERE projectId = :projectId")
    suspend fun getMaxOrderIndex(projectId: Long): Int?
    
    @Query("SELECT SUM(durationMs) FROM scenes WHERE projectId = :projectId")
    suspend fun getTotalDuration(projectId: Long): Long?
}

// ============================================================================
// SCENE ASSET DAO
// ============================================================================

/**
 * Data Access Object for SceneAsset entity.
 */
@Dao
interface SceneAssetDao {
    
    // ---- Insert ----
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: SceneAsset): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<SceneAsset>): List<Long>
    
    // ---- Update ----
    
    @Update
    suspend fun updateAsset(asset: SceneAsset)
    
    @Query("UPDATE scene_assets SET layerOrder = :layerOrder WHERE id = :assetId")
    suspend fun updateLayerOrder(assetId: Long, layerOrder: Int)
    
    @Query("UPDATE scene_assets SET isVisible = :isVisible WHERE id = :assetId")
    suspend fun updateVisibility(assetId: Long, isVisible: Boolean)
    
    // ---- Delete ----
    
    @Delete
    suspend fun deleteAsset(asset: SceneAsset)
    
    @Query("DELETE FROM scene_assets WHERE id = :assetId")
    suspend fun deleteAssetById(assetId: Long)
    
    @Query("DELETE FROM scene_assets WHERE sceneId = :sceneId")
    suspend fun deleteAssetsByScene(sceneId: Long)
    
    // ---- Query (Flow) ----
    
    @Query("SELECT * FROM scene_assets WHERE sceneId = :sceneId ORDER BY layerOrder")
    fun getAssetsFlow(sceneId: Long): Flow<List<SceneAsset>>
    
    // ---- Query (Suspend) ----
    
    @Query("SELECT * FROM scene_assets WHERE sceneId = :sceneId ORDER BY layerOrder")
    suspend fun getAssets(sceneId: Long): List<SceneAsset>
    
    @Query("SELECT * FROM scene_assets WHERE id = :assetId")
    suspend fun getAsset(assetId: Long): SceneAsset?
    
    @Query("SELECT MAX(layerOrder) FROM scene_assets WHERE sceneId = :sceneId")
    suspend fun getMaxLayerOrder(sceneId: Long): Int?
}

// ============================================================================
// CACHED ASSET DAO
// ============================================================================

/**
 * Data Access Object for CachedAsset entity.
 * Manages cached online assets (AI images, TTS audio).
 */
@Dao
interface CachedAssetDao {
    
    // ---- Insert ----
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedAsset(asset: CachedAsset): Long
    
    // ---- Update ----
    
    @Query("UPDATE cached_assets SET useCount = useCount + 1, lastUsedAt = :timestamp WHERE id = :assetId")
    suspend fun incrementUseCount(assetId: Long, timestamp: Long = System.currentTimeMillis())
    
    // ---- Delete ----
    
    @Delete
    suspend fun deleteCachedAsset(asset: CachedAsset)
    
    @Query("DELETE FROM cached_assets WHERE id = :assetId")
    suspend fun deleteCachedAssetById(assetId: Long)
    
    @Query("DELETE FROM cached_assets WHERE lastUsedAt < :beforeTimestamp")
    suspend fun deleteOldAssets(beforeTimestamp: Long): Int
    
    // ---- Query ----
    
    @Query("SELECT * FROM cached_assets WHERE sourceUrl = :url LIMIT 1")
    suspend fun findBySourceUrl(url: String): CachedAsset?
    
    @Query("SELECT * FROM cached_assets WHERE keywords LIKE '%' || :keyword || '%'")
    suspend fun searchByKeyword(keyword: String): List<CachedAsset>
    
    @Query("SELECT * FROM cached_assets ORDER BY lastUsedAt DESC")
    fun getAllCachedAssetsFlow(): Flow<List<CachedAsset>>
    
    @Query("SELECT SUM(fileSizeBytes) FROM cached_assets")
    suspend fun getTotalCacheSize(): Long?
}

// ============================================================================
// DRAWING PATH DAO
// ============================================================================

/**
 * Data Access Object for DrawingPath entity.
 */
@Dao
interface DrawingPathDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPath(path: DrawingPath): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaths(paths: List<DrawingPath>): List<Long>
    
    @Delete
    suspend fun deletePath(path: DrawingPath)
    
    @Query("DELETE FROM drawing_paths WHERE sceneId = :sceneId")
    suspend fun deletePathsByScene(sceneId: Long)
    
    @Query("SELECT * FROM drawing_paths WHERE sceneId = :sceneId ORDER BY layerOrder")
    fun getPathsFlow(sceneId: Long): Flow<List<DrawingPath>>
    
    @Query("SELECT * FROM drawing_paths WHERE sceneId = :sceneId ORDER BY layerOrder")
    suspend fun getPaths(sceneId: Long): List<DrawingPath>
}

// ============================================================================
// TEXT OVERLAY DAO
// ============================================================================

/**
 * Data Access Object for TextOverlay entity.
 */
@Dao
interface TextOverlayDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverlay(overlay: TextOverlay): Long
    
    @Update
    suspend fun updateOverlay(overlay: TextOverlay)
    
    @Delete
    suspend fun deleteOverlay(overlay: TextOverlay)
    
    @Query("DELETE FROM text_overlays WHERE sceneId = :sceneId")
    suspend fun deleteOverlaysByScene(sceneId: Long)
    
    @Query("SELECT * FROM text_overlays WHERE sceneId = :sceneId ORDER BY layerOrder")
    fun getOverlaysFlow(sceneId: Long): Flow<List<TextOverlay>>
    
    @Query("SELECT * FROM text_overlays WHERE sceneId = :sceneId ORDER BY layerOrder")
    suspend fun getOverlays(sceneId: Long): List<TextOverlay>
}

// ============================================================================
// CHARACTER REFERENCE DAO
// ============================================================================

/**
 * Data Access Object for CharacterReference entity.
 * Manages character references for consistency across scenes.
 */
@Dao
interface CharacterRefDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterReference): Long
    
    @Update
    suspend fun updateCharacter(character: CharacterReference)
    
    @Delete
    suspend fun deleteCharacter(character: CharacterReference)
    
    @Query("DELETE FROM character_references WHERE id = :characterId")
    suspend fun deleteCharacterById(characterId: Long)
    
    @Query("DELETE FROM character_references WHERE projectId = :projectId")
    suspend fun deleteCharactersByProject(projectId: Long)
    
    @Query("SELECT * FROM character_references WHERE projectId = :projectId ORDER BY name")
    fun getCharactersFlow(projectId: Long): Flow<List<CharacterReference>>
    
    @Query("SELECT * FROM character_references WHERE projectId = :projectId ORDER BY name")
    suspend fun getCharacters(projectId: Long): List<CharacterReference>
    
    @Query("SELECT * FROM character_references WHERE id = :characterId")
    suspend fun getCharacter(characterId: Long): CharacterReference?
}
