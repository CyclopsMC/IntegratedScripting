package org.cyclops.integratedscripting.evaluate.translation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeDouble;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeListProxyFactories;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeLong;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeNbt;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeString;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class BenchmarkValueTranslators {

    public static int REPLICATION = 1000000;

    private static Engine ENGINE = null;
    private static Context CTX = null;

    public static void main(String[] args) {
        beforeAll();

        /*
Latest results
FromGraal-int: 5.7E-5ms/op
FromGraal-boolean: 3.8E-5ms/op
FromGraal-double: 7.3E-5ms/op
FromGraal-long: 6.2E-5ms/op
FromGraal-string: 6.8E-5ms/op
FromGraal-list: 0.001788ms/op
FromGraal-operator: 9.2E-4ms/op
FromGraal-nbt: 0.006845ms/op
ToGraal-int: 1.3E-4ms/op
ToGraal-boolean: 1.17E-4ms/op
ToGraal-double: 1.3E-4ms/op
ToGraal-long: 1.17E-4ms/op
ToGraal-string: 1.38E-4ms/op
ToGraal-list: 6.08E-4ms/op
ToGraal-operator: 1.76E-4ms/op
ToGraal-nbt: 0.004066ms/op
         */

        runFromGraal("int", getJsValue("10"), REPLICATION);
        runFromGraal("boolean", getJsValue("true"), REPLICATION);
        runFromGraal("double", getJsValue("1.1"), REPLICATION);
        runFromGraal("long", getJsValue("2147483648"), REPLICATION);
        runFromGraal("string", getJsValue("'abc'"), REPLICATION);
        runFromGraal("list", getJsValue("['abc', 'def', 'ghi']"), REPLICATION);
        runFromGraal("operator", getJsValue("(a, b) => true"), REPLICATION);
        runFromGraal("nbt", getJsValue("exports = { a: { b: { c: '1', d: 123 } } }"), REPLICATION);

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
        benchmark("FromGraal-" + label, () -> ValueTranslators.REGISTRY.translateFromGraal(CTX, graalValue), replication);
    }

    private static void runToGraal(String label, IValue idValue, int replication) {
        benchmark("ToGraal-" + label, () -> ValueTranslators.REGISTRY.translateToGraal(CTX, idValue), replication);
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
