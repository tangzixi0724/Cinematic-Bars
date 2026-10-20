package com.zixi.cinematicbars.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/**
 * ModMenu entrypoint. Only resolved when ModMenu is installed.
 */
public final class CinematicBarsModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return CinematicBarsConfigScreen::create;
	}
}
