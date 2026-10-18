package org.cyclops.integratedscripting.client.key;

import net.minecraft.client.Minecraft;
import org.junit.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author rubensworks
 */
public class KeysTest {

    private static final int CONTROL = Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;

    @Test
    public void testMatchesKeyWithModifier() {
        assertTrue(Keys.matches(Keys.UNDO, GLFW.GLFW_KEY_Z, 0, CONTROL));
        assertTrue(Keys.matches(Keys.MOVE_LINES_UP, GLFW.GLFW_KEY_UP, 0, GLFW.GLFW_MOD_ALT));
    }

    @Test
    public void testIgnoresLockModifiers() {
        assertTrue(Keys.matches(Keys.UNDO, GLFW.GLFW_KEY_Z, 0, CONTROL | GLFW.GLFW_MOD_NUM_LOCK | GLFW.GLFW_MOD_CAPS_LOCK));
    }

    @Test
    public void testRejectsMissingOrOtherModifiers() {
        assertFalse(Keys.matches(Keys.UNDO, GLFW.GLFW_KEY_Z, 0, 0));
        assertFalse(Keys.matches(Keys.UNDO, GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_ALT));
        assertFalse(Keys.matches(Keys.MOVE_LINES_UP, GLFW.GLFW_KEY_UP, 0, 0));
    }

    @Test
    public void testRejectsExtraModifiers() {
        // Ctrl+Shift+Z is redo, so it must not undo
        assertFalse(Keys.matches(Keys.UNDO, GLFW.GLFW_KEY_Z, 0, CONTROL | GLFW.GLFW_MOD_SHIFT));
        assertFalse(Keys.matches(Keys.MOVE_LINES_UP, GLFW.GLFW_KEY_UP, 0, GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SHIFT));
    }

    @Test
    public void testRejectsOtherKeys() {
        assertFalse(Keys.matches(Keys.UNDO, GLFW.GLFW_KEY_Y, 0, CONTROL));
    }

}
