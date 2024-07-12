package org.cyclops.integratedscripting.evaluate.translation.translator;

import lombok.SneakyThrows;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integrateddynamics.core.evaluate.variable.Variable;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

/**
 * A Graal proxy executable for operator values.
 * @author rubensworks
 */
public class OperatorProxyExecutable implements ProxyExecutable {
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

    @SneakyThrows
    @Override
    public Object execute(Value... args) {
        IVariable<IValue>[] variables = new IVariable[args.length];
        for (int i = 0; i < args.length; i++) {
            variables[i] = new Variable<>(ValueTranslators.REGISTRY.translateFromGraal(context, args[i], exceptionFactory, valueDeseralizationContext));
        }
        return ValueTranslators.REGISTRY.translateToGraal(context, value.getRawValue().evaluate(variables), exceptionFactory, valueDeseralizationContext);
    }
}
