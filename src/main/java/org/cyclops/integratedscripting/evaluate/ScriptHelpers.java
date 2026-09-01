package org.cyclops.integratedscripting.evaluate;

import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeOperator;
import org.cyclops.integratedscripting.GeneralConfig;
import org.cyclops.integratedscripting.core.packageddependencies.UnsafeHelper;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * @author rubensworks
 */
public class ScriptHelpers {

    private static final Engine ENGINE;
    static {
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(UnsafeHelper.makeFallbackClassloader());
        try {
            ENGINE = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        } finally {
            Thread.currentThread().setContextClassLoader(c);
        }
    }

    /**
     * A factory for the {@code idContext} object, with {@code ops} defined as a self-replacing lazy getter.
     * This way, the global operators are only translated once a script actually accesses them,
     * while accesses after the first one are plain property reads.
     */
    private static final Source SOURCE_ID_CONTEXT = Source.newBuilder("js", """
            (function(resolveOps) {
                var idContext = {};
                Object.defineProperty(idContext, 'ops', {
                    configurable: true,
                    enumerable: true,
                    get: function() {
                        var ops = resolveOps();
                        Object.defineProperty(idContext, 'ops', {
                            value: ops,
                            configurable: true,
                            enumerable: true,
                            writable: true,
                        });
                        return ops;
                    },
                });
                return idContext;
            })
            """, "integratedscripting_idcontext.js").buildLiteral();

    public static void load() {
        // Do nothing
    }

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
                .allowHostAccess(HostAccess.newBuilder()
                        .allowPublicAccess(GeneralConfig.graalAllowHostPublicAccess)
                        .allowAllImplementations(GeneralConfig.graalAllowHostAllImplementations)
                        .allowAllClassImplementations(GeneralConfig.graalAllowHostAllClassImplementations)
                        .allowArrayAccess(GeneralConfig.graalAllowHostArrayAccess)
                        .allowListAccess(GeneralConfig.graalAllowHostListAccess)
                        .allowBufferAccess(GeneralConfig.graalAllowHostBufferAccess)
                        .allowIterableAccess(GeneralConfig.graalAllowHostIterableAccess)
                        .allowIteratorAccess(GeneralConfig.graalAllowHostIteratorAccess)
                        .allowMapAccess(GeneralConfig.graalAllowHostMapAccess)
                        .allowAccessInheritance(GeneralConfig.graalAllowHostAccessInheritance)
                        .build())
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

        // Create idContext field with ops.
        // The ops object is populated lazily, because translating all global operators is expensive,
        // while many scripts never touch them.
        Value jsBindings = context.getBindings("js");
        Value idContext = context.eval(SOURCE_ID_CONTEXT).execute((ProxyExecutable) args -> {
            Value ops = jsBindings.getMember("Object").newInstance();
            try {
                for (Map.Entry<String, IOperator> entry : Operators.REGISTRY.getGlobalInteractOperators().entrySet()) {
                    ops.putMember(entry.getKey(), ValueTranslators.REGISTRY.translateToGraal(context,
                            ValueTypeOperator.ValueOperator.of(entry.getValue()),
                            getDummyEvaluationExceptionFactory(), valueDeseralizationContext));
                }
            } catch (EvaluationException e) {
                throw new RuntimeException(e);
            }
            return ops;
        });
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

    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

}
