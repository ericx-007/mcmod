"""Validate all loader release jars, bilingual text, recipes and transparent sprites."""
from pathlib import Path
import io
import json
import re
import zipfile
from PIL import Image

root = Path(__file__).resolve().parents[1]
for project, loader, recipe_dir, jar_prefix in [
    (root, "fabric", "recipes", "thirteen-blade"),
    (root / "neoforge-1.21.1", "neoforge", "recipe", "thirteen-blade-neoforge-1.21.1"),
    (root / "forge-1.20.1", "forge", "recipes", "thirteen-blade-forge-1.20.1"),
    (root / "forge-1.12.2", "forge-legacy", "recipes", "thirteen-blade-forge-1.12.2"),
]:
    resources = project / "src/main/resources"
    for path in resources.rglob("*.json"):
        json.loads(path.read_text(encoding="utf-8"))
    legacy = loader == "forge-legacy"
    if legacy:
        languages = {name: dict(line.split("=", 1) for line in (resources / f"assets/thirteenblade/lang/{name}.lang").read_text(encoding="utf-8").splitlines()
                               if line and not line.startswith("#")) for name in ["zh_cn", "en_us"]}
    else:
        languages = {name: json.loads((resources / f"assets/thirteenblade/lang/{name}.json").read_text(encoding="utf-8")) for name in ["zh_cn", "en_us"]}
    assert languages["zh_cn"].keys() == languages["en_us"].keys(), "Language keys differ"
    for path in (project / "src").rglob("*.java"):
        source = path.read_text(encoding="utf-8")
        for key in re.findall(r'"((?:chat|tooltip|hud|message|power|key|category)\.thirteenblade[^"\s]*)"', source):
            if key == "power.thirteenblade.":
                continue
            assert key in languages["en_us"], f"Untranslated: {key} in {path}"
        if path.name == "SoulPower.java":
            for suffix in re.findall(r'^\s+[A-Z_]+\("\w+", "(\w+)"', source, re.M):
                assert "power.thirteenblade." + suffix in languages["en_us"], suffix
    for key, english in languages["en_us"].items():
        assert english.count("%s") == languages["zh_cn"][key].count("%s"), key
    version = re.search(r"^mod_version=(.+)$", (project / "gradle.properties").read_text(), re.M).group(1).strip()
    jar = project / f"build/libs/{jar_prefix}-{version}.jar"
    with zipfile.ZipFile(jar) as package:
        names = package.namelist()
        for name in ["dev/thirteenblade/ThirteenBlade.class",
                     "dev/thirteenblade/client/SwordChatScreen.class",
                     "dev/thirteenblade/DragonUpgradeRecipe.class", "dev/thirteenblade/SoulPower.class",
                     "dev/thirteenblade/DragonFlight.class", "dev/thirteenblade/BladeEnchantments.class"]:
            assert name in names, f"Missing {name}"
        assert not any("GameTests" in name or "ChatTransportTest" in name or "LegacyIntegration" in name or name.endswith("empty.nbt") for name in names), "Tests must not ship"
        if legacy:
            assert "dev/thirteenblade/client/ClientProxy.class" in names
            assert "dev/thirteenblade/FirstAidBridge.class" in names
            assert all(int.from_bytes(package.read(n)[6:8], "big") <= 52 for n in names if n.endswith(".class")), "Legacy classes must run on Java 8"
        else:
            assert "dev/thirteenblade/client/ThirteenBladeClient.class" in names
            assert "thirteenblade.mixins.json" in names
        assert any("LICENSE" in name for name in names), "Missing MIT license"
        for item in ["thirteen_blade", "dragon_thirteen_blade"]:
            assert f"assets/thirteenblade/models/item/{item}.json" in names
            with Image.open(io.BytesIO(package.read(f"assets/thirteenblade/textures/item/{item}.png"))) as sprite:
                assert sprite.mode == "RGBA" and sprite.width == sprite.height
                assert sprite.getchannel("A").getextrema() == (0, 255), "Missing real transparency"
                assert sprite.size == ((32, 32) if item == "thirteen_blade" else (64, 64))
            assert (resources / f"assets/thirteenblade/textures/item/{item}.png").read_bytes() == (root / f"src/main/resources/assets/thirteenblade/textures/item/{item}.png").read_bytes()
        recipe_root = "assets" if legacy else "data"
        base = json.loads(package.read(f"{recipe_root}/thirteenblade/{recipe_dir}/thirteen_blade.json"))
        assert any(value.get("item") == ("minecraft:diamond_sword" if legacy else "minecraft:netherite_sword") for value in base["key"].values())
        if legacy:
            assert any(value.get("item") == "minecraft:nether_star" for value in base["key"].values())
        else:
            upgrade = json.loads(package.read(f"data/thirteenblade/{recipe_dir}/dragon_thirteen_blade.json"))
            assert upgrade["type"] == "thirteenblade:dragon_upgrade"
        if loader == "fabric":
            metadata = json.loads(package.read("fabric.mod.json"))
            assert metadata["version"] == version
            mixins = json.loads(package.read("thirteenblade.mixins.json"))
            assert mixins["refmap"] in names, "Missing Fabric remap metadata"
        elif legacy:
            metadata = json.loads(package.read("mcmod.info"))[0]
            assert metadata["version"] == version and metadata["mcversion"] == "1.12.2"
            assert "MIT" in metadata["credits"]
        else:
            metadata = package.read("META-INF/mods.toml" if loader == "forge" else "META-INF/neoforge.mods.toml").decode()
            assert f'version="{version}"' in metadata and 'license="MIT"' in metadata
            assert "fabric.mod.json" not in names
            if loader == "forge":
                mixins = json.loads(package.read("thirteenblade.mixins.json"))
                assert mixins["refmap"] in names and json.loads(package.read(mixins["refmap"]))["mappings"], "Forge refmap must contain remapped targets"
                assert json.loads(package.read("pack.mcmeta"))["pack"]["pack_format"] == 15
        for language in languages:
            reference = json.loads((root / f"src/main/resources/assets/thirteenblade/lang/{language}.json").read_text(encoding="utf-8"))
            if legacy:
                for item in ["thirteen_blade", "dragon_thirteen_blade"]:
                    reference[f"item.thirteenblade.{item}.name"] = reference.pop(f"item.thirteenblade.{item}")
                reference = {key: value.replace("\n", " / ") for key, value in reference.items()}
            assert languages[language] == reference
    print(f"{loader}: {len(languages['en_us'])} bilingual keys, two sprites, recipes, license and release jar OK: {jar.name}")
