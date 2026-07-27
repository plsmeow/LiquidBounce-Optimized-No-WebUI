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
@file:Suppress("UNCHECKED_CAST", "TooManyFunctions")
package net.ccbluex.liquidbounce.gui.clickgui

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.config.types.CurveValue
import net.ccbluex.liquidbounce.config.types.FileValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.ItemListValue
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.joml.Vector2fc
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// --- Shared drawing helpers ---------------------------------------------------

private const val TRACK_HEIGHT = 2f
private val ARROW_EXPANDED = "-"
private val ARROW_COLLAPSED = "+"

private fun Setting.rightText(ctx: GuiGraphicsExtractor, text: String, color: Int) {
    val tx = x + width - ClickGuiTheme.TEXT_PADDING - GuiRender.textWidth(text)
    ctx.drawTextCenteredY(text, tx, y, rowHeight, color)
}

private fun formatNumber(value: Any?): String = when (value) {
    is Float -> if (value == value.toLong().toFloat()) value.toLong().toString() else String.format("%.2f", value)
    is Double -> if (value == value.toLong().toDouble()) value.toLong().toString() else String.format("%.2f", value)
    else -> value.toString()
}

// --- Boolean ------------------------------------------------------------------

class BooleanSetting(value: Value<*>, indent: Int) : Setting(value, indent) {
    private val boolValue get() = value as Value<Boolean>

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val enabled = boolValue.get()
        ctx.drawTextCenteredY(
            displayName, labelX, y, rowHeight,
            if (enabled) ClickGuiTheme.textActive else ClickGuiTheme.textPrimary
        )

        val boxSize = 7f
        val boxX = x + width - ClickGuiTheme.TEXT_PADDING - boxSize
        val boxY = y + (rowHeight - boxSize) / 2f
        ctx.roundedRect(
            boxX, boxY, boxSize, boxSize, 1.5f,
            if (enabled) ClickGuiTheme.checkboxOn else ClickGuiTheme.checkboxOff
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        boolValue.set(!boolValue.get())
        return true
    }
}

// --- Slider -------------------------------------------------------------------

class SliderSetting(private val ranged: RangedValue<*>, indent: Int) : Setting(ranged, indent) {

    override val rowHeight: Float get() = 17f

    private val minD = (ranged.range.start as Number).toDouble()
    private val maxD = (ranged.range.endInclusive as Number).toDouble()
    private val isInt = ranged.get() is Int || ranged.get() is Long

    private val trackX get() = labelX
    private val trackWidth get() = x + width - ClickGuiTheme.TEXT_PADDING - trackX

    private var editing = false
    private var buffer = ""

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        if (editing) {
            ctx.drawTextCenteredY(displayName, labelX, y, rowHeight, ClickGuiTheme.textActive)
            rightText(ctx, buffer + "_", ClickGuiTheme.textActive)
        } else {
            ctx.drawText(displayName, labelX, y + 2f, ClickGuiTheme.textPrimary)
            rightText(ctx, formatNumber(ranged.get()) + ranged.suffix.let { if (it.isEmpty()) "" else " $it" },
                ClickGuiTheme.textSecondary)
        }

        val trackY = y + rowHeight - 5f
        ctx.fillRect(trackX, trackY, trackWidth, TRACK_HEIGHT, ClickGuiTheme.sliderTrack)
        val fraction = fractionOf(current())
        ctx.fillRect(trackX, trackY, trackWidth * fraction.toFloat(), TRACK_HEIGHT, ClickGuiTheme.sliderFill)
        // Handle
        val hx = trackX + trackWidth * fraction.toFloat()
        ctx.fillRect(hx - 1.5f, trackY - 2f, 3f, TRACK_HEIGHT + 4f, ClickGuiTheme.accent)
    }

    private fun current(): Double = (ranged.get() as Number).toDouble()

    private fun fractionOf(v: Double): Double = if (maxD == minD) 0.0 else ((v - minD) / (maxD - minD)).coerceIn(0.0, 1.0)

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 1) {
            editing = true
            buffer = formatNumber(ranged.get())
            return true
        }
        onDrag(mouseX, mouseY)
        return true
    }

    override fun onDrag(mouseX: Double, mouseY: Double) {
        if (editing) return
        val fraction = ((mouseX - trackX) / trackWidth).coerceIn(0.0, 1.0)
        val newValue = minD + fraction * (maxD - minD)
        setNumber(ranged, newValue, isInt)
    }

    override fun onKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!editing) return false
        when (keyCode) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                runCatching { setNumber(ranged, buffer.toDouble(), isInt) }
                editing = false
            }
            GLFW.GLFW_KEY_ESCAPE -> editing = false
            GLFW.GLFW_KEY_BACKSPACE -> if (buffer.isNotEmpty()) buffer = buffer.dropLast(1)
        }
        return true
    }

    override fun onCharTyped(char: Char): Boolean {
        if (!editing) return false
        if (char in '0'..'9' || char == '.' || char == '-' || char == '+') {
            buffer += char
        }
        return true
    }

    override fun onRelease() {
        editing = false
    }
}

private fun setNumber(value: Value<*>, number: Double, isInt: Boolean) {
    when (value.get()) {
        is Int -> (value as Value<Int>).set(number.roundToInt())
        is Long -> (value as Value<Long>).set(number.roundToLong())
        is Float -> (value as Value<Float>).set(if (isInt) number.roundToLong().toFloat() else number.toFloat())
        is Double -> (value as Value<Double>).set(number)
    }
}

// --- Range slider (min..max) --------------------------------------------------

class RangeSliderSetting(private val ranged: RangedValue<*>, indent: Int) : Setting(ranged, indent) {

    override val rowHeight: Float get() = 17f

    private val minD = (ranged.range.start as Number).toDouble()
    private val maxD = (ranged.range.endInclusive as Number).toDouble()
    private val isInt = ranged.get() is IntRange

    private var draggingMax = false
    private var editingHandle = -1
    private var buffer = ""

    private val trackX get() = labelX
    private val trackWidth get() = x + width - ClickGuiTheme.TEXT_PADDING - trackX

    private fun bounds(): Pair<Double, Double> = when (val v = ranged.get()) {
        is IntRange -> v.first.toDouble() to v.last.toDouble()
        is ClosedFloatingPointRange<*> -> (v.start as Number).toDouble() to (v.endInclusive as Number).toDouble()
        else -> minD to maxD
    }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.drawText(displayName, labelX, y + 2f, ClickGuiTheme.textPrimary)
        val (lo, hi) = bounds()
        if (editingHandle >= 0) {
            val label = if (editingHandle == 0) "Min: $buffer" else "Max: $buffer"
            val rtW = GuiRender.textWidth(label) + ClickGuiTheme.TEXT_PADDING
            val mLW = (width - (labelX - x) - rtW - 4f).toInt().coerceAtLeast(20)
            ctx.drawTextCenteredY(GuiRender.trim(displayName, mLW), labelX, y, rowHeight, ClickGuiTheme.textActive)
            rightText(ctx, label, ClickGuiTheme.textActive)
        } else {
            rightText(ctx, "${formatNumber(numberTyped(lo))}-${formatNumber(numberTyped(hi))}", ClickGuiTheme.textSecondary)
        }

        val trackY = y + rowHeight - 5f
        ctx.fillRect(trackX, trackY, trackWidth, TRACK_HEIGHT, ClickGuiTheme.sliderTrack)
        val fLo = fraction(lo)
        val fHi = fraction(hi)
        ctx.fillRect(trackX + trackWidth * fLo.toFloat(), trackY, trackWidth * (fHi - fLo).toFloat(), TRACK_HEIGHT,
            ClickGuiTheme.sliderFill)
        drawHandle(ctx, trackX + trackWidth * fLo.toFloat(), trackY)
        drawHandle(ctx, trackX + trackWidth * fHi.toFloat(), trackY)
    }

    private fun drawHandle(ctx: GuiGraphicsExtractor, hx: Float, trackY: Float) {
        ctx.fillRect(hx - 1.5f, trackY - 2f, 3f, TRACK_HEIGHT + 4f, ClickGuiTheme.accent)
    }

    private fun numberTyped(v: Double): Any = if (isInt) v.roundToInt() else v
    private fun fraction(v: Double): Double = if (maxD == minD) 0.0 else ((v - minD) / (maxD - minD)).coerceIn(0.0, 1.0)

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 1) {
            val (lo, hi) = bounds()
            val fClick = ((mouseX - trackX) / trackWidth).coerceIn(0.0, 1.0)
            editingHandle = if (kotlin.math.abs(fClick - fraction(hi)) < kotlin.math.abs(fClick - fraction(lo))) 1 else 0
            buffer = formatNumber(if (editingHandle == 0) lo else hi)
            return true
        }
        val (lo, hi) = bounds()
        val fClick = ((mouseX - trackX) / trackWidth).coerceIn(0.0, 1.0)
        draggingMax = kotlin.math.abs(fClick - fraction(hi)) < kotlin.math.abs(fClick - fraction(lo))
        onDrag(mouseX, mouseY)
        return true
    }

    override fun onDrag(mouseX: Double, mouseY: Double) {
        if (editingHandle >= 0) return
        val fraction = ((mouseX - trackX) / trackWidth).coerceIn(0.0, 1.0)
        val newValue = minD + fraction * (maxD - minD)
        var (lo, hi) = bounds()
        if (draggingMax) hi = newValue.coerceAtLeast(lo) else lo = newValue.coerceAtMost(hi)
        applyRange(lo, hi)
    }

    override fun onKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (editingHandle < 0) return false
        when (keyCode) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                runCatching {
                    val v = buffer.toDouble()
                    val (lo, hi) = bounds()
                    if (editingHandle == 0) applyRange(v, hi) else applyRange(lo, v)
                }
                editingHandle = -1
            }
            GLFW.GLFW_KEY_ESCAPE -> editingHandle = -1
            GLFW.GLFW_KEY_BACKSPACE -> if (buffer.isNotEmpty()) buffer = buffer.dropLast(1)
        }
        return true
    }

    override fun onCharTyped(char: Char): Boolean {
        if (editingHandle < 0) return false
        if (char in '0'..'9' || char == '.' || char == '-' || char == '+') {
            buffer += char
        }
        return true
    }

    override fun onRelease() {
        editingHandle = -1
    }

    private fun applyRange(lo: Double, hi: Double) {
        if (isInt) {
            (ranged as Value<IntRange>).set(lo.roundToInt()..hi.roundToInt())
        } else {
            (ranged as Value<ClosedFloatingPointRange<Float>>).set(lo.toFloat()..hi.toFloat())
        }
    }
}

// --- Text ---------------------------------------------------------------------

class TextSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private var editing = false
    private var buffer = ""

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.drawTextCenteredY(displayName, labelX, y, rowHeight, ClickGuiTheme.textPrimary)
        val display = if (editing) buffer + "_" else GuiRender.trim(value.get().toString(), 70)
        rightText(ctx, display, if (editing) ClickGuiTheme.textActive else ClickGuiTheme.textSecondary)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        setEditing(true)
        return true
    }

    fun setEditing(edit: Boolean) {
        if (edit && !editing) {
            buffer = value.get().toString()
        } else if (!edit && editing) {
            commit()
        }
        editing = edit
    }

    private fun commit() {
        runCatching { value.setByString(buffer) }
    }

    override fun onKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!editing) {
            return false
        }
        when (keyCode) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> setEditing(false)
            GLFW.GLFW_KEY_BACKSPACE -> if (buffer.isNotEmpty()) buffer = buffer.dropLast(1)
        }
        return true
    }

    override fun onCharTyped(char: Char): Boolean {
        if (!editing) {
            return false
        }
        if (char >= ' ') {
            buffer += char
        }
        return true
    }
}

// --- Bind ---------------------------------------------------------------------

class BindSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private val bindValue get() = value as Value<InputBind>
    var listening = false

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.drawTextCenteredY(displayName, labelX, y, rowHeight, ClickGuiTheme.textPrimary)
        val bind = bindValue.get()
        val text = if (listening) {
            "..."
        } else {
            "[${bind.keyName}] ${bind.action.tag}"
        }
        rightText(ctx, text, if (listening) ClickGuiTheme.textActive else ClickGuiTheme.textSecondary)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 1) {
            val current = bindValue.get()
            val actions = InputBind.BindAction.entries
            val nextIdx = (actions.indexOf(current.action) + 1) % actions.size
            bindValue.set(current.copy(action = actions[nextIdx]))
            return true
        }
        listening = true
        return true
    }

    override fun onKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!listening) {
            return false
        }
        val current = bindValue.get()
        val newBind = if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
            current.copy(boundKey = InputConstants.UNKNOWN)
        } else {
            current.copy(boundKey = InputConstants.Type.KEYSYM.getOrCreate(keyCode))
        }
        bindValue.set(newBind)
        listening = false
        return true
    }
}

// --- Enum (single choice) -----------------------------------------------------

class EnumSetting(private val choice: ChoiceListValue<*>, indent: Int) : Setting(choice, indent) {

    private val choices: List<Tagged> get() = choice.choices.toList()

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val current = (choice.get() as? Tagged)?.tag ?: "?"
        val rightText = "< $current >"
        val rightTextWidth = GuiRender.textWidth(rightText) + ClickGuiTheme.TEXT_PADDING
        val maxLabelWidth = (width - (labelX - x) - rightTextWidth - 4f).toInt().coerceAtLeast(20)
        val label = GuiRender.trim(displayName, maxLabelWidth)
        ctx.drawTextCenteredY(label, labelX, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, rightText, ClickGuiTheme.accent.argb)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val list = choices
        if (list.isEmpty()) {
            return true
        }
        val currentTag = (choice.get() as? Tagged)?.tag
        val idx = list.indexOfFirst { it.tag == currentTag }
        val step = if (button == 1) -1 else 1
        val next = list[((idx + step) % list.size + list.size) % list.size]
        runCatching { choice.setByString(next.tag) }
        return true
    }
}

// --- Multi choice -------------------------------------------------------------

class MultiChooseSetting(private val multi: MultiChoiceListValue<*>, indent: Int) : Setting(multi, indent) {

    private var open = false
    override val expanded: Boolean get() = open

    override val children: List<Setting> by lazy {
        multi.choices.map { MultiChoiceItemSetting(multi, it, indent + 1) }
    }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val arrowX = x + ClickGuiTheme.TEXT_PADDING + indent * ClickGuiTheme.SETTING_INDENT
        ctx.drawTextCenteredY(if (open) ARROW_EXPANDED else ARROW_COLLAPSED, arrowX, y, rowHeight, ClickGuiTheme.textSecondary)
        val rightText = "${(multi.get() as Collection<*>).size} selected"
        val rightTextWidth = GuiRender.textWidth(rightText) + ClickGuiTheme.TEXT_PADDING
        val maxLabelWidth = (width - indent * ClickGuiTheme.SETTING_INDENT - ClickGuiTheme.TEXT_PADDING * 3 - rightTextWidth - 7f).toInt().coerceAtLeast(20)
        val label = GuiRender.trim(displayName, maxLabelWidth)
        ctx.drawTextCenteredY(label, labelX + 7f, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, rightText, ClickGuiTheme.textSecondary)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        open = !open
        return true
    }
}

class MultiChoiceItemSetting(
    private val multi: MultiChoiceListValue<*>,
    private val item: Tagged,
    indent: Int
) : Setting(multi, indent) {

    override val displayName: String get() = item.tag

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val selected = (multi.get() as Collection<*>).any { (it as? Tagged)?.tag == item.tag }
        ctx.drawTextCenteredY(displayName, labelX, y, rowHeight,
            if (selected) ClickGuiTheme.textActive else ClickGuiTheme.textSecondary)
        val boxSize = 7f
        val boxX = x + width - ClickGuiTheme.TEXT_PADDING - boxSize
        val boxY = y + (rowHeight - boxSize) / 2f
        ctx.roundedRect(boxX, boxY, boxSize, boxSize, 1.5f,
            if (selected) ClickGuiTheme.checkboxOn else ClickGuiTheme.checkboxOff)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val value = multi as Value<MutableSet<Tagged>>
        val newSet = LinkedHashSet(value.get())
        if (!newSet.removeIf { it.tag == item.tag }) {
            newSet.add(item)
        }
        runCatching { value.set(newSet) }
        return true
    }
}

// --- Color --------------------------------------------------------------------

class ColorSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private val colorValue get() = value as Value<Color4b>
    private var open = false
    override val expanded: Boolean get() = open

    override val children: List<Setting> by lazy {
        listOf(
            ColorChannelSetting(colorValue, indent + 1, "Red", { it.r }, { c, n -> c.with(r = n) }),
            ColorChannelSetting(colorValue, indent + 1, "Green", { it.g }, { c, n -> c.with(g = n) }),
            ColorChannelSetting(colorValue, indent + 1, "Blue", { it.b }, { c, n -> c.with(b = n) }),
            ColorChannelSetting(colorValue, indent + 1, "Alpha", { it.a }, { c, n -> c.with(a = n) }),
        )
    }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.drawTextCenteredY(if (open) ARROW_EXPANDED else ARROW_COLLAPSED, x + ClickGuiTheme.TEXT_PADDING +
            indent * ClickGuiTheme.SETTING_INDENT, y, rowHeight, ClickGuiTheme.textSecondary)
        ctx.drawTextCenteredY(displayName, labelX + 7f, y, rowHeight, ClickGuiTheme.textPrimary)
        val swatchSize = 8f
        val sx = x + width - ClickGuiTheme.TEXT_PADDING - swatchSize
        val sy = y + (rowHeight - swatchSize) / 2f
        ctx.roundedRect(sx, sy, swatchSize, swatchSize, 1.5f, colorValue.get())
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        open = !open
        return true
    }
}

/**
 * A single 0..255 channel slider that reads/writes one component of a [Color4b] value.
 */
class ColorChannelSetting(
    private val colorValue: Value<Color4b>,
    indent: Int,
    private val channelName: String,
    private val getter: (Color4b) -> Int,
    private val setter: (Color4b, Int) -> Color4b
) : Setting(colorValue, indent) {

    override val displayName: String get() = channelName
    override val rowHeight: Float get() = 15f

    private val trackX get() = labelX
    private val trackWidth get() = x + width - ClickGuiTheme.TEXT_PADDING - trackX

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.drawText(channelName, labelX, y + 1f, ClickGuiTheme.textSecondary)
        val v = getter(colorValue.get())
        rightText(ctx, v.toString(), ClickGuiTheme.textSecondary)
        val trackY = y + rowHeight - 4f
        ctx.fillRect(trackX, trackY, trackWidth, TRACK_HEIGHT, ClickGuiTheme.sliderTrack)
        ctx.fillRect(trackX, trackY, trackWidth * (v / 255f), TRACK_HEIGHT, ClickGuiTheme.sliderFill)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        onDrag(mouseX, mouseY)
        return true
    }

    override fun onDrag(mouseX: Double, mouseY: Double) {
        val fraction = ((mouseX - trackX) / trackWidth).coerceIn(0.0, 1.0)
        val newChannel = (fraction * 255).roundToInt()
        colorValue.set(setter(colorValue.get(), newChannel))
    }
}

// --- Mode (choice group) ------------------------------------------------------

class ModeSetting(private val mode: ModeValueGroup<*>, indent: Int) : Setting(mode, indent) {

    private var open = false
    override val expanded: Boolean get() = open

    // Children depend on the active mode, so cache them per mode to preserve
    // nested expansion state without rebuilding every frame.
    private val childCache = HashMap<String, List<Setting>>()

    override val children: List<Setting>
        get() = if (open) {
            childCache.getOrPut(mode.activeMode.name) {
                buildSettings(mode.activeMode, indent + 1, skipEnabled = true)
            }
        } else {
            emptyList()
        }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.drawTextCenteredY(if (open) ARROW_EXPANDED else ARROW_COLLAPSED, x + ClickGuiTheme.TEXT_PADDING +
            indent * ClickGuiTheme.SETTING_INDENT, y, rowHeight, ClickGuiTheme.textSecondary)
        val rightText = "< ${mode.activeMode.name} >"
        val rightTextWidth = GuiRender.textWidth(rightText) + ClickGuiTheme.TEXT_PADDING
        val maxLabelWidth = (width - indent * ClickGuiTheme.SETTING_INDENT - ClickGuiTheme.TEXT_PADDING * 3 - rightTextWidth).toInt().coerceAtLeast(20)
        val label = GuiRender.trim(displayName, maxLabelWidth)
        ctx.drawTextCenteredY(label, labelX + 7f, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, rightText, ClickGuiTheme.accent.argb)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Right side cycles the mode, left side (arrow/name) expands.
        val cycleZone = x + width * 0.55
        if (mouseX >= cycleZone) {
            cycleMode(if (button == 1) -1 else 1)
        } else {
            open = !open
        }
        return true
    }

    private fun cycleMode(step: Int) {
        val modes = mode.modes
        if (modes.isEmpty()) {
            return
        }
        val idx = modes.indexOf(mode.activeMode)
        val next = modes[((idx + step) % modes.size + modes.size) % modes.size]
        runCatching { mode.setByString(next.name) }
    }
}

// --- Group (configurable / toggleable) ----------------------------------------

class GroupSetting(private val group: ValueGroup, indent: Int) : Setting(group, indent) {

    private var open = false
    override val expanded: Boolean get() = open

    private val toggleable = group as? ToggleableValueGroup

    private val cachedChildren by lazy { buildSettings(group, indent + 1, skipEnabled = toggleable != null) }

    override val children: List<Setting>
        get() = if (open) cachedChildren else emptyList()

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val arrowX = x + ClickGuiTheme.TEXT_PADDING + indent * ClickGuiTheme.SETTING_INDENT
        ctx.drawTextCenteredY(if (open) ARROW_EXPANDED else ARROW_COLLAPSED, arrowX, y, rowHeight, ClickGuiTheme.textSecondary)
        val toggle = toggleable
        val nameColor = if (toggle != null && toggle.enabled) ClickGuiTheme.textActive else ClickGuiTheme.textPrimary
        val rightTextWidth = if (toggle != null) ClickGuiTheme.TEXT_PADDING + 7 else 0
        val maxLabelWidth = (width - indent * ClickGuiTheme.SETTING_INDENT - ClickGuiTheme.TEXT_PADDING * 3 - rightTextWidth - 7f).toInt().coerceAtLeast(20)
        val label = GuiRender.trim(displayName, maxLabelWidth)
        ctx.drawTextCenteredY(label, labelX + 7f, y, rowHeight, nameColor)

        if (toggle != null) {
            val boxSize = 7f
            val boxX = x + width - ClickGuiTheme.TEXT_PADDING - boxSize
            val boxY = y + (rowHeight - boxSize) / 2f
            ctx.roundedRect(boxX, boxY, boxSize, boxSize, 1.5f,
                if (toggle.enabled) ClickGuiTheme.checkboxOn else ClickGuiTheme.checkboxOff)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val toggle = toggleable
        // Clicking near the right checkbox toggles; elsewhere expands.
        if (toggle != null && mouseX >= x + width - 16) {
            toggle.enabled = !toggle.enabled
        } else {
            open = !open
        }
        return true
    }
}

// --- Curve (read-only display) ------------------------------------------------

class CurveSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private val curveValue get() = value as CurveValue

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val points = curveValue.get()
        val rightText = "${points.size} pts"
        val rightTextWidth = GuiRender.textWidth(rightText) + ClickGuiTheme.TEXT_PADDING
        val maxLabelWidth = (width - (labelX - x) - rightTextWidth - 4f).toInt().coerceAtLeast(20)
        val label = GuiRender.trim(displayName, maxLabelWidth)
        ctx.drawTextCenteredY(label, labelX, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, rightText, ClickGuiTheme.textSecondary)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = false
}

// --- File (click to open dialog) ----------------------------------------------

class FileSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private val fileValue get() = value as FileValue

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val file = fileValue.get()
        val fileName = file?.name ?: "(none)"
        val rightText = GuiRender.trim(fileName, (width * 0.5f).toInt().coerceAtLeast(20))
        val rightTextWidth = GuiRender.textWidth(rightText) + ClickGuiTheme.TEXT_PADDING
        val maxLabelWidth = (width - (labelX - x) - rightTextWidth - 4f).toInt().coerceAtLeast(20)
        val label = GuiRender.trim(displayName, maxLabelWidth)
        ctx.drawTextCenteredY(label, labelX, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, rightText, ClickGuiTheme.accent.argb)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val dialogMode = fileValue.dialogMode
        val extensions = fileValue.supportedExtensions
        val results = dialogMode.selectFiles(extensions)
        if (results.isNotEmpty()) {
            fileValue.set(java.io.File(results[0]))
        }
        return true
    }
}

// --- Vector (Vec2f / Vec3i / Vec3d) ----------------------------------------

class VectorSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private var open = false
    override val expanded: Boolean get() = open

    private val axisNames: List<String> = when (value.get()) {
        is Vector2fc -> listOf("X", "Y")
        is Vec3i, is Vec3 -> listOf("X", "Y", "Z")
        else -> emptyList()
    }

    private var childCache = listOf<VectorAxisSetting>()

    override val children: List<Setting>
        get() {
            if (!open) return emptyList()
            if (childCache.size != axisNames.size) {
                childCache = axisNames.mapIndexed { i, name ->
                    VectorAxisSetting(value, indent + 1, name, i)
                }
            }
            return childCache
        }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val arrowX = x + ClickGuiTheme.TEXT_PADDING + indent * ClickGuiTheme.SETTING_INDENT
        ctx.drawTextCenteredY(
            if (open) ARROW_EXPANDED else ARROW_COLLAPSED, arrowX, y, rowHeight, ClickGuiTheme.textSecondary
        )
        val summary = when (val v = value.get()) {
            is Vector2fc -> "X: ${fmt(v.x())} Y: ${fmt(v.y())}"
            is Vec3i -> "X: ${v.x} Y: ${v.y} Z: ${v.z}"
            is Vec3 -> "X: ${fmt(v.x)} Y: ${fmt(v.y)} Z: ${fmt(v.z)}"
            else -> ""
        }
        val rtW = GuiRender.textWidth(summary) + ClickGuiTheme.TEXT_PADDING
        val mLW = (width - indent * ClickGuiTheme.SETTING_INDENT - ClickGuiTheme.TEXT_PADDING * 3 - rtW - 7f).toInt()
            .coerceAtLeast(20)
        val label = GuiRender.trim(displayName, mLW)
        ctx.drawTextCenteredY(label, labelX + 7f, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, summary, ClickGuiTheme.textSecondary)
    }

    private fun fmt(v: Float) = if (v == v.toLong().toFloat()) v.toLong().toString() else String.format("%.1f", v)
    private fun fmt(v: Double) = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        open = !open
        return true
    }
}

class VectorAxisSetting(
    private val parentValue: Value<*>,
    indent: Int,
    axisName: String,
    private val axisIndex: Int
) : Setting(parentValue, indent) {

    override val displayName: String = axisName
    override val rowHeight: Float get() = 15f

    private fun getValue(): Double = when (val v = parentValue.get()) {
        is Vector2fc -> when (axisIndex) { 0 -> v.x().toDouble(); else -> v.y().toDouble() }
        is Vec3i -> when (axisIndex) { 0 -> v.x.toDouble(); 1 -> v.y.toDouble(); else -> v.z.toDouble() }
        is Vec3 -> when (axisIndex) { 0 -> v.x; 1 -> v.y; else -> v.z }
        else -> 0.0
    }

    private fun setValue(d: Double) {
        when (parentValue.get()) {
            is Vector2fc -> {
                val v = parentValue.get() as Vector2fc
                val nx = if (axisIndex == 0) d.toFloat() else v.x()
                val ny = if (axisIndex == 1) d.toFloat() else v.y()
                (parentValue as Value<Any>).set(Vector2f(nx, ny))
            }
            is Vec3i -> {
                val v = parentValue.get() as Vec3i
                val nx = if (axisIndex == 0) d.toInt() else v.x
                val ny = if (axisIndex == 1) d.toInt() else v.y
                val nz = if (axisIndex == 2) d.toInt() else v.z
                (parentValue as Value<Any>).set(Vec3i(nx, ny, nz))
            }
            is Vec3 -> {
                val v = parentValue.get() as Vec3
                val nx = if (axisIndex == 0) d else v.x
                val ny = if (axisIndex == 1) d else v.y
                val nz = if (axisIndex == 2) d else v.z
                (parentValue as Value<Any>).set(Vec3(nx, ny, nz))
            }
        }
    }

    private val trackX get() = labelX
    private val trackWidth get() = x + width - ClickGuiTheme.TEXT_PADDING - trackX

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.drawText(displayName, labelX, y + 1f, ClickGuiTheme.textSecondary)
        val v = getValue()
        rightText(ctx, formatNumber(v), ClickGuiTheme.textSecondary)
        val trackY = y + rowHeight - 4f
        val range = if (parentValue.get() is Vec3i) 30000000.0 else 100.0
        val fraction = ((v + range) / (2.0 * range)).coerceIn(0.0, 1.0)
        ctx.fillRect(trackX, trackY, trackWidth, TRACK_HEIGHT, ClickGuiTheme.sliderTrack)
        ctx.fillRect(trackX, trackY, trackWidth * fraction.toFloat(), TRACK_HEIGHT, ClickGuiTheme.sliderFill)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        onDrag(mouseX, mouseY)
        return true
    }

    override fun onDrag(mouseX: Double, mouseY: Double) {
        val fraction = ((mouseX - trackX) / trackWidth).coerceIn(0.0, 1.0)
        val range = if (parentValue.get() is Vec3i) 30000000.0 else 100.0
        setValue((fraction * 2.0 - 1.0) * range)
    }
}

// --- Mutable List (text/regex list editor) ----------------------------------

class MutableListSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private var open = false
    override val expanded: Boolean get() = open

    private var cachedSize = -1
    private var cachedChildren = listOf<Setting>()

    @Suppress("UNCHECKED_CAST")
    private fun getMutableList(): MutableList<Any>? = value.get() as? MutableList<Any>

    override val children: List<Setting>
        get() {
            if (!open) return emptyList()
            val list = getMutableList()
            val size = list?.size ?: 0
            if (size != cachedSize) {
                cachedSize = size
                val items = mutableListOf<Setting>()
                list?.forEachIndexed { index, _ ->
                    items.add(MutableListWidgetItem(value, indent + 1, index))
                }
                items.add(MutableListAddButton(value, indent + 1))
                cachedChildren = items
            }
            return cachedChildren
        }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val arrowX = x + ClickGuiTheme.TEXT_PADDING + indent * ClickGuiTheme.SETTING_INDENT
        ctx.drawTextCenteredY(
            if (open) ARROW_EXPANDED else ARROW_COLLAPSED, arrowX, y, rowHeight, ClickGuiTheme.textSecondary
        )
        val count = getMutableList()?.size ?: 0
        val rt = "$count items"
        val rtW = GuiRender.textWidth(rt) + ClickGuiTheme.TEXT_PADDING
        val mLW = (width - indent * ClickGuiTheme.SETTING_INDENT - ClickGuiTheme.TEXT_PADDING * 3 - rtW - 7f).toInt()
            .coerceAtLeast(20)
        val label = GuiRender.trim(displayName, mLW)
        ctx.drawTextCenteredY(label, labelX + 7f, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, rt, ClickGuiTheme.textSecondary)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        open = !open
        return true
    }
}

class MutableListWidgetItem(
    private val listValue: Value<*>,
    indent: Int,
    private val index: Int
) : Setting(listValue, indent) {

    override val displayName: String get() = "[$index]"
    override val rowHeight: Float get() = 15f

    private var editing = false
    private var buffer = ""

    @Suppress("UNCHECKED_CAST")
    private fun getMutableList(): MutableList<Any>? = listValue.get() as? MutableList<Any>

    private fun getItemText(): String {
        val list = getMutableList() ?: return ""
        if (index >= list.size) return ""
        return when (val item = list[index]) {
            is Regex -> item.pattern
            else -> item.toString()
        }
    }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.drawText("[$index]", labelX, y + 1f, ClickGuiTheme.textSecondary)
        val display = if (editing) buffer + "_" else GuiRender.trim(getItemText(), 60)
        ctx.drawText(
            display, labelX + 20f, y + 1f,
            if (editing) ClickGuiTheme.textActive else ClickGuiTheme.textPrimary
        )
        val delSize = 7f
        val delX = x + width - ClickGuiTheme.TEXT_PADDING - delSize
        val delY = y + (rowHeight - delSize) / 2f
        ctx.roundedRect(delX, delY, delSize, delSize, 1.5f, Color4b(200, 60, 60, 180))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val delX = x + width - ClickGuiTheme.TEXT_PADDING - 7.0
        if (mouseX >= delX) {
            val list = getMutableList() ?: return true
            if (index < list.size) list.removeAt(index)
            return true
        }
        editing = true
        buffer = getItemText()
        return true
    }

    override fun onRelease() {
        if (editing) {
            commitEdit()
            editing = false
        }
    }

    private fun commitEdit() {
        val list = getMutableList() ?: return
        if (index < list.size) {
            list[index] = buffer
        }
    }

    override fun onKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!editing) return false
        when (keyCode) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                commitEdit()
                editing = false
            }
            GLFW.GLFW_KEY_ESCAPE -> editing = false
            GLFW.GLFW_KEY_BACKSPACE -> if (buffer.isNotEmpty()) buffer = buffer.dropLast(1)
        }
        return true
    }

    override fun onCharTyped(char: Char): Boolean {
        if (!editing) return false
        if (char >= ' ') buffer += char
        return true
    }
}

class MutableListAddButton(
    private val listValue: Value<*>,
    indent: Int
) : Setting(listValue, indent) {

    override val displayName: String = "+ Add"
    override val rowHeight: Float get() = 15f

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        ctx.drawTextCenteredY("+ Add", labelX, y, rowHeight, ClickGuiTheme.accent.argb)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        @Suppress("UNCHECKED_CAST")
        val list = listValue.get() as? MutableList<Any> ?: return true
        list.add("")
        return true
    }
}

// --- Registry List (read-only) ----------------------------------------------

class RegistryListSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private var open = false
    override val expanded: Boolean get() = open

    @Suppress("UNCHECKED_CAST")
    private fun getItems(): Collection<*> = value.get() as? Collection<*> ?: emptyList<Any>()

    private var cachedSize = -1
    private var cachedChildren = listOf<Setting>()

    override val children: List<Setting>
        get() {
            if (!open) return emptyList()
            val items = getItems()
            if (items.size != cachedSize) {
                cachedSize = items.size
                cachedChildren = items.map { item ->
                    RegistryListItemWidget(item?.toString() ?: "?", indent + 1, value)
                }
            }
            return cachedChildren
        }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val arrowX = x + ClickGuiTheme.TEXT_PADDING + indent * ClickGuiTheme.SETTING_INDENT
        ctx.drawTextCenteredY(
            if (open) ARROW_EXPANDED else ARROW_COLLAPSED, arrowX, y, rowHeight, ClickGuiTheme.textSecondary
        )
        val count = getItems().size
        val rt = "$count items"
        val rtW = GuiRender.textWidth(rt) + ClickGuiTheme.TEXT_PADDING
        val mLW = (width - indent * ClickGuiTheme.SETTING_INDENT - ClickGuiTheme.TEXT_PADDING * 3 - rtW - 7f).toInt()
            .coerceAtLeast(20)
        val label = GuiRender.trim(displayName, mLW)
        ctx.drawTextCenteredY(label, labelX + 7f, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, rt, ClickGuiTheme.textSecondary)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        open = !open
        return true
    }
}

class RegistryListItemWidget(
    private val itemName: String,
    indent: Int,
    value: Value<*>
) : Setting(value, indent) {

    override val displayName: String get() = itemName
    override val rowHeight: Float get() = 15f

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val maxW = (width - labelX + x - ClickGuiTheme.TEXT_PADDING).toInt().coerceAtLeast(20)
        val text = GuiRender.trim(itemName, maxW)
        ctx.drawText(text, labelX, y + 1f, ClickGuiTheme.textSecondary)
    }
}

// --- Registry Mutable List (editable) ----------------------------------------

class RegistryMutableListSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private var open = false
    override val expanded: Boolean get() = open

    @Suppress("UNCHECKED_CAST")
    private fun getMutableList(): MutableList<Any>? = value.get() as? MutableList<Any>

    private var cachedSize = -1
    private var cachedChildren = listOf<Setting>()

    override val children: List<Setting>
        get() {
            if (!open) return emptyList()
            val list = getMutableList()
            val size = list?.size ?: 0
            if (size != cachedSize) {
                cachedSize = size
                val items = mutableListOf<Setting>()
                list?.forEachIndexed { index, item ->
                    items.add(RegistryMutableListItemWidget(value, indent + 1, index, item?.toString() ?: "?"))
                }
                items.add(MutableListAddButton(value, indent + 1))
                cachedChildren = items
            }
            return cachedChildren
        }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val arrowX = x + ClickGuiTheme.TEXT_PADDING + indent * ClickGuiTheme.SETTING_INDENT
        ctx.drawTextCenteredY(
            if (open) ARROW_EXPANDED else ARROW_COLLAPSED, arrowX, y, rowHeight, ClickGuiTheme.textSecondary
        )
        val count = getMutableList()?.size ?: 0
        val rt = "$count items"
        val rtW = GuiRender.textWidth(rt) + ClickGuiTheme.TEXT_PADDING
        val mLW = (width - indent * ClickGuiTheme.SETTING_INDENT - ClickGuiTheme.TEXT_PADDING * 3 - rtW - 7f).toInt()
            .coerceAtLeast(20)
        val label = GuiRender.trim(displayName, mLW)
        ctx.drawTextCenteredY(label, labelX + 7f, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, rt, ClickGuiTheme.textSecondary)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        open = !open
        return true
    }
}

class RegistryMutableListItemWidget(
    private val listValue: Value<*>,
    indent: Int,
    private val index: Int,
    private val itemName: String
) : Setting(listValue, indent) {

    override val displayName: String get() = itemName
    override val rowHeight: Float get() = 15f

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val maxW = (width - labelX + x - ClickGuiTheme.TEXT_PADDING - 12f).toInt().coerceAtLeast(20)
        val text = GuiRender.trim(itemName, maxW)
        ctx.drawText(text, labelX, y + 1f, ClickGuiTheme.textSecondary)
        val delSize = 7f
        val delX = x + width - ClickGuiTheme.TEXT_PADDING - delSize
        val delY = y + (rowHeight - delSize) / 2f
        ctx.roundedRect(delX, delY, delSize, delSize, 1.5f, Color4b(200, 60, 60, 180))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        @Suppress("UNCHECKED_CAST")
        val list = listValue.get() as? MutableList<Any> ?: return true
        if (index < list.size) list.removeAt(index)
        return true
    }
}

// --- Named Item List (toggles) ----------------------------------------------

class ItemListSetting(value: Value<*>, indent: Int) : Setting(value, indent) {

    private var open = false
    override val expanded: Boolean get() = open

    @Suppress("UNCHECKED_CAST")
    private fun getSelectedSet(): MutableSet<Any>? = value.get() as? MutableSet<Any>

    private fun getNamedItems(): Set<ItemListValue.NamedItem<*>> {
        @Suppress("UNCHECKED_CAST")
        return (value as? ItemListValue<*, *>)?.items ?: emptySet()
    }

    private var cachedItemCount = -1
    private var cachedChildren = listOf<Setting>()

    override val children: List<Setting>
        get() {
            if (!open) return emptyList()
            val namedItems = getNamedItems()
            if (namedItems.size != cachedItemCount) {
                cachedItemCount = namedItems.size
                cachedChildren = namedItems.map { named ->
                    ItemListToggleWidget(value, indent + 1, named)
                }
            }
            return cachedChildren
        }

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val arrowX = x + ClickGuiTheme.TEXT_PADDING + indent * ClickGuiTheme.SETTING_INDENT
        ctx.drawTextCenteredY(
            if (open) ARROW_EXPANDED else ARROW_COLLAPSED, arrowX, y, rowHeight, ClickGuiTheme.textSecondary
        )
        val selected = getSelectedSet()?.size ?: 0
        val total = getNamedItems().size
        val rt = "$selected/$total"
        val rtW = GuiRender.textWidth(rt) + ClickGuiTheme.TEXT_PADDING
        val mLW = (width - indent * ClickGuiTheme.SETTING_INDENT - ClickGuiTheme.TEXT_PADDING * 3 - rtW - 7f).toInt()
            .coerceAtLeast(20)
        val label = GuiRender.trim(displayName, mLW)
        ctx.drawTextCenteredY(label, labelX + 7f, y, rowHeight, ClickGuiTheme.textPrimary)
        rightText(ctx, rt, ClickGuiTheme.textSecondary)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        open = !open
        return true
    }
}

class ItemListToggleWidget(
    private val listValue: Value<*>,
    indent: Int,
    private val namedItem: ItemListValue.NamedItem<*>
) : Setting(listValue, indent) {

    override val displayName: String get() = namedItem.name
    override val rowHeight: Float get() = 15f

    @Suppress("UNCHECKED_CAST")
    private fun getSelectedSet(): MutableSet<Any>? = listValue.get() as? MutableSet<Any>

    private fun isSelected(): Boolean = getSelectedSet()?.contains(namedItem.value) ?: false

    override fun renderRow(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        rowBackground(ctx, mouseX, mouseY)
        val selected = isSelected()
        ctx.drawTextCenteredY(
            displayName, labelX, y, rowHeight,
            if (selected) ClickGuiTheme.textActive else ClickGuiTheme.textSecondary
        )
        val boxSize = 7f
        val boxX = x + width - ClickGuiTheme.TEXT_PADDING - boxSize
        val boxY = y + (rowHeight - boxSize) / 2f
        ctx.roundedRect(
            boxX, boxY, boxSize, boxSize, 1.5f,
            if (selected) ClickGuiTheme.checkboxOn else ClickGuiTheme.checkboxOff
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val set = getSelectedSet() ?: return true
        @Suppress("UNCHECKED_CAST")
        val item = namedItem.value as Any
        if (set.contains(item)) set.remove(item) else set.add(item)
        return true
    }
}
