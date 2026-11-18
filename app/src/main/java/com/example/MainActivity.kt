package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.AppContainer
import com.example.core.configureAppCheck
import com.example.ui.screens.AiServicesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProjectWorkspaceScreen
import com.example.ui.theme.KineticCyan
import com.example.ui.theme.KineticCyanContainer
import com.example.ui.theme.MotionStudioTheme
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.AiServicesViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.ProjectWorkspaceViewModel

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_AI_SERVICES = "ai_services"
const val ROUTE_WORKSPACE = "workspace/{projectId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val appContainer = AppContainer(applicationContext)

        setContent {
            MotionStudioTheme {
                MotionStudioApp(appContainer = appContainer)
            }
        }
    }
}

@Composable
private fun MotionStudioApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTopLevelNav = currentRoute == ROUTE_DASHBOARD || currentRoute == ROUTE_AI_SERVICES

    val navigateTopLevel: (String) -> Unit = remember(navController) {
        { route ->
            navController.navigate(route) {
                popUpTo(ROUTE_DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = StudioObsidian,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar(
                        containerColor = StudioSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == ROUTE_DASHBOARD,
                            onClick = { navigateTopLevel(ROUTE_DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_DASHBOARD) {
                                        Icons.Filled.Dashboard
                                    } else {
                                        Icons.Outlined.Dashboard
                                    },
                                    contentDescription = "Tableau de bord"
                                )
                            },
                            label = { Text("Projets") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = KineticCyan,
                                selectedTextColor = KineticCyan,
                                indicatorColor = KineticCyanContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_dashboard")
                        )
                        NavigationBarItem(
                            selected = currentRoute == ROUTE_AI_SERVICES,
                            onClick = { navigateTopLevel(ROUTE_AI_SERVICES) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_AI_SERVICES) {
                                        Icons.Filled.Tune
                                    } else {
                                        Icons.Outlined.Tune
                                    },
                                    contentDescription = "Gestion des services IA"
                                )
                            },
                            label = { Text("Services IA") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = KineticCyan,
                                selectedTextColor = KineticCyan,
                                indicatorColor = KineticCyanContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_ai_services")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail(
                        containerColor = StudioSurface
                    ) {
                        NavigationRailItem(
                            selected = currentRoute == ROUTE_DASHBOARD,
                            onClick = { navigateTopLevel(ROUTE_DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_DASHBOARD) {
                                        Icons.Filled.Dashboard
                                    } else {
                                        Icons.Outlined.Dashboard
                                    },
                                    contentDescription = "Projets"
                                )
                            },
                            label = { Text("Projets") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = KineticCyan,
                                selectedTextColor = KineticCyan,
                                indicatorColor = KineticCyanContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("rail_tab_dashboard")
                        )
                        NavigationRailItem(
                            selected = currentRoute == ROUTE_AI_SERVICES,
                            onClick = { navigateTopLevel(ROUTE_AI_SERVICES) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_AI_SERVICES) {
                                        Icons.Filled.Tune
                                    } else {
                                        Icons.Outlined.Tune
                                    },
                                    contentDescription = "Services IA"
                                )
                            },
                            label = { Text("Services IA") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = KineticCyan,
                                selectedTextColor = KineticCyan,
                                indicatorColor = KineticCyanContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("rail_tab_ai_services")
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_DASHBOARD) {
                        val dashboardVm: DashboardViewModel = viewModel(
                            factory = DashboardViewModel.Factory(appContainer.repository)
                        )
                        val uiState by dashboardVm.uiState.collectAsStateWithLifecycle()

                        DashboardScreen(
                            uiState = uiState,
                            onSearchQueryChange = dashboardVm::onSearchQueryChanged,
                            onStatusFilterChange = dashboardVm::onStatusFilterSelected,
                            onToggleSortOrder = dashboardVm::onToggleSortOrder,
                            onCreateProject = { title, brief ->
                                dashboardVm.createNewProject(title, brief) { newId ->
                                    navController.navigate("workspace/$newId")
                                }
                            },
                            onOpenProject = { projectId ->
                                navController.navigate("workspace/$projectId")
                            },
                            onDuplicateProject = dashboardVm::duplicateProject,
                            onDeleteProject = dashboardVm::deleteProject,
                            onOpenAiServices = { navigateTopLevel(ROUTE_AI_SERVICES) }
                        )
                    }

                    composable(ROUTE_AI_SERVICES) {
                        val aiVm: AiServicesViewModel = viewModel(
                            factory = AiServicesViewModel.Factory(appContainer.repository)
                        )
                        val uiState by aiVm.uiState.collectAsStateWithLifecycle()

                        AiServicesScreen(
                            uiState = uiState,
                            onSelectPrimaryModel = aiVm::selectPrimaryModel,
                            onSelectFallbackModel = aiVm::selectFallbackModel,
                            onToggleEconomicMode = aiVm::setEconomicMode,
                            onTestConnection = aiVm::testConnection,
                            onClearLogs = aiVm::clearLogs
                        )
                    }

                    composable(
                        route = ROUTE_WORKSPACE,
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                    ) { entry ->
                        val projectId = entry.arguments?.getString("projectId") ?: ""
                        val workspaceVm: ProjectWorkspaceViewModel = viewModel(
                            key = projectId,
                            factory = ProjectWorkspaceViewModel.Factory(
                                projectId = projectId,
                                repository = appContainer.repository
                            )
                        )
                        val uiState by workspaceVm.uiState.collectAsStateWithLifecycle()

                        ProjectWorkspaceScreen(
                            uiState = uiState,
                            viewModel = workspaceVm,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenAiServices = { navController.navigate(ROUTE_AI_SERVICES) }
                        )
                    }
                }
            }
        }
    }
}
