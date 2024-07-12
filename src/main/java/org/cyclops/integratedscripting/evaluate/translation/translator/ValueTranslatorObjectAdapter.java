package org.cyclops.integratedscripting.evaluate.translation.translator;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
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

    public String getKey() {
        return key;
    }

    @Override
    public IValueType<?> getValueType() {
        return this.valueType;
    }

    @Override
    public boolean canHandleGraalValue(Value value) {
        if (value.isProxyObject()) {
            try {
                ValueObjectProxyObject<?> proxyObject = value.asProxyObject();
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
                    methods.putAll(scopedOperators);
                }
            }

            methodsCache = methods;
        }

        return methods;
    }

    @Override
    public Value translateToGraal(Context context, V value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException {
        return context.asValue(new ValueObjectProxyObject<>(context, exceptionFactory, this.key, getMethods(), this.valueType, value, valueDeseralizationContext));
    }

    @Override
    public V translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException {
        // Unwrap the value if it was translated in the opposite direction before.
        if (value.isProxyObject()) {
            try {
                ValueObjectProxyObject proxyObject = value.asProxyObject();
                return (V) proxyObject.getValue();
            } catch (ClassCastException e) {
                // Fallback to case below
            }
        }

        Value idBlock = value.getMember(this.key);
        ValueTypeNbt.ValueNbt valueNbt = ValueTranslators.REGISTRY.translateFromGraal(context, idBlock, exceptionFactory, valueDeseralizationContext);
        return this.valueType.deserialize(valueDeseralizationContext, valueNbt.getRawValue().orElseThrow());
    }

    @Override
    public Tag translateToNbt(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.unsupported_translateToNbt", Component.translatable(value.getType().getTranslationKey()), value.getType().toCompactString(value)));
    }
}
