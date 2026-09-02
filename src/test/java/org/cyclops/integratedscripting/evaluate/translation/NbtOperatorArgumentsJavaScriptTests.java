package org.cyclops.integratedscripting.evaluate.translation;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeListProxyFactories;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeNbt;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for passing values to operators that expect NBT.
 *
 * NBT values other than compound tags reach scripts as plain JS values,
 * so they must be converted back to NBT when they are passed into an operator again.
 *
 * @author rubensworks
 */
public class NbtOperatorArgumentsJavaScriptTests {

    static {
        SharedConstants.setVersion(DetectedVersion.BUILT_IN);
        Bootstrap.bootStrap();
    }

    private static Context CTX = null;
    private static IEvaluationExceptionFactory EF = ScriptHelpers.getDummyEvaluationExceptionFactory();

    @BeforeClass
    public static void beforeAll() throws EvaluationException {
        ValueTypeListProxyFactories.load();
        Operators.load();
        ValueTranslators.load();

        CTX = ScriptHelpers.createPopulatedContext(null);
    }

    private static Value nbtValue() throws EvaluationException {
        CompoundTag levels = new CompoundTag();
        levels.putInt("a", 5);
        levels.putInt("b", 1);
        levels.putInt("c", 7);
        CompoundTag tag = new CompoundTag();
        tag.put("levels", levels);
        return ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(tag), EF);
    }

    @Test
    public void testListTagMatchPassedBackToNbtOperator() throws EvaluationException {
        // A filter expression matches into a list tag, which reaches the script as a plain array.
        Value function = CTX.eval("js", """
                (function (nbt) {
                    const matched = idContext.ops.stringNbtPathMatchFirst('["levels"][?(@ >= 3)]', nbt);
                    return [Array.isArray(matched), idContext.ops.nbtAsTagList(matched).length];
                })
                """);
        Value result = function.execute(nbtValue());
        assertThat(result.getArrayElement(0).asBoolean(), is(true));
        assertThat(result.getArrayElement(1).asInt(), is(2));
    }

    @Test
    public void testArrayPassedToNbtOperator() {
        Value function = CTX.eval("js", """
                (function () {
                    return idContext.ops.nbtAsTagList([1, 2, 3]).length;
                })
                """);
        assertThat(function.execute().asInt(), is(3));
    }

    @Test
    public void testNullPassedToNbtOperator() throws EvaluationException {
        // An NBT path that matches nothing yields null, which stays usable as an NBT value.
        Value function = CTX.eval("js", """
                (function (nbt) {
                    const matched = idContext.ops.stringNbtPathMatchFirst('["absent"]', nbt);
                    return [matched === null, idContext.ops.nbtAsTagList(matched).length];
                })
                """);
        Value result = function.execute(nbtValue());
        assertThat(result.getArrayElement(0).asBoolean(), is(true));
        assertThat(result.getArrayElement(1).asInt(), is(0));
    }

    @Test
    public void testCompoundTagPassedBackToNbtOperator() throws EvaluationException {
        // Compound tags were already passed back unchanged, and must keep working.
        Value function = CTX.eval("js", """
                (function (nbt) {
                    const levels = idContext.ops.stringNbtPathMatchFirst('["levels"]', nbt);
                    return idContext.ops.nbtSize(levels);
                })
                """);
        assertThat(function.execute(nbtValue()).asInt(), is(3));
    }

    @Test
    public void testNonNbtOperatorArgumentsAreUnaffected() {
        Value function = CTX.eval("js", """
                (function () {
                    return idContext.ops.stringLength("abcd");
                })
                """);
        assertThat(function.execute().asInt(), is(4));
    }
}
