package org.cyclops.integratedscripting.inventory.container;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.cyclops.cyclopscore.inventory.container.InventoryContainer;
import org.cyclops.integrateddynamics.core.inventory.container.slot.SlotVariable;
import org.cyclops.integratedscripting.RegistryEntries;
import org.cyclops.integratedscripting.blockentity.BlockEntityScriptingDrive;

/**
 * Container for the scripting drive.
 * @author rubensworks
 */
public class ContainerScriptingDrive extends InventoryContainer {

    public ContainerScriptingDrive(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(BlockEntityScriptingDrive.INVENTORY_SIZE));
    }

    public ContainerScriptingDrive(int id, Inventory playerInventory, Container inventory) {
        super(RegistryEntries.CONTAINER_SCRIPTING_DRIVE, id, playerInventory, inventory);
        addInventory(inventory, 0, offsetX + 80, offsetY + 16, BlockEntityScriptingDrive.ROWS, BlockEntityScriptingDrive.COLS);
        addPlayerInventory(playerInventory, offsetX + 8, offsetY + 10 + BlockEntityScriptingDrive.ROWS * 18 + 17);
    }

    @Override
    public Slot createNewSlot(Container inventory, int index, int row, int column) {
        if(inventory instanceof Inventory) {
            return super.createNewSlot(inventory, index, row, column);
        }
        return new SlotVariable(inventory, index, row, column);
    }
}
