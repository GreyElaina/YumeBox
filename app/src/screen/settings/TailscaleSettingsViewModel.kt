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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumelira.yumebox.common.util.showToastDialog
import com.github.yumelira.yumebox.core.bridge.TailscaleClient
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.data.store.TailscaleSettingsStore
import com.github.yumelira.yumebox.runtime.client.ProxyFacade
import com.github.yumelira.yumebox.runtime.client.RuntimeStateMapper
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TailscaleSettingsViewModel(
    private val store: TailscaleSettingsStore,
    private val proxyFacade: ProxyFacade,
) : ViewModel() {

    val enabled: Preference<Boolean> = store.enabled
    val ephemeral: Preference<Boolean> = store.ephemeral
    val udp: Preference<Boolean> = store.udp
    val acceptRoutes: Preference<Boolean> = store.acceptRoutes
    val exitNodeAllowLanAccess: Preference<Boolean> = store.exitNodeAllowLanAccess

    private val _hostnameDraft = MutableStateFlow(store.hostname.value)
    val hostnameDraft: StateFlow<String> = _hostnameDraft.asStateFlow()

    private val _authKeyDraft = MutableStateFlow(store.authKey.value)
    val authKeyDraft: StateFlow<String> = _authKeyDraft.asStateFlow()

    private val _controlUrlDraft = MutableStateFlow(store.controlUrl.value)
    val controlUrlDraft: StateFlow<String> = _controlUrlDraft.asStateFlow()

    private val _exitNodeDraft = MutableStateFlow(store.exitNode.value)
    val exitNodeDraft: StateFlow<String> = _exitNodeDraft.asStateFlow()

    private val draftsUiState = combine(
        _hostnameDraft, _authKeyDraft, _controlUrlDraft,
    ) { hostname, authKey, controlUrl ->
        Triple(hostname, authKey, controlUrl)
    }

    val uiState: StateFlow<TailscaleUiState> =
        combine(
            enabled.state,
            draftsUiState,
            ephemeral.state,
            udp.state,
        ) { enabled, drafts, ephemeral, udp ->
            TailscaleUiState(
                enabled = enabled,
                hostnameDraft = drafts.first,
                authKeyDraft = drafts.second,
                controlUrlDraft = drafts.third,
                ephemeral = ephemeral,
                udp = udp,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TailscaleUiState(
                    enabled = enabled.value,
                    hostnameDraft = _hostnameDraft.value,
                    authKeyDraft = _authKeyDraft.value,
                    controlUrlDraft = _controlUrlDraft.value,
                    ephemeral = ephemeral.value,
                    udp = udp.value,
                ),
            )

    val networkUiState: StateFlow<TailscaleNetworkUiState> =
        combine(
            acceptRoutes.state,
            _exitNodeDraft,
            exitNodeAllowLanAccess.state,
        ) { acceptRoutes, exitNodeDraft, exitNodeAllowLan ->
            TailscaleNetworkUiState(
                acceptRoutes = acceptRoutes,
                exitNodeDraft = exitNodeDraft,
                exitNodeAllowLanAccess = exitNodeAllowLan,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TailscaleNetworkUiState(
                    acceptRoutes = acceptRoutes.value,
                    exitNodeDraft = _exitNodeDraft.value,
                    exitNodeAllowLanAccess = exitNodeAllowLanAccess.value,
                ),
            )

    fun onEnabledChange(value: Boolean) {
        enabled.set(value)
        syncAndRestart()
    }

    fun onEphemeralChange(value: Boolean) {
        ephemeral.set(value)
        syncIfEnabled()
    }

    fun onUdpChange(value: Boolean) {
        udp.set(value)
        syncIfEnabled()
    }

    fun onAcceptRoutesChange(value: Boolean) {
        acceptRoutes.set(value)
        syncIfEnabled()
    }

    fun onExitNodeAllowLanChange(value: Boolean) {
        exitNodeAllowLanAccess.set(value)
        syncIfEnabled()
    }

    fun onHostnameDraftChange(value: String) {
        _hostnameDraft.value = value
    }

    fun commitHostname() {
        val normalized = _hostnameDraft.value.trim()
        _hostnameDraft.value = normalized
        store.hostname.set(normalized)
        syncIfEnabled()
    }

    fun onAuthKeyDraftChange(value: String) {
        _authKeyDraft.value = value
    }

    fun commitAuthKey() {
        val normalized = _authKeyDraft.value.trim()
        _authKeyDraft.value = normalized
        store.authKey.set(normalized)
        syncIfEnabled()
    }

    fun onControlUrlDraftChange(value: String) {
        _controlUrlDraft.value = value
    }

    fun commitControlUrl() {
        val normalized = _controlUrlDraft.value.trim()
        _controlUrlDraft.value = normalized
        store.controlUrl.set(normalized)
        syncIfEnabled()
    }

    fun onExitNodeDraftChange(value: String) {
        _exitNodeDraft.value = value
    }

    fun commitExitNode() {
        val normalized = _exitNodeDraft.value.trim()
        _exitNodeDraft.value = normalized
        store.exitNode.set(normalized)
        syncIfEnabled()
    }

    private fun syncIfEnabled() {
        if (!enabled.value) return
        syncAndRestart()
    }

    fun triggerLogin() {
        viewModelScope.launch(Dispatchers.IO) {
            TailscaleClient.loginInteractive()
                .onFailure {
                    showToastDialog(
                        message = it.message ?: "unknown error",
                        title = "Tailscale",
                    )
                }
        }
    }

    fun revokeAuth() {
        viewModelScope.launch(Dispatchers.IO) {
            TailscaleClient.logout()
                .onSuccess {
                    showToastDialog(
                        message = MLang.TailscaleSettings.RevokeSuccess,
                        title = "Tailscale",
                    )
                    syncAndRestart()
                }
                .onFailure {
                    showToastDialog(
                        message = MLang.TailscaleSettings.RevokeFailure.format(it.message),
                        title = "Tailscale",
                    )
                }
        }
    }

    private fun syncAndRestart() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = proxyFacade.runtimeSnapshot.value
            if (RuntimeStateMapper.isActuallyRunning(snapshot)) {
                val mode = RuntimeStateMapper.modeForOwner(snapshot.owner) ?: return@launch
                proxyFacade.startProxy(mode)
            }
        }
    }
}

data class TailscaleUiState(
    val enabled: Boolean = false,
    val hostnameDraft: String = "",
    val authKeyDraft: String = "",
    val controlUrlDraft: String = "",
    val ephemeral: Boolean = false,
    val udp: Boolean = true,
)

data class TailscaleNetworkUiState(
    val acceptRoutes: Boolean = true,
    val exitNodeDraft: String = "",
    val exitNodeAllowLanAccess: Boolean = false,
)
