package org.cyclops.integratedscripting;

import net.minecraftforge.fml.config.ModConfig;
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

    @ConfigurableProperty(category = "general", comment = "The base energy usage for the scripting drive.", minimalValue = 0, configLocation = ModConfig.Type.SERVER)
    public static int scriptingDriveBaseConsumption = 2;
    @ConfigurableProperty(category = "general", comment = "The base energy usage for the scripting terminal.", minimalValue = 0, configLocation = ModConfig.Type.SERVER)
    public static int terminalScriptingBaseConsumption = 1;
    @ConfigurableProperty(category = "general", comment = "The minimum number of ticks inbetween sending a script change packet from client to server.", minimalValue = 0, configLocation = ModConfig.Type.SERVER)
    public static int terminalScriptingClientSyncTickInterval = 20;

    public GeneralConfig() {
        super(IntegratedScripting._instance, "general");
    }

    @Override
    public void onRegistered() {
        if(versionChecker) {
            Versions.registerMod(getMod(), IntegratedScripting._instance, Reference.VERSION_URL);
        }
    }
}
