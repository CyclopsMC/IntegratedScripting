package org.cyclops.integratedscripting.proxy;

import org.cyclops.cyclopscore.init.ModBaseNeoForge;
import org.cyclops.cyclopscore.network.IPacketHandler;
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
    public ModBaseNeoForge<IntegratedScripting> getMod() {
        return IntegratedScripting._instance;
    }

    @Override
    public void registerPackets(IPacketHandler packetHandler) {
        super.registerPackets(packetHandler);

        packetHandler.register(TerminalScriptingModifiedScriptPacket.class, TerminalScriptingModifiedScriptPacket.ID, TerminalScriptingModifiedScriptPacket.CODEC);
        packetHandler.register(TerminalScriptingCreateNewScriptPacket.class, TerminalScriptingCreateNewScriptPacket.ID, TerminalScriptingCreateNewScriptPacket.CODEC);
        packetHandler.register(TerminalScriptingDeleteScriptPacket.class, TerminalScriptingDeleteScriptPacket.ID, TerminalScriptingDeleteScriptPacket.CODEC);
    }
}
