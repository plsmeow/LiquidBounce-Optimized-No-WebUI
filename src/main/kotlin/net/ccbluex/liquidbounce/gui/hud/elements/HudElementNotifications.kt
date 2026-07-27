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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.gui.clickgui.drawText
import net.ccbluex.liquidbounce.gui.clickgui.fillRect
import net.ccbluex.liquidbounce.gui.clickgui.roundedRect
import net.ccbluex.liquidbounce.gui.hud.HudConfig
import net.ccbluex.liquidbounce.gui.hud.HudElement
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Vector2f
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Toast-style notification queue. Listens to [NotificationEvent]s and renders
 * them through the [HudElementNotificationList] HUD element. Notifications
 * fade out after a few seconds.
 */
data class NotificationEntry(
    val title: String,
    val message: String,
    val severity: NotificationEvent.Severity,
    val createdAtMs: Long = System.currentTimeMillis(),
) {
    val lifetimeMs: Long get() = 3000L
    fun isExpired(now: Long): Boolean = now - createdAtMs > lifetimeMs
    fun alpha(now: Long): Int {
        val remaining = lifetimeMs - (now - createdAtMs)
        if (remaining > 500L) return 255
        // Fade out over the last 500ms.
        return (remaining * 255 / 500L).toInt().coerceIn(0, 255)
    }
}

object NotificationManager : EventListener {

    val entries = CopyOnWriteArrayList<NotificationEntry>()

    @Suppress("unused")
    private val handler = handler<NotificationEvent> { event ->
        entries.add(
            NotificationEntry(
                event.title,
                event.message,
                event.severity,
            )
        )
        // Cap the list to avoid memory leaks.
        while (entries.size > 20) {
            entries.removeAt(0)
        }
    }

    fun push(title: String, message: String, severity: NotificationEvent.Severity) {
        entries.add(NotificationEntry(title, message, severity))
    }

    fun tick() {
        val now = System.currentTimeMillis()
        entries.removeAll { it.isExpired(now) }
    }
}

/**
 * Notification list HUD: shows all active notifications as a vertical stack
 * on the right side of the screen. Tied to [NotificationManager].
 */
object HudElementNotificationList : HudElement("notifications", "Notifications") {

    override fun onInitialize(screenWidth: Int, screenHeight: Int) {
        if (position.get().x() == 8f && position.get().y() == 8f) {
            position.set(Vector2f((screenWidth - 200).toFloat(), 4f))
        }
        alignment = Alignment.TOP_RIGHT
    }

    override fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent) {
        NotificationManager.tick()
        val list = NotificationManager.entries

        val s = scale.get()
        val now = System.currentTimeMillis()

        // Always report a placeholder size so the HUD editor shows a draggable box
        val placeholderWidth = 120f
        val placeholderHeight = 32f

        if (list.isEmpty()) {
            lastBaseWidth = placeholderWidth
            lastBaseHeight = placeholderHeight
            return
        }

        val maxWidth = list.maxOfOrNull { entry ->
            mc.font.width(entry.title) + mc.font.width(entry.message) + 24
        } ?: 0
        val itemHeight = mc.font.lineHeight * 2 + 8
        val totalWidth = (maxWidth + 8).toFloat()
        val totalHeight = (list.size * itemHeight + 4).toFloat()

        lastBaseWidth = totalWidth
        lastBaseHeight = totalHeight

        val (offX, offY) = getOffset()
        context.pose().withPush {
            translate(renderPosition.x + offX, renderPosition.y + offY)
            scale(s, s)
            var y = 0f
            for (entry in list) {
                val a = entry.alpha(now)
                val bg = HudConfig.backgroundColor.get().alpha(180).alpha(a)
                val accent = accentFor(entry.severity).alpha(a)
                val text = HudConfig.textColor.get().alpha(a)
                val sub = HudConfig.secondaryTextColor.get().alpha(a)

                context.roundedRect(0f, y, totalWidth, itemHeight.toFloat(), 3f, bg)
                context.fillRect(0f, y, 2f, itemHeight.toFloat(), accent)
                context.drawText(entry.title, 6f, y + 2f, text.argb, shadow = true)
                context.drawText(entry.message, 6f, y + mc.font.lineHeight + 3f, sub.argb, shadow = true)
                y += itemHeight + 1
            }
        }
    }

    private fun accentFor(severity: NotificationEvent.Severity): Color4b = when (severity) {
        NotificationEvent.Severity.INFO -> HudConfig.accentColor.get()
        NotificationEvent.Severity.SUCCESS -> Color4b(89, 255, 89, 255)
        NotificationEvent.Severity.ERROR -> Color4b(255, 89, 89, 255)
        NotificationEvent.Severity.ENABLED -> Color4b(89, 255, 89, 255)
        NotificationEvent.Severity.DISABLED -> Color4b(255, 89, 89, 255)
    }
}
