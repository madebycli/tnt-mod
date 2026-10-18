package de.madebycli.tnt.client;

import de.madebycli.tnt.EditionNetworking;
import de.madebycli.tnt.EditionSettings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TntEditionsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EditionNetworking.OpenPayload.TYPE, (payload, context) -> {
            if (EditionSettings.isValid(payload.strength(), payload.range(), payload.color())) {
                context.client().gui.setScreen(new TunerScreen(payload.hand(),
                        new EditionSettings(payload.strength(), payload.range(), payload.color())));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(EditionNetworking.ResultPayload.TYPE, (payload, context) -> {
            if (context.client().gui.screen() instanceof TunerScreen screen) screen.craftResult(payload.result());
        });
    }
}
