package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.MediaSlot
import com.example.data.MediaType
import com.example.media.MediaImageLoader
import com.example.ui.formatSlotNumber
import com.example.ui.theme.BadgeGifTint
import com.example.ui.theme.BadgeImageTint
import com.example.ui.theme.BadgeVideoTint
import com.example.ui.theme.CardElevated
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkroomBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OpticalAmber
import com.example.ui.theme.OpticalAmberBorder
import com.example.ui.theme.OpticalAmberContainer
import com.example.ui.theme.SubtleSlate
import com.example.ui.theme.WarmWhite

@Composable
fun SettingsScreen(
    slots: List<MediaSlot>,
    snackbarMessage: String?,
    onClearSnackbar: () -> Unit,
    onBackToViewer: (slotIndex: Int) -> Unit,
    onPickNextSlot: () -> Unit,
    onPickMultipleSlots: () -> Unit,
    onReplaceSlot: (MediaSlot, Int) -> Unit,
    onMoveSlotUp: (Long) -> Unit,
    onMoveSlotDown: (Long) -> Unit,
    onDeleteSlot: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToViewer(0)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onClearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkroomBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CardElevated,
                    contentColor = WarmWhite,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        topBar = {
            SettingsHeader(
                slotCount = slots.size,
                onBack = { onBackToViewer(0) },
                onPickMultiple = onPickMultipleSlots
            )
        },
        bottomBar = {
            if (slots.isNotEmpty()) {
                SettingsBottomBar(
                    onViewNow = { onBackToViewer(0) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .testTag("settings_slots_list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = slots,
                    key = { _, slot -> slot.id }
                ) { index, slot ->
                    ConfiguredSlotCard(
                        slot = slot,
                        index = index,
                        totalCount = slots.size,
                        onOpenAtSlot = { onBackToViewer(index) },
                        onMoveUp = { onMoveSlotUp(slot.id) },
                        onMoveDown = { onMoveSlotDown(slot.id) },
                        onReplace = { onReplaceSlot(slot, index) },
                        onDelete = { onDeleteSlot(slot.id) },
                        modifier = Modifier.animateItem()
                    )
                }

                // Always show exactly one unlocked "Next Slot" directly beneath the last slot
                item(key = "next_unlocked_slot_${slots.size + 1}") {
                    NextUnlockedSlotCard(
                        nextSlotNumber = slots.size + 1,
                        isFirstSlot = slots.isEmpty(),
                        onClick = onPickNextSlot,
                        modifier = Modifier.animateItem()
                    )
                }

                item(key = "storage_footer_note") {
                    Text(
                        text = stringResource(R.string.settings_footer_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SubtleSlate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    slotCount: Int,
    onBack: () -> Unit,
    onPickMultiple: () -> Unit
) {
    Surface(
        color = DarkroomBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back_to_viewer),
                                tint = WarmWhite
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.settings_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = WarmWhite
                            )
                            Text(
                                text = if (slotCount > 0) {
                                    stringResource(R.string.settings_subtitle_count, slotCount)
                                } else {
                                    stringResource(R.string.settings_subtitle_empty)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onPickMultiple,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("settings_add_multiple_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryAdd,
                            contentDescription = null,
                            tint = OpticalAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_add_multiple),
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmWhite
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HairlineBorder)
            )
        }
    }
}

@Composable
private fun ConfiguredSlotCard(
    slot: MediaSlot,
    index: Int,
    totalCount: Int,
    onOpenAtSlot: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { MediaImageLoader.get(context) }

    val (typeLabel, typeColor) = when (slot.type) {
        MediaType.IMAGE -> stringResource(R.string.slot_type_image) to BadgeImageTint
        MediaType.GIF -> stringResource(R.string.slot_type_gif) to BadgeGifTint
        MediaType.VIDEO -> stringResource(R.string.slot_type_video) to BadgeVideoTint
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("configured_slot_card_${index + 1}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slot order pill (01, 02, ...)
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (index == 0) OpticalAmberContainer else CardElevated)
                        .border(
                            width = 1.dp,
                            color = if (index == 0) OpticalAmberBorder else HairlineBorder,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatSlotNumber(index + 1),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (index == 0) OpticalAmber else WarmWhite
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Crisp local thumbnail (tap to jump straight to this slide in Viewer)
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianBlack)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenAtSlot)
                        .testTag("slot_thumbnail_${index + 1}"),
                    contentAlignment = Alignment.Center
                ) {
                    val thumbRequest = remember(slot.uriString) {
                        ImageRequest.Builder(context)
                            .data(Uri.parse(slot.uriString))
                            .size(240)
                            .build()
                    }
                    SubcomposeAsyncImage(
                        model = thumbRequest,
                        imageLoader = imageLoader,
                        contentDescription = stringResource(R.string.cd_slot_thumbnail),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BrokenImage,
                                    contentDescription = null,
                                    tint = SubtleSlate,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    )

                    if (slot.type == MediaType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xB3000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = WarmWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Filename & format badge
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenAtSlot)
                ) {
                    Text(
                        text = slot.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = typeColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, typeColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = typeColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "#${formatSlotNumber(index + 1)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SubtleSlate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact action strip with 48dp accessible touch targets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardElevated)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("slot_move_up_${index + 1}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.cd_move_slot_up),
                            tint = if (index > 0) WarmWhite else SubtleSlate.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("slot_move_down_${index + 1}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_move_slot_down),
                            tint = if (index < totalCount - 1) WarmWhite else SubtleSlate.copy(alpha = 0.35f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onReplace,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("slot_replace_${index + 1}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = stringResource(R.string.cd_replace_slot),
                            tint = OpticalAmber
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("slot_delete_${index + 1}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_slot),
                            tint = DangerRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NextUnlockedSlotCard(
    nextSlotNumber: Int,
    isFirstSlot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedNumber = formatSlotNumber(nextSlotNumber)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = OpticalAmberContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.5.dp, OpticalAmberBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("next_unlocked_slot_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(OpticalAmber),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = ObsidianBlack,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.next_slot_badge, formattedNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = OpticalAmber
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(
                        if (isFirstSlot) R.string.next_slot_first_title else R.string.next_slot_title
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        if (isFirstSlot) R.string.next_slot_first_subtitle else R.string.next_slot_subtitle
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )
            }
        }
    }
}

@Composable
private fun SettingsBottomBar(
    onViewNow: () -> Unit
) {
    Surface(
        color = DarkroomBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HairlineBorder)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onViewNow,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OpticalAmber,
                        contentColor = ObsidianBlack
                    ),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .testTag("settings_view_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_done_view),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
