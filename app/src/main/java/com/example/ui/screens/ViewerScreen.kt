package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.R
import com.example.data.MediaSlot
import com.example.data.MediaType
import com.example.media.MediaImageLoader
import com.example.ui.components.LoopingVideoPlayer
import com.example.ui.components.SpringZoomContainer
import com.example.ui.formatSlotNumber
import com.example.ui.theme.BadgeGifTint
import com.example.ui.theme.BadgeImageTint
import com.example.ui.theme.BadgeVideoTint
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DarkroomBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassOverlay
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OpticalAmber
import com.example.ui.theme.OpticalAmberBorder
import com.example.ui.theme.OpticalAmberContainer
import com.example.ui.theme.WarmWhite

@Composable
fun ViewerScreen(
    slots: List<MediaSlot>,
    isLoading: Boolean,
    isVideoMuted: Boolean,
    initialSlotIndex: Int,
    navigationToken: Int,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onHideControls: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleMute: () -> Unit,
    onReplaceMissingSlot: (MediaSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        when {
            isLoading -> {
                // Keep pure OLED black during initial local Room read for zero visual flash.
            }

            slots.isEmpty() -> {
                ViewerEmptyState(
                    onConfigureFirstSlot = onOpenSettings
                )
            }

            else -> {
                MediaLoopPager(
                    slots = slots,
                    isVideoMuted = isVideoMuted,
                    initialSlotIndex = initialSlotIndex,
                    navigationToken = navigationToken,
                    controlsVisible = controlsVisible,
                    onToggleControls = onToggleControls,
                    onHideControls = onHideControls,
                    onOpenSettings = onOpenSettings,
                    onToggleMute = onToggleMute,
                    onReplaceMissingSlot = onReplaceMissingSlot
                )
            }
        }
    }
}

@Composable
private fun MediaLoopPager(
    slots: List<MediaSlot>,
    isVideoMuted: Boolean,
    initialSlotIndex: Int,
    navigationToken: Int,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onHideControls: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleMute: () -> Unit,
    onReplaceMissingSlot: (MediaSlot) -> Unit
) {
    val slotCount = slots.size
    val isCircular = slotCount > 1

    // Re-key cleanly when slot count changes or user explicitly jumped from Settings.
    key(slotCount, navigationToken) {
        val startPage = remember(slotCount, initialSlotIndex) {
            computeLoopStartPage(slotCount, initialSlotIndex)
        }
        val pagerState = rememberPagerState(
            initialPage = startPage,
            pageCount = { if (isCircular) Int.MAX_VALUE else slotCount }
        )
        var isZoomActive by remember { mutableStateOf(false) }

        val currentRealIndex = ((pagerState.currentPage % slotCount) + slotCount) % slotCount
        val currentSlot = slots[currentRealIndex]

        // Auto-hide controls while zooming so pinch gestures stay completely immersive.
        LaunchedEffect(isZoomActive) {
            if (isZoomActive && controlsVisible) {
                onHideControls()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isZoomActive && isCircular,
                beyondViewportPageCount = 1,
                key = { virtualPage ->
                    val realIdx = ((virtualPage % slotCount) + slotCount) % slotCount
                    "${virtualPage}_${slots[realIdx].id}_${slots[realIdx].uriString}"
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("viewer_media_pager")
            ) { virtualPage ->
                val realIdx = ((virtualPage % slotCount) + slotCount) % slotCount
                val slot = slots[realIdx]
                val isSettledActive = pagerState.currentPage == virtualPage

                MediaSlidePage(
                    slot = slot,
                    isActivePage = isSettledActive,
                    isVideoMuted = isVideoMuted,
                    onTap = onToggleControls,
                    onZoomActiveChanged = { active -> isZoomActive = active },
                    onReplaceMissingSlot = { onReplaceMissingSlot(slot) }
                )
            }

            // Subtle floating mute button ONLY for video slides when top bar is hidden
            AnimatedVisibility(
                visible = currentSlot.type == MediaType.VIDEO && !controlsVisible && !isZoomActive,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = GlassOverlay,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleMute)
                        .testTag("viewer_floating_mute_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isVideoMuted) {
                                Icons.AutoMirrored.Filled.VolumeOff
                            } else {
                                Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = stringResource(R.string.cd_toggle_mute),
                            tint = if (isVideoMuted) WarmWhite else OpticalAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Tap-to-toggle minimal top overlay bar
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                    slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { -it / 2 }
                    ),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                    slideOutVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        targetOffsetY = { -it / 2 }
                    ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                ViewerTopOverlayBar(
                    currentSlotIndex = currentRealIndex,
                    totalSlots = slotCount,
                    currentMediaType = currentSlot.type,
                    isVideoMuted = isVideoMuted,
                    onToggleMute = onToggleMute,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun MediaSlidePage(
    slot: MediaSlot,
    isActivePage: Boolean,
    isVideoMuted: Boolean,
    onTap: () -> Unit,
    onZoomActiveChanged: (Boolean) -> Unit,
    onReplaceMissingSlot: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { MediaImageLoader.get(context) }
    var videoError by remember(slot.id, slot.uriString) { mutableStateOf(false) }

    SpringZoomContainer(
        onTap = onTap,
        onZoomActiveChanged = onZoomActiveChanged,
        modifier = Modifier.fillMaxSize()
    ) {
        when (slot.type) {
            MediaType.VIDEO -> {
                if (videoError) {
                    MissingMediaCard(
                        title = stringResource(R.string.media_video_error),
                        subtitle = stringResource(R.string.media_missing_subtitle),
                        onReplace = onReplaceMissingSlot
                    )
                } else {
                    LoopingVideoPlayer(
                        uriString = slot.uriString,
                        isActivePage = isActivePage,
                        isMuted = isVideoMuted,
                        onError = { videoError = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            MediaType.IMAGE, MediaType.GIF -> {
                val request = remember(slot.uriString, slot.type) {
                    ImageRequest.Builder(context)
                        .data(Uri.parse(slot.uriString))
                        // Decode original quality resolution without lossy downsampling
                        .size(Size.ORIGINAL)
                        .allowHardware(slot.type != MediaType.GIF)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = request,
                    imageLoader = imageLoader,
                    contentDescription = slot.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    error = {
                        MissingMediaCard(
                            title = stringResource(R.string.media_missing_title),
                            subtitle = stringResource(R.string.media_missing_subtitle),
                            onReplace = onReplaceMissingSlot
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ViewerTopOverlayBar(
    currentSlotIndex: Int,
    totalSlots: Int,
    currentMediaType: MediaType,
    isVideoMuted: Boolean,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xB8000000),
                        Color(0x4D000000),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 680.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Minimal slot index counter + media type pill
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = GlassOverlay,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${formatSlotNumber(currentSlotIndex + 1)} / ${formatSlotNumber(totalSlots)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmWhite,
                        modifier = Modifier.testTag("viewer_index_badge")
                    )

                    val (typeLabel, typeColor) = when (currentMediaType) {
                        MediaType.IMAGE -> stringResource(R.string.slot_type_image) to BadgeImageTint
                        MediaType.GIF -> stringResource(R.string.slot_type_gif) to BadgeGifTint
                        MediaType.VIDEO -> stringResource(R.string.slot_type_video) to BadgeVideoTint
                    }

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(typeColor)
                    )
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor
                    )
                }
            }

            // Right: Video mute pill (if Video) + Settings button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentMediaType == MediaType.VIDEO) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = GlassOverlay,
                        border = BorderStroke(
                            1.dp,
                            if (isVideoMuted) GlassBorder else OpticalAmberBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onToggleMute)
                            .testTag("viewer_mute_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isVideoMuted) {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = stringResource(R.string.cd_toggle_mute),
                                tint = if (isVideoMuted) WarmWhite else OpticalAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isVideoMuted) R.string.viewer_muted_label else R.string.viewer_unmuted_label
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isVideoMuted) WarmWhite else OpticalAmber
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = GlassOverlay,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("viewer_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings),
                            tint = WarmWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerEmptyState(
    onConfigureFirstSlot: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A160D),
                        DarkroomBackground,
                        ObsidianBlack
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(OpticalAmberContainer)
                    .border(1.dp, OpticalAmberBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Collections,
                    contentDescription = null,
                    tint = OpticalAmber,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = WarmWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.empty_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedSlate,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onConfigureFirstSlot,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OpticalAmber,
                    contentColor = ObsidianBlack
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_configure_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.empty_cta_configure),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun MissingMediaCard(
    title: String,
    subtitle: String,
    onReplace: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 360.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = OpticalAmber,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = WarmWhite,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onReplace,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OpticalAmber,
                    contentColor = ObsidianBlack
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.testTag("missing_media_replace_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.media_missing_action),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun computeLoopStartPage(slotCount: Int, desiredSlotIndex: Int): Int {
    if (slotCount <= 1) return 0
    val safeIndex = desiredSlotIndex.coerceIn(0, slotCount - 1)
    val midpoint = Int.MAX_VALUE / 2
    val alignedBase = midpoint - (midpoint % slotCount)
    return alignedBase + safeIndex
}
