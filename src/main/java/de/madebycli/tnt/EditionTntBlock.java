package de.madebycli.tnt;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.function.BiConsumer;

public final class EditionTntBlock extends TntBlock {
    public static final MapCodec<TntBlock> CODEC = simpleCodec(EditionTntBlock::new);
    public static final IntegerProperty STRENGTH = IntegerProperty.create("strength", 1, EditionSettings.MAX_STRENGTH);
    public static final IntegerProperty RANGE = IntegerProperty.create("range", 1, EditionSettings.MAX_RANGE);

    public EditionTntBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(STRENGTH, 1).setValue(RANGE, 1));
    }

    @Override public MapCodec<TntBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STRENGTH, RANGE);
    }

    /** Vanilla's ignition paths all reach TntBlock.prime, intercepted only for our blocks. */
    public static boolean primeEdition(Level level, BlockPos pos, BlockState state, LivingEntity owner, boolean chain) {
        if (!(level instanceof ServerLevel server) || !server.getGameRules().get(GameRules.TNT_EXPLODES)) return false;
        PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, owner);
        tnt.setBlockState(state);
        if (chain) tnt.setFuse(PrimedTnt.getRandomShortFuse(tnt.getFuse(), level.getRandom()));
        if (!level.addFreshEntity(tnt)) return false;
        level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1, 1);
        level.gameEvent(owner, GameEvent.PRIME_FUSE, pos);
        return true;
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> drops) {
        // Capture the state before vanilla removes the block, so a chain reaction keeps its edition.
        super.onExplosionHit(state, level, pos, explosion, drops);
        if (!level.getBlockState(pos).is(this)) {
            primeEdition(level, pos, state, explosion.getIndirectSourceEntity(), true);
        }
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        // onExplosionHit already handles this, using the original state rather than the removed block.
    }
}
