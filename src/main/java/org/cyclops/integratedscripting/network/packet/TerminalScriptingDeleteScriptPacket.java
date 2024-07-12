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

import java.nio.file.Path;


/**
 * Packet for sending deleted scripts between server and client.
 * @author rubensworks
 *
 */
public class TerminalScriptingDeleteScriptPacket extends PacketCodec<TerminalScriptingDeleteScriptPacket> {

    public static final Type<TerminalScriptingDeleteScriptPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "terminal_scripting_delete_script"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalScriptingDeleteScriptPacket> CODEC = getCodec(TerminalScriptingDeleteScriptPacket::new);

    @CodecField
    private int disk;
    @CodecField
    private String path;

    public TerminalScriptingDeleteScriptPacket() {
        super(ID);
    }

    public TerminalScriptingDeleteScriptPacket(int disk, Path path) {
        super(ID);
        this.disk = disk;
        this.path = path.toString();
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void actionClient(Level world, Player player) {
        if(player.containerMenu instanceof ContainerTerminalScripting container) {
            container.setLastScript(disk, Path.of(path), null);
        }
    }

    @Override
    public void actionServer(Level world, ServerPlayer player) {
        if(player.containerMenu instanceof ContainerTerminalScripting container) {
            container.setServerScript(disk, Path.of(path), null);
        }
    }
}
