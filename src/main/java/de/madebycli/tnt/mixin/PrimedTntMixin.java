package de.madebycli.tnt.mixin;

import de.madebycli.tnt.EditionData;
import de.madebycli.tnt.EditionExplosionCalculator;
import de.madebycli.tnt.EditionTntBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin {
    @Shadow private boolean usedPortal;

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void tntEditions$explode(CallbackInfo ci) {
        PrimedTnt tnt = (PrimedTnt) (Object) this;
        if (!(tnt.getBlockState().getBlock() instanceof EditionTntBlock)) return;
        ci.cancel();
        if (!(tnt.level() instanceof ServerLevel level) || !level.getGameRules().get(GameRules.TNT_EXPLODES)) return;
        var settings = EditionData.fromBlock(tnt.getBlockState());
        var base = usedPortal ? PrimedTnt.USED_PORTAL_DAMAGE_CALCULATOR : new ExplosionDamageCalculator();
        level.explode(tnt, Explosion.getDefaultDamageSource(level, tnt), new EditionExplosionCalculator(settings, base),
                tnt.getX(), tnt.getY(0.0625), tnt.getZ(), settings.explosionPower(), false, Level.ExplosionInteraction.TNT);
    }
}
