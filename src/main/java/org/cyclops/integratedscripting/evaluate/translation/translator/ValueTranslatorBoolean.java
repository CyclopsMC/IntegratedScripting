package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.evaluate.translation.IValueTranslator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class ValueTranslatorBoolean implements IValueTranslator<ValueTypeBoolean.ValueBoolean> {
    @Override
    public boolean canHandleValueType(IValueType<?> valueType) {
        return valueType == ValueTypes.BOOLEAN;
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
    public Value translateToGraal(Context context, ValueTypeBoolean.ValueBoolean value) {
        return context.asValue(value.getRawValue());
    }

    @Override
    public ValueTypeBoolean.ValueBoolean translateFromGraal(Context context, Value value) {
        return ValueTypeBoolean.ValueBoolean.of(value.asBoolean());
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeBoolean.ValueBoolean value) throws EvaluationException {
        return ByteTag.valueOf(value.getRawValue());
    }
}
