package org.cyclops.integratedscripting;

import org.cyclops.cyclopscore.config.ConfigurablePropertyCommon;
import org.cyclops.cyclopscore.config.ModConfigLocation;
import org.cyclops.cyclopscore.config.extendedconfig.DummyConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;

/**
 * A config with general options for this mod.
 * @author rubensworks
 *
 */
public class GeneralConfig extends DummyConfigCommon<IModBase> {

    @ConfigurablePropertyCommon(category = "core", comment = "If the version checker should be enabled.")
    public static boolean versionChecker = true;

    @ConfigurablePropertyCommon(category = "general", comment = "The base energy usage for the scripting drive.", minimalValue = 0, configLocation = ModConfigLocation.SERVER)
    public static int scriptingDriveBaseConsumption = 2;
    @ConfigurablePropertyCommon(category = "general", comment = "The base energy usage for the scripting terminal.", minimalValue = 0, configLocation = ModConfigLocation.SERVER)
    public static int terminalScriptingBaseConsumption = 1;
    @ConfigurablePropertyCommon(category = "general", comment = "The minimum number of ticks inbetween sending a script change packet from client to server.", minimalValue = 0, configLocation = ModConfigLocation.SERVER)
    public static int terminalScriptingClientSyncTickInterval = 20;

    @ConfigurablePropertyCommon(category = "general", comment = "If new processes can be created from guest languages.", configLocation = ModConfigLocation.SERVER)
    public static boolean graalAllowCreateProcess = false;
    @ConfigurablePropertyCommon(category = "general", comment = "If new threads can be created from guest languages.", configLocation = ModConfigLocation.SERVER)
    public static boolean graalAllowCreateThread = false;
    @ConfigurablePropertyCommon(category = "general", comment = "If IO is allowed from guest languages.", configLocation = ModConfigLocation.SERVER)
    public static boolean graalAllowIo = false;
    @ConfigurablePropertyCommon(category = "general", comment = "If host class loading is allowed from guest languages.", configLocation = ModConfigLocation.SERVER)
    public static boolean graalAllowHostClassLoading = false;
    @ConfigurablePropertyCommon(category = "general", comment = "If experimental options can be used in guest languages.", configLocation = ModConfigLocation.SERVER)
    public static boolean graalAllowExperimentalOptions = false;
    @ConfigurablePropertyCommon(category = "general", comment = "If environment variables can be accessed from guest languages.", configLocation = ModConfigLocation.SERVER)
    public static boolean graalAllowEnvironment = false;
    @ConfigurablePropertyCommon(category = "general", comment = "If the native interface can be accessed from guest languages.", configLocation = ModConfigLocation.SERVER)
    public static boolean graalAllowNative = false;
    @ConfigurablePropertyCommon(category = "general", comment = "The maximum number of statements that can be executed in one evaluation. This is to avoid infinite loops or very complex programs. Set to -1 to disable limit (not recommended).", configLocation = ModConfigLocation.SERVER, minimalValue = -1)
    public static int graalStatementLimit = 16384;
    @ConfigurablePropertyCommon(category = "general", comment = "The maximum number of lines in stdout and stderr script log files. Set to -1 to disable limit.", minimalValue = -1, configLocation = ModConfigLocation.SERVER)
    public static int maxLogLines = 2096;

    public GeneralConfig() {
        super(IntegratedScripting._instance, "general");
    }
}
