package com.zixi.cinematicbars.config;

import com.mojang.blaze3d.platform.InputConstants;
import com.zixi.cinematicbars.CinematicBarsKeybind;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Key binding row: a single button that shows the bound key and captures the next key or mouse
 * button pressed while it is waiting.
 *
 * <p>The binding is stored as the {@code InputConstants} name of the key, so the config file stays
 * readable ({@code "key.keyboard.left.shift"}) and an unknown name can fall back to the default.
 */
public final class KeyCaptureController implements Controller<String> {
	private final Option<String> option;
	private final Consumer<String> applier;

	public KeyCaptureController(Option<String> option, Consumer<String> applier) {
		this.option = option;
		this.applier = applier;
	}

	@Override
	public Option<String> option() {
		return this.option;
	}

	@Override
	public Component formatValue() {
		return display(this.option.pendingValue());
	}

	@Override
	public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> dimension) {
		return new KeyCaptureWidget(this, dimension);
	}

	private void assign(InputConstants.Key key) {
		String name = key.getName();
		this.option.requestSet(name);
		this.applier.accept(name);
	}

	/** Localised label for a stored key name. */
	static Component display(String name) {
		InputConstants.Key key = keyOf(name);

		return CinematicBarsKeybind.isUnbound(key)
				? Component.translatable("cinematic-bars.key.unbound")
				: key.getDisplayName();
	}

	/** Resolves a stored name, tolerating a stale or hand-edited value. */
	static InputConstants.Key keyOf(String name) {
		try {
			return InputConstants.getKey(name);
		} catch (Exception e) {
			return InputConstants.UNKNOWN;
		}
	}

	private static final class KeyCaptureWidget extends AbstractWidget {
		private final KeyCaptureController controller;
		private boolean capturing;
		private boolean focused;

		private KeyCaptureWidget(KeyCaptureController controller, Dimension<Integer> dimension) {
			super(dimension);
			this.controller = controller;
		}

		@Override
		public void setFocused(boolean focused) {
			this.focused = focused;

			if (!focused) {
				this.capturing = false;
			}
		}

		@Override
		public boolean isFocused() {
			return this.focused;
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			Dimension<Integer> dim = this.getDimension();
			boolean hovered = this.isMouseOver(mouseX, mouseY);
			this.drawButtonRect(graphics, dim.x(), dim.y(), dim.xLimit(), dim.yLimit(), hovered, true);

			Component label = this.capturing
					? Component.translatable("cinematic-bars.key.press")
					: display(this.controller.option.pendingValue());

			graphics.centeredText(this.textRenderer, label,
					dim.x() + dim.width() / 2, dim.y() + (dim.height() - 8) / 2, -1);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			if (!this.isMouseOver(event.x(), event.y())) {
				return false;
			}

			if (this.capturing) {
				this.controller.assign(InputConstants.Type.MOUSE.getOrCreate(event.button()));
				this.capturing = false;
			} else {
				this.capturing = true;
			}

			this.playDownSound();
			return true;
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (!this.capturing) {
				return false;
			}

			if (event.isEscape()) {
				this.capturing = false;
				return true;
			}

			if (event.key() == InputConstants.KEY_BACKSPACE) {
				this.controller.assign(InputConstants.UNKNOWN);
			} else {
				this.controller.assign(InputConstants.Type.KEYBOARD.getOrCreate(event.key()));
			}

			this.capturing = false;
			return true;
		}

		@Override
		public void unfocus() {
			this.capturing = false;
		}
	}
}
