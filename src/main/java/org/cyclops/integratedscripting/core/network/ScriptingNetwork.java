package org.cyclops.integratedscripting.core.network;

import com.google.common.collect.Sets;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;

import java.util.Collections;
import java.util.Set;

/**
 * @author rubensworks
 */
public class ScriptingNetwork implements IScriptingNetwork {

    private final Set<Integer> disks = Sets.newHashSet();

    @Override
    public void addDisk(int disk) {
        disks.add(disk);
    }

    @Override
    public void removeDisk(int disk) {
        disks.remove(disk);
    }

    @Override
    public Set<Integer> getDisks() {
        return Collections.unmodifiableSet(this.disks);
    }

}
