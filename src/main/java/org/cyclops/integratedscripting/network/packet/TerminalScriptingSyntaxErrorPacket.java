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
import org.cyclops.integratedscripting.api.language.ScriptSyntaxError;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Packet for sending the syntax error of a script from server to client.
 * @author rubensworks
 *
 */
public class TerminalScriptingSyntaxErrorPacket extends PacketCodec<TerminalScriptingSyntaxErrorPacket> {

    public static final Type<TerminalScriptingSyntaxErrorPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "terminal_scripting_syntax_error"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalScriptingSyntaxErrorPacket> CODEC = getCodec(TerminalScriptingSyntaxErrorPacket::new);

    @CodecField
    private int disk;
    @CodecField
    private String path;
    @CodecField
    private String message;
    @CodecField
    private int line;
    @CodecField
    private int column;

    public TerminalScriptingSyntaxErrorPacket() {
        super(ID);
    }

    public TerminalScriptingSyntaxErrorPacket(int disk, Path path, @Nullable ScriptSyntaxError error) {
        super(ID);
        this.disk = disk;
        this.path = path.toString();
        this.message = error == null ? "" : error.message();
        this.line = error == null ? 0 : error.line();
        this.column = error == null ? 0 : error.column();
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void actionClient(Level world, Player player) {
        if(player.containerMenu instanceof ContainerTerminalScripting container) {
            container.setSyntaxError(disk, Path.of(path), message.isEmpty() ? null : new ScriptSyntaxError(message, line, column));
        }
    }

    @Override
    public void actionServer(Level world, ServerPlayer player) {

    }
}
