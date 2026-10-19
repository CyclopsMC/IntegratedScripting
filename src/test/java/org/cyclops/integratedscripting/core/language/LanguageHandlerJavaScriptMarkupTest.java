package org.cyclops.integratedscripting.core.language;

import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class LanguageHandlerJavaScriptMarkupTest {

    private static final LanguageHandlerJavaScript HANDLER = new LanguageHandlerJavaScript();

    private static List<Pair<Style, String>> markup(String script) {
        List<Pair<Style, String>> segments = HANDLER.markupScript(script);
        assertThat(segments.stream().map(Pair::getRight).collect(Collectors.joining()), equalTo(script));
        return segments;
    }

    private static Style styleOf(String script, String token) {
        int pos = script.indexOf(token);
        int i = 0;
        for (Pair<Style, String> segment : markup(script)) {
            if (pos < i + segment.getRight().length()) {
                return segment.getLeft();
            }
            i += segment.getRight().length();
        }
        throw new AssertionError("Token not found");
    }

    @Test
    public void testKeyword() {
        assertThat(styleOf("const a = 1;", "const"), equalTo(LanguageHandlerJavaScript.KEYWORD));
    }

    @Test
    public void testKeywordInsideIdentifier() {
        assertThat(styleOf("const variable = 1;", "variable"), equalTo(Style.EMPTY));
        assertThat(styleOf("let outlet = 1;", "outlet"), equalTo(Style.EMPTY));
        assertThat(styleOf("returnValue();", "returnValue"), equalTo(Style.EMPTY));
    }

    @Test
    public void testSymbol() {
        assertThat(styleOf("f();", "("), equalTo(LanguageHandlerJavaScript.SYMBOL));
    }

    @Test
    public void testNumber() {
        assertThat(styleOf("x = 1.5;", "1.5"), equalTo(LanguageHandlerJavaScript.CONSTANT));
        assertThat(markup("x = 1.5;").stream().anyMatch(s -> s.getRight().equals("1.5")), equalTo(true));
    }

    @Test
    public void testConstant() {
        assertThat(styleOf("return true;", "true"), equalTo(LanguageHandlerJavaScript.CONSTANT));
    }

    @Test
    public void testStrings() {
        assertThat(styleOf("x = \"let it be\";", "let"), equalTo(LanguageHandlerJavaScript.STRING));
        assertThat(styleOf("x = 'delete';", "delete"), equalTo(LanguageHandlerJavaScript.STRING));
        assertThat(styleOf("x = `a ${b}`;", "b"), equalTo(LanguageHandlerJavaScript.STRING));
    }

    @Test
    public void testEscapedQuote() {
        assertThat(styleOf("x = 'it\\'s var';", "var"), equalTo(LanguageHandlerJavaScript.STRING));
    }

    @Test
    public void testUnterminatedStringEndsAtLine() {
        assertThat(styleOf("x = 'abc\nconst y;", "const"), equalTo(LanguageHandlerJavaScript.KEYWORD));
    }

    @Test
    public void testLineComment() {
        assertThat(styleOf("// const a\nconst b;", "const a"), equalTo(LanguageHandlerJavaScript.COMMENT));
        assertThat(styleOf("// const a\nconst b;", "const b"), equalTo(LanguageHandlerJavaScript.KEYWORD));
    }

    @Test
    public void testBlockCommentSpansLines() {
        assertThat(styleOf("/* a\nreturn b */ return", "return b"), equalTo(LanguageHandlerJavaScript.COMMENT));
        assertThat(styleOf("/* a\nreturn b */ let", "let"), equalTo(LanguageHandlerJavaScript.KEYWORD));
    }

    @Test
    public void testMarkupLineCompatible() {
        assertThat(HANDLER.markupLine("const a;"), equalTo(HANDLER.markupScript("const a;")));
    }

}
