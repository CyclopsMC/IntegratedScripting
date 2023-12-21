package org.cyclops.integratedscripting.evaluate.translation;

import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslatorRegistry;
import org.cyclops.integratedscripting.evaluate.translation.translator.*;

/**
 * @author rubensworks
 */
public class ValueTranslators {

    public static final IValueTranslatorRegistry REGISTRY = constructRegistry();
    public static ValueTranslatorNbt TRANSLATOR_NBT;

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

        // Object types
        REGISTRY.register(new ValueTranslatorObjectAdapter<>("id_block", ValueTypes.OBJECT_BLOCK));
        REGISTRY.register(new ValueTranslatorObjectAdapter<>("id_item", ValueTypes.OBJECT_ITEMSTACK));
        REGISTRY.register(new ValueTranslatorObjectAdapter<>("id_entity", ValueTypes.OBJECT_ENTITY));
        REGISTRY.register(new ValueTranslatorObjectAdapter<>("id_fluid", ValueTypes.OBJECT_FLUIDSTACK));
        REGISTRY.register(new ValueTranslatorObjectAdapter<>("id_ingredients", ValueTypes.OBJECT_INGREDIENTS));
        REGISTRY.register(new ValueTranslatorObjectAdapter<>("id_recipe", ValueTypes.OBJECT_RECIPE));

        // NBT has last priority
        REGISTRY.register(TRANSLATOR_NBT = new ValueTranslatorNbt());
    }

}
