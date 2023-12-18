package org.cyclops.integratedscripting.api.network;

import javax.annotation.Nullable;

/**
 * A script that contains executable members.
 * @author rubensworks
 */
public interface IScript {

    /**
     * Get the given script member.
     * @param memberName A member name.
     * @return The member, or null if it does not exist.
     */
    @Nullable
    public IScriptMember getMember(String memberName);

}
