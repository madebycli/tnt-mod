package de.madebycli.tnt;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Optional;

public final class EditionExplosionCalculator extends ExplosionDamageCalculator {
    private final EditionSettings settings;
    private final ExplosionDamageCalculator base;

    public EditionExplosionCalculator(EditionSettings settings, ExplosionDamageCalculator base) {
        this.settings = settings;
        this.base = base;
    }

    @Override
    public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos,
                                                        BlockState state, FluidState fluid) {
        var resistance = base.getBlockExplosionResistance(explosion, level, pos, state, fluid);
        // Never make unbreakable blocks (bedrock, barriers, etc.) destructible.
        if (state.getDestroySpeed(level, pos) < 0) return resistance;
        return resistance.map(value -> value * settings.resistanceMultiplier());
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
        return state.getDestroySpeed(level, pos) >= 0 && base.shouldBlockExplode(explosion, level, pos, state, power);
    }

    @Override public boolean shouldDamageEntity(Explosion explosion, Entity entity) { return base.shouldDamageEntity(explosion, entity); }
    @Override public float getEntityDamageAmount(Explosion explosion, Entity entity, float exposure) {
        return base.getEntityDamageAmount(explosion, entity, exposure) * settings.strength();
    }
    @Override public float getKnockbackMultiplier(Entity entity) {
        return base.getKnockbackMultiplier(entity) * (float) Math.sqrt(settings.strength());
    }
}
