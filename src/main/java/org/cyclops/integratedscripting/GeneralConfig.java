package org.cyclops.integratedscripting;

import org.cyclops.cyclopscore.config.ConfigurableProperty;
import org.cyclops.cyclopscore.config.extendedconfig.DummyConfig;
import org.cyclops.cyclopscore.tracking.Versions;

/**
 * A config with general options for this mod.
 * @author rubensworks
 *
 */
public class GeneralConfig extends DummyConfig {

    @ConfigurableProperty(category = "core", comment = "If the version checker should be enabled.")
    public static boolean versionChecker = true;

    public GeneralConfig() {
        super(IntegratedRest._instance, "general");
    }

    @Override
    public void onRegistered() {
        if(versionChecker) {
            Versions.registerMod(getMod(), IntegratedRest._instance, Reference.VERSION_URL);
        }
    }
}
