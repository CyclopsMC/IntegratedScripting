package org.cyclops.integratedscripting;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.cyclops.integrateddynamics.blockentity.BlockEntityVariablestore;
import org.cyclops.integratedscripting.inventory.container.ContainerScriptingDrive;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;

/**
 * Referenced registry entries.
 * @author rubensworks
 */
public class RegistryEntries {
    public static final DeferredHolder<Item, Item> ITEM_SCRIPTING_DISK = DeferredHolder.create(Registries.ITEM, new ResourceLocation("integratedscripting:scripting_disk"));

    public static final DeferredHolder<Block, Block> BLOCK_SCRIPTING_DRIVE = DeferredHolder.create(Registries.BLOCK, new ResourceLocation("integratedscripting:scripting_drive"));
    public static final DeferredHolder<Block, Block> BLOCK_PART_TERMINAL_SCRIPTING = DeferredHolder.create(Registries.BLOCK, new ResourceLocation("integratedscripting:part_terminal_scripting"));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityVariablestore>> BLOCK_ENTITY_SCRIPTING_DRIVE = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, new ResourceLocation("integratedscripting:scripting_drive"));

    public static final DeferredHolder<MenuType<?>, MenuType<ContainerScriptingDrive>> CONTAINER_SCRIPTING_DRIVE = DeferredHolder.create(Registries.MENU, new ResourceLocation("integratedscripting:scripting_drive"));
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerTerminalScripting>> CONTAINER_TERMINAL_SCRIPTING = DeferredHolder.create(Registries.MENU, new ResourceLocation("integratedscripting:part_terminal_scripting"));
}
