package com.zixi.cinematicbars.mixin;

import com.zixi.cinematicbars.CinematicBarsState;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Advances the slide and paints the bars into the world image, right before {@code render3dHud}
 * draws the first person hand.
 *
 * <p>That is the only point in the frame where "over the world, under the player's hands" is
 * possible; the HUD pass draws after the hand. Injecting at the head of the method is safe because
 * the method immediately re-establishes its own perspective projection afterwards.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "render3dHud", at = @At("HEAD"))
	private void cinematicBars$drawWorldLayer(CallbackInfo ci) {
		// Exactly one advance per frame, so the renderer below always reads a settled value.
		CinematicBarsState.beginFrame();
		CinematicBarsState.renderWorldLayer();
	}
}
