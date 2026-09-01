package org.cyclops.integratedscripting.evaluate.translation;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.*;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

/**
 * @author rubensworks
 */
public class BenchmarkValueTranslators {

    public static int REPLICATION = 100000;

    private static Engine ENGINE = null;
    private static ValueDeseralizationContext VDC = null;
    private static Context CTX = null;

    @Test
    public void main() {
        // Bind components so ItemStack construction works in 26.1 (components are normally bound during resource reload)
        DataComponentMap defaultComponents = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
        Items.ARROW.builtInRegistryHolder().bindComponents(defaultComponents);

        beforeAll();

        /*
Latest results
FromGraal-int: 0.000005884ms/op
FromGraal-boolean: 0.00002270ms/op
FromGraal-double: 0.00003193ms/op
FromGraal-long: 0.00002828ms/op
FromGraal-string: 0.00004549ms/op
FromGraal-list: 0.0007997ms/op
FromGraal-operator: 0.0004249ms/op
FromGraal-nbt: 0.008248ms/op
FromGraal-item: 0.004357ms/op
ToGraal-int: 0.00009166ms/op
ToGraal-boolean: 0.000005404ms/op
ToGraal-double: 0.00006237ms/op
ToGraal-long: 0.00006071ms/op
ToGraal-string: 0.00004514ms/op
ToGraal-list: 0.0002058ms/op
ToGraal-operator: 0.00007643ms/op
ToGraal-nbt: 0.00007930ms/op
ToGraal-item: 0.00007836ms/op
         */

        runFromGraal("int", getJsValue("10"), REPLICATION);
        runFromGraal("boolean", getJsValue("true"), REPLICATION);
        runFromGraal("double", getJsValue("1.1"), REPLICATION);
        runFromGraal("long", getJsValue("2147483648"), REPLICATION);
        runFromGraal("string", getJsValue("'abc'"), REPLICATION);
        runFromGraal("list", getJsValue("['abc', 'def', 'ghi']"), REPLICATION);
        runFromGraal("operator", getJsValue("(a, b) => true"), REPLICATION);
        runFromGraal("nbt", getJsValue("exports = { a: { b: { c: '1', d: 123 } } }"), REPLICATION);
        runFromGraal("item", getJsValue("exports = { id_item: { stack: { id: 'minecraft:arrow', count: 1 } } }"), REPLICATION);

        runToGraal("int", ValueTypeInteger.ValueInteger.of(10), REPLICATION);
        runToGraal("boolean", ValueTypeBoolean.ValueBoolean.of(true), REPLICATION);
        runToGraal("double", ValueTypeDouble.ValueDouble.of(1.1D), REPLICATION);
        runToGraal("long", ValueTypeLong.ValueLong.of(2147483648L), REPLICATION);
        runToGraal("string", ValueTypeString.ValueString.of("abc"), REPLICATION);
        runToGraal("list", ValueTypeList.ValueList.ofAll(
                ValueTypeString.ValueString.of("abc"),
                ValueTypeString.ValueString.of("def"),
                ValueTypeString.ValueString.of("ghi")
        ), REPLICATION);
        runToGraal("operator", ValueTypeOperator.ValueOperator.of(Operators.ARITHMETIC_ADDITION), REPLICATION);
        CompoundTag compoundTag = new CompoundTag();
        CompoundTag compoundTagSub = new CompoundTag();
        compoundTag.put("a", StringTag.valueOf("bla"));
        compoundTag.put("b", compoundTagSub);
        compoundTagSub.put("c", IntTag.valueOf(123));
        runToGraal("nbt", ValueTypeNbt.ValueNbt.of(compoundTag), REPLICATION);
        runToGraal("item", ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.ARROW)), REPLICATION);
    }

    public static void beforeAll() {
        ENGINE = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        VDC = ValueDeseralizationContextMocked.get();
        CTX = Context.newBuilder().engine(ENGINE).allowAllAccess(true).build();
    }

    public static Value getJsValue(String jsString) {
        return CTX.eval("js", jsString);
    }

    private static void runFromGraal(String label, Value graalValue, int replication) {
        benchmark("FromGraal-" + label, () -> ValueTranslators.REGISTRY.translateFromGraal(CTX, graalValue, ScriptHelpers.getDummyEvaluationExceptionFactory(), VDC), replication);
    }

    private static void runToGraal(String label, IValue idValue, int replication) {
        benchmark("ToGraal-" + label, () -> ValueTranslators.REGISTRY.translateToGraal(CTX, idValue, ScriptHelpers.getDummyEvaluationExceptionFactory(), VDC), replication);
    }

    public static void benchmark(String label, ThrowingRunnable runnable, int replication) {
        benchmark(label, runnable, replication / 10, replication);
    }

    public static int ROUNDS = 5;

    public static void benchmark(String label, ThrowingRunnable runnable, int warmup, int replication) {
        try {
            // Warm up the JIT (and Graal's own profiling) before measuring.
            for (int i = 0; i < warmup; i++) {
                runnable.run();
            }

            // Run multiple rounds, and report the fastest one,
            // as that is the least affected by GC pauses and other noise.
            long best = Long.MAX_VALUE;
            for (int round = 0; round < ROUNDS; round++) {
                long startTime = System.nanoTime();
                for (int i = 0; i < replication; i++) {
                    runnable.run();
                }
                best = Math.min(best, System.nanoTime() - startTime);
            }
            System.out.println(label + ": " + format(((double) best) / replication / 1_000_000D) + "ms/op");
        } catch (EvaluationException e) {
            e.printStackTrace();
        }
    }

    private static String format(double value) {
        return new java.math.BigDecimal(value).round(new java.math.MathContext(4)).toPlainString();
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        public abstract void run() throws EvaluationException;
    }

}
