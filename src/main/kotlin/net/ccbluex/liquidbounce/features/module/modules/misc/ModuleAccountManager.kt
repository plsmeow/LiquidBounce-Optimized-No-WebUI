package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import org.lwjgl.glfw.GLFW

object ModuleAccountManager : ClientModule(
    "AccountManager",
    ModuleCategories.MISC,
    bind = GLFW.GLFW_KEY_UNKNOWN,
    disableActivation = true
) {

    override val running get() = true
}