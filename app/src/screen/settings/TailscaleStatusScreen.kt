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

package com.github.yumelira.yumebox.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumelira.yumebox.core.bridge.TailscaleNotifier
import com.github.yumelira.yumebox.core.bridge.TailscaleSnapshot
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.oom_wg.purejoy.mlang.MLang
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
@Destination<RootGraph>
fun TailscaleStatusScreen() {
    val scrollBehavior = MiuixScrollBehavior()
    val snapshot by TailscaleNotifier.snapshot.collectAsState()

    Scaffold(
        topBar = {
            TopBar(title = MLang.TailscaleSettings.Status.Title, scrollBehavior = scrollBehavior)
        }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                TailscaleConnectionSection(snapshot)
            }
            if (snapshot.health?.isNotEmpty() == true) {
                item {
                    TailscaleHealthSection(snapshot)
                }
            }
        }
    }
}

@Composable
private fun TailscaleConnectionSection(snapshot: TailscaleSnapshot) {
    Title(MLang.TailscaleSettings.Status.State)
    Card {
        PreferenceArrowItem(
            title = MLang.TailscaleSettings.Status.State,
            summary = stateDisplayText(snapshot.state),
            onClick = {},
        )
        if (snapshot.hostname.isNotBlank()) {
            PreferenceArrowItem(
                title = MLang.TailscaleSettings.Status.Hostname,
                summary = snapshot.hostname,
                onClick = {},
            )
        }
        val ips = snapshot.ips
        if (!ips.isNullOrEmpty()) {
            PreferenceArrowItem(
                title = MLang.TailscaleSettings.Status.IPs,
                summary = ips.joinToString("\n"),
                onClick = {},
            )
        }
        PreferenceArrowItem(
            title = MLang.TailscaleSettings.Status.Online,
            summary = if (snapshot.online) {
                MLang.TailscaleSettings.Status.Online
            } else {
                MLang.TailscaleSettings.Status.Offline
            },
            onClick = {},
        )
    }
}

@Composable
private fun TailscaleHealthSection(snapshot: TailscaleSnapshot) {
    Title(MLang.TailscaleSettings.Status.Health)
    Card {
        Column {
            snapshot.health?.forEach { warning ->
                PreferenceArrowItem(
                    title = warning,
                    onClick = {},
                )
            }
        }
    }
}

private fun stateDisplayText(state: String): String = when (state) {
    "NoState" -> MLang.TailscaleSettings.Status.NoState
    "NeedsLogin" -> MLang.TailscaleSettings.Status.NeedsLogin
    "NeedsMachineAuth" -> MLang.TailscaleSettings.Status.NeedsMachineAuth
    "Stopped" -> MLang.TailscaleSettings.Status.Stopped
    "Starting" -> MLang.TailscaleSettings.Status.Starting
    "Running" -> MLang.TailscaleSettings.Status.Running
    "InUseOtherUser" -> MLang.TailscaleSettings.Status.InUseOtherUser
    "" -> MLang.TailscaleSettings.Status.NotRunning
    else -> state
}
