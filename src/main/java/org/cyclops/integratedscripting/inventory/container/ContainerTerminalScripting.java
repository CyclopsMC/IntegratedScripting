package org.cyclops.integratedscripting.inventory.container;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
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
import org.cyclops.integratedscripting.RegistryEntries;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;
import org.cyclops.integratedscripting.part.PartTypeTerminalScripting;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

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

    private IntList availableDisks;
    private int activeDisk;

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
                    Map<Path, String> scripts = ScriptingNetworkHelpers.getScriptingData().getScripts(disk);
                    // TODO
                }
            });
        }
    }

    @Override
    protected int getSizeInventory() {
        return 0;
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
