package de.madebycli.tnt;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class TunerItem extends Item {
    public TunerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) {
            var settings = EditionData.fromItem(player.getItemInHand(hand), -1);
            if (!player.getItemInHand(hand).has(DataComponents.BLOCK_STATE)) {
                settings = new EditionSettings(1, 1, player.getRandom().nextInt(EditionSettings.COLORS.size()));
            }
            ServerPlayNetworking.send(serverPlayer, new EditionNetworking.OpenPayload(hand, settings.strength(), settings.range(), settings.color()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : use(context.getLevel(), context.getPlayer(), context.getHand());
    }
}
