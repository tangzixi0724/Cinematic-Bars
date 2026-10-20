# Cinematic Bars

Black letterbox bars, whenever you want them. Press a key and the picture squeezes into a
cinematic frame; let go and it opens back up. Everything is client-side, so it works in singleplayer,
on servers, with a shader pack, or with no shaders at all.

## What it does

Cinematic Bars draws two black bars over the top and bottom of the screen, with a soft feathered edge
that slides in together with the bar. The motion is a real transition, not a snap: 450 ms by default,
easing out so it starts quickly and settles gently.

The effect itself is nothing new — it is the "concentration" look from cinematic shader packs, where
sneaking narrows the frame. This mod pulls that out into a standalone effect you can trigger with any
key, on any setup.

## Features

- **Hold to show** — hold Left Shift (or any key you pick) and the bars slide in; release and they
  retract.
- **Toggle instead** — pick *toggle* in the settings and one press shows the bars, the next hides
  them.
- **Pin them** — keep the bars on screen permanently with no key at all. The trigger settings grey
  out while pinning is on, because nothing is left for them to do.
- **Nine easing curves** — linear, smoothstep, smootherstep, ease-in quad, ease-in-out, and four
  ease-out variants (quad, cubic, quartic, expo). Default is ease out quad.
- **Adjustable geometry** — bar height from 0 to 25 % of the screen per side, and edge feather from
  0 to 100 % of the bar height. At 0 % feather you get the hard, aliased edge of a plain letterbox.
- **F1 friendly** — the bars keep drawing while the HUD is hidden, so recordings stay clean. On by
  default; turn it off if you want the bars to disappear with the rest of the HUD.
- **Rebindable** — click the key row, press the key or mouse button you want, and it is saved to the
  config file. Backspace clears the binding.

## Settings

Everything is edited in-game through a [YACL](https://modrinth.com/mod/yacl) screen, opened from
Mod Menu or from the mod list.

| Option | Default | Notes |
|---|---|---|
| Bar height | 8.0 % | Per side, 0–25 % of the screen height |
| Edge feather | 22 % | Fraction of the bar height, 0–100 % |
| Transition time | 450 ms | Full open or close, 0–2000 ms |
| Easing curve | Ease Out Quad | Nine curves, laid out as a clickable grid |
| Pin the bars | Off | Always visible, no key needed |
| Activation mode | Hold | Or toggle; disabled while pinned |
| Activation key | Left Shift | Any key or mouse button |
| Show when HUD is hidden | On | Keeps the effect during F1 recordings |

Settings are stored in `config/cinematic-bars.json` and can also be edited by hand:

```json
{
  "barHeightPercent": 8.0,
  "edgeFeatherPercent": 22.0,
  "animationDurationMs": 450,
  "curve": "ease_out_quad",
  "pinned": false,
  "activationMode": "hold",
  "activationKey": "key.keyboard.left.shift",
  "showWhenHudHidden": true
}
```

## Good to know

- **The bars sit under your hand and under the HUD.** They cover the world, not your held item, not
  the hotbar. That is deliberate: the bars are painted into the world image just before the first
  person hand is drawn, so first-person gameplay stays fully readable.
- **The activation key is managed by this mod.** The binding is stored in the config file and mirrored
  onto the game's key mapping, so change it from this mod's screen rather than from Minecraft's own
  Controls screen.
- **Client-side only.** Nothing is installed on the server, and no server-side behaviour depends on
  the mod.

## Motion details

Two things make the slide feel right, for anyone who cares about the implementation:

- The tween is **time-anchored** rather than an incremental step toward the target. Incremental
  easing approaches its target asymptotically, so the last pixels crawl for hundreds of milliseconds
  and then pop into place.
- The slide offset stays **fractional**. Rounding it to whole pixels quantises the motion: with an
  ease-out curve the per-frame movement drops below one pixel before the panel arrives, so nothing
  moves, nothing moves, and then it snaps the remaining pixels in one frame.

## Requirements

| | |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.0 or newer |
| Fabric API | required |
| YACL (Yet Another Config Lib) | required, 3.9.0 or newer — provides the settings screen |
| Mod Menu | optional, adds the settings button |

## Installation

1. Install Fabric Loader for Minecraft 26.3 and put Fabric API in `mods/`.
2. Put YACL in `mods/`.
3. Drop `cinematic-bars-1.2.0+26.3.jar` into `mods/`.
4. Optionally add Mod Menu to reach the settings from the mod list.
5. Launch, hold Left Shift, and rebind the key if Shift clashes with something you already use.

## Credits

The effect is modelled on the `CONCENTRATION` pass of the **AstraLex** shader pack — the black bands
that close in while sneaking. Final resting geometry matches a 1920×1080 reference measurement of
87 px ≈ 8.06 % per side, which is where the default bar height comes from.

Licensed under the MIT license.

---

# 中文说明

给画面加一层电影感的挡条。按住按键，上下黑边顺滑地推入画面；松开手，画面重新展开。纯客户端模组，
单人、服务器、带光影或不带光影都能用。

## 功能

- **按住显示**：默认按住左 Shift（可改成任意按键或鼠标键），挡条滑入；松手即收回。
- **切换模式**：在设置里改成「切换」，按一下显示、再按一下收起。
- **固定挡条**：无需按键，挡条常驻画面；开启后「启用方式」自动变灰失效。
- **九种缓动曲线**：线性、平滑阶梯、更平滑阶梯、二次缓入、缓入缓出，以及二次/三次/四次/指数缓出。
  默认二次缓出，起步快、收尾稳。
- **尺寸可调**：单侧挡条高度 0–25 % 屏幕高度，边缘羽化 0–100 % 挡条高度；羽化为 0 时是硬边的纯遮幅。
- **F1 录制友好**：默认在隐藏 HUD 时依然显示挡条，录素材不怕黑边突然消失。
- **按键可改**：点一下按键行，按你想用的键即可，绑定写进配置文件；退格键清除绑定。

## 设置项与默认值

| 选项 | 默认 | 说明 |
|---|---|---|
| 挡条高度 | 8.0 % | 单侧，占屏幕高度 0–25 % |
| 边缘羽化 | 22 % | 占挡条高度 0–100 % |
| 变化时长 | 450 ms | 一次完全展开或收起，0–2000 ms |
| 变化曲线 | 二次缓出 | 九种曲线，点选式九宫格 |
| 固定挡条 | 关 | 常驻显示，无需按键 |
| 启用方式 | 按住 | 或「切换」；固定挡条时失效 |
| 激活按键 | 左 Shift | 支持键盘与鼠标键 |
| 隐藏 HUD 时仍然显示 | 开 | F1 录制时保留挡条 |

配置保存在 `config/cinematic-bars.json`，也可以手动编辑。

## 需要注意

- **挡条在手部与 HUD 之下**：它只盖住世界，不遮挡手持物品、不遮挡物品栏与快捷栏，第一人称游戏
  信息始终可读。
- **激活按键由本模组管理**：绑定存在配置文件里并同步到游戏的按键映射，请在本模组的设置界面里修改，
  而不是原版「按键设置」。
- **纯客户端**：服务端不需要安装，也不依赖服务端行为。

## 前置要求

Minecraft 26.3、Fabric Loader 0.19.0+、Fabric API、YACL 3.9.0+（提供设置界面），Mod Menu 可选。

## 致谢

效果参考 **AstraLex** 光影包的 `CONCENTRATION` 通道（潜行时收拢的黑边）。静止状态下的挡条高度
取自 1920×1080 参考画面的实测值 87 px ≈ 8.06 %（单侧），即默认的 8 %。

以 MIT 协议开源。
