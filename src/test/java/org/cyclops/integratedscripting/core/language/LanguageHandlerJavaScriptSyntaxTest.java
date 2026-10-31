package org.cyclops.integratedscripting.core.language;

import org.cyclops.integratedscripting.api.language.ScriptSyntaxError;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LanguageHandlerJavaScriptSyntaxTest {

    private static ScriptSyntaxError check(String script) {
        return new LanguageHandlerJavaScript().checkSyntax(Path.of("script0.js"), script);
    }

    @Test
    public void testValid() {
        assertThat(check("const a = 1;\nfunction f(b) { return a + b; }"), nullValue());
    }

    @Test
    public void testValidDoesNotExecute() {
        assertThat(check("throw new Error('runtime');"), nullValue());
    }

    @Test
    public void testSyntaxError() {
        ScriptSyntaxError error = check("const a = 1;\nconst = ;");
        assertThat(error.line(), equalTo(2));
        assertThat(error.column(), equalTo(7));
        assertTrue(error.message().contains("SyntaxError"), error.message());
    }

    @Test
    public void testUnclosedBracket() {
        ScriptSyntaxError error = check("function f() {\n  return 1;\n");
        assertTrue(error.message().contains("SyntaxError"), error.message());
    }

}
