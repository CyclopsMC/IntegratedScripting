package org.cyclops.integratedscripting.evaluate.translation.translator;

import net.minecraft.nbt.CompoundTag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueProxy;
import org.cyclops.integrateddynamics.core.evaluate.operator.CurriedOperator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integrateddynamics.core.evaluate.variable.Variable;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * A Graal proxy object for NBT CompoundTag values.
 * @author rubensworks
 */
public class NbtCompoundTagProxyObject implements ProxyObject, IValueProxy {

    private final Context context;
    private final IEvaluationExceptionFactory exceptionFactory;
    private final CompoundTag tag;

    @Nullable
    private final Map<String, IOperator> methods;
    @Nullable
    private final IValue value;
    private final ValueDeseralizationContext valueDeseralizationContext;

    public NbtCompoundTagProxyObject(Context context, IEvaluationExceptionFactory exceptionFactory, CompoundTag tag, @Nullable Map<String, IOperator> methods, @Nullable IValue value, ValueDeseralizationContext valueDeseralizationContext) {
        this.context = context;
        this.exceptionFactory = exceptionFactory;
        this.tag = tag;
        this.methods = methods;
        this.value = value;
        this.valueDeseralizationContext = valueDeseralizationContext;
    }

    public CompoundTag getTag() {
        return tag;
    }

    @Override
    public IValueType<?> getProxiedValueType() {
        return ValueTypes.NBT;
    }

    @Nullable
    public IValue getValue() {
        return value;
    }

    @Override
    public Object getMember(String key) {
        try {
            if (methods != null) {
                IOperator operator = methods.get(key);
                if (operator != null) {
                    CurriedOperator curriedOperator = new CurriedOperator(operator, new Variable(value));
                    return ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeOperator.ValueOperator.of(curriedOperator), exceptionFactory, valueDeseralizationContext);
                }
            }
            return ValueTranslators.TRANSLATOR_NBT.translateTag(context, tag.get(key), exceptionFactory, valueDeseralizationContext);
        } catch (EvaluationException e) {
            ScriptHelpers.sneakyThrow(e);
            return null;
        }
    }

    @Override
    public Object getMemberKeys() {
        return tag.keySet().toArray();
    }

    @Override
    public boolean hasMember(String key) {
        return tag.contains(key) || (this.methods != null && this.methods.containsKey(key));
    }

    @Override
    public void putMember(String key, Value value) {
        try {
            IValue idValue = ValueTranslators.REGISTRY.translateFromGraal(context, value, exceptionFactory, valueDeseralizationContext);
            tag.put(key, ValueTranslators.REGISTRY.translateToNbt(context, idValue, exceptionFactory));
        } catch (EvaluationException e) {
            ScriptHelpers.sneakyThrow(e);
        }
    }
}
