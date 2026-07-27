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

import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import kotlin.math.abs

/**
 * Native, Meteor-Client-style ClickGUI. Every category is a draggable window of
 * modules; each module can be toggled and expanded to reveal its settings.
 */
class ClickGuiScreen : Screen(PlainText.EMPTY) {

    private val panels = ArrayList<Panel>()

    // Drag / interaction state
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

    override fun init() {
        if (panels.isEmpty()) {
            var px = 6f
            for (category in ModuleCategories.entries) {
                panels.add(Panel(category, px, 6f))
                px += ClickGuiTheme.PANEL_WIDTH + 6f
            }
        }
    }

    // --- Rendering ------------------------------------------------------------

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)
        // Subtle dim so the panels stand out over the game world.
        context.fill(0, 0, width, height, 0x60101014)

        for (panel in panels) {
            panel.render(context, mouseX, mouseY, height)
        }
    }

    // --- Mouse ----------------------------------------------------------------

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseX = click.x
        val mouseY = click.y
        val button = click.button()

        // Front-most panels get priority (iterate in reverse).
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
        return super.mouseClicked(click, doubled)
    }

    private fun handleBodyClick(panel: Panel, mouseX: Double, mouseY: Double, button: Int) {
        val setting = panel.settingAt(mouseX, mouseY)
        if (setting != null) {
            clearFocus(setting)
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
                1 -> moduleButton.expanded = !moduleButton.expanded
                else -> moduleButton.module.enabled = !moduleButton.module.enabled
            }
        }
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

        val panel = draggingPanel
        if (panel != null) {
            if (abs(mouseX - pressX) > 3 || abs(mouseY - pressY) > 3) {
                panelMoved = true
            }
            panel.x = (mouseX - dragOffsetX).toFloat()
            panel.y = (mouseY - dragOffsetY).toFloat()
            return true
        }

        pressedSetting?.onDrag(mouseX, mouseY)
        return true
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        val panel = draggingPanel
        if (panel != null) {
            if (!panelMoved) {
                panel.collapsed = !panel.collapsed
            }
            draggingPanel = null
        }
        pressedSetting?.onRelease()
        pressedSetting = null
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        for (panel in panels.asReversed()) {
            if (panel.isInBody(mouseX, mouseY, height)) {
                panel.scroll(verticalAmount)
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    // --- Keyboard -------------------------------------------------------------

    override fun keyPressed(input: KeyEvent): Boolean {
        bindingSetting?.let { bind ->
            bind.onKeyPressed(input.key, input.scancode(), input.modifiers())
            if (!bind.listening) {
                bindingSetting = null
            }
            return true
        }

        focusedText?.let { text ->
            text.onKeyPressed(input.key, input.scancode(), input.modifiers())
            return true
        }

        return super.keyPressed(input)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        focusedText?.let {
            it.onCharTyped(event.codepoint().toChar())
            return true
        }
        return super.charTyped(event)
    }

    // --- Helpers --------------------------------------------------------------

    private fun bringToFront(panel: Panel) {
        panels.remove(panel)
        panels.add(panel)
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
}
