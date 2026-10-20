package com.zixi.cinematicbars;

/**
 * Time-anchored 0..1 tween for the letterbox slide.
 *
 * <p>Deliberately <em>not</em> an incremental "step toward the target each frame" loop. That shape
 * makes the eased value approach the target asymptotically, so the last couple of pixels crawl for
 * hundreds of milliseconds and then pop once a snap threshold is finally crossed — the visible jump
 * this class exists to prevent. Anchoring to an elapsed time with a fixed duration guarantees the
 * curve reaches the target exactly when the duration elapses.
 *
 * <p>Because the renderer keeps the slide offset fractional, the remaining distance at the last
 * frame is all that matters, and for every selectable curve that is a small fraction of a pixel.
 */
public final class CinematicBarsAnimation {
	/** Below this the value counts as settled. */
	private static final double SNAP_THRESHOLD = 1.0e-4;

	private double progress;
	private double startProgress;
	private double target;
	private double durationMillis = 450.0;
	private double elapsedMillis;
	private boolean active;

	public double progress() {
		return this.progress;
	}

	public boolean isActive() {
		return this.active;
	}

	/** Eases toward "shown". */
	public void reveal(double durationMillis) {
		this.tweenTo(1.0, durationMillis);
	}

	/** Eases toward "retracted". */
	public void hide(double durationMillis) {
		this.tweenTo(0.0, durationMillis);
	}

	/** Jumps straight to the target, without animating. */
	public void settle(double target) {
		this.progress = target;
		this.target = target;
		this.elapsedMillis = 0.0;
		this.active = false;
	}

	private void tweenTo(double newTarget, double requestedDuration) {
		double duration = requestedDuration < 0.0 ? 0.0 : requestedDuration;

		if (duration <= 0.0 || Math.abs(newTarget - this.progress) < SNAP_THRESHOLD) {
			this.durationMillis = duration;
			this.settle(newTarget);
			return;
		}

		// Anchoring on the current value keeps reversals mid-flight continuous.
		this.startProgress = this.progress;
		this.target = newTarget;
		this.durationMillis = duration;
		this.elapsedMillis = 0.0;
		this.active = true;
	}

	/**
	 * Advances the tween by one frame.
	 *
	 * @param deltaMillis real milliseconds elapsed since the previous frame
	 */
	public void tick(double deltaMillis) {
		if (!this.active) {
			return;
		}

		if (deltaMillis <= 0.0 || this.durationMillis <= 0.0) {
			this.settle(this.target);
			return;
		}

		this.elapsedMillis += deltaMillis;

		double u = this.elapsedMillis / this.durationMillis;

		if (u >= 1.0) {
			this.settle(this.target);
			return;
		}

		this.progress = this.startProgress + (this.target - this.startProgress)
				* CinematicBarsConfig.get().curve.apply(u);
	}
}
