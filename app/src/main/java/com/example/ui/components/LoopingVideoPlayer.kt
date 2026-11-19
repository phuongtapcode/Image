package com.example.ui.components

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.media.MediaImageLoader

/**
 * Ultra-lightweight local video player built on Android's native MediaPlayer + TextureView
 * (zero external media library APK overhead). Supports original aspect ratio fit, looping,
 * muted-by-default playback, and Compose graphicsLayer pinch-to-zoom transforms.
 */
@Composable
fun LoopingVideoPlayer(
    uriString: String,
    isActivePage: Boolean,
    isMuted: Boolean,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageLoader = remember(context) { MediaImageLoader.get(context) }

    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var videoAspectRatio by remember(uriString) { mutableStateOf<Float?>(null) }
    var isFirstFrameReady by remember(uriString) { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldPlay = isActivePage && isResumed

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Show native first video frame immediately underneath while MediaPlayer prepares
        // or when the slide is off-screen so there is zero black flicker during swipes.
        if (!isFirstFrameReady || !isActivePage) {
            AsyncImage(
                model = remember(uriString) {
                    ImageRequest.Builder(context)
                        .data(Uri.parse(uriString))
                        .build()
                },
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isActivePage) {
            val controller = remember(uriString) {
                LocalVideoController(
                    context = context.applicationContext,
                    uri = Uri.parse(uriString),
                    onAspectRatioChanged = { ratio -> videoAspectRatio = ratio },
                    onReadyToRender = { isFirstFrameReady = true },
                    onError = onError
                )
            }

            LaunchedEffect(controller, shouldPlay) {
                controller.setShouldPlay(shouldPlay)
            }

            LaunchedEffect(controller, isMuted) {
                controller.setMuted(isMuted)
            }

            DisposableEffect(controller) {
                onDispose {
                    controller.release()
                }
            }

            val containerWidth = maxWidth
            val containerHeight = maxHeight
            val containerRatio = if (containerHeight.value > 0f) {
                containerWidth.value / containerHeight.value
            } else {
                1f
            }
            val targetRatio = videoAspectRatio ?: containerRatio
            val (fitWidth, fitHeight) = if (targetRatio >= containerRatio) {
                containerWidth to (containerWidth / targetRatio)
            } else {
                (containerHeight * targetRatio) to containerHeight
            }

            Box(
                modifier = Modifier.size(width = fitWidth, height = fitHeight),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            isOpaque = false
                            surfaceTextureListener = controller.surfaceListener
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private class LocalVideoController(
    private val context: Context,
    private val uri: Uri,
    private val onAspectRatioChanged: (Float) -> Unit,
    private val onReadyToRender: () -> Unit,
    private val onError: () -> Unit
) {
    private var mediaPlayer: MediaPlayer? = null
    private var activeSurface: Surface? = null
    private var isPrepared: Boolean = false
    private var isReleased: Boolean = false
    private var shouldPlay: Boolean = false
    private var isMuted: Boolean = true

    val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            if (isReleased) return
            preparePlayer(surfaceTexture)
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) = Unit

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            release()
            return true
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
    }

    fun setShouldPlay(play: Boolean) {
        shouldPlay = play
        val mp = mediaPlayer ?: return
        if (!isPrepared || isReleased) return
        try {
            if (play && !mp.isPlaying) {
                mp.start()
            } else if (!play && mp.isPlaying) {
                mp.pause()
            }
        } catch (_: IllegalStateException) {
            // Player already released or transitioning
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        applyVolume()
    }

    private fun applyVolume() {
        val mp = mediaPlayer ?: return
        if (isReleased) return
        val volume = if (isMuted) 0f else 1f
        try {
            mp.setVolume(volume, volume)
        } catch (_: IllegalStateException) {
            // Ignore when player is not yet initialized
        }
    }

    private fun preparePlayer(surfaceTexture: SurfaceTexture) {
        releasePlayerOnly()
        val surface = Surface(surfaceTexture)
        activeSurface = surface

        val player = MediaPlayer()
        mediaPlayer = player
        isPrepared = false

        try {
            player.setDataSource(context, uri)
            player.setSurface(surface)
            player.isLooping = true
            player.setOnVideoSizeChangedListener { _, width, height ->
                if (width > 0 && height > 0) {
                    onAspectRatioChanged(width.toFloat() / height.toFloat())
                }
            }
            player.setOnPreparedListener { mp ->
                if (isReleased) {
                    mp.release()
                    return@setOnPreparedListener
                }
                isPrepared = true
                if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                    onAspectRatioChanged(mp.videoWidth.toFloat() / mp.videoHeight.toFloat())
                }
                applyVolume()
                onReadyToRender()
                if (shouldPlay) {
                    mp.start()
                }
            }
            player.setOnErrorListener { _, _, _ ->
                onError()
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            releasePlayerOnly()
            onError()
        }
    }

    private fun releasePlayerOnly() {
        isPrepared = false
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
        mediaPlayer = null
        activeSurface?.release()
        activeSurface = null
    }

    fun release() {
        isReleased = true
        releasePlayerOnly()
    }
}
