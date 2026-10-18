package de.madebycli.tnt.mixin;

import de.madebycli.tnt.EditionTntBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin {
    @Inject(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private static void tntEditions$prime(Level level, BlockPos pos, LivingEntity owner, CallbackInfoReturnable<Boolean> cir) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof EditionTntBlock) {
            cir.setReturnValue(EditionTntBlock.primeEdition(level, pos, state, owner, false));
        }
    }
}
