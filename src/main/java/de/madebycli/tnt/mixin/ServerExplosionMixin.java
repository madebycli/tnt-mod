package de.madebycli.tnt.mixin;

import de.madebycli.tnt.EditionExplosionCalculator;
import de.madebycli.tnt.LoadedExplosionView;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private float radius;
    @Shadow @Final private ExplosionDamageCalculator damageCalculator;
    @Unique private LoadedExplosionView tntEditions$view;

    @Unique private boolean tntEditions$isExtended() {
        return radius > 16 && damageCalculator instanceof EditionExplosionCalculator;
    }
    @Unique private LoadedExplosionView tntEditions$view() {
        if (tntEditions$view == null) tntEditions$view = new LoadedExplosionView(level);
        return tntEditions$view;
    }

    @Redirect(method = "calculateExplodedPositions", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState tntEditions$loadedBlock(ServerLevel world, BlockPos pos) {
        return tntEditions$isExtended() ? tntEditions$view().getBlockState(pos) : world.getBlockState(pos);
    }

    @Redirect(method = "calculateExplodedPositions", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState tntEditions$loadedFluid(ServerLevel world, BlockPos pos) {
        return tntEditions$isExtended() ? tntEditions$view().getFluidState(pos) : world.getFluidState(pos);
    }

    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerExplosion;getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F"))
    private float tntEditions$loadedExposure(Vec3 center, Entity entity) {
        return tntEditions$isExtended() ? tntEditions$view().exposure(center, entity) : ServerExplosion.getSeenPercent(center, entity);
    }
}
