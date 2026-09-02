package org.cyclops.integratedscripting.evaluate.translation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integratedscripting.GeneralConfig;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeNbt;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeString;
import org.cyclops.integrateddynamics.core.evaluate.variable.Variable;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Reproduces the behaviour reported in issue #67:
 * what a script receives from an NBT path match, what happens when such a value is passed back
 * into an NBT operator, and what the reported filter costs per evaluation.
 *
 * @author rubensworks
 */
public class Issue67Tests {

    private static ValueDeseralizationContext VDC = null;
    private static Context CTX = null;
    private static IEvaluationExceptionFactory EF = ScriptHelpers.getDummyEvaluationExceptionFactory();

    @BeforeAll
    public static void beforeAll() throws EvaluationException {
        VDC = ValueDeseralizationContextMocked.get();
        CTX = ScriptHelpers.createPopulatedContext(null, VDC);
        System.out.println("Graal engine implementation: " + CTX.getEngine().getImplementationName());
    }

    private static CompoundTag enchantedNbt() {
        CompoundTag levels = new CompoundTag();
        levels.putInt("minecraft:sharpness", 5);
        levels.putInt("minecraft:unbreaking", 1);
        CompoundTag enchantments = new CompoundTag();
        enchantments.put("levels", levels);
        CompoundTag root = new CompoundTag();
        root.put("minecraft:enchantments", enchantments);
        return root;
    }

    // The NBT path filter yields a ListTag, which reaches the script as a plain JS array.
    @Test
    public void testFilterMatchIsPlainJsArray() throws EvaluationException {
        IValue result = Operators.NBT_PATH_MATCH_FIRST.evaluate(
                new Variable<>(ValueTypeString.ValueString.of("[\"minecraft:enchantments\"].levels[?(@ >= 3)]")),
                new Variable<>(ValueTypeNbt.ValueNbt.of(enchantedNbt())));
        System.out.println("match result tag: " + ((ValueTypeNbt.ValueNbt) result).getRawValue());

        Value graal = ValueTranslators.REGISTRY.translateToGraal(CTX, result, EF, VDC);
        System.out.println("as graal: hasArrayElements=" + graal.hasArrayElements()
                + ", isNull=" + graal.isNull() + ", isProxy=" + graal.isProxyObject() + ", value=" + graal);
        assertThat(graal.hasArrayElements(), is(true));
    }

    // A path that does not match anything reaches the script as null.
    @Test
    public void testNoMatchIsNull() throws EvaluationException {
        IValue result = Operators.NBT_PATH_MATCH_FIRST.evaluate(
                new Variable<>(ValueTypeString.ValueString.of("[\"minecraft:enchantments\"].levels[?(@ >= 3)]")),
                new Variable<>(ValueTypeNbt.ValueNbt.of(new CompoundTag())));
        Value graal = ValueTranslators.REGISTRY.translateToGraal(CTX, result, EF, VDC);
        System.out.println("no-match as graal: isNull=" + graal.isNull());
        assertThat(graal.isNull(), is(true));
    }

    // Feeding such an array back into an NBT operator: what actually happens?
    @Test
    public void testArrayBackIntoNbtOperator() throws EvaluationException {
        ListTag list = new ListTag();
        list.add(IntTag.valueOf(5));
        list.add(IntTag.valueOf(7));
        Value graal = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(list), EF, VDC);

        IValue backToId = ValueTranslators.REGISTRY.translateFromGraal(CTX, graal, EF, VDC);
        System.out.println("round-tripped value type: " + backToId.getType().getTypeName() + " -> " + backToId);

        try {
            IValue result = Operators.NBT_AS_INT_LIST.evaluate(new Variable<>(backToId));
            System.out.println("nbtAsIntList result: " + result);
            if (result instanceof ValueTypeList.ValueList<?, ?> valueList) {
                System.out.println("nbtAsIntList length: " + valueList.getRawValue().getLength());
            }
        } catch (EvaluationException e) {
            System.out.println("nbtAsIntList threw: " + e.getMessage());
        }
    }

    // A compound tag does survive the round trip, but asIntList silently yields an empty list for it.
    @Test
    public void testCompoundBackIntoNbtOperator() throws EvaluationException {
        Value graal = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(enchantedNbt()), EF, VDC);
        IValue backToId = ValueTranslators.REGISTRY.translateFromGraal(CTX, graal, EF, VDC);
        System.out.println("compound round-tripped type: " + backToId.getType().getTypeName());
        IValue result = Operators.NBT_AS_INT_LIST.evaluate(new Variable<>(backToId));
        System.out.println("nbtAsIntList on compound: " + result
                + ", length=" + ((ValueTypeList.ValueList<?, ?>) result).getRawValue().getLength());
    }

    // The full script-side reproduction of the reported snippet.
    @Test
    public void testUserScript() throws EvaluationException {
        Value fn = CTX.eval("js", """
                (function filterEnchantLevel(level, nbt) {
                    const nbtPath = `["minecraft:enchantments"].levels[?(@ >= ${level})]`;
                    const levelsNbt = idContext.ops.stringNbtPathMatchFirst(nbtPath, nbt);
                    let levelsList = [];
                    if (levelsNbt && Array.isArray(levelsNbt)) {
                        levelsList = [...levelsNbt];
                    }
                    return [levelsNbt === null, Array.isArray(levelsNbt), levelsList.length];
                })
                """);
        Value nbtValue = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(enchantedNbt()), EF, VDC);
        System.out.println("script on enchanted item: " + fn.execute(3, nbtValue));
        Value emptyValue = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueTypeNbt.ValueNbt.of(new CompoundTag()), EF, VDC);
        System.out.println("script on plain item: " + fn.execute(3, emptyValue));

        // And what the original, unguarded version does.
        Value fnUnguarded = CTX.eval("js", """
                (function (level, nbt) {
                    const levelsNbt = idContext.ops.stringNbtPathMatchFirst(`["minecraft:enchantments"].levels[?(@ >= ${level})]`, nbt);
                    return [...levelsNbt].length > 0;
                })
                """);
        Assertions.assertThrows(Exception.class, () -> fnUnguarded.execute(3, emptyValue));
    }

    // Rough cost comparison of the reported setup: the same filter in JS versus plain Integrated Dynamics.
    @Test
    public void benchmarkFilter() throws EvaluationException {
        CompoundTag nbt = enchantedNbt();
        IValue nbtValue = ValueTypeNbt.ValueNbt.of(nbt);
        IValue emptyNbtValue = ValueTypeNbt.ValueNbt.of(new CompoundTag());
        IVariable[] nbtArgs = new IVariable[]{ new Variable<>(nbtValue) };
        IVariable[] emptyArgs = new IVariable[]{ new Variable<>(emptyNbtValue) };

        // Pure Integrated Dynamics: the same NBT path match, without any scripting involved.
        IVariable[] idArgs = new IVariable[]{
                new Variable<>(ValueTypeString.ValueString.of("[\"minecraft:enchantments\"].levels[?(@ >= 3)]")),
                new Variable<>(nbtValue),
        };
        BenchmarkValueTranslators.benchmark("Filter-id-pathMatchFirst",
                () -> Operators.NBT_PATH_MATCH_FIRST.evaluate(idArgs), 5000, 20000);

        // The reported script, as an Integrated Dynamics operator.
        IOperator jsFilter = getOperator("""
                (function (nbt) {
                    const nbtPath = `["minecraft:enchantments"].levels[?(@ >= 3)]`;
                    const levelsNbt = idContext.ops.stringNbtPathMatchFirst(nbtPath, nbt);
                    let levelsList = [];
                    if (levelsNbt && Array.isArray(levelsNbt)) {
                        levelsList = [...levelsNbt];
                    }
                    return levelsList.length > 0;
                })
                """);
        BenchmarkValueTranslators.benchmark("Filter-js-guarded", () -> jsFilter.evaluate(nbtArgs), 5000, 20000);

        // The same script on an item without the matched path, i.e. the null case.
        BenchmarkValueTranslators.benchmark("Filter-js-guarded-null", () -> jsFilter.evaluate(emptyArgs), 5000, 20000);

        // The original, unguarded script on an item without the matched path: this throws on every call.
        IOperator jsFilterThrowing = getOperator("""
                (function (nbt) {
                    const levelsNbt = idContext.ops.stringNbtPathMatchFirst(`["minecraft:enchantments"].levels[?(@ >= 3)]`, nbt);
                    return [...levelsNbt].length > 0;
                })
                """);
        BenchmarkValueTranslators.benchmark("Filter-js-unguarded-throwing", () -> {
            try {
                jsFilterThrowing.evaluate(emptyArgs);
            } catch (EvaluationException e) {
                // Expected
            }
        }, 2000, 5000);

        // A trivial JS operator, as a baseline for the fixed per-call overhead.
        IOperator jsNoop = getOperator("(function (nbt) { return true; })");
        BenchmarkValueTranslators.benchmark("Filter-js-noop", () -> jsNoop.evaluate(nbtArgs), 5000, 20000);

        // The same filter, walking the NBT proxy directly instead of parsing an NBT path on every call.
        IOperator jsWalk = getOperator("""
                (function (nbt) {
                    const enchantments = nbt["minecraft:enchantments"];
                    if (!enchantments) {
                        return false;
                    }
                    const levels = enchantments.levels;
                    if (!levels) {
                        return false;
                    }
                    for (const key in levels) {
                        if (levels[key] >= 3) {
                            return true;
                        }
                    }
                    return false;
                })
                """);
        BenchmarkValueTranslators.benchmark("Filter-js-walk", () -> jsWalk.evaluate(nbtArgs), 5000, 20000);
        BenchmarkValueTranslators.benchmark("Filter-js-walk-null", () -> jsWalk.evaluate(emptyArgs), 5000, 20000);
        System.out.println("walk result on enchanted: " + jsWalk.evaluate(nbtArgs) + ", on plain: " + jsWalk.evaluate(emptyArgs));
    }

    // How much does the statement limit (enabled by default) cost?
    @Test
    public void benchmarkStatementLimit() throws EvaluationException {
        int limitOriginal = GeneralConfig.graalStatementLimit;
        try {
            String script = """
                    (function (a) {
                        let sum = 0;
                        for (let i = 0; i < 100; i++) {
                            sum += i * a;
                        }
                        return sum;
                    })
                    """;

            GeneralConfig.graalStatementLimit = 16384;
            Context contextLimited = ScriptHelpers.createPopulatedContext(null, VDC);
            Value fnLimited = contextLimited.eval("js", script);
            BenchmarkValueTranslators.benchmark("StatementLimit-16384", () -> {
                contextLimited.resetLimits();
                fnLimited.execute(2);
            }, 5000, 20000);

            GeneralConfig.graalStatementLimit = -1;
            Context contextUnlimited = ScriptHelpers.createPopulatedContext(null, VDC);
            Value fnUnlimited = contextUnlimited.eval("js", script);
            BenchmarkValueTranslators.benchmark("StatementLimit-disabled", () -> {
                contextUnlimited.resetLimits();
                fnUnlimited.execute(2);
            }, 5000, 20000);

            // And the cost of resetLimits() itself, which happens on every call from Integrated Dynamics.
            BenchmarkValueTranslators.benchmark("ResetLimits-16384-only", contextLimited::resetLimits, 20000, 100000);
            BenchmarkValueTranslators.benchmark("ResetLimits-disabled-only", contextUnlimited::resetLimits, 20000, 100000);

            contextLimited.close();
            contextUnlimited.close();
        } finally {
            GeneralConfig.graalStatementLimit = limitOriginal;
        }
    }

    private static IOperator getOperator(String script) throws EvaluationException {
        Value fn = CTX.eval("js", script);
        return ((ValueTypeOperator.ValueOperator) ValueTranslators.REGISTRY.translateFromGraal(CTX, fn, EF, VDC)).getRawValue();
    }
}
