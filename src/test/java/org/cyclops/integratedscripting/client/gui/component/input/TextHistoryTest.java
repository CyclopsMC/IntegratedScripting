package org.cyclops.integratedscripting.client.gui.component.input;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class TextHistoryTest {

    private static TextHistory.State s(String value, int cursor) {
        return new TextHistory.State(value, cursor, cursor);
    }

    @Test
    public void testEmpty() {
        TextHistory history = new TextHistory();
        assertThat(history.undo(s("a", 1)), nullValue());
        assertThat(history.redo(s("a", 1)), nullValue());
    }

    @Test
    public void testUndoRedo() {
        TextHistory history = new TextHistory();
        history.record(s("", 0), false, 0);
        assertThat(history.undo(s("pasted", 6)), equalTo(s("", 0)));
        assertThat(history.undo(s("", 0)), nullValue());
        assertThat(history.redo(s("", 0)), equalTo(s("pasted", 6)));
        assertThat(history.redo(s("pasted", 6)), nullValue());
    }

    @Test
    public void testGroupsConsecutiveTyping() {
        TextHistory history = new TextHistory();
        history.record(s("", 0), true, 0);
        history.record(s("a", 1), true, 100);
        history.record(s("ab", 2), true, 200);
        assertThat(history.undo(s("abc", 3)), equalTo(s("", 0)));
        assertThat(history.undo(s("", 0)), nullValue());
    }

    @Test
    public void testDoesNotGroupTypingAfterPause() {
        TextHistory history = new TextHistory();
        history.record(s("", 0), true, 0);
        history.record(s("a", 1), true, 5000);
        assertThat(history.undo(s("ab", 2)), equalTo(s("a", 1)));
        assertThat(history.undo(s("a", 1)), equalTo(s("", 0)));
    }

    @Test
    public void testDoesNotGroupTypingAtOtherPosition() {
        TextHistory history = new TextHistory();
        history.record(s("xy", 0), true, 0);
        history.record(s("axy", 3), true, 100);
        assertThat(history.undo(s("axyb", 4)), equalTo(s("axy", 3)));
    }

    @Test
    public void testDoesNotGroupTypingWithOtherEdits() {
        TextHistory history = new TextHistory();
        history.record(s("", 0), true, 0);
        history.record(s("a", 1), false, 100);
        history.record(s("a\n", 2), true, 200);
        assertThat(history.undo(s("a\nb", 3)), equalTo(s("a\n", 2)));
        assertThat(history.undo(s("a\n", 2)), equalTo(s("a", 1)));
        assertThat(history.undo(s("a", 1)), equalTo(s("", 0)));
    }

    @Test
    public void testNewEditClearsRedo() {
        TextHistory history = new TextHistory();
        history.record(s("", 0), false, 0);
        history.undo(s("a", 1));
        history.record(s("", 0), false, 100);
        assertThat(history.redo(s("b", 1)), nullValue());
    }

    @Test
    public void testMaxSize() {
        TextHistory history = new TextHistory();
        for (int i = 0; i < TextHistory.MAX_SIZE + 10; i++) {
            history.record(s(String.valueOf(i), 0), false, i * 10000L);
        }
        TextHistory.State last = null;
        TextHistory.State state;
        while ((state = history.undo(s("current", 0))) != null) {
            last = state;
        }
        assertThat(last, equalTo(s("10", 0)));
    }

}
