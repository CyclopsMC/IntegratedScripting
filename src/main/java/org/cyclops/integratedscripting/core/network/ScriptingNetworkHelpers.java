package org.cyclops.integratedscripting.core.network;

import net.minecraftforge.common.util.LazyOptional;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;
import org.cyclops.integratedscripting.capability.network.ScriptingNetworkConfig;

import javax.annotation.Nullable;

/**
 * @author rubensworks
 */
public class ScriptingNetworkHelpers {

    /**
     * Get the scripting data.
     * @return The scripting data.
     */
    public static IScriptingData getScriptingData() {
        return IntegratedScripting._instance.scriptingData;
    }

    /**
     * Get the scripting network capability of a network.
     * @param network The network.
     * @return The optional scripting network.
     */
    public static LazyOptional<IScriptingNetwork> getScriptingNetwork(@Nullable INetwork network) {
        if (network == null) {
            return LazyOptional.empty();
        }
        return network.getCapability(ScriptingNetworkConfig.CAPABILITY);
    }

}
