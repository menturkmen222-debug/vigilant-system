package com.whiteboard.animator.data.repository

import com.whiteboard.animator.data.database.*
import com.whiteboard.animator.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.whiteboard.animator.data.network.AiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val sceneDao: SceneDao,
    private val assetDao: SceneAssetDao,
    private val drawingPathDao: DrawingPathDao,
    private val textOverlayDao: TextOverlayDao,
    private val cachedAssetDao: CachedAssetDao,
    private val characterRefDao: CharacterRefDao,
    private val aiService: AiService
) {
    
    // ============================================================================
    // PROJECT OPERATIONS
    // ============================================================================
    
    /** Flow of all user projects (non-templates), ordered by last update */
    val allProjects: Flow<List<Project>> = projectDao.getAllProjectsFlow()
    
    /** Flow of all templates */
    val allTemplates: Flow<List<Project>> = projectDao.getAllTemplatesFlow()
    
    /**
     * Create a new project with the given name.
     * @return The ID of the created project
     */
    suspend fun createProject(name: String, script: String = ""): Long {
        val project = Project(
            name = name.ifBlank { "Untitled Project" },
            script = script
        )
        return projectDao.insertProject(project)
    }
    
    /**
     * Generate a script from a topic using AI.
     */
    suspend fun generateScriptFromTopic(topic: String): String {
        return try {
            val response = aiService.createCompletion(
                 AiService.CompletionRequest(
                     messages = listOf(
                         AiService.Message("system", "You are a creative scriptwriter for whiteboard animation videos. Write a concise, engaging script about the given topic. Format it as plain text with paragraphs. Do not include scene numbers or camera directions, just the spoken narration."),
                         AiService.Message("user", topic)
                     )
                 )
            )
            response.choices.firstOrNull()?.message?.content ?: "Failed to generate script."
        } catch (e: Exception) {
            "Error generating script: ${e.message}. Please check your API Key in Settings."
        }
    }
    
    /**
     * Save or update a project.
     * @return The project ID
     */
    suspend fun saveProject(project: Project): Long {
        return projectDao.insertProject(project.copy(updatedAt = System.currentTimeMillis()))
    }
    
    /**
     * Get a project by ID as Flow for reactive updates.
     */
    fun getProjectFlow(projectId: Long): Flow<Project?> = projectDao.getProjectFlow(projectId)
    
    /**
     * Get a project by ID (one-time fetch).
     */
    suspend fun getProject(projectId: Long): Project? = projectDao.getProject(projectId)
    
    /**
     * Update project name.
     */
    suspend fun updateProjectName(projectId: Long, name: String) {
        projectDao.updateName(projectId, name)
    }
    
    /**
     * Update project script.
     */
    suspend fun updateProjectScript(projectId: Long, script: String) {
        projectDao.updateScript(projectId, script)
    }
    
    /**
     * Delete a project and all its related data (cascades).
     */
    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }
    
    /**
     * Search projects by name.
     */
    fun searchProjects(query: String): Flow<List<Project>> = projectDao.searchProjectsFlow(query)
    
    /**
     * Get project with all scenes and their assets.
     */
    suspend fun getProjectWithScenes(projectId: Long): ProjectWithScenes? {
        val project = projectDao.getProject(projectId) ?: return null
        val scenes = sceneDao.getScenes(projectId)
        
        val scenesWithAssets = scenes.map { scene ->
            SceneWithAssets(
                scene = scene,
                assets = assetDao.getAssets(scene.id),
                drawings = drawingPathDao.getPaths(scene.id),
                textOverlays = textOverlayDao.getOverlays(scene.id)
            )
        }
        
        return ProjectWithScenes(project, scenesWithAssets)
    }
    
    // ============================================================================
    // SCENE OPERATIONS
    // ============================================================================
    
    /**
     * Get scenes for a project as Flow.
     */
    fun getScenesFlow(projectId: Long): Flow<List<Scene>> = sceneDao.getScenesFlow(projectId)
    
    /**
     * Get scenes for a project (one-time fetch).
     */
    suspend fun getScenes(projectId: Long): List<Scene> = sceneDao.getScenes(projectId)
    
    /**
     * Get a single scene.
     */
    suspend fun getScene(sceneId: Long): Scene? = sceneDao.getScene(sceneId)
    
    /**
     * Create scenes from script text.
     * Automatically splits by paragraphs or sentences.
     * 
     * @param projectId The project to add scenes to
     * @param script The script text to parse
     * @return List of created scene IDs
     */
    suspend fun createScenesFromScript(projectId: Long, script: String): List<Long> {
        // Delete existing scenes
        sceneDao.deleteScenesByProject(projectId)
        
        // Split script into scene texts
        val sceneTexts = splitScriptToScenes(script)
        
        // Create scene entities
        val scenes = sceneTexts.mapIndexed { index, text ->
            Scene(
                projectId = projectId,
                text = text,
                orderIndex = index,
                durationMs = calculateSceneDuration(text)
            )
        }
        
        // Insert and update project
        val ids = sceneDao.insertScenes(scenes)
        projectDao.updateScript(projectId, script)
        
        return ids
    }
    
    /**
     * Add a single scene to a project.
     */
    suspend fun addScene(projectId: Long, text: String): Long {
        val maxOrder = sceneDao.getMaxOrderIndex(projectId) ?: -1
        val scene = Scene(
            projectId = projectId,
            text = text,
            orderIndex = maxOrder + 1,
            durationMs = calculateSceneDuration(text)
        )
        val sceneId = sceneDao.insertScene(scene)
        projectDao.updateTimestamp(projectId)
        return sceneId
    }
    
    /**
     * Update a scene.
     */
    suspend fun updateScene(scene: Scene) {
        sceneDao.updateScene(scene.copy(updatedAt = System.currentTimeMillis()))
        projectDao.updateTimestamp(scene.projectId)
    }
    
    /**
     * Update scene text.
     */
    suspend fun updateSceneText(sceneId: Long, text: String) {
        val scene = sceneDao.getScene(sceneId) ?: return
        sceneDao.updateText(sceneId, text)
        projectDao.updateTimestamp(scene.projectId)
    }
    
    /**
     * Update scene duration.
     */
    suspend fun updateSceneDuration(sceneId: Long, durationMs: Long) {
        val scene = sceneDao.getScene(sceneId) ?: return
        sceneDao.updateDuration(sceneId, durationMs)
        projectDao.updateTimestamp(scene.projectId)
    }
    
    /**
     * Delete a scene.
     */
    suspend fun deleteScene(sceneId: Long) {
        val scene = sceneDao.getScene(sceneId) ?: return
        sceneDao.deleteSceneById(sceneId)
        projectDao.updateTimestamp(scene.projectId)
        
        // Reorder remaining scenes
        val remainingScenes = sceneDao.getScenes(scene.projectId)
        remainingScenes.forEachIndexed { index, s ->
            if (s.orderIndex != index) {
                sceneDao.updateOrder(s.id, index)
            }
        }
    }
    
    /**
     * Reorder scenes after drag-and-drop.
     */
    suspend fun reorderScenes(projectId: Long, fromIndex: Int, toIndex: Int) {
        val scenes = sceneDao.getScenes(projectId).toMutableList()
        if (fromIndex < 0 || fromIndex >= scenes.size || toIndex < 0 || toIndex >= scenes.size) return
        
        val movedScene = scenes.removeAt(fromIndex)
        scenes.add(toIndex, movedScene)
        
        scenes.forEachIndexed { index, scene ->
            if (scene.orderIndex != index) {
                sceneDao.updateOrder(scene.id, index)
            }
        }
        
        projectDao.updateTimestamp(projectId)
    }
    
    /**
     * Get total project duration in milliseconds.
     */
    suspend fun getTotalDuration(projectId: Long): Long {
        return sceneDao.getTotalDuration(projectId) ?: 0L
    }
    
    // ============================================================================
    // ASSET OPERATIONS
    // ============================================================================
    
    /**
     * Get assets for a scene as Flow.
     */
    fun getAssetsFlow(sceneId: Long): Flow<List<SceneAsset>> = assetDao.getAssetsFlow(sceneId)
    
    /**
     * Get assets for a scene (one-time fetch).
     */
    suspend fun getAssets(sceneId: Long): List<SceneAsset> = assetDao.getAssets(sceneId)
    
    /**
     * Add a built-in asset to a scene.
     */
    suspend fun addBuiltInAsset(
        sceneId: Long,
        assetType: AssetType,
        resourceName: String,
        placement: ImagePlacement = ImagePlacement.CENTER,
        animationStyle: AnimationStyle = AnimationStyle.FADE_IN
    ): Long {
        val scene = sceneDao.getScene(sceneId) ?: return -1
        val maxLayer = assetDao.getMaxLayerOrder(sceneId) ?: -1
        
        val asset = SceneAsset(
            sceneId = sceneId,
            assetType = assetType,
            assetPath = resourceName,
            isBuiltIn = true,
            placement = placement,
            animationStyle = animationStyle,
            layerOrder = maxLayer + 1
        )
        
        val assetId = assetDao.insertAsset(asset)
        projectDao.updateTimestamp(scene.projectId)
        return assetId
    }
    
    /**
     * Add a user-imported or AI-generated image to a scene.
     */
    suspend fun addUserImage(
        sceneId: Long,
        imagePath: String,
        placement: ImagePlacement = ImagePlacement.CENTER,
        isAiGenerated: Boolean = false
    ): Long {
        val scene = sceneDao.getScene(sceneId) ?: return -1
        val maxLayer = assetDao.getMaxLayerOrder(sceneId) ?: -1
        
        val asset = SceneAsset(
            sceneId = sceneId,
            assetType = if (isAiGenerated) AssetType.AI_GENERATED else AssetType.USER_IMAGE,
            assetPath = imagePath,
            isBuiltIn = false,
            placement = placement,
            animationStyle = AnimationStyle.FADE_IN,
            layerOrder = maxLayer + 1
        )
        
        val assetId = assetDao.insertAsset(asset)
        projectDao.updateTimestamp(scene.projectId)
        return assetId
    }
    
    /**
     * Update an asset.
     */
    suspend fun updateAsset(asset: SceneAsset) {
        assetDao.updateAsset(asset)
        val scene = sceneDao.getScene(asset.sceneId)
        scene?.let { projectDao.updateTimestamp(it.projectId) }
    }
    
    /**
     * Delete an asset.
     */
    suspend fun deleteAsset(assetId: Long) {
        val asset = assetDao.getAsset(assetId) ?: return
        val scene = sceneDao.getScene(asset.sceneId)
        assetDao.deleteAssetById(assetId)
        scene?.let { projectDao.updateTimestamp(it.projectId) }
    }
    
    // ============================================================================
    // TEMPLATE OPERATIONS
    // ============================================================================
    
    /**
     * Create a new project from a template.
     * Copies all scenes and assets.
     */
    suspend fun createProjectFromTemplate(templateId: Long, name: String): Long {
        val template = projectDao.getProject(templateId) ?: return -1
        
        // Create new project from template
        val newProject = template.copy(
            id = 0,
            name = name,
            isTemplate = false,
            templateCategory = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newProjectId = projectDao.insertProject(newProject)
        
        // Copy scenes
        val templateScenes = sceneDao.getScenes(templateId)
        for (scene in templateScenes) {
            val newScene = scene.copy(
                id = 0,
                projectId = newProjectId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newSceneId = sceneDao.insertScene(newScene)
            
            // Copy assets
            val assets = assetDao.getAssets(scene.id)
            for (asset in assets) {
                val newAsset = asset.copy(
                    id = 0,
                    sceneId = newSceneId,
                    createdAt = System.currentTimeMillis()
                )
                assetDao.insertAsset(newAsset)
            }
        }
        
        return newProjectId
    }
    
    /**
     * Save a project as a template.
     */
    suspend fun saveAsTemplate(projectId: Long, category: String): Long {
        val project = projectDao.getProject(projectId) ?: return -1
        val templateProject = project.copy(
            id = 0,
            isTemplate = true,
            templateCategory = category,
            name = "${project.name} (Template)",
            createdAt = System.currentTimeMillis()
        )
        return projectDao.insertProject(templateProject)
    }
    
    // ============================================================================
    // CACHE OPERATIONS
    // ============================================================================
    
    /**
     * Get cached asset by source URL.
     */
    suspend fun getCachedAsset(sourceUrl: String): CachedAsset? {
        return cachedAssetDao.findBySourceUrl(sourceUrl)
    }
    
    /**
     * Save a cached asset.
     */
    suspend fun saveCachedAsset(asset: CachedAsset): Long {
        return cachedAssetDao.insertCachedAsset(asset)
    }
    
    /**
     * Get total cache size in bytes.
     */
    suspend fun getCacheSize(): Long {
        return cachedAssetDao.getTotalCacheSize() ?: 0L
    }
    
    /**
     * Clear old cached assets.
     * @param maxAgeMs Maximum age in milliseconds
     * @return Number of deleted assets
     */
    suspend fun clearOldCache(maxAgeMs: Long = 30L * 24 * 60 * 60 * 1000): Int {
        val cutoffTime = System.currentTimeMillis() - maxAgeMs
        return cachedAssetDao.deleteOldAssets(cutoffTime)
    }
    
    // ============================================================================
    // HELPER FUNCTIONS
    // ============================================================================
    
    /**
     * Split script text into individual scenes.
     * 
     * Strategy:
     * 1. Try paragraph splitting (double newlines)
     * 2. Fall back to sentence splitting
     * 3. Limit to reasonable scene count
     */
    private fun splitScriptToScenes(script: String): List<String> {
        if (script.isBlank()) return emptyList()
        
        // Try splitting by paragraphs first
        val paragraphs = script.split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        if (paragraphs.size > 1) {
            return paragraphs.take(50)  // Limit to 50 scenes
        }
        
        // Fall back to sentence splitting
        val sentences = script.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        // Group short sentences together
        val groupedScenes = mutableListOf<String>()
        var currentGroup = StringBuilder()
        
        for (sentence in sentences) {
            if (currentGroup.length + sentence.length > 200 && currentGroup.isNotEmpty()) {
                groupedScenes.add(currentGroup.toString().trim())
                currentGroup = StringBuilder()
            }
            if (currentGroup.isNotEmpty()) currentGroup.append(" ")
            currentGroup.append(sentence)
        }
        
        if (currentGroup.isNotEmpty()) {
            groupedScenes.add(currentGroup.toString().trim())
        }
        
        return groupedScenes.ifEmpty { listOf(script) }.take(50)
    }
    
    /**
     * Calculate appropriate scene duration based on text length.
     * Assumes ~150 words per minute speaking rate.
     */
    private fun calculateSceneDuration(text: String): Long {
        val wordCount = text.split(Regex("\\s+")).size
        val wordsPerMinute = 150
        val minDurationMs = 3000L
        val maxDurationMs = 30000L
        
        val calculatedMs = (wordCount.toFloat() / wordsPerMinute * 60 * 1000).toLong()
        return calculatedMs.coerceIn(minDurationMs, maxDurationMs)
    }
    
    // ============================================================================
    // CHARACTER REFERENCE OPERATIONS
    // ============================================================================
    
    /**
     * Get character references for a project.
     */
    fun getCharacterReferencesFlow(projectId: Long): Flow<List<CharacterReference>> {
        return characterRefDao.getCharactersFlow(projectId)
    }
    
    /**
     * Get character references for a project (one-time fetch).
     */
    suspend fun getCharacterReferences(projectId: Long): List<CharacterReference> {
        return characterRefDao.getCharacters(projectId)
    }
    
    /**
     * Add a character reference to a project.
     */
    suspend fun addCharacterReference(
        projectId: Long,
        name: String,
        type: CharacterType,
        imagePath: String,
        description: String = ""
    ): Long {
        val character = CharacterReference(
            projectId = projectId,
            name = name,
            type = type,
            imagePath = imagePath,
            description = description
        )
        return characterRefDao.insertCharacter(character)
    }
    
    /**
     * Delete a character reference.
     */
    suspend fun deleteCharacterReference(characterId: Long) {
        characterRefDao.deleteCharacterById(characterId)
    }
    
    // ============================================================================
    // STYLE-AWARE AI GENERATION
    // ============================================================================
    
    /**
     * Generate a style-aware prompt for illustration generation.
     * Incorporates video style and character references.
     */
    suspend fun generateIllustrationPrompt(
        sceneText: String,
        videoStyle: VideoStyle,
        projectId: Long
    ): String {
        val stylePrompt = getStylePromptPrefix(videoStyle)
        val characters = getCharacterReferences(projectId)
        val characterPrompt = if (characters.isNotEmpty()) {
            val characterDescriptions = characters.joinToString("; ") { 
                "${it.name}: ${it.description.ifEmpty { it.type.name.lowercase() }}"
            }
            "Include these characters: $characterDescriptions. "
        } else ""
        
        return "$stylePrompt$characterPrompt Illustrate: $sceneText"
    }
    
    /**
     * Get style-specific prompt prefix for AI illustration.
     */
    private fun getStylePromptPrefix(style: VideoStyle): String {
        return when (style) {
            VideoStyle.WHITEBOARD -> "Clean whiteboard animation style with black line drawings on white background. Simple hand-drawn look. "
            VideoStyle.CARTOON -> "Colorful cartoon style illustration with bright colors, bold outlines, and playful design. "
            VideoStyle.REALISTIC -> "Realistic sketch style with detailed shading and natural proportions. Pencil drawing aesthetic. "
            VideoStyle.CHALKBOARD -> "Chalkboard style with white chalk-like lines on dark green background. Educational feel. "
            VideoStyle.MINIMAL -> "Minimalist line art with simple shapes and limited color palette. Clean and modern. "
        }
    }
    
    /**
     * Generate script with style context for better scene descriptions.
     */
    suspend fun generateStyledScriptFromTopic(
        topic: String,
        videoStyle: VideoStyle
    ): String {
        val styleContext = when (videoStyle) {
            VideoStyle.WHITEBOARD -> "classic educational whiteboard animation"
            VideoStyle.CARTOON -> "fun, colorful cartoon animation"
            VideoStyle.REALISTIC -> "professional, realistic illustration style"
            VideoStyle.CHALKBOARD -> "nostalgic chalkboard teaching format"
            VideoStyle.MINIMAL -> "clean, modern minimalist design"
        }
        
        return try {
            val response = aiService.createCompletion(
                AiService.CompletionRequest(
                    messages = listOf(
                        AiService.Message(
                            "system",
                            "You are a creative scriptwriter for $styleContext videos. " +
                            "Write a concise, engaging script about the given topic. " +
                            "Format it as plain text with paragraphs. " +
                            "Each paragraph will become a scene. " +
                            "Keep each paragraph under 50 words for optimal pacing. " +
                            "Do not include scene numbers or camera directions."
                        ),
                        AiService.Message("user", topic)
                    )
                )
            )
            response.choices.firstOrNull()?.message?.content ?: "Failed to generate script."
        } catch (e: Exception) {
            "Error generating script: ${e.message}. Please check your API Key in Settings."
        }
    }
    
    /**
     * Suggest multiple illustration prompts for a scene.
     * Returns a list of prompts for different visual elements.
     */
    suspend fun generateMultiIllustrationPrompts(
        sceneText: String,
        videoStyle: VideoStyle,
        projectId: Long,
        maxIllustrations: Int = 3
    ): List<String> {
        val basePrompt = generateIllustrationPrompt(sceneText, videoStyle, projectId)
        
        // Extract key concepts from scene text for varied illustrations
        val concepts = extractKeyConceptsFromText(sceneText)
        
        return if (concepts.size <= 1) {
            listOf(basePrompt)
        } else {
            concepts.take(maxIllustrations).map { concept ->
                "${getStylePromptPrefix(videoStyle)}Focus on: $concept. Context: $sceneText"
            }
        }
    }
    
    /**
     * Extract key concepts from text for multi-illustration generation.
     */
    private fun extractKeyConceptsFromText(text: String): List<String> {
        // Simple extraction: split by commas, "and", or sentences
        val concepts = text
            .split(Regex("[,;]|\\band\\b|\\bor\\b"))
            .map { it.trim() }
            .filter { it.length > 10 }
        
        return if (concepts.isEmpty()) listOf(text) else concepts
    }
    
}

