package org.cyclops.integratedscripting.client.gui.component.input;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class TextEditingTest {

    private static final String FOUR = "    ";
    private static final String TWO = "  ";

    private static TextHistory.State s(String value, int cursor) {
        return new TextHistory.State(value, cursor, cursor);
    }

    private static TextHistory.State s(String value, int cursor, int selection) {
        return new TextHistory.State(value, cursor, selection);
    }

    @Test
    public void testIndentAtCursor() {
        assertThat(TextEditing.indent(s("ab", 1), FOUR), equalTo(s("a    b", 5)));
    }

    @Test
    public void testIndentReplacesSingleLineSelection() {
        assertThat(TextEditing.indent(s("abc", 1, 2), FOUR), equalTo(s("a    c", 5)));
    }

    @Test
    public void testIndentMultipleLines() {
        assertThat(TextEditing.indent(s("a\nb\nc", 0, 3), FOUR), equalTo(s("    a\n    b\nc", 4, 11)));
    }

    @Test
    public void testIndentMultipleLinesSelectionEndingAtLineStart() {
        assertThat(TextEditing.indent(s("a\nb\nc", 0, 4), FOUR), equalTo(s("    a\n    b\nc", 4, 12)));
    }

    @Test
    public void testUnindentCurrentLine() {
        assertThat(TextEditing.unindent(s("a\n      b", 9), FOUR), equalTo(s("a\n  b", 5)));
    }

    @Test
    public void testUnindentCursorInsideIndent() {
        assertThat(TextEditing.unindent(s("    b", 2), FOUR), equalTo(s("b", 0)));
    }

    @Test
    public void testUnindentPartialIndent() {
        assertThat(TextEditing.unindent(s("  b", 3), FOUR), equalTo(s("b", 1)));
    }

    @Test
    public void testUnindentNoIndent() {
        assertThat(TextEditing.unindent(s("b", 1), FOUR), equalTo(s("b", 1)));
    }

    @Test
    public void testUnindentMultipleLines() {
        assertThat(TextEditing.unindent(s("    a\n  b\nc", 0, 11), FOUR), equalTo(s("a\nb\nc", 0, 5)));
    }

    @Test
    public void testNewlineKeepsIndent() {
        assertThat(TextEditing.newline(s("    a;", 6), FOUR), equalTo(s("    a;\n    ", 11)));
    }

    @Test
    public void testNewlineIndentsAfterOpeningBracket() {
        assertThat(TextEditing.newline(s("  if (a) {", 10), FOUR), equalTo(s("  if (a) {\n      ", 17)));
    }

    @Test
    public void testNewlineIndentsAfterOpeningBracketWithTrailingSpace() {
        assertThat(TextEditing.newline(s("f(", 2), FOUR), equalTo(s("f(\n    ", 7)));
    }

    @Test
    public void testNewlineSplitsBracketPair() {
        assertThat(TextEditing.newline(s("  {}", 3), FOUR), equalTo(s("  {\n      \n  }", 10)));
    }

    @Test
    public void testNewlineReplacesSelection() {
        assertThat(TextEditing.newline(s("abc", 1, 2), FOUR), equalTo(s("a\nc", 2)));
    }

    @Test
    public void testNewlineInMiddleOfIndent() {
        assertThat(TextEditing.newline(s("    a", 2), FOUR), equalTo(s("  \n    a", 5)));
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

    @Test
    public void testToggleCommentAdds() {
        assertThat(TextEditing.toggleComment(s("a\nb", 3)), equalTo(s("a\n//b", 5)));
    }

    @Test
    public void testToggleCommentRemoves() {
        assertThat(TextEditing.toggleComment(s("//a\n//b", 0, 7)), equalTo(s("a\nb", 0, 3)));
    }

    @Test
    public void testToggleCommentMixedAdds() {
        assertThat(TextEditing.toggleComment(s("//a\nb", 0, 5)), equalTo(s("////a\n//b", 2, 9)));
    }

    @Test
    public void testMoveLineUp() {
        assertThat(TextEditing.moveLines(s("a\nbc\nd", 4), -1), equalTo(s("bc\na\nd", 2)));
    }

    @Test
    public void testMoveLineUpAtTop() {
        assertThat(TextEditing.moveLines(s("a\nb", 1), -1), equalTo(s("a\nb", 1)));
    }

    @Test
    public void testMoveLineDown() {
        assertThat(TextEditing.moveLines(s("a\nbc\nd", 0), 1), equalTo(s("bc\na\nd", 3)));
    }

    @Test
    public void testMoveLineDownAtBottom() {
        assertThat(TextEditing.moveLines(s("a\nb", 2), 1), equalTo(s("a\nb", 2)));
    }

    @Test
    public void testMoveMultipleLinesDown() {
        assertThat(TextEditing.moveLines(s("a\nb\nc", 0, 3), 1), equalTo(s("c\na\nb", 2, 5)));
    }

    @Test
    public void testIndentTwoSpaces() {
        assertThat(TextEditing.indent(s("ab", 1), TWO), equalTo(s("a  b", 3)));
        assertThat(TextEditing.indent(s("a\nb", 0, 3), TWO), equalTo(s("  a\n  b", 2, 7)));
    }

    @Test
    public void testUnindentTwoSpaces() {
        assertThat(TextEditing.unindent(s("    b", 5), TWO), equalTo(s("  b", 3)));
    }

    @Test
    public void testNewlineTwoSpaces() {
        assertThat(TextEditing.newline(s("  if (a) {", 10), TWO), equalTo(s("  if (a) {\n    ", 15)));
    }


    private static int[] wrap(String value, int width) {
        return TextEditing.wrapLines(value, width, c -> 1);
    }

    @Test
    public void testWrapLinesFits() {
        assertThat(wrap("", 10), equalTo(new int[]{0}));
        assertThat(wrap("abc\ndef", 10), equalTo(new int[]{0, 4}));
        assertThat(wrap("abc\n", 10), equalTo(new int[]{0}));
        assertThat(wrap("a\n\nb", 10), equalTo(new int[]{0, 2, 3}));
    }

    @Test
    public void testWrapLinesAtSpace() {
        // Trailing spaces stay on the line
        assertThat(wrap("return abc", 8), equalTo(new int[]{0, 7}));
        assertThat(wrap("a   bc", 3), equalTo(new int[]{0, 4}));
    }

    @Test
    public void testWrapLinesAtPunctuation() {
        assertThat(wrap("ctx.ops.size(item)", 10), equalTo(new int[]{0, 7, 13}));
        assertThat(wrap("abc,def", 5), equalTo(new int[]{0, 4}));
    }

    @Test
    public void testWrapLinesKeepsOperatorsAndNumbers() {
        assertThat(wrap("a>=b", 2), equalTo(new int[]{0, 1, 3}));
        assertThat(wrap("x=12.5", 5), equalTo(new int[]{0, 2}));
    }

    @Test
    public void testWrapLinesSplitsLongTokens() {
        assertThat(wrap("abcdefgh", 3), equalTo(new int[]{0, 3, 6}));
    }

}
