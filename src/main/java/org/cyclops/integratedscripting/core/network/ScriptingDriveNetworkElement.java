package org.cyclops.integratedscripting.core.network;

import net.minecraft.resources.ResourceLocation;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.integrateddynamics.api.network.IIdentifiableNetworkElement;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.core.network.TileNetworkElement;
import org.cyclops.integratedscripting.GeneralConfig;
import org.cyclops.integratedscripting.Reference;
import org.cyclops.integratedscripting.blockentity.BlockEntityScriptingDrive;

import java.util.function.Supplier;

/**
 * @author rubensworks
 */
public class ScriptingDriveNetworkElement extends TileNetworkElement<BlockEntityScriptingDrive> implements
        IIdentifiableNetworkElement {

    public static final ResourceLocation GROUP = new ResourceLocation(Reference.MOD_ID, "scripting_drive");

    private final Supplier<Integer> idGetter;

    public ScriptingDriveNetworkElement(DimPos pos, Supplier<Integer> idGetter) {
        super(pos);
        this.idGetter = idGetter;
    }

    @Override
    public int getId() {
        return this.idGetter.get();
    }

    @Override
    public ResourceLocation getGroup() {
        return ScriptingDriveNetworkElement.GROUP;
    }

    @Override
    public boolean onNetworkAddition(INetwork network) {
        if(super.onNetworkAddition(network)) {
            return ScriptingNetworkHelpers.getScriptingNetwork(network)
                    .map(scriptingNetwork -> {
                        scriptingNetwork.addDisk(getId());
                        return true;
                    })
                    .orElse(false);
        }
        return false;
    }

    @Override
    public void onNetworkRemoval(INetwork network) {
        super.onNetworkRemoval(network);
        ScriptingNetworkHelpers.getScriptingNetwork(network)
                .ifPresent(scriptingNetwork -> {
                    scriptingNetwork.removeDisk(getId());
                });
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
