package org.cyclops.integratedscripting.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integratedscripting.Reference;

import java.util.List;

/**
 * Item for storing scripts.
 * @author rubensworks
 */
public class ItemScriptingDisk extends Item {

    private static String NBT_KEY_ID = "integratedscripting:disk-key";

    public ItemScriptingDisk(Properties properties) {
        super(properties);
    }

    public static int generateScriptingId() {
        return IntegratedDynamics.globalCounters.getNext(Reference.MOD_ID  + ":scripting-ids");
    }

    public int getDiskId(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ItemScriptingDisk &&
                itemStack.hasTag() && itemStack.getTag().contains(NBT_KEY_ID, Tag.TAG_INT)) {
            return itemStack.getTag().getInt(NBT_KEY_ID);
        }
        return -1;
    }

    public int getOrCreateDiskId(ItemStack itemStack) {
        int id = getDiskId(itemStack);

        // Initialize an ID if none has been set yet
        if (id < 0) {
            id = generateScriptingId();
            itemStack.getOrCreateTag().putInt(NBT_KEY_ID, id);
        }

        return id;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack itemStack, Level world, List<Component> list, TooltipFlag flag) {
        int id = getDiskId(itemStack);
        if (id >= 0) {
            list.add(Component.translatable("item.integratedscripting.scripting_disk.id", id)
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
//            int bytes = IntegratedScripting._instance.scriptingData.getScripts(id).values()
//                    .stream().mapToInt(String::length).sum();
//            list.add(Component.translatable("item.integratedscripting.scripting_disk.bytes", bytes)
//                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
        }
        if (id >= 0 && Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative()) {
            list.add(Component.translatable("item.integrateddynamics.variable.warning"));
        }
        super.appendHoverText(itemStack, world, list, flag);
    }

}
