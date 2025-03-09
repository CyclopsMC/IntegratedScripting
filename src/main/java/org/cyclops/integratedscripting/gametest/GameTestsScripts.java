package org.cyclops.integratedscripting.gametest;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.part.PartTypePanelDisplay;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;

import java.nio.file.Path;

import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.*;
import static org.cyclops.integratedscripting.gametest.GameTestHelpersIntegratedScripting.createBasicNetwork;
import static org.cyclops.integratedscripting.gametest.GameTestHelpersIntegratedScripting.createVariableForScript;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsScripts {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final int TIMEOUT = 2000;
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testScriptsDisplayScriptConst(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Write script
        ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), Path.of("script0.js"), "const abc = 3", IScriptingData.ChangeLocation.MEMORY);

        // Create variable from script
        ItemStack variableScript = createVariableForScript(helper.getLevel(), positions.diskId(), Path.of("script0.js"), "abc");

        // Place variable in display
        Pair<PartTypePanelDisplay, PartTypePanelDisplay.State> partAndState = placeVariableInDisplayPanel(helper.getLevel(), positions.displayPanel(), variableScript);

        helper.succeedWhen(() -> {
            assertValueEqual(partAndState.getRight().getDisplayValue(), ValueTypeInteger.ValueInteger.of(3));
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testScriptsDisplayScriptApplied(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Write script
        ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), Path.of("script0.js"), "function abc(a, b) { return a + b; }", IScriptingData.ChangeLocation.MEMORY);

        // Create variable from script
        ItemStack variableScript = createVariableForScript(helper.getLevel(), positions.diskId(), Path.of("script0.js"), "abc");

        // Create constants as input to the script's function
        ItemStack variableConst1 = createVariableForValue(helper.getLevel(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(3));
        ItemStack variableConst2 = createVariableForValue(helper.getLevel(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(8));

        // Insert all variables into the variable store
        positions.variableStore().getInventory().setItem(0, variableScript);
        positions.variableStore().getInventory().setItem(1, variableConst1);
        positions.variableStore().getInventory().setItem(2, variableConst2);

        // Create variable card for applying the function
        ItemStack variableAdded = createVariableForOperator(helper.getLevel(), Operators.OPERATOR_APPLY_2, new int[]{
                getVariableFacade(helper.getLevel(), variableScript).getId(),
                getVariableFacade(helper.getLevel(), variableConst1).getId(),
                getVariableFacade(helper.getLevel(), variableConst2).getId()
        });

        // Place variable in display
        Pair<PartTypePanelDisplay, PartTypePanelDisplay.State> partAndState = placeVariableInDisplayPanel(helper.getLevel(), positions.displayPanel(), variableAdded);

        helper.succeedWhen(() -> {
            assertValueEqual(partAndState.getRight().getDisplayValue(), ValueTypeInteger.ValueInteger.of(11));
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testScriptsDisplayScriptOnItem(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Write script
        ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), Path.of("script0.js"), "function abc(item) { return item.isStackable() && item.size() >= 16; }", IScriptingData.ChangeLocation.MEMORY);

        // Create variable from script
        ItemStack variableScript = createVariableForScript(helper.getLevel(), positions.diskId(), Path.of("script0.js"), "abc");

        // Create constants as input to the script's function
        ItemStack variableConst1 = createVariableForValue(helper.getLevel(), ValueTypes.OBJECT_ITEMSTACK, ValueObjectTypeItemStack.ValueItemStack.of(new ItemStack(Items.APPLE, 32)));

        // Insert all variables into the variable store
        positions.variableStore().getInventory().setItem(0, variableScript);
        positions.variableStore().getInventory().setItem(1, variableConst1);

        // Create variable card for applying the function
        ItemStack variableApplied = createVariableForOperator(helper.getLevel(), Operators.OPERATOR_APPLY, new int[]{
                getVariableFacade(helper.getLevel(), variableScript).getId(),
                getVariableFacade(helper.getLevel(), variableConst1).getId(),
        });

        // Place variable in display
        Pair<PartTypePanelDisplay, PartTypePanelDisplay.State> partAndState = placeVariableInDisplayPanel(helper.getLevel(), positions.displayPanel(), variableApplied);

        helper.succeedWhen(() -> {
            assertValueEqual(partAndState.getRight().getDisplayValue(), ValueTypeBoolean.ValueBoolean.of(true));
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testScriptsDisplayScriptInfiniteLoop(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Write script
        ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), Path.of("script0.js"), "function oops() {\n" +
                "    while (true) {}\n" +
                "}", IScriptingData.ChangeLocation.MEMORY);

        // Create variable from script
        ItemStack variableScript = createVariableForScript(helper.getLevel(), positions.diskId(), Path.of("script0.js"), "abc");

        // Insert all variables into the variable store
        positions.variableStore().getInventory().setItem(0, variableScript);

        // Create variable card for applying the function
        ItemStack variableAdded = createVariableForOperator(helper.getLevel(), Operators.OPERATOR_APPLY_0, new int[]{
                getVariableFacade(helper.getLevel(), variableScript).getId()
        });

        // Place variable in display
        Pair<PartTypePanelDisplay, PartTypePanelDisplay.State> partAndState = placeVariableInDisplayPanel(helper.getLevel(), positions.displayPanel(), variableAdded);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(partAndState.getRight().getGlobalErrors(), Lists.newArrayList(
                    Component.translatable("script.integratedscripting.error.member_not_in_network", positions.diskId(), "abc", "script0.js"),
                    Component.translatable("script.integratedscripting.error.member_not_in_network", positions.diskId(), "abc", "script0.js")
            ), "Display panel errors do not match");
        });
    }

}
