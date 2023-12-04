package org.cyclops.integratedscripting.inventory.container;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.cyclops.cyclopscore.inventory.container.InventoryContainer;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.RegistryEntries;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingCreateNewScriptPacket;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingModifiedScriptPacket;
import org.cyclops.integratedscripting.part.PartTypeTerminalScripting;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Container for the crafting jobs overview gui.
 * @author rubensworks
 */
public class ContainerTerminalScripting extends InventoryContainer {

    private final PartTarget target;
    private final Optional<IPartContainer> partContainer;
    private final PartTypeTerminalScripting partType;
    private final Level world;
    private final Optional<INetwork> network;
    private final Optional<IScriptingNetwork> scriptingNetwork;

    private final Int2ObjectMap<Map<Path, String>> lastScripts = new Int2ObjectAVLTreeMap<>();
    private IntList availableDisks;
    private int activeDisk;
    private Path activeScriptPath;

    public ContainerTerminalScripting(int id, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
        this(id, playerInventory, PartHelpers.readPartTarget(packetBuffer), Optional.empty(),
                PartHelpers.readPart(packetBuffer), InitData.readFromPacketBuffer(packetBuffer));
    }

    public ContainerTerminalScripting(int id, Inventory playerInventory,
                                      PartTarget target, Optional<IPartContainer> partContainer,
                                      PartTypeTerminalScripting partType, InitData initData) {
        super(RegistryEntries.CONTAINER_TERMINAL_SCRIPTING, id, playerInventory, new SimpleContainer());
        this.target = target;
        this.partType = partType;
        this.partContainer = partContainer;
        this.world = player.getCommandSenderWorld();

        this.network = NetworkHelpers.getNetwork(getTarget().getCenter()).resolve();
        this.scriptingNetwork = this.network.flatMap(network -> ScriptingNetworkHelpers.getScriptingNetwork(network).resolve());

        this.availableDisks = initData.getAvailableDisks();
        this.activeDisk = this.availableDisks.isEmpty() ? -1 : this.availableDisks.getInt(0);
        this.activeScriptPath = null;
    }

    public Level getLevel() {
        return world;
    }

    public PartTypeTerminalScripting getPartType() {
        return partType;
    }

    public PartTarget getTarget() {
        return target;
    }

    public Optional<PartTypeTerminalScripting> getPartState() {
        return partContainer.map(p -> (PartTypeTerminalScripting) p.getPartState(getTarget().getCenter().getSide()));
    }

    public Optional<IPartContainer> getPartContainer() {
        return partContainer;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return PartHelpers.canInteractWith(getTarget(), player, this.partContainer.get());
    }

    public Optional<INetwork> getNetwork() {
        return network;
    }

    public Optional<IScriptingNetwork> getScriptingNetwork() {
        return scriptingNetwork;
    }

    public IntList getAvailableDisks() {
        return availableDisks;
    }

    public int getActiveDisk() {
        return activeDisk;
    }

    public void setActiveDisk(int activeDisk) {
        this.activeDisk = activeDisk;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // Send disk contents to clients
        if (!this.getLevel().isClientSide()) {
            this.getScriptingNetwork().ifPresent(scriptingNetwork -> {
                for (Integer disk : this.getAvailableDisks()) {
                    Map<Path, String> scriptsNew = ScriptingNetworkHelpers.getScriptingData().getScripts(disk);
                    Map<Path, String> scriptsOld = lastScripts.get((int) disk);

                    // Determine files (union of new and old)
                    Set<Path> files;
                    if (scriptsOld == null) {
                        scriptsOld = Maps.newHashMap();
                        lastScripts.put((int) disk, scriptsOld);
                        files = scriptsNew.keySet();
                    } else {
                        files = Sets.newHashSet(scriptsNew.keySet());
                        files.addAll(scriptsOld.keySet());
                    }

                    // Determine changed files
                    for (Path file : files) {
                        String scriptNew = scriptsNew.get(file);
                        String scriptOld = scriptsOld.get(file);
                        if (!Objects.equals(scriptNew, scriptOld)) {
                            // Send separate packet for each modified file
                            IntegratedScripting._instance.getPacketHandler()
                                    .sendToPlayer(new TerminalScriptingModifiedScriptPacket(disk, file, scriptNew), (ServerPlayer) player);

                            // Update the next old value
                            if (scriptNew != null) {
                                scriptsOld.put(file, scriptNew);
                            } else {
                                scriptsOld.remove(file);
                            }

                            // Cleanup if we have no scripts anymore for a certain disk
                            if (scriptsOld.isEmpty()) {
                                lastScripts.remove((int) disk);
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    protected int getSizeInventory() {
        return 0;
    }

    public void setLastScript(int disk, Path path, @Nullable String script) {
        Map<Path, String> lastScriptsDisk = lastScripts.get(disk);
        if (lastScriptsDisk == null) {
            lastScriptsDisk = Maps.newHashMap();
            lastScripts.put(disk, lastScriptsDisk);
        }

        // Store or delete script
        if (script != null) {
            lastScriptsDisk.put(path, script);
        } else {
            lastScriptsDisk.remove(path);
        }

        // Cleanup if we have no scripts anymore for a certain disk
        if (lastScriptsDisk.isEmpty()) {
            lastScripts.remove(disk);
        }
    }

    public Int2ObjectMap<Map<Path, String>> getLastScripts() {
        return lastScripts;
    }

    public void setServerScript(int disk, Path path, @Nullable String script) {
        ScriptingNetworkHelpers.getScriptingData().setScript(disk, path, script, IScriptingData.ChangeLocation.MEMORY);
    }

    public void createNewServerScript(int disk) {
        // Determine unique script name
        Set<Path> existingScripts = ScriptingNetworkHelpers.getScriptingData().getScripts(disk).keySet();
        Path path;
        int i = 0;
        do {
            path = Path.of("script" + i++ + ".js");
        } while (existingScripts.contains(path));

        // Create empty script
        ScriptingNetworkHelpers.getScriptingData().setScript(disk, path, "", IScriptingData.ChangeLocation.MEMORY);
    }

    @Nullable
    public Path getActiveScriptPath() {
        return activeScriptPath;
    }

    public void setActiveScriptPath(Path activeScriptPath) {
        this.activeScriptPath = activeScriptPath;
    }

    @Nullable
    public String getActiveScript() {
        Path path = getActiveScriptPath();
        int disk = getActiveDisk();
        if (path != null && disk >= 0) {
            Map<Path, String> diskScripts = getLastScripts().get(disk);
            if (diskScripts != null) {
                return diskScripts.get(path);
            }
        }
        return null;
    }

    public void setActiveScript(String scriptNew) {
        Path path = getActiveScriptPath();
        int disk = getActiveDisk();
        if (path != null && disk >= 0) {
            Map<Path, String> diskScripts = getLastScripts().get(disk);
            if (diskScripts != null) {
                diskScripts.put(path, scriptNew);
                IntegratedScripting._instance.getPacketHandler()
                        .sendToServer(new TerminalScriptingModifiedScriptPacket(disk, path, scriptNew));
            }
        }
    }

    public void createNewFile() {
        int disk = getActiveDisk();
        if (disk >= 0) {
            IntegratedScripting._instance.getPacketHandler().sendToServer(new TerminalScriptingCreateNewScriptPacket(disk));
        }
    }

    public static class InitData {

        private final IntList availableDisks;

        public InitData(IntList availableDisks) {
            this.availableDisks = availableDisks;
        }

        public IntList getAvailableDisks() {
            return availableDisks;
        }

        public void writeToPacketBuffer(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeIntIdList(getAvailableDisks());
        }

        public static InitData readFromPacketBuffer(FriendlyByteBuf packetBuffer) {
            return new InitData(packetBuffer.readIntIdList());
        }

    }

}
