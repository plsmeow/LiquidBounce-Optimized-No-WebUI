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

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.gui.hud.HudConfig
import net.ccbluex.liquidbounce.gui.hud.HudElementRegistry
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Vector2f

/**
 * In-game HUD renderer. Delegates to registered [HudElement] instances.
 *
 * Activated by [ModuleHud]. The built-in watermark and array list are
 * handled by `HudElementWatermark` and `HudElementArrayList` respectively.
 */
object NativeHud {

    fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        if (!HudConfig.hudEnabled.get()) {
            return
        }
        for (element in HudElementRegistry.getAll()) {
            if (element.enabled.get()) {
                element.renderPosition = Vector2f(element.position.get())
                element.render(context, event)
            }
        }
    }
}
