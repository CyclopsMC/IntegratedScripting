package org.cyclops.integratedscripting.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import org.cyclops.cyclopscore.config.extendedconfig.DataComponentConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * @author rubensworks
 */
public class DataComponentDiskIdConfig extends DataComponentConfigCommon<Integer, IModBase> {

    public DataComponentDiskIdConfig() {
        super(IntegratedScripting._instance, "disk_id", builder -> builder
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.INT));
    }
}
