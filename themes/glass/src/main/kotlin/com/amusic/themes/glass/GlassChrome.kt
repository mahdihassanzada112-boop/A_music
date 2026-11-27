package com.amusic.themes.glass

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** A floating pill; only the current destination shows its label. */
@Composable
internal fun GlassNavigationBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit) {
    Box(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.glass(CircleShape).padding(6.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEachIndexed { index, item ->
                val chosen = index == selected
                val container by animateColorAsState(
                    if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "nav",
                )
                val content =
                    if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                Row(
                    Modifier.clip(CircleShape).background(container)
                        .selectable(chosen, role = Role.Tab) { onSelect(index) }
                        .heightIn(min = 48.dp).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.icon, if (chosen) null else item.label, tint = content)
                    if (chosen) {
                        Spacer(Modifier.width(8.dp))
                        Text(item.label, color = content)
                    }
                }
            }
        }
    }
}

/** A floating capsule whose ring around the artwork shows progress. */
@Composable
internal fun GlassMiniPlayer(ui: PlayerUi, position: Position, actions: PlayerActions, onOpen: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 12.dp).fillMaxWidth().glass(CircleShape).testTag("mini_player")
            .clickable(onClickLabel = stringResource(ThemingR.string.player_open), onClick = onOpen)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Artwork(ui.artworkUri, Modifier.size(44.dp).clip(CircleShape))
            CircularProgressIndicator(
                progress = { position.value.toFloat() / ui.durationMs.coerceAtLeast(1) },
                modifier = Modifier.size(52.dp),
                strokeWidth = 2.dp,
                trackColor = Color.Transparent,
            )
        }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayButton(
                    ui, actions, 44.dp,
                    MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary,
                )
                NextButton(actions, 28.dp)
            }
        }
    }
}
