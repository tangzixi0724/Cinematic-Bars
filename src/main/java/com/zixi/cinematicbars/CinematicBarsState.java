package com.zixi.cinematicbars;

import net.minecraft.client.Minecraft;

/**
 * Per-frame state for the letterbox bars: how far they are out and whether they may be drawn.
 *
 * <p>Progress is advanced exactly once per frame and then read by the hook that paints that frame.
 * The bars are always painted into the world image, right before the first person hand is drawn, so
 * they sit above the world but below the hand and the whole HUD.
 */
public final class CinematicBarsState {
	private static final CinematicBarsAnimation ANIMATION = new CinematicBarsAnimation();

	private static CinematicBarsWorldOverlay worldOverlay;

	/** Last observed target, so the tween only restarts when the intent actually changes. */
	private static Boolean lastHeld;
	private static long lastFrameNanos = -1L;

	private static double frameProgress;
	private static boolean frameWanted;

	private CinematicBarsState() {
	}

	/** What the settings ask for this frame, before the animation is taken into account. */
	private static boolean wantsVisible(CinematicBarsConfig config) {
		if (config.pinned) {
			return true;
		}

		return config.activationMode == CinematicBarsConfig.ActivationMode.TOGGLE
				? CinematicBarsKeybind.toggled()
				: CinematicBarsKeybind.isConfiguredKeyDown();
	}

	public static double progress() {
		return frameProgress;
	}

	public static double rawProgress() {
		return ANIMATION.progress();
	}

	/**
	 * Advances the slide and latches this frame's progress. Must be called exactly once per rendered
	 * frame, before any hook tries to draw.
	 */
	public static void beginFrame() {
		CinematicBarsConfig config = CinematicBarsConfig.get();

		long nowNanos = System.nanoTime();
		double deltaMillis = lastFrameNanos < 0L ? 0.0 : (nowNanos - lastFrameNanos) / 1_000_000.0;
		lastFrameNanos = nowNanos;

		// Guard against a stall (alt-tab, world load) blowing the animation through.
		if (deltaMillis < 0.0) {
			deltaMillis = 0.0;
		} else if (deltaMillis > 250.0) {
			deltaMillis = 250.0;
		}

		boolean wanted = wantsVisible(config);

		if (lastHeld == null || lastHeld != wanted) {
			lastHeld = wanted;

			if (wanted) {
				ANIMATION.reveal(config.animationDurationMs);
			} else {
				ANIMATION.hide(config.animationDurationMs);
			}
		}

		ANIMATION.tick(deltaMillis);

		frameWanted = wanted;
		frameProgress = ANIMATION.progress();
	}

	/** Cheap guard used to skip work when the effect cannot be visible. */
	public static boolean shouldRender() {
		Minecraft client = Minecraft.getInstance();

		if (client.level == null || client.player == null) {
			return false;
		}

		// Never intrude on full-screen menus or the pause screen.
		if (client.gui == null || client.gui.screen() != null) {
			return false;
		}

		CinematicBarsConfig config = CinematicBarsConfig.get();

		if (config.barHeightPercent <= 0.0) {
			return false;
		}

		if (!config.showWhenHudHidden && hudHidden(client)) {
			return false;
		}

		// Hidden and fully retracted: nothing to contribute.
		return frameWanted || ANIMATION.progress() > 0.0;
	}

	private static boolean hudHidden(Minecraft client) {
		return client.gui != null && client.gui.hud.isHidden();
	}

	/** Paints the bars into the world pass, ahead of the hand. */
	public static void renderWorldLayer() {
		if (!shouldRender()) {
			return;
		}

		if (worldOverlay == null) {
			worldOverlay = new CinematicBarsWorldOverlay();
		}

		worldOverlay.render(progress());
	}
}
