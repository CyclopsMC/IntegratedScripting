package org.cyclops.integratedscripting.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.cyclops.integrateddynamics.core.block.BlockWithEntityGuiCabled;
import org.cyclops.integratedscripting.blockentity.BlockEntityScriptingDrive;

import javax.annotation.Nullable;

/**
 * A block that can hold scripting disks so that they can be referred to elsewhere in the network.
 *
 * @author rubensworks
 */
public class BlockScriptingDrive extends BlockWithEntityGuiCabled {

    public static final MapCodec<BlockScriptingDrive> CODEC = simpleCodec(BlockScriptingDrive::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockScriptingDrive(Properties properties) {
        super(properties, BlockEntityScriptingDrive::new);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

}
