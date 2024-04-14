package org.cyclops.integratedscripting.api.evaluate.translation;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;

/**
 * Construct evaluation exceptions with a given message.
 * @author rubensworks
 */
public interface IEvaluationExceptionFactory {

    public default EvaluationException createError(String message) {
        return this.createError(Component.literal(message));
    }

    public EvaluationException createError(MutableComponent message);

}
