package com.lifeos.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lifeos.app.core.designsystem.component.LifeBackground
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.LocalLayoutInfo
import com.lifeos.app.core.designsystem.theme.layoutInfoFor
import com.lifeos.app.feature.assistant.AssistantScreen
import com.lifeos.app.feature.dashboard.DashboardScreen
import com.lifeos.app.feature.module.ModulePendingScreen
import com.lifeos.app.feature.more.MoreScreen
import com.lifeos.app.feature.settings.SettingsScreen

/** Root UI: adaptive navigation chrome (rail on tablets, floating bar on phones) + NavHost. */
@Composable
fun LifeOsAppShell() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = layoutInfoFor(maxWidth)
        CompositionLocalProvider(LocalLayoutInfo provides layout) {
            val navController = rememberNavController()
            val backStack by navController.currentBackStackEntryAsState()
            val destination = backStack?.destination
            val selected = TopLevel.entries.firstOrNull { it.matches(destination) }
            val settingsSelected = destination?.hierarchy?.any { it.hasRoute<SettingsRoute>() } == true
            val onSelect: (TopLevel) -> Unit = { navController.navigateTopLevel(it) }

            LifeBackground {
                if (layout.useNavRail) {
                    Row(Modifier.fillMaxSize()) {
                        FloatingNavRail(
                            selected = selected,
                            onSelect = onSelect,
                            onOpenSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
                            settingsSelected = settingsSelected,
                        )
                        LifeNavHost(navController, Modifier.weight(1f))
                    }
                } else {
                    Box(Modifier.fillMaxSize()) {
                        LifeNavHost(navController, Modifier.fillMaxSize())
                        FloatingBottomBar(
                            selected = selected,
                            onSelect = onSelect,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(item: TopLevel) {
    navigate(item.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun LifeNavHost(navController: NavHostController, modifier: Modifier) {
    val reduced = LifeTheme.reducedMotion
    val enter: EnterTransition = if (reduced) EnterTransition.None else fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.985f)
    val exit: ExitTransition = if (reduced) ExitTransition.None else fadeOut(tween(160))

    val openModule: (String) -> Unit = { key ->
        when (key) {
            "finance" -> navController.navigateTopLevel(TopLevel.FINANCE)
            "tasks" -> navController.navigateTopLevel(TopLevel.TASKS)
            "assistant" -> navController.navigateTopLevel(TopLevel.ASSISTANT)
            else -> navController.navigate(ModuleRoute(key))
        }
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit },
    ) {
        composable<HomeRoute> {
            DashboardScreen(
                onOpenModule = openModule,
                onOpenSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
            )
        }
        composable<AssistantRoute> { AssistantScreen() }
        composable<FinanceRoute> { ModulePendingScreen(LifeModule.FINANCE, onBack = null) }
        composable<TasksRoute> { ModulePendingScreen(LifeModule.TASKS, onBack = null) }
        composable<MoreRoute> {
            MoreScreen(
                onOpenModule = openModule,
                onOpenSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
            )
        }
        composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable<ModuleRoute> { entry ->
            val route = entry.toRoute<ModuleRoute>()
            val module = LifeModule.fromKey(route.key) ?: LifeModule.NOTES
            ModulePendingScreen(module, onBack = { navController.popBackStack() })
        }
    }
}
