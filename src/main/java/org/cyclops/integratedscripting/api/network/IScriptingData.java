package org.cyclops.integratedscripting.api.network;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Capability for handling scripts inside a world.
 * @author rubensworks
 */
public interface IScriptingData {

    public Collection<Integer> getDisks();

    public Map<Path, String> getScripts(int disk);

    public void setScripts(int disk, Map<Path, String> scripts);

    public void markDirty(int disk, Path scriptPathRelative);

    public void addListener(int disk, IScriptChangeListener listener);

    public void removeListener(int disk, IScriptChangeListener listener);

    public static interface IScriptChangeListener {
        public void onChange(Path scriptPathRelative);
    }

}
