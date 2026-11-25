package com.lifeos.app.domain.model

enum class ThemeMode { DARK, LIGHT, SYSTEM }

enum class MotionPreference { FOLLOW_SYSTEM, FULL, REDUCED }

enum class TransactionType { INCOME, EXPENSE, TRANSFER }

enum class TransactionCategory(val label: String) {
    FOOD("Food"), SHOPPING("Shopping"), TRAVEL("Travel"), MEDICAL("Medical"),
    EDUCATION("Education"), BILLS("Bills"), FAMILY("Family"), ENTERTAINMENT("Entertainment"),
    TRANSPORT("Transport"), SALARY("Salary"), OTHER("Other"),
}

enum class AccountType { CASH, BANK, CREDIT_CARD, LOAN, WALLET }

enum class TaskPriority(val label: String, val weight: Int) {
    CRITICAL("Critical", 4), HIGH("High", 3), MEDIUM("Medium", 2), LOW("Low", 1),
}

enum class TaskStatus { TODO, IN_PROGRESS, DONE }

enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

enum class ReminderType { GENERAL, BILL, TASK, BIRTHDAY, MEDICINE, APPOINTMENT, DOCUMENT, EVENT }

enum class HealthMetric { WATER_ML, SLEEP_MINUTES, WEIGHT_KG, EXERCISE_MINUTES, NOTE }

enum class DocumentCategory(val label: String) {
    PERSONAL("Personal"), FINANCIAL("Financial"), MEDICAL("Medical"), EMPLOYMENT("Employment"),
    EDUCATION("Education"), PROPERTY("Property"), TRAVEL("Travel"), OTHER("Other"),
}

/**
 * REFERENCE: we hold a persisted SAF URI to the user's original file.
 * APP_PRIVATE: copied into app storage (needed when the source can't grant persistent access).
 * ENCRYPTED: copied and encrypted with a Keystore-backed key.
 */
enum class DocumentStorage { REFERENCE, APP_PRIVATE, ENCRYPTED }

enum class GoalType { SAVINGS, LEARNING, PROJECT, READING, FITNESS, HABIT, OTHER }

enum class HabitFrequency { DAILY, WEEKLY }

enum class ProjectStatus { PLANNED, ACTIVE, ON_HOLD, COMPLETED }

enum class MessageRole { USER, ASSISTANT, SYSTEM }

/** Dashboard widgets, in their default order. */
enum class WidgetType(val title: String) {
    TODAY_SCHEDULE("Today's Schedule"),
    FINANCE_OVERVIEW("Finance Overview"),
    TASK_COMPLETION("Task Completion"),
    UPCOMING_REMINDERS("Upcoming Reminders"),
    ACTIVE_GOALS("Active Goals"),
    FAMILY_OVERVIEW("Family Overview"),
    QUICK_NOTES("Quick Notes"),
    RECENT_DOCUMENTS("Recent Documents"),
}
