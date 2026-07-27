/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.gui.clickgui

import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.fileGson
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Persists ClickGUI panel positions and collapsed state between sessions.
 * Saved to `LiquidBounce/clickgui.json`.
 */
object ClickGuiState {

    private val file get() = File(ConfigSystem.rootFolder, "clickgui.json")

    /** Map of category tag -> PanelState (x, y, collapsed). */
    private val panels = ConcurrentHashMap<String, PanelState>()

    data class PanelState(
        var x: Float = 0f,
        var y: Float = 0f,
        var collapsed: Boolean = false
    )

    fun load() {
        if (!file.exists()) return
        runCatching {
            val type = object : TypeToken<Map<String, PanelState>>() {}.type
            val map: Map<String, PanelState> = fileGson.fromJson(file.readText(), type)
            panels.putAll(map)
        }
    }

    fun save() {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(fileGson.toJson(panels))
        }
    }

    fun getState(categoryTag: String): PanelState? = panels[categoryTag]

    fun setState(categoryTag: String, state: PanelState) {
        panels[categoryTag] = state
    }
}
