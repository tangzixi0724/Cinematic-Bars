package com.zixi.cinematicbars;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for <em>Cinematic Bars</em>, the cinematic letterbox mod.
 */
public class CinematicBars implements ClientModInitializer {
	public static final String MOD_ID = "cinematic-bars";
	public static final Logger LOGGER = LoggerFactory.getLogger("Cinematic Bars");

	@Override
	public void onInitializeClient() {
		CinematicBarsConfig.load();
		CinematicBarsKeybind.register();

		// Drives the hold/toggle state machine; the animation itself is stepped per rendered frame.
		ClientTickEvents.END_CLIENT_TICK.register(client -> CinematicBarsKeybind.tick(client));
	}
}
