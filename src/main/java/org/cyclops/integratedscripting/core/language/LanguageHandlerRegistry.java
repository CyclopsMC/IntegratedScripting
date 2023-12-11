package org.cyclops.integratedscripting.core.language;

import com.google.common.collect.Maps;
import org.cyclops.integratedscripting.api.language.ILanguageHandler;
import org.cyclops.integratedscripting.api.language.ILanguageHandlerRegistry;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author rubensworks
 */
public class LanguageHandlerRegistry implements ILanguageHandlerRegistry {

    private static LanguageHandlerRegistry INSTANCE = new LanguageHandlerRegistry();

    private final Map<String, ILanguageHandler> extensionToHandlerMap = Maps.newHashMap();

    private LanguageHandlerRegistry() {
    }

    /**
     * @return The unique instance.
     */
    public static LanguageHandlerRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public void register(ILanguageHandler translator) {
        for (String extension : translator.getExtensions()) {
            extensionToHandlerMap.put(extension, translator);
        }
    }

    @Nullable
    @Override
    public ILanguageHandler getProvider(Path filePath) {
        String filePathString = filePath.toString();
        int dotPos = filePathString.lastIndexOf('.');
        if (dotPos >= 0 && dotPos + 1 < filePathString.length()) {
            String extension = filePathString.substring(dotPos + 1);
            return extensionToHandlerMap.get(extension);
        }
        return null;
    }
}
