package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.FrameLoopDatabase
import com.example.data.MediaSlot
import com.example.data.MediaSlotRepository
import com.example.data.MediaUriHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data object Viewer : AppScreen
    data object Settings : AppScreen
}

data class MainUiState(
    val slots: List<MediaSlot> = emptyList(),
    val isLoading: Boolean = true,
    val isVideoMuted: Boolean = true,
    val activeScreen: AppScreen = AppScreen.Viewer,
    val viewerStartSlotIndex: Int = 0,
    val viewerNavigationToken: Int = 0,
    val snackbarMessage: String? = null
)

private data class LocalUiControlState(
    val isVideoMuted: Boolean = true,
    val activeScreen: AppScreen = AppScreen.Viewer,
    val viewerStartSlotIndex: Int = 0,
    val viewerNavigationToken: Int = 0,
    val snackbarMessage: String? = null
)

class MediaSlotsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MediaSlotRepository = MediaSlotRepository(
        dao = FrameLoopDatabase.getInstance(application).mediaSlotDao(),
        uriHelper = MediaUriHelper(application)
    )

    private val controlState = MutableStateFlow(LocalUiControlState())

    val uiState: StateFlow<MainUiState> = combine(
        repository.slots,
        controlState
    ) { slots, controls ->
        MainUiState(
            slots = slots,
            isLoading = false,
            isVideoMuted = controls.isVideoMuted,
            activeScreen = controls.activeScreen,
            viewerStartSlotIndex = controls.viewerStartSlotIndex.coerceIn(
                0,
                (slots.size - 1).coerceAtLeast(0)
            ),
            viewerNavigationToken = controls.viewerNavigationToken,
            snackbarMessage = controls.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MainUiState(isLoading = true)
    )

    fun openSettings() {
        controlState.update { it.copy(activeScreen = AppScreen.Settings) }
    }

    fun openViewer(slotIndex: Int = 0) {
        controlState.update {
            it.copy(
                activeScreen = AppScreen.Viewer,
                viewerStartSlotIndex = slotIndex.coerceAtLeast(0),
                viewerNavigationToken = it.viewerNavigationToken + 1
            )
        }
    }

    fun toggleVideoMute() {
        controlState.update { it.copy(isVideoMuted = !it.isVideoMuted) }
    }

    fun clearSnackbar() {
        controlState.update { it.copy(snackbarMessage = null) }
    }

    fun addSingleUri(uri: Uri) {
        viewModelScope.launch {
            val result = repository.addMediaUris(listOf(uri))
            if (result.addedCount > 0) {
                val app = getApplication<Application>()
                val message = if (result.allPersisted) {
                    app.getString(
                        R.string.snackbar_slot_added,
                        formatSlotNumber(result.newTotalCount),
                        formatSlotNumber(result.newTotalCount + 1)
                    )
                } else {
                    app.getString(R.string.snackbar_uri_permission_warning)
                }
                showSnackbar(message)
            }
        }
    }

    fun addMultipleUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val result = repository.addMediaUris(uris)
            if (result.addedCount > 0) {
                val app = getApplication<Application>()
                val message = if (result.addedCount == 1) {
                    app.getString(
                        R.string.snackbar_slot_added,
                        formatSlotNumber(result.newTotalCount),
                        formatSlotNumber(result.newTotalCount + 1)
                    )
                } else {
                    app.getString(R.string.snackbar_slots_added_multi, result.addedCount)
                }
                showSnackbar(message)
            }
        }
    }

    fun replaceSlotUri(slotId: Long, displayOrderIndex: Int, uri: Uri) {
        viewModelScope.launch {
            val persisted = repository.replaceSlot(slotId, uri)
            val app = getApplication<Application>()
            val message = if (persisted) {
                app.getString(R.string.snackbar_slot_replaced, formatSlotNumber(displayOrderIndex + 1))
            } else {
                app.getString(R.string.snackbar_uri_permission_warning)
            }
            showSnackbar(message)
        }
    }

    fun moveSlotUp(slotId: Long) {
        viewModelScope.launch {
            repository.moveSlot(slotId, delta = -1)
        }
    }

    fun moveSlotDown(slotId: Long) {
        viewModelScope.launch {
            repository.moveSlot(slotId, delta = 1)
        }
    }

    fun deleteSlot(slotId: Long) {
        viewModelScope.launch {
            repository.deleteSlot(slotId)
            showSnackbar(getApplication<Application>().getString(R.string.snackbar_slot_deleted))
        }
    }

    private fun showSnackbar(message: String) {
        controlState.update { it.copy(snackbarMessage = message) }
    }
}

fun formatSlotNumber(oneBasedIndex: Int): String =
    oneBasedIndex.toString().padStart(2, '0')
