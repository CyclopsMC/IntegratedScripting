package org.cyclops.integratedscripting.evaluate.translation.translator;

import com.google.common.collect.Sets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeBase;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeNbt;
import org.cyclops.integratedscripting.evaluate.translation.IValueTranslator;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Set;

/**
 * @author rubensworks
 */
public class ValueTranslatorObjectAdapter<V extends IValue> implements IValueTranslator<V> {

    private final String key;
    private final Set<String> keys;
    private final ValueObjectTypeBase<V> valueType;

    public ValueTranslatorObjectAdapter(String key, ValueObjectTypeBase<V> valueType) {
        this.key = key;
        this.keys = Sets.newHashSet(this.key);
        this.valueType = valueType;
    }

    @Override
    public boolean canHandleValueType(IValueType<?> valueType) {
        return valueType == this.valueType;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        return value.getMemberKeys().equals(this.keys);
    }

    @Override
    public boolean canTranslateNbt() {
        return true;
    }

    @Override
    public Value translateToGraal(Context context, V value) throws EvaluationException {
        CompoundTag tag = new CompoundTag();
        Tag subTag = this.valueType.serialize(value);
        tag.put(this.key, subTag);
        return ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeNbt.ValueNbt.of(tag));
    }

    @Override
    public V translateFromGraal(Context context, Value value) throws EvaluationException {
        Value idBlock = value.getMember(this.key);
        ValueTypeNbt.ValueNbt valueNbt = ValueTranslators.REGISTRY.translateFromGraal(context, idBlock);
        return this.valueType.deserialize(valueNbt.getRawValue().orElseThrow());
    }

    @Override
    public Tag translateToNbt(Context context, V value) throws EvaluationException {
        throw new UnsupportedOperationException("translateToNbt is not supported");
    }
}
