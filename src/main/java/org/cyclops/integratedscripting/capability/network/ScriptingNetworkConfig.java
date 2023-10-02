package org.cyclops.integratedscripting.capability.network;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.cyclops.cyclopscore.config.extendedconfig.CapabilityConfig;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;

/**
 * Config for the item network capability.
 * @author rubensworks
 *
 */
public class ScriptingNetworkConfig extends CapabilityConfig<IScriptingNetwork> {

    public static Capability<IScriptingNetwork> CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    public ScriptingNetworkConfig() {
        super(
                IntegratedScripting._instance,
                "scriptingNetwork",
                IScriptingNetwork.class
        );
    }

}
