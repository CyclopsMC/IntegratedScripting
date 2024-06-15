package org.cyclops.integratedscripting.capability;

import net.neoforged.bus.api.SubscribeEvent;
import org.cyclops.cyclopscore.modcompat.capabilities.DefaultCapabilityProvider;
import org.cyclops.integrateddynamics.api.network.AttachCapabilitiesEventNetwork;
import org.cyclops.integratedscripting.Capabilities;
import org.cyclops.integratedscripting.core.network.ScriptingNetwork;

/**
 * @author rubensworks
 */
public class ScriptingNetworkCapabilityConstructors {

    @SubscribeEvent
    public void onNetworkLoad(AttachCapabilitiesEventNetwork event) {
        ScriptingNetwork scriptingNetwork = new ScriptingNetwork();
        event.register(Capabilities.ScriptingNetwork.NETWORK, new DefaultCapabilityProvider<>(scriptingNetwork));
    }

}
