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

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Packet for renaming a script from client to server.
 * @author rubensworks
 *
 */
public class TerminalScriptingRenameScriptPacket extends PacketCodec<TerminalScriptingRenameScriptPacket> {

    public static final Type<TerminalScriptingRenameScriptPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "terminal_scripting_rename_script"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalScriptingRenameScriptPacket> CODEC = getCodec(TerminalScriptingRenameScriptPacket::new);

    @CodecField
    private int disk;
    @CodecField
    private String from;
    @CodecField
    private String to;

    public TerminalScriptingRenameScriptPacket() {
        super(ID);
    }

    public TerminalScriptingRenameScriptPacket(int disk, Path from, Path to) {
        super(ID);
        this.disk = disk;
        this.from = from.toString();
        this.to = to.toString();
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
            try {
                container.renameServerScript(disk, Path.of(from), Path.of(to));
            } catch (InvalidPathException e) {
                // Ignore invalid paths
            }
        }
    }
}
