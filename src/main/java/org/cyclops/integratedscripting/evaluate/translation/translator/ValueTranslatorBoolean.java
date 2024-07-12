package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class ValueTranslatorBoolean implements IValueTranslator<ValueTypeBoolean.ValueBoolean> {

    private boolean valuesInitialized = false;
    private Value valueTrue;
    private Value valueFalse;

    @Override
    public IValueType<?> getValueType() {
        return ValueTypes.BOOLEAN;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.isBoolean();
    }

    @Override
    public boolean canTranslateNbt() {
        return true;
    }

    @Override
    public Value translateToGraal(Context context, ValueTypeBoolean.ValueBoolean value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) {
        if (!valuesInitialized) {
            valuesInitialized = true;
            valueTrue = context.asValue(true);
            valueFalse = context.asValue(false);
        }
        return value.getRawValue() ? valueTrue : valueFalse;
    }

    @Override
    public ValueTypeBoolean.ValueBoolean translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) {
        return ValueTypeBoolean.ValueBoolean.of(value.asBoolean());
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeBoolean.ValueBoolean value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        return ByteTag.valueOf(value.getRawValue());
    }
}
