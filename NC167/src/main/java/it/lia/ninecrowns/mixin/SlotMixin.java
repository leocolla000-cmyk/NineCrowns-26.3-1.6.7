package it.lia.ninecrowns.mixin;

import it.lia.ninecrowns.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Inject(method="mayPickup", at=@At("HEAD"), cancellable=true)
    private void ninecrowns$blockStaleLegendaryResult(
        Player player,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!((Object)this instanceof ResultSlot)) return;
        if (NineCrowns.data == null) return;

        ItemStack stack = ((Slot)(Object)this).getItem();
        if ((stack.is(ModItems.SWORD) || stack.is(ModItems.SPEAR) || stack.is(ModItems.CROWN))
            && NineCrowns.data.isCrafted(stack.getItem())) {
            cir.setReturnValue(false);
        }
    }
}
