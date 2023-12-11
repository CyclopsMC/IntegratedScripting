package org.cyclops.integratedscripting.core.language;

import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.api.language.ILanguageHandlerRegistry;

/**
 * @author rubensworks
 */
public class LanguageHandlers {

    public static final ILanguageHandlerRegistry REGISTRY = constructRegistry();

    private static ILanguageHandlerRegistry constructRegistry() {
        // This also allows this registry to be used outside of a minecraft environment.
        if(MinecraftHelpers.isModdedEnvironment()) {
            return IntegratedScripting._instance.getRegistryManager().getRegistry(ILanguageHandlerRegistry.class);
        } else {
            return LanguageHandlerRegistry.getInstance();
        }
    }

    public static void load() {
        REGISTRY.register(new LanguageHandlerJavaScript());
    }

}
