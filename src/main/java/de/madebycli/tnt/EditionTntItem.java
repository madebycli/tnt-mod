package de.madebycli.tnt;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public final class EditionTntItem extends BlockItem {
    private final int color;

    public EditionTntItem(Block block, int color, Properties properties) {
        super(block, properties);
        this.color = color;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.tnt_editions.edition",
                Component.translatable("color.tnt_editions." + EditionSettings.COLORS.get(color).id()))
                .withColor(EditionSettings.COLORS.get(color).rgb());
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        // Apply values before onPlace: powered placements can ignite immediately.
        return state == null ? null : context.getItemInHand()
                .getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(state);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        var settings = EditionData.fromItem(stack, color);
        tooltip.accept(Component.translatable("tooltip.tnt_editions.strength", settings.strength()).withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.tnt_editions.range", settings.range()).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.tnt_editions.ignite").withStyle(ChatFormatting.GRAY));
    }
}
