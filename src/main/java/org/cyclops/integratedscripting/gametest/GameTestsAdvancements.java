package org.cyclops.integratedscripting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.logicprogrammer.event.LogicProgrammerVariableFacadeCreatedEvent;
import org.cyclops.integrateddynamics.core.part.PartTypes;
import org.cyclops.integrateddynamics.part.PartTypePanelDisplay;
import org.cyclops.integrateddynamics.part.aspect.Aspects;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.RegistryEntries;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.core.item.ScriptVariableFacade;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;

import java.nio.file.Path;

import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.*;
import static org.cyclops.integratedscripting.gametest.GameTestHelpersIntegratedScripting.createBasicNetwork;
import static org.cyclops.integratedscripting.gametest.GameTestHelpersIntegratedScripting.createVariableForScript;

/**
 * Game tests for all advancements in IntegratedScripting.
 * @author rubensworks
 */
@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsAdvancements {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    private static void assertAdvancementDone(GameTestHelper helper, ServerPlayer player, String path) {
        var advancement = helper.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.parse("integratedscripting:" + path));
        helper.assertTrue(
                advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone(),
                "Advancement 'integratedscripting:" + path + "' was not achieved");
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementRoot(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().add(new ItemStack(org.cyclops.integrateddynamics.RegistryEntries.ITEM_VARIABLE));
        player.containerMenu.broadcastChanges();
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "root"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementMendesite(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("integratedscripting:mendesite"))));
        player.containerMenu.broadcastChanges();
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "basics/mendesite"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementScriptingDisk(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player, new ItemStack(RegistryEntries.ITEM_SCRIPTING_DISK), player.getInventory()));
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "basics/scripting_disk"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementScriptingDrive(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player,
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("integratedscripting:scripting_drive"))),
                player.getInventory()));
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "basics/scripting_drive"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementTerminalOpen(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PartHelpers.openContainerPart(player, positions.terminal(),
                org.cyclops.integratedscripting.part.PartTypes.TERMINAL_SCRIPTING);
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "terminal/open"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementTerminalBind(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ScriptVariableFacade facade = new ScriptVariableFacade(true, positions.diskId(), Path.of("script0.js"), "abc");
        NeoForge.EVENT_BUS.post(new LogicProgrammerVariableFacadeCreatedEvent(
                player, facade, RegistryEntries.BLOCK_PART_TERMINAL_SCRIPTING.value().defaultBlockState()));
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "terminal/bind"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementTerminalDisplay(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Write script
        ScriptingNetworkHelpers.getScriptingData().setScript(
                positions.diskId(), Path.of("script0.js"), "const abc = 3", IScriptingData.ChangeLocation.MEMORY);

        // Create script variable card
        ItemStack variableScript = createVariableForScript(
                helper.getLevel(), positions.diskId(), Path.of("script0.js"), "abc");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Open display panel GUI for the player (registers container as dirty mark listener on state inventory)
        PartHelpers.PartStateHolder displayHolder = PartHelpers.getPart(positions.displayPanel());
        PartTypePanelDisplay displayPartType = (PartTypePanelDisplay) displayHolder.getPart();
        PartHelpers.openContainerPart(player, positions.displayPanel(), displayPartType);

        // Setting the variable after opening GUI triggers container's onDirty() which fires the event with the player
        ((PartTypePanelDisplay.State) displayHolder.getState()).getInventory().setItem(0, variableScript);

        helper.succeedIf(() -> assertAdvancementDone(helper, player, "terminal/display"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementFilterChest(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Write a filter script function
        ScriptingNetworkHelpers.getScriptingData().setScript(
                positions.diskId(), Path.of("script0.js"),
                "function filterFunc(item) { return true; }",
                IScriptingData.ChangeLocation.MEMORY);

        // Create script variable card for the filter function
        ItemStack variableScript = createVariableForScript(
                helper.getLevel(), positions.diskId(), Path.of("script0.js"), "filterFunc");

        // Place a chest adjacent to the cable at POS (to the east)
        helper.setBlock(POS.east(), Blocks.CHEST);

        // Add an inventory reader part to the cable at POS, facing east toward the chest
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.EAST,
                PartTypes.INVENTORY_READER, new ItemStack(PartTypes.INVENTORY_READER.getItem()));

        // Create aspect variable card for inventory itemstacks from the reader
        ItemStack variableAspect = createVariableFromReader(
                helper.getLevel(),
                PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.EAST),
                Aspects.Read.Inventory.LIST_ITEMSTACKS);

        // Store the script and aspect variables in the variable store so they can be resolved by ID
        positions.variableStore().getInventory().setItem(0, variableScript);
        positions.variableStore().getInventory().setItem(1, variableAspect);

        // Create the filter operator variable: OPERATOR_FILTER(filterFunc, inventoryItemstacks)
        ItemStack variableFilter = createVariableForOperator(
                helper.getLevel(),
                org.cyclops.integrateddynamics.core.evaluate.operator.Operators.OPERATOR_FILTER,
                new int[]{
                        getVariableFacade(helper.getLevel(), variableScript).getId(),
                        getVariableFacade(helper.getLevel(), variableAspect).getId()
                });

        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Open display panel GUI for the player (registers container as dirty mark listener on state inventory)
        PartHelpers.PartStateHolder displayHolder = PartHelpers.getPart(positions.displayPanel());
        PartTypePanelDisplay displayPartType = (PartTypePanelDisplay) displayHolder.getPart();
        PartHelpers.openContainerPart(player, positions.displayPanel(), displayPartType);

        // Setting the filter operator variable after opening GUI triggers container's onDirty() → fires event with player
        ((PartTypePanelDisplay.State) displayHolder.getState()).getInventory().setItem(0, variableFilter);

        helper.succeedIf(() -> assertAdvancementDone(helper, player, "functions/filter_chest"));
    }

}
