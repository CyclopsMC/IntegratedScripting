package org.cyclops.integratedscripting.evaluate.translation;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.*;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class BenchmarkValueTranslators {

    static {
        SharedConstants.setVersion(DetectedVersion.BUILT_IN);
        Bootstrap.bootStrap();
        Registry.ITEM.unfreeze();
    }

    public static int REPLICATION = 100000;

    private static Engine ENGINE = null;
    private static Context CTX = null;

    public static void main(String[] args) {
        beforeAll();

        /*
Latest results
FromGraal-int: 1.6E-4ms/op
FromGraal-boolean: 1.8E-4ms/op
FromGraal-double: 3.1E-4ms/op
FromGraal-long: 1.1E-4ms/op
FromGraal-string: 1.3E-4ms/op
FromGraal-list: 0.0033ms/op
FromGraal-operator: 0.00185ms/op
FromGraal-nbt: 0.0222ms/op
FromGraal-item: 0.00963ms/op
ToGraal-int: 3.1E-4ms/op
ToGraal-boolean: 2.5E-4ms/op
ToGraal-double: 2.9E-4ms/op
ToGraal-long: 2.8E-4ms/op
ToGraal-string: 2.5E-4ms/op
ToGraal-list: 8.3E-4ms/op
ToGraal-operator: 3.1E-4ms/op
ToGraal-nbt: 4.2E-4ms/op
ToGraal-item: 6.7E-4ms/op
         */

        runFromGraal("int", getJsValue("10"), REPLICATION);
        runFromGraal("boolean", getJsValue("true"), REPLICATION);
        runFromGraal("double", getJsValue("1.1"), REPLICATION);
        runFromGraal("long", getJsValue("2147483648"), REPLICATION);
        runFromGraal("string", getJsValue("'abc'"), REPLICATION);
        runFromGraal("list", getJsValue("['abc', 'def', 'ghi']"), REPLICATION);
        runFromGraal("operator", getJsValue("(a, b) => true"), REPLICATION);
        runFromGraal("nbt", getJsValue("exports = { a: { b: { c: '1', d: 123 } } }"), REPLICATION);
        runFromGraal("item", getJsValue("exports = { id_item: { id: 'minecraft:arrow', Count: 1 } }"), REPLICATION);

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
        ValueTypeListProxyFactories.load();
        Operators.load();
        ValueTranslators.load();

        ENGINE = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        CTX = Context.newBuilder().engine(ENGINE).allowAllAccess(true).build();
    }

    public static Value getJsValue(String jsString) {
        return CTX.eval("js", jsString);
    }

    private static void runFromGraal(String label, Value graalValue, int replication) {
        benchmark("FromGraal-" + label, () -> ValueTranslators.REGISTRY.translateFromGraal(CTX, graalValue, ScriptHelpers.getDummyEvaluationExceptionFactory()), replication);
    }

    private static void runToGraal(String label, IValue idValue, int replication) {
        benchmark("ToGraal-" + label, () -> ValueTranslators.REGISTRY.translateToGraal(CTX, idValue, ScriptHelpers.getDummyEvaluationExceptionFactory()), replication);
    }

    public static void benchmark(String label, ThrowingRunnable runnable, int replication) {
        long startTime = System.currentTimeMillis();
        try {
            for (int i = 0; i < replication; i++) {
                runnable.run();
            }
        } catch (EvaluationException e) {
            e.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println(label + ": " + ((double) elapsedTime) / replication + "ms/op");
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        public abstract void run() throws EvaluationException;
    }

}
