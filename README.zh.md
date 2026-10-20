# Cinematic Bars

[![最新版本](https://img.shields.io/github/v/release/tangzixi0724/Cinematic-Bars?sort=semver&label=release)](https://github.com/tangzixi0724/Cinematic-Bars/releases/latest)
[![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-62b47a)](https://www.minecraft.net/)
[![Fabric Loader 0.19.0+](https://img.shields.io/badge/Fabric%20Loader-0.19.0%2B-dbbd8b)](https://fabricmc.net/)
[![MIT 协议](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![English](https://img.shields.io/badge/English-README.md-lightgrey)](README.md)

按住一个按键，电影感的上下黑边就会从画面上下缘缓缓收拢；松开手，画面重新展开。这个效果取自
**AstraLex** 光影包的 `CONCENTRATION`（潜行黑边）通道，被重写成了一个独立的 Fabric 模组，所以它跟任何
光影都能共存——或者不带光影也一样用。

显示名与模组 id 都是 **Cinematic Bars** / `cinematic-bars`。早期版本曾以 `flimbar`、`flim-bar` 发布，
配置不迁移：旧的 `config/flimbar.json` 和其中的按键绑定不会再被读取。

## 下载

到 [Releases](https://github.com/tangzixi0724/Cinematic-Bars/releases/latest) 下载 jar，和 Fabric API、
YACL 一起丢进 `mods/` 即可。GitHub 会把文件名里的 `+` 换成点，所以下载下来叫
`cinematic-bars-1.2.0.26.3.jar`；jar 本身没有任何区别，Modrinth 上保留的则是
`cinematic-bars-1.2.0+26.3.jar` 这个原始名字。

## 功能

- **按住或切换**：默认按住左 Shift；把「启用方式」改成*切换*，就是按一下显示、再按一下收起。启用方式
  用两格并排的控件选择，和曲线选择器同一套样式；固定挡条时这一行会变灰失效。
- **激活按键**：默认左 Shift。在设置界面点一下按键行，按下你想用的键盘键或鼠标键即可；退格键清除绑定。
  数值存在 `config/cinematic-bars.json` 的 `activationKey` 里，重启后依然有效。配置文件是唯一事实来源：
  模组每 tick 把它同步到游戏的按键映射，所以请在本模组的设置界面里改，而不是原版的「按键设置」。
- **固定挡条**：无需按键，挡条常驻画面；开启期间「启用方式」被禁用。
- **滑入动画**：默认 450 ms，九种可选曲线，默认是*二次缓出*——起步快、收尾稳。
- **挡条高度**：单侧占屏幕高度的 0–25 %。
- **边缘羽化**：占挡条高度的 0–100 %，作为滑动面板的柔和前沿一起移动。设为 `0` 就是参考录像里那种
  硬边、带锯齿的遮幅。
- **隐藏 HUD 时仍然显示**：默认开启，方便 F1 录制。
- 设置存在 `config/cinematic-bars.json`，通过 YACL 界面编辑（一个分类、三个分组）。

## 挡条画在哪一层

挡条被画进世界画面里，就在第一人称手部绘制之前：在世界之上，手部之下，HUD 与快捷栏之下。这是整个帧里
唯一能做到这个层次关系的位置——手和整个 HUD 都在它之后绘制。

这一份绘制用 GUI 的投影和管线，作为独立的 render pass 提交，因此它和 HUD 共享像素空间，却不必等 HUD
那个 pass。

有两个细节决定了动效是否好看：

- 补间是**时间锚定**的（`CinematicBarsAnimation`），不是“每帧朝目标挪一小步”。增量式缓动只会无限逼近
  目标：最后几个像素会磨上好几百毫秒，然后突然跳到位。
- 滑动偏移保持**分数像素**。取整到整像素会把运动量化：用缓出曲线时，每帧位移在面板到达之前很久就掉到
  一像素以下，于是连续几帧什么都不动，然后一帧内把剩下的像素硬生生跳完。

这两点做到之后不需要额外加“速度下限”：在所有可选曲线上，真正的落点步长都只是零点几像素。

## 效果出处

在 AstraLex 里，`shaders/program/final.glsl` 用 `sneakSmooth` 混合两条黑带：

```glsl
bottomplus = mix(bottom, CONCENTRATION_BAND, sneakSmooth);
topplus    = mix(top,    CONCENTRATION_BAND, sneakSmooth);
float bottombandfade = clamp01((newtex.t - bottomplus) / 0.003);
float topbandfade    = clamp01((1.0 - newtex.t - topplus) / 0.003);
color.rgb = mix(vec3(0.0), color.rgb, bottombandfade * topbandfade);
```

从 1920×1080 的参考录像实测：单侧 **87 px ≈ 8.06 %**，上下对称。这就是默认挡条高度的来历。

关于动画风格的说明：那段录像里挡条是原地改变*覆盖度*，观感像淡入。本模组改成让不透明面板从画外滑入。
静止时的几何完全一致，只是中间过程不同。

## 前置要求

| | |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.0+ |
| Fabric API | 必需 |
| Yet Another Config Lib | 必需，提供设置界面 |
| ModMenu | 可选，提供设置按钮 |

Minecraft 26.x 不再混淆，所以本项目使用不重映射的 `net.fabricmc.fabric-loom` 插件，也不声明
`mappings` 依赖。

## 构建

ModMenu 和 YACL 在这里无法从 Maven 仓库获取，所以两者都放在 `libs/` 里，并以 `compileOnly` /
`localRuntime` 接入。它们都**不会**被打进产物 jar：玩家自己安装 YACL（硬依赖），ModMenu 保持可选。

```bash
./gradlew build
```

产物在 `build/libs/`，名为 `cinematic-bars-1.2.0+26.3.jar`。装进普通启动器配置：把这个 jar 连同
Fabric API、YACL（以及想要设置按钮时的 ModMenu）一起放进 `mods/`。

## 目录结构

```
src/main/java/com/zixi/cinematicbars/
  CinematicBars.java                   客户端入口
  CinematicBarsConfig.java             设置 + JSON 持久化 + 缓动曲线
  CinematicBarsKeybind.java            激活按键、按住/切换锁存
  CinematicBarsAnimation.java          时间锚定补间
  CinematicBarsState.java              每帧决策：何时显示、画在哪层
  CinematicBarsRenderer.java           挡条几何与原始四边形
  CinematicBarsWorldOverlay.java       把这些四边形作为独立 render pass 提交
  mixin/GameRendererMixin.java         帧步进 + 在第一人称手部之前画挡条
  config/CinematicBarsConfigScreen.java  YACL 界面：分类、三个分组、选项接线
  config/CurveGridController.java   曲线选择器，每种曲线一格
  config/ModeGridController.java    按住/切换选择器，每种方式一格
  config/KeyCaptureController.java  按键行：点一下，再按下要绑的键
  config/CinematicBarsModMenu.java       ModMenu 入口
```

## 协议

MIT
