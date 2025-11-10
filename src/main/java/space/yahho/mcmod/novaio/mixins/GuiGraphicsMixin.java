package space.yahho.mcmod.novaio.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.math.BigInteger;

import static space.yahho.mcmod.novaio.NovaIO.FINITE_QTY_ITEM;
import static space.yahho.mcmod.novaio.NovaIO.LOGGER;
import static space.yahho.mcmod.novaio.item.FiniteQtyItem.TAG_CACHED_QTY;
import static space.yahho.mcmod.novaio.item.FiniteQtyItem.TAG_MIMIC_ITEM;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyArg(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getModel(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Lnet/minecraft/client/resources/model/BakedModel;"
            ),
            index = 0
    )
    protected ItemStack checkRealModel(ItemStack pStack, @Share("actualItemStack") LocalRef<ItemStack> pActualItemStack) {
        if (pStack.is(FINITE_QTY_ITEM.get()) && pStack.hasTag() && pStack.getTag().contains(TAG_MIMIC_ITEM)) {
            pActualItemStack.set(ItemStack.of(pStack.getTag().getCompound(TAG_MIMIC_ITEM)));
            //LOGGER.debug("Finite Qty item found, so render \"{}\" instead.", pActualItemStack.get().getDisplayName().getString());
            return pActualItemStack.get();
        } else  {
            pActualItemStack.set(pStack);
            return pStack;
        }
    }

    @ModifyArg(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"
            ),
            index = 0
    )
    protected ItemStack renderActualItem(ItemStack pStack, @Share("actualItemStack") LocalRef<ItemStack> pActualItemStack) {
        return pActualItemStack.get();
    }

    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE_ASSIGN",
                            target = "Lnet/minecraft/world/item/ItemStack;getCount()I",
                            ordinal = 0
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
                    )
            )
    )
    protected void renderCorrectItemCount(Font pFont, ItemStack pStack, int pX, int pY, @Nullable String pText, CallbackInfo ci, @Local(name = "s") LocalRef<String> s) {
        if (pStack.is(FINITE_QTY_ITEM.get()) && pStack.hasTag() && pStack.getTag().contains(TAG_CACHED_QTY) && !s.equals(pText)) {
            s.set(pStack.getTag().getString(TAG_CACHED_QTY));
            pStack = ItemStack.of(pStack.getTag().getCompound(TAG_MIMIC_ITEM));
            //LOGGER.debug("The correct amount of \"{}\" is '{}'(might approximated).", pStack.getDisplayName().getString(), s.get());
        }
    }

    @WrapOperation(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I")
    )
    private int evaluateActualAmount(ItemStack pStack, Operation<Integer> original) {
        if (pStack.is(FINITE_QTY_ITEM.get()) && pStack.hasTag() && pStack.getTag().contains(TAG_CACHED_QTY)) {
            String actualApproximatedAmount = pStack.getTag().getString(TAG_CACHED_QTY);
            if (actualApproximatedAmount.contains("0x")) return Integer.MAX_VALUE;
            BigInteger bigInteger = new BigInteger(actualApproximatedAmount);
            return (bigInteger.signum() >= 0) ? ((bigInteger.intValue() < 0) ? Integer.MAX_VALUE : bigInteger.intValue()) : 1;
        }
        return original.call(pStack);
    }
}
