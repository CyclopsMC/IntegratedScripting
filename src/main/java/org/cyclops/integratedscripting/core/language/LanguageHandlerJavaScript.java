package org.cyclops.integratedscripting.core.language;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.client.gui.image.IImage;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.api.language.ILanguageHandler;
import org.cyclops.integratedscripting.api.language.ScriptCompletions;
import org.cyclops.integratedscripting.api.language.ScriptSyntaxError;
import org.cyclops.integratedscripting.api.network.IScriptFactory;
import org.cyclops.integratedscripting.client.gui.image.ScriptImages;
import org.cyclops.integratedscripting.core.network.GraalScriptFactory;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static final Style STRING = Style.EMPTY.withColor(TextColor.fromRgb(Helpers.RGBToInt(24, 54, 145))); // Dark blue

    // Keywords and literals, which can never be the name of a member in a (non-strict) script
    public static final Set<String> RESERVED_WORDS = Set.of(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else",
            "enum", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof",
            "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var", "void",
            "while", "with"
    );

    public final Map<String, Style> tokenStyles;
    private final IScriptFactory scriptFactory;
    @Nullable
    private Map<String, Component> operatorSignatures;

    public LanguageHandlerJavaScript() {
        this.tokenStyles = Maps.newHashMap();

        for (String keyword : new String[]{
                "const", "delete", "let", "var", "function", "return", "if", "else", "for", "while", "do", "break",
                "continue", "switch", "case", "default", "new", "this", "typeof", "instanceof", "in", "of", "class",
                "extends", "super", "import", "export", "from", "try", "catch", "finally", "throw", "async", "await",
                "yield", "void"
        }) {
            this.tokenStyles.put(keyword, KEYWORD);
        }
        for (String constant : new String[]{"true", "false", "null", "undefined"}) {
            this.tokenStyles.put(constant, CONSTANT);
        }

        this.tokenStyles.put("{", SYMBOL);
        this.tokenStyles.put("}", SYMBOL);
        this.tokenStyles.put("(", SYMBOL);
        this.tokenStyles.put(")", SYMBOL);
        this.tokenStyles.put("[", SYMBOL);
        this.tokenStyles.put("]", SYMBOL);
        this.tokenStyles.put(".", SYMBOL);
        this.tokenStyles.put(";", SYMBOL);
        this.tokenStyles.put("=", SYMBOL);

        this.scriptFactory = new GraalScriptFactory("js");
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
        return this.markupScript(line);
    }

    @Override
    public List<Pair<Style, String>> markupScript(String script) {
        List<Pair<Style, String>> segments = Lists.newArrayList();
        int length = script.length();
        int i = 0;
        while (i < length) {
            char c = script.charAt(i);
            int end;
            Style style;
            if (c == '/' && i + 1 < length && script.charAt(i + 1) == '/') {
                end = script.indexOf('\n', i);
                end = end < 0 ? length : end;
                style = COMMENT;
            } else if (c == '/' && i + 1 < length && script.charAt(i + 1) == '*') {
                end = script.indexOf("*/", i + 2);
                end = end < 0 ? length : end + 2;
                style = COMMENT;
            } else if (c == '"' || c == '\'' || c == '`') {
                end = i + 1;
                while (end < length && script.charAt(end) != c && (c == '`' || script.charAt(end) != '\n')) {
                    end += script.charAt(end) == '\\' ? 2 : 1;
                }
                if (end < length && script.charAt(end) == c) {
                    end++;
                }
                end = Math.min(length, end);
                style = STRING;
            } else if (Character.isDigit(c)) {
                end = i + 1;
                while (end < length && (Character.isLetterOrDigit(script.charAt(end)) || script.charAt(end) == '.' || script.charAt(end) == '_')) {
                    end++;
                }
                style = CONSTANT;
            } else if (Character.isJavaIdentifierStart(c)) {
                end = i + 1;
                while (end < length && Character.isJavaIdentifierPart(script.charAt(end))) {
                    end++;
                }
                style = this.tokenStyles.getOrDefault(script.substring(i, end), Style.EMPTY);
            } else {
                end = i + 1;
                style = this.tokenStyles.getOrDefault(String.valueOf(c), Style.EMPTY);
            }
            addSegment(segments, style, script.substring(i, end));
            i = end;
        }
        return segments;
    }

    private static void addSegment(List<Pair<Style, String>> segments, Style style, String text) {
        // Merge with the previous segment if it has the same style
        if (!segments.isEmpty() && segments.get(segments.size() - 1).getLeft().equals(style)) {
            Pair<Style, String> last = segments.remove(segments.size() - 1);
            segments.add(Pair.of(style, last.getRight() + text));
        } else {
            segments.add(Pair.of(style, text));
        }
    }

    @Override
    public Set<String> getReservedWords() {
        return RESERVED_WORDS;
    }

    @Nullable
    @Override
    public Set<String> getTopLevelMembers(String script) {
        return JavaScriptDeclarations.find(script);
    }

    @Nullable
    @Override
    public ScriptCompletions getCompletions(String script, int cursor) {
        return JavaScriptCompletions.complete(script, cursor, getOperatorSignatures());
    }

    @Nullable
    @Override
    public Component getSignatureHint(String script, int cursor) {
        return JavaScriptCompletions.signatureHint(script, cursor, getOperatorSignatures());
    }

    protected Map<String, Component> getOperatorSignatures() {
        // Operators are registered once, so this can be cached
        if (this.operatorSignatures == null) {
            Map<String, Component> signatures = Maps.newHashMap();
            for (Map.Entry<String, IOperator> entry : Operators.REGISTRY.getGlobalInteractOperators().entrySet()) {
                IOperator operator = entry.getValue();
                MutableComponent signature = Component.literal("(");
                IValueType<?>[] inputs = operator.getInputTypes();
                for (int i = 0; i < inputs.length; i++) {
                    if (i > 0) {
                        signature.append(", ");
                    }
                    signature.append(getTypeName(inputs[i]));
                }
                signatures.put(entry.getKey(), signature.append("): ").append(getTypeName(operator.getOutputType())));
            }
            this.operatorSignatures = signatures;
        }
        return this.operatorSignatures;
    }

    /**
     * @return The name of a value type in its color, as in tooltips.
     */
    protected static Component getTypeName(IValueType<?> type) {
        return Component.translatable(type.getTranslationKey()).withStyle(type.getDisplayColorFormat());
    }

    @Override
    public IScriptFactory getScriptFactory() throws EvaluationException {
        return this.scriptFactory;
    }

    @Nullable
    @Override
    public ScriptSyntaxError checkSyntax(Path path, String script) {
        try (Context context = ScriptHelpers.createBaseContext(null)) {
            // Parsing doesn't execute the script
            context.parse(Source.newBuilder("js", script, path.toString()).build());
            return null;
        } catch (PolyglotException e) {
            if (!e.isSyntaxError()) {
                return null;
            }
            SourceSection location = e.getSourceLocation();
            // Only keep the first line, as the following lines contain a source excerpt
            return new ScriptSyntaxError(e.getMessage().split("\n", 2)[0],
                    location != null && location.hasLines() ? location.getStartLine() : 1,
                    location != null && location.hasColumns() ? location.getStartColumn() : 1);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void onServerStarted() {
        this.generateTypeScriptTypes();
    }

    private void generateTypeScriptTypes() {
        Path path = IntegratedScripting._instance.scriptingData.getRootPath().resolve(Reference.MOD_ID + ".d.ts");
        try {
            FileUtils.write(path.toFile(), new TypeScriptTypingsGenerator().generate(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
