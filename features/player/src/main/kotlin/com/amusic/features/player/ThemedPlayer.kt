package com.amusic.features.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import com.amusic.core.theming.LocalDarkTheme
import com.amusic.core.theming.ApplyTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.VisualizerSettings
import com.amusic.core.theming.LocalAudioFeed
import com.amusic.core.theming.LocalPlayerTheme
import com.amusic.core.theming.LyricsEditorDialog
import com.amusic.core.theming.PlayerActions
import com.amusic.core.theming.PlayerPanel
import com.amusic.core.theming.PlayerUi
import com.amusic.core.theming.Position
import com.amusic.core.theming.RemoveLyricsDialog
import com.amusic.core.theming.VisualizerDialog
import com.amusic.core.theming.R as ThemingR
import com.amusic.data.ImportResult
import kotlinx.coroutines.delay

private class Actions(
    private val vm: PlayerViewModel,
    private val onBack: () -> Unit,
    private val position: androidx.compose.runtime.MutableLongState,
    private val onImport: () -> Unit,
    private val onStudio: () -> Unit,
) : PlayerActions {
    override fun back() = onBack()
    override fun togglePlay() = vm.togglePlay()
    override fun next() = vm.next()
    override fun previous() = vm.previous()
    override fun seekTo(positionMs: Long) {
        position.longValue = positionMs // avoids the thumb jumping back until the next poll
        vm.seekTo(positionMs)
    }
    override fun toggleShuffle() = vm.toggleShuffle()
    override fun cycleRepeat() = vm.cycleRepeat()
    override fun toggleFavorite() = vm.toggleFavorite()
    override fun setSpeed(speed: Float) = vm.setSpeed(speed)
    override fun setSleepTimer(minutes: Int?) = vm.setSleepTimer(minutes)
    override fun jumpTo(index: Int) = vm.jumpTo(index)
    override fun removeAt(index: Int) = vm.removeAt(index)
    override fun move(from: Int, to: Int) = vm.move(from, to)
    override fun clearQueue() = vm.clearQueue()
    override fun setOrientationMode(mode: OrientationMode) = vm.setOrientationMode(mode)
    override fun showPanel(panel: PlayerPanel) = vm.showPanel(panel)
    override fun setVisualizer(settings: VisualizerSettings) = vm.setVisualizer(settings)
    override fun openVisualizerOptions() {
        vm.showVisualizerOptions = true
    }
    override fun openBackgroundStudio() = onStudio()
    override fun importLyrics() = onImport()
    override fun editLyrics() = vm.editLyrics()
    override fun removeLyrics() = vm.askToRemoveLyrics()
    override fun setLyricsOffset(offsetMs: Long) = vm.setLyricsOffset(offsetMs)
}

private class Session(val ui: PlayerUi, val position: Position, val actions: PlayerActions)

/** Connects the shared playback state to whichever theme draws it. */
@Composable
private fun rememberSession(
    vm: PlayerViewModel,
    onBack: () -> Unit,
    onImport: () -> Unit = {},
    onStudio: () -> Unit = {},
): Session? {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val position = remember { mutableLongStateOf(vm.positionMs()) }
    val latestImport by rememberUpdatedState(onImport)
    val latestStudio by rememberUpdatedState(onStudio)
    val actions = remember(vm, onBack) {
        Actions(vm, onBack, position, { latestImport() }, { latestStudio() })
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Polls only while visible.
    LaunchedEffect(ui?.queueIndex, ui?.title, ui?.isPlaying, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                position.longValue = vm.positionMs()
                delay(200) // fine enough to highlight lyric lines on time
            }
        }
    }
    return ui?.let { Session(it, position, actions) }
}

/** Shown above the navigation bar whenever something is queued. */
@Composable
fun MiniPlayer(onOpen: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val session = rememberSession(viewModel, onBack = {}) ?: return
    LocalPlayerTheme.current.MiniPlayer(session.ui, session.position, session.actions, onOpen)
}

@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    onOpenStudio: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    // Remembered across the activity being rebuilt while the file picker is open.
    var importTarget by rememberSaveable { mutableLongStateOf(-1) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { file ->
        if (file != null && importTarget >= 0) viewModel.importLyrics(importTarget, file)
    }
    val pickFile = {
        viewModel.currentSongId()?.let {
            importTarget = it
            // LRC files have no registered type, so every file has to be offered.
            picker.launch(arrayOf("*/*"))
        }
        Unit
    }
    val session = rememberSession(viewModel, onBack, pickFile, onOpenStudio)
    if (session == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text(stringResource(R.string.player_nothing))
            TextButton(onClick = onBack) { Text(stringResource(ThemingR.string.player_back)) }
        }
        return
    }
    val mode = session.ui.orientationMode
    LockOrientation(mode)
    val theme = LocalPlayerTheme.current
    val background by viewModel.background.collectAsStateWithLifecycle()
    // Studio backgrounds are dark, so the player on top of them uses the dark colors.
    val dark = background?.enabled == true || LocalDarkTheme.current
    ApplyTheme(theme, dark) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val wide = maxWidth > maxHeight
        val landscape = mode.usesLandscape(wide)
        // Large screens may ignore the lock; the chosen layout is then fitted into the window.
        val frame = when {
            landscape == wide -> Modifier.fillMaxSize()
            landscape -> Modifier.width(maxWidth).aspectRatio(16f / 9f)
            else -> Modifier.width(min(maxWidth, maxHeight * 0.6f)).fillMaxSize()
        }
        Box(frame) {
            CompositionLocalProvider(LocalAudioFeed provides viewModel.audioFeed) {
                if (landscape) theme.LandscapePlayer(session.ui, session.position, session.actions)
                else theme.PortraitPlayer(session.ui, session.position, session.actions)
            }
        }
    }
    PlayerDialogs(session.ui, viewModel)
    }
    }
}

@Composable
private fun PlayerDialogs(ui: PlayerUi, vm: PlayerViewModel) {
    vm.draft?.let { draft ->
        LyricsEditorDialog(draft.text, vm::changeDraft, vm::saveDraft, vm::discardDraft)
    }
    if (vm.removing != null) {
        RemoveLyricsDialog(onRemove = { vm.finishRemoving(true) }, onDismiss = { vm.finishRemoving(false) })
    }
    if (vm.showVisualizerOptions) {
        VisualizerDialog(ui.visualizer, vm::setVisualizer) { vm.showVisualizerOptions = false }
    }
    val context = LocalContext.current
    LaunchedEffect(vm.importResult) {
        val result = vm.importResult ?: return@LaunchedEffect
        val message = when (result) {
            ImportResult.IMPORTED -> ThemingR.string.lyrics_imported
            ImportResult.EMPTY -> ThemingR.string.lyrics_import_empty
            ImportResult.TOO_LARGE -> ThemingR.string.lyrics_import_too_large
            ImportResult.UNREADABLE -> ThemingR.string.lyrics_import_unreadable
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        vm.importResultShown()
    }
}

/** Requests the orientation only while the player is on screen. */
@Composable
private fun LockOrientation(mode: OrientationMode) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity, mode) {
        activity.requestedOrientation = when (mode) {
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            // During a rotation the activity is rebuilt and must keep the request.
            if (!activity.isChangingConfigurations) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
