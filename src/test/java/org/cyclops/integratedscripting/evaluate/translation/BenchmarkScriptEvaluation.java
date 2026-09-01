package org.cyclops.integratedscripting.evaluate.translation;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.*;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

/**
 * Benchmarks for the full JavaScript script evaluation pipeline,
 * complementing the more narrowly scoped {@link BenchmarkValueTranslators}.
 *
 * @author rubensworks
 */
public class BenchmarkScriptEvaluation {

    private static ValueDeseralizationContext VDC = null;

    private static final String SCRIPT = """
            exports = {
              value: 42,
              add: (a, b) => a + b,
              identity: (a) => a,
              useOps: (a) => idContext.ops.numberIncrement(a),
            };
            """;

    @Test
    public void main() throws EvaluationException {
        // Bind components so ItemStack construction works in 26.1 (components are normally bound during resource reload)
        DataComponentMap defaultComponents = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
        Items.ARROW.builtInRegistryHolder().bindComponents(defaultComponents);

        beforeAll();

        /*
Latest results
Context-createBase: 0.02202ms/op
Context-createBaseJs: 0.3006ms/op
Context-createPopulated: 0.3253ms/op
Script-instantiate: 0.2021ms/op
Script-instantiate-useOps: 0.4355ms/op
Script-memberValue: 0.0001532ms/op
Operator-callJsFromId-int: 0.0004033ms/op
Operator-callJsFromId-item: 0.0009777ms/op
Operator-callIdFromJs-int: 0.0004680ms/op
Operator-roundtrip-int: 0.0006388ms/op
Proxy-unwrapItem: 0.0005577ms/op
Proxy-unwrapNbt: 0.0005382ms/op
Proxy-unwrapOperator: 0.0004444ms/op
         */

        // Context creation, as happens for every (re)instantiated script.
        BenchmarkValueTranslators.benchmark("Context-createBase",
                () -> ScriptHelpers.createBaseContext(null).close(), 500, 1000);
        BenchmarkValueTranslators.benchmark("Context-createBaseJs", () -> {
            Context context = ScriptHelpers.createBaseContext(null);
            context.getBindings("js");
            context.close();
        }, 500, 1000);
        BenchmarkValueTranslators.benchmark("Context-createPopulated",
                () -> ScriptHelpers.createPopulatedContext(null, VDC).close(), 500, 1000);

        // Full script instantiation, as happens whenever a script (re)loads.
        Source source = Source.newBuilder("js", SCRIPT, "bench.js").buildLiteral();
        BenchmarkValueTranslators.benchmark("Script-instantiate", () -> {
            Context context = ScriptHelpers.createPopulatedContext(null, VDC);
            try {
                context.eval(source);
            } finally {
                context.close();
            }
        }, 300, 1000);

        // The same, but with a script that actually makes use of the global operators.
        BenchmarkValueTranslators.benchmark("Script-instantiate-useOps", () -> {
            Context context = ScriptHelpers.createPopulatedContext(null, VDC);
            try {
                context.eval(source);
                context.getBindings("js").getMember("exports").getMember("useOps").execute(1);
            } finally {
                context.close();
            }
        }, 300, 1000);

        // Reading a script member value, as happens on every script variable (re)evaluation.
        Context context = ScriptHelpers.createPopulatedContext(null, VDC);
        context.eval(source);
        Value exports = context.getBindings("js").getMember("exports");
        BenchmarkValueTranslators.benchmark("Script-memberValue", () -> {
            context.resetLimits();
            ValueTranslators.REGISTRY.translateFromGraal(context, exports.getMember("value"),
                    ScriptHelpers.getDummyEvaluationExceptionFactory(), VDC);
        }, 100000, 200000);

        // Calling a JS function as an Integrated Dynamics operator.
        IOperator jsOperatorAdd = getOperator(context, exports, "add");
        IVariable[] intArgs = new IVariable[]{
                new Variable<>(ValueTypeInteger.ValueInteger.of(1)),
                new Variable<>(ValueTypeInteger.ValueInteger.of(2)),
        };
        BenchmarkValueTranslators.benchmark("Operator-callJsFromId-int",
                () -> jsOperatorAdd.evaluate(intArgs), 20000, 50000);

        IOperator jsOperatorIdentity = getOperator(context, exports, "identity");
        IVariable[] itemArgs = new IVariable[]{
                new Variable<>(ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW))),
        };
        BenchmarkValueTranslators.benchmark("Operator-callJsFromId-item",
                () -> jsOperatorIdentity.evaluate(itemArgs), 20000, 50000);

        // Calling an Integrated Dynamics operator from JS.
        Value opsCaller = context.eval(Source.newBuilder("js",
                "(function() { return idContext.ops.numberIncrement(1); })", "bench-ops.js").buildLiteral());
        BenchmarkValueTranslators.benchmark("Operator-callIdFromJs-int", () -> {
            context.resetLimits();
            opsCaller.execute();
        }, 20000, 50000);

        // A JS function that calls into an Integrated Dynamics operator, called from Integrated Dynamics.
        IOperator jsOperatorUseOps = getOperator(context, exports, "useOps");
        IVariable[] intArg = new IVariable[]{ new Variable<>(ValueTypeInteger.ValueInteger.of(1)) };
        BenchmarkValueTranslators.benchmark("Operator-roundtrip-int",
                () -> jsOperatorUseOps.evaluate(intArg), 20000, 50000);

        // Translating values that were translated in the other direction before (proxy unwrapping),
        // as happens for all Integrated Dynamics values that a script passes back unchanged.
        runUnwrap(context, "Item", ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW)));
        runUnwrap(context, "Nbt", ValueTypeNbt.ValueNbt.of(new CompoundTag()));
        runUnwrap(context, "Operator", ValueTypeOperator.ValueOperator.of(Operators.ARITHMETIC_ADDITION));

        context.close();
    }

    private static IOperator getOperator(Context context, Value exports, String member) throws EvaluationException {
        return ((ValueTypeOperator.ValueOperator) ValueTranslators.REGISTRY.translateFromGraal(context,
                exports.getMember(member), ScriptHelpers.getDummyEvaluationExceptionFactory(), VDC)).getRawValue();
    }

    private static <V extends org.cyclops.integrateddynamics.api.evaluate.variable.IValue> void runUnwrap(
            Context context, String label, V value) throws EvaluationException {
        Value graalValue = ValueTranslators.REGISTRY.translateToGraal(context, value,
                ScriptHelpers.getDummyEvaluationExceptionFactory(), VDC);
        BenchmarkValueTranslators.benchmark("Proxy-unwrap" + label,
                () -> ValueTranslators.REGISTRY.translateFromGraal(context, graalValue,
                        ScriptHelpers.getDummyEvaluationExceptionFactory(), VDC), 20000, 50000);
    }

    public static void beforeAll() {
        VDC = ValueDeseralizationContextMocked.get();
    }

}
