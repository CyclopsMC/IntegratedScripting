package org.cyclops.integratedscripting.api.evaluate.translation;

import net.minecraft.nbt.Tag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import javax.annotation.Nullable;

/**
 * Translates ID values to and from Graal values.
 * @author rubensworks
 */
public interface IValueTranslator<V extends IValue> {

    public IValueType<?> getValueType();

    public boolean canHandleGraalValue(Value value);

    /**
     * If this translator handles Graal values that have exactly one member with a fixed key,
     * then returning that key here allows {@link IValueTranslatorRegistry} to dispatch on it directly.
     *
     * This is purely an optimization: it avoids having to inspect the member keys of a value
     * once for every such translator, which is relatively expensive as it crosses the host boundary.
     * Translators returning a non-null key here must handle exactly those Graal values
     * whose member keys are exactly the returned key.
     *
     * @return The single member key this translator dispatches on, or null if it dispatches differently.
     */
    @Nullable
    public default String getGraalValueMemberKey() {
        return null;
    }

    boolean canTranslateNbt();

    public Value translateToGraal(Context context, V value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException;

    public V translateFromGraal(Context context, Value value, IEvaluationExceptionFactory exceptionFactory, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException;

    public Tag translateToNbt(Context context, V value, IEvaluationExceptionFactory exceptionFactory) throws EvaluationException;
}
