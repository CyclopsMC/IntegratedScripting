package org.cyclops.integratedscripting.evaluate.translation.translator;

import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeNbt;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.evaluate.variable.Variable;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueProxy;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import javax.annotation.Nullable;

/**
 * A Graal proxy executable for operator values.
 * @author rubensworks
 */
public class OperatorProxyExecutable implements ProxyExecutable, IValueProxy {
    private final Context context;
    private final ValueTypeOperator.ValueOperator value;
    private final IEvaluationExceptionFactory exceptionFactory;
    private final ValueDeseralizationContext valueDeseralizationContext;

    public OperatorProxyExecutable(Context context, ValueTypeOperator.ValueOperator value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) {
        this.context = context;
        this.value = value;
        this.exceptionFactory = exceptionFactory;
        this.valueDeseralizationContext = valueDeseralizationContext;
    }

    public ValueTypeOperator.ValueOperator getValue() {
        return value;
    }

    @Override
    public IValueType<?> getProxiedValueType() {
        return ValueTypes.OPERATOR;
    }

    @Override
    public Object execute(Value... args) {
        try {
            IOperator operator = value.getRawValue();
            IValueType[] inputTypes = operator.getInputTypes();
            IVariable<IValue>[] variables = new IVariable[args.length];
            for (int i = 0; i < args.length; i++) {
                variables[i] = new Variable<>(translateArgument(args[i], i < inputTypes.length ? inputTypes[i] : null));
            }
            return ValueTranslators.REGISTRY.translateToGraal(context, operator.evaluate(variables), exceptionFactory, valueDeseralizationContext);
        } catch (EvaluationException e) {
            ScriptHelpers.sneakyThrow(e);
            return null;
        }
    }

    /**
     * Translate a single argument of this operator.
     *
     * Only compound tags keep their NBT type when they are translated to a script,
     * all other tags become plain script values, and an absent NBT value becomes null.
     * These are translated back to NBT here, so that they can be passed to operators again.
     *
     * @param arg A script value.
     * @param inputType The value type the operator expects for this argument, if known.
     * @return The translated value.
     * @throws EvaluationException If translation failed.
     */
    protected IValue translateArgument(Value arg, @Nullable IValueType<?> inputType) throws EvaluationException {
        if (inputType == ValueTypes.NBT) {
            if (arg.isNull()) {
                return ValueTypeNbt.ValueNbt.of();
            }
            IValue value = ValueTranslators.REGISTRY.translateFromGraal(context, arg, exceptionFactory, valueDeseralizationContext);
            if (value.getType() != ValueTypes.NBT) {
                return ValueTypeNbt.ValueNbt.of(ValueTranslators.REGISTRY.translateToNbt(context, value, exceptionFactory));
            }
            return value;
        }
        return ValueTranslators.REGISTRY.translateFromGraal(context, arg, exceptionFactory, valueDeseralizationContext);
    }
}
