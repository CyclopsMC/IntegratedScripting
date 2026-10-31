package org.cyclops.integratedscripting.core.language;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JavaScriptDeclarationsTest {

    @Test
    public void testDeclarations() {
        assertThat(JavaScriptDeclarations.find("function a(x, y) { return x; }\nconst b = 1;\nlet c = 2\nvar d\nclass E {}\nasync function f() {}\nfunction* g() {}\nexport const h = 3;"),
                equalTo(Sets.newHashSet("a", "b", "c", "d", "E", "f", "g", "h")));
    }

    @Test
    public void testMultipleDeclarators() {
        assertThat(JavaScriptDeclarations.find("const a = f(1, 2), b = [3, 4], c = { x: 1, y: 2 };\nfoo(d, e);"),
                equalTo(Sets.newHashSet("a", "b", "c")));
    }

    @Test
    public void testIgnoresNested() {
        assertThat(JavaScriptDeclarations.find("function a(item) {\n  const b = item.size();\n  function c() {}\n  return b;\n}\nconst d = (x) => { let e = x; return e; };"),
                equalTo(Sets.newHashSet("a", "d")));
    }

    @Test
    public void testIgnoresExpressions() {
        assertThat(JavaScriptDeclarations.find("const a = function b() {};\nconst c = class D {};\nfoo(function e() {});"),
                equalTo(Sets.newHashSet("a", "c")));
    }

    @Test
    public void testIgnoresStringsCommentsAndRegexes() {
        assertThat(JavaScriptDeclarations.find("// const a = 1;\n/* function b() {} */\nconst c = \"const d = '{';\";\nconst e = /[{(]/.test(c);\nconst f = `${c} {`;\nconst g = 4 / 2;"),
                equalTo(Sets.newHashSet("c", "e", "f", "g")));
    }

    @Test
    public void testIgnoresMemberAccesses() {
        assertThat(JavaScriptDeclarations.find("const a = idContext.ops.function;"), equalTo(Sets.newHashSet("a")));
    }

    @Test
    public void testDestructuringUnsupported() {
        assertThat(JavaScriptDeclarations.find("const { a, b } = c;"), nullValue());
        assertThat(JavaScriptDeclarations.find("const [a, b] = c;"), nullValue());
    }

}
