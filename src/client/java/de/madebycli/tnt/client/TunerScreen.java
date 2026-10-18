package de.madebycli.tnt.client;

import de.madebycli.tnt.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;

/** Uses Minecraft's extracted GUI rendering only; no OpenGL calls or custom GPU pipelines. */
public final class TunerScreen extends Screen {
    private final InteractionHand hand;
    private int strength, range, color;
    private int left, top, panelWidth;
    private Button createButton;
    private Component status = Component.empty();
    private int statusColor = 0xFF98A6BA;
    private int pendingTicks;

    public TunerScreen(InteractionHand hand, EditionSettings settings) {
        super(Component.translatable("screen.tnt_editions.title"));
        this.hand = hand;
        strength = settings.strength();
        range = settings.range();
        color = settings.color();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(328, width - 16);
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - 232) / 2);
        addRenderableWidget(Button.builder(Component.literal("×"), b -> onClose())
                .bounds(left + panelWidth - 31, top + 8, 22, 20).build());
        addRenderableWidget(new EditionSlider(left + 16, top + 62, panelWidth - 32,
                strength, EditionSettings.MAX_STRENGTH, "screen.tnt_editions.strength", v -> strength = v));
        addRenderableWidget(new EditionSlider(left + 16, top + 102, panelWidth - 32,
                range, EditionSettings.MAX_RANGE, "screen.tnt_editions.range", v -> range = v));
        int half = (panelWidth - 38) / 2;
        var shuffle = addRenderableWidget(Button.builder(Component.translatable("screen.tnt_editions.shuffle"), b -> {
            // A reroll always changes the color, even when the random source repeats an index.
            color = (color + 1 + ThreadLocalRandom.current().nextInt(EditionSettings.COLORS.size() - 1)) % EditionSettings.COLORS.size();
        }).bounds(left + 16, top + 144, half, 20).build());
        shuffle.setTooltip(Tooltip.create(Component.translatable("screen.tnt_editions.shuffle_hint")));
        addRenderableWidget(Button.builder(Component.translatable("screen.tnt_editions.reset"), b -> {
            strength = 1;
            range = 1;
            rebuildWidgets();
        }).bounds(left + 22 + half, top + 144, half, 20).build());
        createButton = addRenderableWidget(Button.builder(Component.translatable("screen.tnt_editions.create"), b -> {
            ClientPlayNetworking.send(new EditionNetworking.CreatePayload(hand, strength, range, color));
            pendingTicks = 60;
            createButton.active = false;
            status = Component.translatable("screen.tnt_editions.waiting");
            statusColor = 0xFF98A6BA;
        }).bounds(left + 16, top + 174, panelWidth - 32, 20).build());
    }

    public void craftResult(int result) {
        pendingTicks = 5;
        String key = switch (result) {
            case EditionCrafting.SUCCESS -> "success";
            case EditionCrafting.NO_TNT -> "no_tnt";
            case EditionCrafting.COOLDOWN -> "cooldown";
            default -> "invalid";
        };
        status = Component.translatable("screen.tnt_editions." + key);
        statusColor = result == EditionCrafting.SUCCESS ? 0xFF83E0AF : 0xFFFFAD87;
    }

    @Override
    public void tick() {
        if (minecraft.player == null || !minecraft.player.getItemInHand(hand).is(TntEditions.TUNER)) {
            onClose();
            return;
        }
        if (pendingTicks > 0 && --pendingTicks == 0) {
            createButton.active = true;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);
        var settings = new EditionSettings(strength, range, color);
        graphics.fill(left, top, left + panelWidth, top + 232, 0xF5141D2C);
        graphics.fill(left, top, left + panelWidth, top + 3, 0xFF000000 | settings.rgb());
        graphics.text(font, title, left + 16, top + 13, 0xFFF1F5FC);
        graphics.fill(left + 16, top + 34, left + panelWidth - 16, top + 55, 0xFF202D40);
        graphics.item(EditionData.createTnt(settings), left + 21, top + 36);
        graphics.text(font, Component.translatable("item.tnt_editions.edition",
                Component.translatable("color.tnt_editions." + settings.colorId())), left + 44, top + 41, 0xFF000000 | settings.rgb());
        graphics.text(font, Component.translatable("screen.tnt_editions.strength_hint"), left + 18, top + 86, 0xFF98A6BA);
        graphics.text(font, Component.translatable("screen.tnt_editions.range_hint"), left + 18, top + 126, 0xFF98A6BA);
        int count = 0;
        if (minecraft.player != null) {
            var inventory = minecraft.player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (inventory.getItem(slot).is(Items.TNT)) count += inventory.getItem(slot).getCount();
            }
        }
        var cost = minecraft.player != null && minecraft.player.isCreative()
                ? Component.translatable("screen.tnt_editions.creative")
                : Component.translatable("screen.tnt_editions.cost", count);
        graphics.text(font, cost, left + 16, top + 201, 0xFF98A6BA);
        graphics.text(font, status, left + 16, top + 216, statusColor);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }

    private static final class EditionSlider extends AbstractSliderButton {
        private final int maximum;
        private final String translation;
        private final IntConsumer consumer;

        EditionSlider(int x, int y, int width, int initial, int maximum, String translation, IntConsumer consumer) {
            super(x, y, width, 20, Component.empty(), (initial - 1.0) / (maximum - 1.0));
            this.maximum = maximum;
            this.translation = translation;
            this.consumer = consumer;
            updateMessage();
        }

        private int selected() { return 1 + (int) Math.round(value * (maximum - 1)); }
        @Override protected void updateMessage() { setMessage(Component.translatable(translation, selected())); }
        @Override protected void applyValue() { consumer.accept(selected()); }
    }
}
