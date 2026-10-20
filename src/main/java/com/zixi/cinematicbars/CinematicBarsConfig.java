package com.zixi.cinematicbars;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serialised settings for the letterbox bars.
 *
 * <p>Plain Gson on purpose: the values are edited through the YACL settings screen, which talks to
 * this object directly, so no reflection-based config framework is involved.
 */
public final class CinematicBarsConfig {
	/** Easing functions offered in the settings screen, in display order. */
	public enum Curve {
		LINEAR("linear"),
		SMOOTHSTEP("smoothstep"),
		SMOOTHERSTEP("smootherstep"),
		EASE_IN_QUAD("ease_in_quad"),
		EASE_IN_OUT("ease_in_out"),
		EASE_OUT_QUAD("ease_out_quad"),
		EASE_OUT_CUBIC("ease_out_cubic"),
		EASE_OUT_QUARTIC("ease_out_quartic"),
		EASE_OUT_EXPO("ease_out_expo");

		private final String id;

		Curve(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}

		/** Maps 0 (fully retracted) .. 1 (at rest) through the easing curve. */
		public double apply(double progress) {
			double t = progress < 0.0 ? 0.0 : Math.min(progress, 1.0);

			return switch (this) {
				case LINEAR -> t;
				case SMOOTHSTEP -> t * t * (3.0 - 2.0 * t);
				case SMOOTHERSTEP -> t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
				case EASE_IN_QUAD -> t * t;
				case EASE_IN_OUT -> t < 0.5 ? 2.0 * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 2.0) / 2.0;
				case EASE_OUT_QUAD -> 1.0 - (1.0 - t) * (1.0 - t);
				case EASE_OUT_CUBIC -> 1.0 - Math.pow(1.0 - t, 3.0);
				case EASE_OUT_QUARTIC -> 1.0 - Math.pow(1.0 - t, 4.0);
				case EASE_OUT_EXPO -> t >= 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
			};
		}

		public static Curve fromId(String id) {
			if (id != null) {
				for (Curve curve : values()) {
					if (curve.id.equals(id)) {
						return curve;
					}
				}
			}

			return EASE_OUT_QUAD;
		}
	}

	/** How holding the activation key behaves. */
	public enum ActivationMode {
		HOLD("hold"),
		TOGGLE("toggle");

		private final String id;

		ActivationMode(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}

		public static ActivationMode fromId(String id) {
			if (id != null) {
				for (ActivationMode mode : values()) {
					if (mode.id.equals(id)) {
						return mode;
					}
				}
			}

			return HOLD;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static CinematicBarsConfig instance;

	// --- appearance ---
	/** Height of each bar, in percent of the screen height. */
	public double barHeightPercent = 8.0;
	/** Soft edge below each bar, in percent of the bar height. */
	public double edgeFeatherPercent = 22.0;

	// --- motion ---
	/** Length of the slide in/out, in milliseconds. */
	public int animationDurationMs = 450;
	/** Easing applied to the slide. */
	public Curve curve = Curve.EASE_OUT_QUAD;

	// --- behaviour ---
	/** Keeps the bars on screen permanently; disables the activation mode. */
	public boolean pinned = false;
	/** Hold-to-show or press-to-toggle. */
	public ActivationMode activationMode = ActivationMode.HOLD;
	/** Name of the activation key, in {@code InputConstants} form, e.g. {@code key.keyboard.left.shift}. */
	public String activationKey = "key.keyboard.left.shift";
	/** Keeps the bars visible while the HUD is hidden with F1. */
	public boolean showWhenHudHidden = true;

	public static CinematicBarsConfig get() {
		if (instance == null) {
			load();
		}

		return instance;
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CinematicBars.MOD_ID + ".json");
	}

	public static void load() {
		Path path = configPath();
		CinematicBarsConfig loaded = null;

		if (Files.exists(path)) {
			try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, CinematicBarsConfig.class);
			} catch (Exception e) {
				CinematicBars.LOGGER.warn("Could not read {}, using defaults", path, e);
			}
		}

		if (loaded == null) {
			loaded = new CinematicBarsConfig();
		}

		loaded.sanitise();
		instance = loaded;
	}

	public static void save() {
		Path path = configPath();

		try {
			Files.createDirectories(path.getParent());

			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(get(), writer);
			}
		} catch (Exception e) {
			CinematicBars.LOGGER.warn("Could not write {}", path, e);
		}
	}

	/** Clamps anything the file (or a stale file from another version) could have broken. */
	public void sanitise() {
		if (Double.isNaN(this.barHeightPercent) || this.barHeightPercent < 0.0) {
			this.barHeightPercent = 0.0;
		} else if (this.barHeightPercent > 25.0) {
			this.barHeightPercent = 25.0;
		}

		if (Double.isNaN(this.edgeFeatherPercent) || this.edgeFeatherPercent < 0.0) {
			this.edgeFeatherPercent = 0.0;
		} else if (this.edgeFeatherPercent > 100.0) {
			this.edgeFeatherPercent = 100.0;
		}

		this.animationDurationMs = Math.max(0, Math.min(this.animationDurationMs, 5000));

		if (this.curve == null) {
			this.curve = Curve.EASE_OUT_QUAD;
		}

		if (this.activationMode == null) {
			this.activationMode = ActivationMode.HOLD;
		}

		if (this.activationKey == null || this.activationKey.isBlank()) {
			this.activationKey = CinematicBarsKeybind.DEFAULT_KEY_NAME;
		}
	}
}
