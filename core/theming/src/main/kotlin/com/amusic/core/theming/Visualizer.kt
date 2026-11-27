package com.amusic.core.theming

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.amusic.core.common.AudioFeed
import com.amusic.core.common.AudioFrame

/** Silent unless the player screen supplies the real feed. */
val LocalAudioFeed = staticCompositionLocalOf<AudioFeed> {
    object : AudioFeed {
        override fun watch() = AutoCloseable {}
        override fun frame() = AudioFrame.SILENCE
    }
}

/** The chosen visualizer, moving with the music and standing still without it. */
@Composable
fun VisualizerStage(ui: PlayerUi, actions: PlayerActions, modifier: Modifier = Modifier) {
    val settings by rememberUpdatedState(ui.visualizer)
    val feed = LocalAudioFeed.current
    val motion = remember { VisualizerMotion() }
    var frameTime by remember { mutableLongStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Runs only while visible, and stops once a paused song has faded out.
    LaunchedEffect(feed, ui.isPlaying, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val watch = feed.watch()
            try {
                var last = 0L
                while (ui.isPlaying || !motion.settled) {
                    withInfiniteAnimationFrameMillis { now ->
                        val seconds = if (last == 0L) 0.016f else ((now - last) / 1_000f).coerceIn(0f, 0.05f)
                        last = now
                        motion.advance(feed.frame(), settings, seconds)
                        frameTime = now
                    }
                }
            } finally {
                watch.close()
            }
        }
    }
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier.testTag("visualizer")) {
        Canvas(Modifier.matchParentSize().clipToBounds()) {
            frameTime // reading it redraws the canvas on every frame
            drawVisualizer(motion, ui.visualizer, accent)
        }
        IconButton(actions::openVisualizerOptions, Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Filled.Settings, stringResource(R.string.visualizer_options))
        }
    }
}
