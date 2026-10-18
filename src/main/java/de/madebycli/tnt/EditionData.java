package de.madebycli.tnt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;

import java.util.Map;

public final class EditionData {
    private EditionData() {}

    public static EditionSettings fromItem(ItemStack stack, int color) {
        var properties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).properties();
        try {
            int strength = Integer.parseInt(properties.getOrDefault("strength", "1"));
            int range = stack.getOrDefault(TntEditions.BLAST_RANGE,
                    Integer.parseInt(properties.getOrDefault("range", "1")));
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
        stack.set(TntEditions.BLAST_RANGE, settings.range());
    }

    public static ItemStack createTnt(EditionSettings settings) {
        ItemStack stack = new ItemStack(TntEditions.EDITION_ITEMS.get(settings.color()));
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
                .with(EditionTntBlock.STRENGTH, settings.strength())
                .with(EditionTntBlock.RANGE, Math.min(settings.range(), EditionTntBlock.LEGACY_MAX_RANGE)));
        stack.set(TntEditions.BLAST_RANGE, settings.range());
        return stack;
    }

    public static EditionSettings fromBlock(BlockState state) {
        return new EditionSettings(state.getValue(EditionTntBlock.STRENGTH),
                state.getValue(EditionTntBlock.RANGE), TntEditions.colorOf(state.getBlock()));
    }

    public static EditionSettings fromBlock(BlockGetter level, BlockPos pos, BlockState state) {
        var legacy = fromBlock(state);
        int range = level.getBlockEntity(pos) instanceof EditionBlockEntity edition
                ? edition.blastRange() : legacy.range();
        return new EditionSettings(legacy.strength(), range, legacy.color());
    }
}
