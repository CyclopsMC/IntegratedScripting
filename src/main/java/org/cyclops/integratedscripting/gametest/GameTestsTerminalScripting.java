package org.cyclops.integratedscripting.gametest;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;
import org.cyclops.integratedscripting.part.PartTypes;

import java.nio.file.Path;

import static org.cyclops.integratedscripting.gametest.GameTestHelpersIntegratedScripting.createBasicNetwork;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsTerminalScripting {

    public static final String TEMPLATE_EMPTY = "empty10";

    @GameTest(template = TEMPLATE_EMPTY)
    public void testTerminalScriptingKeywordDoesNotRebindCard(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, GameTestsScripts.POS);
        Path path = Path.of("script0.js");
        ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), path, "function abc() {}\nconst def = 1;", IScriptingData.ChangeLocation.MEMORY);

        ContainerTerminalScripting container = new ContainerTerminalScripting(0, helper.makeMockPlayer(GameType.CREATIVE).getInventory(),
                PartTarget.fromCenter(positions.terminal()), PartHelpers.getPartContainer(positions.terminal().getPos(), positions.terminal().getSide()),
                PartTypes.TERMINAL_SCRIPTING, new ContainerTerminalScripting.InitData(IntList.of(positions.diskId()))) {
            // Handle values as if they were sent by the client, as the mock player has no connection
            @Override
            public void setValue(int valueId, CompoundTag value) {
                onUpdate(valueId, value);
            }
        };
        container.setActiveScriptPath(path);

        // Inserting a card binds it to the selected member
        container.setSelection("abc");
        container.getContainerInventory().setItem(0, new ItemStack(RegistryEntries.ITEM_VARIABLE));
        helper.assertValueEqual(container.getCardMember(), "abc", "card member");

        // Selecting a keyword keeps the card as is
        container.setSelection("const");
        helper.assertFalse(container.isMemberSelected(), "Keyword must not be a selected member");
        helper.assertValueEqual(container.getCardMember(), "abc", "card member");

        // Selecting another member rebinds the card
        container.setSelection("def");
        helper.assertValueEqual(container.getCardMember(), "def", "card member");

        helper.succeed();
    }

}
