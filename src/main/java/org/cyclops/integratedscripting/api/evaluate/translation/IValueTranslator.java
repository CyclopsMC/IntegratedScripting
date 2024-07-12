package org.cyclops.integratedscripting.api.evaluate.translation;

import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * Translates ID values to and from Graal values.
 * @author rubensworks
 */
public interface IValueTranslator<V extends IValue> {

    public IValueType<?> getValueType();

    public boolean canHandleGraalValue(Value value);

    boolean canTranslateNbt();

    public Value translateToGraal(Context context, V value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException;

    public V translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException;

    public Tag translateToNbt(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException;
}
