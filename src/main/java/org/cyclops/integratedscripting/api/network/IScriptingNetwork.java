package org.cyclops.integratedscripting.api.network;

import java.util.Set;

/**
 * Capability for handling scripts inside a network.
 * @author rubensworks
 */
public interface IScriptingNetwork {

    public void addDisk(int disk);

    public void removeDisk(int disk);

    public Set<Integer> getDisks();

}
