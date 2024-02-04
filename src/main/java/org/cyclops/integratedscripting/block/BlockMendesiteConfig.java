package org.cyclops.integratedscripting.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfig;
import org.cyclops.integratedscripting.IntegratedScripting;

/**
 * Config for the Mendesite block.
 * @author rubensworks
 *
 */
public class BlockMendesiteConfig extends BlockConfig {

    public BlockMendesiteConfig() {
        super(
                IntegratedScripting._instance,
                "mendesite",
                eConfig -> new Block(Block.Properties.of()
                        .sound(SoundType.SNOW)
                        .strength(1.5F)
                        .noOcclusion()),
                getDefaultItemConstructor(IntegratedScripting._instance)
        );
    }

}
