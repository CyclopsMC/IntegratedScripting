package org.cyclops.integratedscripting.evaluate.translation.translator;

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
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
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
    private Value prototypeCache;

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

    /**
     * This will create instances based on a prototype containing valuetype-specific methods.
     * These methods are derived from the operators that are applicable for the current value type(s).
     * @param context The context.
     * @param exceptionFactory Factory for exceptions.
     * @return The instance constructor.
     * @throws EvaluationException If an evaluation error occurs.
     */
    protected IInstanceConstructor getInstanceConstructor(Context context, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        // Create a reusable prototype containing value-specific methods.
        Value prototype = prototypeCache;
        if (prototype == null) {
            // Determine applicable types for this value
            Set<IValueType<?>> valueTypes = Sets.newHashSet();
            for (IValueType<?> valueType : ValueTypes.REGISTRY.getValueTypes()) {
                if (valueType.correspondsTo(this.valueType)) {
                    valueTypes.add(valueType);
                }
            }

            // Prepare Graal values
            Value jsBindings = context.getBindings("js");
            Value jsObjectClass = jsBindings.getMember("Object");
            Value jsProxyClass = jsBindings.getMember("Proxy");
            Value operatorCurrier = context.eval("js", "(operator, thisArg, remainingArgs) => operator(thisArg, ...remainingArgs)");

            // Create the actual prototype and assign methods as members
            prototype = jsObjectClass.newInstance();
            for (IValueType<?> valueType : valueTypes) {
                Map<String, IOperator> scopedOperators = Operators.REGISTRY.getScopedInteractOperators().get(valueType);
                if (scopedOperators != null) {
                    for (Map.Entry<String, IOperator> entry : scopedOperators.entrySet()) {
                        // Create a JS Proxy that curries all operators.
                        // Concretely, the "this" value in JS will be passed as first argument to the operator.
                        // We have to do it this way because Graal does not offer a way to get the JS "this" from Java-land.
                        Value proxyHandler = jsObjectClass.newInstance();
                        proxyHandler.putMember("apply", operatorCurrier);
                        Value objectMethod = jsProxyClass.newInstance(ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeOperator.ValueOperator.of(entry.getValue()), exceptionFactory), proxyHandler);

                        prototype.putMember(entry.getKey(), objectMethod);
                    }
                }
            }
            prototypeCache = prototype;
        }

        // Create instances based on the prototype.
        Value finalPrototype = prototype;
        return (subContext) -> subContext.getBindings("js").getMember("Object").invokeMember("create", finalPrototype);
    }

    @Override
    public Value translateToGraal(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        CompoundTag tag = new CompoundTag();
        Tag subTag = this.valueType.serialize(value);
        tag.put(this.key, subTag);

        return ValueTranslators.TRANSLATOR_NBT.translateCompoundTag(context, tag, getInstanceConstructor(context, exceptionFactory));
    }

    @Override
    public V translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        Value idBlock = value.getMember(this.key);
        ValueTypeNbt.ValueNbt valueNbt = ValueTranslators.REGISTRY.translateFromGraal(context, idBlock, exceptionFactory);
        return this.valueType.deserialize(valueNbt.getRawValue().orElseThrow());
    }

    @Override
    public Tag translateToNbt(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        throw new UnsupportedOperationException("translateToNbt is not supported");
    }

    public static interface IInstanceConstructor {
        public Value construct(Context context) throws EvaluationException;
    }
}
