package dev.sj010.ssjanitor.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.data.db.entity.ScreenshotEntity
import dev.sj010.ssjanitor.data.repository.ScreenshotRepository
import dev.sj010.ssjanitor.data.repository.SettingsRepository
import dev.sj010.ssjanitor.worker.CleanupScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val screenshots: List<ScreenshotEntity> = emptyList(),
    val totalCount: Int = 0,
    val archivedCount: Int = 0,
    val keptCount: Int = 0,
    val deletedCount: Int = 0,
    val deletedBytes: Long = 0L,
    val pendingCount: Int = 0,
    val isAutoArchiveEnabled: Boolean = false
)

class HomeViewModel(
    private val repository: ScreenshotRepository,
    private val settingsRepository: SettingsRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _isAutoArchiveEnabled = MutableStateFlow(settingsRepository.isAutoArchiveEnabled())
    val isAutoArchiveEnabled = _isAutoArchiveEnabled.asStateFlow()

    private val _isJanitorEnabled = MutableStateFlow(settingsRepository.isJanitorEnabled())
    val isJanitorEnabled = _isJanitorEnabled.asStateFlow()

    init {
        // Run reconciliation when ViewModel is created (app start)
        // We use a separate context/scope if needed, but viewModelScope is fine.
        // But we need a Context for reconciliation. We can't easily get it here unless we pass it.
        // Actually, ScreenshotRepository might need Context for reconcileDatabase.
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.allScreenshots,
        _isAutoArchiveEnabled
    ) { screenshots, isAutoEnabled ->
        var archived = 0
        var kept = 0
        var deleted = 0
        var deletedBytes = 0L
        var pending = 0
        screenshots.forEach {
            when {
                it.deleted -> {
                    deleted++
                    deletedBytes += it.fileSize
                }
                it.kept -> kept++
                it.archived -> archived++
                else -> pending++
            }
        }
        HomeUiState(
            screenshots = screenshots,
            totalCount = screenshots.size,
            archivedCount = archived,
            keptCount = kept,
            deletedCount = deleted,
            deletedBytes = deletedBytes,
            pendingCount = pending,
            isAutoArchiveEnabled = isAutoEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleAutoArchive() {
        val newValue = !settingsRepository.isAutoArchiveEnabled()
        settingsRepository.setAutoArchiveEnabled(newValue)
        _isAutoArchiveEnabled.value = newValue
        // We might need to refresh uiState if it doesn't observe settingsRepository
        // Actually uiState map above re-reads it, but it might not trigger on change.
        // For simplicity, we can just rely on isAutoArchiveEnabled flow for UI feedback.
    }

    fun toggleJanitor() {
        val newValue = !settingsRepository.isJanitorEnabled()
        settingsRepository.setJanitorEnabled(newValue)
        _isJanitorEnabled.value = newValue
    }

    val nextCleanupTimeMillis: StateFlow<Long?> = workManager.getWorkInfosForUniqueWorkFlow(AppConstants.WORK_CLEANUP_NAME)
        .map { workInfos ->
            val info = workInfos.firstOrNull()
            Log.d("HomeViewModel", "WorkInfo updated: state=${info?.state}, nextScheduleTimeMillis=${info?.nextScheduleTimeMillis}, runAttemptCount=${info?.runAttemptCount}")
            val time = info?.nextScheduleTimeMillis
            if (time != null && time > 0) time else null
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun archiveScreenshot(uri: String) {
        viewModelScope.launch {
            repository.archiveScreenshot(uri)
        }
    }

    fun keepScreenshot(uri: String) {
        viewModelScope.launch {
            repository.keepScreenshot(uri)
        }
    }

    fun reconcileDatabase(context: Context) {
        viewModelScope.launch {
            repository.reconcileDatabase(context)
        }
    }

    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<HomeEvent>()
    val events: kotlinx.coroutines.flow.SharedFlow<HomeEvent> = _events.asSharedFlow()

    private var pendingUrisToDelete: List<String> = emptyList()

    fun deleteScreenshot(context: Context, uri: String) {
        viewModelScope.launch {
            pendingUrisToDelete = listOf(uri)
            val result = repository.deleteScreenshots(context, pendingUrisToDelete)
                if (result is dev.sj010.ssjanitor.data.repository.DeleteResult.RequiresPermission) {
                _events.emit(HomeEvent.RequestDeletePermission(result.intentSender))
            }
        }
    }

    fun runCleanupNow(context: Context) {
        viewModelScope.launch {
            // Cleanup targets only archived screenshots (not kept, not already deleted)
            val archivedScreenshots = repository.getArchivedForCleanup()
            if (archivedScreenshots.isNotEmpty()) {
                pendingUrisToDelete = archivedScreenshots.map { it.uri }
                val result = repository.deleteScreenshots(context, pendingUrisToDelete)
            if (result is dev.sj010.ssjanitor.data.repository.DeleteResult.RequiresPermission) {
                    _events.emit(HomeEvent.RequestDeletePermission(result.intentSender))
                }
            }
        }
    }

    fun onDeletePermissionGranted() {
        viewModelScope.launch {
            repository.markAsDeleted(pendingUrisToDelete)
            pendingUrisToDelete = emptyList()
        }
    }

    fun onDeletePermissionDenied() {
        pendingUrisToDelete = emptyList()
    }

    fun rescheduleCleanup(hour: Int, minute: Int, context: Context) {
        val delayMillis = CleanupScheduler.computeDelayMillis(hour, minute)

        // Override any existing schedule (user explicitly chose a new time)
        CleanupScheduler.scheduleCleanup(
            context,
            delayMillis,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
        )
        CleanupScheduler.scheduleReminder(
            context,
            delayMillis,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
        )
        settingsRepository.setCleanupTime(hour, minute)
    }
}

sealed class HomeEvent {
    class RequestDeletePermission(val intentSender: android.content.IntentSender) : HomeEvent()
}

class HomeViewModelFactory(
    private val repository: ScreenshotRepository,
    private val settingsRepository: SettingsRepository,
    private val workManager: WorkManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, settingsRepository, workManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
