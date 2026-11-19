package com.example

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MediaSlot
import com.example.ui.AppScreen
import com.example.ui.MediaSlotsViewModel
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ViewerScreen
import com.example.ui.theme.FrameLoopTheme

private val SUPPORTED_MEDIA_MIME_TYPES = arrayOf("image/*", "video/*")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContent {
            FrameLoopTheme {
                FrameLoopApp()
            }
        }
    }
}

@Composable
private fun FrameLoopApp(
    viewModel: MediaSlotsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Viewer overlay starts hidden on app launch for instant distraction-free viewing
    var viewerControlsVisible by rememberSaveable { mutableStateOf(false) }

    // Track which slot is being replaced (null means appending to the next unlocked slot)
    var pendingReplaceTarget by remember { mutableStateOf<Pair<MediaSlot, Int>?>(null) }

    val singleDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val replaceTarget = pendingReplaceTarget
        pendingReplaceTarget = null
        if (uri != null) {
            if (replaceTarget != null) {
                viewModel.replaceSlotUri(
                    slotId = replaceTarget.first.id,
                    displayOrderIndex = replaceTarget.second,
                    uri = uri
                )
            } else {
                viewModel.addSingleUri(uri)
            }
        }
    }

    val multiDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addMultipleUris(uris)
        }
    }

    // Manage immersive system bars: hide them cleanly when viewing media with overlay hidden
    val isImmersiveViewer = uiState.activeScreen == AppScreen.Viewer &&
        uiState.slots.isNotEmpty() &&
        !viewerControlsVisible

    SystemBarsVisibilityEffect(hideSystemBars = isImmersiveViewer)

    Crossfade(
        targetState = uiState.activeScreen,
        animationSpec = tween(durationMillis = 220),
        label = "screen_crossfade",
        modifier = Modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AppScreen.Viewer -> {
                ViewerScreen(
                    slots = uiState.slots,
                    isLoading = uiState.isLoading,
                    isVideoMuted = uiState.isVideoMuted,
                    initialSlotIndex = uiState.viewerStartSlotIndex,
                    navigationToken = uiState.viewerNavigationToken,
                    controlsVisible = viewerControlsVisible,
                    onToggleControls = { viewerControlsVisible = !viewerControlsVisible },
                    onHideControls = { viewerControlsVisible = false },
                    onOpenSettings = {
                        viewerControlsVisible = false
                        viewModel.openSettings()
                    },
                    onToggleMute = viewModel::toggleVideoMute,
                    onReplaceMissingSlot = { missingSlot ->
                        val index = uiState.slots.indexOfFirst { it.id == missingSlot.id }
                            .coerceAtLeast(0)
                        pendingReplaceTarget = missingSlot to index
                        singleDocumentLauncher.launch(SUPPORTED_MEDIA_MIME_TYPES)
                    }
                )
            }

            AppScreen.Settings -> {
                SettingsScreen(
                    slots = uiState.slots,
                    snackbarMessage = uiState.snackbarMessage,
                    onClearSnackbar = viewModel::clearSnackbar,
                    onBackToViewer = { slotIndex ->
                        viewerControlsVisible = false
                        viewModel.openViewer(slotIndex)
                    },
                    onPickNextSlot = {
                        pendingReplaceTarget = null
                        singleDocumentLauncher.launch(SUPPORTED_MEDIA_MIME_TYPES)
                    },
                    onPickMultipleSlots = {
                        multiDocumentLauncher.launch(SUPPORTED_MEDIA_MIME_TYPES)
                    },
                    onReplaceSlot = { slot, index ->
                        pendingReplaceTarget = slot to index
                        singleDocumentLauncher.launch(SUPPORTED_MEDIA_MIME_TYPES)
                    },
                    onMoveSlotUp = viewModel::moveSlotUp,
                    onMoveSlotDown = viewModel::moveSlotDown,
                    onDeleteSlot = viewModel::deleteSlot
                )
            }
        }
    }
}

@Composable
private fun SystemBarsVisibilityEffect(hideSystemBars: Boolean) {
    val view = LocalView.current
    LaunchedEffect(view, hideSystemBars) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hideSystemBars) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
