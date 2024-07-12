package org.cyclops.integratedscripting.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import org.cyclops.cyclopscore.config.extendedconfig.DataComponentConfig;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * @author rubensworks
 */
public class DataComponentDiskIdConfig extends DataComponentConfig<Integer> {

    public DataComponentDiskIdConfig() {
        super(IntegratedScripting._instance, "disk_id", builder -> builder
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.INT));
    }
}
