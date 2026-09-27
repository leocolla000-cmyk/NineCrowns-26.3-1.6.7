package it.lia.ninecrowns;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingInput;

public final class Recipes {
    // Original 3x3 legendary layout:
    // H H H
    // H X H
    // H H H
    // H = one of the eight OTHER registered player heads
    // X = Netherite Sword / Netherite Spear / Empty Crown
    private static final int[] HEAD_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    private static final int CENTER_SLOT = 4;

    public static Item resultForCenter(ItemStack stack) {
        if (stack.is(Items.NETHERITE_SWORD)) return ModItems.SWORD;
        if (stack.is(Items.NETHERITE_SPEAR)) return ModItems.SPEAR;
        if (stack.is(ModItems.EMPTY_CROWN)) return ModItems.CROWN;
        return null;
    }

    /** Shape-only check used both for JEI/vanilla recipe enforcement and ingredient consumption. */
    public static boolean opShape(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) return false;
        if (resultForCenter(input.getItem(CENTER_SLOT)) == null) return false;
        for (int slot : HEAD_SLOTS) {
            if (!input.getItem(slot).is(Items.PLAYER_HEAD)) return false;
        }
        return true;
    }

    public static boolean opShape(CraftingContainer grid) {
        if (grid.getWidth() != 3 || grid.getHeight() != 3) return false;
        if (resultForCenter(grid.getItem(CENTER_SLOT)) == null) return false;
        for (int slot : HEAD_SLOTS) {
            if (!grid.getItem(slot).is(Items.PLAYER_HEAD)) return false;
        }
        return true;
    }

    public static ItemStack craft(ServerPlayer player, CraftingContainer grid) {
        if (!opShape(grid)) return ItemStack.EMPTY;

        Item result = resultForCenter(grid.getItem(CENTER_SLOT));
        if (result == null) return ItemStack.EMPTY;

        // Exactly one of each legendary item may ever be crafted in the world.
        if (NineCrowns.data == null || NineCrowns.data.isCrafted(result)) {
            return ItemStack.EMPTY;
        }

        List<UUID> heads = new ArrayList<>(Roster.SIZE - 1);
        for (int slot : HEAD_SLOTS) {
            ItemStack stack = grid.getItem(slot);
            var profile = stack.get(DataComponents.PROFILE);
            if (profile == null) return ItemStack.EMPTY;

            UUID id = profile.partialProfile().id();
            if (id == null) return ItemStack.EMPTY;
            heads.add(id);
        }

        if (!NineCrowns.data.roster.validHeads(player.getUUID(), heads)) {
            return ItemStack.EMPTY;
        }

        return ModItems.enchanted(result, player.registryAccess());
    }
}
