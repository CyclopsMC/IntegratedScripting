package org.cyclops.integratedscripting.api.item;

import org.cyclops.integrateddynamics.api.item.IVariableFacade;

import java.nio.file.Path;

/**
 * Variable facade for variables that refer to a script.
 * @author rubensworks
 */
public interface IScriptVariableFacade extends IVariableFacade {

    /**
     * @return The script disk id.
     */
    public int getDisk();

    /**
     * @return The path to the script inside the disk.
     */
    public Path getPath();

    /**
     * @return The referenced member element inside the file referred to by the path inside the disk.
     */
    public String getMember();

}
