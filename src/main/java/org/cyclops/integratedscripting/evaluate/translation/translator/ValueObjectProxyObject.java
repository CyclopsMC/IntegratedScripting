package org.cyclops.integratedscripting.evaluate.translation.translator;

import lombok.SneakyThrows;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.core.evaluate.operator.CurriedOperator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeBase;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integrateddynamics.core.evaluate.variable.Variable;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * A Graal proxy object for object values.
 * @author rubensworks
 */
public class ValueObjectProxyObject<V extends IValue> implements ProxyObject {

    private final Context context;
    private final IEvaluationExceptionFactory exceptionFactory;
    private final ValueObjectTypeBase<V> valueType;
    private final V value;
    private final Map<String, IOperator> methods;
    private final String memberNbtKey;

    @Nullable
    private Value memberNbtValue;

    public ValueObjectProxyObject(Context context, IEvaluationExceptionFactory exceptionFactory,
                                  String memberNbtKey, Map<String, IOperator> methods,
                                  ValueObjectTypeBase<V> valueType, V value) {
        this.context = context;
        this.exceptionFactory = exceptionFactory;
        this.valueType = valueType;
        this.methods = methods;
        this.value = value;
        this.memberNbtKey = memberNbtKey;
    }

    public ValueObjectTypeBase<V> getValueType() {
        return valueType;
    }

    @Nullable
    public IValue getValue() {
        return value;
    }

    @SneakyThrows
    @Override
    public Object getMember(String key) {
        IOperator operator = methods.get(key);
        if (operator != null) {
            CurriedOperator curriedOperator = new CurriedOperator(operator, new Variable(value));
            return ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeOperator.ValueOperator.of(curriedOperator), exceptionFactory);
        }
        if (key.equals(this.memberNbtKey)) {
            if (this.memberNbtValue == null) {
                Tag tag = this.valueType.serialize(this.value);
                this.memberNbtValue = ValueTranslators.TRANSLATOR_NBT.translateTag(context, tag, exceptionFactory);
            }
            return this.memberNbtValue;
        }
        return null;
    }

    @Override
    public Object getMemberKeys() {
        return new String[]{ this.memberNbtKey };
    }

    @Override
    public boolean hasMember(String key) {
        return key.equals(this.memberNbtKey) || (this.methods != null && this.methods.containsKey(key));
    }

    @SneakyThrows
    @Override
    public void putMember(String key, Value value) {
        throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.proxyobject_putMember", key, valueType.getTypeName()));
    }
}
