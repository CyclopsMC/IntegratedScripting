package org.cyclops.integratedscripting.blockentity;

import com.google.common.collect.Sets;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.cyclops.cyclopscore.config.extendedconfig.BlockEntityConfig;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.RegistryEntries;

/**
 * Config for the {@link BlockEntityScriptingDrive}.
 * @author rubensworks
 *
 */
public class BlockEntityScriptingDriveConfig extends BlockEntityConfig<BlockEntityScriptingDrive> {

    public BlockEntityScriptingDriveConfig() {
        super(
                IntegratedScripting._instance,
                "scripting_drive",
                (eConfig) -> new BlockEntityType<>(BlockEntityScriptingDrive::new,
                        Sets.newHashSet(RegistryEntries.BLOCK_SCRIPTING_DRIVE.get()), null)
        );
        IntegratedDynamics._instance.getModEventBus().addListener(this::registerCapability);
    }

    protected void registerCapability(RegisterCapabilitiesEvent event) {
        BlockEntityScriptingDrive.registerScriptingDriveCapabilities(event, getInstance());
    }

}
