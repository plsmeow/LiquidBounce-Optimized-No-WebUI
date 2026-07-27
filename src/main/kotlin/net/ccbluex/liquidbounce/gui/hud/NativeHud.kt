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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.gui.clickgui.ClickGuiTheme
import net.ccbluex.liquidbounce.gui.clickgui.drawText
import net.ccbluex.liquidbounce.gui.clickgui.fillRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Simple native, Meteor-Client-style HUD drawn directly with the render engine:
 * a watermark in the top-left corner and an arraylist of enabled modules in the
 * top-right corner. Replaces the former browser-rendered HUD.
 */
object NativeHud {

    private const val MARGIN = 2f
    private const val PADDING = 2f
    private val background = Color4b(0x0A, 0x0A, 0x0F, 0xB4)

    fun render(context: GuiGraphicsExtractor) {
        drawWatermark(context)
        drawArrayList(context)
    }

    private fun drawWatermark(context: GuiGraphicsExtractor) {
        val name = LiquidBounce.CLIENT_NAME
        val version = "v${LiquidBounce.clientVersion}"
        val lineHeight = mc.font.lineHeight + 1

        val width = maxOf(context.stringWidth(name), context.stringWidth(version)) + PADDING * 2
        context.fillRect(MARGIN, MARGIN, width, (lineHeight * 2 + PADDING).toFloat(), background)
        context.fillRect(MARGIN, MARGIN, 2f, (lineHeight * 2 + PADDING).toFloat(), ClickGuiTheme.accent)

        context.drawText(name, MARGIN + PADDING + 2f, MARGIN + PADDING, ClickGuiTheme.accent.argb, shadow = true)
        context.drawText(version, MARGIN + PADDING + 2f, MARGIN + PADDING + lineHeight,
            ClickGuiTheme.textSecondary, shadow = true)
    }

    private fun drawArrayList(context: GuiGraphicsExtractor) {
        val screenWidth = mc.window.guiScaledWidth.toFloat()
        val lineHeight = mc.font.lineHeight + 2

        val entries = ModuleManager
            .filter { it.enabled && !it.hidden }
            .map { module -> module.name + (module.tag?.let { " $it" } ?: "") }
            .sortedByDescending { context.stringWidth(it) }

        var y = MARGIN
        for ((index, text) in entries.withIndex()) {
            val textWidth = context.stringWidth(text)
            val x = screenWidth - textWidth - PADDING * 2 - 2f
            val color = accentFor(index, entries.size)

            context.fillRect(x, y, textWidth + PADDING * 2 + 2f, lineHeight.toFloat(), background)
            context.drawText(text, x + PADDING, y + 2f, color.argb, shadow = true)
            context.fillRect(screenWidth - 2f, y, 2f, lineHeight.toFloat(), color)

            y += lineHeight
        }
    }

    private fun accentFor(index: Int, total: Int): Color4b {
        if (total <= 1) {
            return ClickGuiTheme.accent
        }
        val hue = 0.6f + (index.toFloat() / total) * 0.25f
        return Color4b.ofHSB(hue % 1f, 0.55f, 1f)
    }

    private fun GuiGraphicsExtractor.stringWidth(text: String): Int = mc.font.width(text)
}
