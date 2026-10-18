package org.cyclops.integratedscripting.client.gui.component.input;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo and redo history of a text area.
 * @author rubensworks
 */
public class TextHistory {

    public static final int MAX_SIZE = 500;
    public static final long TYPING_GROUP_MILLIS = 1000;

    private final Deque<State> undoStack = new ArrayDeque<>();
    private final Deque<State> redoStack = new ArrayDeque<>();
    private boolean lastWasTyping;
    private long lastRecordTime;
    private int lastTypingEnd = -1;

    /**
     * Record the state before an edit.
     * Consecutive typing at the same position within a short time is grouped into a single undo step.
     * @param before The state before the edit.
     * @param typing If the edit was caused by typing a single character.
     * @param time The current time in milliseconds.
     */
    public void record(State before, boolean typing, long time) {
        boolean group = typing && this.lastWasTyping && !this.undoStack.isEmpty()
                && time - this.lastRecordTime < TYPING_GROUP_MILLIS
                && Math.min(before.cursorPos(), before.selectionPos()) == this.lastTypingEnd;
        if (!group) {
            this.undoStack.push(before);
            if (this.undoStack.size() > MAX_SIZE) {
                this.undoStack.removeLast();
            }
        }
        this.redoStack.clear();
        this.lastWasTyping = typing;
        this.lastRecordTime = time;
        this.lastTypingEnd = typing ? Math.min(before.cursorPos(), before.selectionPos()) + 1 : -1;
    }

    /**
     * @param current The current state, which will become redoable.
     * @return The state to restore, or null if there is nothing to undo.
     */
    @Nullable
    public State undo(State current) {
        if (this.undoStack.isEmpty()) {
            return null;
        }
        this.redoStack.push(current);
        this.lastWasTyping = false;
        return this.undoStack.pop();
    }

    /**
     * @param current The current state, which will become undoable.
     * @return The state to restore, or null if there is nothing to redo.
     */
    @Nullable
    public State redo(State current) {
        if (this.redoStack.isEmpty()) {
            return null;
        }
        this.undoStack.push(current);
        this.lastWasTyping = false;
        return this.redoStack.pop();
    }

    public static record State(String value, int cursorPos, int selectionPos) {}

}
