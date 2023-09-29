package org.cyclops.integratedscripting.inventory.container;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerMultipart;
import org.cyclops.integrateddynamics.core.part.PartStateEmpty;
import org.cyclops.integratedscripting.RegistryEntries;
import org.cyclops.integratedscripting.part.PartTypeTerminalScripting;

import java.util.Optional;

/**
 * Container for the crafting jobs overview gui.
 * @author rubensworks
 */
public class ContainerTerminalScripting extends ContainerMultipart<PartTypeTerminalScripting, PartStateEmpty<PartTypeTerminalScripting>> {

    private final LazyOptional<INetwork> network;

    public ContainerTerminalScripting(int id, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
        this(id, playerInventory, PartHelpers.readPartTarget(packetBuffer), Optional.empty(), PartHelpers.readPart(packetBuffer));
    }

    public ContainerTerminalScripting(int id, Inventory playerInventory,
                                      PartTarget target, Optional<IPartContainer> partContainer,
                                      PartTypeTerminalScripting partType) {
        super(RegistryEntries.CONTAINER_TERMINAL_SCRIPTING, id, playerInventory, new SimpleContainer(), Optional.of(target), partContainer, partType);

        this.network = getTarget()
                .map(t -> NetworkHelpers.getNetwork(t.getCenter()))
                .orElse(LazyOptional.empty());
    }

    public LazyOptional<INetwork> getNetwork() {
        return network;
    }

    public int getChannel() {
        return IPositionedAddonsNetwork.WILDCARD_CHANNEL;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // TODO: Send initial script data to clients
    }

    @Override
    protected int getSizeInventory() {
        return 0;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return true;
    }

    @Override
    public void onUpdate(int valueId, CompoundTag value) {
        super.onUpdate(valueId, value);

        // TODO: Read updates from server
    }

}
