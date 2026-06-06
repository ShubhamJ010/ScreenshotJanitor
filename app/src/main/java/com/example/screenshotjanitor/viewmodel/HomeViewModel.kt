package com.example.screenshotjanitor.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import com.example.screenshotjanitor.data.repository.ScreenshotRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val screenshots: List<ScreenshotEntity> = emptyList(),
    val totalCount: Int = 0,
    val archivedCount: Int = 0,
    val deletedCount: Int = 0,
    val pendingCount: Int = 0
)

class HomeViewModel(private val repository: ScreenshotRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.allScreenshots
        .map { list ->
            val total = list.size
            val archived = list.count { it.archived }
            val deleted = list.count { it.deleted }
            val pending = list.count { !it.archived && !it.deleted }
            HomeUiState(
                screenshots = list,
                totalCount = total,
                archivedCount = archived,
                deletedCount = deleted,
                pendingCount = pending
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
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

    fun deleteScreenshot(context: Context, uri: String) {
        viewModelScope.launch {
            repository.deleteScreenshot(context, uri)
        }
    }
}

class HomeViewModelFactory(private val repository: ScreenshotRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
