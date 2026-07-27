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

import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Base class for a single configurable value rendered inside the ClickGUI.
 *
 * Layout works in immediate mode: every frame [layout] is called top-down to
 * assign [x]/[y]/[width]; input handlers rely on the bounds from the previous
 * layout pass. Expandable settings return their (possibly nested) children from
 * [children] and are only visited while [expanded] is true.
 */
abstract class Setting(val value: Value<*>, val indent: Int) {

    var x = 0f
    var y = 0f
    var width = 0f

    /** Height of just this setting's own row. */
    open val rowHeight: Float get() = ClickGuiTheme.SETTING_HEIGHT.toFloat()

    /** Nested settings shown below this one when [expanded]. */
    open val children: List<Setting> get() = emptyList()

    open val expanded: Boolean get() = false

    protected val labelX: Float get() = x + ClickGuiTheme.TEXT_PADDING + indent * ClickGuiTheme.SETTING_INDENT

    fun hovered(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + rowHeight

    abstract fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int)

    /** @return true if the click was consumed (starts a possible drag on this setting). */
    open fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = false

    open fun onDrag(mouseX: Double, mouseY: Double) {}

    open fun onRelease() {}

    open fun onKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false

    open fun onCharTyped(char: Char): Boolean = false

    protected fun rowBackground(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        if (hovered(mouseX.toDouble(), mouseY.toDouble())) {
            ctx.fillRect(x, y, width, rowHeight, ClickGuiTheme.settingHover)
        }
    }

    open val displayName: String get() = value.name
}

/**
 * Builds the flat list of top-level [Setting]s for a [ValueGroup], filtering out
 * values that should never appear in the GUI.
 */
fun buildSettings(group: ValueGroup, indent: Int, skipEnabled: Boolean): List<Setting> {
    val result = ArrayList<Setting>()
    for (child in group.get()) {
        if (child.notAnOption) {
            continue
        }
        if (skipEnabled && child.name.equals("Enabled", ignoreCase = true)) {
            continue
        }
        SettingFactory.create(child, indent)?.let(result::add)
    }
    return result
}

object SettingFactory {

    @Suppress("CyclomaticComplexMethod")
    fun create(value: Value<*>, indent: Int): Setting? = when (value.valueType) {
        ValueType.BOOLEAN -> BooleanSetting(value, indent)
        ValueType.FLOAT, ValueType.INT -> {
            if (value is RangedValue<*>) SliderSetting(value, indent) else LabelSetting(value, indent)
        }
        ValueType.FLOAT_RANGE, ValueType.INT_RANGE -> {
            if (value is RangedValue<*>) RangeSliderSetting(value, indent) else LabelSetting(value, indent)
        }
        ValueType.TEXT -> TextSetting(value, indent)
        ValueType.BIND, ValueType.KEY -> BindSetting(value, indent)
        ValueType.COLOR -> ColorSetting(value, indent)
        ValueType.CHOOSE -> if (value is ChoiceListValue<*>) EnumSetting(value, indent) else LabelSetting(value, indent)
        ValueType.MULTI_CHOOSE ->
            if (value is MultiChoiceListValue<*>) MultiChooseSetting(value, indent) else LabelSetting(value, indent)
        ValueType.CHOICE -> if (value is ModeValueGroup<*>) ModeSetting(value, indent) else LabelSetting(value, indent)
        ValueType.CONFIGURABLE, ValueType.TOGGLEABLE ->
            if (value is ValueGroup) GroupSetting(value, indent) else LabelSetting(value, indent)
        else -> LabelSetting(value, indent)
    }
}

/**
 * Read-only fallback for value types the ClickGUI does not (yet) edit inline.
 */
class LabelSetting(value: Value<*>, indent: Int) : Setting(value, indent) {
    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val text = valueText()
        val rightTextWidth = if (text.isNotEmpty()) ClickGuiTheme.TEXT_PADDING + GuiRender.textWidth(text) else 0
        val maxLabelWidth = (width - (labelX - x) - rightTextWidth - 4f).toInt().coerceAtLeast(20)
        val label = GuiRender.trim(displayName, maxLabelWidth)
        ctx.drawTextCenteredY(label, labelX, y, rowHeight, ClickGuiTheme.textPrimary)
        if (text.isNotEmpty()) {
            val tx = x + width - ClickGuiTheme.TEXT_PADDING - GuiRender.textWidth(text)
            ctx.drawTextCenteredY(text, tx, y, rowHeight, ClickGuiTheme.textSecondary)
        }
    }

    private fun valueText(): String = when (val v = value.get()) {
        is Tagged -> v.tag
        is Collection<*> -> ""
        else -> {
            val s = v.toString()
            if (GuiRender.textWidth(s) > 80) GuiRender.trim(s, 80) else s
        }
    }
}
