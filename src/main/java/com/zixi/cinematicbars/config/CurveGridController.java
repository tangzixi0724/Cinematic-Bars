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
 * Curve picker laid out as a grid of cells, one per easing curve: a single click selects a curve, so
 * the whole set is visible at once instead of hidden behind a cycling button.
 */
public final class CurveGridController implements Controller<CinematicBarsConfig.Curve> {
	private static final int COLUMNS = 3;
	private static final int CELL_HEIGHT = 20;
	private static final int IDLE = 0x40000000;
	private static final int HOVERED = 0x60FFFFFF;
	private static final int SELECTED = 0xFF3B5BDB;
	private static final int SELECTED_HOVERED = 0xFF5C7CFA;
	private static final int TEXT = 0xFFE0E0E0;

	private final Option<CinematicBarsConfig.Curve> option;
	private final Consumer<CinematicBarsConfig.Curve> applier;

	public CurveGridController(Option<CinematicBarsConfig.Curve> option, Consumer<CinematicBarsConfig.Curve> applier) {
		this.option = option;
		this.applier = applier;
	}

	@Override
	public Option<CinematicBarsConfig.Curve> option() {
		return this.option;
	}

	@Override
	public Component formatValue() {
		return Component.translatable("cinematic-bars.curve." + this.option.pendingValue().id());
	}

	@Override
	public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> dimension) {
		int rows = rowsFor(CinematicBarsConfig.Curve.values().length);

		if (dimension instanceof MutableDimension<Integer> mutable) {
			mutable.setHeight(rows * CELL_HEIGHT);
		}

		return new CurveGridWidget(this, dimension);
	}

	static int rowsFor(int curveCount) {
		return Math.max(1, (curveCount + COLUMNS - 1) / COLUMNS);
	}

	private static final class CurveGridWidget extends AbstractWidget {
		private final CurveGridController controller;
		private boolean focused;

		private CurveGridWidget(CurveGridController controller, Dimension<Integer> dimension) {
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
			int index = this.indexAt(event.x(), event.y());

			if (index < 0) {
				return false;
			}

			CinematicBarsConfig.Curve selected = CinematicBarsConfig.Curve.values()[index];
			this.controller.option.requestSet(selected);
			this.controller.applier.accept(selected);
			this.playDownSound();
			return true;
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			Dimension<Integer> dim = this.getDimension();
			CinematicBarsConfig.Curve[] values = CinematicBarsConfig.Curve.values();
			CinematicBarsConfig.Curve current = this.controller.option.pendingValue();
			int rows = rowsFor(values.length);
			int cellWidth = Math.max(1, dim.width() / COLUMNS);
			int cellHeight = Math.max(1, dim.height() / rows);
			int hovered = this.indexAt(mouseX, mouseY);

			for (int i = 0; i < values.length; i++) {
				int column = i % COLUMNS;
				int row = i / COLUMNS;
				int x0 = dim.x() + column * cellWidth;
				int y0 = dim.y() + row * cellHeight;
				int x1 = column == COLUMNS - 1 ? dim.xLimit() : x0 + cellWidth;
				int y1 = row == rows - 1 ? dim.yLimit() : y0 + cellHeight;
				boolean selected = values[i] == current;
				boolean isHovered = i == hovered;

				graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, cellColour(selected, isHovered));
				graphics.centeredText(this.textRenderer,
						Component.translatable("cinematic-bars.curve." + values[i].id()),
						(x0 + x1) / 2, (y0 + y1) / 2 - 4, selected ? -1 : TEXT);
			}
		}

		private int indexAt(double mouseX, double mouseY) {
			Dimension<Integer> dim = this.getDimension();
			int values = CinematicBarsConfig.Curve.values().length;
			int rows = rowsFor(values);
			int cellWidth = Math.max(1, dim.width() / COLUMNS);
			int cellHeight = Math.max(1, dim.height() / rows);

			if (mouseX < dim.x() || mouseX >= dim.xLimit() || mouseY < dim.y() || mouseY >= dim.yLimit()) {
				return -1;
			}

			int column = (int) Math.floor((mouseX - dim.x()) / cellWidth);
			int row = (int) Math.floor((mouseY - dim.y()) / cellHeight);

			if (column < 0 || column >= COLUMNS || row < 0 || row >= rows) {
				return -1;
			}

			int index = row * COLUMNS + column;
			return index < values ? index : -1;
		}

		private static int cellColour(boolean selected, boolean hovered) {
			if (selected) {
				return hovered ? SELECTED_HOVERED : SELECTED;
			}

			return hovered ? HOVERED : IDLE;
		}
	}
}
