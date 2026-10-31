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

import java.util.List;

/**
 * Rebindable keyboard shortcuts of the script editor.
 * @author rubensworks
 */
public class Keys {

    // Same as the main category of the client proxy, which registers it
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "main"));

    public static final KeyMapping UNDO = create("undo", KeyModifier.CONTROL_OR_COMMAND, InputConstants.KEY_Z);
    public static final KeyMapping REDO = create("redo", KeyModifier.CONTROL_OR_COMMAND, InputConstants.KEY_Y);
    public static final KeyMapping FIND = create("find", KeyModifier.CONTROL_OR_COMMAND, InputConstants.KEY_F);
    public static final KeyMapping GOTO_LINE = create("goto_line", KeyModifier.CONTROL_OR_COMMAND, InputConstants.KEY_G);
    public static final KeyMapping COMMENT = create("comment", KeyModifier.CONTROL_OR_COMMAND, InputConstants.KEY_SLASH);
    public static final KeyMapping MOVE_LINES_UP = create("move_lines_up", KeyModifier.ALT, InputConstants.KEY_UP);
    public static final KeyMapping MOVE_LINES_DOWN = create("move_lines_down", KeyModifier.ALT, InputConstants.KEY_DOWN);
    // F11 is taken by the window fullscreen toggle of Minecraft
    public static final KeyMapping FOCUS_MODE = create("focus_mode", KeyModifier.NONE, InputConstants.KEY_F12);

    public static final List<KeyMapping> ALL = List.of(UNDO, REDO, FIND, GOTO_LINE, COMMENT, MOVE_LINES_UP, MOVE_LINES_DOWN, FOCUS_MODE);

    private static KeyMapping create(String name, KeyModifier modifier, int key) {
        return new KeyMapping("key." + Reference.MOD_ID + "." + name, KeyConflictContext.GUI, modifier,
                InputConstants.Type.KEYBOARD, key, CATEGORY);
    }

    /**
     * Check if a key event matches a key mapping.
     * Modifiers are read from the event instead of the global keyboard state, and must match exactly,
     * so that for example Ctrl+Shift+Z does not trigger a Ctrl+Z mapping.
     */
    public static boolean matches(KeyMapping keyMapping, int key, int keycode, int modifiers) {
        if (keyMapping.isUnbound() || !keyMapping.getKey().equals(InputConstants.getKey(new KeyEvent(key, keycode, modifiers)))) {
            return false;
        }
        // Each modifier has a bit for its left and its right key
        boolean control = (modifiers & InputConstants.MOD_CONTROL) != 0;
        boolean command = (modifiers & InputConstants.MOD_SUPER) != 0;
        boolean shift = (modifiers & InputConstants.MOD_SHIFT) != 0;
        boolean alt = (modifiers & InputConstants.MOD_ALT) != 0;
        return switch (keyMapping.getKeyModifier()) {
            case CONTROL -> control && !command && !shift && !alt;
            case CONTROL_OR_COMMAND -> (InputQuirks.ON_OSX ? command && !control : control && !command) && !shift && !alt;
            case SHIFT -> shift && !control && !command && !alt;
            case ALT -> alt && !control && !command && !shift;
            case NONE -> !control && !command && !shift && !alt;
        };
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
