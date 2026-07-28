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

import net.ccbluex.liquidbounce.features.global.GlobalManager
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.gui.hud.HudConfig
import net.ccbluex.liquidbounce.gui.hud.HudElement
import net.ccbluex.liquidbounce.gui.hud.HudElementRegistry
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.joml.Vector2f
import kotlin.math.abs

/**
 * Native, Meteor-Client-style ClickGUI with 3 horizontal tabs:
 * **Modules** – per-category panel windows (the original meteor-style layout).
 * **HudEditor** – drag-and-drop HUD element editor with live preview.
 * **Settings** – global client settings (language, commands, targets, etc.).
 */
class ClickGuiScreen : Screen(PlainText.EMPTY) {

    enum class Tab(val label: String) {
        MODULES("Modules"),
        HUD_EDITOR("HudEditor"),
        SETTINGS("Settings")
    }

    // --- Tab state --------------------------------------------------------
    private var activeTab = Tab.MODULES

    // --- Modules tab state ------------------------------------------------
    private val panels = ArrayList<Panel>()
    private var draggingPanel: Panel? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var pressX = 0.0
    private var pressY = 0.0
    private var panelMoved = false

    private var pressedSetting: Setting? = null
    private var focusedText: TextSetting? = null
    private var bindingSetting: BindSetting? = null

    /** True while a text field is focused, so keybinds are suppressed elsewhere. */
    val isCapturingText: Boolean get() = focusedText != null

    /** Ctrl+F search state for modules tab. */
    private var searchQuery = ""
    private var searchFocused = false

    // --- HudEditor tab state ----------------------------------------------
    private var draggingElement: HudElement? = null
    private var hudDragOffsetX = 0f
    private var hudDragOffsetY = 0f
    private var hudPressX = 0.0
    private var hudPressY = 0.0
    private var hudMoved = false

    // --- HudEditor alignment context menu ---------------------------------
    private var contextElement: HudElement? = null
    private var contextMenuX = 0f
    private var contextMenuY = 0f

    // --- Settings tab state -----------------------------------------------
    private val settingsGroups = LinkedHashMap<String, List<Setting>>()
    private var settingsScroll = 0f
    private var pressedSettingsSetting: Setting? = null

    // --- Tab bar layout ---------------------------------------------------
    private val tabBarHeight = 20
    private val tabButtonWidth = 80

    // --- Init -------------------------------------------------------------

    override fun init() {
        ClickGuiState.load()
        if (panels.isEmpty()) {
            var px = 6f
            for (category in ModuleCategories.entries) {
                val saved = ClickGuiState.getState(category.tag)
                if (saved != null) {
                    panels.add(Panel(category, saved.x, saved.y, saved.collapsed))
                } else {
                    panels.add(Panel(category, px, (tabBarHeight + 4).toFloat()))
                }
                px += ClickGuiTheme.PANEL_WIDTH + 6f
            }
        }
        // Restore module expanded states
        for (panel in panels) {
            for (button in panel.buttons) {
                button.expanded = ClickGuiState.getModuleExpanded(button.module.name)
            }
        }

        // Initialize HUD elements so they have default sizes for hit-testing
        for (element in HudElementRegistry.getAll()) {
            element.renderPosition = Vector2f(element.position.get())
            element.setEditorDefaultSize(80f, 16f)
        }
        buildSettingsGroups()
    }

    private fun buildSettingsGroups() {
        settingsGroups.clear()

        // Build settings from GlobalManager sub-trees (language, commands, targets, etc.)
        for (child in GlobalManager.containedValues) {
            if (child is net.ccbluex.liquidbounce.config.types.group.ValueGroup) {
                val name = child.name
                val settings = buildSettingsFromGroup(child, 0)
                if (settings.isNotEmpty()) {
                    settingsGroups[name] = settings
                }
            }
        }

        // HUD Theme accent color
        settingsGroups["HUD Theme"] = listOf(
            HudConfig.accentColor
        ).mapNotNull { SettingFactory.create(it, 0) }

        // HUD element toggles
        settingsGroups["HUD Elements"] = HudElementRegistry.getAll().mapNotNull { element ->
            SettingFactory.create(element.enabled, 0)
        }
    }

    private fun buildSettingsFromGroup(group: net.ccbluex.liquidbounce.config.types.group.ValueGroup, depth: Int): List<Setting> {
        val result = mutableListOf<Setting>()
        for (value in group.containedValues) {
            if (value is net.ccbluex.liquidbounce.config.types.group.ValueGroup) {
                if (depth == 0) {
                    // Top-level sub-group: recurse into it with its name as header
                    val sub = buildSettingsFromGroup(value, depth + 1)
                    if (sub.isNotEmpty()) {
                        // Add a label separator for the sub-group
                        result.addAll(sub)
                    }
                } else {
                    result.addAll(buildSettingsFromGroup(value, depth + 1))
                }
            } else {
                val setting = SettingFactory.create(value, depth)
                if (setting != null) {
                    result.add(setting)
                }
            }
        }
        return result
    }

    // --- Rendering --------------------------------------------------------

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)
        context.fill(0, 0, width, height, 0x60101014)

        renderTabBar(context, mouseX, mouseY)

        when (activeTab) {
            Tab.MODULES -> renderModulesTab(context, mouseX, mouseY)
            Tab.HUD_EDITOR -> renderHudEditorTab(context, mouseX, mouseY, delta)
            Tab.SETTINGS -> renderSettingsTab(context, mouseX, mouseY)
        }
    }

    private fun renderTabBar(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val totalWidth = Tab.entries.size * tabButtonWidth
        val barX = (width - totalWidth) / 2f
        ctx.fillRect(barX - 4f, 0f, totalWidth + 8f, tabBarHeight.toFloat(), ClickGuiTheme.panelHeader)
        var tx = (width - totalWidth) / 2f

        for (tab in Tab.entries) {
            val isHovered = mouseX >= tx && mouseX <= tx + tabButtonWidth &&
                mouseY >= 0 && mouseY <= tabBarHeight
            val isActive = tab == activeTab

            if (isActive) {
                ctx.fillRect(tx, 0f, tabButtonWidth.toFloat(), tabBarHeight.toFloat(), ClickGuiTheme.panelHeaderActive)
            } else if (isHovered) {
                ctx.fillRect(tx, 0f, tabButtonWidth.toFloat(), tabBarHeight.toFloat(), ClickGuiTheme.moduleHover)
            }

            val textColor = if (isActive) ClickGuiTheme.textOnAccent else {
                if (isHovered) ClickGuiTheme.textActive else ClickGuiTheme.textPrimary
            }
            val textW = GuiRender.textWidth(tab.label)
            ctx.drawTextCenteredY(tab.label,
                tx + (tabButtonWidth - textW) / 2f,
                0f, tabBarHeight.toFloat(), textColor)

            tx += tabButtonWidth
        }
    }

    // --- Modules tab ------------------------------------------------------

    private fun renderModulesTab(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        // Search bar at top
        renderSearchBar(context, mouseX, mouseY)

        for (panel in panels) {
            panel.render(context, mouseX, mouseY, height)
        }
    }

    private fun renderSearchBar(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val barY = tabBarHeight + 2f
        val barWidth = 180f
        val barHeight = 14f
        val barX = (width - barWidth) / 2f

        val bgColor = if (searchFocused) ClickGuiTheme.panelHeaderActive else ClickGuiTheme.panelBackground
        ctx.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, 3f,
            fillColor = bgColor, outlineColor = ClickGuiTheme.panelOutline)

        val searchText = if (searchQuery.isEmpty() && !searchFocused) "Search... (Ctrl+F)" else searchQuery
        val textColor = if (searchQuery.isEmpty() && !searchFocused) ClickGuiTheme.textSecondary else ClickGuiTheme.textPrimary
        ctx.drawText(searchText, barX + 4f, barY + 3f, textColor, shadow = false)

        // Filter panels based on search query
        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()
            for (panel in panels) {
                panel.searchFilter = query
            }
        } else {
            for (panel in panels) {
                panel.searchFilter = null
            }
        }
    }

    // --- HudEditor tab ----------------------------------------------------

    private fun renderHudEditorTab(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val elements = HudElementRegistry.getAll()

        // Draw grid background
        val gridSpacing = 20
        val gridColor = Color4b(0xFF, 0xFF, 0xFF, 0x10)
        for (gx in 0..width step gridSpacing) {
            context.fillRect(gx.toFloat(), tabBarHeight.toFloat(), 1f, height.toFloat(), gridColor)
        }
        for (gy in tabBarHeight..height step gridSpacing) {
            context.fillRect(0f, gy.toFloat(), width.toFloat(), 1f, gridColor)
        }

        // Render enabled elements first so lastBaseWidth/Height are updated
        val event = net.ccbluex.liquidbounce.event.events.OverlayRenderEvent(context, delta)
        for (element in elements) {
            if (element.enabled.get()) {
                runCatching { element.render(context, event) }
            }
        }

        // Draw ALL elements (even disabled ones, dimmed)
        for (element in elements) {
            val enabled = element.enabled.get()
            val (w, h) = element.getScaledSize()
            if (w <= 0f || h <= 0f) continue
            val anchor = element.getAnchor()
            val ex = anchor.x
            val ey = anchor.y

            val outlineAlpha = if (enabled) 255 else 100
            val labelAlpha = if (enabled) 255 else 80

            // Outline box
            context.drawRoundedRect(ex, ey, ex + w, ey + h, 3f,
                fillColor = if (element === draggingElement) ClickGuiTheme.accent.alpha(60)
                    else null,
                outlineColor = if (element === draggingElement) ClickGuiTheme.accent
                    else ClickGuiTheme.panelOutline.alpha(outlineAlpha),
                outlineWidth = 1.5f)

            // Label
            val label = element.displayName + if (!enabled) " (disabled)" else ""
            context.drawTextCenteredY(label, ex + 3f, ey, h.coerceAtMost(14f),
                Color4b(ClickGuiTheme.textPrimary).alpha(labelAlpha).argb)

            // Status indicator for disabled elements
            if (!enabled) {
                val statusX = ex + w - 14f
                context.drawTextCenteredY("OFF", statusX, ey, h.coerceAtMost(14f),
                    Color4b(255, 80, 80, 160).argb)
            }
        }

        // Instruction label
        context.drawTextCenteredY("[Drag elements to reposition / Right-click for alignment / Click HUD Elements in Settings to toggle]",
            (width / 2f) - 160f, tabBarHeight + 4f, 12f, ClickGuiTheme.textSecondary)

        // Context menu for alignment
        renderContextMenu(context, mouseX, mouseY)
    }

    // --- Settings tab -----------------------------------------------------

    private fun renderSettingsTab(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val panelWidth = 350f
        val panelX = (width - panelWidth) / 2f
        val startY = tabBarHeight + 6f - settingsScroll
        var yy = startY

        for ((groupName, settings) in settingsGroups) {
            if (settings.isEmpty()) continue

            // Section header
            context.fillRect(panelX, yy, panelWidth, 16f, ClickGuiTheme.panelHeader)
            context.drawTextCenteredY(groupName, panelX + 4f, yy, 16f, ClickGuiTheme.accent.argb)
            yy += 18f

            yy = layoutSettings(settings, context, panelX + 4f, yy, panelWidth - 8f, mouseX, mouseY)
            yy += 6f
        }

        // Clamp scroll
        val maxContent = yy - startY + settingsScroll - height + tabBarHeight + 20
        if (maxContent > 0) {
            settingsScroll = settingsScroll.coerceIn(0f, maxContent)
        } else {
            settingsScroll = 0f
        }
    }

    // --- Mouse ------------------------------------------------------------

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseX = click.x
        val mouseY = click.y
        val button = click.button()

        // Context menu click (highest priority)
        if (contextElement != null) {
            if (handleContextMenuClick(mouseX, mouseY, button)) return true
            contextElement = null
            return true
        }

        // HUD editor element click (elements may overlap tab bar)
        if (activeTab == Tab.HUD_EDITOR && handleHudEditorClick(mouseX, mouseY, button)) return true

        // Tab bar click
        if (mouseY <= tabBarHeight) {
            val totalWidth = Tab.entries.size * tabButtonWidth
            var tx = (width - totalWidth) / 2f
            for (tab in Tab.entries) {
                if (mouseX >= tx && mouseX <= tx + tabButtonWidth) {
                    if (tab != activeTab) {
                        activeTab = tab
                        if (tab == Tab.SETTINGS) {
                            buildSettingsGroups()
                        }
                    }
                    return true
                }
                tx += tabButtonWidth
            }
            return true
        }

        when (activeTab) {
            Tab.MODULES -> return handleModulesClick(mouseX, mouseY, button)
            Tab.HUD_EDITOR -> return handleHudEditorClick(mouseX, mouseY, button)
            Tab.SETTINGS -> return handleSettingsClick(mouseX, mouseY, button)
        }
        return super.mouseClicked(click, doubled)
    }

    private fun handleModulesClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Check search bar click first
        val barY = tabBarHeight + 2.0
        val barWidth = 180.0
        val barHeight = 14.0
        val barX = (width - barWidth) / 2.0
        if (mouseX in barX..barX + barWidth && mouseY in barY..barY + barHeight) {
            searchFocused = true
            return true
        } else {
            searchFocused = false
        }

        for (panel in panels.asReversed()) {
            if (panel.isInHeader(mouseX, mouseY)) {
                bringToFront(panel)
                startHeaderDrag(panel, mouseX, mouseY)
                clearFocus(null)
                return true
            }
            if (panel.isInBody(mouseX, mouseY, height)) {
                bringToFront(panel)
                handleBodyClick(panel, mouseX, mouseY, button)
                return true
            }
        }
        clearFocus(null)
        return false
    }

    private fun handleBodyClick(panel: Panel, mouseX: Double, mouseY: Double, button: Int) {
        val setting = panel.settingAt(mouseX, mouseY)
        if (setting != null) {
            clearFocus(setting)
            pressedSetting?.onRelease()
            setting.mouseClicked(mouseX, mouseY, button)
            pressedSetting = setting
            (setting as? TextSetting)?.let { focusedText = it }
            (setting as? BindSetting)?.let { bindingSetting = it }
            return
        }

        val moduleButton = panel.buttonAt(mouseX, mouseY)
        if (moduleButton != null) {
            clearFocus(null)
            when (button) {
                1 -> {
                    moduleButton.expanded = !moduleButton.expanded
                    ClickGuiState.setModuleExpanded(moduleButton.module.name, moduleButton.expanded)
                }
                else -> moduleButton.module.enabled = !moduleButton.module.enabled
            }
        }
    }

    private fun handleHudEditorClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Right-click: show alignment context menu for the element under cursor
        if (button == 1) {
            val elements = HudElementRegistry.getAll().asReversed()
            for (element in elements) {
                val (w, h) = element.getScaledSize()
                if (w <= 0f || h <= 0f) continue
                val anchor = element.getAnchor()
                val ex = anchor.x
                val ey = anchor.y
                if (mouseX.toFloat() in ex..ex + w && mouseY.toFloat() in ey..ey + h) {
                    contextElement = element
                    contextMenuX = mouseX.toFloat()
                    contextMenuY = mouseY.toFloat()
                    return true
                }
            }
            return false
        }
        // Only left mouse button starts a drag
        if (button != 0) return false
        // Check all elements, including disabled ones (use getAnchor for proper hit-testing)
        val elements = HudElementRegistry.getAll().asReversed()
        for (element in elements) {
            val (w, h) = element.getScaledSize()
            if (w <= 0f || h <= 0f) continue
            val anchor = element.getAnchor()
            val ex = anchor.x
            val ey = anchor.y
            if (mouseX.toFloat() in ex..ex + w && mouseY.toFloat() in ey..ey + h) {
                draggingElement = element
                hudDragOffsetX = (mouseX - element.renderPosition.x).toFloat()
                hudDragOffsetY = (mouseY - element.renderPosition.y).toFloat()
                hudPressX = mouseX
                hudPressY = mouseY
                hudMoved = false
                return true
            }
        }
        return false
    }

    // --- Alignment context menu -------------------------------------------

    private fun renderContextMenu(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val element = contextElement ?: return

        val panelW = 150f
        val rowH = 14f
        val padding = 4f
        val btnW = 40f
        val headerH = 16f
        val totalH = headerH + padding + rowH + padding + rowH + padding

        val px = contextMenuX.coerceIn(0f, (width - panelW).coerceAtLeast(0f))
        val py = contextMenuY.coerceIn(0f, (height - totalH).coerceAtLeast(0f))

        // Background
        context.drawRoundedRect(px, py, px + panelW, py + totalH, 3f,
            fillColor = ClickGuiTheme.panelBackground,
            outlineColor = ClickGuiTheme.panelOutline,
            outlineWidth = 1f)

        // Header
        context.fillRect(px, py, panelW, headerH, ClickGuiTheme.panelHeader)
        context.drawTextCenteredY(element.displayName, px + 4f, py, headerH, ClickGuiTheme.textPrimary)

        var yy = py + headerH + padding

        val alignment = element.alignment.get()
        val h = alignment.horizontalComponent

        // Horizontal row: L C R
        context.drawTextCenteredY("H:", px + 4f, yy, rowH, ClickGuiTheme.textSecondary)
        var bx = px + 20f
        for (horiz in HudElement.Alignment.Horizontal.entries) {
            val selected = horiz == h
            val bgColor = if (selected) ClickGuiTheme.accent else ClickGuiTheme.settingBackground
            context.drawRoundedRect(bx, yy, bx + btnW, yy + rowH, 2f,
                fillColor = bgColor,
                outlineColor = ClickGuiTheme.panelOutline,
                outlineWidth = 0.5f)
            val textColor = if (selected) ClickGuiTheme.textOnAccent else ClickGuiTheme.textPrimary
            val label = when (horiz) {
                HudElement.Alignment.Horizontal.LEFT -> "L"
                HudElement.Alignment.Horizontal.CENTER -> "C"
                HudElement.Alignment.Horizontal.RIGHT -> "R"
            }
            context.drawTextCenteredY(label, bx + (btnW - GuiRender.textWidth(label)) / 2f, yy, rowH, textColor)
            bx += btnW + 2f
        }
        yy += rowH + padding

        // Vertical row: T C B
        val v = alignment.verticalComponent
        context.drawTextCenteredY("V:", px + 4f, yy, rowH, ClickGuiTheme.textSecondary)
        bx = px + 20f
        for (vert in HudElement.Alignment.Vertical.entries) {
            val selected = vert == v
            val bgColor = if (selected) ClickGuiTheme.accent else ClickGuiTheme.settingBackground
            context.drawRoundedRect(bx, yy, bx + btnW, yy + rowH, 2f,
                fillColor = bgColor,
                outlineColor = ClickGuiTheme.panelOutline,
                outlineWidth = 0.5f)
            val textColor = if (selected) ClickGuiTheme.textOnAccent else ClickGuiTheme.textPrimary
            val label = when (vert) {
                HudElement.Alignment.Vertical.TOP -> "T"
                HudElement.Alignment.Vertical.CENTER -> "C"
                HudElement.Alignment.Vertical.BOTTOM -> "B"
            }
            context.drawTextCenteredY(label, bx + (btnW - GuiRender.textWidth(label)) / 2f, yy, rowH, textColor)
            bx += btnW + 2f
        }
    }

    private fun handleContextMenuClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val element = contextElement ?: return false
        if (button != 0) return false

        val panelW = 150f
        val rowH = 14f
        val padding = 4f
        val btnW = 40f
        val headerH = 16f
        val totalH = headerH + padding + rowH + padding + rowH + padding

        val px = contextMenuX.coerceIn(0f, (width - panelW).coerceAtLeast(0f))
        val py = contextMenuY.coerceIn(0f, (height - totalH).coerceAtLeast(0f))

        val mx = mouseX.toFloat()
        val my = mouseY.toFloat()

        // Check if click is within the panel
        if (mx < px || mx > px + panelW || my < py || my > py + totalH) {
            return false
        }

        var yy = py + headerH + padding

        // Horizontal buttons
        var bx = px + 20f
        for (horiz in HudElement.Alignment.Horizontal.entries) {
            if (mx in bx..bx + btnW && my in yy..yy + rowH) {
                val newAlignment = HudElement.Alignment.fromComponents(element.alignment.get().verticalComponent, horiz)
                element.alignment.set(newAlignment)
                contextElement = null
                return true
            }
            bx += btnW + 2f
        }
        yy += rowH + padding

        // Vertical buttons
        bx = px + 20f
        for (vert in HudElement.Alignment.Vertical.entries) {
            if (mx in bx..bx + btnW && my in yy..yy + rowH) {
                val newAlignment = HudElement.Alignment.fromComponents(vert, element.alignment.get().horizontalComponent)
                element.alignment.set(newAlignment)
                contextElement = null
                return true
            }
            bx += btnW + 2f
        }

        return false
    }

    private fun handleSettingsClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for ((_, settings) in settingsGroups) {
            val setting = settingAt(settings, mouseX, mouseY)
            if (setting != null) {
                pressedSettingsSetting?.onRelease()
                setting.mouseClicked(mouseX, mouseY, button)
                pressedSettingsSetting = setting
                (setting as? TextSetting)?.let { focusedText = it }
                (setting as? BindSetting)?.let { bindingSetting = it }
                return true
            }
        }
        return false
    }

    private fun startHeaderDrag(panel: Panel, mouseX: Double, mouseY: Double) {
        draggingPanel = panel
        dragOffsetX = (mouseX - panel.x).toFloat()
        dragOffsetY = (mouseY - panel.y).toFloat()
        pressX = mouseX
        pressY = mouseY
        panelMoved = false
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        val mouseX = click.x
        val mouseY = click.y

        // Modules tab panel drag
        val panel = draggingPanel
        if (panel != null) {
            if (abs(mouseX - pressX) > 3 || abs(mouseY - pressY) > 3) {
                panelMoved = true
            }
            panel.x = (mouseX - dragOffsetX).toFloat()
            panel.y = (mouseY - dragOffsetY).toFloat()
            return true
        }

        // HudEditor element drag
        val element = draggingElement
        if (element != null) {
            if (abs(mouseX - hudPressX) > 3 || abs(mouseY - hudPressY) > 3) {
                hudMoved = true
            }
            element.renderPosition = Vector2f(
                (mouseX - hudDragOffsetX).toFloat(),
                (mouseY - hudDragOffsetY).toFloat()
            )
            return true
        }

        pressedSetting?.onDrag(mouseX, mouseY)
        pressedSettingsSetting?.onDrag(mouseX, mouseY)
        return true
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        val panel = draggingPanel
        if (panel != null) {
            if (!panelMoved) {
                panel.collapsed = !panel.collapsed
            }
            ClickGuiState.setState(panel.category.tag, ClickGuiState.PanelState(panel.x, panel.y, panel.collapsed))
            draggingPanel = null
        }

        val element = draggingElement
        if (element != null) {
            if (hudMoved) {
                // Commit drag position back to persistent storage
                element.commitRenderPosition()
            }
            draggingElement = null
        }

        pressedSetting?.onRelease()
        pressedSetting = null
        pressedSettingsSetting?.onRelease()
        pressedSettingsSetting = null
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        when (activeTab) {
            Tab.MODULES -> {
                for (panel in panels.asReversed()) {
                    if (panel.isInBody(mouseX, mouseY, height)) {
                        panel.scroll(verticalAmount)
                        return true
                    }
                }
            }
            Tab.SETTINGS -> {
                settingsScroll -= verticalAmount.toFloat() * 12f
                settingsScroll = settingsScroll.coerceAtLeast(0f)
                return true
            }
            else -> {}
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    // --- Keyboard ---------------------------------------------------------

    override fun keyPressed(input: KeyEvent): Boolean {
        // Ctrl+F: toggle search in modules tab
        if (activeTab == Tab.MODULES) {
            val ctrlHeld = input.modifiers() and 0x3 != 0 // GLFW mod bitmask
            if (ctrlHeld && input.key == 'f'.code) {
                searchFocused = !searchFocused
                if (!searchFocused) {
                    searchQuery = ""
                }
                return true
            }
        }

        bindingSetting?.let { bind ->
            bind.onKeyPressed(input.key, input.scancode(), input.modifiers())
            if (!bind.listening) {
                bindingSetting = null
            }
            return true
        }

        if (searchFocused) {
            when (input.key) {
                256 -> { // ESC
                    searchFocused = false
                    searchQuery = ""
                    return true
                }
                259 -> { // BACKSPACE
                    if (searchQuery.isNotEmpty()) {
                        searchQuery = searchQuery.dropLast(1)
                    }
                    return true
                }
            }
            return true
        }

        focusedText?.let { text ->
            text.onKeyPressed(input.key, input.scancode(), input.modifiers())
            return true
        }

        pressedSetting?.let { s ->
            if (s.onKeyPressed(input.key, input.scancode(), input.modifiers())) return true
        }
        pressedSettingsSetting?.let { s ->
            if (s.onKeyPressed(input.key, input.scancode(), input.modifiers())) return true
        }

        return super.keyPressed(input)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (searchFocused) {
            val c = event.codepoint().toChar()
            if (c.isLetterOrDigit() || c == ' ') {
                searchQuery += c
            }
            return true
        }

        focusedText?.let {
            it.onCharTyped(event.codepoint().toChar())
            return true
        }

        pressedSetting?.let { s ->
            if (s.onCharTyped(event.codepoint().toChar())) return true
        }
        pressedSettingsSetting?.let { s ->
            if (s.onCharTyped(event.codepoint().toChar())) return true
        }

        return super.charTyped(event)
    }

    // --- Helpers ----------------------------------------------------------

    private fun bringToFront(panel: Panel) {
        panels.remove(panel)
        panels.add(panel)
    }

    /** Lays out a list of settings (with expanded children), returns the next Y. */
    private fun layoutSettings(
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

    /** Commits/cancels any active text or bind capture unless it is [keep]. */
    private fun clearFocus(keep: Setting?) {
        focusedText?.takeIf { it !== keep }?.let {
            it.setEditing(false)
            focusedText = null
        }
        bindingSetting?.takeIf { it !== keep }?.let {
            it.listening = false
            bindingSetting = null
        }
    }

    override fun isPauseScreen(): Boolean = false

    override fun removed() {
        saveExpandedStates()
        ClickGuiState.save()
        super.removed()
    }

    private fun saveExpandedStates() {
        for (panel in panels) {
            for (button in panel.buttons) {
                ClickGuiState.setModuleExpanded(button.module.name, button.expanded)
                if (button.expanded) {
                    saveSettingExpandedStates(button.settings, button.module.name, "")
                }
            }
        }
    }

    private fun saveSettingExpandedStates(settings: List<Setting>, moduleName: String, parentPath: String) {
        for (s in settings) {
            val key = "$moduleName/$parentPath${s.displayName}"
            ClickGuiState.setSettingExpanded(key, s.expanded)
            if (s.children.isNotEmpty()) {
                saveSettingExpandedStates(s.children, moduleName, "$key/")
            }
        }
    }
}
