package org.cyclops.integratedscripting.core.network;

import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.api.network.IScript;
import org.cyclops.integratedscripting.api.network.IScriptFactory;
import org.graalvm.polyglot.Context;
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
            this.graalContext.eval(source);
            return new GraalScript(this.graalContext, this.languageBinding);
        } catch (IOException e) {
            throw new EvaluationException(Component.literal(e.getMessage()));
        }
    }
}
