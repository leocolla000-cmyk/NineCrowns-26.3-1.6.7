package it.lia.ninecrowns.mixin;

import it.lia.ninecrowns.Recipes;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.server.level.*;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method="slotChangedCraftingGrid", at=@At("TAIL"))
    private static void ninecrowns$result(
        AbstractContainerMenu menu,
        ServerLevel level,
        Player player,
        CraftingContainer grid,
        ResultContainer result,
        RecipeHolder<CraftingRecipe> recipe,
        CallbackInfo ci
    ) {
        if (!(player instanceof ServerPlayer sp)) return;

        // The JSON recipes exist so the patterns are visible in JEI and the recipe book.
        // Actual crafting is still validated here: eight distinct OTHER-player heads,
        // correct registered roster, and only one legendary item of each type.
        if (!Recipes.opShape(grid)) return;

        ItemStack out = Recipes.craft(sp, grid);
        result.setItem(0, out);
        menu.setRemoteSlot(0, out);
        sp.connection.send(new ClientboundContainerSetSlotPacket(
            menu.containerId,
            menu.incrementStateId(),
            0,
            out
        ));
    }
}
