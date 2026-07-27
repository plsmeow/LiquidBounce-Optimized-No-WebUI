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
package net.ccbluex.liquidbounce.integration.theme.component

enum class HudComponentTweak {
    TWEAK_HOTBAR,
    DISABLE_CROSSHAIR,
    DISABLE_SCOREBOARD,
    DISABLE_STATUS_BAR,
    DISABLE_EXP_BAR,
    DISABLE_HELD_ITEM_TOOL_TIP,
    DISABLE_OVERLAY_MESSAGE,
    DISABLE_STATUS_EFFECT_OVERLAY,
    DISABLE_LOCATOR_BAR,
}

interface HudComponentAlignment {
    fun getBounds(width: Float, height: Float): HudComponentBounds
}

interface HudComponentBounds {
    fun xCenter(): Float
    fun yMin(): Float
}

interface HudComponent {
    fun getRunning(): Boolean
    fun getAlignment(): HudComponentAlignment
}

object HudComponentManager {
    @JvmStatic
    fun isTweakEnabled(tweak: HudComponentTweak): Boolean = false

    @JvmStatic
    fun getComponentWithTweak(tweak: HudComponentTweak): HudComponent? = null
}
