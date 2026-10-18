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
import java.util.ArrayList;
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

    /**
     * Mark up a full script, which allows constructs such as block comments to span multiple lines.
     * @param script The script contents.
     * @return Styled segments that together form the full script.
     */
    public default List<Pair<Style, String>> markupScript(String script) {
        List<Pair<Style, String>> segments = new ArrayList<>();
        String[] lines = script.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                segments.add(Pair.of(Style.EMPTY, "\n"));
            }
            segments.addAll(markupLine(lines[i]));
        }
        return segments;
    }

    public IScriptFactory getScriptFactory() throws EvaluationException;

    /**
     * @return Words of the language that can not be the name of a script member, such as keywords.
     */
    public default Set<String> getReservedWords() {
        return Collections.emptySet();
    }

    /**
     * @param script The script contents.
     * @return The names of the members declared at the top level of the script,
     *         or null if they can not be determined.
     */
    @Nullable
    public default Set<String> getTopLevelMembers(String script) {
        return null;
    }

    /**
     * @param script The script contents.
     * @param cursor The cursor position.
     * @return Completion candidates at the cursor, or null if there are none.
     */
    @Nullable
    public default ScriptCompletions getCompletions(String script, int cursor) {
        return null;
    }

    /**
     * @param script The script contents.
     * @param cursor The cursor position.
     * @return The signature of the function call the cursor is in, or null if unknown.
     */
    @Nullable
    public default String getSignatureHint(String script, int cursor) {
        return null;
    }

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
