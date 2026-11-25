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
                TASKS -> d.hasRoute<TasksRoute>()
                MORE -> d.hasRoute<MoreRoute>() || d.hasRoute<SettingsRoute>() || d.hasRoute<ModuleRoute>()
            }
        }
    }
}

/**
 * Feature modules and the phase that implements them. Screens for modules not yet built say so
 * plainly instead of showing fake content or dead buttons.
 */
enum class LifeModule(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val phase: Int,
    val summary: String,
    val features: List<String>,
) {
    FAMILY(
        "family", "Family", Icons.Outlined.FamilyRestroom, 3,
        "Private profiles and important dates for everyone at home.",
        listOf("Member profiles", "Birthday reminders", "Family events & tasks", "Emergency contacts", "Document references"),
    ),
    HEALTH(
        "health", "Health", Icons.Outlined.Favorite, 3,
        "Personal wellness organisation — not diagnosis or medical advice.",
        listOf("Medicine reminders (your prescribed doses, never changed)", "Appointments", "Water, sleep, weight, exercise", "Health notes"),
    ),
    CALENDAR(
        "calendar", "Calendar", Icons.Outlined.CalendarMonth, 3,
        "Day, week and month views with reliable reminders.",
        listOf("Events & recurring reminders", "Bill and birthday alerts", "Reboot & time-zone safe scheduling", "Notification permission guide"),
    ),
    GOALS(
        "goals", "Goals", Icons.Outlined.EmojiEvents, 4,
        "Targets, habits and streaks with animated progress.",
        listOf("Goals with milestones", "Daily habits & streaks", "Weekly reports", "Achievement badges"),
    ),
    DOCUMENTS(
        "documents", "Documents", Icons.Outlined.Description, 4,
        "A private vault that never uploads anything.",
        listOf("Import PDF, images, text (Storage Access Framework)", "Folders, tags, categories", "Optional biometric lock & encryption", "Expiry reminders"),
    ),
    NOTES(
        "notes", "Notes", Icons.Outlined.StickyNote2, 2,
        "Quick notes, checklists and linked knowledge.",
        listOf("Rich text & checklists", "Tags, categories, search", "Pin & archive", "Link to tasks and projects"),
    ),
    FINANCE(
        "finance", "Finance", Icons.Outlined.AccountBalanceWallet, 2,
        "A manual personal finance tracker in ₹ — it never connects to your bank.",
        listOf("Income & expenses", "Accounts, cards, loans & EMIs", "Monthly budgets by category", "Savings goals", "Charts & reports"),
    ),
    TASKS(
        "tasks", "Tasks", Icons.Outlined.CheckCircle, 2,
        "Tasks, projects and a Kanban board.",
        listOf("Priorities, due dates, recurrence", "Subtasks & categories", "Projects with milestones", "Kanban: To Do / In Progress / Completed", "Search & filters"),
    );

    companion object {
        fun fromKey(key: String): LifeModule? = entries.firstOrNull { it.key == key }

        /** Order shown on the More screen (spec section 16). */
        val moreSection = listOf(FAMILY, HEALTH, CALENDAR, GOALS, DOCUMENTS, NOTES)
    }
}
