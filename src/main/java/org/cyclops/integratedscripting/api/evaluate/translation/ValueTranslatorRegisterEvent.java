package org.cyclops.integratedscripting.api.evaluate.translation;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.ModLifecycleEvent;

/**
 * This event is emitted on the mod lifecycle bus.
 * @author rubensworks
 */
public class ValueTranslatorRegisterEvent extends ModLifecycleEvent {

    private final IValueTranslatorRegistry registry;

    public ValueTranslatorRegisterEvent(ModContainer container, IValueTranslatorRegistry registry) {
        super(container);
        this.registry = registry;
    }

    public IValueTranslatorRegistry getRegistry() {
        return registry;
    }
}
