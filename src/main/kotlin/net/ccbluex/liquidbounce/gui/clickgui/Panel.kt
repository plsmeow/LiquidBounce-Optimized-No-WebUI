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

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.render.getBounds
import net.ccbluex.liquidbounce.render.withPush
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.max

/**
 * A single module row inside a [Panel]. Toggling happens on left click; the
 * settings tree expands on right click.
 */
class ModuleButton(val module: ClientModule) {

    var x = 0f
    var y = 0f
    var width = 0f
    var expanded = false

    val settings: List<Setting> by lazy {
        buildSettings(module, indent = 0, skipEnabled = true).also { list ->
            restoreSettingExpandedStates(list, "")
        }
    }

    private fun restoreSettingExpandedStates(settings: List<Setting>, parentPath: String) {
        for (s in settings) {
            val key = "${module.name}/$parentPath${s.displayName}"
            if (ClickGuiState.getSettingExpanded(key)) {
                s.setExpanded(true)
            }
            if (s.children.isNotEmpty()) {
                restoreSettingExpandedStates(s.children, "$key/")
            }
        }
    }

    val rowHeight: Float get() = ClickGuiTheme.MODULE_HEIGHT.toFloat()

    fun contentHeight(): Float = rowHeight + if (expanded) settingsHeight(settings) else 0f

    fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + rowHeight
        when {
            module.enabled -> ctx.fillRect(x, y, width, rowHeight, ClickGuiTheme.moduleActive)
            hovered -> ctx.fillRect(x, y, width, rowHeight, ClickGuiTheme.moduleHover)
        }
        val textColor = if (module.enabled) ClickGuiTheme.textOnAccent else ClickGuiTheme.textPrimary

        // Collapse/expand icon on the left: "+" when collapsed, "-" when expanded
        val icon = if (expanded) "-" else "+"
        ctx.drawTextCenteredY(icon, x + ClickGuiTheme.TEXT_PADDING, y, rowHeight, textColor)

        // Module name next to the icon
        ctx.drawTextCenteredY(module.name, x + ClickGuiTheme.TEXT_PADDING + 10f, y, rowHeight, textColor)
    }

    fun hoveredRow(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + rowHeight
}

/**
 * A draggable, scrollable window holding all modules of one [category].
 */
class Panel(val category: ModuleCategory, var x: Float, var y: Float, collapsed: Boolean = false) {

    val width = ClickGuiTheme.PANEL_WIDTH.toFloat()
    var collapsed = collapsed
        set(value) {
            field = value
            ClickGuiState.setState(category.tag, ClickGuiState.PanelState(x, y, value))
        }
    private var scroll = 0f

    /** Non-null means only modules whose name contains [searchFilter] are shown. */
    var searchFilter: String? = null

    val buttons: List<ModuleButton> =
        ModuleManager.filter { it.category == category }
            .sortedBy { it.name }
            .map(::ModuleButton)

    private fun displayedButtons(): List<ModuleButton> {
        val query = searchFilter
        if (query == null || query.isEmpty()) return buttons
        return buttons.filter { it.module.name.lowercase().contains(query) }
    }

    private val headerHeight get() = ClickGuiTheme.HEADER_HEIGHT.toFloat()

    private fun contentHeight(): Float = displayedButtons().sumOf { it.contentHeight().toDouble() }.toFloat()

    private fun maxBodyHeight(screenHeight: Int): Float = max(40f, screenHeight * 0.75f)

    private fun visibleBodyHeight(screenHeight: Int): Float =
        if (collapsed) 0f else minOf(contentHeight(), maxBodyHeight(screenHeight))

    fun totalHeight(screenHeight: Int): Float = headerHeight + visibleBodyHeight(screenHeight)

    fun render(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, screenHeight: Int) {
        clampScroll(screenHeight)

        // Header
        ctx.roundedRect(x, y, width, headerHeight, 2f, ClickGuiTheme.panelHeaderActive)
        ctx.drawTextCenteredY(category.tag, x + ClickGuiTheme.TEXT_PADDING, y, headerHeight,
            ClickGuiTheme.textOnAccent)
        val indicator = if (collapsed) "+" else "-"
        ctx.drawTextCenteredY(indicator, x + width - ClickGuiTheme.TEXT_PADDING - GuiRender.textWidth(indicator), y,
            headerHeight, ClickGuiTheme.textOnAccent)

        if (collapsed) {
            return
        }

        val bodyTop = y + headerHeight
        val bodyHeight = visibleBodyHeight(screenHeight)
        ctx.fillRect(x, bodyTop, width, bodyHeight, ClickGuiTheme.panelBackground)

        val bounds = ctx.getBounds(x, bodyTop, x + width, bodyTop + bodyHeight)
        ctx.scissorStack.withPush(bounds) {
            var yy = bodyTop - scroll
            for (button in displayedButtons()) {
                button.x = x
                button.y = yy
                button.width = width
                button.renderRow(ctx, mouseX, mouseY)
                yy += button.rowHeight
                if (button.expanded) {
                    yy = layoutSettings(button.settings, ctx, x, yy, width, mouseX, mouseY)
                }
            }
        }
    }

    private fun clampScroll(screenHeight: Int) {
        val maxScroll = max(0f, contentHeight() - maxBodyHeight(screenHeight))
        scroll = scroll.coerceIn(0f, maxScroll)
    }

    fun scroll(amount: Double) {
        scroll -= amount.toFloat() * 12f
    }

    /** @return the module button whose row is under the cursor, or null. */
    fun buttonAt(mouseX: Double, mouseY: Double): ModuleButton? =
        displayedButtons().firstOrNull { it.hoveredRow(mouseX, mouseY) }

    fun settingAt(mouseX: Double, mouseY: Double): Setting? {
        for (button in displayedButtons()) {
            if (button.expanded) {
                settingAt(button.settings, mouseX, mouseY)?.let { return it }
            }
        }
        return null
    }

    fun isInBody(mouseX: Double, mouseY: Double, screenHeight: Int): Boolean {
        if (collapsed) {
            return false
        }
        val bodyTop = y + headerHeight
        val bodyHeight = visibleBodyHeight(screenHeight)
        return mouseX >= x && mouseX <= x + width && mouseY >= bodyTop && mouseY <= bodyTop + bodyHeight
    }

    fun isInHeader(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight
}

// --- Recursive settings walker ------------------------------------------------

internal fun layoutSettings(
    list: List<Setting>,
    ctx: GuiGraphicsExtractor,
    x: Float,
    startY: Float,
    width: Float,
    mouseX: Int,
    mouseY: Int
): Float {
    var yy = startY
    for (s in list) {
        s.x = x
        s.y = yy
        s.width = width
        s.renderRow(ctx, mouseX, mouseY)
        yy += s.rowHeight
        if (s.expanded) {
            yy = layoutSettings(s.children, ctx, x, yy, width, mouseX, mouseY)
        }
    }
    return yy
}

internal fun settingsHeight(list: List<Setting>): Float {
    var total = 0f
    for (s in list) {
        total += s.rowHeight
        if (s.expanded) {
            total += settingsHeight(s.children)
        }
    }
    return total
}

internal fun settingAt(list: List<Setting>, mouseX: Double, mouseY: Double): Setting? {
    for (s in list) {
        if (s.hovered(mouseX, mouseY)) {
            return s
        }
        if (s.expanded) {
            settingAt(s.children, mouseX, mouseY)?.let { return it }
        }
    }
    return null
}
