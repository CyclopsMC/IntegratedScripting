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

    @ConfigurableProperty(category = "general", comment = "If new processes can be created from guest languages. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowCreateProcess = false;
    @ConfigurableProperty(category = "general", comment = "If new threads can be created from guest languages. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowCreateThread = false;
    @ConfigurableProperty(category = "general", comment = "If IO is allowed from guest languages. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowIo = false;
    @ConfigurableProperty(category = "general", comment = "If host class loading is allowed from guest languages. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostClassLoading = false;
    @ConfigurableProperty(category = "general", comment = "If experimental options can be used in guest languages. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowExperimentalOptions = false;
    @ConfigurableProperty(category = "general", comment = "If environment variables can be accessed from guest languages. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowEnvironment = false;
    @ConfigurableProperty(category = "general", comment = "If the native interface can be accessed from guest languages. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowNative = false;
    @ConfigurableProperty(category = "general", comment = "If all Java public constructors should be accessible. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostPublicAccess = false;
    @ConfigurableProperty(category = "general", comment = "Allow guest languages to implement any Java interface. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostAllImplementations = false;
    @ConfigurableProperty(category = "general", comment = "Allow guest languages to implement (extend) any Java class. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostAllClassImplementations = false;
    @ConfigurableProperty(category = "general", comment = "Allows the guest application to access arrays as values with array elements.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostArrayAccess = true;
    @ConfigurableProperty(category = "general", comment = "Allows the guest application to access lists as values with array elements and iterators.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostListAccess = true;
    @ConfigurableProperty(category = "general", comment = "Allows the guest application to access java. nio. ByteBuffers as values with buffer elements.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostBufferAccess = true;
    @ConfigurableProperty(category = "general", comment = "Allows the guest application to access iterables as values with iterators.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostIterableAccess = true;
    @ConfigurableProperty(category = "general", comment = "Allows the guest application to access iterators as iterator values.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostIteratorAccess = true;
    @ConfigurableProperty(category = "general", comment = "Allows the guest application to access map as hash values.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostMapAccess = true;
    @ConfigurableProperty(category = "general", comment = "Allows the guest application to inherit access to allowed methods, i. e. implementations of allowed abstract and interface methods and overrides of allowed concrete methods. Only enable this on private servers and if you know what you are doing.", configLocation = ModConfig.Type.SERVER)
    public static boolean graalAllowHostAccessInheritance = false;
    @ConfigurableProperty(category = "general", comment = "The maximum number of statements that can be executed in one evaluation. This is to avoid infinite loops or very complex programs. Set to -1 to disable limit (not recommended).", configLocation = ModConfig.Type.SERVER, minimalValue = -1)
    public static int graalStatementLimit = 16384;
    @ConfigurableProperty(category = "general", comment = "The maximum number of lines in stdout and stderr script log files. Set to -1 to disable limit.", minimalValue = -1, configLocation = ModConfig.Type.SERVER)
    public static int maxLogLines = 2096;

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
