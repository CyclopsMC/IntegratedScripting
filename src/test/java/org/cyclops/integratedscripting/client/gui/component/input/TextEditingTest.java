package org.cyclops.integratedscripting.client.gui.component.input;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TextEditingTest {

    private static TextHistory.State s(String value, int cursor) {
        return new TextHistory.State(value, cursor, cursor);
    }

    private static TextHistory.State s(String value, int cursor, int selection) {
        return new TextHistory.State(value, cursor, selection);
    }

    @Test
    public void testIndentAtCursor() {
        assertThat(TextEditing.indent(s("ab", 1)), equalTo(s("a    b", 5)));
    }

    @Test
    public void testIndentReplacesSingleLineSelection() {
        assertThat(TextEditing.indent(s("abc", 1, 2)), equalTo(s("a    c", 5)));
    }

    @Test
    public void testIndentMultipleLines() {
        assertThat(TextEditing.indent(s("a\nb\nc", 0, 3)), equalTo(s("    a\n    b\nc", 4, 11)));
    }

    @Test
    public void testIndentMultipleLinesSelectionEndingAtLineStart() {
        assertThat(TextEditing.indent(s("a\nb\nc", 0, 4)), equalTo(s("    a\n    b\nc", 4, 12)));
    }

    @Test
    public void testUnindentCurrentLine() {
        assertThat(TextEditing.unindent(s("a\n      b", 9)), equalTo(s("a\n  b", 5)));
    }

    @Test
    public void testUnindentCursorInsideIndent() {
        assertThat(TextEditing.unindent(s("    b", 2)), equalTo(s("b", 0)));
    }

    @Test
    public void testUnindentPartialIndent() {
        assertThat(TextEditing.unindent(s("  b", 3)), equalTo(s("b", 1)));
    }

    @Test
    public void testUnindentNoIndent() {
        assertThat(TextEditing.unindent(s("b", 1)), equalTo(s("b", 1)));
    }

    @Test
    public void testUnindentMultipleLines() {
        assertThat(TextEditing.unindent(s("    a\n  b\nc", 0, 11)), equalTo(s("a\nb\nc", 0, 5)));
    }

}
