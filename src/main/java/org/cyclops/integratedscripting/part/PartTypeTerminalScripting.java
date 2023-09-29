package org.cyclops.integratedscripting.part;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfig;
import org.cyclops.cyclopscore.init.ModBase;
import org.cyclops.cyclopscore.network.PacketCodec;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.block.IgnoredBlock;
import org.cyclops.integrateddynamics.core.helper.L10NValues;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.part.PartStateEmpty;
import org.cyclops.integrateddynamics.core.part.PartTypeBase;
import org.cyclops.integrateddynamics.core.part.panel.PartTypePanel;
import org.cyclops.integratedscripting.GeneralConfig;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.inventory.container.ContainerTerminalScripting;

import java.util.Optional;

/**
 * A part that exposes a gui using which players can manage scripts in the network.
 * @author rubensworks
 */
public class PartTypeTerminalScripting extends PartTypePanel<PartTypeTerminalScripting, PartStateEmpty<PartTypeTerminalScripting>> {

    public PartTypeTerminalScripting(String name) {
        super(name);
    }

    @Override
    public InteractionResult onPartActivated(PartStateEmpty<PartTypeTerminalScripting> partState, BlockPos pos, Level world, Player player, InteractionHand hand, ItemStack heldItem, BlockHitResult hit) {
        if (isUpdate(partState) && !partState.isEnabled()) {
            player.displayClientMessage(Component.translatable(L10NValues.PART_ERROR_LOWENERGY), true);
            return InteractionResult.FAIL;
        }
        return super.onPartActivated(partState, pos, world, player, hand, heldItem, hit);
    }

    @Override
    protected Block createBlock(BlockConfig blockConfig) {
        return new IgnoredBlock();
    }

    @Override
    public ModBase<?> getMod() {
        return IntegratedScripting._instance;
    }

    @Override
    public boolean isUpdate(PartStateEmpty<PartTypeTerminalScripting> state) {
        return getConsumptionRate(state) > 0 && org.cyclops.integrateddynamics.GeneralConfig.energyConsumptionMultiplier > 0;
    }

    @Override
    public int getConsumptionRate(PartStateEmpty<PartTypeTerminalScripting> state) {
        return GeneralConfig.terminalScriptingBaseConsumption;
    }

    @Override
    protected PartStateEmpty<PartTypeTerminalScripting> constructDefaultState() {
        return new PartStateEmpty<PartTypeTerminalScripting>() {
            @Override
            public int getUpdateInterval() {
                return 1; // For enabling energy consumption
            }
        };
    }

    @Override
    public Optional<MenuProvider> getContainerProvider(PartPos pos) {
        return Optional.of(new MenuProvider() {

            @Override
            public Component getDisplayName() {
                return Component.translatable(getTranslationKey());
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
                Triple<IPartContainer, PartTypeBase, PartTarget> data = PartHelpers.getContainerPartConstructionData(pos);
                return new ContainerTerminalScripting(id, playerInventory,
                        data.getRight(), Optional.of(data.getLeft()), (PartTypeTerminalScripting) data.getMiddle());
            }
        });
    }

    @Override
    public void writeExtraGuiData(FriendlyByteBuf packetBuffer, PartPos pos, ServerPlayer player) {
        PacketCodec.write(packetBuffer, pos);
        super.writeExtraGuiData(packetBuffer, pos, player);
    }
}
