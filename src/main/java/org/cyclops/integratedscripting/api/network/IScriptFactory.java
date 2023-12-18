package org.cyclops.integratedscripting.api.network;

import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Instantiates scripts for paths in disks.
 * @author rubensworks
 */
public interface IScriptFactory {

    /**
     * Get the script by the given path in the given disk.
     * @param disk A disk id.
     * @param path A script path.
     * @return The script or null.
     */
    @Nullable
    public IScript getScript(int disk, Path path) throws EvaluationException;

}
