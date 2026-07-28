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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.gui.clickgui.drawText
import net.ccbluex.liquidbounce.gui.clickgui.fillRect
import net.ccbluex.liquidbounce.gui.hud.HudConfig
import net.ccbluex.liquidbounce.gui.hud.HudElement
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Vector2f

/** Watermark in the top-left corner: client name + version. */
object HudElementWatermark : HudElement("watermark", "Watermark") {

    private val background = Color4b(0x0A, 0x0A, 0x0F, 0xB4)

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            position.set(Vector2f(4f, 4f))
        }
        alignment.set(Alignment.TOP_LEFT)
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        val name = LiquidBounce.CLIENT_NAME
        val version = "v${LiquidBounce.clientVersion}"
        val s = scale.get()
        val lineHeight = (mc.font.lineHeight + 1) * s
        val textPadding = 6f
        val width = maxOf(mc.font.width(name), mc.font.width(version)) + textPadding * 2f
        val barWidth = 2f
        val totalHeight = lineHeight * 2 + 4f

        lastBaseWidth = width
        lastBaseHeight = totalHeight

        val (offX, offY) = getOffset()
        val horizontal = alignment.get().horizontalComponent
        val barX = when (horizontal) {
            HudElement.Alignment.Horizontal.LEFT -> 0f
            HudElement.Alignment.Horizontal.RIGHT -> width - barWidth
            HudElement.Alignment.Horizontal.CENTER -> 0f
        }
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)
            context.fillRect(0f, 0f, width, totalHeight, background)
            context.fillRect(barX, 0f, barWidth, totalHeight, HudConfig.accentColor.get())
            context.drawText(name, 6f, 2f, HudConfig.accentColor.get().argb, shadow = true)
            context.drawText(version, 6f, 2f + mc.font.lineHeight + 1f,
                HudConfig.secondaryTextColor.get().argb, shadow = true)
        }
    }
}
