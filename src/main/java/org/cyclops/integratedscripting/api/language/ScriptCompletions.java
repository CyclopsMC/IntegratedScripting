package org.cyclops.integratedscripting.api.language;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Completion candidates at a position in a script.
 * @param start The position from which the text before the cursor is replaced when accepting a candidate.
 * @param items The candidates.
 * @author rubensworks
 */
public record ScriptCompletions(int start, List<Item> items) {

    /**
     * @param name The text to insert.
     * @param detail A short description, such as a signature.
     */
    public record Item(String name, Component detail) {
    }

}
