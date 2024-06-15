package org.cyclops.integratedscripting.core.network;

import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integratedscripting.Capabilities;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;

import javax.annotation.Nullable;
import java.util.Optional;

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
    public static Optional<IScriptingNetwork> getScriptingNetwork(@Nullable INetwork network) {
        if (network == null) {
            return Optional.empty();
        }
        return network.getCapability(Capabilities.ScriptingNetwork.NETWORK);
    }

}
