package org.cyclops.integratedscripting.api.network;

import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.integratedscripting.IntegratedScripting;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Server-side singleton for handling scripts inside the game.
 * @see IntegratedScripting#scriptingData
 * @author rubensworks
 */
public interface IScriptingData {

    public Collection<Integer> getDisks();

    public Map<Path, String> getScripts(int disk);

    public void setScripts(int disk, Map<Path, String> scripts, ChangeLocation changeLocation);
    public void setScript(int disk, Path scriptPathRelative, @Nullable String script, ChangeLocation changeLocation);

    public void markDirty(int disk, Path scriptPathRelative);

    public void addListener(int disk, IDiskScriptsChangeListener listener);

    public void removeListener(int disk, IDiskScriptsChangeListener listener);

    public Pair<OutputStream, OutputStream> getOutputStreams(int disk, Path scriptPathRelative);

    public static interface IDiskScriptsChangeListener {
        public void onChange(Path scriptPathRelative);
    }

    /**
     * Indicates the location from where the change originated.
     */
    public static enum ChangeLocation {
        /**
         * If the change originates in-memory from within the game.
         * This will cause changes to propagate to the disk.
         */
        MEMORY,
        /**
         * If the change originates from the disk outside of the game.
         * This will cause changes to propagate to the game.
         */
        DISK
    }

}
