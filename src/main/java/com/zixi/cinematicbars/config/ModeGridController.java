package com.zixi.cinematicbars.config;

import com.zixi.cinematicbars.CinematicBarsConfig;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.api.utils.MutableDimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Activation mode picker: hold and toggle as two side-by-side cells, the same one-click style as the
 * curve grid, so the choice is visible without opening anything.
 *
 * <p>The row is dimmed and inert while the option is unavailable, which is the case while the bars
 * are pinned: nothing is left for the trigger to do.
 */
public final class ModeGridController implements Controller<CinematicBarsConfig.ActivationMode> {
	private static final int COLUMNS = 2;
	private static final int CELL_HEIGHT = 20;
	private static final int IDLE = 0x40000000;
	private static final int HOVERED = 0x60FFFFFF;
	private static final int SELECTED = 0xFF3B5BDB;
	private static final int SELECTED_HOVERED = 0xFF5C7CFA;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int TEXT_DISABLED = 0x80909090;

	private final Option<CinematicBarsConfig.ActivationMode> option;
	private final Consumer<CinematicBarsConfig.ActivationMode> applier;

	public ModeGridController(Option<CinematicBarsConfig.ActivationMode> option,
			Consumer<CinematicBarsConfig.ActivationMode> applier) {
		this.option = option;
		this.applier = applier;
	}

	@Override
	public Option<CinematicBarsConfig.ActivationMode> option() {
		return this.option;
	}

	@Override
	public Component formatValue() {
		return Component.translatable("cinematic-bars.mode." + this.option.pendingValue().id());
	}

	@Override
	public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> dimension) {
		if (dimension instanceof MutableDimension<Integer> mutable) {
			mutable.setHeight(CELL_HEIGHT);
		}

		return new ModeGridWidget(this, dimension);
	}

	private static final class ModeGridWidget extends AbstractWidget {
		private final ModeGridController controller;
		private boolean focused;

		private ModeGridWidget(ModeGridController controller, Dimension<Integer> dimension) {
			super(dimension);
			this.controller = controller;
		}

		@Override
		public void setFocused(boolean focused) {
			this.focused = focused;
		}

		@Override
		public boolean isFocused() {
			return this.focused;
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			if (!this.controller.option.available()) {
				return false;
			}

			int index = this.indexAt(event.x(), event.y());

			if (index < 0) {
				return false;
			}

			CinematicBarsConfig.ActivationMode selected = CinematicBarsConfig.ActivationMode.values()[index];
			this.controller.option.requestSet(selected);
			this.controller.applier.accept(selected);
			this.playDownSound();
			return true;
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			Dimension<Integer> dim = this.getDimension();
			CinematicBarsConfig.ActivationMode[] values = CinematicBarsConfig.ActivationMode.values();
			CinematicBarsConfig.ActivationMode current = this.controller.option.pendingValue();
			boolean enabled = this.controller.option.available();
			int cellWidth = Math.max(1, dim.width() / COLUMNS);
			int cellHeight = Math.max(1, dim.height());
			int hovered = enabled ? this.indexAt(mouseX, mouseY) : -1;

			for (int i = 0; i < values.length; i++) {
				int x0 = dim.x() + i * cellWidth;
				int y0 = dim.y();
				int x1 = i == COLUMNS - 1 ? dim.xLimit() : x0 + cellWidth;
				int y1 = dim.yLimit();
				boolean selected = values[i] == current;

				graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1,
						enabled ? cellColour(selected, i == hovered) : IDLE);

				int textColour = !enabled ? TEXT_DISABLED : selected ? -1 : TEXT;
				graphics.centeredText(this.textRenderer,
						Component.translatable("cinematic-bars.mode." + values[i].id()),
						(x0 + x1) / 2, (y0 + y1) / 2 - 4, textColour);
			}

			// Keep the cells from reading as one solid block.
			graphics.fill(dim.x() + cellWidth, dim.y(), dim.x() + cellWidth + 1, dim.yLimit(), 0x60000000);
		}

		private int indexAt(double mouseX, double mouseY) {
			Dimension<Integer> dim = this.getDimension();

			if (mouseX < dim.x() || mouseX >= dim.xLimit() || mouseY < dim.y() || mouseY >= dim.yLimit()) {
				return -1;
			}

			int cellWidth = Math.max(1, dim.width() / COLUMNS);
			int column = (int) Math.floor((mouseX - dim.x()) / cellWidth);

			return column < 0 || column >= COLUMNS ? -1 : column;
		}

		private static int cellColour(boolean selected, boolean hovered) {
			if (selected) {
				return hovered ? SELECTED_HOVERED : SELECTED;
			}

			return hovered ? HOVERED : IDLE;
		}
	}
}
