package com.whiteboard.animator.di

import android.content.Context
import com.whiteboard.animator.data.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for dependency injection.
 * Provides singleton instances of database and DAOs.
 * Repositories use @Inject constructor and are automatically provided by Hilt.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // ============================================================================
    // DATABASE
    // ============================================================================
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    // ============================================================================
    // DAOs
    // ============================================================================
    
    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }
    
    @Provides
    @Singleton
    fun provideSceneDao(database: AppDatabase): SceneDao {
        return database.sceneDao()
    }
    
    @Provides
    @Singleton
    fun provideSceneAssetDao(database: AppDatabase): SceneAssetDao {
        return database.sceneAssetDao()
    }
    
    @Provides
    @Singleton
    fun provideCachedAssetDao(database: AppDatabase): CachedAssetDao {
        return database.cachedAssetDao()
    }
    
    @Provides
    @Singleton
    fun provideDrawingPathDao(database: AppDatabase): DrawingPathDao {
        return database.drawingPathDao()
    }
    
    @Provides
    @Singleton
    fun provideTextOverlayDao(database: AppDatabase): TextOverlayDao {
        return database.textOverlayDao()
    }
    
    @Provides
    @Singleton
    fun provideCharacterRefDao(database: AppDatabase): CharacterRefDao {
        return database.characterRefDao()
    }
    
    // Note: ProjectRepository uses @Inject constructor and is automatically provided by Hilt
}

