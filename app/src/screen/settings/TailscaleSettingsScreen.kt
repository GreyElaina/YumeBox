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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumelira.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumelira.yumebox.presentation.component.Card
import com.github.yumelira.yumebox.presentation.component.DialogButtonRow
import com.github.yumelira.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumelira.yumebox.presentation.component.PreferenceSwitchItem
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.Title
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.TailscaleStatusScreenDestination
import dev.oom_wg.purejoy.mlang.MLang
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
@Destination<RootGraph>
fun TailscaleSettingsScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<TailscaleSettingsViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val networkUiState by viewModel.networkUiState.collectAsState()
    val showRevokeDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(title = MLang.TailscaleSettings.Title, scrollBehavior = scrollBehavior)
        }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                TailscaleEnableSection(
                    enabled = uiState.enabled,
                    onEnabledChange = viewModel::onEnabledChange,
                )
            }
            item {
                AnimatedVisibility(
                    visible = uiState.enabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        Card {
                            PreferenceArrowItem(
                                title = MLang.TailscaleSettings.StatusTitle,
                                summary = MLang.TailscaleSettings.StatusSummary,
                                onClick = { navigator.navigate(TailscaleStatusScreenDestination) },
                            )
                        }
                        TailscaleGeneralSection(
                            uiState = uiState,
                            onHostnameDraftChange = viewModel::onHostnameDraftChange,
                            commitHostname = viewModel::commitHostname,
                            onAuthKeyDraftChange = viewModel::onAuthKeyDraftChange,
                            commitAuthKey = viewModel::commitAuthKey,
                            onControlUrlDraftChange = viewModel::onControlUrlDraftChange,
                            commitControlUrl = viewModel::commitControlUrl,
                            onEphemeralChange = viewModel::onEphemeralChange,
                            onUdpChange = viewModel::onUdpChange,
                        )
                        TailscaleNetworkSection(
                            networkUiState = networkUiState,
                            onAcceptRoutesChange = viewModel::onAcceptRoutesChange,
                            onExitNodeDraftChange = viewModel::onExitNodeDraftChange,
                            commitExitNode = viewModel::commitExitNode,
                            onExitNodeAllowLanChange = viewModel::onExitNodeAllowLanChange,
                        )
                        TailscaleDangerSection(
                            onLoginClick = viewModel::triggerLogin,
                            onRevokeClick = { showRevokeDialog.value = true },
                        )
                    }
                }
            }
        }
    }

    TailscaleRevokeDialog(
        showState = showRevokeDialog,
        onConfirm = {
            showRevokeDialog.value = false
            viewModel.revokeAuth()
        },
        onDismiss = { showRevokeDialog.value = false },
    )
}

@Composable
private fun TailscaleEnableSection(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Card {
        PreferenceSwitchItem(
            title = MLang.TailscaleSettings.EnableTitle,
            summary = MLang.TailscaleSettings.EnableSummary,
            checked = enabled,
            onCheckedChange = onEnabledChange,
        )
    }
}

@Composable
private fun TailscaleGeneralSection(
    uiState: TailscaleUiState,
    onHostnameDraftChange: (String) -> Unit,
    commitHostname: () -> Unit,
    onAuthKeyDraftChange: (String) -> Unit,
    commitAuthKey: () -> Unit,
    onControlUrlDraftChange: (String) -> Unit,
    commitControlUrl: () -> Unit,
    onEphemeralChange: (Boolean) -> Unit,
    onUdpChange: (Boolean) -> Unit,
) {
    var editDialog by remember { mutableStateOf<TailscaleEditDialog?>(null) }

    Title(MLang.TailscaleSettings.Section.General)
    Card {
        PreferenceArrowItem(
            title = MLang.TailscaleSettings.HostnameTitle,
            summary = uiState.hostnameDraft.ifBlank { MLang.TailscaleSettings.HostnameSummary },
            onClick = { editDialog = TailscaleEditDialog.Hostname },
        )
        PreferenceArrowItem(
            title = MLang.TailscaleSettings.AuthKeyTitle,
            summary = if (uiState.authKeyDraft.isNotBlank()) "••••••••" else MLang.TailscaleSettings.AuthKeySummary,
            onClick = { editDialog = TailscaleEditDialog.AuthKey },
        )
        PreferenceArrowItem(
            title = MLang.TailscaleSettings.ControlUrlTitle,
            summary = uiState.controlUrlDraft.ifBlank { MLang.TailscaleSettings.ControlUrlSummary },
            onClick = { editDialog = TailscaleEditDialog.ControlUrl },
        )
        PreferenceSwitchItem(
            title = MLang.TailscaleSettings.EphemeralTitle,
            summary = MLang.TailscaleSettings.EphemeralSummary,
            checked = uiState.ephemeral,
            onCheckedChange = onEphemeralChange,
        )
        PreferenceSwitchItem(
            title = MLang.TailscaleSettings.UdpTitle,
            summary = MLang.TailscaleSettings.UdpSummary,
            checked = uiState.udp,
            onCheckedChange = onUdpChange,
        )
    }

    TailscaleEditDialogs(
        editDialog = editDialog,
        uiState = uiState,
        onHostnameDraftChange = onHostnameDraftChange,
        commitHostname = commitHostname,
        onAuthKeyDraftChange = onAuthKeyDraftChange,
        commitAuthKey = commitAuthKey,
        onControlUrlDraftChange = onControlUrlDraftChange,
        commitControlUrl = commitControlUrl,
        onDismiss = { editDialog = null },
    )
}

@Composable
private fun TailscaleNetworkSection(
    networkUiState: TailscaleNetworkUiState,
    onAcceptRoutesChange: (Boolean) -> Unit,
    onExitNodeDraftChange: (String) -> Unit,
    commitExitNode: () -> Unit,
    onExitNodeAllowLanChange: (Boolean) -> Unit,
) {
    var editDialog by remember { mutableStateOf<TailscaleEditDialog?>(null) }

    Title(MLang.TailscaleSettings.Section.Network)
    Card {
        PreferenceSwitchItem(
            title = MLang.TailscaleSettings.AcceptRoutesTitle,
            summary = MLang.TailscaleSettings.AcceptRoutesSummary,
            checked = networkUiState.acceptRoutes,
            onCheckedChange = onAcceptRoutesChange,
        )
        PreferenceArrowItem(
            title = MLang.TailscaleSettings.ExitNodeTitle,
            summary = networkUiState.exitNodeDraft.ifBlank { MLang.TailscaleSettings.ExitNodeSummary },
            onClick = { editDialog = TailscaleEditDialog.ExitNode },
        )
        AnimatedVisibility(
            visible = networkUiState.exitNodeDraft.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            PreferenceSwitchItem(
                title = MLang.TailscaleSettings.ExitNodeAllowLanTitle,
                summary = MLang.TailscaleSettings.ExitNodeAllowLanSummary,
                checked = networkUiState.exitNodeAllowLanAccess,
                onCheckedChange = onExitNodeAllowLanChange,
            )
        }
    }

    when (editDialog) {
        TailscaleEditDialog.ExitNode ->
            TailscaleTextEditDialog(
                title = MLang.TailscaleSettings.ExitNodeTitle,
                value = networkUiState.exitNodeDraft,
                onValueChange = onExitNodeDraftChange,
                onDismiss = { editDialog = null },
                onCommit = commitExitNode,
            )
        else -> Unit
    }
}

@Composable
private fun TailscaleEditDialogs(
    editDialog: TailscaleEditDialog?,
    uiState: TailscaleUiState,
    onHostnameDraftChange: (String) -> Unit,
    commitHostname: () -> Unit,
    onAuthKeyDraftChange: (String) -> Unit,
    commitAuthKey: () -> Unit,
    onControlUrlDraftChange: (String) -> Unit,
    commitControlUrl: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (editDialog) {
        TailscaleEditDialog.Hostname ->
            TailscaleTextEditDialog(
                title = MLang.TailscaleSettings.HostnameTitle,
                value = uiState.hostnameDraft,
                onValueChange = onHostnameDraftChange,
                onDismiss = onDismiss,
                onCommit = commitHostname,
            )
        TailscaleEditDialog.AuthKey ->
            TailscaleTextEditDialog(
                title = MLang.TailscaleSettings.AuthKeyTitle,
                value = uiState.authKeyDraft,
                onValueChange = onAuthKeyDraftChange,
                onDismiss = onDismiss,
                onCommit = commitAuthKey,
            )
        TailscaleEditDialog.ControlUrl ->
            TailscaleTextEditDialog(
                title = MLang.TailscaleSettings.ControlUrlTitle,
                value = uiState.controlUrlDraft,
                onValueChange = onControlUrlDraftChange,
                onDismiss = onDismiss,
                onCommit = commitControlUrl,
            )
        else -> Unit
    }
}

@Composable
private fun TailscaleTextEditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCommit: () -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
) {
    val focusManager = LocalFocusManager.current
    var localTextFieldValue by
        remember(title) {
            mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
        }

    AppTextFieldDialog(
        show = true,
        title = title,
        textFieldValue = localTextFieldValue,
        onTextFieldValueChange = { updatedTextFieldValue ->
            localTextFieldValue = updatedTextFieldValue
            onValueChange(updatedTextFieldValue.text)
        },
        onDismissRequest = onDismiss,
        onConfirm = {
            onCommit()
            focusManager.clearFocus()
            onDismiss()
        },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions =
            KeyboardActions(
                onDone = {
                    onCommit()
                    onDismiss()
                    focusManager.clearFocus()
                }
            ),
    )
}

@Composable
private fun TailscaleDangerSection(
    onLoginClick: () -> Unit,
    onRevokeClick: () -> Unit,
) {
    Title(MLang.TailscaleSettings.Section.Danger)
    Card {
        PreferenceArrowItem(
            title = MLang.TailscaleSettings.LoginTitle,
            summary = MLang.TailscaleSettings.LoginSummary,
            onClick = onLoginClick,
        )
        PreferenceArrowItem(
            title = MLang.TailscaleSettings.RevokeTitle,
            summary = MLang.TailscaleSettings.RevokeSummary,
            onClick = onRevokeClick,
        )
    }
}

@Composable
private fun TailscaleRevokeDialog(
    showState: MutableState<Boolean>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = showState.value,
        title = MLang.TailscaleSettings.RevokeConfirmTitle,
        titleColor = DialogDefaults.titleColor(),
        summary = MLang.TailscaleSettings.RevokeConfirmMessage,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onConfirm,
            )
        },
    )
}

private enum class TailscaleEditDialog {
    Hostname,
    AuthKey,
    ControlUrl,
    ExitNode,
}
