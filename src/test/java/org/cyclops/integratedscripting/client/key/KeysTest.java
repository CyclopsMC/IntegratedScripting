package org.cyclops.integratedscripting.client.key;

import net.minecraft.client.input.InputQuirks;
import org.junit.jupiter.api.Test;
import com.mojang.blaze3d.platform.InputConstants;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author rubensworks
 */
public class KeysTest {

    private static final int CONTROL = InputQuirks.ON_OSX ? InputConstants.MOD_SUPER : InputConstants.MOD_CONTROL;

    @Test
    public void testMatchesKeyWithModifier() {
        assertTrue(Keys.matches(Keys.UNDO, InputConstants.KEY_Z, 0, CONTROL));
        assertTrue(Keys.matches(Keys.MOVE_LINES_UP, InputConstants.KEY_UP, 0, InputConstants.MOD_ALT));
    }

    @Test
    public void testIgnoresLockModifiers() {
        assertTrue(Keys.matches(Keys.UNDO, InputConstants.KEY_Z, 0, CONTROL | InputConstants.MOD_NUM_LOCK | InputConstants.MOD_CAPS_LOCK));
    }

    @Test
    public void testRejectsMissingOrOtherModifiers() {
        assertFalse(Keys.matches(Keys.UNDO, InputConstants.KEY_Z, 0, 0));
        assertFalse(Keys.matches(Keys.UNDO, InputConstants.KEY_Z, 0, InputConstants.MOD_ALT));
        assertFalse(Keys.matches(Keys.MOVE_LINES_UP, InputConstants.KEY_UP, 0, 0));
    }

    @Test
    public void testRejectsExtraModifiers() {
        // Ctrl+Shift+Z is redo, so it must not undo
        assertFalse(Keys.matches(Keys.UNDO, InputConstants.KEY_Z, 0, CONTROL | InputConstants.MOD_SHIFT));
        assertFalse(Keys.matches(Keys.MOVE_LINES_UP, InputConstants.KEY_UP, 0, InputConstants.MOD_ALT | InputConstants.MOD_SHIFT));
    }

    @Test
    public void testRejectsOtherKeys() {
        assertFalse(Keys.matches(Keys.UNDO, InputConstants.KEY_Y, 0, CONTROL));
    }

}
