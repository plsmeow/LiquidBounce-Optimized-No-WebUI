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
import net.ccbluex.liquidbounce.gui.clickgui.roundedRect
import net.ccbluex.liquidbounce.gui.hud.HudConfig
import net.ccbluex.liquidbounce.gui.hud.HudElement
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.EntityHitResult
import org.joml.Vector2f

/**
 * TargetHUD: shows the entity the player is currently aiming at (the crosshair
 * target). Renders the entity's name, health bar and distance. Position is
 * saved through [HudElement.position].
 */
object HudElementTargetHud : HudElement("targethud", "TargetHUD") {

    private const val HEIGHT = 36f
    private const val WIDTH = 140f
    private val targetBackground = Color4b(0x0A, 0x0A, 0x0F, 0xB4)

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            position.set(Vector2f(screenWidth / 2f, screenHeight / 2f + 30f))
        }
        alignment.set(Alignment.TOP_CENTER)
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        val target = currentTarget()

        val s = scale.get()
        val width = WIDTH * s
        val height = HEIGHT * s

        // Always report base size so the HUD editor can show a placeholder box
        lastBaseWidth = WIDTH
        lastBaseHeight = HEIGHT

        if (target == null) {
            return
        }

        val (offX, offY) = getOffset()
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)

            // Background box
            context.roundedRect(0f, 0f, WIDTH, HEIGHT, 3f, targetBackground)
            // Border
            context.drawRoundedRect(0f, 0f, WIDTH, HEIGHT, 3f, outlineColor = Color4b(0, 0, 0, 100))

            // Name
            val name = target.name.string
            context.drawText(name, 4f, 4f, HudConfig.textColor.get().argb, shadow = true)

            // Health bar
            val maxHealth = target.maxHealth.coerceAtLeast(1f)
            val hp = (target.health / maxHealth).coerceIn(0f, 1f)
            val hpColor = when {
                hp > 0.5f -> Color4b(89, 255, 89, 255)
                hp > 0.25f -> Color4b(255, 200, 89, 255)
                else -> Color4b(255, 89, 89, 255)
            }
            val barY = 18f
            val barH = 6f
            context.fillRect(4f, barY, WIDTH - 8f, barH, Color4b(0, 0, 0, 100))
            context.fillRect(4f, barY, (WIDTH - 8f) * hp, barH, hpColor)
            val hpText = "${"%.1f".format(target.health)} / ${"%.1f".format(maxHealth)} HP"
            context.drawText(hpText, 4f, barY + barH + 2f, HudConfig.secondaryTextColor.get().argb, shadow = true)

            // Distance
            val dist = mc.player?.let { it.distanceTo(target) } ?: 0f
            val distText = "${"%.1f".format(dist)}m"
            val tw = mc.font.width(distText)
            context.drawText(distText, WIDTH - tw - 4f, 4f, HudConfig.secondaryTextColor.get().argb, shadow = true)
        }
    }

    private fun currentTarget(): LivingEntity? {
        val hitResult = mc.hitResult
        val entity = (hitResult as? EntityHitResult)?.entity ?: return null
        return entity as? LivingEntity
    }
}
