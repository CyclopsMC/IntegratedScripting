package org.cyclops.integratedscripting.api.language;

import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.api.network.IScriptFactory;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;

/**
 * Logic for handling a specific programming language.
 * @author rubensworks
 */
public interface ILanguageHandler {

    public String getName();

    @OnlyIn(Dist.CLIENT)
    public IImage getIcon();

    public List<String> getExtensions();

    public List<Pair<Style, String>> markupLine(String line);

    public IScriptFactory getScriptFactory() throws EvaluationException;

    /**
     * Check a script for syntax errors, without executing it.
     * @param path The script path.
     * @param script The script contents.
     * @return The first syntax error, or null if there is none or if checking is not supported.
     */
    @Nullable
    public default ScriptSyntaxError checkSyntax(Path path, String script) {
        return null;
    }

    public void onServerStarted();

}
