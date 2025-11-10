package space.yahho.mcmod.novaio.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import space.yahho.mcmod.novaio.block.entity.SlimBarrelBlockEntity;
import space.yahho.mcmod.novaio.number.HeavyBigInteger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

import static space.yahho.mcmod.novaio.NovaIO.FINITE_QTY_ITEM;
import static space.yahho.mcmod.novaio.NovaIO.LOGGER;
import static space.yahho.mcmod.novaio.NovaIO.NUMBER_MANAGER;
import static space.yahho.mcmod.novaio.block.entity.SlimBarrelBlockEntity.SLOT_COUNT;
import static space.yahho.mcmod.novaio.item.FiniteQtyItem.*;

public abstract class FiniteQtyStorageHandler implements IItemHandler, INBTSerializable<CompoundTag> {
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    public FiniteQtyStorageHandler() {
    }

    @Override
    public int getSlots() {
        return SLOT_COUNT;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= 9) return ItemStack.EMPTY;
        return this.items.get(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot < 9 || slot >= SLOT_COUNT) return stack;
        if (!stack.isEmpty()) {
            Optional<ItemStack> existing = this.items.stream().filter(s -> !s.isEmpty()).filter((is) -> {
                if (is.is(FINITE_QTY_ITEM.get()) && is.hasTag() && is.getTag().contains(TAG_MIMIC_ITEM)) {
                    ItemStack test = ItemStack.of(is.getTag().getCompound(TAG_MIMIC_ITEM));
                    if (!test.hasTag()) test.setTag(null);
                    return ItemStack.isSameItemSameTags(stack, test);
                } else return false;
            }).findFirst();

            if (existing.isEmpty()) {
                if (this.items.stream().noneMatch(ItemStack::isEmpty)) return stack;
                if (simulate) return ItemStack.EMPTY;

                ItemStack newItem = new ItemStack(FINITE_QTY_ITEM.get(), stack.getCount());
                CompoundTag compoundTag = stack.getOrCreateTag();
                HeavyBigInteger qty = new HeavyBigInteger(BigInteger.valueOf(stack.getCount()));
                compoundTag.put(TAG_MIMIC_ITEM, stack.save(new CompoundTag()));

                boolean registerSuccess;
                UUID uuid;
                do {
                    uuid = UUID.randomUUID();
                    registerSuccess = NUMBER_MANAGER.claimAndStoreNumber(uuid, qty);
                } while (!registerSuccess);
                compoundTag.putString(TAG_CACHED_QTY, qty.toString());
                compoundTag.putUUID(TAG_TRACE_ID, uuid);
                newItem.setTag(compoundTag);

                int i = 0;
                while (i < items.size() && !items.get(i).isEmpty()) i++;
                items.set(i, newItem);

                onChange();
                return ItemStack.EMPTY;
            } else {
                if (simulate) return ItemStack.EMPTY;

                ItemStack updItem = existing.get();
                UUID uuid = updItem.getTag().getUUID(TAG_TRACE_ID);
                HeavyBigInteger qty = NUMBER_MANAGER.getStoredNumber(uuid).orElse(new HeavyBigInteger(BigInteger.ONE));

                qty = qty.add(new HeavyBigInteger(BigInteger.valueOf(stack.getCount())));
                if (!NUMBER_MANAGER.updateStoredNumber(uuid, qty)) NUMBER_MANAGER.claimAndStoreNumber(uuid, qty);
                updItem.getTag().putString(TAG_CACHED_QTY, qty.toString());
                updItem.setCount((qty.compareTo(HeavyBigInteger.MAX_INTEGER) >= 0 ? Integer.MAX_VALUE : qty.intValue()));

                onChange();
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= items.size() || items.get(slot).isEmpty() || amount <= 0) return ItemStack.EMPTY;
        if (!items.get(slot).is(FINITE_QTY_ITEM.get())) {
            if (simulate) {
                ItemStack estimate = items.get(slot).copy();
                return estimate.split(amount);
            }
            ItemStack split = ContainerHelper.removeItem(items, slot, amount);

            onChange();
            return split;
        }

        ItemStack tmpStk = items.get(slot);
        ItemStack targetStack = ItemStack.of(tmpStk.getTag().getCompound(TAG_MIMIC_ITEM));
        if (!targetStack.hasTag()) targetStack.setTag(null);

        HeavyBigInteger availableAmounts = NUMBER_MANAGER.getStoredNumber(tmpStk.getTag().getUUID(TAG_TRACE_ID)).orElse(HeavyBigInteger.ONE);
        HeavyBigInteger amountToTake = new  HeavyBigInteger(BigInteger.valueOf(amount));
        targetStack.setCount((availableAmounts.compareTo(amountToTake) < 0) ? availableAmounts.intValue() : amount);

        if (!simulate) {
            if (availableAmounts.compareTo(amountToTake) == 0) {
                items.set(slot, ItemStack.EMPTY);
                NUMBER_MANAGER.setDisappeared(tmpStk.getTag().getUUID(TAG_TRACE_ID));
            } else {
                HeavyBigInteger subtractedAmount = availableAmounts.subtract(new HeavyBigInteger(BigInteger.valueOf(targetStack.getCount())));
                NUMBER_MANAGER.updateStoredNumber(tmpStk.getTag().getUUID(TAG_TRACE_ID), subtractedAmount);
                tmpStk.getTag().putString(TAG_CACHED_QTY, subtractedAmount.toString());
            }
            onChange();
        }

        return targetStack;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (slot < 9 || slot >= SLOT_COUNT) return false;
        return !stack.isEmpty();
    }

    public abstract void onChange();

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        items.forEach(stack -> {
            // Clamp stack count to Byte limit
            if (stack.getCount() > 127) stack.setCount(127);
            if (stack.is(FINITE_QTY_ITEM.get())) {
                try {
                    NUMBER_MANAGER.saveStoredNumber(stack.getTag().getUUID(TAG_TRACE_ID));
                } catch (IOException e) {
                    LOGGER.error("Could not save Slim Barrel ItemStack", e);
                }
            }
        });
        ContainerHelper.saveAllItems(tag, this.items);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        for (int i = 0; i < 9; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, items);
        items.forEach(stack -> {
            if (stack.is(FINITE_QTY_ITEM.get())) {
                Optional<HeavyBigInteger> actualCount = NUMBER_MANAGER.getStoredNumber(stack.getTag().getUUID(TAG_TRACE_ID));
                if (actualCount.isPresent() && actualCount.get().signum() >= 0) {
                    // write back stack count
                    stack.setCount((actualCount.get().compareTo(HeavyBigInteger.MAX_INTEGER) >= 0) ? Integer.MAX_VALUE : actualCount.get().intValue());
                    stack.getTag().putString(TAG_CACHED_QTY, actualCount.get().toString());
                }
            }
        });
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }
}
