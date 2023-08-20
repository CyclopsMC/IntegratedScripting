package org.cyclops.integratedscripting.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.ByteArrayTag;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeNbt;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.IOException;

/**
 * @author rubensworks
 */
public class CommandTestScript implements Command<CommandSourceStack> {

    private static Engine ENGINE = null;

    @Override
    public int run(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        // Create engine only once
        if (ENGINE == null) {
            ENGINE = Engine.newBuilder()
                    .option("engine.WarnInterpreterOnly", "false")
                    .build();
        }

        try {
            Source source = Source.newBuilder("js", "function testFunction(a, b, c, d) { console.log('Args: ' + JSON.stringify(a) + JSON.stringify(b) + JSON.stringify(c) + JSON.stringify(d)); console.log(typeof d); return 10; }", "src.js").build();
            try (Context context = Context.newBuilder().engine(ENGINE).allowAllAccess(true).build()) {
//                long timeStart = System.currentTimeMillis();
//                for (int i = 0; i < 100; i++) {
//                    context.eval(source);
//                    Value primesMain = context.getBindings("js").getMember("testFunction");
//                    primesMain.execute();
//                }
//                System.out.println("Exec time with re-eval (ms): " + (System.currentTimeMillis() - timeStart)); // 1931 ms

//                long timeStart2 = System.currentTimeMillis();
//                context.eval(source);
//                Value primesMain = context.getBindings("js").getMember("testFunction");
//                for (int i = 0; i < 100; i++) {
//                    primesMain.execute();
//                }
//                System.out.println("Exec time with cached eval (ms): " + (System.currentTimeMillis() - timeStart2)); // 10 ms

                context.eval(source);
                Value primesMain = context.getBindings("js").getMember("testFunction");
                Value ret = primesMain.execute(
                        ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeInteger.ValueInteger.of(10)),
                        ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeBoolean.ValueBoolean.of(true)),
                        ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeList.ValueList.ofAll(
                                ValueTypeInteger.ValueInteger.of(10),
                                ValueTypeBoolean.ValueBoolean.of(true)
                        )),
                        ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeNbt.ValueNbt.of(new ByteArrayTag(new byte[]{1, 2, 3})))
                );
                System.out.println(ValueTranslators.REGISTRY.translateFromGraal(context, ret).toString()); // TODO
            }
        } catch (IOException | EvaluationException | RuntimeException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> make() {
        return Commands.literal("testscript")
                .requires((commandSource) -> commandSource.hasPermission(2))
                .executes(new CommandTestScript());
    }

}
