package org.cyclops.integratedscripting.client.gui.component.input;

/**
 * Code editing operations on the state of a text area.
 * @author rubensworks
 */
public class TextEditing {

    public static final String INDENT = "    ";

    private static int start(TextHistory.State state) {
        return Math.min(state.cursorPos(), state.selectionPos());
    }

    private static int end(TextHistory.State state) {
        return Math.max(state.cursorPos(), state.selectionPos());
    }

    public static int lineStart(String value, int pos) {
        return value.lastIndexOf('\n', pos - 1) + 1;
    }

    public static int lineEnd(String value, int pos) {
        int i = value.indexOf('\n', pos);
        return i < 0 ? value.length() : i;
    }

    private static String leadingWhitespace(String value, int lineStart) {
        int i = lineStart;
        while (i < value.length() && (value.charAt(i) == ' ' || value.charAt(i) == '\t')) {
            i++;
        }
        return value.substring(lineStart, i);
    }

    private static boolean isMultiLineSelection(TextHistory.State state) {
        return state.value().substring(start(state), end(state)).indexOf('\n') >= 0;
    }

    /**
     * Indent the selected lines, or insert an indent at the cursor if no lines are selected.
     */
    public static TextHistory.State indent(TextHistory.State state) {
        String value = state.value();
        if (!isMultiLineSelection(state)) {
            int start = start(state);
            String newValue = value.substring(0, start) + INDENT + value.substring(end(state));
            return new TextHistory.State(newValue, start + INDENT.length(), start + INDENT.length());
        }
        return mapLines(state, line -> INDENT + line);
    }

    /**
     * Remove one indent from the start of the lines touched by the cursor or selection.
     */
    public static TextHistory.State unindent(TextHistory.State state) {
        return mapLines(state, line -> {
            int remove = 0;
            while (remove < INDENT.length() && remove < line.length() && line.charAt(remove) == ' ') {
                remove++;
            }
            if (remove == 0 && !line.isEmpty() && line.charAt(0) == '\t') {
                remove = 1;
            }
            return line.substring(remove);
        });
    }

    /**
     * Apply a function to each line touched by the cursor or selection,
     * keeping the cursor and selection on the same characters.
     */
    private static TextHistory.State mapLines(TextHistory.State state, java.util.function.UnaryOperator<String> mapper) {
        String value = state.value();
        int from = lineStart(value, start(state));
        // A selection ending at the start of a line doesn't include that line
        int endPos = end(state) > start(state) && value.charAt(end(state) - 1) == '\n' ? end(state) - 1 : end(state);
        int to = lineEnd(value, endPos);
        String[] lines = value.substring(from, to).split("\n", -1);
        String[] newLines = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            newLines[i] = mapper.apply(lines[i]);
        }
        String newValue = value.substring(0, from) + String.join("\n", newLines) + value.substring(to);
        return new TextHistory.State(newValue,
                mapPosition(state.cursorPos(), from, lines, newLines),
                mapPosition(state.selectionPos(), from, lines, newLines));
    }

    /**
     * Map a position from before to after changing lines, where each line only changed at its start.
     */
    private static int mapPosition(int pos, int from, String[] lines, String[] newLines) {
        if (pos < from) {
            return pos;
        }
        int lineStart = from;
        int delta = 0;
        for (int i = 0; i < lines.length; i++) {
            int lineDelta = newLines[i].length() - lines[i].length();
            int lineEnd = lineStart + lines[i].length();
            if (pos <= lineEnd || i == lines.length - 1) {
                // Positions inside a removed prefix move to the start of the line
                return Math.max(lineStart + delta, pos + delta + lineDelta);
            }
            delta += lineDelta;
            lineStart = lineEnd + 1;
        }
        return pos + delta;
    }

    /**
     * Insert a newline that keeps the indentation of the current line,
     * and indents one more level after an opening bracket.
     */
    public static TextHistory.State newline(TextHistory.State state) {
        String value = state.value();
        int start = start(state);
        int end = end(state);
        int lineStart = lineStart(value, start);
        String indent = leadingWhitespace(value, lineStart);
        indent = indent.substring(0, Math.min(indent.length(), start - lineStart));
        String before = value.substring(lineStart, start).stripTrailing();
        char open = before.isEmpty() ? 0 : before.charAt(before.length() - 1);
        boolean opens = open == '{' || open == '(' || open == '[';
        String insert = "\n" + indent + (opens ? INDENT : "");
        int cursor = start + insert.length();
        // Put a directly following closing bracket on its own line
        if (opens && end < value.length() && value.charAt(end) == closing(open)) {
            insert += "\n" + indent;
        }
        String newValue = value.substring(0, start) + insert + value.substring(end);
        return new TextHistory.State(newValue, cursor, cursor);
    }

    private static char closing(char open) {
        return switch (open) {
            case '{' -> '}';
            case '(' -> ')';
            case '[' -> ']';
            default -> 0;
        };
    }

    private static char opening(char close) {
        return switch (close) {
            case '}' -> '{';
            case ')' -> '(';
            case ']' -> '[';
            default -> 0;
        };
    }

    /**
     * Find the bracket matching the one right after or right before the cursor.
     * @return A pair of bracket positions, or null if there is no matching bracket.
     */
    public static int[] findMatchingBrackets(String value, int cursorPos) {
        for (int pos : new int[]{cursorPos, cursorPos - 1}) {
            if (pos < 0 || pos >= value.length()) {
                continue;
            }
            char c = value.charAt(pos);
            if (closing(c) != 0) {
                int match = scan(value, pos, c, closing(c), 1);
                return match < 0 ? null : new int[]{pos, match};
            } else if (opening(c) != 0) {
                int match = scan(value, pos, c, opening(c), -1);
                return match < 0 ? null : new int[]{match, pos};
            }
        }
        return null;
    }

    private static int scan(String value, int from, char self, char other, int step) {
        int depth = 0;
        for (int i = from; i >= 0 && i < value.length(); i += step) {
            char c = value.charAt(i);
            if (c == self) {
                depth++;
            } else if (c == other && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

}
