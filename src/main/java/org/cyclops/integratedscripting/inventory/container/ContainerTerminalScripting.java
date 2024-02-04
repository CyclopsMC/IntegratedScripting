package org.cyclops.integratedscripting.inventory.container;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.helper.ValueNotifierHelpers;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.inventory.container.InventoryContainer;
import org.cyclops.cyclopscore.persist.IDirtyMarkListener;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.item.IVariableFacadeHandlerRegistry;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.inventory.container.slot.SlotVariable;
import org.cyclops.integratedscripting.IntegratedScripting;
import org.cyclops.integratedscripting.RegistryEntries;
import org.cyclops.integratedscripting.api.item.IScriptVariableFacade;
import org.cyclops.integratedscripting.api.network.IScriptingData;
import org.cyclops.integratedscripting.api.network.IScriptingNetwork;
import org.cyclops.integratedscripting.core.evaluate.ScriptVariableFacadeHandler;
import org.cyclops.integratedscripting.core.item.ScriptVariableFacade;
import org.cyclops.integratedscripting.core.network.ScriptingNetworkHelpers;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingCreateNewScriptPacket;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingDeleteScriptPacket;
import org.cyclops.integratedscripting.network.packet.TerminalScriptingModifiedScriptPacket;
import org.cyclops.integratedscripting.part.PartTypeTerminalScripting;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Container for the crafting jobs overview gui.
 * @author rubensworks
 */
public class ContainerTerminalScripting extends InventoryContainer implements IDirtyMarkListener {

    public static Pattern INVALID_MEMBER_NAME = Pattern.compile("[^0-9a-zA-Z_]");
    public static Pattern VALID_MEMBER_NAME = Pattern.compile("[0-9a-zA-Z_]+");

    private final PartTarget target;
    private final Optional<IPartContainer> partContainer;
    private final PartTypeTerminalScripting partType;
    private final Level world;
    private final Optional<INetwork> network;
    private final Optional<IScriptingNetwork> scriptingNetwork;
    private final Set<Pair<Integer, Path>> clientScriptsDirty;

    private final Int2ObjectMap<Map<Path, String>> lastScripts = new Int2ObjectAVLTreeMap<>();
    private IntList availableDisks;
    private final int activeDiskId;
    private final int activeScriptPathId;
    private final int selectionId;

    public ContainerTerminalScripting(int id, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
        this(id, playerInventory, PartHelpers.readPartTarget(packetBuffer), Optional.empty(),
                PartHelpers.readPart(packetBuffer), InitData.readFromPacketBuffer(packetBuffer));
    }

    public ContainerTerminalScripting(int id, Inventory playerInventory,
                                      PartTarget target, Optional<IPartContainer> partContainer,
                                      PartTypeTerminalScripting partType, InitData initData) {
        super(RegistryEntries.CONTAINER_TERMINAL_SCRIPTING, id, playerInventory, new SimpleInventory(1, 1));

        ((SimpleInventory) getContainerInventory()).addDirtyMarkListener(this);
        addSlot(new SlotVariable(getContainerInventory(), 0, 232, 137));
        addPlayerInventory(playerInventory, 88, 158);

        this.target = target;
        this.partType = partType;
        this.partContainer = partContainer;
        this.world = player.getCommandSenderWorld();

        this.network = NetworkHelpers.getNetwork(getTarget().getCenter()).resolve();
        this.scriptingNetwork = this.network.flatMap(network -> ScriptingNetworkHelpers.getScriptingNetwork(network).resolve());
        this.clientScriptsDirty = Sets.newHashSet();

        this.availableDisks = initData.getAvailableDisks();
        this.activeDiskId = getNextValueId();
        setActiveDisk(this.availableDisks.isEmpty() ? -1 : this.availableDisks.getInt(0));
        this.activeScriptPathId = getNextValueId();
        this.selectionId = getNextValueId();
    }


    public Level getLevel() {
        return world;
    }

    public PartTypeTerminalScripting getPartType() {
        return partType;
    }

    public PartTarget getTarget() {
        return target;
    }

    public Optional<PartTypeTerminalScripting> getPartState() {
        return partContainer.map(p -> (PartTypeTerminalScripting) p.getPartState(getTarget().getCenter().getSide()));
    }

    public Optional<IPartContainer> getPartContainer() {
        return partContainer;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return PartHelpers.canInteractWith(getTarget(), player, this.partContainer.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            ItemStack itemStack = getContainerInventory().getItem(0);
            if(!itemStack.isEmpty()) {
                player.drop(itemStack, false);
            }
        }
    }

    public Optional<INetwork> getNetwork() {
        return network;
    }

    public Optional<IScriptingNetwork> getScriptingNetwork() {
        return scriptingNetwork;
    }

    public IntList getAvailableDisks() {
        return availableDisks;
    }

    public int getActiveDisk() {
        return ValueNotifierHelpers.getValueInt(this, activeDiskId);
    }

    public void setActiveDisk(int activeDisk) {
        ValueNotifierHelpers.setValue(this, activeDiskId, activeDisk);
    }

    public Set<Pair<Integer, Path>> getClientScriptsDirty() {
        return clientScriptsDirty;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (!this.getLevel().isClientSide()) {
            // Send disk contents to clients
            this.getScriptingNetwork().ifPresent(scriptingNetwork -> {
                for (Integer disk : this.getAvailableDisks()) {
                    Map<Path, String> scriptsNew = ScriptingNetworkHelpers.getScriptingData().getScripts(disk);
                    Map<Path, String> scriptsOld = lastScripts.get((int) disk);

                    // Determine files (union of new and old)
                    Set<Path> files;
                    if (scriptsOld == null) {
                        scriptsOld = Maps.newHashMap();
                        lastScripts.put((int) disk, scriptsOld);
                        files = scriptsNew.keySet();
                    } else {
                        files = Sets.newHashSet(scriptsNew.keySet());
                        files.addAll(scriptsOld.keySet());
                    }

                    // Determine changed files
                    for (Path file : files) {
                        String scriptNew = scriptsNew.get(file);
                        String scriptOld = scriptsOld.get(file);
                        if (!Objects.equals(scriptNew, scriptOld)) {
                            // Send separate packet for each modified file
                            if (scriptNew != null) {
                                IntegratedScripting._instance.getPacketHandler()
                                        .sendToPlayer(new TerminalScriptingModifiedScriptPacket(disk, file, scriptNew), (ServerPlayer) player);
                            } else {
                                IntegratedScripting._instance.getPacketHandler()
                                        .sendToPlayer(new TerminalScriptingDeleteScriptPacket(disk, file), (ServerPlayer) player);
                            }

                            // Update the next old value
                            if (scriptNew != null) {
                                scriptsOld.put(file, scriptNew);
                            } else {
                                scriptsOld.remove(file);
                            }

                            // Cleanup if we have no scripts anymore for a certain disk
                            if (scriptsOld.isEmpty()) {
                                lastScripts.remove((int) disk);
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    protected int getSizeInventory() {
        return getContainerInventory().getContainerSize();
    }

    public void setLastScript(int disk, Path path, @Nullable String script) {
        Map<Path, String> lastScriptsDisk = lastScripts.get(disk);
        if (lastScriptsDisk == null) {
            lastScriptsDisk = Maps.newHashMap();
            lastScripts.put(disk, lastScriptsDisk);
        }

        // Store or delete script
        if (script != null) {
            lastScriptsDisk.put(path, script);
        } else {
            lastScriptsDisk.remove(path);
        }

        // Cleanup if we have no scripts anymore for a certain disk
        if (lastScriptsDisk.isEmpty()) {
            lastScripts.remove(disk);
        }
    }

    public Int2ObjectMap<Map<Path, String>> getLastScripts() {
        return lastScripts;
    }

    public void setServerScript(int disk, Path path, @Nullable String script) {
        ScriptingNetworkHelpers.getScriptingData().setScript(disk, path, script, IScriptingData.ChangeLocation.MEMORY);
    }

    public void createNewServerScript(int disk) {
        // Determine unique script name
        Set<Path> existingScripts = ScriptingNetworkHelpers.getScriptingData().getScripts(disk).keySet();
        Path path;
        int i = 0;
        do {
            path = Path.of("script" + i++ + ".js");
        } while (existingScripts.contains(path));

        // Create empty script
        ScriptingNetworkHelpers.getScriptingData().setScript(disk, path, "", IScriptingData.ChangeLocation.MEMORY);
    }

    @Nullable
    public Path getActiveScriptPath() {
        String str = ValueNotifierHelpers.getValueString(this, activeScriptPathId);
        return StringUtil.isNullOrEmpty(str) ? null : Path.of(str);
    }

    public void setActiveScriptPath(Path activeScriptPath) {
        ValueNotifierHelpers.setValue(this, activeScriptPathId, activeScriptPath == null ? "" : activeScriptPath.toString());
    }

    public String getSelectedMember() {
        String str = ValueNotifierHelpers.getValueString(this, selectionId);

        if (str != null) {
            // If "func(abc, ...)" is selected, splice of everything after "("
            int openBracketsPos = str.indexOf("(");
            if (openBracketsPos >= 0) {
                str = str.substring(0, openBracketsPos);
            }
        }

        return str == null ? "" : str;
    }

    public void setSelection(String selection) {
        ValueNotifierHelpers.setValue(this, selectionId, selection);
    }

    public boolean isMemberSelected() {
        return VALID_MEMBER_NAME.matcher(getSelectedMember()).matches();
    }

    @Nullable
    public String getActiveScript() {
        Path path = getActiveScriptPath();
        int disk = getActiveDisk();
        if (path != null && disk >= 0) {
            Map<Path, String> diskScripts = getLastScripts().get(disk);
            if (diskScripts != null) {
                return diskScripts.get(path);
            }
        }
        return null;
    }

    public void setActiveScript(String scriptNew) {
        Path path = getActiveScriptPath();
        int disk = getActiveDisk();
        if (path != null && disk >= 0) {
            Map<Path, String> diskScripts = getLastScripts().get(disk);
            if (diskScripts != null) {
                String scriptOld = diskScripts.put(path, scriptNew);
                if (!Objects.equals(scriptOld, scriptNew)) {
                    this.clientScriptsDirty.add(Pair.of(disk, path));
                }
            }
        }
    }

    public void createNewFile() {
        int disk = getActiveDisk();
        if (disk >= 0) {
            IntegratedScripting._instance.getPacketHandler().sendToServer(new TerminalScriptingCreateNewScriptPacket(disk));
        }
    }

    public List<MutableComponent> getReadErrors() {
        List<MutableComponent> list = Lists.newArrayList();
        if (canWriteScriptToVariable()) {
            if (!isMemberSelected()) {
                list.add(Component.translatable("gui.integratedscripting.error.invalid_member"));
                list.add(Component.translatable("gui.integratedscripting.error.invalid_member.current", getSelectedMember()));
            }
        }
        return list;
    }

    public boolean canWriteScriptToVariable() {
        return !getContainerInventory().getItem(0).isEmpty() && getActiveDisk() >= 0 && getActiveScriptPath() != null;
    }

    @Override
    public void onDirty() {
        SimpleInventory writeInv = (SimpleInventory) getContainerInventory();
        ItemStack itemStack = writeInv.getItem(0);
        if (canWriteScriptToVariable() && !itemStack.isEmpty() && isMemberSelected()) {
            ItemStack outputStack = writeScriptVariable(!world.isClientSide, itemStack.copy(), getActiveDisk(), getActiveScriptPath(), getSelectedMember());
            writeInv.removeDirtyMarkListener(this);
            writeInv.setItem(0, outputStack);
            writeInv.addDirtyMarkListener(this);
        }
    }

    public ItemStack writeScriptVariable(boolean generateId, ItemStack itemStack, final int disk, final Path path, String member) {
        IVariableFacadeHandlerRegistry registry = IntegratedDynamics._instance.getRegistryManager().getRegistry(IVariableFacadeHandlerRegistry.class);
        return registry.writeVariableFacadeItem(generateId, itemStack, ScriptVariableFacadeHandler.getInstance(), new IVariableFacadeHandlerRegistry.IVariableFacadeFactory<IScriptVariableFacade>() {
            @Override
            public IScriptVariableFacade create(boolean generateId) {
                return new ScriptVariableFacade(generateId, disk, path, member);
            }

            @Override
            public IScriptVariableFacade create(int id) {
                return new ScriptVariableFacade(id, disk, path, member);
            }
        }, getLevel(), player, RegistryEntries.BLOCK_PART_TERMINAL_SCRIPTING.defaultBlockState());
    }

    @Override
    public void setValue(int valueId, CompoundTag value) {
        super.setValue(valueId, value);

        // Potentially trigger a variable write when a new member was selected
        if (valueId == selectionId) {
            this.onDirty();
        }
    }

    public static class InitData {

        private final IntList availableDisks;

        public InitData(IntList availableDisks) {
            this.availableDisks = availableDisks;
        }

        public IntList getAvailableDisks() {
            return availableDisks;
        }

        public void writeToPacketBuffer(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeIntIdList(getAvailableDisks());
        }

        public static InitData readFromPacketBuffer(FriendlyByteBuf packetBuffer) {
            return new InitData(packetBuffer.readIntIdList());
        }

    }

}
