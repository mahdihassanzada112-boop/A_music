package com.amusic.features.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.amusic.core.audio.PlayerState
import com.amusic.core.audio.RepeatSetting

/** Media controls and the timeline stay left-to-right in every language. */
@Composable
internal fun LeftToRight(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)

@Composable
internal fun PlayPauseIcon(showPause: Boolean, modifier: Modifier = Modifier) = Icon(
    painterResource(if (showPause) R.drawable.ic_pause else R.drawable.ic_play),
    stringResource(if (showPause) R.string.player_pause else R.string.player_play),
    modifier,
)

@Composable
internal fun TransportRow(
    state: PlayerState,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
) = LeftToRight {
    val active = MaterialTheme.colorScheme.primary
    val idle = LocalContentColor.current.copy(alpha = 0.55f)
    val shuffleState = stringResource(if (state.shuffle) R.string.state_on else R.string.state_off)
    val repeatState = stringResource(
        when (state.repeat) {
            RepeatSetting.OFF -> R.string.state_off
            RepeatSetting.ALL -> R.string.repeat_all
            RepeatSetting.ONE -> R.string.repeat_one
        },
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onShuffle, Modifier.semantics { stateDescription = shuffleState }) {
            Icon(
                painterResource(R.drawable.ic_shuffle),
                stringResource(R.string.player_shuffle),
                tint = if (state.shuffle) active else idle,
            )
        }
        IconButton(onPrevious) {
            Icon(painterResource(R.drawable.ic_previous), stringResource(R.string.player_previous), Modifier.size(36.dp))
        }
        FilledIconButton(onToggle, Modifier.size(72.dp)) {
            PlayPauseIcon(state.showPause, Modifier.size(36.dp))
        }
        IconButton(onNext) {
            Icon(painterResource(R.drawable.ic_next), stringResource(R.string.player_next), Modifier.size(36.dp))
        }
        IconButton(onRepeat, Modifier.semantics { stateDescription = repeatState }) {
            Icon(
                painterResource(
                    if (state.repeat == RepeatSetting.ONE) R.drawable.ic_repeat_one
                    else R.drawable.ic_repeat,
                ),
                stringResource(R.string.player_repeat),
                tint = if (state.repeat == RepeatSetting.OFF) idle else active,
            )
        }
    }
}
