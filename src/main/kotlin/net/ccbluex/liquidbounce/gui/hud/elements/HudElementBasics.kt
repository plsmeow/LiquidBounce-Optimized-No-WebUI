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
import net.ccbluex.liquidbounce.gui.clickgui.drawText
import net.ccbluex.liquidbounce.gui.clickgui.fillRect
import net.ccbluex.liquidbounce.gui.hud.HudConfig
import net.ccbluex.liquidbounce.gui.hud.HudElement
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.isSingleplayer
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.armorItems
import net.ccbluex.liquidbounce.utils.entity.ping
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Vector2f

/** XYZ coordinates of the player. */
object HudElementCoordinates : HudElement("coordinates", "Coordinates") {

    private val TEXT_PADDING = 4f

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            position.set(Vector2f(4f, 60f))
        }
        alignment.set(Alignment.TOP_LEFT)
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        val player = mc.player ?: return
        val s = scale.get()
        val pos = player.position()
        val text = "XYZ: ${"%.1f".format(pos.x)} ${"%.1f".format(pos.y)} ${"%.1f".format(pos.z)}"
        val textWidth = mc.font.width(text).toFloat()
        val width = textWidth + TEXT_PADDING
        val height = mc.font.lineHeight + TEXT_PADDING

        lastBaseWidth = width
        lastBaseHeight = height

        val (offX, offY) = getOffset()
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)
            val textX = textAlignX(textWidth, width)
            context.drawText(text, textX, 2f, HudConfig.textColor.get().argb, shadow = true)
        }
    }
}

/** FPS counter. */
object HudElementFps : HudElement("fps", "FPS") {

    private val TEXT_PADDING = 4f

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            position.set(Vector2f(4f, 100f))
        }
        alignment.set(Alignment.TOP_LEFT)
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        val s = scale.get()
        val fps = mc.fps
        val text = "FPS: $fps"
        val textWidth = mc.font.width(text).toFloat()
        val width = textWidth + TEXT_PADDING
        val height = mc.font.lineHeight + TEXT_PADDING

        lastBaseWidth = width
        lastBaseHeight = height

        val (offX, offY) = getOffset()
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)
            val textX = textAlignX(textWidth, width)
            context.drawText(text, textX, 2f, HudConfig.textColor.get().argb, shadow = true)
        }
    }
}

/** Network ping (ms) to the current server. */
object HudElementPing : HudElement("ping", "Ping") {

    private val TEXT_PADDING = 4f

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            position.set(Vector2f(80f, 100f))
        }
        alignment.set(Alignment.TOP_LEFT)
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        val s = scale.get()
        val ping = mc.player?.ping ?: 0
        val text = "Ping: ${ping}ms"
        val textWidth = mc.font.width(text).toFloat()
        val width = textWidth + TEXT_PADDING
        val height = mc.font.lineHeight + TEXT_PADDING

        lastBaseWidth = width
        lastBaseHeight = height

        val (offX, offY) = getOffset()
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)
            val textX = textAlignX(textWidth, width)
            context.drawText(text, textX, 2f, HudConfig.textColor.get().argb, shadow = true)
        }
    }
}

/** Server IP (or "Singleplayer"). */
object HudElementServerIp : HudElement("serverip", "Server IP") {

    private val TEXT_PADDING = 4f

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            position.set(Vector2f(4f, 140f))
        }
        alignment.set(Alignment.TOP_LEFT)
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        val s = scale.get()
        val text = if (mc.isSingleplayer) {
            "Singleplayer"
        } else {
            mc.currentServer?.ip ?: "N/A"
        }
        val display = "Server: $text"
        val textWidth = mc.font.width(display).toFloat()
        val width = textWidth + TEXT_PADDING
        val height = mc.font.lineHeight + TEXT_PADDING

        lastBaseWidth = width
        lastBaseHeight = height

        val (offX, offY) = getOffset()
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)
            val textX = textAlignX(textWidth, width)
            context.drawText(display, textX, 2f, HudConfig.textColor.get().argb, shadow = true)
        }
    }
}

/** Armor display (vanilla-style armor icons). */
object HudElementArmor : HudElement("armor", "Armor") {

    private val ICON_SIZE = 16
    private val GAP = 1
    private val BG_PADDING = 2
    private val armorBackground = Color4b(0x0A, 0x0A, 0x0F, 0xB4)

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            position.set(Vector2f(4f, (mc.window.guiScaledHeight - 22).toFloat()))
        }
        alignment.set(Alignment.TOP_LEFT)
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        val player = mc.player ?: return
        val s = scale.get()

        val armorSlots = arrayOf(
            player.armorItems[3],
            player.armorItems[2],
            player.armorItems[1],
            player.armorItems[0],
        )
        val nonEmpty = armorSlots.filter { !it.isEmpty }
        if (nonEmpty.isEmpty()) {
            lastBaseWidth = 0f
            lastBaseHeight = 0f
            return
        }

        val totalWidth = (nonEmpty.size * (ICON_SIZE + GAP) - GAP + BG_PADDING * 2).toFloat()
        val totalHeight = (ICON_SIZE + BG_PADDING * 2).toFloat()
        lastBaseWidth = totalWidth
        lastBaseHeight = totalHeight

        val (offX, offY) = getOffset()
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)
            context.fillRect(0f, 0f, totalWidth, totalHeight, armorBackground)
            var x = BG_PADDING.toFloat()
            for (stack in nonEmpty) {
                context.item(stack, x.toInt(), BG_PADDING)
                x += ICON_SIZE + GAP
            }
        }
    }
}

/** Active status effects (potions). */
object HudElementEffects : HudElement("effects", "Effects") {

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            position.set(Vector2f(4f, 160f))
        }
        alignment.set(Alignment.TOP_LEFT)
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        val player = mc.player ?: return
        val s = scale.get()
        val effects = player.activeEffects
        if (effects.isEmpty()) {
            return
        }
        val lines = effects.map { effect ->
            "${effect.effect.value().displayName.string} " +
                "${effect.amplifier + 1} " +
                "${effect.duration / 20}s"
        }
        val maxTextWidth = lines.maxOfOrNull { mc.font.width(it) } ?: 0
        val width = maxTextWidth + 4f
        val height = (lines.size * (mc.font.lineHeight + 1)).toFloat()

        lastBaseWidth = width
        lastBaseHeight = height

        val (offX, offY) = getOffset()
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)
            var y = 0f
            for (line in lines) {
                val textWidth = mc.font.width(line).toFloat()
                val textX = textAlignX(textWidth, width)
                context.drawText(line, textX, y, HudConfig.textColor.get().argb, shadow = true)
                y += mc.font.lineHeight + 1f
            }
        }
    }
}

