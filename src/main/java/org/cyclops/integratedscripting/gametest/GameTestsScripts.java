package org.cyclops.integratedscripting.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
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

}
