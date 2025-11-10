package space.yahho.mcmod.novaio.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FiniteQtyItem extends Item {
    public static final String TAG_MIMIC_ITEM = "novaio.mimic_item";
    public static final String TAG_TRACE_ID = "novaio.trace_id";
    public static final String TAG_CACHED_QTY = "novaio.cached_qty";
    public FiniteQtyItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        if (pStack.hasTag() && pStack.getTag().contains(TAG_MIMIC_ITEM)) {
            ItemStack temp = ItemStack.of(pStack.getTag().getCompound(TAG_MIMIC_ITEM));
            return Component.translatable(temp.getDescriptionId());
        }
        return super.getName(pStack);
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        if (pStack.hasTag()) {
            CompoundTag compoundtag = pStack.getTag();
            if (compoundtag != null && compoundtag.contains(TAG_CACHED_QTY)) {
                pTooltipComponents.add(Component.translatable("tooltip.novaio.item_qty", compoundtag.getString(TAG_CACHED_QTY)));
            }
            if (compoundtag != null && compoundtag.contains(TAG_TRACE_ID)) {
                pTooltipComponents.add(Component.literal("UUID: " + compoundtag.getUUID(TAG_TRACE_ID)));
            }
        }
    }
}
