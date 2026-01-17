package com.whiteboard.animator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.whiteboard.animator.ui.navigation.AppNavigation
import com.whiteboard.animator.ui.theme.WhiteboardAnimatorTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Whiteboard Animator Pro.
 * 
 * Entry point for the app with:
 * - Edge-to-edge display support
 * - Compose-based UI
 * - Hilt dependency injection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @javax.inject.Inject
    lateinit var preferenceManager: com.whiteboard.animator.data.preferences.PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge for modern immersive UI
        enableEdgeToEdge()
        
        setContent {
            val isDarkMode by preferenceManager.isDarkMode.collectAsState(initial = androidx.compose.foundation.isSystemInDarkTheme())
            
            WhiteboardAnimatorTheme(darkTheme = isDarkMode) {
                val isOnboardingCompleted by preferenceManager.isOnboardingCompleted
                    .collectAsState(initial = null) // Null means loading

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isOnboardingCompleted != null) {
                        val startDest = if (isOnboardingCompleted == true) 
                            com.whiteboard.animator.ui.navigation.Routes.HOME 
                        else 
                            com.whiteboard.animator.ui.navigation.Routes.ONBOARDING
                        
                        AppNavigation(startDestination = startDest)
                    } else {
                        // Splash / Loading state
                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
