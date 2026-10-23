package org.cyclops.integratedscripting.gametest;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.gametest.GameTest;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeString;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.part.PartTypePanelDisplay;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.core.evaluate.ScriptEvaluationStatuses;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.*;
import static org.cyclops.integratedscripting.gametest.GameTestHelpersIntegratedScripting.createBasicNetwork;
import static org.cyclops.integratedscripting.gametest.GameTestHelpersIntegratedScripting.createVariableForScript;

public class GameTestsScripts {

    public static final String TEMPLATE_EMPTY = Reference.MOD_ID + ":empty10";
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
            assertValueEqual(helper, partAndState.getRight().getDisplayValue(), ValueTypeInteger.ValueInteger.of(3));
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
            assertValueEqual(helper, partAndState.getRight().getDisplayValue(), ValueTypeInteger.ValueInteger.of(11));
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
            assertValueEqual(helper, partAndState.getRight().getDisplayValue(), ValueTypeBoolean.ValueBoolean.of(true));
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testScriptsDisplayScriptRegex(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Write script
        ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), Path.of("script0.js"), "function abc(a) { return /^aether:.+_gloves/.test(a); }", IScriptingData.ChangeLocation.MEMORY);

        // Create variable from script
        ItemStack variableScript = createVariableForScript(helper.getLevel(), positions.diskId(), Path.of("script0.js"), "abc");

        // Create constants as input to the script's function
        ItemStack variableConst1 = createVariableForValue(helper.getLevel(), ValueTypes.STRING, ValueTypeString.ValueString.of("aether:leather_gloves"));

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
            assertValueEqual(helper, partAndState.getRight().getDisplayValue(), ValueTypeBoolean.ValueBoolean.of(true));
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
            try {
                helper.assertValueEqual(partAndState.getRight().getGlobalErrors(), Lists.newArrayList(
                        Component.translatable("script.integratedscripting.error.member_not_in_network", positions.diskId(), "abc", "script0.js"),
                        Component.translatable("script.integratedscripting.error.member_not_in_network", positions.diskId(), "abc", "script0.js")
                ), "Display panel errors do not match");
            } catch (GameTestAssertException e) {
                helper.assertValueEqual(partAndState.getRight().getGlobalErrors(), Lists.newArrayList(
                        Component.translatable("script.integratedscripting.error.script_missing", "script0.js", positions.diskId()),
                        Component.translatable("script.integratedscripting.error.script_missing", "script0.js", positions.diskId())
                ), "Display panel errors do not match");
            }
        });
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testScriptsEvaluationStatus(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);
        Path path = Path.of("script0.js");
        ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), path, "const abc = 3", IScriptingData.ChangeLocation.MEMORY);
        helper.assertTrue(getEvaluationStatusKey(positions.diskId(), path, "abc").equals("gui.integratedscripting.evaluation.none"), "Script must not be evaluated yet");

        ItemStack variableScript = createVariableForScript(helper.getLevel(), positions.diskId(), path, "abc");
        placeVariableInDisplayPanel(helper.getLevel(), positions.displayPanel(), variableScript);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(getEvaluationStatusKey(positions.diskId(), path, "abc").equals("gui.integratedscripting.evaluation.value"), "Value must be recorded");
                    helper.assertTrue(ScriptEvaluationStatuses.describe(positions.diskId(), path, "abc").get(0).getString().contains("3"), "Value must be 3");
                })
                .thenExecute(() -> ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), path, "const abc = doesNotExist();", IScriptingData.ChangeLocation.MEMORY))
                .thenWaitUntil(() -> helper.assertTrue(getEvaluationStatusKey(positions.diskId(), path, "abc").equals("gui.integratedscripting.evaluation.error"), "Error must be recorded"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testScriptsEvaluationStatusCallError(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);
        Path path = Path.of("script0.js");
        ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), path, "function abc(a, b) { throw new Error('boom ' + (a + b)); }", IScriptingData.ChangeLocation.MEMORY);

        // Apply the function in a display panel
        ItemStack variableScript = createVariableForScript(helper.getLevel(), positions.diskId(), path, "abc");
        ItemStack variableConst1 = createVariableForValue(helper.getLevel(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(3));
        ItemStack variableConst2 = createVariableForValue(helper.getLevel(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(8));
        positions.variableStore().getInventory().setItem(0, variableScript);
        positions.variableStore().getInventory().setItem(1, variableConst1);
        positions.variableStore().getInventory().setItem(2, variableConst2);
        ItemStack variableApplied = createVariableForOperator(helper.getLevel(), Operators.OPERATOR_APPLY_2, new int[]{
                getVariableFacade(helper.getLevel(), variableScript).getId(),
                getVariableFacade(helper.getLevel(), variableConst1).getId(),
                getVariableFacade(helper.getLevel(), variableConst2).getId()
        });
        placeVariableInDisplayPanel(helper.getLevel(), positions.displayPanel(), variableApplied);

        helper.succeedWhen(() -> {
            List<MutableComponent> lines = ScriptEvaluationStatuses.describe(positions.diskId(), path, "abc");
            helper.assertTrue(lines.size() == 2, "The value and call error must be described");
            helper.assertTrue(((TranslatableContents) lines.get(1).getContents()).getKey().equals("gui.integratedscripting.evaluation.call_error"), "Call error must be recorded");
            helper.assertTrue(lines.get(1).getString().contains("boom 11"), "Call error must contain the thrown message");
        });
    }

    private static String getEvaluationStatusKey(int disk, Path path, String member) {
        return ((TranslatableContents) ScriptEvaluationStatuses.describe(disk, path, member).get(0).getContents()).getKey();
    }

    @GameTest(template = TEMPLATE_EMPTY, timeoutTicks = TIMEOUT)
    public void testScriptsDisplayScriptErrorResolvedAfterExternalFix(GameTestHelper helper) {
        GameTestHelpersIntegratedScripting.NetworkPositions positions = createBasicNetwork(helper, POS);

        // Write a script that fails when evaluated
        ScriptingNetworkHelpers.getScriptingData().setScript(positions.diskId(), Path.of("script0.js"), "const abc = doesNotExist();", IScriptingData.ChangeLocation.MEMORY);

        // Create variable from script and place it in a display
        ItemStack variableScript = createVariableForScript(helper.getLevel(), positions.diskId(), Path.of("script0.js"), "abc");
        Pair<PartTypePanelDisplay, PartTypePanelDisplay.State> partAndState = placeVariableInDisplayPanel(helper.getLevel(), positions.displayPanel(), variableScript);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertFalse(partAndState.getRight().getGlobalErrors().isEmpty(), "Display panel must show the script error"))
                // Let the network settle, so that only the script change can make the display update
                .thenIdle(40)
                // Fix the script externally, so the file watcher picks it up
                .thenExecute(() -> {
                    Path scriptFile = IntegratedScripting._instance.scriptingData.getRootPath()
                            .resolve("scripting-disks").resolve(String.valueOf(positions.diskId())).resolve("script0.js");
                    try {
                        Files.writeString(scriptFile, "const abc = 3;");
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .thenWaitUntil(() -> assertValueEqual(helper, partAndState.getRight().getDisplayValue(), ValueTypeInteger.ValueInteger.of(3)))
                .thenSucceed();
    }

}
