package org.cyclops.integratedscripting.core.language;

import com.google.common.collect.Lists;
import org.cyclops.integratedscripting.api.language.ScriptCompletions;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class JavaScriptCompletionsTest {

    private static final Map<String, String> SIGNATURES = Map.of(
            "itemstackSize", "(Item): Integer",
            "itemstackIsStackable", "(Item): Boolean",
            "integerMax", "(Integer, Integer): Integer"
    );

    private static ScriptCompletions complete(String script) {
        return JavaScriptCompletions.complete(script, script.length(), SIGNATURES);
    }

    @Test
    public void testCompleteAll() {
        assertThat(complete("const a = idContext.ops."), equalTo(new ScriptCompletions(24, Lists.newArrayList(
                new ScriptCompletions.Item("integerMax", "(Integer, Integer): Integer"),
                new ScriptCompletions.Item("itemstackIsStackable", "(Item): Boolean"),
                new ScriptCompletions.Item("itemstackSize", "(Item): Integer")
        ))));
    }

    @Test
    public void testCompletePrefixIgnoringCase() {
        assertThat(complete("idContext.ops.ITEMSTACKs"), equalTo(new ScriptCompletions(14, Lists.newArrayList(
                new ScriptCompletions.Item("itemstackSize", "(Item): Integer")
        ))));
    }

    @Test
    public void testCompleteNone() {
        assertThat(complete("const a = ops."), nullValue());
        assertThat(complete("idContext.ops.foo"), nullValue());
        assertThat(complete("idContext.ops.itemstackSize"), nullValue());
        assertThat(JavaScriptCompletions.complete("idContext.ops.itemstack", 18, SIGNATURES), nullValue());
    }

}
