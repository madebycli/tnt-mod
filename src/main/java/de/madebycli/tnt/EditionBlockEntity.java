package de.madebycli.tnt;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;

/** Keeps extended ranges out of the block-state Cartesian product; old block states stay valid. */
public final class EditionBlockEntity extends BlockEntity {
    private int blastRange;

    public EditionBlockEntity(BlockPos pos, BlockState state) {
        super(TntEditions.EDITION_BLOCK_ENTITY, pos, state);
        blastRange = state.getValue(EditionTntBlock.RANGE);
    }

    public int blastRange() { return blastRange; }

    public void setBlastRange(int range) {
        if (range < 1 || range > EditionSettings.MAX_RANGE) throw new IllegalArgumentException("Invalid TNT range");
        blastRange = range;
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        blastRange = input.read("blast_range", Codec.intRange(1, EditionSettings.MAX_RANGE))
                .orElse(getBlockState().getValue(EditionTntBlock.RANGE));
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("blast_range", blastRange);
    }

    @Override protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer range = components.get(TntEditions.BLAST_RANGE);
        if (range != null && range >= 1 && range <= EditionSettings.MAX_RANGE) blastRange = range;
    }

    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(TntEditions.BLAST_RANGE, blastRange);
    }
}
