package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.apache.commons.compress.utils.Lists;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueTypeListProxy;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.evaluate.translation.IValueTranslator;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rubensworks
 */
public class ValueTranslatorList implements IValueTranslator<ValueTypeList.ValueList> {
    @Override
    public boolean canHandleValueType(IValueType<?> valueType) {
        return valueType == ValueTypes.LIST;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.hasArrayElements();
    }

    @Override
    public boolean canTranslateNbt() {
        return true;
    }

    @Override
    public Value translateToGraal(Context context, ValueTypeList.ValueList value) throws EvaluationException {
        if (value.getRawValue().isInfinite()) {
            throw new EvaluationException(Component.translatable("valuetype.integratedscripting.error.translation.list_infinite"));
        }

        // The following should work according to the docs, but doesn't. So we fallback to a sub-optimal approach.
        List<Value> list = new ArrayList<>();
        for (IValue innerValue : (Iterable<IValue>) value.getRawValue()) {
            Value translate = ValueTranslators.REGISTRY.translateToGraal(context, innerValue);
            list.add(translate);
        }
        return context.asValue(list);
    }

    @Override
    public ValueTypeList.ValueList translateFromGraal(Context context, Value value) throws EvaluationException {
        List<IValue> values = Lists.newArrayList();
        long length = value.getArraySize();
        for (long i = 0; i < length; i++) {
            values.add(ValueTranslators.REGISTRY.translateFromGraal(context, value.getArrayElement(i)));
        }
        return ValueTypeList.ValueList.ofList(ValueTypes.CATEGORY_ANY, values);
    }

    @Override
    public Tag translateToNbt(Context context, ValueTypeList.ValueList value) throws EvaluationException {
        if (value.getRawValue().isInfinite()) {
            throw new EvaluationException(Component.translatable("valuetype.integratedscripting.error.translation.list_infinite"));
        }

        ListTag tag = new ListTag();
        for (IValue subValue : (IValueTypeListProxy<IValueType<IValue>, IValue>) value.getRawValue()) {
            tag.add(ValueTranslators.REGISTRY.translateToNbt(context, subValue));
        }
        return tag;
    }
}
