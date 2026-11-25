package com.lifeos.app.feature.family

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.app.core.designsystem.component.DateField
import com.lifeos.app.core.designsystem.component.EmptyState
import com.lifeos.app.core.designsystem.component.GlassCard
import com.lifeos.app.core.designsystem.component.LifeChip
import com.lifeos.app.core.designsystem.component.LifePrimaryButton
import com.lifeos.app.core.designsystem.component.LifeSheet
import com.lifeos.app.core.designsystem.component.LifeTextField
import com.lifeos.app.core.designsystem.component.ListRow
import com.lifeos.app.core.designsystem.component.ScreenHeader
import com.lifeos.app.core.designsystem.component.SwitchRow
import com.lifeos.app.core.designsystem.theme.LifeTheme
import com.lifeos.app.core.designsystem.theme.Spacing
import com.lifeos.app.core.designsystem.theme.WidthClass
import com.lifeos.app.core.format.TimeFormat
import com.lifeos.app.core.time.TimeProvider
import com.lifeos.app.data.local.entity.FamilyMemberEntity
import com.lifeos.app.data.repository.FamilyRepository
import com.lifeos.app.domain.usecase.DashboardMath
import com.lifeos.app.feature.common.LifeViewModel
import com.lifeos.app.feature.common.MessageBox
import com.lifeos.app.navigation.screenContentPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Period
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(private val repo: FamilyRepository, private val time: TimeProvider) : LifeViewModel() {
    val members = repo.observeMembers().stateWhileSubscribed(emptyList())
    fun today() = time.now().atZone(time.zone()).toLocalDate()
    fun now() = time.now()
    fun save(m: FamilyMemberEntity) = act("Saved ${m.name}.") { repo.save(m) }
    fun delete(m: FamilyMemberEntity) = act("Removed ${m.name}.") { repo.delete(m) }
}

/**
 * Family members are private records on this device. Their tasks, events, documents and
 * health entries link here; nothing is shared externally.
 */
@Composable
fun FamilyScreen(onBack: () -> Unit, vm: FamilyViewModel = hiltViewModel()) {
    val members by vm.members.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf<Pair<Boolean, FamilyMemberEntity?>?>(null) }
    val colors = LifeTheme.colors
    val context = LocalContext.current
    val today = vm.today()
    val columns = when (LifeTheme.layout.widthClass) { WidthClass.Compact -> 1; WidthClass.Medium -> 2; WidthClass.Expanded -> 3 }
    val emergency = members.filter { it.isEmergencyContact }

    MessageBox(vm) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns), contentPadding = screenContentPadding(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScreenHeader("Family", "Private profiles, birthdays and emergency contacts.", onBack = onBack) {
                    LifePrimaryButton("Member", onClick = { edit = true to null }, icon = Icons.Outlined.Add)
                }
            }
            if (emergency.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard(strong = true) {
                    Text("Emergency contacts", style = MaterialTheme.typography.titleMedium, color = colors.danger)
                    emergency.forEach { m ->
                        ListRow(m.name, "${m.relation}${m.phone?.let { " · $it" } ?: ""}", trailing = {
                            m.phone?.let { phone ->
                                IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }) {
                                    Icon(Icons.Outlined.Call, "Call ${m.name}", tint = colors.success)
                                }
                            }
                        })
                    }
                }
            }
            if (members.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard { EmptyState(Icons.Outlined.FamilyRestroom, "No family members yet", "Add people to track birthdays, important dates and contacts.") }
            }
            items(members, key = { it.id }) { m ->
                val next = m.birthDate?.let { DashboardMath.nextBirthday(it, today) }
                val age = m.birthDate?.let { Period.between(it, today).years }
                GlassCard(onClick = { edit = true to m }, onClickLabel = "Edit ${m.name}") {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(colors.accentBrush), contentAlignment = Alignment.Center) {
                            Text(m.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = Color.White)
                        }
                        Spacer(Modifier.size(Spacing.md))
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(m.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(m.relation + (age?.let { " · $it yrs" } ?: ""), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                        if (m.isEmergencyContact) LifeChip("SOS", colors.danger)
                    }
                    if (next != null) {
                        Spacer(Modifier.height(Spacing.sm))
                        val days = java.time.temporal.ChronoUnit.DAYS.between(today, next)
                        Text(
                            "🎂 " + when (days) { 0L -> "Birthday today!"; 1L -> "Birthday tomorrow"; else -> "Birthday ${TimeFormat.shortDate(next)} · in $days days" },
                            style = MaterialTheme.typography.bodyMedium, color = colors.accentCyan,
                        )
                    }
                    m.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 3) }
                }
            }
        }
    }

    edit?.let { (_, m) -> MemberSheet(m, vm) { edit = null } }
}

@Composable
private fun MemberSheet(item: FamilyMemberEntity?, vm: FamilyViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(item?.name.orEmpty()) }
    var relation by remember { mutableStateOf(item?.relation.orEmpty()) }
    var birth by remember { mutableStateOf(item?.birthDate) }
    var phone by remember { mutableStateOf(item?.phone.orEmpty()) }
    var emergency by remember { mutableStateOf(item?.isEmergencyContact ?: false) }
    var notes by remember { mutableStateOf(item?.notes.orEmpty()) }
    val phoneValid = phone.isBlank() || Regex("""^\+?[0-9 ()-]{6,20}$""").matches(phone.trim())
    val birthValid = birth?.isAfter(vm.today()) != true
    LifeSheet(
        if (item == null) "Add family member" else "Edit ${item.name}", onDismiss,
        saveEnabled = name.isNotBlank() && relation.isNotBlank() && phoneValid && birthValid,
        onSave = {
            vm.save((item ?: FamilyMemberEntity(name = name, relation = relation, createdAt = vm.now())).copy(
                name = name.trim(), relation = relation.trim(), birthDate = birth, phone = phone.trim().ifBlank { null },
                isEmergencyContact = emergency, notes = notes.trim().ifBlank { null }))
            onDismiss()
        },
        onDelete = item?.let { { vm.delete(it); onDismiss() } },
        deleteMessage = "Removes this person, their birthday reminder and their health records. Linked expenses, tasks and documents are kept.",
    ) {
        LifeTextField(name, { name = it.take(50) }, "Name", Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(relation, { relation = it.take(30) }, "Relation (e.g. Mother, Son)", Modifier.fillMaxWidth())
        DateField("Birthday (creates a yearly reminder)", birth, { birth = it })
        if (!birthValid) Text("Birthday can't be in the future.", color = LifeTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(Spacing.sm))
        LifeTextField(phone, { phone = it.take(20) }, "Phone (optional)", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Phone, isError = !phoneValid)
        SwitchRow("Emergency contact", emergency, { emergency = it })
        LifeTextField(notes, { notes = it.take(500) }, "Important notes", Modifier.fillMaxWidth(), singleLine = false)
    }
}
