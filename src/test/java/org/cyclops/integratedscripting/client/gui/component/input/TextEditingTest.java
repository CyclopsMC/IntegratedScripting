package org.cyclops.integratedscripting.client.gui.component.input;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
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

    @Test
    public void testNewlineKeepsIndent() {
        assertThat(TextEditing.newline(s("    a;", 6)), equalTo(s("    a;\n    ", 11)));
    }

    @Test
    public void testNewlineIndentsAfterOpeningBracket() {
        assertThat(TextEditing.newline(s("  if (a) {", 10)), equalTo(s("  if (a) {\n      ", 17)));
    }

    @Test
    public void testNewlineIndentsAfterOpeningBracketWithTrailingSpace() {
        assertThat(TextEditing.newline(s("f(", 2)), equalTo(s("f(\n    ", 7)));
    }

    @Test
    public void testNewlineSplitsBracketPair() {
        assertThat(TextEditing.newline(s("  {}", 3)), equalTo(s("  {\n      \n  }", 10)));
    }

    @Test
    public void testNewlineReplacesSelection() {
        assertThat(TextEditing.newline(s("abc", 1, 2)), equalTo(s("a\nc", 2)));
    }

    @Test
    public void testNewlineInMiddleOfIndent() {
        assertThat(TextEditing.newline(s("    a", 2)), equalTo(s("  \n    a", 5)));
    }

    @Test
    public void testMatchingBracketAfterCursor() {
        assertThat(TextEditing.findMatchingBrackets("f(a(b))", 1), equalTo(new int[]{1, 6}));
    }

    @Test
    public void testMatchingBracketBeforeCursor() {
        assertThat(TextEditing.findMatchingBrackets("f(a(b))", 7), equalTo(new int[]{1, 6}));
    }

    @Test
    public void testMatchingBracketNested() {
        assertThat(TextEditing.findMatchingBrackets("{[()]}", 1), equalTo(new int[]{1, 4}));
    }

    @Test
    public void testMatchingBracketUnbalanced() {
        assertThat(TextEditing.findMatchingBrackets("((a)", 0), nullValue());
    }

    @Test
    public void testMatchingBracketNone() {
        assertThat(TextEditing.findMatchingBrackets("abc", 1), nullValue());
    }

    @Test
    public void testFindNext() {
        assertThat(TextEditing.findNext("abc abc", "abc", 1), equalTo(4));
    }

    @Test
    public void testFindNextWraps() {
        assertThat(TextEditing.findNext("abc abc", "abc", 5), equalTo(0));
    }

    @Test
    public void testFindNextCaseInsensitive() {
        assertThat(TextEditing.findNext("const Foo", "foo", 0), equalTo(6));
    }

    @Test
    public void testFindNextMissing() {
        assertThat(TextEditing.findNext("abc", "x", 0), equalTo(-1));
        assertThat(TextEditing.findNext("abc", "", 0), equalTo(-1));
    }

    @Test
    public void testLineOffset() {
        assertThat(TextEditing.lineOffset("a\nbc\nd", 1), equalTo(0));
        assertThat(TextEditing.lineOffset("a\nbc\nd", 3), equalTo(5));
        assertThat(TextEditing.lineOffset("a\nbc\nd", 10), equalTo(5));
        assertThat(TextEditing.lineOffset("a\nbc\nd", 0), equalTo(0));
    }

    @Test
    public void testFindAll() {
        assertThat(TextEditing.findAll("Item item ITEM", "item"), equalTo(new int[]{0, 5, 10}));
        assertThat(TextEditing.findAll("aaaa", "aa"), equalTo(new int[]{0, 2}));
        assertThat(TextEditing.findAll("abc", "x"), equalTo(new int[0]));
        assertThat(TextEditing.findAll("abc", ""), equalTo(new int[0]));
    }

    @Test
    public void testFindPrevious() {
        assertThat(TextEditing.findPrevious("abc abc abc", "abc", 8), equalTo(4));
        assertThat(TextEditing.findPrevious("abc abc abc", "abc", 4), equalTo(0));
        assertThat(TextEditing.findPrevious("abc abc abc", "abc", 0), equalTo(8));
        assertThat(TextEditing.findPrevious("abc", "x", 2), equalTo(-1));
    }

}
