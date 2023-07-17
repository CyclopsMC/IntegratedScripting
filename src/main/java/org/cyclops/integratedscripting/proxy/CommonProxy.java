package org.cyclops.integratedscripting.proxy;

import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.cyclopscore.proxy.CommonProxyComponent;
import org.cyclops.integratedscripting.IntegratedRest;

/**
 * Proxy for server and client side.
 * @author rubensworks
 *
 */
public class CommonProxy extends CommonProxyComponent {

    @Override
    public ModBase getMod() {
        return IntegratedRest._instance;
    }

}
