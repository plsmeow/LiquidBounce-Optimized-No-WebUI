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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.ClickGuiScaleChangeEvent
import net.ccbluex.liquidbounce.event.events.ClickGuiValueChangeEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.gui.clickgui.ClickGuiScreen
import net.ccbluex.liquidbounce.utils.client.inGame
import org.lwjgl.glfw.GLFW

/**
 * ClickGUI module
 *
 * Shows you an easy-to-use, native Meteor-style menu to toggle and configure modules.
 */
object ModuleClickGui :
    ClientModule("ClickGUI", ModuleCategories.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT, disableActivation = true) {

    override val running get() = true

    val scale by float("Scale", 1f, 0.5f..2f).onChange { value ->
        EventManager.callEvent(ClickGuiScaleChangeEvent(value))
        value
    }

    val searchBarAutoFocus by boolean("SearchBarAutoFocus", true).onChanged {
        EventManager.callEvent(ClickGuiValueChangeEvent(this@ModuleClickGui))
    }

    val snapping = tree(Snapping)

    object Snapping : ToggleableValueGroup(ModuleClickGui, "Snapping", enabled = true) {
        val gridSize by int("GridSize", 10, 1..100, "px")
    }

    /**
     * True while the player is typing inside a text field of the native ClickGUI,
     * used by other modules (e.g. InventoryMove) to avoid reacting to keybinds.
     */
    val isInSearchBar: Boolean
        get() = (mc.gui.screen() as? ClickGuiScreen)?.isCapturingText == true

    override fun onEnabled() {
        if (!LiquidBounce.isInitialized || !inGame) {
            return
        }

        mc.execute {
            mc.gui.setScreen(ClickGuiScreen())
        }
        super.onEnabled()
    }

    @JvmStatic
    fun sync() = Unit

    @JvmStatic
    fun invalidate() = Unit
}
