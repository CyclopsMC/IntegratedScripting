package org.cyclops.integratedscripting.core.client.model;

import net.minecraft.resources.ResourceLocation;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.client.model.IVariableModelProviderRegistry;
import org.cyclops.integrateddynamics.core.client.model.SingleVariableModelProvider;
import org.cyclops.integratedscripting.Reference;

/**
 * Collection of variable model providers.
 * @author rubensworks
 */
public class ScriptingVariableModelProviders {

    public static final IVariableModelProviderRegistry REGISTRY = IntegratedDynamics._instance.getRegistryManager().getRegistry(IVariableModelProviderRegistry.class);

    public static final SingleVariableModelProvider SCRIPT = REGISTRY.addProvider(new SingleVariableModelProvider(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "customoverlay/script")));

    public static void load() {}

}
