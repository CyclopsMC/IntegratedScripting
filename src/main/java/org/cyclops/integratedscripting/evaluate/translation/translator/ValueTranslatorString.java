package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeString;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class ValueTranslatorString implements IValueTranslator<ValueTypeString.ValueString> {

    @Override
    public IValueType<?> getValueType() {
        return ValueTypes.STRING;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.isString();
    }

    @Override
    public boolean canTranslateNbt() {
        return true;
    }

    @Override
    public Value translateToGraal(Context context, ValueTypeString.ValueString value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) {
        return context.asValue(value.getRawValue());
    }

    @Override
    public ValueTypeString.ValueString translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) {
        return ValueTypeString.ValueString.of(value.asString());
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeString.ValueString value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        return StringTag.valueOf(value.getRawValue());
    }
}
