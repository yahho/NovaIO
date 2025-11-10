package space.yahho.mcmod.novaio.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.yahho.mcmod.novaio.NovaIO;
import space.yahho.mcmod.novaio.inventory.FiniteQtyStorageHandler;
import space.yahho.mcmod.novaio.number.HeavyBigInteger;

import static space.yahho.mcmod.novaio.NovaIO.*;
import static space.yahho.mcmod.novaio.item.FiniteQtyItem.*;

public class SlimBarrelBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_COUNT = 8192;
    public static final int STACK_SIZE = Integer.MAX_VALUE;
    private final LazyOptional<IItemHandler> lazyStorage;
    public FiniteQtyStorageHandler storage;

    public SlimBarrelBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(NovaIO.SLIM_BARREL_ENTITY.get(), pPos, pBlockState);
        this.storage = new FiniteQtyStorageHandler() {
            public void onChange() {
                SlimBarrelBlockEntity.this.setChanged();
            }
        };
        lazyStorage = LazyOptional.of(() -> this.storage);
    }

    @Override
    public int getMaxStackSize() {
        return STACK_SIZE;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.merge(storage.serializeNBT());
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        storage.deserializeNBT(pTag);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.novaio.slim_barrel");
    }

    @Override
    protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, pContainerId, pInventory, this, 1);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.getItems().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return (pSlot < 9 && pSlot >= 0) ? this.storage.getItems().get(pSlot) : ItemStack.EMPTY;
    }

    public NonNullList<ItemStack> getItems() {
        return this.storage.getItems();
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        return storage.extractItem(pSlot, pAmount, false);
//        //ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), pSlot, pAmount);
//        if (pSlot < 0 || pSlot >= this.getItems().size() || this.getItems().get(pSlot).isEmpty() || pAmount <= 0) return ItemStack.EMPTY;
//        if (!this.getItems().get(pSlot).is(FINITE_QTY_ITEM.get())) return ContainerHelper.removeItem(this.getItems(), pSlot, pAmount);
//
//        ItemStack tmpStk = this.getItems().get(pSlot);
//        ItemStack targetStack = ItemStack.of(tmpStk.getTag().getCompound(TAG_MIMIC_ITEM));
//        HeavyBigInteger availableAmounts = NUMBER_MANAGER.getStoredNumber(tmpStk.getTag().getUUID(TAG_TRACE_ID)).orElse(HeavyBigInteger.ONE);
//        HeavyBigInteger amountToTake = new  HeavyBigInteger(BigInteger.valueOf(pAmount));
//        targetStack.setCount((availableAmounts.compareTo(amountToTake) < 0) ? availableAmounts.intValue() : pAmount);
//        if (availableAmounts.compareTo(amountToTake) == 0) {
//            this.getItems().set(pSlot, ItemStack.EMPTY);
//            NUMBER_MANAGER.setDisappeared(tmpStk.getTag().getUUID(TAG_TRACE_ID));
//        } else {
//            HeavyBigInteger subtractedAmount = availableAmounts.subtract(new HeavyBigInteger(BigInteger.valueOf(targetStack.getCount())));
//            NUMBER_MANAGER.updateStoredNumber(tmpStk.getTag().getUUID(TAG_TRACE_ID), subtractedAmount);
//            tmpStk.getTag().putString(TAG_CACHED_QTY, subtractedAmount.toString());
//        }
//        this.setChanged();
//
//        if (!targetStack.hasTag()) targetStack.setTag(null);
//        return targetStack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        ItemStack stack =  ContainerHelper.takeItem(this.getItems(), pSlot);
        if (!stack.is(FINITE_QTY_ITEM.get())) return stack;

        ItemStack actualStack = ItemStack.of(stack.getTag().getCompound(TAG_MIMIC_ITEM));
        HeavyBigInteger amount = NUMBER_MANAGER.getStoredNumber(stack.getTag().getUUID(TAG_TRACE_ID)).orElse(HeavyBigInteger.ONE);
        actualStack.setCount((amount.compareTo(HeavyBigInteger.MAX_INTEGER) < 0) ? amount.intValue() : Integer.MAX_VALUE);
        if (!actualStack.hasTag()) actualStack.setTag(null);
        return actualStack;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        storage.insertItem(pSlot, pStack, false);
//        if (!pStack.isEmpty()) {
//            Optional<ItemStack> existing = this.getItems().stream().filter(stack -> !stack.isEmpty()).filter((is) -> {
//                if (is.is(FINITE_QTY_ITEM.get()) && is.hasTag() && is.getTag().contains(TAG_MIMIC_ITEM)) {
//                    return ItemStack.isSameItem(pStack, ItemStack.of(is.getTag().getCompound(TAG_MIMIC_ITEM)));
//                } else return false;
//            }).findFirst();
//            if (existing.isEmpty()) {
//                ItemStack newItem = new ItemStack(FINITE_QTY_ITEM.get(), pStack.getCount());
//                CompoundTag compoundTag = pStack.getOrCreateTag();
//                HeavyBigInteger qty = new HeavyBigInteger(BigInteger.valueOf(pStack.getCount()));
//                compoundTag.put(TAG_MIMIC_ITEM, pStack.save(new CompoundTag()));
//
//                boolean registerSuccess;
//                UUID uuid;
//                do {
//                    uuid = UUID.randomUUID();
//                    registerSuccess = NUMBER_MANAGER.claimAndStoreNumber(uuid, qty);
//                } while (!registerSuccess);
//                compoundTag.putString(TAG_CACHED_QTY, qty.toString());
//                compoundTag.putUUID(TAG_TRACE_ID, uuid);
//                newItem.setTag(compoundTag);
//                this.getItems().set(pSlot, newItem);
//            } else {
//                ItemStack updItem = existing.get();
//                UUID uuid = updItem.getTag().getUUID(TAG_TRACE_ID);
//                HeavyBigInteger qty = NUMBER_MANAGER.getStoredNumber(uuid).orElse(new HeavyBigInteger(BigInteger.ONE));
//
//                qty = qty.add(new HeavyBigInteger(BigInteger.valueOf(pStack.getCount())));
//                if (!NUMBER_MANAGER.updateStoredNumber(uuid, qty)) NUMBER_MANAGER.claimAndStoreNumber(uuid, qty);
//                updItem.getTag().putString(TAG_CACHED_QTY, qty.toString());
//                updItem.setCount((qty.compareTo(HeavyBigInteger.MAX_INTEGER) >= 0 ? Integer.MAX_VALUE : qty.intValue()));
//            }
//        }
//        this.setChanged();
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return Container.stillValidBlockEntity(this, pPlayer);
    }

    @Override
    public void clearContent() {
        this.getItems().clear();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,  @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return this.lazyStorage.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyStorage.invalidate();
    }

}
