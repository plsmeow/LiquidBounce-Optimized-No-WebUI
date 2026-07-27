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

package net.ccbluex.liquidbounce.utils.client.error

class Instructions(
    val showStepIndex: Boolean,
    val steps: (error: Throwable) -> Array<String>?
)

enum class QuickFix (
    val description: String,
    val testError: (error: Throwable) -> Boolean = { false },
    val whatYouNeed: Instructions? = null,
    val whatToDo: Instructions? = null
) {
    CLASS_NOT_FOUND(
        description = "Some class not found",
        testError = { it is ClassNotFoundException },
        whatYouNeed = Instructions(false) { _ ->
            arrayOf(
                "Make sure you have all the libraries required by minecraft installed"
            )
        },
        whatToDo = Instructions(false) {
            val message = it.message
            if (message == null) {
                null
            } else {
                when {
                    message.contains("viaversion") -> arrayOf("Try to install ViaFabric")
                    message.contains("modmenu") -> arrayOf("Try to install ModMenu")
                    else -> null
                }
            }
        }
    ),
    D3D11_UNSATISFIED_LINK(
        description = "D3D11 not installed",
        testError = { throwable ->
            throwable is UnsatisfiedLinkError && throwable.message?.contains("d3dcompiler_47.dll") == true
        },
        whatToDo = Instructions(true) {
            // Tracking issue: https://github.com/CCBlueX/LiquidBounce/issues/6841
            // For some reason, this seems to always happen for Russian users.
            // We were never able to reproduce this on a clean Windows install.
            arrayOf(
                "Install Windows Updates",
                "Install DirectX End-User Runtime",
                "Install C++ Redistributable for Visual Studio 2017–2026",
                "Restart LiquidBounce and try again."
            )
        }
    );

    val messages = mapOf(
        "What you need" to whatYouNeed,
        "What to do" to whatToDo
    ).filter { it.value != null }
}
