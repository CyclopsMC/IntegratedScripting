package org.cyclops.integratedscripting.api.network;

import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Set;

/**
 * Capability for handling scripts inside a network.
 * @author rubensworks
 */
public interface IScriptingNetwork {

    /**
     * Indicate that the given disk is contained in this network.
     * @param disk A disk id.
     */
    public void addDisk(int disk);

    /**
     * Indicate that the given disk is not contained in this network.
     * @param disk A disk id.
     */
    public void removeDisk(int disk);

    /**
     * @return All disks contained in this network.
     */
    public Set<Integer> getDisks();

    /**
     * Get the script by the given path in the given disk.
     * @param disk A disk id.
     * @param path A script path.
     * @return The script or null.
     */
    @Nullable
    public IScript getScript(int disk, Path path) throws EvaluationException;

    /**
     * Get a variable containing the value of the given script member.
     * This variable can be cached and invalidated if the underlying script is modified.
     * @param disk A disk id.
     * @param path A script path.
     * @param member A script member name.
     * @param <V> The value type.
     * @return A variable.
     */
    public <V extends IValue> IVariable<V> getOrCreateVariable(int disk, Path path, String member);

}
