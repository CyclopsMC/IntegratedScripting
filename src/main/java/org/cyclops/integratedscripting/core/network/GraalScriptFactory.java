package org.cyclops.integratedscripting.core.network;

import net.minecraft.network.chat.Component;
import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.api.network.IScript;
import org.cyclops.integratedscripting.api.network.IScriptFactory;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Instantiates scripts using the GraalVM.
 * @author rubensworks
 */
public class GraalScriptFactory implements IScriptFactory {

    private final Context graalContext;
    private final Value languageBinding;
    private final String languageId;

    public GraalScriptFactory(Context graalContext, Value languageBinding, String languageId) {
        this.graalContext = graalContext;
        this.languageBinding = languageBinding;
        this.languageId = languageId;
    }

    @Nullable
    @Override
    public IScript getScript(int disk, Path path) throws EvaluationException {
        try {
            Source source = Source.newBuilder(this.languageId, ScriptingNetworkHelpers.getScriptingData().getScripts(disk).get(path), path.toString()).build();
            try {
                this.graalContext.eval(source);
            } catch (PolyglotException e) {
                throw new EvaluationException(Component.translatable("script.integratedscripting.error.script_read", path.toString(), disk, e.getMessage()));
            }
            Wrapper<IScriptingData.IDiskScriptsChangeListener> diskListener = new Wrapper<>();
            return new GraalScript(this.graalContext, this.languageBinding, scriptInvalidateListener -> {
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
            throw new EvaluationException(Component.literal(e.getMessage()));
        }
    }
}
