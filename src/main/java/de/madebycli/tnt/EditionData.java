package de.madebycli.tnt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class EditionData {
    private EditionData() {}

    public static EditionSettings fromItem(ItemStack stack, int color) {
        var properties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).properties();
        try {
            int strength = Integer.parseInt(properties.getOrDefault("strength", "1"));
            int range = Integer.parseInt(properties.getOrDefault("range", "1"));
            int actualColor = color >= 0 ? color : Integer.parseInt(properties.getOrDefault("edition_color", "0"));
            return new EditionSettings(strength, range, actualColor);
        } catch (IllegalArgumentException ignored) {
            return new EditionSettings(1, 1, Math.max(color, 0));
        }
    }

    public static void saveTuner(ItemStack stack, EditionSettings settings) {
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of(
                "strength", Integer.toString(settings.strength()),
                "range", Integer.toString(settings.range()),
                "edition_color", Integer.toString(settings.color()))));
    }

    public static ItemStack createTnt(EditionSettings settings) {
        ItemStack stack = new ItemStack(TntEditions.EDITION_ITEMS.get(settings.color()));
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
                .with(EditionTntBlock.STRENGTH, settings.strength())
                .with(EditionTntBlock.RANGE, settings.range()));
        return stack;
    }

    public static EditionSettings fromBlock(BlockState state) {
        return new EditionSettings(state.getValue(EditionTntBlock.STRENGTH),
                state.getValue(EditionTntBlock.RANGE), TntEditions.colorOf(state.getBlock()));
    }
}
