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

import net.ccbluex.liquidbounce.gui.hud.elements.*
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.scaledDimension

/**
 * Central place where every built-in [HudElement] is instantiated and registered.
 *
 * Called once on client startup (after [HudConfig] is loaded) and any time the
 * GUI wants to (re)build the list. The instance is reused across restarts so
 * the [HudConfig]-backed [HudElement.position] / [HudElement.scale] values
 * keep their persisted state.
 */
object HudElements {

    fun registerAll() {
        HudElementRegistry.clear()

        // Each object here is a singleton (same as ClientModule). The order
        // below is also the in-game rendering order (back to front).
        HudElementRegistry.register(HudElementArrayList)
        HudElementRegistry.register(HudElementWatermark)
        HudElementRegistry.register(HudElementCoordinates)
        HudElementRegistry.register(HudElementFps)
        HudElementRegistry.register(HudElementPing)
        HudElementRegistry.register(HudElementServerIp)
        HudElementRegistry.register(HudElementArmor)
        HudElementRegistry.register(HudElementEffects)
        HudElementRegistry.register(HudElementTargetHud)
        HudElementRegistry.register(HudElementNotificationList)

        val (w, h) = mc.window.scaledDimension
        HudElementRegistry.initialize(w, h)
    }
}
