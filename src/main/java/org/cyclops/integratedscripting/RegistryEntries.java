package org.cyclops.integratedscripting;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.ObjectHolder;
import org.cyclops.integrateddynamics.blockentity.BlockEntityVariablestore;
import org.cyclops.integratedscripting.inventory.container.ContainerScriptingDrive;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;

/**
 * Referenced registry entries.
 * @author rubensworks
 */
public class RegistryEntries {
    @ObjectHolder(registryName = "item", value = "integratedscripting:scripting_disk")
    public static final Item ITEM_SCRIPTING_DISK = null;

    @ObjectHolder(registryName = "block", value = "integratedscripting:scripting_drive")
    public static final Block BLOCK_SCRIPTING_DRIVE = null;
    @ObjectHolder(registryName = "block", value = "integratedscripting:part_terminal_scripting")
    public static final Block BLOCK_PART_TERMINAL_SCRIPTING = null;

    @ObjectHolder(registryName = "block_entity_type", value = "integratedscripting:scripting_drive")
    public static final BlockEntityType<BlockEntityVariablestore> BLOCK_ENTITY_SCRIPTING_DRIVE = null;

    @ObjectHolder(registryName = "menu", value = "integratedscripting:scripting_drive")
    public static final MenuType<ContainerScriptingDrive> CONTAINER_SCRIPTING_DRIVE = null;
    @ObjectHolder(registryName = "menu", value = "integratedscripting:part_terminal_scripting")
    public static final MenuType<ContainerTerminalScripting> CONTAINER_TERMINAL_SCRIPTING = null;
}
