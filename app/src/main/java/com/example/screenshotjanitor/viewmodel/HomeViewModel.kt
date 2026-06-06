package com.example.screenshotjanitor.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import com.example.screenshotjanitor.data.repository.ScreenshotRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val pendingCount: Int = 0
)

class HomeViewModel(
    private val repository: ScreenshotRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.allScreenshots
        .map { list ->
            val total = list.size
            val archived = list.count { it.archived && !it.kept && !it.deleted }
            val kept = list.count { it.kept && !it.deleted }
            val deleted = list.count { it.deleted }
            val pending = list.count { !it.archived && !it.kept && !it.deleted }
            HomeUiState(
                screenshots = list,
                totalCount = total,
                archivedCount = archived,
                keptCount = kept,
                deletedCount = deleted,
                pendingCount = pending
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )

    val nextCleanupTimeMillis: StateFlow<Long?> = workManager.getWorkInfosForUniqueWorkFlow("ScreenshotCleanupWork")
        .map { workInfos ->
            val time = workInfos.firstOrNull()?.nextScheduleTimeMillis
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
}

sealed class HomeEvent {
    class RequestDeletePermission(val intentSender: android.content.IntentSender) : HomeEvent()
}

class HomeViewModelFactory(
    private val repository: ScreenshotRepository,
    private val workManager: WorkManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, workManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
