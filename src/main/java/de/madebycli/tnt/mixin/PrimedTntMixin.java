package de.madebycli.tnt.mixin;

import de.madebycli.tnt.EditionData;
import de.madebycli.tnt.EditionExplosionCalculator;
import de.madebycli.tnt.EditionTntBlock;
import de.madebycli.tnt.EditionRangeCarrier;
import de.madebycli.tnt.EditionSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin implements EditionRangeCarrier {
    @Shadow private boolean usedPortal;
    @Unique private int tntEditions$range;

    @Override public int tntEditions$getRange() { return tntEditions$range; }
    @Override public void tntEditions$setRange(int range) {
        if (range < 1 || range > EditionSettings.MAX_RANGE) throw new IllegalArgumentException("Invalid TNT range");
        tntEditions$range = range;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tntEditions$saveRange(ValueOutput output, CallbackInfo ci) {
        if (tntEditions$range > 0) output.putInt("tnt_editions_range", tntEditions$range);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tntEditions$loadRange(ValueInput input, CallbackInfo ci) {
        int range = input.getIntOr("tnt_editions_range", 0);
        tntEditions$range = range >= 1 && range <= EditionSettings.MAX_RANGE ? range : 0;
    }

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void tntEditions$explode(CallbackInfo ci) {
        PrimedTnt tnt = (PrimedTnt) (Object) this;
        if (!(tnt.getBlockState().getBlock() instanceof EditionTntBlock)) return;
        ci.cancel();
        if (!(tnt.level() instanceof ServerLevel level) || !level.getGameRules().get(GameRules.TNT_EXPLODES)) return;
        var legacy = EditionData.fromBlock(tnt.getBlockState());
        var settings = new EditionSettings(legacy.strength(),
                tntEditions$range > 0 ? tntEditions$range : legacy.range(), legacy.color());
        var base = usedPortal ? PrimedTnt.USED_PORTAL_DAMAGE_CALCULATOR : new ExplosionDamageCalculator();
        level.explode(tnt, Explosion.getDefaultDamageSource(level, tnt), new EditionExplosionCalculator(settings, base),
                tnt.getX(), tnt.getY(0.0625), tnt.getZ(), settings.explosionPower(), false, Level.ExplosionInteraction.TNT);
    }
}
