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

    /**
     * Register a listener that will be invoked when this script gets invalidated.
     * @param listener A listener for invalidations.
     */
    public void addInvalidationListener(IInvalidateListener listener);

    /**
     * Remove all registered invalidation listeners.
     */
    public void removeInvalidationListeners();

    public interface IInvalidateListener {

        /**
         * Called when a script was invalidated.
         */
        public void invalidate();

    }

}
