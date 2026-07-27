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
package net.ccbluex.liquidbounce.gui.hud

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Vector2f
import org.joml.Vector2fc

/**
 * A draggable in-game HUD element rendered on top of the world.
 *
 * Persistence is provided by [HudConfig] which stores a [Value] for every
 * public field of this class. When a subclass declares
 * `val x by HudConfig.float("X", default)`, the value is automatically saved
 * to `LiquidBounce/hud.json` and survives game restarts.
 *
 * Layout contract:
 * - The element is expected to render at [renderPosition] (top-left corner
 *   in scaled GUI pixels) and honour the [scale] factor.
 * - [getBaseSize] returns the element's natural size at scale 1.0; it is
 *   updated every frame by [render] and used by the editor for hit-testing
 *   and the auto-arranger.
 */
abstract class HudElement(
    val id: String,
    val displayName: String,
) {
    val enabled: Value<Boolean> = HudConfig.boolean("$id-enabled", true)
    val position: Value<Vector2fc> = HudConfig.vec2f("$id-position", Vector2f(8f, 8f))
    val scale: Value<Float> = HudConfig.float("$id-scale", 1.0f, 0.5f..2.0f)

    /** Where the position anchor refers to on the element's own bounds. */
    var alignment: Alignment = Alignment.TOP_LEFT

    /** Cached natural size (at scale 1.0). Updated every frame by [render]. */
    protected var lastBaseWidth = 80f
    protected var lastBaseHeight = 16f

    /**
     * Position override used by the editor while the user is dragging. The
     * renderer should always use [renderPosition] instead of [position] so
     * the user sees the element where the cursor is. When the user releases
     * the drag, the editor commits the value back into [position].
     */
    open var renderPosition: Vector2f = Vector2f(position.get())

    /**
     * Called once after the element instance is created. Use this to apply
     * sensible defaults that depend on the screen size, e.g. anchoring the
     * array list to the top-right corner.
     */
    open fun onInitialize(screenWidth: Int, screenHeight: Int) {}

    /**
     * Called every overlay frame while the element is enabled. Implementations
     * should render at [renderPosition] and update [lastBaseWidth] /
     * [lastBaseHeight] so the editor can use them for hit-testing.
     */
    abstract fun render(context: GuiGraphicsExtractor, event: OverlayRenderEvent)

    /** @return the natural (unscaled) size of the element. */
    fun getBaseSize(): Pair<Float, Float> = lastBaseWidth to lastBaseHeight

    /** @return the rendered (scaled) size of the element. */
    fun getScaledSize(): Pair<Float, Float> =
        lastBaseWidth * scale.get() to lastBaseHeight * scale.get()

    fun getOffset(): Pair<Float, Float> {
        val (w, h) = getScaledSize()
        val p = renderPosition
        return when (alignment) {
            Alignment.TOP_LEFT -> 0f to 0f
            Alignment.TOP_CENTER -> -w / 2f to 0f
            Alignment.TOP_RIGHT -> -w to 0f
            Alignment.CENTER_LEFT -> 0f to -h / 2f
            Alignment.CENTER -> -w / 2f to -h / 2f
            Alignment.CENTER_RIGHT -> -w to -h / 2f
            Alignment.BOTTOM_LEFT -> 0f to -h
            Alignment.BOTTOM_CENTER -> -w / 2f to -h
            Alignment.BOTTOM_RIGHT -> -w to -h
        }
    }

    /** Anchor opposite to [renderPosition], used by the editor to position the box outline. */
    fun getAnchor(): Vector2f {
        val p = renderPosition
        val (w, h) = getScaledSize()
        return when (alignment) {
            Alignment.TOP_LEFT -> Vector2f(p.x, p.y)
            Alignment.TOP_CENTER -> Vector2f(p.x - w / 2f, p.y)
            Alignment.TOP_RIGHT -> Vector2f(p.x - w, p.y)
            Alignment.CENTER_LEFT -> Vector2f(p.x, p.y - h / 2f)
            Alignment.CENTER -> Vector2f(p.x - w / 2f, p.y - h / 2f)
            Alignment.CENTER_RIGHT -> Vector2f(p.x - w, p.y - h / 2f)
            Alignment.BOTTOM_LEFT -> Vector2f(p.x, p.y - h)
            Alignment.BOTTOM_CENTER -> Vector2f(p.x - w / 2f, p.y - h)
            Alignment.BOTTOM_RIGHT -> Vector2f(p.x - w, p.y - h)
        }
    }

    fun setRenderPosition(x: Float, y: Float) {
        renderPosition = Vector2f(x, y)
    }

    fun commitRenderPosition() {
        position.set(Vector2f(renderPosition.x, renderPosition.y))
    }

    /**
     * Sets a sensible default size for an element so the HUD editor can
     * hit-test and outline it before the element has been rendered once.
     */
    fun setEditorDefaultSize(width: Float, height: Float) {
        lastBaseWidth = width
        lastBaseHeight = height
    }

    enum class Alignment {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
    }
}

/**
 * Global registry of all [HudElement]s. The order of elements in the registry
 * is also the rendering order (back to front).
 */
object HudElementRegistry {

    private val elements = ArrayList<HudElement>()

    fun register(element: HudElement) {
        if (elements.none { it.id == element.id }) {
            elements.add(element)
        }
    }

    fun getAll(): List<HudElement> = elements

    fun byId(id: String): HudElement? = elements.firstOrNull { it.id == id }

    fun clear() {
        elements.clear()
    }

    fun initialize(screenWidth: Int, screenHeight: Int) {
        for (e in elements) {
            e.onInitialize(screenWidth, screenHeight)
        }
    }
}
