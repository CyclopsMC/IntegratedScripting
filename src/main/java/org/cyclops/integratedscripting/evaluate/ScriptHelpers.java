package org.cyclops.integratedscripting.evaluate;

import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integratedscripting.GeneralConfig;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Value;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * @author rubensworks
 */
public class ScriptHelpers {

    private static final Engine ENGINE = Engine
            .newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    public static Context createBaseContext(@Nullable Function<Context.Builder, Context.Builder> contextBuilderModifier) {
        Context.Builder contextBuilder = Context
                .newBuilder()
                .engine(ENGINE)
//                .allowAllAccess(true)
                .allowCreateProcess(GeneralConfig.graalAllowCreateProcess)
                .allowCreateThread(GeneralConfig.graalAllowCreateThread)
                .allowIO(GeneralConfig.graalAllowIo)
                .allowHostClassLoading(GeneralConfig.graalAllowHostClassLoading)
                .allowExperimentalOptions(GeneralConfig.graalAllowExperimentalOptions)
                .allowEnvironmentAccess(GeneralConfig.graalAllowEnvironment ? EnvironmentAccess.INHERIT : EnvironmentAccess.NONE)
                .allowNativeAccess(GeneralConfig.graalAllowNative)
                .allowHostAccess(HostAccess.ALL)
                .allowInnerContextOptions(false);
        if (GeneralConfig.graalStatementLimit > 0) {
            contextBuilder = contextBuilder.resourceLimits(ResourceLimits.newBuilder()
                    .statementLimit(GeneralConfig.graalStatementLimit, null)
                    .build());
        }
        if (contextBuilderModifier != null) {
            contextBuilder = contextBuilderModifier.apply(contextBuilder);
        }
        return contextBuilder.build();
    }

    public static Context createPopulatedContext(@Nullable Function<Context.Builder, Context.Builder> contextBuilderModifier, ValueDeseralizationContext valueDeseralizationContext) throws EvaluationException {
        Context context = createBaseContext(contextBuilderModifier);

        // Create idContext field with ops
        Value jsBindings = context.getBindings("js");
        Value jsObjectClass = jsBindings.getMember("Object");
        Value idContext = jsObjectClass.newInstance();
        Value ops = jsObjectClass.newInstance();
        for (Map.Entry<String, IOperator> entry : Operators.REGISTRY.getGlobalInteractOperators().entrySet()) {
            ops.putMember(entry.getKey(), ValueTranslators.REGISTRY.translateToGraal(context, ValueTypeOperator.ValueOperator.of(entry.getValue()), getDummyEvaluationExceptionFactory(), valueDeseralizationContext));
        }
        idContext.putMember("ops", ops);
        jsBindings.putMember("idContext", idContext);

        return context;
    }

    @Nullable
    public static String getPathExtension(Path path) {
        String filePathString = path.toString();
        int dotPos = filePathString.lastIndexOf('.');
        if (dotPos >= 0 && dotPos + 1 < filePathString.length()) {
            return filePathString.substring(dotPos + 1);
        }
        return null;
    }

    public static IEvaluationExceptionFactory getDummyEvaluationExceptionFactory() {
        return EvaluationException::new;
    }

    public static IEvaluationExceptionFactory getEvaluationExceptionFactory(int disk, Path path, String member) {
        EvaluationExceptionResolutionHelpers.expungeStaleEvaluationExceptions();

        return message -> EvaluationExceptionResolutionHelpers.resolveOnScriptChange(
                new EvaluationException(Component.translatable("script.integratedscripting.error.script_exec", member, path.toString(), disk, message)),
                disk, path);
    }

}
