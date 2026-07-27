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
package net.ccbluex.liquidbounce.gui

import net.ccbluex.liquidbounce.gui.clickgui.ClickGuiScreen
import net.minecraft.client.gui.screens.Screen

/**
 * Central registry of LiquidBounce's own native screens.
 *
 * Replaces the former `ScreenManager.isClientScreen` check that recognized the
 * browser-backed custom screens. Native code (including Java mixins) uses this
 * to know whether the current screen is a client-owned GUI so it can keep game
 * input flowing correctly behind it.
 */
object ClientScreens {

    @JvmStatic
    fun isClientScreen(screen: Screen?): Boolean = screen is ClickGuiScreen
}
