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
 * Persists ClickGUI panel positions, collapsed state, module expanded state,
 * and setting expanded state between sessions.
 * Saved to `LiquidBounce/clickgui.json`.
 */
object ClickGuiState {

    private val file get() = File(ConfigSystem.rootFolder, "clickgui.json")

    /** Map of category tag -> PanelState (x, y, collapsed). */
    private val panels = ConcurrentHashMap<String, PanelState>()

    /** Map of module name -> expanded state. */
    private val moduleExpandedStates = ConcurrentHashMap<String, Boolean>()

    /** Map of setting identifier -> expanded state. */
    private val settingExpandedStates = ConcurrentHashMap<String, Boolean>()

    data class PanelState(
        var x: Float = 0f,
        var y: Float = 0f,
        var collapsed: Boolean = false
    )

    fun load() {
        if (!file.exists()) return
        runCatching {
            val json = file.readText()
            val tree = fileGson.fromJson(json, Map::class.java) as? Map<*, *> ?: return

            // Support both new format { panels: {...}, expanded: {...} } and
            // old format { "combat": { x, y, collapsed }, ... }
            val panelsData = if (tree.containsKey("panels")) {
                @Suppress("UNCHECKED_CAST")
                tree["panels"] as? Map<String, Map<String, Any>>
            } else {
                @Suppress("UNCHECKED_CAST")
                tree as? Map<String, Map<String, Any>>
            }

            if (panelsData != null) {
                for ((key, value) in panelsData) {
                    val x = (value["x"] as? Number)?.toFloat() ?: 0f
                    val y = (value["y"] as? Number)?.toFloat() ?: 0f
                    val collapsed = value["collapsed"] as? Boolean ?: false
                    panels[key] = PanelState(x, y, collapsed)
                }
            }

            // Load expanded states
            val expandedData = tree["expanded"]
            if (expandedData is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val modules = expandedData["modules"] as? Map<String, Boolean>
                if (modules != null) {
                    moduleExpandedStates.putAll(modules)
                }
                @Suppress("UNCHECKED_CAST")
                val settings = expandedData["settings"] as? Map<String, Boolean>
                if (settings != null) {
                    settingExpandedStates.putAll(settings)
                }
            }
        }
    }

    fun save() {
        runCatching {
            file.parentFile?.mkdirs()

            val panelsJson = fileGson.toJson(panels)
            val expandedJson = fileGson.toJson(mapOf(
                "modules" to moduleExpandedStates,
                "settings" to settingExpandedStates
            ))

            file.writeText("""{"panels":$panelsJson,"expanded":$expandedJson}""")
        }
    }

    fun getState(categoryTag: String): PanelState? = panels[categoryTag]

    fun setState(categoryTag: String, state: PanelState) {
        panels[categoryTag] = state
    }

    fun getModuleExpanded(moduleName: String): Boolean = moduleExpandedStates[moduleName] ?: false

    fun setModuleExpanded(moduleName: String, expanded: Boolean) {
        moduleExpandedStates[moduleName] = expanded
    }

    fun getSettingExpanded(key: String): Boolean = settingExpandedStates[key] ?: false

    fun setSettingExpanded(key: String, expanded: Boolean) {
        settingExpandedStates[key] = expanded
    }

    fun collectCurrentExpandedStates(
        getModuleExpanded: (String) -> Boolean,
        getSettingExpanded: (String) -> Boolean
    ) {
        // Placeholder for future use
    }
}
