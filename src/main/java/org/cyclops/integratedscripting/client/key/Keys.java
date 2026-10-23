package org.cyclops.integratedscripting.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.cyclops.integratedscripting.Reference;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Rebindable keyboard shortcuts of the script editor.
 * @author rubensworks
 */
public class Keys {

    // Same as the main category of the client proxy, which registers it
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "main"));

    public static final KeyMapping UNDO = create("undo", KeyModifier.CONTROL_OR_COMMAND, GLFW.GLFW_KEY_Z);
    public static final KeyMapping REDO = create("redo", KeyModifier.CONTROL_OR_COMMAND, GLFW.GLFW_KEY_Y);
    public static final KeyMapping FIND = create("find", KeyModifier.CONTROL_OR_COMMAND, GLFW.GLFW_KEY_F);
    public static final KeyMapping GOTO_LINE = create("goto_line", KeyModifier.CONTROL_OR_COMMAND, GLFW.GLFW_KEY_G);
    public static final KeyMapping COMMENT = create("comment", KeyModifier.CONTROL_OR_COMMAND, GLFW.GLFW_KEY_SLASH);
    public static final KeyMapping MOVE_LINES_UP = create("move_lines_up", KeyModifier.ALT, GLFW.GLFW_KEY_UP);
    public static final KeyMapping MOVE_LINES_DOWN = create("move_lines_down", KeyModifier.ALT, GLFW.GLFW_KEY_DOWN);
    // F11 is taken by the window fullscreen toggle of Minecraft
    public static final KeyMapping FOCUS_MODE = create("focus_mode", KeyModifier.NONE, GLFW.GLFW_KEY_F12);

    public static final List<KeyMapping> ALL = List.of(UNDO, REDO, FIND, GOTO_LINE, COMMENT, MOVE_LINES_UP, MOVE_LINES_DOWN, FOCUS_MODE);

    private static KeyMapping create(String name, KeyModifier modifier, int key) {
        return new KeyMapping("key." + Reference.MOD_ID + "." + name, KeyConflictContext.GUI, modifier,
                InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    /**
     * Check if a key event matches a key mapping.
     * Modifiers are read from the event instead of the global keyboard state, and must match exactly,
     * so that for example Ctrl+Shift+Z does not trigger a Ctrl+Z mapping.
     */
    public static boolean matches(KeyMapping keyMapping, int keyCode, int scanCode, int modifiers) {
        if (keyMapping.isUnbound() || !keyMapping.getKey().equals(InputConstants.getKey(new KeyEvent(keyCode, scanCode, modifiers)))) {
            return false;
        }
        int expected = switch (keyMapping.getKeyModifier()) {
            case CONTROL -> GLFW.GLFW_MOD_CONTROL;
            case CONTROL_OR_COMMAND -> InputQuirks.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
            case SHIFT -> GLFW.GLFW_MOD_SHIFT;
            case ALT -> GLFW.GLFW_MOD_ALT;
            case NONE -> 0;
        };
        return (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT)) == expected;
    }

    /**
     * @return The name of the key mapping, followed by its current key, if bound.
     */
    public static MutableComponent getTooltip(KeyMapping keyMapping) {
        MutableComponent tooltip = Component.translatable(keyMapping.getName());
        if (!keyMapping.isUnbound()) {
            tooltip.append(Component.literal(" (").append(keyMapping.getTranslatedKeyMessage()).append(")").withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }

}
