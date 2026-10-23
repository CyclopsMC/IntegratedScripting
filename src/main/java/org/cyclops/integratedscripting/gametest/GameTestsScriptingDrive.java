package org.cyclops.integratedscripting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.blockentity.BlockEntityScriptingDrive;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;

import java.util.Set;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsScriptingDrive {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    @GameTest(template = TEMPLATE_EMPTY)
    public void testScriptingDriveExposedDisks(GameTestHelper helper) {
        // An empty drive in a network
        helper.setBlock(POS, RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(POS.south(), org.cyclops.integratedscripting.RegistryEntries.BLOCK_SCRIPTING_DRIVE.value());
        BlockEntityScriptingDrive drive = helper.getBlockEntity(POS.south());
        assertDisks(helper, drive, Set.of());

        // Insert a disk
        ItemStack disk1 = new ItemStack(org.cyclops.integratedscripting.RegistryEntries.ITEM_SCRIPTING_DISK);
        int id1 = org.cyclops.integratedscripting.RegistryEntries.ITEM_SCRIPTING_DISK.get().getOrCreateDiskId(disk1);
        drive.getInventory().setItem(0, disk1);
        assertDisks(helper, drive, Set.of(id1));

        // Swap it for another disk
        ItemStack disk2 = new ItemStack(org.cyclops.integratedscripting.RegistryEntries.ITEM_SCRIPTING_DISK);
        int id2 = org.cyclops.integratedscripting.RegistryEntries.ITEM_SCRIPTING_DISK.get().getOrCreateDiskId(disk2);
        drive.getInventory().setItem(0, disk2);
        assertDisks(helper, drive, Set.of(id2));

        // Remove it
        drive.getInventory().setItem(0, ItemStack.EMPTY);
        assertDisks(helper, drive, Set.of());

        helper.succeed();
    }

    private static void assertDisks(GameTestHelper helper, BlockEntityScriptingDrive drive, Set<Integer> expected) {
        Set<Integer> disks = ScriptingNetworkHelpers.getScriptingNetwork(drive.getNetwork()).orElseThrow().getDisks();
        helper.assertTrue(disks.equals(expected), "Expected disks " + expected + " but got " + disks);
    }

}
