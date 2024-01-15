package org.cyclops.integratedscripting.core.language;

import com.google.common.collect.Maps;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.cyclops.integratedscripting.api.language.ILanguageHandler;
import org.cyclops.integratedscripting.api.language.ILanguageHandlerRegistry;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;

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
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
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
        String extension = ScriptHelpers.getPathExtension(filePath);
        if (extension != null) {
            return extensionToHandlerMap.get(extension);
        }
        return null;
    }

    public void onServerStarted(ServerStartedEvent event) {
        for (ILanguageHandler languageHandler : extensionToHandlerMap.values()) {
            languageHandler.onServerStarted();
        }
    }
}
