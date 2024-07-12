package org.cyclops.integratedscripting.api.evaluate.translation;

import net.minecraft.nbt.Tag;
import org.cyclops.cyclopscore.init.IRegistry;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import javax.annotation.Nullable;

/**
 * Registry for {@link IValueTranslator}'s.
 * @author rubensworks
 */
public interface IValueTranslatorRegistry extends IRegistry {

    public void register(IValueTranslator translator);

    @Nullable
    public <V extends IValue> IValueTranslator getValueTypeTranslator(IValueType<V> valueType);

    public <V extends IValue> Value translateToGraal(Context context, V value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException;

    @Nullable
    public IValueTranslator getScriptValueTranslator(Value scriptValue);

    public <V extends IValue> V translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException;

    public <V extends IValue> Tag translateToNbt(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException;

}
