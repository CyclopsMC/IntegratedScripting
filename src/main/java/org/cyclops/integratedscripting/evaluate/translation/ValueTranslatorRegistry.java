package org.cyclops.integratedscripting.evaluate.translation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslatorRegistry;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.List;
import java.util.Map;

/**
 * @author rubensworks
 */
public class ValueTranslatorRegistry implements IValueTranslatorRegistry {

    private static ValueTranslatorRegistry INSTANCE = new ValueTranslatorRegistry();

    private final List<IValueTranslator> translators = Lists.newArrayList();
    private final Map<IValueType<?>, IValueTranslator> valueTypeTranslators = Maps.newIdentityHashMap();

    private ValueTranslatorRegistry() {
    }

    /**
     * @return The unique instance.
     */
    public static ValueTranslatorRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public void register(IValueTranslator translator) {
        translators.add(translator);
        valueTypeTranslators.put(translator.getValueType(), translator);
    }

    @Override
    public <V extends IValue> IValueTranslator getValueTypeTranslator(IValueType<V> valueType) {
        return valueTypeTranslators.get(valueType);
    }

    @Override
    public <V extends IValue> Value translateToGraal(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        IValueTranslator translator = getValueTypeTranslator(value.getType());
        if (translator == null) {
            throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.unknown_to_graal", value.getType()));
        }
        return translator.translateToGraal(context, value, exceptionFactory);
    }

    @Override
    public IValueTranslator getScriptValueTranslator(Value scriptValue) {
        for (IValueTranslator translator : translators) {
            if (translator.canHandleGraalValue(scriptValue)) {
                return translator;
            }
        }
        return null;
    }

    @Override
    public <V extends IValue> V translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        IValueTranslator translator = getScriptValueTranslator(value);
        if (translator == null) {
            throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.unknown_from_graal", value));
        }
        return (V) translator.translateFromGraal(context, value, exceptionFactory);
    }

    @Override
    public <V extends IValue> Tag translateToNbt(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        IValueTranslator translator = getValueTypeTranslator(value.getType());
        if (translator == null) {
            throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.unknown_to_graal_nbt", value.getType()));
        }
        if (translator.canTranslateNbt()) {
            return translator.translateToNbt(context, value, exceptionFactory);
        }
        throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.nbt_unmatched", value.getType()));
    }
}
