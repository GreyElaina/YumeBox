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

package com.github.yumelira.yumebox.core.bridge

import androidx.annotation.Keep
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TailscaleSnapshot(
    val state: String = "",
    val ips: List<String>? = null,
    val hostname: String = "",
    val authUrl: String = "",
    val health: List<String>? = null,
    val online: Boolean = false,
)

@Keep
object TailscaleNotifier {
    private val json = Json { ignoreUnknownKeys = true }

    private val _snapshot = MutableStateFlow(TailscaleSnapshot())
    val snapshot: StateFlow<TailscaleSnapshot> = _snapshot.asStateFlow()

    @JvmStatic
    @Keep
    fun onNotify(snapshotJson: String) {
        runCatching { json.decodeFromString<TailscaleSnapshot>(snapshotJson) }
            .onSuccess { _snapshot.value = it }
    }

    fun reset() {
        _snapshot.value = TailscaleSnapshot()
    }
}

object TailscaleClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exec(request: String): String {
        val deferred = CompletableDeferred<String>()
        Bridge.nativeTailscaleExec(deferred, request)
        return deferred.await()
    }

    suspend fun logout(): Result<Unit> = runCatching {
        exec("""{"cmd":"logout"}""")
        Unit
    }

    suspend fun loginInteractive(): Result<Unit> = runCatching {
        exec("""{"cmd":"login-interactive"}""")
        Unit
    }

    suspend fun status(): String = exec("""{"cmd":"status"}""")
}
