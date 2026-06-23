package com.example.screenshotjanitor.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import com.example.screenshotjanitor.data.repository.ScreenshotRepository
import com.example.screenshotjanitor.data.repository.SettingsRepository
import com.example.screenshotjanitor.worker.ScreenshotCleanupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

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

    val nextCleanupTimeMillis: StateFlow<Long?> = workManager.getWorkInfosForUniqueWorkFlow("ScreenshotCleanupWork")
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
            if (result is com.example.screenshotjanitor.data.repository.DeleteResult.RequiresPermission) {
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
                if (result is com.example.screenshotjanitor.data.repository.DeleteResult.RequiresPermission) {
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
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the chosen time is already past for today, schedule for tomorrow
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delayMillis = target.timeInMillis - now.timeInMillis

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<ScreenshotCleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ScreenshotCleanupWork",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            cleanupRequest
        )
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
