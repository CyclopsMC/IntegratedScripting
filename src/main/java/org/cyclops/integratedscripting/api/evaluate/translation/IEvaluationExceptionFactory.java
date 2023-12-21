package org.cyclops.integratedscripting.api.evaluate.translation;

import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;

/**
 * Construct evaluation exceptions with a given message.
 * @author rubensworks
 */
public interface IEvaluationExceptionFactory {

    public EvaluationException createError(String message);

}
