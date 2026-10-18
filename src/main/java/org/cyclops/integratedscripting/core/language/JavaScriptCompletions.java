package org.cyclops.integratedscripting.core.language;

import com.google.common.collect.Lists;
import org.cyclops.integratedscripting.api.language.ScriptCompletions;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Completion of the global operators on idContext.ops in JavaScript.
 * @author rubensworks
 */
public class JavaScriptCompletions {

    public static final String OPS_PREFIX = "idContext.ops.";

    /**
     * @param script A script.
     * @param cursor The cursor position.
     * @param signatures Operator names mapped to their signatures.
     * @return The operators matching the name before the cursor, or null if the cursor is not after idContext.ops.
     */
    @Nullable
    public static ScriptCompletions complete(String script, int cursor, Map<String, String> signatures) {
        int start = cursor;
        while (start > 0 && Character.isJavaIdentifierPart(script.charAt(start - 1))) {
            start--;
        }
        if (!script.startsWith(OPS_PREFIX, start - OPS_PREFIX.length())) {
            return null;
        }
        // Don't complete in the middle of a name
        if (cursor < script.length() && Character.isJavaIdentifierPart(script.charAt(cursor))) {
            return null;
        }
        String prefix = script.substring(start, cursor);
        String prefixLower = prefix.toLowerCase(Locale.ROOT);
        List<ScriptCompletions.Item> items = Lists.newArrayList();
        for (Map.Entry<String, String> entry : signatures.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).startsWith(prefixLower)) {
                items.add(new ScriptCompletions.Item(entry.getKey(), entry.getValue()));
            }
        }
        if (items.isEmpty() || (items.size() == 1 && items.get(0).name().equals(prefix))) {
            return null;
        }
        items.sort(Comparator.comparing(ScriptCompletions.Item::name));
        return new ScriptCompletions(start, items);
    }

}
