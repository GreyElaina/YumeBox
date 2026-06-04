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

package com.github.yumelira.yumebox.data.store

import com.tencent.mmkv.MMKV

class TailscaleSettingsStore(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {

    val enabled by boolFlow(false)
    val hostname by strFlow("")
    val authKey by strFlow("")
    val controlUrl by strFlow("")
    val ephemeral by boolFlow(false)
    val udp by boolFlow(true)
    val acceptRoutes by boolFlow(true)
    val exitNode by strFlow("")
    val exitNodeAllowLanAccess by boolFlow(false)
}
