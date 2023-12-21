package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.core.evaluate.operator.OperatorBase;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.evaluate.variable.Variable;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class ValueTranslatorOperator implements IValueTranslator<ValueTypeOperator.ValueOperator> {

    @Override
    public boolean canHandleValueType(IValueType<?> valueType) {
        return valueType == ValueTypes.OPERATOR;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.canExecute();
    }

    @Override
    public boolean canTranslateNbt() {
        return false;
    }

    @Override
    public Value translateToGraal(Context context, ValueTypeOperator.ValueOperator value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        return context.asValue((JavaRunnableFromGraal) args -> {
            IVariable<IValue>[] variables = new IVariable[args.length];
            for (int i = 0; i < args.length; i++) {
                variables[i] = new Variable<>(ValueTranslators.REGISTRY.translateFromGraal(context, args[i], exceptionFactory));
            }
            return ValueTranslators.REGISTRY.translateToGraal(context, value.getRawValue().evaluate(variables), exceptionFactory);
        });
    }

    @Override
    public ValueTypeOperator.ValueOperator translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        // Determine input args of the function
        int argCount = value.getMember("length").asInt();
        IValueType[] inputValueTypes = new IValueType[argCount];
        for (int i = 0; i < argCount; i++) {
            inputValueTypes[i] = ValueTypes.CATEGORY_ANY;
        }

        // Create custom operator
        return ValueTypeOperator.ValueOperator.of(new GraalOperator(inputValueTypes, args -> {
            IVariable[] variables = args.getVariables();
            Value[] values = new Value[variables.length];
            for (int i = 0; i < variables.length; i++) {
                values[i] = ValueTranslators.REGISTRY.translateToGraal(context, variables[i].getValue(), exceptionFactory);
            }
            Value returnValue;
            try {
                returnValue = value.execute((Object[]) values);
            } catch (PolyglotException e) {
                throw exceptionFactory.createError(e.getMessage());
            }
            return ValueTranslators.REGISTRY.translateFromGraal(context, returnValue, exceptionFactory);
        }));
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeOperator.ValueOperator value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        throw new UnsupportedOperationException("translateToNbt is not supported");
    }

    @FunctionalInterface
    public static interface JavaRunnableFromGraal {
        public Value run(Value... args) throws EvaluationException;
    }

    public static class GraalOperator extends OperatorBase {
        protected GraalOperator(IValueType[] inputTypes, IFunction function) {
            super("graal", "graal", "graal", null, false, inputTypes, ValueTypes.CATEGORY_ANY, function, null);
        }

        @Override
        protected String getUnlocalizedType() {
            return "graal";
        }
    }

}
