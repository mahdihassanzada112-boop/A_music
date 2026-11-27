package com.amusic.themes.darkmodern

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amusic.core.design.Artwork
import com.amusic.core.design.artistLabel
import com.amusic.core.theming.LeftToRight
import com.amusic.core.theming.NavItem
import com.amusic.core.theming.NextButton
import com.amusic.core.theming.PlayButton
import com.amusic.core.theming.PlayerActions
import com.amusic.core.theming.PlayerUi
import com.amusic.core.theming.Position
import com.amusic.core.theming.R as ThemingR

/** Flat bar; an amber line marks the current destination. */
@Composable
internal fun DarkNavigationBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).selectableGroup()) {
            items.forEachIndexed { index, item ->
                val color by animateColorAsState(
                    if (index == selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "nav",
                )
                Column(
                    Modifier.weight(1f).heightIn(min = 56.dp)
                        .selectable(index == selected, role = Role.Tab) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.width(32.dp).height(3.dp)
                            .background(if (index == selected) color else Color.Transparent),
                    )
                    Spacer(Modifier.height(6.dp))
                    Icon(item.icon, contentDescription = null, tint = color)
                    Text(item.label, color = color, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** Full-width strip with a thin progress line on top. */
@Composable
internal fun DarkMiniPlayer(ui: PlayerUi, position: Position, actions: PlayerActions, onOpen: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(
            Modifier.testTag("mini_player")
                .clickable(onClickLabel = stringResource(ThemingR.string.player_open), onClick = onOpen),
        ) {
            LeftToRight {
                LinearProgressIndicator(
                    progress = { position.value.toFloat() / ui.durationMs.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    drawStopIndicator = {},
                )
            }
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(ui.artworkUri, Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ui.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        artistLabel(ui.artist),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LeftToRight {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlayButton(
                            ui, actions, 44.dp,
                            MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary,
                        )
                        NextButton(actions, 28.dp)
                    }
                }
            }
        }
    }
}
