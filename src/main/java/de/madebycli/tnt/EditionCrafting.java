package de.madebycli.tnt;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class EditionCrafting {
    public static final int SUCCESS = 0, NO_TNT = 1, INVALID = 2, COOLDOWN = 3;
    private EditionCrafting() {}

    /** Runs on the server thread. The client cannot supply an output item or item count. */
    public static int craft(ServerPlayer player, InteractionHand hand, EditionSettings settings) {
        var tuner = player.getItemInHand(hand);
        if (!player.isAlive() || player.isSpectator() || !tuner.is(TntEditions.TUNER)) return INVALID;
        if (player.getCooldowns().isOnCooldown(tuner)) return COOLDOWN;
        player.getCooldowns().addCooldown(tuner, 5);
        var inventory = player.getInventory();
        int source = -1;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Items.TNT)) { source = slot; break; }
        }
        if (!player.isCreative() && source < 0) return NO_TNT;
        var output = EditionData.createTnt(settings);
        if (!player.isCreative()) inventory.getItem(source).shrink(1);
        EditionData.saveTuner(tuner, settings);
        inventory.placeItemBackInInventory(output);
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        return SUCCESS;
    }
}
