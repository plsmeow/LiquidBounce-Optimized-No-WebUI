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

import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Small immediate-mode drawing helpers on top of [GuiGraphicsExtractor] used
 * throughout the native ClickGUI. Coordinates are floats so the widgets can be
 * laid out precisely even after GUI scaling.
 */
object GuiRender {

    val fontHeight: Int get() = mc.font.lineHeight

    fun textWidth(text: String): Int = mc.font.width(text)

    /**
     * Trims [text] with an ellipsis so its rendered width does not exceed [maxWidth].
     */
    fun trim(text: String, maxWidth: Int): String {
        if (textWidth(text) <= maxWidth) {
            return text
        }

        val ellipsis = ".."
        var result = text
        while (result.isNotEmpty() && textWidth(result + ellipsis) > maxWidth) {
            result = result.dropLast(1)
        }
        return result + ellipsis
    }
}

fun GuiGraphicsExtractor.fillRect(x: Float, y: Float, width: Float, height: Float, color: Color4b) {
    if (color.a == 0) {
        return
    }
    fill(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt(), color.argb)
}

fun GuiGraphicsExtractor.roundedRect(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
    color: Color4b
) {
    drawRoundedRect(x, y, x + width, y + height, radius, fillColor = color)
}

/**
 * Draws left-aligned text using the vanilla font renderer.
 */
fun GuiGraphicsExtractor.drawText(text: String, x: Float, y: Float, color: Int, shadow: Boolean = false) {
    text(mc.font, text.asPlainText(), x.toInt(), y.toInt(), color, shadow)
}

/**
 * Draws text vertically centered within a row of the given [rowHeight].
 */
fun GuiGraphicsExtractor.drawTextCenteredY(
    text: String,
    x: Float,
    rowTop: Float,
    rowHeight: Float,
    color: Int,
    shadow: Boolean = false
) {
    val y = rowTop + (rowHeight - mc.font.lineHeight) / 2f
    drawText(text, x, y, color, shadow)
}
