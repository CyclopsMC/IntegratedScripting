package org.cyclops.integratedscripting.core.language;

import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Finds the names declared at the top level of a JavaScript script, without executing or fully parsing it.
 * @author rubensworks
 */
public class JavaScriptDeclarations {

    // Characters after which a slash starts a regex instead of a division
    private static final String REGEX_PRECEDERS = "(,=:[!&|?{};+-*%<>~^";
    // Characters after which a function or class is an expression instead of a declaration
    private static final String EXPRESSION_PRECEDERS = "(,=:[!&|?+-*%<>~^";

    /**
     * @param script A script.
     * @return The names of the top-level functions, classes and variables,
     *         or null if the script contains declarations that are not supported, such as destructuring.
     */
    @Nullable
    public static Set<String> find(String script) {
        Set<String> names = Sets.newHashSet();
        int depth = 0;
        int length = script.length();
        // The last significant character, to detect member accesses and regexes
        char previous = ';';
        String previousWord = "";
        // If a name is expected next, after a declaration keyword or a comma in a variable declaration
        boolean expectName = false;
        boolean inVariableDeclaration = false;
        int i = 0;
        while (i < length) {
            char c = script.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '/' && i + 1 < length && script.charAt(i + 1) == '/') {
                int end = script.indexOf('\n', i);
                i = end < 0 ? length : end;
            } else if (c == '/' && i + 1 < length && script.charAt(i + 1) == '*') {
                int end = script.indexOf("*/", i + 2);
                i = end < 0 ? length : end + 2;
            } else if (c == '"' || c == '\'' || c == '`'
                    || (c == '/' && (REGEX_PRECEDERS.indexOf(previous) >= 0 || previousWord.equals("return")))) {
                i = skipQuoted(script, i, c);
                previous = 'a';
                previousWord = "";
                expectName = false;
            } else if (Character.isJavaIdentifierStart(c)) {
                int start = i;
                while (i < length && Character.isJavaIdentifierPart(script.charAt(i))) {
                    i++;
                }
                String word = script.substring(start, i);
                if (depth == 0 && previous != '.') {
                    if (expectName) {
                        names.add(word);
                        expectName = false;
                    } else if ((word.equals("function") || word.equals("class"))
                            && EXPRESSION_PRECEDERS.indexOf(previous) < 0 && !previousWord.equals("return")) {
                        expectName = true;
                        inVariableDeclaration = false;
                    } else if (word.equals("var") || word.equals("let") || word.equals("const")) {
                        expectName = true;
                        inVariableDeclaration = true;
                    }
                }
                previous = 'a';
                previousWord = word;
            } else {
                if (depth == 0) {
                    if (expectName && (c == '{' || c == '[') && inVariableDeclaration) {
                        // Destructuring
                        return null;
                    }
                    if (c == ';') {
                        inVariableDeclaration = false;
                    } else if (c == ',' && inVariableDeclaration) {
                        expectName = true;
                    } else if (c != '*') {
                        // Allow generator functions
                        expectName = false;
                    }
                }
                if (c == '{' || c == '(' || c == '[') {
                    depth++;
                } else if ((c == '}' || c == ')' || c == ']') && depth > 0) {
                    depth--;
                }
                previous = c;
                previousWord = "";
                i++;
            }
        }
        return names;
    }

    private static int skipQuoted(String script, int start, char quote) {
        int i = start + 1;
        while (i < script.length()) {
            char c = script.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == quote) {
                return i + 1;
            } else if (c == '\n' && quote != '`') {
                // Unterminated
                return i;
            } else {
                i++;
            }
        }
        return i;
    }

}
