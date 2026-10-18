package org.cyclops.integratedscripting.client.gui.component.input;

import org.lwjgl.glfw.GLFW;

/**
 * Platform conventions for moving the cursor in text editors.
 * @author rubensworks
 */
public final class EditorKeys {

    private EditorKeys() {
    }

    /**
     * @return If the cursor should move or delete by word: Option on macOS, Ctrl elsewhere.
     */
    public static boolean isWordModifier(int modifiers, boolean osx) {
        return (modifiers & (osx ? GLFW.GLFW_MOD_ALT : GLFW.GLFW_MOD_CONTROL)) != 0;
    }

    /**
     * @return If arrow keys should jump to the line or document start or end, which is Cmd on macOS.
     */
    public static boolean isLineModifier(int modifiers, boolean osx) {
        return osx && (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
    }

}
