package com.lifeos.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import kotlinx.serialization.Serializable

// Type-safe navigation routes (Navigation Compose 2.8+).
@Serializable object HomeRoute
@Serializable object AssistantRoute
@Serializable object FinanceRoute
@Serializable object TasksRoute
@Serializable object MoreRoute
@Serializable object SettingsRoute
@Serializable data class ModuleRoute(val key: String)
@Serializable data class ProjectRoute(val id: Long)
@Serializable data class NoteRoute(val id: Long)
@Serializable data class DocumentRoute(val id: Long)

/** The five primary destinations shown in the floating bar (phone) or rail (tablet). */
enum class TopLevel(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    ASSISTANT("LIFE AI", Icons.Outlined.AutoAwesome),
    FINANCE("Finance", Icons.Outlined.AccountBalanceWallet),
    TASKS("Tasks", Icons.Outlined.CheckCircle),
    MORE("More", Icons.Outlined.GridView);

    val route: Any
        get() = when (this) {
            HOME -> HomeRoute
            ASSISTANT -> AssistantRoute
            FINANCE -> FinanceRoute
            TASKS -> TasksRoute
            MORE -> MoreRoute
        }

    /** True if [destination] belongs to this tab. Settings and modules live under More. */
    fun matches(destination: NavDestination?): Boolean {
        val chain = destination?.hierarchy ?: return false
        return chain.any { d ->
            when (this) {
                HOME -> d.hasRoute<HomeRoute>()
                ASSISTANT -> d.hasRoute<AssistantRoute>()
                FINANCE -> d.hasRoute<FinanceRoute>()
                TASKS -> d.hasRoute<TasksRoute>() || d.hasRoute<ProjectRoute>()
                MORE -> d.hasRoute<MoreRoute>() || d.hasRoute<SettingsRoute>() || d.hasRoute<ModuleRoute>() || d.hasRoute<NoteRoute>() || d.hasRoute<DocumentRoute>()
            }
        }
    }
}

/** Feature modules reachable from the More screen and the dashboard. */
enum class LifeModule(val key: String, val title: String, val icon: ImageVector, val summary: String) {
    FAMILY("family", "Family", Icons.Outlined.FamilyRestroom, "Profiles, birthdays, emergency contacts."),
    HEALTH("health", "Health", Icons.Outlined.Favorite, "Medicines, appointments, water, sleep, weight."),
    CALENDAR("calendar", "Calendar", Icons.Outlined.CalendarMonth, "Day, week, month and all reminders."),
    GOALS("goals", "Goals", Icons.Outlined.EmojiEvents, "Targets, habits, streaks and badges."),
    DOCUMENTS("documents", "Documents", Icons.Outlined.Description, "Private vault — never uploaded."),
    NOTES("notes", "Notes", Icons.Outlined.StickyNote2, "Notes, checklists, tags and links."),
    INSIGHTS("insights", "Insights", Icons.Outlined.Insights, "Weekly report and productivity analytics.");

    companion object {
        fun fromKey(key: String): LifeModule? = entries.firstOrNull { it.key == key }
        val moreSection = entries.toList()
    }
}
