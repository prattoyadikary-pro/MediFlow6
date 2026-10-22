package com.example.ui.screens.medicines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MedicineEntity
import com.example.data.local.entity.MedicineScheduleEntity
import com.example.data.model.FrequencyType
import com.example.data.model.MealInstruction
import com.example.data.model.MedicineCategory
import com.example.ui.components.PillGraphic
import com.example.ui.components.ShowcaseGlassCard
import com.example.ui.components.ShowcasePillButton
import com.example.ui.theme.MediFlowBorderSubtle
import com.example.ui.theme.ShowcaseTealPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMedicineScreen(
    initialMedicine: MedicineEntity? = null,
    initialSchedule: MedicineScheduleEntity? = null,
    onBack: () -> Unit,
    onSave: (MedicineEntity, MedicineScheduleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var name by remember { mutableStateOf(initialMedicine?.name ?: "") }
    var dosage by remember { mutableStateOf(initialMedicine?.strength ?: "500 mg") }
    var category by remember { mutableStateOf(initialMedicine?.category ?: MedicineCategory.TABLET) }
    var frequencyType by remember { mutableStateOf(initialSchedule?.frequencyType ?: FrequencyType.ONCE_DAILY) }
    var selectedTime by remember { mutableStateOf(initialSchedule?.scheduledTimes?.firstOrNull() ?: "10:00 AM") }
    var startDateText by remember { mutableStateOf("29 Apr 2025") }
    var selectedColorHex by remember { mutableStateOf(initialMedicine?.colorHex ?: "#0284C7") }
    var nameError by remember { mutableStateOf(false) }

    var showTypeMenu by remember { mutableStateOf(false) }
    var showFreqMenu by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = if (isDark) Color(0x99081D25) else Color(0xCCFFFFFF),
        unfocusedContainerColor = if (isDark) Color(0x66081D25) else Color(0xB3F8FCFC),
        focusedBorderColor = ShowcaseTealPrimary,
        unfocusedBorderColor = if (isDark) Color(0x4D27B9C0) else MediFlowBorderSubtle
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (initialMedicine == null) "Add Medicine" else "Edit Medicine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Single Cohesive Frosted Glass Form Card matching Screen 4
        ShowcaseGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero 3D Pill Preview Box with Camera Icon at top-right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x3327B9C0) else Color(0xFFE0F2F4))
                            .clickable {
                                // Cycle pill visual style on tap
                                selectedColorHex = when (selectedColorHex) {
                                    "#0284C7" -> "#DF6666"
                                    "#DF6666" -> "#E9A23B"
                                    else -> "#0284C7"
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Pill Style",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PillGraphic(
                            colorHex = selectedColorHex,
                            size = 96.dp,
                            isSquareTile = true
                        )
                    }
                }

                // 1. Medicine Name
                Column {
                    Text(
                        text = "Medicine Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = it.isBlank()
                        },
                        placeholder = { Text("Paracetamol", fontSize = 14.sp) },
                        isError = nameError,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_medicine_name")
                    )
                }

                // 2. Dosage
                Column {
                    Text(
                        text = "Dosage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        placeholder = { Text("500 mg", fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 3. Medicine Type (Dropdown field matching Screen 4)
                Column {
                    Text(
                        text = "Medicine Type",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0x66081D25) else Color(0xB3F8FCFC))
                                .border(
                                    1.dp,
                                    if (isDark) Color(0x4D27B9C0) else MediFlowBorderSubtle,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { showTypeMenu = true }
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false }
                        ) {
                            MedicineCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        category = cat
                                        showTypeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Frequency (Dropdown field matching Screen 4)
                Column {
                    Text(
                        text = "Frequency",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Color(0x66081D25) else Color(0xB3F8FCFC))
                                .border(
                                    1.dp,
                                    if (isDark) Color(0x4D27B9C0) else MediFlowBorderSubtle,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { showFreqMenu = true }
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = frequencyType.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        DropdownMenu(
                            expanded = showFreqMenu,
                            onDismissRequest = { showFreqMenu = false }
                        ) {
                            FrequencyType.entries.take(4).forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq.displayName) },
                                    onClick = {
                                        frequencyType = freq
                                        showFreqMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. Time Field with Clock Icon matching Screen 4
                Column {
                    Text(
                        text = "Time",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x66081D25) else Color(0xB3F8FCFC))
                            .border(
                                1.dp,
                                if (isDark) Color(0x4D27B9C0) else MediFlowBorderSubtle,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { showTimePickerDialog = true }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedTime,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Select Time",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 6. Start Date Field with Calendar Icon matching Screen 4
                Column {
                    Text(
                        text = "Start Date",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x66081D25) else Color(0xB3F8FCFC))
                            .border(
                                1.dp,
                                if (isDark) Color(0x4D27B9C0) else MediFlowBorderSubtle,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { showDatePickerDialog = true }
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = startDateText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Start Date",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // "Save Medicine" Prominent Button inside the glass card matching Screen 4
                ShowcasePillButton(
                    text = "Save Medicine",
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@ShowcasePillButton
                        }

                        val med = initialMedicine?.copy(
                            name = name.trim(),
                            category = category,
                            strength = dosage.trim(),
                            doseAmount = 1.0,
                            doseUnit = category.displayName.lowercase(),
                            mealInstruction = MealInstruction.AFTER_FOOD,
                            colorHex = selectedColorHex,
                            remainingQuantity = 30,
                            refillThreshold = 5
                        ) ?: MedicineEntity(
                            name = name.trim(),
                            category = category,
                            strength = dosage.trim(),
                            doseAmount = 1.0,
                            doseUnit = category.displayName.lowercase(),
                            mealInstruction = MealInstruction.AFTER_FOOD,
                            colorHex = selectedColorHex,
                            remainingQuantity = 30,
                            refillThreshold = 5
                        )

                        val sched = initialSchedule?.copy(
                            frequencyType = frequencyType,
                            scheduledTimes = listOf(selectedTime),
                            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
                            intervalHours = 8
                        ) ?: MedicineScheduleEntity(
                            medicineId = 0,
                            frequencyType = frequencyType,
                            scheduledTimes = listOf(selectedTime),
                            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
                            intervalHours = 8
                        )

                        onSave(med, sched)
                    },
                    testTag = "btn_save_medicine"
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showTimePickerDialog) {
        val timePresets = listOf("08:00 AM", "09:00 AM", "10:00 AM", "02:00 PM", "06:00 PM", "08:00 PM", "10:00 PM")
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Select Reminder Time", fontWeight = FontWeight.Bold) },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timePresets.forEach { preset ->
                        val isSelected = preset == selectedTime
                        Surface(
                            color = if (isSelected) ShowcaseTealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                selectedTime = preset
                                showTimePickerDialog = false
                            }
                        ) {
                            Text(
                                text = preset,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showDatePickerDialog) {
        val datePresets = listOf("29 Apr 2025", "30 Apr 2025", "1 May 2025", "Today")
        AlertDialog(
            onDismissRequest = { showDatePickerDialog = false },
            title = { Text("Select Start Date", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    datePresets.forEach { preset ->
                        Surface(
                            color = if (preset == startDateText) ShowcaseTealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    startDateText = preset
                                    showDatePickerDialog = false
                                }
                        ) {
                            Text(
                                text = preset,
                                color = if (preset == startDateText) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
