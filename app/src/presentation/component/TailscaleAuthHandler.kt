/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira & YumeRiMoe 2025 - Present
 *
 */

package com.github.yumelira.yumebox.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.yumelira.yumebox.common.util.openUrl
import com.github.yumelira.yumebox.core.bridge.TailscaleNotifier
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import kotlinx.coroutines.flow.filter
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private enum class PillAnchor { Visible, Dismissed }

@Composable
fun TailscaleAuthHandler() {
    val snapshot by TailscaleNotifier.snapshot.collectAsState()
    val authUrl = snapshot.authUrl
    val context = LocalContext.current
    var dismissed by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val dismissDistancePx = with(density) { 160.dp.toPx() }

    val anchoredState = remember {
        AnchoredDraggableState(
            initialValue = PillAnchor.Visible,
            anchors = DraggableAnchors {
                PillAnchor.Visible at 0f
                PillAnchor.Dismissed at -dismissDistancePx
            },
        )
    }

    LaunchedEffect(anchoredState) {
        snapshotFlow { anchoredState.settledValue }
            .filter { it == PillAnchor.Dismissed }
            .collect { dismissed = authUrl }
    }

    val visible = authUrl.isNotBlank() && authUrl != dismissed
    val pillShape = RoundedCornerShape(50)
    val progress = if (dismissDistancePx != 0f) {
        (anchoredState.offset / -dismissDistancePx).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .offset { IntOffset(0, anchoredState.offset.roundToInt().coerceAtMost(0)) }
                    .alpha(1f - progress * 0.6f)
                    .anchoredDraggable(anchoredState, Orientation.Vertical)
                    .padding(top = AppTheme.spacing.space8)
                    .shadow(elevation = 6.dp, shape = pillShape)
                    .clip(pillShape)
                    .background(MiuixTheme.colorScheme.surfaceVariant)
                    .clickable {
                        runCatching { openUrl(context, authUrl) }
                        dismissed = authUrl
                    }
                    .padding(
                        horizontal = AppTheme.spacing.space16,
                        vertical = AppTheme.spacing.space8,
                    ),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tailscale Login Required",
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Open →",
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                )
            }
        }
    }
}
