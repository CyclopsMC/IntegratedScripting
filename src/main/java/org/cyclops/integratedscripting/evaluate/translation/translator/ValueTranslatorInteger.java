package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class ValueTranslatorInteger implements IValueTranslator<ValueTypeInteger.ValueInteger> {

    @Override
    public IValueType<?> getValueType() {
        return ValueTypes.INTEGER;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.fitsInInt();
    }

    @Override
    public boolean canTranslateNbt() {
        return true;
    }

    @Override
    public Value translateToGraal(Context context, ValueTypeInteger.ValueInteger value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) {
        return context.asValue(value.getRawValue());
    }

    @Override
    public ValueTypeInteger.ValueInteger translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) {
        return ValueTypeInteger.ValueInteger.of(value.asInt());
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeInteger.ValueInteger value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        return IntTag.valueOf(value.getRawValue());
    }
}
