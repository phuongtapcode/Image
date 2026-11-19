package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

/**
 * Pinch-to-zoom container that passes single-finger horizontal drags directly through
 * to a parent HorizontalPager, captures two-finger pinch + pan gestures around the
 * finger midpoint, and smoothly springs back to scale 1x / origin when fingers release.
 */
@Composable
fun SpringZoomContainer(
    modifier: Modifier = Modifier,
    maxScale: Float = 5f,
    onTap: () -> Unit = {},
    onZoomActiveChanged: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val latestOnTap by rememberUpdatedState(onTap)
    val latestOnZoomActiveChanged by rememberUpdatedState(onZoomActiveChanged)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(maxScale) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = firstDown.position
                    val downUptime = firstDown.uptimeMillis
                    var movedBeyondSlop = false
                    var didZoom = false
                    var workingScale = scaleAnim.value
                    var workingOffset = offsetAnim.value

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }

                            if (pressed.size >= 2) {
                                if (!didZoom) {
                                    didZoom = true
                                    movedBeyondSlop = true
                                    latestOnZoomActiveChanged(true)
                                    workingScale = scaleAnim.value
                                    workingOffset = offsetAnim.value
                                }

                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)

                                val oldScale = workingScale
                                val newScale = (oldScale * zoomChange).coerceIn(1f, maxScale)
                                val scaleFactor = if (oldScale > 0f) newScale / oldScale else 1f

                                val center = Offset(
                                    x = containerSize.width / 2f,
                                    y = containerSize.height / 2f
                                )
                                val centroidFromCenter = if (centroid != Offset.Unspecified) {
                                    centroid - center
                                } else {
                                    Offset.Zero
                                }

                                val rawOffset =
                                    (workingOffset + panChange - centroidFromCenter) * scaleFactor +
                                        centroidFromCenter

                                val maxOffsetX = containerSize.width * (newScale - 1f) * 0.65f +
                                    containerSize.width * 0.2f
                                val maxOffsetY = containerSize.height * (newScale - 1f) * 0.65f +
                                    containerSize.height * 0.2f

                                workingScale = newScale
                                workingOffset = Offset(
                                    x = rawOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                    y = rawOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                )

                                val snapScale = workingScale
                                val snapOffset = workingOffset
                                coroutineScope.launch {
                                    scaleAnim.snapTo(snapScale)
                                    offsetAnim.snapTo(snapOffset)
                                }

                                event.changes.forEach { it.consume() }
                            } else if (pressed.size == 1) {
                                if (didZoom) {
                                    // Keep consuming the remaining finger during pinch release so
                                    // the parent HorizontalPager does not accidentally swipe.
                                    event.changes.forEach { it.consume() }
                                } else {
                                    val change = pressed[0]
                                    if (change.isConsumed ||
                                        (change.position - downPosition).getDistance() > touchSlop
                                    ) {
                                        movedBeyondSlop = true
                                    }
                                }
                            } else {
                                // All fingers lifted
                                val wasConsumedByOther = event.changes.any { it.isConsumed }
                                val upUptime = event.changes.maxOfOrNull { it.uptimeMillis } ?: downUptime
                                if (!didZoom && !movedBeyondSlop && !wasConsumedByOther &&
                                    (upUptime - downUptime) <= 450L
                                ) {
                                    latestOnTap()
                                }
                                break
                            }
                        }
                    } finally {
                        if (didZoom) {
                            latestOnZoomActiveChanged(false)
                            coroutineScope.launch {
                                launch {
                                    scaleAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = 0.78f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                                launch {
                                    offsetAnim.animateTo(
                                        targetValue = Offset.Zero,
                                        animationSpec = spring(
                                            dampingRatio = 0.78f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    translationX = offsetAnim.value.x
                    translationY = offsetAnim.value.y
                },
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
