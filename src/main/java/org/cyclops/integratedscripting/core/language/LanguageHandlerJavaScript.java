package org.cyclops.integratedscripting.core.language;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integratedscripting.api.language.ILanguageHandler;
import org.cyclops.integratedscripting.api.network.IScriptFactory;
import org.cyclops.integratedscripting.client.gui.image.ScriptImages;
import org.cyclops.integratedscripting.core.network.GraalScriptFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author rubensworks
 */
public class LanguageHandlerJavaScript implements ILanguageHandler {

    // These colors are inspired by GitHub's color palette
    public static final Style ATTRIBUTE = Style.EMPTY.withColor(TextColor.fromRgb(Helpers.RGBToInt(121, 93, 163))); // Purple
    public static final Style COMMENT = Style.EMPTY.withColor(TextColor.fromRgb(Helpers.RGBToInt(150, 152, 150))); // Gray
    public static final Style SYMBOL = Style.EMPTY.withColor(TextColor.fromRgb(Helpers.RGBToInt(99, 163, 92))); // Green
    public static final Style CONSTANT = Style.EMPTY.withColor(TextColor.fromRgb(Helpers.RGBToInt(0, 134, 179))); // Blue
    public static final Style KEYWORD = Style.EMPTY.withColor(TextColor.fromRgb(Helpers.RGBToInt(167, 29, 93))); // Red-purple

    public final Map<String, Style> tokenStyles;

    public LanguageHandlerJavaScript() {
        this.tokenStyles = Maps.newHashMap();

        this.tokenStyles.put("const", KEYWORD);
        this.tokenStyles.put("let", KEYWORD);
        this.tokenStyles.put("var", KEYWORD);
        this.tokenStyles.put("function", KEYWORD);
        this.tokenStyles.put("return", KEYWORD);
        this.tokenStyles.put("true", KEYWORD);
        this.tokenStyles.put("false", KEYWORD);

        this.tokenStyles.put("{", SYMBOL);
        this.tokenStyles.put("}", SYMBOL);
        this.tokenStyles.put("(", SYMBOL);
        this.tokenStyles.put(")", SYMBOL);
        this.tokenStyles.put("[", SYMBOL);
        this.tokenStyles.put("]", SYMBOL);
        this.tokenStyles.put(".", SYMBOL);
        this.tokenStyles.put(";", SYMBOL);
        this.tokenStyles.put("=", SYMBOL);
    }

    @Override
    public String getName() {
        return "JavaScript";
    }

    @Override
    public IImage getIcon() {
        return ScriptImages.FILE_JS;
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.stream(new String[]{ "js", "cjs", "mjs" }).toList();
    }

    @Override
    public List<Pair<Style, String>> markupLine(String line) {
        // TODO: in the future, write/use JS lexer/tokenizer

        List<Pair<Style, String>> segments = Lists.newArrayList();
        segments.add(Pair.of(Style.EMPTY, line));

        // Split up string by tokens
        for (Map.Entry<String, Style> entry : this.tokenStyles.entrySet()) {
            String token = entry.getKey();
            Style tokenStyle = entry.getValue();
            List<Pair<Style, String>> segmentsNew = Lists.newArrayList();

            for (Pair<Style, String> segment : segments) {
                Style styleOriginal = segment.getLeft();
                String hayStack = segment.getRight();
                int pos;
                while ((pos = hayStack.indexOf(token)) >= 0) {
                    segmentsNew.add(Pair.of(styleOriginal, hayStack.substring(0, pos)));
                    segmentsNew.add(Pair.of(tokenStyle, hayStack.substring(pos, pos + token.length())));
                    hayStack = hayStack.substring(pos + token.length());
                }

                segmentsNew.add(Pair.of(styleOriginal, hayStack));
            }

            segments = segmentsNew;
        }

        return segments;
    }

    @Override
    public IScriptFactory getScriptFactory() throws EvaluationException {
        return new GraalScriptFactory("js");
    }
}
