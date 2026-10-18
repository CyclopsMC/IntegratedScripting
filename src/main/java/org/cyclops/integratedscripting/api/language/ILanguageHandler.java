package org.cyclops.integratedscripting.api.language;

import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.api.network.IScriptFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
     * @return Words of the language that can not be the name of a script member, such as keywords.
     */
    public default Set<String> getReservedWords() {
        return Collections.emptySet();
    }

    public void onServerStarted();

}
