package com.lifeos.app.feature.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.AnimatedBarChart
import com.lifeos.app.core.designsystem.component.BarDatum
import com.lifeos.app.core.designsystem.component.CardHeader
import com.lifeos.app.core.designsystem.component.ChoiceChips
import com.lifeos.app.core.designsystem.component.DateField
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.FieldLabel
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeSecondaryButton
import com.lifeos.app.core.designsystem.component.LifeSheet
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ListRow
import com.lifeos.app.core.designsystem.component.NoticeCard
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.SegmentedTabs
import com.lifeos.app.core.designsystem.component.SwitchRow
import com.lifeos.app.core.designsystem.component.TimeField
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.core.time.dayRange
import com.lifeos.app.data.local.entity.AppointmentEntity
import com.lifeos.app.data.local.entity.FamilyMemberEntity
import com.lifeos.app.data.local.entity.HealthRecordEntity
import com.lifeos.app.data.local.entity.MedicationReminderEntity
import com.lifeos.app.data.repository.FamilyRepository
import com.lifeos.app.data.repository.HealthRepository
import com.lifeos.app.domain.logic.TimeListParser
import com.lifeos.app.domain.model.HealthMetric
import com.lifeos.app.feature.common.LifeViewModel
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.screenContentPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repo: HealthRepository,
    family: FamilyRepository,
    private val time: TimeProvider,
) : LifeViewModel() {
    private val since = time.now().atZone(time.zone()).toLocalDate().minusDays(29).dayRange(time.zone()).start
    val records = repo.observeRecordsSince(since).stateWhileSubscribed(emptyList())
    val medications = repo.observeMedications().stateWhileSubscribed(emptyList())
    val appointments = repo.observeAppointments().stateWhileSubscribed(emptyList())
    val members = family.observeMembers().stateWhileSubscribed(emptyList())

    fun now() = time.now()
    fun zone() = time.zone()
    fun today() = time.now().atZone(time.zone()).toLocalDate()

    fun add(metric: HealthMetric, value: Double, note: String?, memberId: Long?) =
        act("Logged.") { repo.addRecord(HealthRecordEntity(metric = metric, value = value, note = note, recordedAt = time.now(), familyMemberId = memberId)) }
    fun deleteRecord(r: HealthRecordEntity) = act("Entry removed.") { repo.deleteRecord(r) }
    fun saveMedication(m: MedicationReminderEntity) = act("Medicine reminder saved.") { repo.saveMedication(m) }
    fun deleteMedication(m: MedicationReminderEntity) = act("Medicine reminder deleted.") { repo.deleteMedication(m) }
    fun saveAppointment(a: AppointmentEntity) = act("Appointment saved. You'll be reminded 1 hour before.") { repo.saveAppointment(a) }
    fun deleteAppointment(a: AppointmentEntity) = act("Appointment deleted.") { repo.deleteAppointment(a) }
}

private enum class HealthTab(val label: String) { TODAY("Today"), MEDICINES("Medicines"), APPOINTMENTS("Appointments"), HISTORY("History") }

@Composable
fun HealthScreen(onBack: () -> Unit, vm: HealthViewModel = hiltViewModel()) {
    val records by vm.records.collectAsStateWithLifecycle()
    val meds by vm.medications.collectAsStateWithLifecycle()
    val appts by vm.appointments.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(HealthTab.TODAY) }
    var person by rememberSaveable { mutableStateOf<Long?>(null) }
    var logMetric by remember { mutableStateOf<HealthMetric?>(null) }
    var medEdit by remember { mutableStateOf<Pair<Boolean, MedicationReminderEntity?>?>(null) }
    var apptEdit by remember { mutableStateOf<Pair<Boolean, AppointmentEntity?>?>(null) }
    val colors = LifeTheme.colors
    val zone = vm.zone()
    val today = vm.today()
    val mine = records.filter { it.familyMemberId == person }
    val todays = mine.filter { it.recordedAt.atZone(zone).toLocalDate() == today }
    val memberName: (Long?) -> String = { id -> members.firstOrNull { it.id == id }?.name ?: "Me" }

    MessageBox(vm) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = screenContentPadding(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            item {
                ScreenHeader("Health", "Personal wellness log", onBack = onBack)
                NoticeCard("This module helps you organise your own health information. It does not diagnose, prescribe or give emergency advice, and it never changes medicine doses. In an emergency, call 112.")
                Spacer(Modifier.height(Spacing.sm))
                if (members.isNotEmpty()) {
                    ChoiceChips(listOf<FamilyMemberEntity?>(null) + members, members.firstOrNull { it.id == person }, { it?.name ?: "Me" }, { person = it?.id })
                    Spacer(Modifier.height(Spacing.sm))
                }
                SegmentedTabs(HealthTab.entries, tab, { it.label }, { tab = it })
            }
            when (tab) {
                HealthTab.TODAY -> {
                    item {
                        val water = todays.filter { it.metric == HealthMetric.WATER_ML }.sumOf { it.value }
                        val sleep = todays.filter { it.metric == HealthMetric.SLEEP_MINUTES }.sumOf { it.value }
                        val exercise = todays.filter { it.metric == HealthMetric.EXERCISE_MINUTES }.sumOf { it.value }
                        val weight = mine.firstOrNull { it.metric == HealthMetric.WEIGHT_KG }
                        GlassCard {
                            Text("Today · ${memberName(person)}", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Spacer(Modifier.height(Spacing.sm))
                            TrackerRow(Icons.Outlined.WaterDrop, "Water", "${water.toInt()} ml") {
                                LifeSecondaryButton("+250 ml", onClick = { vm.add(HealthMetric.WATER_ML, 250.0, null, person) })
                                Spacer(Modifier.size(Spacing.xs))
                                LifeSecondaryButton("Custom", onClick = { logMetric = HealthMetric.WATER_ML })
                            }
                            TrackerRow(Icons.Outlined.Bedtime, "Sleep", if (sleep > 0) "%.1f h".format(sleep / 60) else "—") {
                                LifeSecondaryButton("Log", onClick = { logMetric = HealthMetric.SLEEP_MINUTES })
                            }
                            TrackerRow(Icons.Outlined.DirectionsRun, "Exercise", "${exercise.toInt()} min") {
                                LifeSecondaryButton("Log", onClick = { logMetric = HealthMetric.EXERCISE_MINUTES })
                            }
                            TrackerRow(Icons.Outlined.MonitorWeight, "Weight", weight?.let { "%.1f kg".format(it.value) } ?: "—") {
                                LifeSecondaryButton("Log", onClick = { logMetric = HealthMetric.WEIGHT_KG })
                            }
                            TrackerRow(Icons.Outlined.EventNote, "Health note", "${todays.count { it.metric == HealthMetric.NOTE }} today") {
                                LifeSecondaryButton("Write", onClick = { logMetric = HealthMetric.NOTE })
                            }
                        }
                    }
                    item {
                        val upcomingMeds = meds.filter { it.isActive && it.familyMemberId == person }
                        GlassCard {
                            CardHeader(Icons.Outlined.Medication, "Today's medicines")
                            if (upcomingMeds.isEmpty()) Text("No active medicine reminders.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            upcomingMeds.forEach { m -> ListRow(m.medicineName, "${m.doseLabel} · ${m.times.replace(",", ", ")}") }
                        }
                    }
                }
                HealthTab.MEDICINES -> item {
                    GlassCard {
                        CardHeader(Icons.Outlined.Medication, "Medicine reminders") {
                            LifePrimaryButton("Add", onClick = { medEdit = true to null }, icon = Icons.Outlined.Add)
                        }
                        Text("Enter doses exactly as your doctor prescribed. LIFE OS only reminds you; it never recommends or changes doses.",
                            style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                        if (meds.isEmpty()) EmptyState(Icons.Outlined.Medication, "No medicine reminders", "Add a medicine with the times you take it.")
                        meds.forEach { m ->
                            ListRow(
                                m.medicineName,
                                "${m.doseLabel} · ${m.times.replace(",", ", ")} · ${memberName(m.familyMemberId)}" + (m.endDate?.let { " · until ${TimeFormat.shortDate(it)}" } ?: ""),
                                trailing = { LifeChip(if (m.isActive) "Active" else "Paused", if (m.isActive) colors.success else colors.textTertiary) },
                                onClick = { medEdit = true to m },
                            )
                        }
                    }
                }
                HealthTab.APPOINTMENTS -> item {
                    GlassCard {
                        CardHeader(Icons.Outlined.EventNote, "Appointments") {
                            LifePrimaryButton("Add", onClick = { apptEdit = true to null }, icon = Icons.Outlined.Add)
                        }
                        if (appts.isEmpty()) EmptyState(Icons.Outlined.EventNote, "No appointments", "Add doctor or lab visits to get a reminder 1 hour before.")
                        val now = vm.now()
                        val (upcoming, past) = appts.partition { !it.at.isBefore(now) }
                        (upcoming.sortedBy { it.at } + past).forEach { a ->
                            ListRow(
                                a.title,
                                "${TimeFormat.relative(a.at, today, zone)}${a.withWhom?.let { " · $it" } ?: ""}${a.location?.let { " · $it" } ?: ""} · ${memberName(a.familyMemberId)}",
                                trailing = { if (a.at.isBefore(now)) LifeChip("Past", colors.textTertiary) },
                                onClick = { apptEdit = true to a },
                            )
                        }
                    }
                }
                HealthTab.HISTORY -> {
                    item {
                        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
                        GlassCard {
                            CardHeader(Icons.Outlined.WaterDrop, "Water · last 7 days")
                            Spacer(Modifier.height(Spacing.sm))
                            val water = days.map { d -> mine.filter { it.metric == HealthMetric.WATER_ML && it.recordedAt.atZone(zone).toLocalDate() == d }.sumOf { it.value } }
                            AnimatedBarChart(days.mapIndexed { i, d -> BarDatum(d.dayOfWeek.name.take(2), water[i].toFloat()) }, height = 100.dp,
                                accessibilitySummary = "Water per day: " + days.indices.joinToString { "${days[it].dayOfWeek.name.lowercase()} ${water[it].toInt()} ml" })
                            Spacer(Modifier.height(Spacing.md))
                            CardHeader(Icons.Outlined.Bedtime, "Sleep · last 7 days")
                            Spacer(Modifier.height(Spacing.sm))
                            val sleep = days.map { d -> mine.filter { it.metric == HealthMetric.SLEEP_MINUTES && it.recordedAt.atZone(zone).toLocalDate() == d }.sumOf { it.value } / 60 }
                            AnimatedBarChart(days.mapIndexed { i, d -> BarDatum(d.dayOfWeek.name.take(2), sleep[i].toFloat()) }, height = 100.dp,
                                accessibilitySummary = "Sleep hours per day: " + days.indices.joinToString { "%.1f".format(sleep[it]) })
                        }
                    }
                    item {
                        GlassCard {
                            CardHeader(Icons.Outlined.EventNote, "Entries · last 30 days")
                            if (mine.isEmpty()) Text("Nothing logged yet.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            mine.take(60).forEach { r ->
                                ListRow(
                                    metricLabel(r.metric) + ": " + metricValue(r),
                                    TimeFormat.relative(r.recordedAt, today, zone) + (r.note?.let { " · $it" } ?: ""),
                                    trailing = { IconButton(onClick = { vm.deleteRecord(r) }) { Icon(Icons.Outlined.Close, "Delete entry", tint = colors.textTertiary) } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    logMetric?.let { m -> LogSheet(m, onDismiss = { logMetric = null }) { value, note -> vm.add(m, value, note, person); logMetric = null } }
    medEdit?.let { (_, m) -> MedicationSheet(m, members, person, vm) { medEdit = null } }
    apptEdit?.let { (_, a) -> AppointmentSheet(a, members, person, vm) { apptEdit = null } }
}

@Composable
private fun TrackerRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, actions: @Composable () -> Unit) {
    val colors = LifeTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = colors.accentCyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(Spacing.sm))
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            Text(value, style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
        }
        actions()
    }
}

private fun metricLabel(m: HealthMetric) = when (m) {
    HealthMetric.WATER_ML -> "Water"
    HealthMetric.SLEEP_MINUTES -> "Sleep"
    HealthMetric.WEIGHT_KG -> "Weight"
    HealthMetric.EXERCISE_MINUTES -> "Exercise"
    HealthMetric.NOTE -> "Note"
}

private fun metricValue(r: HealthRecordEntity) = when (r.metric) {
    HealthMetric.WATER_ML -> "${r.value.toInt()} ml"
    HealthMetric.SLEEP_MINUTES -> "%.1f h".format(r.value / 60)
    HealthMetric.WEIGHT_KG -> "%.1f kg".format(r.value)
    HealthMetric.EXERCISE_MINUTES -> "${r.value.toInt()} min"
    HealthMetric.NOTE -> "—"
}

@Composable
private fun LogSheet(metric: HealthMetric, onDismiss: () -> Unit, onSave: (Double, String?) -> Unit) {
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val (label, range) = when (metric) {
        HealthMetric.WATER_ML -> "Water (ml)" to 1.0..5000.0
        HealthMetric.SLEEP_MINUTES -> "Sleep (hours)" to 0.25..24.0
        HealthMetric.WEIGHT_KG -> "Weight (kg)" to 1.0..400.0
        HealthMetric.EXERCISE_MINUTES -> "Exercise (minutes)" to 1.0..1440.0
        HealthMetric.NOTE -> "" to 0.0..0.0
    }
    val v = value.replace(',', '.').toDoubleOrNull()
    val valid = if (metric == HealthMetric.NOTE) note.isNotBlank() else v != null && v in range
    LifeSheet("Log ${metricLabel(metric).lowercase()}", onDismiss, saveEnabled = valid, onSave = {
        val stored = when (metric) {
            HealthMetric.SLEEP_MINUTES -> (v ?: 0.0) * 60
            HealthMetric.NOTE -> 0.0
            else -> v ?: 0.0
        }
        onSave(stored, note.trim().ifBlank { null })
    }) {
        if (metric != HealthMetric.NOTE) {
            LifeTextField(value, { value = it.take(8) }, label, Modifier.fillMaxWidth(), keyboardType = KeyboardType.Decimal,
                isError = value.isNotBlank() && !valid, supportingText = if (value.isNotBlank() && !valid) "Enter a value between ${range.start.toInt()} and ${range.endInclusive.toInt()}" else null)
            Spacer(Modifier.height(Spacing.sm))
        }
        LifeTextField(note, { note = it.take(500) }, if (metric == HealthMetric.NOTE) "Health note" else "Note (optional)", Modifier.fillMaxWidth(), singleLine = metric != HealthMetric.NOTE)
    }
}

@Composable
private fun MedicationSheet(item: MedicationReminderEntity?, members: List<FamilyMemberEntity>, defaultPerson: Long?, vm: HealthViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(item?.medicineName.orEmpty()) }
    var dose by remember { mutableStateOf(item?.doseLabel.orEmpty()) }
    var times by remember { mutableStateOf(item?.times?.replace(",", ", ") ?: "08:00, 20:00") }
    var start by remember { mutableStateOf(item?.startDate ?: vm.today()) }
    var end by remember { mutableStateOf(item?.endDate) }
    var active by remember { mutableStateOf(item?.isActive ?: true) }
    var person by remember { mutableStateOf(item?.familyMemberId ?: defaultPerson) }
    var notes by remember { mutableStateOf(item?.notes.orEmpty()) }
    val parsed = TimeListParser.parse(times)
    val e = end
    val valid = name.isNotBlank() && dose.isNotBlank() && parsed != null && (e == null || !e.isBefore(start))
    LifeSheet(
        if (item == null) "Add medicine reminder" else "Edit medicine reminder", onDismiss, saveEnabled = valid,
        onSave = {
            vm.saveMedication((item ?: MedicationReminderEntity(medicineName = name, doseLabel = dose, times = "", startDate = start)).copy(
                medicineName = name.trim(), doseLabel = dose.trim(), times = TimeListParser.format(parsed ?: emptyList()),
                startDate = start, endDate = end, isActive = active, familyMemberId = person, notes = notes.trim().ifBlank { null }))
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteMedication(it); onDismiss() } },
    ) {
        LifeTextField(name, { name = it.take(60) }, "Medicine name", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(dose, { dose = it.take(60) }, "Dose, exactly as prescribed (e.g. 1 tablet 500 mg)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(times, { times = it.take(80) }, "Times (24h, comma separated)", Modifier.fillMaxWidth(),
            isError = parsed == null, supportingText = if (parsed == null) "Use times like 08:00, 14:30, 21:00" else "Reminders at: " + parsed.joinToString { "%d:%02d".format(it.hour, it.minute) })
        Row {
            DateField("Start", start, { d -> if (d != null) start = d }, Modifier.weight(1f), allowClear = false)
            Spacer(Modifier.size(Spacing.sm))
            DateField("End (optional)", end, { end = it }, Modifier.weight(1f))
        }
        if (members.isNotEmpty()) {
            FieldLabel("For")
            ChoiceChips(listOf<FamilyMemberEntity?>(null) + members, members.firstOrNull { it.id == person }, { it?.name ?: "Me" }, { person = it?.id })
        }
        SwitchRow("Active", active, { active = it })
        LifeTextField(notes, { notes = it.take(300) }, "Notes (e.g. after food)", Modifier.fillMaxWidth())
    }
}

@Composable
private fun AppointmentSheet(item: AppointmentEntity?, members: List<FamilyMemberEntity>, defaultPerson: Long?, vm: HealthViewModel, onDismiss: () -> Unit) {
    val zone = vm.zone()
    var title by remember { mutableStateOf(item?.title.orEmpty()) }
    var withWhom by remember { mutableStateOf(item?.withWhom.orEmpty()) }
    var location by remember { mutableStateOf(item?.location.orEmpty()) }
    var date by remember { mutableStateOf(item?.at?.atZone(zone)?.toLocalDate() ?: vm.today().plusDays(1)) }
    var time by remember { mutableStateOf<LocalTime?>(item?.at?.atZone(zone)?.toLocalTime() ?: LocalTime.of(10, 0)) }
    var person by remember { mutableStateOf(item?.familyMemberId ?: defaultPerson) }
    var notes by remember { mutableStateOf(item?.notes.orEmpty()) }
    LifeSheet(
        if (item == null) "Add appointment" else "Edit appointment", onDismiss, saveEnabled = title.isNotBlank() && time != null,
        onSave = {
            val at = date.atTime(time ?: LocalTime.NOON).atZone(zone).toInstant()
            vm.saveAppointment((item ?: AppointmentEntity(title = title, at = at)).copy(
                title = title.trim(), withWhom = withWhom.trim().ifBlank { null }, location = location.trim().ifBlank { null },
                at = at, familyMemberId = person, notes = notes.trim().ifBlank { null }))
            onDismiss()
        },
        onDelete = item?.let { { vm.deleteAppointment(it); onDismiss() } },
    ) {
        LifeTextField(title, { title = it.take(80) }, "Title (e.g. Dental check-up)", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(withWhom, { withWhom = it.take(60) }, "Doctor / with", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(location, { location = it.take(80) }, "Location", Modifier.fillMaxWidth())
        Row {
            DateField("Date", date, { d -> if (d != null) date = d }, Modifier.weight(1f), allowClear = false)
            Spacer(Modifier.size(Spacing.sm))
            TimeField("Time", time, { time = it }, Modifier.weight(1f), allowClear = false)
        }
        if (members.isNotEmpty()) {
            FieldLabel("For")
            ChoiceChips(listOf<FamilyMemberEntity?>(null) + members, members.firstOrNull { it.id == person }, { it?.name ?: "Me" }, { person = it?.id })
        }
        LifeTextField(notes, { notes = it.take(300) }, "Notes", Modifier.fillMaxWidth())
    }
}
