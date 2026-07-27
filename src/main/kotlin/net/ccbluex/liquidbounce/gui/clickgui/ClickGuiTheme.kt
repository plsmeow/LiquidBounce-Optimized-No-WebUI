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

import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * Central color palette and metric constants for the native Meteor-style ClickGUI.
 *
 * Keeping every visual constant here makes it trivial to retheme the whole GUI
 * from a single place, similar to Meteor Client's `GuiTheme`.
 */
object ClickGuiTheme {

    // --- Accent -----------------------------------------------------------
    /** Primary accent color, used for enabled modules, sliders and highlights. */
    val accent = Color4b(0x59, 0x7C, 0xFF, 0xFF)
    val accentDim = Color4b(0x59, 0x7C, 0xFF, 0x40)

    // --- Panels -----------------------------------------------------------
    val panelBackground = Color4b(0x14, 0x15, 0x1A, 0xF2)
    val panelHeader = Color4b(0x1E, 0x1F, 0x28, 0xFF)
    val panelHeaderActive = accent
    val panelOutline = Color4b(0x00, 0x00, 0x00, 0x50)

    // --- Modules / rows ---------------------------------------------------
    val moduleBackground = Color4b(0x00, 0x00, 0x00, 0x00)
    val moduleHover = Color4b(0xFF, 0xFF, 0xFF, 0x14)
    val moduleActive = accent

    // --- Settings ---------------------------------------------------------
    val settingBackground = Color4b(0x0C, 0x0D, 0x11, 0xC0)
    val settingHover = Color4b(0xFF, 0xFF, 0xFF, 0x0E)
    val sliderTrack = Color4b(0x3A, 0x3C, 0x4A, 0xFF)
    val sliderFill = accent
    val checkboxOff = Color4b(0x3A, 0x3C, 0x4A, 0xFF)
    val checkboxOn = accent

    // --- Text -------------------------------------------------------------
    val textPrimary = Color4b(0xE8, 0xEA, 0xF0, 0xFF).argb
    val textSecondary = Color4b(0x9A, 0x9E, 0xAD, 0xFF).argb
    val textActive = Color4b(0xFF, 0xFF, 0xFF, 0xFF).argb
    val textOnAccent = Color4b(0xFF, 0xFF, 0xFF, 0xFF).argb

    // --- Metrics ----------------------------------------------------------
    const val PANEL_WIDTH = 120
    const val HEADER_HEIGHT = 15
    const val MODULE_HEIGHT = 14
    const val SETTING_HEIGHT = 13
    const val SETTING_INDENT = 6
    const val TEXT_PADDING = 4
}
