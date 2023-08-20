package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeLong;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.evaluate.translation.IValueTranslator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class ValueTranslatorLong implements IValueTranslator<ValueTypeLong.ValueLong> {
    @Override
    public boolean canHandleValueType(IValueType<?> valueType) {
        return valueType == ValueTypes.LONG;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.fitsInLong();
    }

    @Override
    public boolean canTranslateNbt() {
        return true;
    }

    @Override
    public Value translateToGraal(Context context, ValueTypeLong.ValueLong value) {
        return context.asValue(value.getRawValue());
    }

    @Override
    public ValueTypeLong.ValueLong translateFromGraal(Context context, Value value) {
        return ValueTypeLong.ValueLong.of(value.asLong());
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeLong.ValueLong value) throws EvaluationException {
        return LongTag.valueOf(value.getRawValue());
    }
}
