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
package net.ccbluex.liquidbounce.gui.hud

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import org.joml.Vector2f

/**
 * Central persistence layer for all [HudElement]s and HUD-wide preferences
 * (theme colors, scale, font, etc.). The whole group is registered as a
 * single ConfigSystem root, so every value is saved to `LiquidBounce/hud.json`
 * on shutdown and loaded on the next launch.
 *
 * Note: keys are namespaced by the element id (e.g. `arraylist-enabled`) so
 * that elements can be added/removed safely without colliding with each other.
 */
object HudConfig : ValueGroup("HUD") {

    // --- Theme (used by both the in-game HUD and the native ClickGUI) -----
    val accentColor: Value<Color4b> = color("AccentColor", Color4b(89, 124, 255, 255))
    val backgroundColor: Value<Color4b> = color("BackgroundColor", Color4b(20, 21, 26, 242))
    val textColor: Value<Color4b> = color("TextColor", Color4b(232, 234, 240, 255))
    val secondaryTextColor: Value<Color4b> = color("SecondaryTextColor", Color4b(154, 158, 173, 255))
    val fontSize: Value<Float> = float("FontSize", 1.0f, 0.5f..2.0f)
    val rainbow: Value<Boolean> = boolean("Rainbow", false)

    // --- HUD master toggle -----------------------------------------------
    val hudEnabled: Value<Boolean> = boolean("HudEnabled", true)

    /**
     * Casts [this] to [Value]<[T]> for concise typed access in element classes.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> Value<*>.typed(): Value<T> = this as Value<T>

    init {
        // Register the whole group as a top-level config. After this call, every
        // value added by an element (e.g. `HudConfig.boolean("arraylist-enabled", true)`)
        // is automatically persisted to disk and reloaded on next start.
        ConfigSystem.root("hud", mutableListOf(this))
    }
}
