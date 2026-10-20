package com.zixi.cinematicbars;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.ARGB;

/**
 * Bar geometry and painting.
 *
 * <p>The bars are always emitted as raw quads into the world image, right before the first person
 * hand, so they cover the world without covering the hand or the HUD.
 */
public final class CinematicBarsRenderer {
	private static final int OPAQUE_BLACK = ARGB.color(255, 0, 0, 0);
	private static final int CLEAR = ARGB.color(0, 0, 0, 0);

	private CinematicBarsRenderer() {
	}

	/**
	 * Pixel geometry for one frame.
	 *
	 * @param slide how far each panel still sits outside the screen, in pixels
	 */
	public record Geometry(int width, int height, int barHeight, int feather, double slide) {
		public static Geometry of(int width, int height, double progress) {
			if (width <= 0 || height <= 0) {
				return null;
			}

			CinematicBarsConfig config = CinematicBarsConfig.get();
			int barHeight = (int) Math.round(height * config.barHeightPercent / 100.0);

			if (barHeight <= 0) {
				return null;
			}

			// Feather is a fraction of the bar height, matching the shader's proportional edge.
			int feather = (int) Math.round(barHeight * config.edgeFeatherPercent / 100.0);
			feather = Math.max(0, Math.min(feather, barHeight));

			double clamped = progress < 0.0 ? 0.0 : Math.min(progress, 1.0);

			// At progress 0 each panel sits just past its screen edge; at progress 1 it is at rest.
			// Kept fractional on purpose: rounding the offset to whole pixels quantises the motion,
			// so the panel cannot move until it has accumulated a full pixel and then snaps.
			double slide = (1.0 - clamped) * (barHeight + feather);

			return new Geometry(width, height, barHeight, feather, slide);
		}

		public int coreHeight() {
			return this.barHeight - this.feather;
		}
	}

	/**
	 * Emits the bars as raw quads.
	 *
	 * <p>Vertex order and per-vertex colours follow the vanilla coloured-rectangle element:
	 * top-left, bottom-left, bottom-right, top-right, with the first two carrying the top colour.
	 */
	public static void emitQuads(VertexConsumer vertices, Geometry geometry) {
		double slide = geometry.slide();
		float width = geometry.width();
		float height = geometry.height();
		int barHeight = geometry.barHeight();
		int feather = geometry.feather();

		// --- top bar ---
		float topShift = (float) -slide;

		if (feather <= 0) {
			quad(vertices, 0.0F, topShift, width, barHeight + topShift, OPAQUE_BLACK, OPAQUE_BLACK);
		} else {
			int coreHeight = geometry.coreHeight();

			if (coreHeight > 0) {
				quad(vertices, 0.0F, topShift, width, coreHeight + topShift, OPAQUE_BLACK, OPAQUE_BLACK);
			}

			quad(vertices, 0.0F, coreHeight + topShift, width, barHeight + topShift, OPAQUE_BLACK, CLEAR);
		}

		// --- bottom bar ---
		float bottomShift = (float) slide;
		int start = geometry.height() - barHeight;

		if (feather <= 0) {
			quad(vertices, 0.0F, start + bottomShift, width, height + bottomShift, OPAQUE_BLACK, OPAQUE_BLACK);
		} else {
			quad(vertices, 0.0F, start + bottomShift, width, start + feather + bottomShift, CLEAR, OPAQUE_BLACK);

			if (start + feather < height) {
				quad(vertices, 0.0F, start + feather + bottomShift, width, height + bottomShift,
						OPAQUE_BLACK, OPAQUE_BLACK);
			}
		}
	}

	private static void quad(VertexConsumer vertices, float x0, float y0, float x1, float y1,
			int topColor, int bottomColor) {
		vertices.addVertex(x0, y0, 0.0F).setColor(topColor);
		vertices.addVertex(x0, y1, 0.0F).setColor(bottomColor);
		vertices.addVertex(x1, y1, 0.0F).setColor(bottomColor);
		vertices.addVertex(x1, y0, 0.0F).setColor(topColor);
	}
}
