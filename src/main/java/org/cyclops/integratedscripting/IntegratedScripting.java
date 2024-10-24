package org.cyclops.integratedscripting;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import org.apache.logging.log4j.Level;
import org.cyclops.cyclopscore.config.ConfigHandler;
import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.cyclopscore.infobook.IInfoBookRegistry;
import org.cyclops.cyclopscore.init.ModBaseVersionable;
import org.cyclops.cyclopscore.proxy.IClientProxy;
import org.cyclops.cyclopscore.proxy.ICommonProxy;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.item.IVariableFacadeHandlerRegistry;
import org.cyclops.integrateddynamics.infobook.OnTheDynamicsOfIntegrationBook;
import org.cyclops.integratedscripting.api.evaluate.translation.IValueTranslatorRegistry;
import org.cyclops.integratedscripting.api.language.ILanguageHandlerRegistry;
import org.cyclops.integratedscripting.block.BlockMendesiteConfig;
import org.cyclops.integratedscripting.block.BlockScriptingDriveConfig;
import org.cyclops.integratedscripting.blockentity.BlockEntityScriptingDriveConfig;
import org.cyclops.integratedscripting.capability.ScriptingNetworkCapabilityConstructors;
import org.cyclops.integratedscripting.command.CommandTestScript;
import org.cyclops.integratedscripting.component.DataComponentDiskIdConfig;
import org.cyclops.integratedscripting.core.client.model.ScriptingVariableModelProviders;
import org.cyclops.integratedscripting.core.evaluate.ScriptVariableFacadeHandler;
import org.cyclops.integratedscripting.core.language.LanguageHandlerRegistry;
import org.cyclops.integratedscripting.core.language.LanguageHandlers;
import org.cyclops.integratedscripting.core.network.ScriptingData;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslatorRegistry;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.cyclops.integratedscripting.inventory.container.ContainerScriptingDriveConfig;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScriptingConfig;
import org.cyclops.integratedscripting.item.ItemScriptingDiskConfig;
import org.cyclops.integratedscripting.part.PartTypes;
import org.cyclops.integratedscripting.proxy.ClientProxy;
import org.cyclops.integratedscripting.proxy.CommonProxy;

/**
 * The main mod class of this mod.
 * @author rubensworks (aka kroeserr)
 *
 */
@Mod(Reference.MOD_ID)
public class IntegratedScripting extends ModBaseVersionable<IntegratedScripting> {

    public static IntegratedScripting _instance;

    public ScriptingData scriptingData;

    public IntegratedScripting(IEventBus modEventBus) {
        super(Reference.MOD_ID, (instance) -> _instance = instance, modEventBus);

        getRegistryManager().addRegistry(IValueTranslatorRegistry.class, ValueTranslatorRegistry.getInstance());
        getRegistryManager().addRegistry(ILanguageHandlerRegistry.class, LanguageHandlerRegistry.getInstance());

        modEventBus.addListener(this::onRegistriesCreate);
        modEventBus.addListener(this::afterSetup);
        modEventBus.register(new ScriptingNetworkCapabilityConstructors());
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
    }

    public void onRegistriesCreate(NewRegistryEvent event) {
        // Register handlers
        IntegratedDynamics._instance.getRegistryManager().getRegistry(IVariableFacadeHandlerRegistry.class)
                .registerHandler(ScriptVariableFacadeHandler.getInstance());

        // Load client models
        if (MinecraftHelpers.isClientSide()) {
            ScriptingVariableModelProviders.load();
        }

        // Load parts
        PartTypes.load();
    }

    @Override
    protected LiteralArgumentBuilder<CommandSourceStack> constructBaseCommand(Commands.CommandSelection selection, CommandBuildContext context) {
        LiteralArgumentBuilder<CommandSourceStack> root = super.constructBaseCommand(selection, context);

        root.then(CommandTestScript.make());

        return root;
    }

    @Override
    protected void setup(FMLCommonSetupEvent event) {
        super.setup(event);

        ValueTranslators.load();
        LanguageHandlers.load();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        this.scriptingData = new ScriptingData(event.getServer().getWorldPath(ScriptingData.LEVEL_RESOURCE));
    }

    @Override
    protected void onServerStopping(ServerStoppingEvent event) {
        if (this.scriptingData != null) {
            this.scriptingData.close();
        }
        this.scriptingData = null;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        if (this.scriptingData != null) {
            this.scriptingData.tick();
        }
    }

    protected void afterSetup(FMLLoadCompleteEvent event) {
        // Initialize info book
        IntegratedDynamics._instance.getRegistryManager().getRegistry(IInfoBookRegistry.class)
                .registerSection(this,
                        OnTheDynamicsOfIntegrationBook.getInstance(), "info_book.integrateddynamics.manual",
                        "/data/" + Reference.MOD_ID + "/info/scripting_info.xml");
        IntegratedDynamics._instance.getRegistryManager().getRegistry(IInfoBookRegistry.class)
                .registerSection(this,
                        OnTheDynamicsOfIntegrationBook.getInstance(), "info_book.integrateddynamics.tutorials",
                        "/data/" + Reference.MOD_ID + "/info/scripting_tutorials.xml");
    }

    @Override
    protected CreativeModeTab.Builder constructDefaultCreativeModeTab(CreativeModeTab.Builder builder) {
        return super.constructDefaultCreativeModeTab(builder)
                .icon(() -> new ItemStack(RegistryEntries.ITEM_SCRIPTING_DISK));
    }

    @Override
    protected void onConfigsRegister(ConfigHandler configHandler) {
        super.onConfigsRegister(configHandler);

        configHandler.addConfigurable(new GeneralConfig());

        configHandler.addConfigurable(new ItemScriptingDiskConfig());

        configHandler.addConfigurable(new BlockScriptingDriveConfig());
        configHandler.addConfigurable(new BlockMendesiteConfig());

        configHandler.addConfigurable(new BlockEntityScriptingDriveConfig());

        configHandler.addConfigurable(new ContainerScriptingDriveConfig());
        configHandler.addConfigurable(new ContainerTerminalScriptingConfig());

        configHandler.addConfigurable(new DataComponentDiskIdConfig());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected IClientProxy constructClientProxy() {
        return new ClientProxy();
    }

    @Override
    protected ICommonProxy constructCommonProxy() {
        return new CommonProxy();
    }

    /**
     * Log a new info message for this mod.
     * @param message The message to show.
     */
    public static void clog(String message) {
        clog(Level.INFO, message);
    }

    /**
     * Log a new message of the given level for this mod.
     * @param level The level in which the message must be shown.
     * @param message The message to show.
     */
    public static void clog(Level level, String message) {
        IntegratedScripting._instance.getLoggerHelper().log(level, message);
    }

}
