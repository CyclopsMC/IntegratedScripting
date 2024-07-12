package org.cyclops.integratedscripting.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.network.CodecField;
import org.cyclops.cyclopscore.network.PacketCodec;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;


/**
 * Packet for letting a client trigger the creation of a new script by sending to the server.
 * @author rubensworks
 *
 */
public class TerminalScriptingCreateNewScriptPacket extends PacketCodec<TerminalScriptingCreateNewScriptPacket> {

    public static final Type<TerminalScriptingCreateNewScriptPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "terminal_scripting_create_new_script"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalScriptingCreateNewScriptPacket> CODEC = getCodec(TerminalScriptingCreateNewScriptPacket::new);

    @CodecField
    private int disk;

    public TerminalScriptingCreateNewScriptPacket() {
        super(ID);
    }

    public TerminalScriptingCreateNewScriptPacket(int disk) {
        super(ID);
        this.disk = disk;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void actionClient(Level world, Player player) {

    }

    @Override
    public void actionServer(Level world, ServerPlayer player) {
        if(player.containerMenu instanceof ContainerTerminalScripting container) {
            container.createNewServerScript(disk);
        }
    }
}
