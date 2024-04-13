package org.cyclops.integratedscripting.core.network;

import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.api.network.IScript;
import org.cyclops.integratedscripting.api.network.IScriptFactory;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.evaluate.EvaluationExceptionResolutionHelpers;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Instantiates scripts using the GraalVM.
 * @author rubensworks
 */
public class GraalScriptFactory implements IScriptFactory {

    private final String languageId;

    public GraalScriptFactory(String languageId) {
        this.languageId = languageId;
    }

    @Nullable
    @Override
    public IScript getScript(int disk, Path path) throws EvaluationException {
        // Construct stdout and stderr output streams
        Pair<OutputStream, OutputStream> outputStreams = ScriptingNetworkHelpers.getScriptingData().getOutputStreams(disk, path);

        // Construct graal context
        Context graalContext = ScriptHelpers.createPopulatedContext((contextBuilder) -> contextBuilder
                .out(outputStreams.getLeft())
                .err(outputStreams.getRight()));
        Value languageBinding = graalContext.getBindings("js");

        try {
            // Read script
            Source source = Source.newBuilder(this.languageId, ScriptingNetworkHelpers.getScriptingData().getScripts(disk).get(path), path.toString()).build();
            try {
                graalContext.eval(source);
            } catch (PolyglotException e) {
                throw EvaluationExceptionResolutionHelpers.resolveOnScriptChange(
                        new EvaluationException(Component.translatable("script.integratedscripting.error.script_read", path.toString(), disk, e.getMessage())),
                        disk, path);
            }
            Wrapper<IScriptingData.IDiskScriptsChangeListener> diskListener = new Wrapper<>();
            return new GraalScript(graalContext, languageBinding, scriptInvalidateListener -> {
                // Register script invalidate listener
                // Delegate the script invalidation listener to the disk change listener.
                diskListener.set(scriptPathRelative -> {
                    // Since disk change listeners listen to ALL changes in a disk, we filter by the current path.
                    if (scriptPathRelative.equals(path)) {
                        scriptInvalidateListener.invalidate();
                    }
                });
                ScriptingNetworkHelpers.getScriptingData().addListener(disk, diskListener.get());
            }, () -> {
                // Remove all script invalidate listeners
                if (diskListener.get() != null) {
                    ScriptingNetworkHelpers.getScriptingData().removeListener(disk, diskListener.get());
                }
            }, disk, path, null);
        } catch (IOException e) {
            throw EvaluationExceptionResolutionHelpers.resolveOnScriptChange(
                    new EvaluationException(Component.translatable("script.integratedscripting.error.script_read", path.toString(), disk, e.getMessage())),
                    disk, path);
        }
    }
}
