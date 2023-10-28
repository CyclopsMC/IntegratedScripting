package org.cyclops.integratedscripting.network.packet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.cyclops.cyclopscore.network.CodecField;
import org.cyclops.cyclopscore.network.PacketCodec;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;

import java.nio.file.Path;

/**
 * Packet for sending modified scripts between server and client.
 * @author rubensworks
 *
 */
public class TerminalScriptingModifiedScriptPacket extends PacketCodec {

    @CodecField
    private int disk;
    @CodecField
    private String path;
    @CodecField
    private String script;

    public TerminalScriptingModifiedScriptPacket() {

    }

    public TerminalScriptingModifiedScriptPacket(int disk, Path path, String script) {
        this.disk = disk;
        this.path = path.toString();
        this.script = script;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void actionClient(Level world, Player player) {
        if(player.containerMenu instanceof ContainerTerminalScripting container) {
            container.setLastScript(disk, Path.of(path), script);
        }
    }

    @Override
    public void actionServer(Level world, ServerPlayer player) {
        if(player.containerMenu instanceof ContainerTerminalScripting container) {
            container.setServerScript(disk, Path.of(path), script);
        }
    }
}
