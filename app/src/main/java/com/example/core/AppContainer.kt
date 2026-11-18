package com.example.core

import android.content.Context
import com.example.data.ai.GeminiStudioService
import com.example.data.local.MotionStudioDatabase
import com.example.data.preferences.AiSettingsStore
import com.example.data.repository.ProjectRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { MotionStudioDatabase.getInstance(appContext) }
    private val aiService by lazy { GeminiStudioService() }
    private val settingsStore by lazy { AiSettingsStore(appContext) }

    val repository: ProjectRepository by lazy {
        ProjectRepository(
            dao = database.studioDao(),
            aiService = aiService,
            settingsStore = settingsStore,
            appContext = appContext
        )
    }
}
