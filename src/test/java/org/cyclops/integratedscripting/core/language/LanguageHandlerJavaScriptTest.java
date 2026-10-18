package org.cyclops.integratedscripting.core.language;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author rubensworks
 */
public class LanguageHandlerJavaScriptTest {

    private static Context CTX;

    @BeforeClass
    public static void beforeClass() {
        CTX = Context.newBuilder("js").build();
    }

    @AfterClass
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
            assertTrue(name, isValidVariableName(name));
            assertFalse(name, LanguageHandlerJavaScript.RESERVED_WORDS.contains(name));
        }
    }

}
