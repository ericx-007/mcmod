"""Package the generated sprite at Minecraft item resolution, retaining its alpha."""
from pathlib import Path
import shutil
import sys
from PIL import Image

root = Path(__file__).resolve().parents[1]
source = Path(sys.argv[1])
art = root / "docs" / "art"
art.mkdir(parents=True, exist_ok=True)
shutil.copy2(source, art / "sword-source.png")
texture = root / "src/main/resources/assets/thirteenblade/textures/item/thirteen_blade.png"
texture.parent.mkdir(parents=True, exist_ok=True)
with Image.open(source) as image:
    image = image.convert("RGBA")
    assert image.getchannel("A").getextrema()[0] == 0, "Sprite must have a transparent background"
    sprite = image.resize((32, 32), Image.Resampling.NEAREST)
    sprite.save(texture)
    sprite.resize((384, 384), Image.Resampling.NEAREST).save(art / "sword-preview.png")
print(texture)
