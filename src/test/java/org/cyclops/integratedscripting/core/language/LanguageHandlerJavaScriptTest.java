package org.cyclops.integratedscripting.core.language;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author rubensworks
 */
public class LanguageHandlerJavaScriptTest {

    private static Context CTX;

    @BeforeAll
    public static void beforeClass() {
        CTX = Context.newBuilder("js").build();
    }

    @AfterAll
    public static void afterClass() {
        CTX.close();
    }

    private static boolean isValidVariableName(String name) {
        try {
            CTX.parse(Source.create("js", "var " + name + " = 1;"));
            return true;
        } catch (PolyglotException e) {
            if (e.isSyntaxError()) {
                return false;
            }
            throw e;
        }
    }

    @Test
    public void testReservedWordsAreNoValidNames() {
        for (String word : LanguageHandlerJavaScript.RESERVED_WORDS) {
            if (isValidVariableName(word)) {
                fail("\"" + word + "\" is a valid name in a script, so it must not be reserved");
            }
        }
    }

    @Test
    public void testValidNamesAreNotReserved() {
        for (String name : new String[]{"double", "offset", "of", "async", "get", "set", "let", "static", "yield", "await"}) {
            assertTrue(isValidVariableName(name), name);
            assertFalse(LanguageHandlerJavaScript.RESERVED_WORDS.contains(name), name);
        }
    }

}
