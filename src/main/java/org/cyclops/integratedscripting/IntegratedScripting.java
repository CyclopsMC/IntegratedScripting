package org.cyclops.integratedscripting;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.Level;
import org.cyclops.cyclopscore.config.ConfigHandler;
import org.cyclops.cyclopscore.infobook.IInfoBookRegistry;
import org.cyclops.cyclopscore.init.ItemGroupMod;
import org.cyclops.cyclopscore.init.ModBaseVersionable;
import org.cyclops.cyclopscore.proxy.IClientProxy;
import org.cyclops.cyclopscore.proxy.ICommonProxy;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.infobook.OnTheDynamicsOfIntegrationBook;
import org.cyclops.integratedscripting.command.CommandTestScript;
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

    public IntegratedScripting() {
        super(Reference.MOD_ID, (instance) -> _instance = instance);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::afterSetup);
    }

    @Override
    protected LiteralArgumentBuilder<CommandSourceStack> constructBaseCommand() {
        LiteralArgumentBuilder<CommandSourceStack> root = super.constructBaseCommand();

        root.then(CommandTestScript.make());

        return root;
    }

    protected void afterSetup(FMLLoadCompleteEvent event) {
        // Initialize info book
        IntegratedDynamics._instance.getRegistryManager().getRegistry(IInfoBookRegistry.class)
                .registerSection(this,
                        OnTheDynamicsOfIntegrationBook.getInstance(), "info_book.integrateddynamics.manual",
                        "/data/" + Reference.MOD_ID + "/info/scripting_info.xml");
    }

    @Override
    public CreativeModeTab constructDefaultCreativeModeTab() {
        return new ItemGroupMod(this, () -> Items.BARRIER);
    }

    @Override
    protected void onConfigsRegister(ConfigHandler configHandler) {
        super.onConfigsRegister(configHandler);

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
