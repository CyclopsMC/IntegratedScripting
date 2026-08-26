package org.cyclops.integratedscripting.evaluate.translation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslator;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslatorRegistry;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rubensworks
 */
public class ValueTranslatorRegistry implements IValueTranslatorRegistry {

    private static ValueTranslatorRegistry INSTANCE = new ValueTranslatorRegistry();

    private final List<IValueTranslator> translators = Lists.newArrayList();
    private final Map<IValueType<?>, IValueTranslator> valueTypeTranslators = Maps.newIdentityHashMap();

    // Snapshots of translators, and the member keys they dispatch on, to avoid repeated lookups while dispatching.
    private IValueTranslator[] translatorsArray = new IValueTranslator[0];
    private String[] translatorMemberKeys = new String[0];

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

        this.translatorsArray = translators.toArray(new IValueTranslator[0]);
        this.translatorMemberKeys = translators.stream()
                .map(IValueTranslator::getGraalValueMemberKey)
                .toArray(String[]::new);
    }

    @Override
    public <V extends IValue> IValueTranslator getValueTypeTranslator(IValueType<V> valueType) {
        return valueTypeTranslators.get(valueType);
    }

    @Override
    public <V extends IValue> Value translateToGraal(Context context, V value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException {
        IValueTranslator translator = getValueTypeTranslator(value.getType());
        if (translator == null) {
            throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.unknown_to_graal", value.getType().getTranslationKey()));
        }
        return translator.translateToGraal(context, value, exceptionFactory, valueDeseralizationContext);
    }

    @Override
    public IValueTranslator getScriptValueTranslator(Value scriptValue) {
        // Translators that dispatch on a single member key are all matched against the same member key set,
        // which is only materialized once, and only once such a translator is actually reached.
        // Crossing the host boundary is relatively expensive,
        // so the number of calls on the Graal value is deliberately kept as low as possible here.
        Set<String> valueMemberKeys = null;
        boolean valueMembersResolved = false;

        IValueTranslator[] translators = this.translatorsArray;
        String[] translatorMemberKeys = this.translatorMemberKeys;
        for (int i = 0; i < translators.length; i++) {
            String translatorMemberKey = translatorMemberKeys[i];
            if (translatorMemberKey == null) {
                if (translators[i].canHandleGraalValue(scriptValue)) {
                    return translators[i];
                }
            } else {
                if (!valueMembersResolved) {
                    valueMembersResolved = true;
                    valueMemberKeys = scriptValue.hasMembers() ? scriptValue.getMemberKeys() : null;
                }
                if (valueMemberKeys != null
                        && valueMemberKeys.size() == 1
                        && valueMemberKeys.contains(translatorMemberKey)) {
                    return translators[i];
                }
            }
        }
        return null;
    }

    @Override
    public <V extends IValue> V translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException {
        IValueTranslator translator = getScriptValueTranslator(value);
        if (translator == null) {
            throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.unknown_from_graal", value.toString()));
        }
        return (V) translator.translateFromGraal(context, value, exceptionFactory, valueDeseralizationContext);
    }

    @Override
    public <V extends IValue> Tag translateToNbt(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException {
        IValueTranslator translator = getValueTypeTranslator(value.getType());
        if (translator == null) {
            throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.unknown_to_graal_nbt", value.getType().getTypeName()));
        }
        if (translator.canTranslateNbt()) {
            return translator.translateToNbt(context, value, exceptionFactory);
        }
        throw exceptionFactory.createError(Component.translatable("valuetype.integratedscripting.error.translation.nbt_unmatched", value.getType().getTypeName()));
    }
}
