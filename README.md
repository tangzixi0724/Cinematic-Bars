# Cinematic Bars

[![Latest release](https://img.shields.io/github/v/release/tangzixi0724/Cinematic-Bars?sort=semver&label=release)](https://github.com/tangzixi0724/Cinematic-Bars/releases/latest)
[![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-62b47a)](https://www.minecraft.net/)
[![Fabric Loader 0.19.0+](https://img.shields.io/badge/Fabric%20Loader-0.19.0%2B-dbbd8b)](https://fabricmc.net/)
[![License MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![中文说明](https://img.shields.io/badge/%E4%B8%AD%E6%96%87%E8%AF%B4%E6%98%8E-README.zh.md-lightgrey)](README.zh.md)

Hold a key and cinematic letterbox bars close in from the top and bottom of the screen, easing
smoothly. Let go and they retract. The effect is extracted from the **AstraLex** shader pack's
`CONCENTRATION` ("sneak bars") pass and reimplemented as a standalone Fabric mod, so it works with
any shader pack — or with no shaders at all.

Display name and mod id are both **Cinematic Bars** / `cinematic-bars`. Earlier releases shipped as
`flimbar` and `flim-bar`, so settings start from scratch: an old `config/flimbar.json` and its key
binding are not read any more.

## Download

Grab the jar from [Releases](https://github.com/tangzixi0724/Cinematic-Bars/releases/latest) and drop
it into `mods/` next to Fabric API and YACL. GitHub turns the `+` in the file name into a dot, so the
asset arrives as `cinematic-bars-1.2.0.26.3.jar`; the jar inside is identical, and Modrinth keeps the
original `cinematic-bars-1.2.0+26.3.jar` name.

## Features

- **Hold or toggle** — hold Left Shift by default; switch the activation mode to *toggle* to press
  once for on and once for off. The mode is picked with two side-by-side cells, the same style as the
  curve picker, and the mode row greys out while the bars are pinned.
- **Activation key** — left Shift out of the box. Click the key row in the settings screen and press
  the key or mouse button you want; backspace clears it, and the value is stored in
  `config/cinematic-bars.json` (`activationKey`) so it survives a restart. The config file is the source of
  truth: the mod mirrors it into the key mapping every tick, so the binding is managed from this
  mod's screen rather than from Minecraft's own Controls screen.
- **Pin the bars** — keep them on screen permanently, no key needed. Disables the activation mode
  while it is on.
- **Sliding animation**, 450 ms by default, over nine selectable curves. The default is
  *Ease Out Quad*: quick to start, gentle to settle.
- **Bar height** — 0–25 % of the screen height, per side.
- **Edge feather** — 0–100 % of the bar height, riding along as the soft leading edge of the
  sliding panel. `0` gives the hard, aliased edge of the reference recording.
- **Show while HUD is hidden** — on by default, so F1 recordings keep the bars.
- Settings live in `config/cinematic-bars.json`, edited through a YACL screen (one category, three groups).

## Where the bars are drawn

The bars are painted into the world image, right before the first person hand is drawn: above the
world, below the hand, below the HUD and the hotbar. That is the only point in the frame where this
layering is possible, because the hand and then the whole HUD are drawn after it.

The copy is submitted as its own render pass with the GUI projection and pipeline, so it shares the
HUD's pixel space without waiting for the HUD pass.

Two details matter for the motion to look right:

- The tween is **time-anchored** (`CinematicBarsAnimation`), not an incremental "step toward the target
  each frame". Incremental easing approaches the target asymptotically, so the last pixels crawl for
  hundreds of milliseconds and then pop.
- The slide offset stays **fractional**. Rounding it to whole pixels quantises the motion: with
  ease-out the per-frame movement drops below one pixel long before the panel arrives, so nothing
  moves, nothing moves, and then it snaps the remaining pixels in a single frame.

With both of those in place no extra velocity floor is needed: the true landing step is a small
fraction of a pixel on every selectable curve.

## Where the effect comes from

In AstraLex, `shaders/program/final.glsl` blends two black bands in with the `sneakSmooth` uniform:

```glsl
bottomplus = mix(bottom, CONCENTRATION_BAND, sneakSmooth);
topplus    = mix(top,    CONCENTRATION_BAND, sneakSmooth);
float bottombandfade = clamp01((newtex.t - bottomplus) / 0.003);
float topbandfade    = clamp01((1.0 - newtex.t - topplus) / 0.003);
color.rgb = mix(vec3(0.0), color.rgb, bottombandfade * topbandfade);
```

Measured from the reference 1920×1080 recording: **87 px ≈ 8.06 %** per side, symmetric. This is
where the default bar height comes from.

Note on animation style: in that recording the bars ramp their *coverage* in place, which reads as
a fade. This mod instead slides opaque panels in from off-screen. Final resting geometry is
identical; only the in-between motion differs.

## Requirements

| | |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.0+ |
| Fabric API | required |
| Yet Another Config Lib | required, provides the settings screen |
| ModMenu | optional, adds the settings button |

Minecraft 26.x ships unobfuscated, so this project uses the non-remapping
`net.fabricmc.fabric-loom` plugin and declares no `mappings` dependency.

## Building

ModMenu and YACL are not reachable from a Maven repository here, so both are vendored in `libs/`
and wired up as `compileOnly` / `localRuntime`. Neither is **bundled** into the output jar: the
player installs YACL themselves (it is a hard dependency), while ModMenu stays optional.

```bash
./gradlew build
```

The distributable ends up in `build/libs/` as `cinematic-bars-1.2.0+26.3.jar`. Installing into a normal
launcher profile: drop that jar into `mods/` together with Fabric API and YACL (plus ModMenu if you
want the settings button).

## Layout

```
src/main/java/com/zixi/cinematicbars/
  CinematicBars.java                   client entrypoint
  CinematicBarsConfig.java             settings + JSON persistence + easing curves
  CinematicBarsKeybind.java            activation key, hold/toggle latch
  CinematicBarsAnimation.java          time-anchored tween
  CinematicBarsState.java              per-frame decisions: when to show, when to draw
  CinematicBarsRenderer.java           bar geometry and the raw quads
  CinematicBarsWorldOverlay.java       submits those quads as their own render pass
  mixin/GameRendererMixin.java   frame step + bars before the first person hand
  config/CinematicBarsConfigScreen.java  YACL screen: category, three groups, option wiring
  config/CurveGridController.java   curve picker, one cell per curve
  config/ModeGridController.java    hold/toggle picker, one cell per mode
  config/KeyCaptureController.java  key row: click once, then press the key to bind
  config/CinematicBarsModMenu.java       ModMenu entrypoint
```

## License

MIT
