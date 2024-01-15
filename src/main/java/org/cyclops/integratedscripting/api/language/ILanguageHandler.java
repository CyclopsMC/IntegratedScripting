package org.cyclops.integratedscripting.api.language;

import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.api.network.IScriptFactory;

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

    public void onServerStarted();

}
