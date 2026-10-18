package org.cyclops.integratedscripting.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    public static final String CATEGORY = "key.categories." + Reference.MOD_ID;

    public static final KeyMapping UNDO = create("undo", KeyModifier.CONTROL, GLFW.GLFW_KEY_Z);
    public static final KeyMapping REDO = create("redo", KeyModifier.CONTROL, GLFW.GLFW_KEY_Y);
    public static final KeyMapping FIND = create("find", KeyModifier.CONTROL, GLFW.GLFW_KEY_F);
    public static final KeyMapping GOTO_LINE = create("goto_line", KeyModifier.CONTROL, GLFW.GLFW_KEY_G);
    public static final KeyMapping COMMENT = create("comment", KeyModifier.CONTROL, GLFW.GLFW_KEY_SLASH);
    public static final KeyMapping MOVE_LINES_UP = create("move_lines_up", KeyModifier.ALT, GLFW.GLFW_KEY_UP);
    public static final KeyMapping MOVE_LINES_DOWN = create("move_lines_down", KeyModifier.ALT, GLFW.GLFW_KEY_DOWN);

    public static final List<KeyMapping> ALL = List.of(UNDO, REDO, FIND, GOTO_LINE, COMMENT, MOVE_LINES_UP, MOVE_LINES_DOWN);

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
        if (keyMapping.isUnbound() || !keyMapping.getKey().equals(InputConstants.getKey(keyCode, scanCode))) {
            return false;
        }
        int control = Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
        int expected = switch (keyMapping.getKeyModifier()) {
            case CONTROL -> control;
            case SHIFT -> GLFW.GLFW_MOD_SHIFT;
            case ALT -> GLFW.GLFW_MOD_ALT;
            case NONE -> 0;
        };
        return (modifiers & (control | GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT)) == expected;
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
