package org.cyclops.integratedscripting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.item.IVariableFacadeHandlerRegistry;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.blockentity.BlockEntityVariablestore;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integratedscripting.api.item.IScriptVariableFacade;
import org.cyclops.integratedscripting.blockentity.BlockEntityScriptingDrive;
import org.cyclops.integratedscripting.core.evaluate.ScriptVariableFacadeHandler;
import org.cyclops.integratedscripting.core.item.ScriptVariableFacade;
import org.cyclops.integratedscripting.part.PartTypes;

import java.nio.file.Path;

/**
 * @author rubensworks
 */
public class GameTestHelpersIntegratedScripting {

    public static NetworkPositions createBasicNetwork(GameTestHelper helper, BlockPos pos) {
        // Place cable
        helper.setBlock(pos, RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(pos.above(), RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(pos.above().east(), RegistryEntries.BLOCK_CABLE.value());

        // Place scripting drive
        helper.setBlock(pos.south(), org.cyclops.integratedscripting.RegistryEntries.BLOCK_SCRIPTING_DRIVE.value());

        // Insert scripting disk into scripting drive
        BlockEntityScriptingDrive scriptingDrive = helper.getBlockEntity(pos.south(), BlockEntityScriptingDrive.class);
        scriptingDrive.getInventory().setItem(0, new ItemStack(org.cyclops.integratedscripting.RegistryEntries.ITEM_SCRIPTING_DISK));

        // Place variable store
        helper.setBlock(pos.south().south(), RegistryEntries.BLOCK_VARIABLE_STORE.value());

        // Place scripting terminal
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(pos.above()), Direction.NORTH, PartTypes.TERMINAL_SCRIPTING, new ItemStack(PartTypes.TERMINAL_SCRIPTING.getItem()));

        // Place display panel
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(pos.above().east()), Direction.NORTH, org.cyclops.integrateddynamics.core.part.PartTypes.DISPLAY_PANEL, new ItemStack(org.cyclops.integrateddynamics.core.part.PartTypes.DISPLAY_PANEL.getItem()));

        BlockEntityVariablestore variableStore = helper.getBlockEntity(pos.south().south(), BlockEntityVariablestore.class);

        return new NetworkPositions(
                PartPos.of(helper.getLevel(), helper.absolutePos(pos.above()), Direction.NORTH),
                PartPos.of(helper.getLevel(), helper.absolutePos(pos.above().east()), Direction.NORTH),
                variableStore,
                org.cyclops.integratedscripting.RegistryEntries.ITEM_SCRIPTING_DISK.get().getOrCreateDiskId(scriptingDrive.getInventory().getItem(0))
        );
    }

    public static ItemStack createVariableForScript(Level level, int disk, Path path, String member) {
        IVariableFacadeHandlerRegistry registry = IntegratedDynamics._instance.getRegistryManager().getRegistry(IVariableFacadeHandlerRegistry.class);
        ItemStack itemStack = new ItemStack(RegistryEntries.ITEM_VARIABLE);
        return registry.writeVariableFacadeItem(true, itemStack, ScriptVariableFacadeHandler.getInstance(),
                new IVariableFacadeHandlerRegistry.IVariableFacadeFactory<IScriptVariableFacade>() {
                    @Override
                    public IScriptVariableFacade create(boolean generateId) {
                        return new ScriptVariableFacade(generateId, disk, path, member);
                    }

                    @Override
                    public IScriptVariableFacade create(int id) {
                        return new ScriptVariableFacade(id, disk, path, member);
                    }
                }, level, null, org.cyclops.integratedscripting.RegistryEntries.BLOCK_PART_TERMINAL_SCRIPTING.get().defaultBlockState());
    }

    public static record NetworkPositions(
            PartPos terminal,
            PartPos displayPanel,
            BlockEntityVariablestore variableStore,
            int diskId
    ){}

}
