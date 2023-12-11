package org.cyclops.integratedscripting.api.language;

import org.cyclops.cyclopscore.init.IRegistry;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Registry for {@link ILanguageHandler}'s.
 * @author rubensworks
 */
public interface ILanguageHandlerRegistry extends IRegistry {

    public void register(ILanguageHandler provider);

    @Nullable
    public ILanguageHandler getProvider(Path filePath);

}
