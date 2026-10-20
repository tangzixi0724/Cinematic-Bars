package com.zixi.cinematicbars;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The activation key for the bars.
 *
 * <p>The binding lives in the config file ({@code activationKey}) and is mirrored into a real
 * {@link KeyMapping} every tick, so the mod always uses what the settings say - the same arrangement
 * the first release used. The key is polled directly rather than through {@link KeyMapping#isDown()},
 * because a mapping only receives key events for the key it was built with.
 */
public final class CinematicBarsKeybind {
	public static final KeyMapping.Category CATEGORY =
			new KeyMapping.Category(Identifier.fromNamespaceAndPath(CinematicBars.MOD_ID, "main"));

	/** Left shift: the binding a fresh config starts with. */
	public static final String DEFAULT_KEY_NAME = "key.keyboard.left.shift";

	private static final KeyMapping MAPPING = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key." + CinematicBars.MOD_ID + ".activation",
					InputConstants.Type.KEYBOARD, InputConstants.KEY_LSHIFT, CATEGORY));

	/** Latched visibility used by {@link CinematicBarsConfig.ActivationMode#TOGGLE}. */
	private static boolean toggled;
	private static boolean lastDown;

	private CinematicBarsKeybind() {
	}

	public static void register() {
		// Registration happens in the static initialiser; touching the class is enough.
	}

	/** The key named by the settings, or left shift if the stored name no longer resolves. */
	public static InputConstants.Key configuredKey() {
		String name = CinematicBarsConfig.get().activationKey;

		try {
			return InputConstants.getKey(name);
		} catch (Exception e) {
			CinematicBars.LOGGER.warn("Unknown activation key \"{}\", falling back to left shift", name);
			return InputConstants.getKey(DEFAULT_KEY_NAME);
		}
	}

	public static boolean isUnbound(InputConstants.Key key) {
		return key == null || key.getValue() == InputConstants.UNKNOWN.getValue();
	}

	/** Localised name of the current binding, for the settings screen. */
	public static Component displayComponent() {
		InputConstants.Key key = configuredKey();

		return isUnbound(key)
				? Component.translatable("cinematic-bars.key.unbound")
				: key.getDisplayName();
	}

	/** True while the configured key or mouse button is held down. */
	public static boolean isConfiguredKeyDown() {
		InputConstants.Key key = configuredKey();

		if (isUnbound(key)) {
			return false;
		}

		if (key.getType() == InputConstants.Type.MOUSE) {
			Minecraft client = Minecraft.getInstance();

			if (client == null || client.mouseHandler == null) {
				return false;
			}

			// Mouse bindings store the GLFW button code: 1 left, 2 middle, 3 right.
			return switch (key.getValue()) {
				case 1 -> client.mouseHandler.isLeftPressed();
				case 2 -> client.mouseHandler.isMiddlePressed();
				case 3 -> client.mouseHandler.isRightPressed();
				default -> false;
			};
		}

		return InputConstants.isKeyDown(key.getValue());
	}

	public static boolean toggled() {
		return toggled;
	}

	/** Drops the latched state, e.g. after switching back to hold-to-show. */
	public static void resetToggle() {
		toggled = false;
		lastDown = false;
	}

	/**
	 * Runs every client tick: keeps the mapping in step with the config, turns a fresh key press into
	 * a toggle flip, and drops the toggle when the activation mode is no longer a toggle.
	 */
	public static void tick(Minecraft client) {
		MAPPING.setKey(configuredKey());

		CinematicBarsConfig config = CinematicBarsConfig.get();

		if (config.pinned || config.activationMode != CinematicBarsConfig.ActivationMode.TOGGLE) {
			resetToggle();
			return;
		}

		// Ignore the key while a menu is open, so the state cannot flip behind the settings screen.
		boolean down = isConfiguredKeyDown() && (client.gui == null || client.gui.screen() == null);

		if (down && !lastDown) {
			toggled = !toggled;
		}

		lastDown = down;
	}
}
