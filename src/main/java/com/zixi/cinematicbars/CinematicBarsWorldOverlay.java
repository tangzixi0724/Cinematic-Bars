package com.zixi.cinematicbars;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Draws the bars into the world image, immediately before the first person hand.
 *
 * <p>The hand is rendered by {@code GameRenderer#render3dHud}, i.e. after the world but before the
 * HUD, so an ordinary HUD quad always covers it. To let the bars sit <em>behind</em> the hand the
 * mod builds its own tiny quad batch and issues its own render pass at the head of that method.
 *
 * <p>The pass is copied from vanilla's {@code GuiRenderer}: same orthographic projection helper, same
 * {@code RenderPipelines.GUI} pipeline (no depth test, no depth write) and the same dynamic transform.
 * Anything unexpected disables the layer permanently instead of spamming the log every frame.
 */
public final class CinematicBarsWorldOverlay {
	private static final float Z_NEAR = 1000.0F;
	private static final float Z_FAR = 11000.0F;
	private static final int INITIAL_CAPACITY = 1024;

	private final StagedVertexBuffer vertexBuffer =
			new StagedVertexBuffer(() -> "Cinematic Bars letterbox", INITIAL_CAPACITY);
	private final Projection projection = new Projection();
	private final ProjectionMatrixBuffer projectionMatrixBuffer =
			new ProjectionMatrixBuffer("cinematic_bars_letterbox");

	private boolean disabled;

	public void render(double progress) {
		if (this.disabled) {
			return;
		}

		try {
			this.draw(progress);
		} catch (Throwable t) {
			this.disabled = true;

			// Stops retrying a broken pass every frame. Never rethrow: this runs in the middle of
			// frame setup.
			CinematicBars.LOGGER.error("Cinematic Bars: world-space bars disabled after a rendering error", t);
		}
	}

	private void draw(double progress) {
		Minecraft client = Minecraft.getInstance();
		WindowRenderState window = client.gameRenderer.gameRenderState().windowRenderState;
		float guiWidth = (float) window.width / window.guiScale;
		float guiHeight = (float) window.height / window.guiScale;
		CinematicBarsRenderer.Geometry geometry =
				CinematicBarsRenderer.Geometry.of((int) guiWidth, (int) guiHeight, progress);

		if (geometry == null) {
			return;
		}

		this.vertexBuffer.endDraw();

		StagedVertexBuffer.Draw draw = this.vertexBuffer.appendDraw(
				DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS);
		VertexConsumer vertices = this.vertexBuffer.getVertexBuilder(draw);
		CinematicBarsRenderer.emitQuads(vertices, geometry);
		this.vertexBuffer.requestIndexCount(draw);
		this.vertexBuffer.upload();

		StagedVertexBuffer.ExecuteInfo info = this.vertexBuffer.getExecuteInfo(draw);

		if (info == null) {
			this.vertexBuffer.endDraw();
			return;
		}

		this.projection.setupOrtho(Z_NEAR, Z_FAR, guiWidth, guiHeight, true);
		RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(this.projection),
				com.mojang.blaze3d.ProjectionType.ORTHOGRAPHIC);

		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
				.writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -Z_FAR));
		RenderTarget target = client.gameRenderer.mainRenderTarget();

		try (RenderPass pass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(
						() -> "Cinematic Bars letterbox",
						target.getColorTextureView(),
						Optional.empty(),
						target.hasDepth() ? target.getDepthTextureView() : null,
						OptionalDouble.empty())) {
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", dynamicTransforms);
			pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.GUI));
			pass.setVertexBuffer(0, info.vertexBuffer().slice());
			pass.disableScissor();
			pass.setIndexBuffer(info.indexBuffer(), info.indexType());
			pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
		}

		this.vertexBuffer.endDraw();
		this.vertexBuffer.endFrame();
	}
}
