package org.cyclops.integratedscripting.evaluate.translation;

import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.evaluate.translation.translator.ValueTranslatorBoolean;
import org.cyclops.integratedscripting.evaluate.translation.translator.ValueTranslatorDouble;
import org.cyclops.integratedscripting.evaluate.translation.translator.ValueTranslatorInteger;
import org.cyclops.integratedscripting.evaluate.translation.translator.ValueTranslatorList;
import org.cyclops.integratedscripting.evaluate.translation.translator.ValueTranslatorLong;
import org.cyclops.integratedscripting.evaluate.translation.translator.ValueTranslatorNbt;
import org.cyclops.integratedscripting.evaluate.translation.translator.ValueTranslatorOperator;
import org.cyclops.integratedscripting.evaluate.translation.translator.ValueTranslatorString;

/**
 * @author rubensworks
 */
public class ValueTranslators {

    public static final IValueTranslatorRegistry REGISTRY = constructRegistry();

    private static IValueTranslatorRegistry constructRegistry() {
        // This also allows this registry to be used outside of a minecraft environment.
        if(MinecraftHelpers.isModdedEnvironment()) {
            return IntegratedScripting._instance.getRegistryManager().getRegistry(IValueTranslatorRegistry.class);
        } else {
            return ValueTranslatorRegistry.getInstance();
        }
    }

    public static void load() {
        // Raw value types
        REGISTRY.register(new ValueTranslatorBoolean());
        REGISTRY.register(new ValueTranslatorInteger());
        REGISTRY.register(new ValueTranslatorLong());
        REGISTRY.register(new ValueTranslatorDouble());
        REGISTRY.register(new ValueTranslatorString());
        REGISTRY.register(new ValueTranslatorList());
        REGISTRY.register(new ValueTranslatorOperator());
        REGISTRY.register(new ValueTranslatorNbt());

        // Object types
        // TODO
    }

}
