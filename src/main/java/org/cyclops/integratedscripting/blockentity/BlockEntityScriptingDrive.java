package org.cyclops.integratedscripting.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.persist.IDirtyMarkListener;
import org.cyclops.integrateddynamics.Capabilities;
import org.cyclops.integrateddynamics.api.network.INetworkElement;
import org.cyclops.integrateddynamics.api.network.INetworkElementProvider;
import org.cyclops.integrateddynamics.capability.networkelementprovider.NetworkElementProviderSingleton;
import org.cyclops.integrateddynamics.core.blockentity.BlockEntityCableConnectableInventory;
import org.cyclops.integratedscripting.RegistryEntries;
import org.cyclops.integratedscripting.core.network.ScriptingDriveNetworkElement;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;
import org.cyclops.integratedscripting.inventory.container.ContainerScriptingDrive;
import org.cyclops.integratedscripting.item.ItemScriptingDisk;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * A part entity used to store variables.
 * Internally, this also acts as an expression cache
 * @author rubensworks
 */
public class BlockEntityScriptingDrive extends BlockEntityCableConnectableInventory
        implements IDirtyMarkListener, MenuProvider {

    public static final int ROWS = 1;
    public static final int COLS = 1;
    public static final int INVENTORY_SIZE = ROWS * COLS;

    private int exposedDiskId = -1;

    public BlockEntityScriptingDrive(BlockPos blockPos, BlockState blockState) {
        super(RegistryEntries.BLOCK_ENTITY_SCRIPTING_DRIVE.get(), blockPos, blockState, BlockEntityScriptingDrive.INVENTORY_SIZE, 1);
        getInventory().addDirtyMarkListener(this);
    }

    public static class CapabilityRegistrar extends BlockEntityCableConnectableInventory.CapabilityRegistrar<BlockEntityScriptingDrive> {
        public CapabilityRegistrar(Supplier<BlockEntityType<? extends BlockEntityScriptingDrive>> blockEntityType) {
            super(blockEntityType);
        }

        @Override
        public void populate() {
            super.populate();

            add(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    (blockEntity, context) -> blockEntity.getInventory().getItemHandler()
            );
            add(
                    Capabilities.NetworkElementProvider.BLOCK,
                    (blockEntity, context) -> blockEntity.getNetworkElementProvider()
            );
        }
    }

    @Override
    public INetworkElementProvider getNetworkElementProvider() {
        return new NetworkElementProviderSingleton() {
            @Override
            public INetworkElement createNetworkElement(Level world, BlockPos blockPos) {
                return new ScriptingDriveNetworkElement(DimPos.of(world, blockPos), () -> getExposedDiskId());
            }
        };
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider provider) {
        super.read(tag, provider);
        this.exposedDiskId = tag.getInt("exposedDiskId");
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("exposedDiskId", this.exposedDiskId);
    }

    public int getExposedDiskId() {
        return exposedDiskId;
    }

    public void setExposedDiskId(int exposedDiskId) {
        int oldExposedDiskId = this.exposedDiskId;
        this.exposedDiskId = exposedDiskId;

        if (oldExposedDiskId != exposedDiskId) {
            this.onDirty();
            ScriptingNetworkHelpers.getScriptingNetwork(getNetwork())
                    .ifPresent(scriptingNetwork -> {
                        if (exposedDiskId == -1) {
                            scriptingNetwork.removeDisk(oldExposedDiskId);
                        } else {
                            scriptingNetwork.addDisk(exposedDiskId);
                        }
                    });
        }
    }

    @Override
    protected SimpleInventory createInventory(int inventorySize, int stackSize) {
        return new SimpleInventory(inventorySize, stackSize) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack itemStack) {
                return super.canPlaceItem(slot, itemStack)
                        && (itemStack.isEmpty() || itemStack.getItem() instanceof ItemScriptingDisk);
            }

            @Override
            protected void onInventoryChanged() {
                super.onInventoryChanged();
                ItemStack itemStack = getItem(0);
                int id = -1;
                if (itemStack.getItem() instanceof ItemScriptingDisk itemScriptingDisk) {
                    id = itemScriptingDisk.getOrCreateDiskId(itemStack);
                }
                setExposedDiskId(id);
            }
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
        return new ContainerScriptingDrive(id, playerInventory, this.getInventory());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.integratedscripting.scripting_drive");
    }
}
