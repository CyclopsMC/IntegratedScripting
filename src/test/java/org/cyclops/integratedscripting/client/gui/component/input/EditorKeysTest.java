package org.cyclops.integratedscripting.client.gui.component.input;

import org.junit.jupiter.api.Test;
import com.mojang.blaze3d.platform.InputConstants;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author rubensworks
 */
public class EditorKeysTest {

    @Test
    public void testWordModifierMac() {
        assertTrue(EditorKeys.isWordModifier(InputConstants.MOD_ALT, true));
        assertTrue(EditorKeys.isWordModifier(InputConstants.MOD_ALT | InputConstants.MOD_SHIFT, true));
        assertFalse(EditorKeys.isWordModifier(InputConstants.MOD_SUPER, true));
        assertFalse(EditorKeys.isWordModifier(InputConstants.MOD_CONTROL, true));
        assertFalse(EditorKeys.isWordModifier(0, true));
    }

    @Test
    public void testWordModifierOthers() {
        assertTrue(EditorKeys.isWordModifier(InputConstants.MOD_CONTROL, false));
        assertTrue(EditorKeys.isWordModifier(InputConstants.MOD_CONTROL | InputConstants.MOD_SHIFT, false));
        assertFalse(EditorKeys.isWordModifier(InputConstants.MOD_ALT, false));
        assertFalse(EditorKeys.isWordModifier(0, false));
    }

    @Test
    public void testLineModifier() {
        assertTrue(EditorKeys.isLineModifier(InputConstants.MOD_SUPER, true));
        assertTrue(EditorKeys.isLineModifier(InputConstants.MOD_SUPER | InputConstants.MOD_SHIFT, true));
        assertFalse(EditorKeys.isLineModifier(InputConstants.MOD_ALT, true));
        assertFalse(EditorKeys.isLineModifier(InputConstants.MOD_SUPER, false));
        assertFalse(EditorKeys.isLineModifier(InputConstants.MOD_CONTROL, false));
    }

}
