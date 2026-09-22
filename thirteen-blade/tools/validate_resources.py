"""Validate shipped resources and the remapped distribution without launching a client."""
from pathlib import Path
import io
import json
import re
import zipfile
from PIL import Image

root = Path(__file__).resolve().parents[1]
resources = root / "src/main/resources"
for path in resources.rglob("*.json"):
    json.loads(path.read_text(encoding="utf-8"))

languages = {
    name: json.loads((resources / f"assets/thirteenblade/lang/{name}.json").read_text(encoding="utf-8"))
    for name in ["zh_cn", "en_us"]
}
assert languages["zh_cn"].keys() == languages["en_us"].keys(), "Language keys must agree"
for path in (root / "src").rglob("*.java"):
    for key in re.findall(r'"((?:chat|tooltip|hud|message|power|key|category)\.thirteenblade[^"\s]*)"', path.read_text(encoding="utf-8")):
        assert key in languages["en_us"], f"Untranslated key: {key} ({path})"
for key, english in languages["en_us"].items():
    assert english.count("%s") == languages["zh_cn"][key].count("%s"), f"Mismatched placeholders: {key}"

version = re.search(r"^mod_version=(.+)$", (root / "gradle.properties").read_text(), re.M).group(1).strip()
jar = root / f"build/libs/thirteen-blade-{version}.jar"
with zipfile.ZipFile(jar) as package:
    names = package.namelist()
    for name in [
        "fabric.mod.json", "thirteenblade.mixins.json",
        "dev/thirteenblade/ThirteenBlade.class",
        "dev/thirteenblade/client/ThirteenBladeClient.class",
        "dev/thirteenblade/client/SwordChatScreen.class",
        "assets/thirteenblade/models/item/thirteen_blade.json",
        "data/thirteenblade/recipes/thirteen_blade.json",
    ]:
        assert name in names, f"Missing jar entry: {name}"
    assert not any("GameTests" in name or "ChatTransportTest" in name for name in names), "Tests must not ship"
    metadata = json.loads(package.read("fabric.mod.json"))
    assert metadata["version"] == version, "Unexpanded mod version"
    with Image.open(io.BytesIO(package.read("assets/thirteenblade/textures/item/thirteen_blade.png"))) as image:
        assert image.size == (32, 32) and image.mode == "RGBA"
        assert image.getchannel("A").getextrema() == (0, 255), "Sprite must preserve transparency"
    mixins = json.loads(package.read("thirteenblade.mixins.json"))
    assert mixins["refmap"] in names, "Mixin refmap must ship"
print(f"Validated {len(languages['en_us'])} bilingual keys, JSON resources, transparent texture and {jar.name}")
