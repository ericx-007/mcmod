# 贴图生成记录

工具：内置 `image_gen`，全新生成，无上传参考图。生成后保留透明通道，使用 Pillow 最近邻缩放至 32×32；构建本身不依赖 Python。

游戏贴图：`src/main/resources/assets/thirteenblade/textures/item/thirteen_blade.png`。

最终提示词：

> Use case: stylized-concept. Asset type: actual Minecraft inventory sword item texture, ONE isolated sprite with true transparent background (alpha), square canvas. Draw a very simple crisp low-resolution pixel-art magic diamond sword on a 32 by 32 logical pixel grid upscaled with hard nearest-neighbor edges. Standard Minecraft diamond sword silhouette and orientation: short hilt at bottom left, long straight broad blade diagonally up to top right, small crossguard. Keep sword within a square, filling 90 percent of canvas and centered. Terraria-inspired enchanted material: cyan diamond edges, deep teal outlines, violet core running along blade, tiny near-white highlight pixels, purple crossguard, dark navy hilt with one amethyst pixel. Limited palette about 12 colors, chunky square pixels, clear readable silhouette at inventory size. No glow outside the silhouette, no particles, no shadows, no scenery, no UI, no writing, no watermark, no frame, no checkerboard baked into background. Deliver a single clean usable transparent sprite asset, not a mockup.


## 0.4.0 龙魂进阶剑

工具：内置 `image_gen`，参考本项目原剑 `docs/art/sword-preview.png` 编辑生成。原图为带真实透明通道的 1254×1254 PNG，保存为 `docs/art/dragon-sword-source.png`。用户明确授权后，以 Pillow 最近邻缩放到 64×64 用于两个加载器的 `assets/thirteenblade/textures/item/dragon_thirteen_blade.png`。没有提取其他游戏资源。

设计提示词要点：保持剑柄左下、剑尖右上的 Minecraft 物品朝向与紫青配色；更宽的下界合金 / 黑曜石剑身、青色刃缘、紫色剑芯、龙翼护手、紫色龙眼宝石与几何尖刺；方形画布、清晰像素轮廓、透明背景、不添加文字和外部光晕。
