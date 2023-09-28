package org.cyclops.integratedscripting.network;

import net.minecraft.resources.ResourceLocation;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.integrateddynamics.api.network.IIdentifiableNetworkElement;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.core.network.TileNetworkElement;
import org.cyclops.integratedscripting.GeneralConfig;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.blockentity.BlockEntityScriptingDrive;

/**
 * @author rubensworks
 */
public class ScriptingDriveNetworkElement extends TileNetworkElement<BlockEntityScriptingDrive> implements
        IIdentifiableNetworkElement {

    public static final ResourceLocation GROUP = new ResourceLocation(Reference.MOD_ID, "scripting_drive");

    public ScriptingDriveNetworkElement(DimPos pos) {
        super(pos);
    }

    @Override
    public int getId() {
        // TODO: forward disk id
        return 0;
    }

    @Override
    public ResourceLocation getGroup() {
        return ScriptingDriveNetworkElement.GROUP;
    }

    @Override
    public boolean onNetworkAddition(INetwork network) {
        if(super.onNetworkAddition(network)) {
            // TODO: add to network if we have a valid disk
            return true;
        }
        return false;
    }

    @Override
    public void onNetworkRemoval(INetwork network) {
        super.onNetworkRemoval(network);
        // TODO: remove from network
    }

    @Override
    public void setPriorityAndChannel(INetwork network, int priority, int channel) {

    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getChannel() {
        return IPositionedAddonsNetwork.DEFAULT_CHANNEL;
    }

    @Override
    public int getConsumptionRate() {
        return GeneralConfig.scriptingDriveBaseConsumption;
    }

    @Override
    protected Class<BlockEntityScriptingDrive> getTileClass() {
        return BlockEntityScriptingDrive.class;
    }

}
