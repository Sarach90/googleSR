package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiStudioService
import com.example.data.local.MotionStudioDatabase
import com.example.data.model.AdBrief
import com.example.data.model.AdConcept
import com.example.data.model.AdScript
import com.example.data.model.GeminiModelCatalog
import com.example.data.model.ScriptSection
import com.example.data.model.ScriptSectionType
import com.example.data.model.StoryboardScene
import com.example.data.model.WorkflowStep
import com.example.data.preferences.AiSettingsStore
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudioRepositoryTest {

    private lateinit var db: MotionStudioDatabase
    private lateinit var repository: ProjectRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MotionStudioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ProjectRepository(
            dao = db.studioDao(),
            aiService = GeminiStudioService(),
            settingsStore = AiSettingsStore(context),
            appContext = context
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `recalculateTimeline updates scene numbers and contiguous timecodes`() {
        val scenes = listOf(
            StoryboardScene(durationSec = 4, narrativeObjective = "Hook"),
            StoryboardScene(durationSec = 6, narrativeObjective = "Demo"),
            StoryboardScene(durationSec = 5, narrativeObjective = "CTA")
        )
        val result = repository.recalculateTimeline(scenes)
        assertEquals(3, result.size)
        assertEquals(1, result[0].sceneNumber)
        assertEquals(0, result[0].startTimeSec)
        assertEquals(4, result[0].endTimeSec)

        assertEquals(2, result[1].sceneNumber)
        assertEquals(4, result[1].startTimeSec)
        assertEquals(10, result[1].endTimeSec)

        assertEquals(3, result[2].sceneNumber)
        assertEquals(10, result[2].startTimeSec)
        assertEquals(15, result[2].endTimeSec)
    }

    @Test
    fun `alignTimelineToDuration adjusts total duration to exact target`() {
        val scenes = listOf(
            StoryboardScene(durationSec = 5),
            StoryboardScene(durationSec = 10),
            StoryboardScene(durationSec = 8)
        )
        val alignedUp = repository.alignTimelineToDuration(scenes, 30)
        assertEquals(30, alignedUp.sumOf { it.durationSec })
        assertEquals(30, alignedUp.last().endTimeSec)

        val alignedDown = repository.alignTimelineToDuration(scenes, 15)
        assertEquals(15, alignedDown.sumOf { it.durationSec })
        assertEquals(15, alignedDown.last().endTimeSec)
    }

    @Test
    fun `project persistence and version snapshot restore preserve validated state`() = runTest {
        val brief = AdBrief(
            productName = "Nova Watch",
            sector = "Wearables",
            description = "Montre connectée titane",
            durationSeconds = 30,
            platform = "TikTok / Reels (9:16)",
            productImagePath = "/tmp/product_images/nova_watch.jpg"
        )
        val created = repository.createProject("Campagne Nova", brief)
        val concept = AdConcept(
            name = "Éclat Urbain",
            marketingAngle = "Performance",
            creativeIdea = "Transitions match-cut",
            hook = "0.2g au poignet",
            commercialPromise = "7 jours d'autonomie",
            visualMood = "Néon cyan sombre",
            directingStyle = "Rythme rapide 120bpm",
            generalFlow = "Hook -> Demo -> CTA"
        )
        val script = AdScript(
            sections = listOf(
                ScriptSection(type = ScriptSectionType.HOOK, voiceOver = "Stop.", onScreenText = "NOVA", durationSeconds = 5)
            ),
            directorNotes = "Voix posée"
        )
        val progressed = created.copy(
            currentStep = WorkflowStep.SCRIPT,
            briefValidated = true,
            conceptsValidated = true,
            concepts = listOf(concept),
            selectedConceptId = concept.id,
            script = script
        )
        repository.saveProject(progressed)
        repository.saveVersionSnapshot(progressed, "Avant modification")

        val modified = progressed.copy(
            title = "Titre modifié",
            concepts = emptyList(),
            brief = progressed.brief.copy(productImagePath = null)
        )
        repository.saveProject(modified)

        val versions = repository.observeVersions(created.id).first()
        assertEquals(1, versions.size)

        val restored = repository.restoreVersion(modified, versions.first())
        assertEquals("Campagne Nova", restored.title)
        assertEquals(1, restored.concepts.size)
        assertEquals("/tmp/product_images/nova_watch.jpg", restored.brief.productImagePath)
        assertTrue(restored.briefValidated)
        assertTrue(restored.conceptsValidated)
        assertNotNull(restored.script)
    }

    @Test
    fun `model catalog contains non-billing default and explicit billing flags`() {
        val defaultModel = GeminiModelCatalog.findById(GeminiModelCatalog.DEFAULT_PRIMARY_ID)
        assertEquals("gemini-3.8-flash", defaultModel.id)
        assertEquals(false, defaultModel.requiresBilling)

        val argonModel = GeminiModelCatalog.findById("gemini-4-argon")
        assertEquals(true, argonModel.requiresBilling)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `brief validation unlocks concepts step immediately after typing`() = runBlocking(Dispatchers.Default) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val created = repository.createProject("Test Unlock", AdBrief())
        val store = androidx.lifecycle.ViewModelStore()
        try {
            val vm = androidx.lifecycle.ViewModelProvider(
                store,
                com.example.ui.viewmodel.ProjectWorkspaceViewModel.Factory(created.id, repository)
            )[com.example.ui.viewmodel.ProjectWorkspaceViewModel::class.java]

            withTimeout(10_000L) {
                vm.uiState.first { it.project != null }
                vm.updateBrief(
                    AdBrief(
                        productName = "Bouteille Pulse",
                        features = "Isotherme 24h",
                        durationSeconds = 30
                    )
                )
                vm.selectStep(WorkflowStep.CONCEPTS)
                val state = vm.uiState.first { it.project?.briefValidated == true }
                val project = state.project
                assertNotNull(project)
                assertTrue(project!!.briefValidated)
                assertEquals(WorkflowStep.CONCEPTS, project.currentStep)
            }
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `importProductImage saves bitmap locally and removeProductImage clears it`() = runBlocking(Dispatchers.Default) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempInput = java.io.File(context.cacheDir, "sample_product.png")
        val sampleBmp = android.graphics.Bitmap.createBitmap(120, 80, android.graphics.Bitmap.Config.ARGB_8888)
        java.io.FileOutputStream(tempInput).use { out ->
            sampleBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        sampleBmp.recycle()

        val created = repository.createProject("Test Image", AdBrief(productName = "Lampe Studio", description = "LED"))
        val store = androidx.lifecycle.ViewModelStore()
        try {
            val vm = androidx.lifecycle.ViewModelProvider(
                store,
                com.example.ui.viewmodel.ProjectWorkspaceViewModel.Factory(created.id, repository)
            )[com.example.ui.viewmodel.ProjectWorkspaceViewModel::class.java]

            withTimeout(10_000L) {
                vm.uiState.first { it.project != null }
                vm.importProductImage(android.net.Uri.fromFile(tempInput))
                val withImage = vm.uiState.first { !it.project?.brief?.productImagePath.isNullOrBlank() }
                val savedPath = withImage.project!!.brief.productImagePath!!
                assertTrue(java.io.File(savedPath).exists())
                assertNotNull(com.example.data.local.ProductImageStorage.loadBitmap(savedPath))

                vm.removeProductImage()
                val cleared = vm.uiState.first { it.project?.brief?.productImagePath == null }
                assertEquals(null, cleared.project?.brief?.productImagePath)
                assertTrue(!java.io.File(savedPath).exists())
            }
        } finally {
            tempInput.delete()
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
