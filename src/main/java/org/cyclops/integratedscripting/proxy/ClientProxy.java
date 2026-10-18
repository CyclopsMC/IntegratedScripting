package org.cyclops.integratedscripting.proxy;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.cyclops.cyclopscore.client.key.IKeyRegistry;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.cyclopscore.proxy.ClientProxyComponent;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.client.key.Keys;

/**
 * Proxy for the client side.
 *
 * @author rubensworks
 *
 */
public class ClientProxy extends ClientProxyComponent {

    public ClientProxy() {
        super(new CommonProxy());
    }

    @Override
    public ModBase getMod() {
        return IntegratedScripting._instance;
    }

    @Override
    public void registerKeyBindings(IKeyRegistry keyRegistry, RegisterKeyMappingsEvent event) {
        super.registerKeyBindings(keyRegistry, event);
        Keys.ALL.forEach(event::register);
    }

}
