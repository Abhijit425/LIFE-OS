package com.lifeos.app.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.ChoiceChips
import com.lifeos.app.core.designsystem.component.DateField
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.FieldLabel
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeSheet
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ListRow
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.SegmentedTabs
import com.lifeos.app.core.designsystem.component.SwitchRow
import com.lifeos.app.core.designsystem.component.TimeField
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Radii
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.core.time.dayRange
import com.lifeos.app.data.local.entity.AppointmentEntity
import com.lifeos.app.data.local.entity.CalendarEventEntity
import com.lifeos.app.data.local.entity.ReminderEntity
import com.lifeos.app.data.repository.CalendarRepository
import com.lifeos.app.data.repository.HealthRepository
import com.lifeos.app.data.repository.ReminderEngine
import com.lifeos.app.domain.model.Recurrence
import com.lifeos.app.domain.model.ReminderType
import com.lifeos.app.feature.common.LifeViewModel
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.feature.common.ReminderReadinessCard
import com.lifeos.app.navigation.screenContentPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repo: CalendarRepository,
    private val reminders: ReminderEngine,
    health: HealthRepository,
    private val time: TimeProvider,
) : LifeViewModel() {
    val selected = MutableStateFlow(today())
    val month = MutableStateFlow(YearMonth.from(today()))

    /** Events for the visible month plus a week either side (covers the grid and week view). */
    val events: StateFlow<List<CalendarEventEntity>> = month.flatMapLatest { m ->
        val zone = time.zone()
        repo.observeRange(m.atDay(1).minusDays(7).dayRange(zone).start, m.atEndOfMonth().plusDays(8).dayRange(zone).start)
    }.stateWhileSubscribed(emptyList())
    val reminderList = reminders.observeAll().stateWhileSubscribed(emptyList())
    val appointments = health.observeAppointments().stateWhileSubscribed(emptyList())

    fun today(): LocalDate = time.now().atZone(time.zone()).toLocalDate()
    fun zone() = time.zone()
    fun now() = time.now()

    fun select(d: LocalDate) {
        selected.value = d
        month.value = YearMonth.from(d)
    }

    fun saveEvent(e: CalendarEventEntity) = act("Event saved.") { repo.save(e) }
    fun deleteEvent(e: CalendarEventEntity) = act("Event deleted.") { repo.delete(e) }
    fun saveReminder(r: ReminderEntity) = act("Reminder saved.") {
        require(r.recurrence != Recurrence.NONE || r.triggerAt.isAfter(time.now())) { "That time has already passed. Pick a future time." }
        reminders.save(r)
    }
    fun deleteReminder(r: ReminderEntity) = act("Reminder deleted.") { reminders.delete(r) }
    fun toggleDone(r: ReminderEntity) = act { reminders.setCompleted(r, !r.isCompleted) }
    fun toggleEnabled(r: ReminderEntity) = act { reminders.setEnabled(r, !r.isEnabled) }
}

private enum class CalView(val label: String) { MONTH("Month"), WEEK("Week"), DAY("Day"), REMINDERS("Reminders") }

private sealed interface Agenda {
    val at: java.time.Instant
    data class Ev(val e: CalendarEventEntity) : Agenda { override val at get() = e.startAt }
    data class Ap(val a: AppointmentEntity) : Agenda { override val at get() = a.at }
    data class Re(val r: ReminderEntity) : Agenda { override val at get() = r.triggerAt }
}

@Composable
fun CalendarScreen(onBack: () -> Unit, vm: CalendarViewModel = hiltViewModel()) {
    val selected by vm.selected.collectAsStateWithLifecycle()
    val month by vm.month.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val reminders by vm.reminderList.collectAsStateWithLifecycle()
    val appts by vm.appointments.collectAsStateWithLifecycle()
    var view by rememberSaveable { mutableStateOf(CalView.MONTH) }
    var eventEdit by remember { mutableStateOf<Pair<Boolean, CalendarEventEntity?>?>(null) }
    var reminderEdit by remember { mutableStateOf<Pair<Boolean, ReminderEntity?>?>(null) }
    val zone = vm.zone()
    val today = vm.today()
    val colors = LifeTheme.colors

    fun agendaFor(d: LocalDate): List<Agenda> {
        val r = d.dayRange(zone)
        return (events.filter { it.startAt < r.end && it.endAt > r.start }.map { Agenda.Ev(it) } +
            appts.filter { it.at >= r.start && it.at < r.end }.map { Agenda.Ap(it) } +
            reminders.filter { !it.isCompleted && it.triggerAt >= r.start && it.triggerAt < r.end }.map { Agenda.Re(it) }).sortedBy { it.at }
    }

    MessageBox(vm) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = screenContentPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            item {
                ScreenHeader("Calendar", TimeFormat.longDate(selected), onBack = onBack) {
                    if (view == CalView.REMINDERS) LifePrimaryButton("Reminder", onClick = { reminderEdit = true to null }, icon = Icons.Outlined.Add)
                    else LifePrimaryButton("Event", onClick = { eventEdit = true to null }, icon = Icons.Outlined.Add)
                }
                SegmentedTabs(CalView.entries, view, { it.label }, { view = it })
            }
            item { ReminderReadinessCard() }
            when (view) {
                CalView.MONTH -> {
                    item {
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.select(selected.minusMonths(1).withDayOfMonth(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous month", tint = colors.textPrimary) }
                                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                TextButton(onClick = { vm.select(today) }) { Text("Today", color = colors.accentCyan) }
                                IconButton(onClick = { vm.select(selected.plusMonths(1).withDayOfMonth(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next month", tint = colors.textPrimary) }
                            }
                            MonthGrid(month, selected, today, count = { d -> agendaFor(d).size }, onPick = { vm.select(it) })
                        }
                    }
                    item { DayAgenda(selected, agendaFor(selected), vm, { eventEdit = true to it }, { reminderEdit = true to it }) }
                }
                CalView.WEEK -> {
                    val monday = selected.with(DayOfWeek.MONDAY)
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { vm.select(selected.minusWeeks(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous week", tint = colors.textPrimary) }
                            Text("Week of ${TimeFormat.shortDate(monday)}", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.select(selected.plusWeeks(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next week", tint = colors.textPrimary) }
                        }
                    }
                    (0L..6L).forEach { i ->
                        val d = monday.plusDays(i)
                        item(key = "w$d") { DayAgenda(d, agendaFor(d), vm, { eventEdit = true to it }, { reminderEdit = true to it }, compact = true) }
                    }
                }
                CalView.DAY -> {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { vm.select(selected.minusDays(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous day", tint = colors.textPrimary) }
                            Text(TimeFormat.longDate(selected), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.select(today) }) { Text("Today", color = colors.accentCyan) }
                            IconButton(onClick = { vm.select(selected.plusDays(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next day", tint = colors.textPrimary) }
                        }
                    }
                    item { DayAgenda(selected, agendaFor(selected), vm, { eventEdit = true to it }, { reminderEdit = true to it }) }
                }
                CalView.REMINDERS -> item {
                    GlassCard {
                        CardHeader(Icons.Outlined.NotificationsActive, "All reminders")
                        if (reminders.isEmpty()) EmptyState(Icons.Outlined.NotificationsActive, "No reminders", "Create bill, birthday or general reminders. They survive restarts and time-zone changes.")
                        reminders.forEach { r ->
                            ListRow(
                                r.title,
                                "${TimeFormat.relative(r.triggerAt, today, zone)} · ${typeLabel(r.type)}" + (if (r.recurrence != Recurrence.NONE) " · ${r.recurrence.name.lowercase()}" else ""),
                                strikeThrough = r.isCompleted,
                                trailing = {
                                    Switch(checked = r.isEnabled && !r.isCompleted, onCheckedChange = { vm.toggleEnabled(r) }, enabled = !r.isCompleted,
                                        modifier = Modifier.semantics { contentDescription = "Enable ${r.title}" })
                                },
                                onClick = { reminderEdit = true to r },
                            )
                        }
                    }
                }
            }
        }
    }

    eventEdit?.let { (_, e) -> EventSheet(e, selected, vm) { eventEdit = null } }
    reminderEdit?.let { (_, r) -> ReminderSheet(r, selected, vm) { reminderEdit = null } }
}

/** Monday-first month grid with a dot on days that have items. */
@Composable
private fun MonthGrid(month: YearMonth, selected: LocalDate, today: LocalDate, count: (LocalDate) -> Int, onPick: (LocalDate) -> Unit) {
    val colors = LifeTheme.colors
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1 // Monday-first
    val cells = (0 until 42).map { first.minusDays(offset.toLong()).plusDays(it.toLong()) }
    Column {
        Row(Modifier.fillMaxWidth()) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textTertiary, modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        cells.chunked(7).forEach { week ->
            if (week.any { YearMonth.from(it) == month }) Row(Modifier.fillMaxWidth()) {
                week.forEach { d ->
                    val inMonth = YearMonth.from(d) == month
                    val isSel = d == selected
                    val n = count(d)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1.15f)
                            .padding(2.dp)
                            .clip(Radii.sm)
                            .background(if (isSel) colors.accentPurple.copy(alpha = 0.35f) else Color.Transparent)
                            .border(1.dp, if (d == today) colors.accentCyan else Color.Transparent, Radii.sm)
                            .clickable(role = Role.Button, onClickLabel = "Select ${TimeFormat.shortDate(d)}") { onPick(d) }
                            .semantics { contentDescription = "${TimeFormat.longDate(d)}, $n items" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, color = if (inMonth) colors.textPrimary else colors.textTertiary)
                            if (n > 0) Box(Modifier.size(6.dp).clip(CircleShape).background(colors.accentCyan))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayAgenda(
    date: LocalDate,
    items: List<Agenda>,
    vm: CalendarViewModel,
    onEvent: (CalendarEventEntity) -> Unit,
    onReminder: (ReminderEntity) -> Unit,
    compact: Boolean = false,
) {
    val colors = LifeTheme.colors
    val zone = vm.zone()
    GlassCard {
        Text(TimeFormat.longDate(date), style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            color = if (date == vm.today()) colors.accentCyan else colors.textPrimary)
        if (items.isEmpty()) {
            if (!compact) EmptyState(Icons.Outlined.Event, "Nothing scheduled", "Add an event or reminder for this day.")
            else Text("—", color = colors.textTertiary)
        }
        items.forEach { a ->
            when (a) {
                is Agenda.Ev -> ListRow(a.e.title,
                    if (a.e.allDay) "All day" else "${TimeFormat.time(a.e.startAt, zone)} – ${TimeFormat.time(a.e.endAt, zone)}" + (a.e.location?.let { " · $it" } ?: ""),
                    trailing = { LifeChip("Event", colors.accentCyan) }, onClick = { onEvent(a.e) })
                is Agenda.Ap -> ListRow(a.a.title, TimeFormat.time(a.a.at, zone) + (a.a.withWhom?.let { " · $it" } ?: ""), trailing = { LifeChip("Health", colors.success) })
                is Agenda.Re -> ListRow(a.r.title, "${TimeFormat.time(a.r.triggerAt, zone)} · ${typeLabel(a.r.type)}",
                    trailing = { LifeChip("Reminder", colors.accentPurple) }, onClick = { onReminder(a.r) })
            }
        }
    }
}

private fun typeLabel(t: ReminderType) = t.name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun EventSheet(item: CalendarEventEntity?, day: LocalDate, vm: CalendarViewModel, onDismiss: () -> Unit) {
    val zone = vm.zone()
    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var date by remember { mutableStateOf(item?.startAt?.atZone(zone)?.toLocalDate() ?: day) }
    var start by remember { mutableStateOf<LocalTime?>(item?.startAt?.atZone(zone)?.toLocalTime() ?: LocalTime.of(10, 0)) }
    var end by remember { mutableStateOf<LocalTime?>(item?.endAt?.atZone(zone)?.toLocalTime() ?: LocalTime.of(11, 0)) }
    var allDay by remember { mutableStateOf(item?.allDay ?: false) }
    var location by remember { mutableStateOf(item?.location.orEmpty()) }
    var description by remember { mutableStateOf(item?.description.orEmpty()) }
    var remind by remember { mutableStateOf(item == null) }
    val s = start
    val e = end
    val valid = title.isNotBlank() && (allDay || (s != null && e != null && e.isAfter(s)))
    LifeSheet(
        if (item == null) "New event" else "Edit event", onDismiss, saveEnabled = valid,
        onSave = {
            val startAt = if (allDay) date.atStartOfDay(zone).toInstant() else date.atTime(s ?: LocalTime.NOON).atZone(zone).toInstant()
            val endAt = if (allDay) date.plusDays(1).atStartOfDay(zone).toInstant() else date.atTime(e ?: LocalTime.NOON).atZone(zone).toInstant()
            vm.saveEvent((item ?: CalendarEventEntity(title = title, startAt = startAt, endAt = endAt, createdAt = vm.now())).copy(
                title = title.trim(), startAt = startAt, endAt = endAt, allDay = allDay, location = location.trim().ifBlank { null },
                description = description.trim().ifBlank { null }))
            if (remind && item == null) {
                val alertAt = if (allDay) date.atTime(9, 0).atZone(zone).toInstant() else startAt.minusSeconds(15 * 60)
                if (alertAt.isAfter(vm.now())) vm.saveReminder(ReminderEntity(title = title.trim(), message = "Event" + (if (allDay) " today" else " in 15 minutes"),
                    type = ReminderType.EVENT, triggerAt = alertAt, createdAt = vm.now()))
            }
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteEvent(it); onDismiss() } },
    ) {
        LifeTextField(title, { title = it.take(100) }, "Title", Modifier.fillMaxWidth())
        DateField("Date", date, { d -> if (d != null) date = d }, allowClear = false)
        SwitchRow("All day", allDay, { allDay = it })
        if (!allDay) Row {
            TimeField("Starts", start, { start = it }, Modifier.weight(1f), allowClear = false)
            Spacer(Modifier.size(Spacing.sm))
            TimeField("Ends", end, { end = it }, Modifier.weight(1f), allowClear = false)
        }
        if (!valid && title.isNotBlank()) Text("End time must be after start time.", color = LifeTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(location, { location = it.take(100) }, "Location", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(description, { description = it.take(1000) }, "Description", Modifier.fillMaxWidth(), singleLine = false)
        if (item == null) SwitchRow("Remind me", remind, { remind = it }, if (allDay) "At 9 AM on the day" else "15 minutes before")
    }
}

@Composable
private fun ReminderSheet(item: ReminderEntity?, day: LocalDate, vm: CalendarViewModel, onDismiss: () -> Unit) {
    val zone = vm.zone()
    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var message by remember { mutableStateOf(item?.message.orEmpty()) }
    var type by remember { mutableStateOf(item?.type ?: ReminderType.GENERAL) }
    var date by remember { mutableStateOf(item?.triggerAt?.atZone(zone)?.toLocalDate() ?: day) }
    var time by remember { mutableStateOf<LocalTime?>(item?.triggerAt?.atZone(zone)?.toLocalTime() ?: LocalTime.of(9, 0)) }
    var recurrence by remember { mutableStateOf(item?.recurrence ?: Recurrence.NONE) }
    LifeSheet(
        if (item == null) "New reminder" else "Edit reminder", onDismiss, saveEnabled = title.isNotBlank() && time != null,
        onSave = {
            val at = date.atTime(time ?: LocalTime.of(9, 0)).atZone(zone).toInstant()
            vm.saveReminder((item ?: ReminderEntity(title = title, triggerAt = at, createdAt = vm.now())).copy(
                title = title.trim(), message = message.trim().ifBlank { null }, type = type, triggerAt = at, recurrence = recurrence,
                isCompleted = false, isEnabled = true))
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteReminder(it); onDismiss() } },
    ) {
        LifeTextField(title, { title = it.take(100) }, "Remind me to…", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(message, { message = it.take(200) }, "Details (optional)", Modifier.fillMaxWidth())
        FieldLabel("Type")
        ChoiceChips(listOf(ReminderType.GENERAL, ReminderType.BILL, ReminderType.BIRTHDAY, ReminderType.APPOINTMENT, ReminderType.DOCUMENT, ReminderType.EVENT),
            type, { typeLabel(it) }, { type = it })
        Row {
            DateField("Date", date, { d -> if (d != null) date = d }, Modifier.weight(1f), allowClear = false)
            Spacer(Modifier.size(Spacing.sm))
            TimeField("Time", time, { time = it }, Modifier.weight(1f), allowClear = false)
        }
        FieldLabel("Repeats")
        ChoiceChips(Recurrence.entries, recurrence, { if (it == Recurrence.NONE) "Never" else it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, { recurrence = it })
        if (item != null) {
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick = { vm.toggleDone(item); onDismiss() }) {
                Text(if (item.isCompleted) "Mark as not done" else "Mark as done", color = LifeTheme.colors.accentCyan)
            }
        }
    }
}
