package org.cyclops.integratedscripting.api.language;

/**
 * A syntax error in a script.
 * @param message The error message.
 * @param line The 1-based line of the error.
 * @param column The 1-based column of the error.
 * @author rubensworks
 */
public record ScriptSyntaxError(String message, int line, int column) {
}
