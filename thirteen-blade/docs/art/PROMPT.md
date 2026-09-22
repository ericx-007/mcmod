# 贴图生成记录

工具：内置 `image_gen`，全新生成，无上传参考图。生成后保留透明通道，使用 Pillow 最近邻缩放至 32×32；构建本身不依赖 Python。

游戏贴图：`src/main/resources/assets/thirteenblade/textures/item/thirteen_blade.png`。

最终提示词：

> Use case: stylized-concept. Asset type: actual Minecraft inventory sword item texture, ONE isolated sprite with true transparent background (alpha), square canvas. Draw a very simple crisp low-resolution pixel-art magic diamond sword on a 32 by 32 logical pixel grid upscaled with hard nearest-neighbor edges. Standard Minecraft diamond sword silhouette and orientation: short hilt at bottom left, long straight broad blade diagonally up to top right, small crossguard. Keep sword within a square, filling 90 percent of canvas and centered. Terraria-inspired enchanted material: cyan diamond edges, deep teal outlines, violet core running along blade, tiny near-white highlight pixels, purple crossguard, dark navy hilt with one amethyst pixel. Limited palette about 12 colors, chunky square pixels, clear readable silhouette at inventory size. No glow outside the silhouette, no particles, no shadows, no scenery, no UI, no writing, no watermark, no frame, no checkerboard baked into background. Deliver a single clean usable transparent sprite asset, not a mockup.
