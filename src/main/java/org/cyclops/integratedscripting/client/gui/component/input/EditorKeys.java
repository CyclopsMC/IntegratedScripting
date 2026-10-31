package org.cyclops.integratedscripting.client.gui.component.input;

import com.mojang.blaze3d.platform.InputConstants;

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
        return (modifiers & (osx ? InputConstants.MOD_ALT : InputConstants.MOD_CONTROL)) != 0;
    }

    /**
     * @return If arrow keys should jump to the line or document start or end, which is Cmd on macOS.
     */
    public static boolean isLineModifier(int modifiers, boolean osx) {
        return osx && (modifiers & InputConstants.MOD_SUPER) != 0;
    }

}
