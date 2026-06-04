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

package com.github.yumelira.yumebox.data.controller

import com.github.yumelira.yumebox.data.store.TailscaleSettingsStore

class TailscaleConfigPostProcessor(
    private val store: TailscaleSettingsStore,
) {
    fun isEnabled(): Boolean = store.enabled.value

    fun buildProxyEntry(): LinkedHashMap<String, Any?> = linkedMapOf<String, Any?>().apply {
        put("name", PROXY_NAME)
        put("type", "tailscale")

        val hostname = store.hostname.value.trim()
        if (hostname.isNotEmpty()) put("hostname", hostname)

        val authKey = store.authKey.value.trim()
        if (authKey.isNotEmpty()) put("auth-key", authKey)

        val controlUrl = store.controlUrl.value.trim()
        if (controlUrl.isNotEmpty()) put("control-url", controlUrl)

        put("ephemeral", store.ephemeral.value)
        put("udp", store.udp.value)
        put("accept-routes", store.acceptRoutes.value)

        val exitNode = store.exitNode.value.trim()
        if (exitNode.isNotEmpty()) {
            put("exit-node", exitNode)
            put("exit-node-allow-lan-access", store.exitNodeAllowLanAccess.value)
        }
    }

    companion object {
        const val PROXY_NAME = "Tailscale"

        val ROUTING_RULES = listOf(
            "IP-CIDR,100.64.0.0/10,$PROXY_NAME,no-resolve",
            "IP-CIDR6,fd7a:115c:a1e0::/48,$PROXY_NAME,no-resolve",
            "DOMAIN-SUFFIX,ts.net,$PROXY_NAME",
        )
    }
}
