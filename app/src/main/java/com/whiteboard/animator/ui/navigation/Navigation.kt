package com.whiteboard.animator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whiteboard.animator.ui.screens.asset.AssetLibraryScreen
import com.whiteboard.animator.ui.screens.editor.EditorScreen
import com.whiteboard.animator.ui.screens.export.ExportScreen
import com.whiteboard.animator.ui.screens.home.HomeScreen
import com.whiteboard.animator.ui.screens.script.ScriptInputScreen
import com.whiteboard.animator.ui.screens.settings.SettingsScreen

/**
 * Navigation routes definitions.
 */
object Routes {
    const val ONBOARDING = "onboarding" // Added
    const val HOME = "home"
    const val SCRIPT_INPUT = "script_input"
    const val EDITOR = "editor/{projectId}"
    const val EXPORT = "export/{projectId}"
    const val SETTINGS = "settings"
    const val ASSET_LIBRARY = "asset_library/{projectId}/{sceneId}"
    const val CHARACTER_MANAGER = "character_manager/{projectId}" // Added
    
    // Helper to build routes with arguments
    fun editor(projectId: Long) = "editor/$projectId"
    fun export(projectId: Long) = "export/$projectId"
    fun assetLibrary(projectId: Long, sceneId: Long) = "asset_library/$projectId/$sceneId"
    fun characterManager(projectId: Long) = "character_manager/$projectId"
}

/**
 * Main Navigation Graph for the application.
 */
@Composable
fun AppNavigation(
    startDestination: String = Routes.HOME, // Added parameter
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding Screen
        composable(Routes.ONBOARDING) {
            com.whiteboard.animator.ui.screens.onboarding.OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(Routes.HOME) {
            HomeScreen(
                onCreateProject = { navController.navigate(Routes.SCRIPT_INPUT) },
                onOpenProject = { projectId -> navController.navigate(Routes.editor(projectId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        
        // Script Input Screen (New Project)
        composable(Routes.SCRIPT_INPUT) {
            ScriptInputScreen(
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = { projectId ->
                    navController.navigate(Routes.editor(projectId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        
        // Editor Screen
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            EditorScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onExport = { navController.navigate(Routes.export(projectId)) },
                onOpenAssetLibrary = { sceneId -> 
                    navController.navigate(Routes.assetLibrary(projectId, sceneId)) 
                },
                onOpenCharacterManager = {
                    navController.navigate(Routes.characterManager(projectId))
                }
            )
        }
        
        // Export Screen
        composable(
            route = Routes.EXPORT,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ExportScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onExportComplete = { navController.popBackStack() }
            )
        }
        
        // Settings Screen
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Asset Library Screen
        composable(
            route = Routes.ASSET_LIBRARY,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("sceneId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            // project Id unused for now but kept for context if needed later
            val sceneId = backStackEntry.arguments?.getLong("sceneId") ?: return@composable
            
            AssetLibraryScreen(
                sceneId = sceneId,
                onDismiss = { navController.popBackStack() }
            )
        }
        
        // Character Manager Screen
        composable(
            route = Routes.CHARACTER_MANAGER,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            val viewModel: com.whiteboard.animator.ui.screens.CharacterManagerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            
            androidx.compose.runtime.LaunchedEffect(projectId) {
                viewModel.loadCharacters(projectId)
            }
            
            val characters by androidx.lifecycle.compose.collectAsStateWithLifecycle(viewModel.characters)
            
            com.whiteboard.animator.ui.screens.CharacterManagerScreen(
                projectId = projectId,
                characters = characters,
                onAddCharacter = { name, type, imagePath, description ->
                    viewModel.addCharacter(projectId, name, type, imagePath, description)
                },
                onDeleteCharacter = { character ->
                    viewModel.deleteCharacter(character)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
