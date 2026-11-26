package com.amusic.features.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.design.Artwork
import com.amusic.core.design.artistLabel

/** Shown above the navigation bar whenever something is queued. */
@Composable
fun MiniPlayer(onOpen: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.player.state.collectAsStateWithLifecycle()
    val current = state.current ?: return
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier.clickable(onClickLabel = stringResource(R.string.player_open), onClick = onOpen)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(state.artworkUri, Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(current.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    artistLabel(current.artist),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LeftToRight {
                Row {
                    IconButton(onClick = { viewModel.player.togglePlay() }) {
                        PlayPauseIcon(state.showPause)
                    }
                    IconButton(onClick = { viewModel.player.next() }) {
                        Icon(painterResource(R.drawable.ic_next), stringResource(R.string.player_next))
                    }
                }
            }
        }
    }
}
