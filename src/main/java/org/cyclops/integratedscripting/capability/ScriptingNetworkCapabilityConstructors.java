package org.cyclops.integratedscripting.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.cyclops.cyclopscore.modcompat.capabilities.DefaultCapabilityProvider;
import org.cyclops.integrateddynamics.api.network.AttachCapabilitiesEventNetwork;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.capability.network.ScriptingNetworkConfig;
import org.cyclops.integratedscripting.core.network.ScriptingNetwork;

/**
 * @author rubensworks
 */
public class ScriptingNetworkCapabilityConstructors {

    @SubscribeEvent
    public void onNetworkLoad(AttachCapabilitiesEventNetwork event) {
        ScriptingNetwork scriptingNetwork = new ScriptingNetwork();
        event.addCapability(
                new ResourceLocation(Reference.MOD_ID, "scripting_network"),
                new DefaultCapabilityProvider<>(() -> ScriptingNetworkConfig.CAPABILITY, scriptingNetwork)
        );
    }

}
