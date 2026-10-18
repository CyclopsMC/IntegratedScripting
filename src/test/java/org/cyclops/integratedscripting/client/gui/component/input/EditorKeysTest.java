package org.cyclops.integratedscripting.client.gui.component.input;

import org.junit.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author rubensworks
 */
public class EditorKeysTest {

    @Test
    public void testWordModifierMac() {
        assertTrue(EditorKeys.isWordModifier(GLFW.GLFW_MOD_ALT, true));
        assertTrue(EditorKeys.isWordModifier(GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SHIFT, true));
        assertFalse(EditorKeys.isWordModifier(GLFW.GLFW_MOD_SUPER, true));
        assertFalse(EditorKeys.isWordModifier(GLFW.GLFW_MOD_CONTROL, true));
        assertFalse(EditorKeys.isWordModifier(0, true));
    }

    @Test
    public void testWordModifierOthers() {
        assertTrue(EditorKeys.isWordModifier(GLFW.GLFW_MOD_CONTROL, false));
        assertTrue(EditorKeys.isWordModifier(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT, false));
        assertFalse(EditorKeys.isWordModifier(GLFW.GLFW_MOD_ALT, false));
        assertFalse(EditorKeys.isWordModifier(0, false));
    }

    @Test
    public void testLineModifier() {
        assertTrue(EditorKeys.isLineModifier(GLFW.GLFW_MOD_SUPER, true));
        assertTrue(EditorKeys.isLineModifier(GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT, true));
        assertFalse(EditorKeys.isLineModifier(GLFW.GLFW_MOD_ALT, true));
        assertFalse(EditorKeys.isLineModifier(GLFW.GLFW_MOD_SUPER, false));
        assertFalse(EditorKeys.isLineModifier(GLFW.GLFW_MOD_CONTROL, false));
    }

}
