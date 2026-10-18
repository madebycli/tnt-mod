package de.madebycli.tnt;

import de.madebycli.tnt.client.TunerScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/** Full client → server → inventory check, plus a screenshot of the actual rendered menu. */
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
            context.runOnClient(mc -> {
                var screen = (TunerScreen) mc.gui.screen();
                var sliders = screen.children().stream().filter(AbstractSliderButton.class::isInstance)
                        .map(AbstractSliderButton.class::cast).toList();
                if (sliders.size() != 2) throw new AssertionError("Expected strength and range sliders");
                for (var slider : sliders) {
                    var click = new MouseButtonEvent(slider.getRight() - 4, slider.getY() + 10, new MouseButtonInfo(0, 0));
                    slider.mouseClicked(click, false);
                    slider.mouseReleased(click);
                }
            });
            context.clickScreenButton(Component.translatable("screen.tnt_editions.shuffle").getString());
            context.waitTicks(3);
            context.takeScreenshot("tnt-tuner");
            context.clickScreenButton(Component.translatable("screen.tnt_editions.create").getString());
            world.getServer().waitFor(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    var stack = player.getInventory().getItem(i);
                    if (stack.getItem() instanceof EditionTntItem) {
                        var settings = EditionData.fromItem(stack, TntEditions.colorOf(((EditionTntItem) stack.getItem()).getBlock()));
                        if (settings.strength() != 8 || settings.range() != 4 || stack.getCount() != 1) {
                            throw new AssertionError("Sliders were not applied to the server output: " + settings);
                        }
                        return true;
                    }
                }
                return false;
            });
            world.getServer().runOnServer(server -> {
                var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
                int remaining = 0;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    if (inventory.getItem(i).is(Items.TNT)) remaining += inventory.getItem(i).getCount();
                }
                if (remaining != 2) throw new AssertionError("Expected exactly one consumed TNT, remaining=" + remaining);
            });
        }
    }
}
