package org.cyclops.integratedscripting.evaluate.translation.translator;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeBase;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeNbt;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * @author rubensworks
 */
public class ValueTranslatorObjectAdapter<V extends IValue> implements IValueTranslator<V> {

    private final String key;
    private final Set<String> keys;
    private final ValueObjectTypeBase<V> valueType;

    @Nullable
    private Map<String, IOperator> methodsCache;

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
        if (value.isProxyObject()) {
            try {
                NbtCompoundTagProxyObject proxyObject = value.asProxyObject();
                return proxyObject.getValue() != null && proxyObject.getValue().getType() == this.valueType;
            } catch (ClassCastException e) {
                // Ignore error
            }
        }
        return value.getMemberKeys().equals(this.keys);
    }

    @Override
    public boolean canTranslateNbt() {
        return true;
    }

    protected Map<String, IOperator> getMethods() {
        // Create a reusable prototype containing value-specific methods.
        Map<String, IOperator> methods = methodsCache;
        if (methods == null) {
            // Determine applicable types for this value
            Set<IValueType<?>> valueTypes = Sets.newHashSet();
            for (IValueType<?> valueType : ValueTypes.REGISTRY.getValueTypes()) {
                if (valueType.correspondsTo(this.valueType)) {
                    valueTypes.add(valueType);
                }
            }

            // Create the actual prototype and assign methods as members
            methods = Maps.newHashMap();
            for (IValueType<?> valueType : valueTypes) {
                Map<String, IOperator> scopedOperators = Operators.REGISTRY.getScopedInteractOperators().get(valueType);
                if (scopedOperators != null) {
                    for (Map.Entry<String, IOperator> entry : scopedOperators.entrySet()) {
                        methods.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            methodsCache = methods;
        }

        return methods;
    }

    @Override
    public Value translateToGraal(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        CompoundTag tag = new CompoundTag();
        Tag subTag = this.valueType.serialize(value);
        tag.put(this.key, subTag);

        return ValueTranslators.TRANSLATOR_NBT.translateCompoundTag(context, tag, exceptionFactory, getMethods(), value);
    }

    @Override
    public V translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        // Unwrap the value if it was translated in the opposite direction before.
        if (value.isProxyObject()) {
            try {
                NbtCompoundTagProxyObject proxy = value.asProxyObject();
                return (V) proxy.getValue();
            } catch (ClassCastException classCastException) {
                // Fallback to case below
            }
        }

        Value idBlock = value.getMember(this.key);
        ValueTypeNbt.ValueNbt valueNbt = ValueTranslators.REGISTRY.translateFromGraal(context, idBlock, exceptionFactory);
        return this.valueType.deserialize(valueNbt.getRawValue().orElseThrow());
    }

    @Override
    public Tag translateToNbt(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        throw new UnsupportedOperationException("translateToNbt is not supported");
    }
}
