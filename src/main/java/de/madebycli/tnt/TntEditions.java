package de.madebycli.tnt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;

public final class TntEditions implements ModInitializer {
    public static final String ID = "tnt_editions";
    public static final List<EditionTntBlock> EDITION_BLOCKS = new ArrayList<>();
    public static final List<EditionTntItem> EDITION_ITEMS = new ArrayList<>();
    public static TunerItem TUNER;
    public static DataComponentType<Integer> BLAST_RANGE;
    public static BlockEntityType<EditionBlockEntity> EDITION_BLOCK_ENTITY;

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }

    @Override
    public void onInitialize() {
        BLAST_RANGE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("blast_range"),
                DataComponentType.<Integer>builder().persistent(Codec.intRange(1, EditionSettings.MAX_RANGE))
                        .networkSynchronized(ByteBufCodecs.VAR_INT).build());
        for (int color = 0; color < EditionSettings.COLORS.size(); color++) {
            Identifier id = id(EditionSettings.COLORS.get(color).id() + "_tnt");
            var blockKey = ResourceKey.create(Registries.BLOCK, id);
            var itemKey = ResourceKey.create(Registries.ITEM, id);
            var block = new EditionTntBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TNT).setId(blockKey));
            Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
            var item = new EditionTntItem(block, color, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
            Registry.register(BuiltInRegistries.ITEM, itemKey, item);
            EDITION_BLOCKS.add(block);
            EDITION_ITEMS.add(item);
        }
        EDITION_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("edition_tnt"),
                FabricBlockEntityTypeBuilder.create(EditionBlockEntity::new, EDITION_BLOCKS.toArray(Block[]::new)).build());

        var tunerKey = ResourceKey.create(Registries.ITEM, id("tnt_tuner"));
        TUNER = Registry.register(BuiltInRegistries.ITEM, tunerKey,
                new TunerItem(new Item.Properties().setId(tunerKey).stacksTo(1)));

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("editions"), FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.tnt_editions"))
                .icon(() -> EditionData.createTnt(new EditionSettings(3, 2, 6)))
                .displayItems((parameters, output) -> {
                    output.accept(TUNER);
                    for (int color = 0; color < EDITION_ITEMS.size(); color++) {
                        output.accept(EditionData.createTnt(new EditionSettings(1, 1, color)));
                    }
                }).build());
        EditionNetworking.register();
    }

    public static int colorOf(Block block) {
        int index = EDITION_BLOCKS.indexOf(block);
        return index < 0 ? 0 : index;
    }
}
