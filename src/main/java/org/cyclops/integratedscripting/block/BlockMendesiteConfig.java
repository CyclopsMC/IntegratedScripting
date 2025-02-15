package org.cyclops.integratedscripting.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfigCommon;
import org.cyclops.cyclopscore.init.IModBase;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * Config for the Mendesite block.
 * @author rubensworks
 *
 */
public class BlockMendesiteConfig extends BlockConfigCommon<IModBase> {

    public BlockMendesiteConfig() {
        super(
                IntegratedScripting._instance,
                "mendesite",
                (eConfig, properties) -> new Block(properties
                        .sound(SoundType.SNOW)
                        .strength(1.5F)
                        .noOcclusion()),
                getDefaultItemConstructor(IntegratedScripting._instance)
        );
    }

}
