package com.zixi.cinematicbars.config;

import com.zixi.cinematicbars.CinematicBarsConfig;
import com.zixi.cinematicbars.CinematicBarsKeybind;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.StateManager;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Settings screen, built with Yet Another Config Lib.
 *
 * <p>One category with labelled groups: the geometry of the bars, their motion, and how they are
 * triggered. Values are written straight into the live config and persisted on close.
 */
public final class CinematicBarsConfigScreen {
	private CinematicBarsConfigScreen() {
	}

	public static Screen create(Screen parent) {
		CinematicBarsConfig config = CinematicBarsConfig.get();

		Option<Double> barHeight = Option.<Double>createBuilder()
				.name(Component.translatable("cinematic-bars.option.bar_height"))
				.description(description("cinematic-bars.option.bar_height"))
				.controller(opt -> DoubleSliderControllerBuilder.create(opt)
						.range(0.0, 25.0)
						.step(0.1)
						.formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.1f%%", value))))
				.stateManager(StateManager.createSimple(8.0, () -> config.barHeightPercent,
						value -> config.barHeightPercent = value))
				.build();

		Option<Double> edgeFeather = Option.<Double>createBuilder()
				.name(Component.translatable("cinematic-bars.option.edge_feather"))
				.description(description("cinematic-bars.option.edge_feather"))
				.controller(opt -> DoubleSliderControllerBuilder.create(opt)
						.range(0.0, 100.0)
						.step(1.0)
						.formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.0f%%", value))))
				.stateManager(StateManager.createSimple(22.0, () -> config.edgeFeatherPercent,
						value -> config.edgeFeatherPercent = value))
				.build();

		Option<Integer> duration = Option.<Integer>createBuilder()
				.name(Component.translatable("cinematic-bars.option.duration"))
				.description(description("cinematic-bars.option.duration"))
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(0, 2000)
						.step(10)
						.formatValue(value -> Component.literal(value + " ms")))
				.stateManager(StateManager.createSimple(450, () -> config.animationDurationMs,
						value -> config.animationDurationMs = value))
				.build();

		Option<CinematicBarsConfig.Curve> curve = Option.<CinematicBarsConfig.Curve>createBuilder()
				.name(Component.translatable("cinematic-bars.option.curve"))
				.description(description("cinematic-bars.option.curve"))
				.customController(opt -> new CurveGridController(opt, selected -> {
					config.curve = selected;
					CinematicBarsConfig.save();
				}))
				.stateManager(StateManager.createSimple(CinematicBarsConfig.Curve.EASE_OUT_QUAD, () -> config.curve,
						value -> config.curve = value))
				.build();

		Option<Boolean> pinned = Option.<Boolean>createBuilder()
				.name(Component.translatable("cinematic-bars.option.pinned"))
				.description(description("cinematic-bars.option.pinned"))
				.controller(TickBoxControllerBuilder::create)
				.stateManager(StateManager.createSimple(Boolean.FALSE, () -> config.pinned,
						value -> config.pinned = value))
				.build();

		Option<CinematicBarsConfig.ActivationMode> activationMode = Option.<CinematicBarsConfig.ActivationMode>createBuilder()
				.name(Component.translatable("cinematic-bars.option.activation_mode"))
				.description(description("cinematic-bars.option.activation_mode"))
				.customController(opt -> new ModeGridController(opt, selected -> config.activationMode = selected))
				.stateManager(StateManager.createSimple(CinematicBarsConfig.ActivationMode.HOLD,
						() -> config.activationMode, value -> config.activationMode = value))
				.build();

		Option<String> activationKey = Option.<String>createBuilder()
				.name(Component.translatable("cinematic-bars.option.activation_key"))
				.description(description("cinematic-bars.option.activation_key"))
				.customController(opt -> new KeyCaptureController(opt, name -> config.activationKey = name))
				.stateManager(StateManager.createSimple(CinematicBarsKeybind.DEFAULT_KEY_NAME,
						() -> config.activationKey, value -> config.activationKey = value))
				.build();

		Option<Boolean> showWhenHudHidden = Option.<Boolean>createBuilder()
				.name(Component.translatable("cinematic-bars.option.show_when_hud_hidden"))
				.description(description("cinematic-bars.option.show_when_hud_hidden"))
				.controller(TickBoxControllerBuilder::create)
				.stateManager(StateManager.createSimple(Boolean.TRUE, () -> config.showWhenHudHidden,
						value -> config.showWhenHudHidden = value))
				.build();

		// Pinning makes the bars always visible, which leaves nothing for the trigger mode to do.
		activationMode.setAvailable(!config.pinned);
		pinned.stateManager().addListener((oldValue, newValue) -> activationMode.setAvailable(!newValue));

		// Leaving toggle mode must drop the latched state, or the bars would jump on the next frame.
		activationMode.stateManager().addListener((oldValue, newValue) -> {
			if (newValue != CinematicBarsConfig.ActivationMode.TOGGLE) {
				CinematicBarsKeybind.resetToggle();
			}
		});

		return YetAnotherConfigLib.createBuilder()
				.title(Component.translatable("cinematic-bars.config.title"))
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("cinematic-bars.category.general"))
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("cinematic-bars.group.shape"))
								.option(barHeight)
								.option(edgeFeather)
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("cinematic-bars.group.motion"))
								.option(duration)
								.option(curve)
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("cinematic-bars.group.control"))
								.option(pinned)
								.option(activationMode)
								.option(activationKey)
								.option(showWhenHudHidden)
								.build())
						.build())
				.save(CinematicBarsConfig::save)
				.build()
				.generateScreen(parent);
	}

	private static OptionDescription description(String key) {
		return OptionDescription.of(Component.translatable(key + ".desc"));
	}
}
