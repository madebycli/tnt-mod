package de.madebycli.tnt;

import de.madebycli.tnt.client.TunerScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.InputType;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Full menu → network → 100×/100× inventory → placed block/client synchronization check. */
public final class TunerClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("give @a tnt_editions:tnt_tuner");
            world.getServer().runCommand("give @a minecraft:tnt 3");
            context.waitFor(mc -> mc.player != null && mc.player.getMainHandItem().is(TntEditions.TUNER));
            context.runOnClient(mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
            context.waitForScreen(TunerScreen.class);
            for (int row = 0; row < 2; row++) {
                clickRowButton(context, row, "100×");
                clickRowButton(context, row, "+");
                assertSetting(context, row, 100);
                clickRowButton(context, row, "−");
                assertSetting(context, row, 99);
                clickRowButton(context, row, "+");
                assertSetting(context, row, 100);
                clickRowButton(context, row, "1×");
                clickRowButton(context, row, "−");
                assertSetting(context, row, 1);
                clickRowButton(context, row, "25×");
                assertSetting(context, row, 25);
                final int index = row;
                context.runOnClient(mc -> {
                    var slider = slider(mc, index);
                    mc.setLastInputType(InputType.MOUSE);
                    var click = new MouseButtonEvent(slider.getX() + 4, slider.getY() + 10, new MouseButtonInfo(0, 0));
                    mc.gui.screen().mouseClicked(click, false);
                    mc.gui.screen().mouseReleased(click);
                    slider.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0));
                });
                assertSetting(context, row, 2);
                context.runOnClient(mc -> slider(mc, index).keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, GLFW.GLFW_MOD_SHIFT)));
                assertSetting(context, row, 12);
                context.runOnClient(mc -> slider(mc, index).keyPressed(new KeyEvent(GLFW.GLFW_KEY_END, 0, 0)));
                assertSetting(context, row, 100);
            }
            context.clickScreenButton(Component.translatable("screen.tnt_editions.shuffle").getString());
            context.waitTicks(3);
            context.takeScreenshot("tnt-tuner");
            context.clickScreenButton(Component.translatable("screen.tnt_editions.create").getString());
            world.getServer().waitFor(server -> {
                var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    var stack = inventory.getItem(i);
                    if (stack.getItem() instanceof EditionTntItem item) {
                        var settings = EditionData.fromItem(stack, TntEditions.colorOf(item.getBlock()));
                        if (settings.strength() != 100 || settings.range() != 100 || stack.getCount() != 1) {
                            throw new AssertionError("Sliders were not applied to server output: " + settings);
                        }
                        return true;
                    }
                }
                return false;
            });
            var placed = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var inventory = player.getInventory();
                int remaining = 0;
                ItemStack edition = ItemStack.EMPTY;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    var stack = inventory.getItem(i);
                    if (stack.is(Items.TNT)) remaining += stack.getCount();
                    if (stack.getItem() instanceof EditionTntItem) edition = stack.copy();
                }
                if (remaining != 2) throw new AssertionError("Expected one consumed TNT, remaining=" + remaining);
                var level = player.level();
                var pos = player.blockPosition().east(3);
                level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
                player.setItemInHand(InteractionHand.OFF_HAND, edition);
                var placement = new BlockPlaceContext(player, InteractionHand.OFF_HAND, edition,
                        new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false));
                ((EditionTntItem) edition.getItem()).place(placement);
                if (!(level.getBlockEntity(pos) instanceof EditionBlockEntity block) || block.blastRange() != 100) {
                    throw new AssertionError("Placed block lost extended range");
                }
                return pos;
            });
            context.waitFor(mc -> mc.level.getBlockEntity(placed) instanceof EditionBlockEntity block && block.blastRange() == 100);
        }
    }

    private static AbstractSliderButton slider(Minecraft mc, int row) {
        var sliders = mc.gui.screen().children().stream().filter(AbstractSliderButton.class::isInstance)
                .map(AbstractSliderButton.class::cast).toList();
        if (sliders.size() != 2) throw new AssertionError("Expected strength and range sliders");
        return sliders.get(row);
    }

    private static void clickRowButton(ClientGameTestContext context, int row, String label) {
        context.runOnClient(mc -> {
            var slider = slider(mc, row);
            var button = mc.gui.screen().children().stream().filter(Button.class::isInstance)
                    .map(Button.class::cast).filter(b -> b.getMessage().getString().equals(label)
                            && (b.getY() == slider.getY() || b.getY() == slider.getY() + 23))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing button " + label + " in row " + row));
            mc.setLastInputType(InputType.MOUSE);
            var click = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                    button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(0, 0));
            mc.gui.screen().mouseClicked(click, false);
            mc.gui.screen().mouseReleased(click);
        });
    }

    private static void assertSetting(ClientGameTestContext context, int row, int expected) {
        context.runOnClient(mc -> {
            var slider = slider(mc, row);
            String key = row == 0 ? "screen.tnt_editions.strength" : "screen.tnt_editions.range";
            if (!slider.getMessage().getString().equals(Component.translatable(key, expected).getString())) {
                throw new AssertionError("Expected " + expected + " in row " + row + ", actual: " + slider.getMessage().getString());
            }
        });
    }
}
