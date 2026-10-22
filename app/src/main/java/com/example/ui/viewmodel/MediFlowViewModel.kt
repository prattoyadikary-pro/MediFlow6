package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MediFlowApplication
import com.example.data.local.entity.HydrationLogEntity
import com.example.data.local.entity.HydrationProfileEntity
import com.example.data.local.entity.MedicineEntity
import com.example.data.local.entity.MedicineScheduleEntity
import com.example.data.model.AdherenceSummary
import com.example.data.model.BeverageType
import com.example.data.model.DailyHydrationSummary
import com.example.data.model.DoseItem
import com.example.data.model.MedicineWithSchedule
import com.example.data.model.UnitSystem
import com.example.data.remote.UserProfile
import com.example.data.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MediFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MediFlowApplication).repository
    private val userProfileRepository = UserProfileRepository(application)

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _profileStatusMessage = MutableStateFlow<String?>(null)
    val profileStatusMessage: StateFlow<String?> = _profileStatusMessage.asStateFlow()

    private var profileObserverJob: Job? = null
    private var observedUid: String? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialShowcaseData()
        }
    }

    // Next scheduled dose for home hero
    val nextDose: StateFlow<DoseItem?> = repository.getNextPendingDoseItem()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Today's doses
    val todayDoses: StateFlow<List<DoseItem>> = repository.getDoseItemsForDay(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All medicines with schedule
    val allMedicines: StateFlow<List<MedicineWithSchedule>> = repository.allMedicinesWithSchedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's hydration summary
    val todayHydration: StateFlow<DailyHydrationSummary> = repository.getTodayHydrationSummary()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DailyHydrationSummary(System.currentTimeMillis(), 0, 2000, 0f, 0)
        )

    // Hydration profile
    val hydrationProfile: StateFlow<HydrationProfileEntity> = repository.getHydrationProfile()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HydrationProfileEntity()
        )

    // Selected Date for History Screen
    private val _selectedDateEpochMs = MutableStateFlow(System.currentTimeMillis())
    val selectedDateEpochMs: StateFlow<Long> = _selectedDateEpochMs.asStateFlow()

    // History doses for selected date
    val selectedDateDoses: StateFlow<List<DoseItem>> = _selectedDateEpochMs.flatMapLatest { epochMs ->
        repository.getDoseItemsForDay(epochMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History adherence for selected date
    val selectedDateAdherence: StateFlow<AdherenceSummary> = _selectedDateEpochMs.flatMapLatest { epochMs ->
        repository.getAdherenceSummaryForDay(epochMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdherenceSummary(0, 0, 0, 0, 100f))

    // History hydration logs for selected date
    val selectedDateHydrationLogs: StateFlow<List<HydrationLogEntity>> = _selectedDateEpochMs.flatMapLatest { epochMs ->
        repository.getHydrationLogsForDay(epochMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Preferences
    val themeMode: StateFlow<String> = repository.preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val unitSystem: StateFlow<UnitSystem> = repository.preferences.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnitSystem.METRIC)

    val snoozeMinutes: StateFlow<Int> = repository.preferences.snoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val hideNamesOnLockScreen: StateFlow<Boolean> = repository.preferences.hideNamesOnLockScreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val quietHoursEnabled: StateFlow<Boolean> = repository.preferences.quietHoursEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val quietHoursStart: StateFlow<String> = repository.preferences.quietHoursStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "22:00")

    val quietHoursEnd: StateFlow<String> = repository.preferences.quietHoursEnd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "07:00")

    fun syncAuthenticatedUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
        if (observedUid == uid && profileObserverJob?.isActive == true) return

        observedUid = uid
        profileObserverJob?.cancel()

        // Immediately reflect Google account defaults while Firestore loads
        val defaultName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "Alex Johnson"
        val defaultEmail = currentUser.email?.takeIf { it.isNotBlank() } ?: "alex@example.com"
        _userProfile.value = _userProfile.value.copy(
            id = uid,
            userId = uid,
            displayName = defaultName,
            email = defaultEmail,
            avatarUrl = currentUser.photoUrl?.toString() ?: ""
        )

        profileObserverJob = viewModelScope.launch {
            // Ensure profile document exists in Firestore
            val existingResult = userProfileRepository.getUserProfileById(uid)
            if (existingResult.isSuccess && existingResult.getOrNull() == null) {
                userProfileRepository.saveUserProfile(
                    displayName = defaultName,
                    email = defaultEmail,
                    avatarUrl = currentUser.photoUrl?.toString() ?: ""
                )
            }

            userProfileRepository.observeUserProfile(uid)
                .catch { e ->
                    Log.w("MediFlowVM", "Error observing user profile", e)
                }
                .collect { remoteProfile ->
                    if (remoteProfile != null) {
                        _userProfile.value = remoteProfile
                    }
                }
        }
    }

    fun clearAuthenticatedUserSession() {
        profileObserverJob?.cancel()
        profileObserverJob = null
        observedUid = null
        _userProfile.value = UserProfile()
    }

    fun saveUserProfile(
        displayName: String,
        email: String,
        phone: String,
        age: String,
        bloodGroup: String,
        weightKg: String,
        emergencyContact: String
    ) {
        // Update local state immediately for snappy UX
        _userProfile.value = _userProfile.value.copy(
            displayName = displayName.trim().ifBlank { "Alex Johnson" },
            email = email.trim().ifBlank { "alex@example.com" },
            phone = phone.trim(),
            age = age.trim(),
            bloodGroup = bloodGroup.trim(),
            weightKg = weightKg.trim(),
            emergencyContact = emergencyContact.trim()
        )

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                val result = userProfileRepository.saveUserProfile(
                    displayName = displayName,
                    email = email,
                    phone = phone,
                    age = age,
                    bloodGroup = bloodGroup,
                    weightKg = weightKg,
                    emergencyContact = emergencyContact,
                    avatarUrl = _userProfile.value.avatarUrl
                )
                _profileStatusMessage.value = if (result.isSuccess) {
                    "Personal details saved to cloud"
                } else {
                    "Saved locally (${result.exceptionOrNull()?.localizedMessage ?: "sync error"})"
                }
            }
        } else {
            _profileStatusMessage.value = "Personal details updated"
        }
    }

    fun clearProfileStatusMessage() {
        _profileStatusMessage.value = null
    }

    fun setSelectedDate(epochMs: Long) {
        _selectedDateEpochMs.value = epochMs
    }

    // Actions
    fun markDoseTaken(doseId: Long) {
        viewModelScope.launch {
            repository.markDoseTaken(doseId)
        }
    }

    fun snoozeDose(doseId: Long) {
        viewModelScope.launch {
            repository.snoozeDose(doseId)
        }
    }

    fun skipDose(doseId: Long, reason: String? = null) {
        viewModelScope.launch {
            repository.skipDose(doseId, reason)
        }
    }

    fun addMedicine(medicine: MedicineEntity, schedule: MedicineScheduleEntity) {
        viewModelScope.launch {
            repository.addMedicine(medicine, schedule)
        }
    }

    fun updateMedicine(medicine: MedicineEntity, schedule: MedicineScheduleEntity) {
        viewModelScope.launch {
            repository.updateMedicine(medicine, schedule)
        }
    }

    fun deleteMedicine(medicineId: Long) {
        viewModelScope.launch {
            repository.deleteMedicine(medicineId)
        }
    }

    fun toggleMedicineActive(medicineId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleMedicineActive(medicineId, isActive)
        }
    }

    fun refillMedicine(medicineId: Long, additionalQuantity: Int) {
        viewModelScope.launch {
            repository.refillMedicine(medicineId, additionalQuantity)
        }
    }

    fun logWater(amountMl: Int, beverageType: BeverageType = BeverageType.WATER, notes: String? = null) {
        viewModelScope.launch {
            repository.logWaterIntake(amountMl, beverageType, notes)
        }
    }

    fun deleteWaterLog(logId: Long) {
        viewModelScope.launch {
            repository.deleteHydrationLog(logId)
        }
    }

    fun updateHydrationGoal(goalMl: Int) {
        viewModelScope.launch {
            val current = hydrationProfile.value
            repository.updateHydrationProfile(current.copy(dailyGoalMl = goalMl))
        }
    }

    fun updateHydrationTimes(wakeMin: Int, bedMin: Int, intervalMin: Int) {
        viewModelScope.launch {
            val current = hydrationProfile.value
            repository.updateHydrationProfile(
                current.copy(
                    wakeTimeMinutes = wakeMin,
                    bedTimeMinutes = bedMin,
                    reminderIntervalMinutes = intervalMin
                )
            )
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { repository.preferences.setThemeMode(mode) }
    }

    fun setUnitSystem(unit: UnitSystem) {
        viewModelScope.launch { repository.preferences.setUnitSystem(unit) }
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { repository.preferences.setSnoozeMinutes(minutes) }
    }

    fun setHideNamesOnLockScreen(hide: Boolean) {
        viewModelScope.launch { repository.preferences.setHideNamesOnLockScreen(hide) }
    }

    fun setQuietHours(enabled: Boolean, start: String, end: String) {
        viewModelScope.launch { repository.preferences.setQuietHours(enabled, start, end) }
    }

    suspend fun exportDataJson(): String {
        return repository.exportDataAsJson()
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}
