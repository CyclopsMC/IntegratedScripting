package org.cyclops.integratedscripting.network.packet;

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
import java.util.Arrays;
import java.util.List;

/**
 * Packet for sending modified scripts between server and client.
 * @author rubensworks
 *
 */
public class TerminalScriptingModifiedScriptPacket extends PacketCodec {

    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "terminal_scripting_modified_script");

    @CodecField
    private int disk;
    @CodecField
    private String path;
    @CodecField
    private List<String> script;

    public TerminalScriptingModifiedScriptPacket() {
        super(ID);
    }

    public TerminalScriptingModifiedScriptPacket(int disk, Path path, String script) {
        super(ID);
        this.disk = disk;
        this.path = path.toString();
        this.script = Arrays.stream(script.split("\n")).toList();
    }

    public String getScript() {
        return String.join("\n", script);
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void actionClient(Level world, Player player) {
        if(player.containerMenu instanceof ContainerTerminalScripting container) {
            container.setLastScript(disk, Path.of(path), getScript());
        }
    }

    @Override
    public void actionServer(Level world, ServerPlayer player) {
        if(player.containerMenu instanceof ContainerTerminalScripting container) {
            container.setServerScript(disk, Path.of(path), getScript());
        }
    }
}
