package org.cyclops.integratedscripting.api.evaluate.translation;

import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;

/**
 * A Graal proxy that wraps an Integrated Dynamics value of a known value type.
 *
 * Proxies implementing this interface can be mapped to their {@link IValueTranslator} directly,
 * instead of having to fall back to a linear scan over all registered translators.
 *
 * @author rubensworks
 */
public interface IValueProxy {

    /**
     * @return The value type of the Integrated Dynamics value that is being proxied.
     */
    public IValueType<?> getProxiedValueType();

}
