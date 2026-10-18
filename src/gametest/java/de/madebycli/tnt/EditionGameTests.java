package de.madebycli.tnt;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import io.netty.channel.embedded.EmbeddedChannel;

public final class EditionGameTests {
    private static <T> void check(GameTestHelper test, T expected, T actual, String description) {
        test.assertValueEqual(actual, expected, description);
    }

    private static ServerPlayer testPlayer(GameTestHelper test, GameType mode) {
        // makeMockServerPlayerInLevel hardcodes CREATIVE even after setGameMode.
        // Use the mode-aware helper and attach the same embedded connection as the in-level helper.
        var player = (ServerPlayer) test.makeMockServerPlayer(mode);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        test.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.getInventory().clearContent();
        return player;
    }

    @GameTest
    public void saveReloadKeepsEditionAndTunerSettings(GameTestHelper test) {
        var settings = new EditionSettings(8, 4, 15);
        var ops = test.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var item = EditionData.createTnt(settings);
        var savedItem = ItemStack.CODEC.encodeStart(ops, item).getOrThrow();
        var reloadedItem = ItemStack.CODEC.parse(ops, savedItem).getOrThrow();
        check(test, settings, EditionData.fromItem(reloadedItem, 15), "Saved item edition");
        var tuner = new ItemStack(TntEditions.TUNER);
        EditionData.saveTuner(tuner, settings);
        var savedTuner = ItemStack.CODEC.encodeStart(ops, tuner).getOrThrow();
        check(test, settings, EditionData.fromItem(ItemStack.CODEC.parse(ops, savedTuner).getOrThrow(), -1), "Saved tuner settings");
        var state = TntEditions.EDITION_BLOCKS.get(15).defaultBlockState()
                .setValue(EditionTntBlock.STRENGTH, 8).setValue(EditionTntBlock.RANGE, 4);
        var savedBlock = BlockState.CODEC.encodeStart(ops, state).getOrThrow();
        check(test, settings, EditionData.fromBlock(BlockState.CODEC.parse(ops, savedBlock).getOrThrow()), "Saved block edition");
        test.succeed();
    }

    @GameTest
    public void itemPlacementMiningKeepsEdition(GameTestHelper test) {
        var settings = new EditionSettings(7, 3, 9);
        var stack = EditionData.createTnt(settings);
        var pos = new BlockPos(2, 2, 2);
        test.setBlock(pos.below(), Blocks.STONE);
        var player = testPlayer(test, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var absolute = test.absolutePos(pos);
        var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atBottomCenterOf(absolute), Direction.UP, absolute.below(), false));
        ((EditionTntItem) stack.getItem()).place(context);
        test.assertBlockProperty(pos, EditionTntBlock.STRENGTH, 7);
        test.assertBlockProperty(pos, EditionTntBlock.RANGE, 3);
        var state = test.getBlockState(pos);
        check(test, settings, EditionData.fromBlock(state), "Placed edition");
        var drops = Block.getDrops(state, test.getLevel(), absolute, null);
        check(test, 1, drops.size(), "Drop count");
        check(test, settings, EditionData.fromItem(drops.getFirst(), 9), "Mined edition");
        test.succeed();
    }

    @GameTest
    public void survivalCraftConsumesExactlyOneTnt(GameTestHelper test) {
        var player = testPlayer(test, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TntEditions.TUNER));
        player.getInventory().setItem(1, new ItemStack(Items.TNT, 3));
        var settings = new EditionSettings(5, 2, 6);
        check(test, EditionCrafting.SUCCESS, EditionCrafting.craft(player, InteractionHand.MAIN_HAND, settings), "Craft result");
        check(test, 2, player.getInventory().getItem(1).getCount(), "Remaining vanilla TNT");
        check(test, settings, EditionData.fromItem(player.getMainHandItem(), -1), "Tuner remembers settings");
        int outputs = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(TntEditions.EDITION_ITEMS.get(6))) {
                outputs += stack.getCount();
                check(test, settings, EditionData.fromItem(stack, 6), "Output parameters");
            }
        }
        check(test, 1, outputs, "Exactly one output");
        check(test, EditionCrafting.COOLDOWN, EditionCrafting.craft(player, InteractionHand.MAIN_HAND, settings), "Duplicate click cooldown");
        test.succeed();
    }

    @GameTest
    public void missingTntAndMissingTunerCannotCreateItems(GameTestHelper test) {
        var player = testPlayer(test, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TntEditions.TUNER));
        check(test, EditionCrafting.NO_TNT, EditionCrafting.craft(player, InteractionHand.MAIN_HAND, EditionSettings.DEFAULT), "Missing resource");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        check(test, EditionCrafting.INVALID, EditionCrafting.craft(player, InteractionHand.MAIN_HAND, EditionSettings.DEFAULT), "Missing tuner");
        test.succeed();
    }

    @GameTest
    public void redstoneIgnitionPreservesValuesAndColor(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        var state = TntEditions.EDITION_BLOCKS.get(8).defaultBlockState()
                .setValue(EditionTntBlock.STRENGTH, 6).setValue(EditionTntBlock.RANGE, 3);
        test.setBlock(pos, state);
        test.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
        var tnt = test.findOneEntity(EntityTypes.TNT);
        check(test, state, tnt.getBlockState(), "Primed block state");
        test.assertBlockPresent(Blocks.AIR, pos);
        test.succeed();
    }

    @GameTest
    public void alreadyPoweredPlacementPreservesValues(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        test.setBlock(pos.below(), Blocks.REDSTONE_BLOCK);
        var player = testPlayer(test, GameType.SURVIVAL);
        var stack = EditionData.createTnt(new EditionSettings(8, 4, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var absolute = test.absolutePos(pos);
        var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atBottomCenterOf(absolute), Direction.UP, absolute.below(), false));
        ((EditionTntItem) stack.getItem()).place(context);
        var primed = test.findOneEntity(EntityTypes.TNT);
        check(test, new EditionSettings(8, 4, 2), EditionData.fromBlock(primed.getBlockState()), "Powered placement settings");
        test.succeed();
    }

    @GameTest
    public void normalTntStaysNormal(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        test.setBlock(pos, Blocks.TNT);
        TntBlock.prime(test.getLevel(), test.absolutePos(pos));
        var tnt = test.findOneEntity(EntityTypes.TNT);
        test.assertTrue(tnt.getBlockState().is(Blocks.TNT), "Vanilla TNT renderer state remains vanilla");
        check(test, 80, tnt.getFuse(), "Vanilla fuse");
        test.succeed();
    }

    @GameTest
    public void chainReactionPreservesEdition(GameTestHelper test) {
        var pos = new BlockPos(4, 2, 4);
        var state = TntEditions.EDITION_BLOCKS.get(11).defaultBlockState()
                .setValue(EditionTntBlock.STRENGTH, 7).setValue(EditionTntBlock.RANGE, 2);
        test.setBlock(pos, state);
        var absolute = test.absolutePos(pos);
        test.getLevel().explode(null, absolute.getX() + 0.5, absolute.getY() + 0.5, absolute.getZ() + 0.5,
                2, Level.ExplosionInteraction.TNT);
        var tnt = test.findOneEntity(EntityTypes.TNT);
        check(test, state, tnt.getBlockState(), "Chain reaction edition");
        test.assertTrue(tnt.getFuse() < 80, "Chain reaction has short fuse");
        test.succeed();
    }

    @GameTest
    public void strengthScalesResistanceButBedrockIsProtected(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        var normal = new ExplosionDamageCalculator();
        var stronger = new EditionExplosionCalculator(new EditionSettings(8, 1, 0), normal);
        var stone = Blocks.STONE.defaultBlockState();
        float before = normal.getBlockExplosionResistance(null, level, pos, stone, stone.getFluidState()).orElseThrow();
        float after = stronger.getBlockExplosionResistance(null, level, pos, stone, stone.getFluidState()).orElseThrow();
        check(test, before / 8, after, "Resistance reduction");
        test.assertTrue(!stronger.shouldBlockExplode(null, level, pos, Blocks.BEDROCK.defaultBlockState(), 10000), "Bedrock is protected");
        test.succeed();
    }

    @GameTest(maxTicks = 30)
    public void customTntActuallyExplodes(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        test.setBlock(pos.below(), Blocks.STONE);
        var state = TntEditions.EDITION_BLOCKS.get(0).defaultBlockState().setValue(EditionTntBlock.STRENGTH, 8);
        var absolute = test.absolutePos(pos);
        EditionTntBlock.primeEdition(test.getLevel(), absolute, state, null, false);
        var tnt = test.findOneEntity(EntityTypes.TNT);
        tnt.setFuse(1);
        test.runAfterDelay(3, () -> {
            test.assertTrue(!tnt.isAlive(), "Fuse completed");
            test.assertBlockPresent(Blocks.AIR, pos.below());
            test.succeed();
        });
    }
}
