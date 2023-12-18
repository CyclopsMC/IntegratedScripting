package org.cyclops.integratedscripting.api.network;

import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;

/**
 * A script member that contains a value.
 * @author rubensworks
 */
public interface IScriptMember {

    /**
     * @return The member value as Integrated Dynamics value.
     * @throws EvaluationException If an error occurred.
     */
    public IValue getValue() throws EvaluationException;

}
