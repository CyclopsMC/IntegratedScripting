package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeDouble;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class ValueTranslatorDouble implements IValueTranslator<ValueTypeDouble.ValueDouble> {

    @Override
    public IValueType<?> getValueType() {
        return ValueTypes.DOUBLE;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.fitsInDouble();
    }

    @Override
    public boolean canTranslateNbt() {
        return true;
    }

    @Override
    public Value translateToGraal(Context context, ValueTypeDouble.ValueDouble value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) {
        return context.asValue(value.getRawValue());
    }

    @Override
    public ValueTypeDouble.ValueDouble translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) {
        return ValueTypeDouble.ValueDouble.of(value.asDouble());
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeDouble.ValueDouble value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        return DoubleTag.valueOf(value.getRawValue());
    }
}
