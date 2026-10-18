package de.madebycli.tnt;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Chunk lookups here never load or generate terrain. Missing chunks stop explosion rays. */
public final class LoadedExplosionView implements BlockGetter {
    private final ServerLevel level;

    public LoadedExplosionView(ServerLevel level) { this.level = level; }

    @Override public BlockState getBlockState(BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? Blocks.BEDROCK.defaultBlockState() : chunk.getBlockState(pos);
    }
    @Override public FluidState getFluidState(BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? Fluids.EMPTY.defaultFluidState() : chunk.getFluidState(pos);
    }
    @Override public BlockEntity getBlockEntity(BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }
    @Override public int getHeight() { return level.getHeight(); }
    @Override public int getMinY() { return level.getMinY(); }

    /** Vanilla's bounding-box sampling, using non-loading collision queries for entity exposure. */
    public float exposure(Vec3 center, Entity entity) {
        var box = entity.getBoundingBox();
        double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
        double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
        int visible = 0, samples = 0;
        for (double x = 0; x <= 1; x += stepX) {
            for (double y = 0; y <= 1; y += stepY) {
                for (double z = 0; z <= 1; z += stepZ) {
                    var point = new Vec3(box.minX + (box.maxX - box.minX) * x + offsetX,
                            box.minY + (box.maxY - box.minY) * y,
                            box.minZ + (box.maxZ - box.minZ) * z + offsetZ);
                    if (clip(new ClipContext(point, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity))
                            .getType() == HitResult.Type.MISS) visible++;
                    samples++;
                }
            }
        }
        return samples == 0 ? 0 : (float) visible / samples;
    }
}
