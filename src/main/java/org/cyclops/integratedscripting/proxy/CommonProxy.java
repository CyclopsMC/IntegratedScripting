package org.cyclops.integratedscripting.proxy;

import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.cyclopscore.network.PacketHandler;
import org.cyclops.cyclopscore.proxy.CommonProxyComponent;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingCreateNewScriptPacket;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingDeleteScriptPacket;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingModifiedScriptPacket;

/**
 * Proxy for server and client side.
 * @author rubensworks
 *
 */
public class CommonProxy extends CommonProxyComponent {

    @Override
    public ModBase getMod() {
        return IntegratedScripting._instance;
    }

    @Override
    public void registerPacketHandlers(PacketHandler packetHandler) {
        super.registerPacketHandlers(packetHandler);

        packetHandler.register(TerminalScriptingModifiedScriptPacket.class);
        packetHandler.register(TerminalScriptingCreateNewScriptPacket.class);
        packetHandler.register(TerminalScriptingDeleteScriptPacket.class);
    }
}
