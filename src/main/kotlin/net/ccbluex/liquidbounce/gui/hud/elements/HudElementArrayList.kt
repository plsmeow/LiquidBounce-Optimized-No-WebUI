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
package net.ccbluex.liquidbounce.gui.hud.elements

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.gui.clickgui.drawText
import net.ccbluex.liquidbounce.gui.clickgui.fillRect
import net.ccbluex.liquidbounce.gui.hud.HudConfig
import net.ccbluex.liquidbounce.gui.hud.HudElement
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Vector2f

/**
 * Module list HUD: shows every enabled non-hidden module in a vertical strip,
 * sorted by text width. Renders on the right edge of the screen by default.
 */
object HudElementArrayList : HudElement("arraylist", "ArrayList") {

    private const val PADDING = 2f
    private const val SIDE_BAR = 2f
    private val background = Color4b(0x0A, 0x0A, 0x0F, 0xB4)

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            // Default to top-right corner on first launch.
            position.set(Vector2f((screenWidth - 100).toFloat(), 8f))
        }
        alignment = Alignment.TOP_RIGHT
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        val s = scale.get()
        val lineHeight = (mc.font.lineHeight + 2) * s

        val entries = ModuleManager
            .filter { it.enabled && !it.hidden }
            .map { module -> module.name + (module.tag?.let { " $it" } ?: "") }
            .sortedByDescending { mc.font.width(it) }

        val maxWidth = entries.maxOfOrNull { mc.font.width(it) } ?: 0

        lastBaseWidth = (maxWidth + PADDING * 2 + SIDE_BAR)
        lastBaseHeight = (entries.size * (mc.font.lineHeight + 2)).toFloat()

        if (entries.isEmpty()) {
            return
        }

        val (offX, offY) = getOffset()
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)
            var y = 0f
            for ((index, text) in entries.withIndex()) {
                val textWidth = mc.font.width(text).toFloat()
                val color = accentFor(index, entries.size)

                context.fillRect(0f, y, textWidth + PADDING * 2 + SIDE_BAR, mc.font.lineHeight + 2f, background)
                context.drawText(text, PADDING, y + 2f, color.argb, shadow = true)
                context.fillRect(textWidth + PADDING * 2, y, SIDE_BAR, mc.font.lineHeight + 2f, color)
                y += mc.font.lineHeight + 2f
            }
        }
    }

    private fun accentFor(index: Int, total: Int): Color4b {
        if (total <= 1) {
            return HudConfig.accentColor.get()
        }
        val hue = 0.6f + (index.toFloat() / total) * 0.25f
        return Color4b.ofHSB(hue % 1f, 0.55f, 1f)
    }
}
