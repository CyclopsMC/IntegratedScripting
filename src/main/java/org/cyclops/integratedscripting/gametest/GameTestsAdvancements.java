package org.cyclops.integratedscripting.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
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
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

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

    /**
     * An accept-all set used to make a mock player's connection accept all custom payload channels.
     * This is needed because in game test environments, the mock player's connection has not gone through
     * the NeoForge channel negotiation process, which would normally register payload channel IDs.
     * Without this, mods that send custom payloads on PlayerLoggedInEvent would throw exceptions.
     */
    private static final Set<ResourceLocation> ACCEPT_ALL_CHANNELS = new AbstractSet<>() {
        @Override
        public boolean contains(Object o) {
            return true;
        }

        @Override
        public Iterator<ResourceLocation> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return Integer.MAX_VALUE;
        }
    };

    /**
     * Creates a mock server player that can receive all custom payload channels.
     * This avoids "Payload may not be sent to the client!" errors during player login in game tests,
     * which occur because mods may send custom payloads (e.g., to sync data) during PlayerLoggedInEvent,
     * but mock players have not completed NeoForge's channel negotiation handshake.
     */
    private static ServerPlayer makeMockPlayer(GameTestHelper helper) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // Register as an accept-all adhoc channel set so any custom payload can be sent to this mock player
        connection.channel()
                .attr(AttributeKey.<Set<ResourceLocation>>valueOf("neoforge:adhoc_channels"))
                .set(ACCEPT_ALL_CHANNELS);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    private static void assertAdvancementDone(GameTestHelper helper, ServerPlayer player, String path) {
        var advancement = helper.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.parse("integratedscripting:" + path));
        helper.assertTrue(
                advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone(),
                "Advancement 'integratedscripting:" + path + "' was not achieved");
    }

    private static void assertAdvancementNotDone(GameTestHelper helper, ServerPlayer player, String path) {
        var advancement = helper.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.parse("integratedscripting:" + path));
        if (advancement != null) {
            helper.assertTrue(
                    !player.getAdvancements().getOrStartProgress(advancement).isDone(),
                    "Advancement 'integratedscripting:" + path + "' should NOT have been achieved");
        }
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementRoot(GameTestHelper helper) {
        ServerPlayer player = makeMockPlayer(helper);
        player.getInventory().add(new ItemStack(org.cyclops.integrateddynamics.RegistryEntries.ITEM_VARIABLE));
        player.containerMenu.broadcastChanges();
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "root"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementMendesite(GameTestHelper helper) {
        ServerPlayer player = makeMockPlayer(helper);
        player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("integratedscripting:mendesite"))));
        player.containerMenu.broadcastChanges();
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "basics/mendesite"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementScriptingDisk(GameTestHelper helper) {
        ServerPlayer player = makeMockPlayer(helper);
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player, new ItemStack(RegistryEntries.ITEM_SCRIPTING_DISK), player.getInventory()));
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "basics/scripting_disk"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementScriptingDrive(GameTestHelper helper) {
        ServerPlayer player = makeMockPlayer(helper);
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player,
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("integratedscripting:scripting_drive"))),
                player.getInventory()));
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "basics/scripting_drive"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementTerminalOpen(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);
        ServerPlayer player = makeMockPlayer(helper);
        PartHelpers.openContainerPart(player, positions.terminal(),
                org.cyclops.integratedscripting.part.PartTypes.TERMINAL_SCRIPTING);
        helper.succeedIf(() -> assertAdvancementDone(helper, player, "terminal/open"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementTerminalBind(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);
        ServerPlayer player = makeMockPlayer(helper);
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

        ServerPlayer player = makeMockPlayer(helper);

        // Open display panel GUI for the player (registers container as dirty mark listener on state inventory)
        PartHelpers.PartStateHolder displayHolder = PartHelpers.getPart(positions.displayPanel());
        PartTypePanelDisplay displayPartType = (PartTypePanelDisplay) displayHolder.getPart();
        PartHelpers.openContainerPart(player, positions.displayPanel(), displayPartType);

        // Setting the variable after opening GUI triggers container's onDirty() which fires the event with the player
        ((PartTypePanelDisplay.State) displayHolder.getState()).getInventory().setItem(0, variableScript);

        helper.succeedIf(() -> assertAdvancementDone(helper, player, "terminal/display"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementRootNegative(GameTestHelper helper) {
        ServerPlayer player = makeMockPlayer(helper);
        player.getInventory().add(new ItemStack(Blocks.STONE)); // Wrong item - not integrateddynamics:variable
        player.containerMenu.broadcastChanges();
        helper.succeedIf(() -> assertAdvancementNotDone(helper, player, "root"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementMendesiteNegative(GameTestHelper helper) {
        ServerPlayer player = makeMockPlayer(helper);
        player.getInventory().add(new ItemStack(Blocks.STONE)); // Wrong item - not integratedscripting:mendesite
        player.containerMenu.broadcastChanges();
        helper.succeedIf(() -> assertAdvancementNotDone(helper, player, "basics/mendesite"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementScriptingDiskNegative(GameTestHelper helper) {
        ServerPlayer player = makeMockPlayer(helper);
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player, new ItemStack(Blocks.STONE), player.getInventory())); // Wrong item - not scripting_disk
        helper.succeedIf(() -> assertAdvancementNotDone(helper, player, "basics/scripting_disk"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementScriptingDriveNegative(GameTestHelper helper) {
        ServerPlayer player = makeMockPlayer(helper);
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(
                player, new ItemStack(RegistryEntries.ITEM_SCRIPTING_DISK), player.getInventory())); // Wrong item - scripting_disk instead of scripting_drive
        helper.succeedIf(() -> assertAdvancementNotDone(helper, player, "basics/scripting_drive"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementTerminalOpenNegative(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);
        ServerPlayer player = makeMockPlayer(helper);
        PartHelpers.openContainerPart(player, positions.displayPanel(),
                org.cyclops.integrateddynamics.core.part.PartTypes.DISPLAY_PANEL); // Wrong container - not scripting terminal
        helper.succeedIf(() -> assertAdvancementNotDone(helper, player, "terminal/open"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementTerminalBindNegative(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);
        ServerPlayer player = makeMockPlayer(helper);
        ScriptVariableFacade facade = new ScriptVariableFacade(true, positions.diskId(), Path.of("script0.js"), "abc");
        NeoForge.EVENT_BUS.post(new LogicProgrammerVariableFacadeCreatedEvent(
                player, facade, Blocks.STONE.defaultBlockState())); // Wrong block - not integratedscripting:part_terminal_scripting
        helper.succeedIf(() -> assertAdvancementNotDone(helper, player, "terminal/bind"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementTerminalDisplayNegative(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Place a chest adjacent to the cable at POS (to the east)
        helper.setBlock(POS.east(), Blocks.CHEST);

        // Add an inventory reader part to the cable at POS, facing east toward the chest
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.EAST,
                PartTypes.INVENTORY_READER, new ItemStack(PartTypes.INVENTORY_READER.getItem()));

        // Create an aspect variable (NOT a script variable)
        ItemStack variableAspect = createVariableFromReader(
                helper.getLevel(),
                PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.EAST),
                Aspects.Read.Inventory.LIST_ITEMSTACKS);

        ServerPlayer player = makeMockPlayer(helper);

        // Open display panel GUI for the player
        PartHelpers.PartStateHolder displayHolder = PartHelpers.getPart(positions.displayPanel());
        PartTypePanelDisplay displayPartType = (PartTypePanelDisplay) displayHolder.getPart();
        PartHelpers.openContainerPart(player, positions.displayPanel(), displayPartType);

        // Setting an aspect variable (not a script variable) should NOT trigger terminal/display
        ((PartTypePanelDisplay.State) displayHolder.getState()).getInventory().setItem(0, variableAspect);

        helper.succeedIf(() -> assertAdvancementNotDone(helper, player, "terminal/display"));
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

        ServerPlayer player = makeMockPlayer(helper);

        // Open display panel GUI for the player (registers container as dirty mark listener on state inventory)
        PartHelpers.PartStateHolder displayHolder = PartHelpers.getPart(positions.displayPanel());
        PartTypePanelDisplay displayPartType = (PartTypePanelDisplay) displayHolder.getPart();
        PartHelpers.openContainerPart(player, positions.displayPanel(), displayPartType);

        // Setting the filter operator variable after opening GUI triggers container's onDirty() -> fires event with player
        ((PartTypePanelDisplay.State) displayHolder.getState()).getInventory().setItem(0, variableFilter);

        helper.succeedIf(() -> assertAdvancementDone(helper, player, "functions/filter_chest"));
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testAdvancementFilterChestNegative(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Place a chest adjacent to the cable at POS (to the east)
        helper.setBlock(POS.east(), Blocks.CHEST);

        // Add an inventory reader part to the cable at POS, facing east toward the chest
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS), Direction.EAST,
                PartTypes.INVENTORY_READER, new ItemStack(PartTypes.INVENTORY_READER.getItem()));

        // Create two aspect variables - input[0] is NOT a script variable (the negative case)
        ItemStack firstAspectVariable = createVariableFromReader(
                helper.getLevel(),
                PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.EAST),
                Aspects.Read.Inventory.LIST_ITEMSTACKS);
        ItemStack secondAspectVariable = createVariableFromReader(
                helper.getLevel(),
                PartPos.of(helper.getLevel(), helper.absolutePos(POS), Direction.EAST),
                Aspects.Read.Inventory.LIST_ITEMSTACKS);

        // Store the aspect variables so they can be resolved by ID
        positions.variableStore().getInventory().setItem(0, firstAspectVariable);
        positions.variableStore().getInventory().setItem(1, secondAspectVariable);

        // Create the filter operator variable: OPERATOR_FILTER(aspectVar, aspectVar) - input[0] is aspect, NOT script
        ItemStack variableFilter = createVariableForOperator(
                helper.getLevel(),
                org.cyclops.integrateddynamics.core.evaluate.operator.Operators.OPERATOR_FILTER,
                new int[]{
                        getVariableFacade(helper.getLevel(), firstAspectVariable).getId(),
                        getVariableFacade(helper.getLevel(), secondAspectVariable).getId()
                });

        ServerPlayer player = makeMockPlayer(helper);

        // Open display panel GUI for the player
        PartHelpers.PartStateHolder displayHolder = PartHelpers.getPart(positions.displayPanel());
        PartTypePanelDisplay displayPartType = (PartTypePanelDisplay) displayHolder.getPart();
        PartHelpers.openContainerPart(player, positions.displayPanel(), displayPartType);

        // Setting operator_filter with non-script input[0] should NOT trigger functions/filter_chest
        ((PartTypePanelDisplay.State) displayHolder.getState()).getInventory().setItem(0, variableFilter);

        helper.succeedIf(() -> assertAdvancementNotDone(helper, player, "functions/filter_chest"));
    }
}
