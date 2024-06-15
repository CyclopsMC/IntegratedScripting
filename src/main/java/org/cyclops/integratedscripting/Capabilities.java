package org.cyclops.integratedscripting;

import net.minecraft.resources.ResourceLocation;
import org.cyclops.integrateddynamics.api.network.NetworkCapability;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;

/**
 * @author rubensworks
 */
public class Capabilities {
    public static final class ScriptingNetwork {
        public static final NetworkCapability<IScriptingNetwork> NETWORK = NetworkCapability.create(new ResourceLocation(Reference.MOD_ID, "scripting_network"), IScriptingNetwork.class);
    }
}
