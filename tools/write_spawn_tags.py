"""Write the biome tags that decide where each line spawns and which pelt it favours there.

    python tools/write_spawn_tags.py

Every reference to a tag is optional, so a pack without a tag loses nothing, and any mod that tags
its biomes with the common `c:` tags (c:is_forest, c:is_snowy, c:is_jungle, ...) gets the right
mounts in them. Packs can add biomes by writing to the same tags in a datapack:

    data/shamanicmounts/tags/worldgen/biome/spawns/<line>.json      where the line spawns
    data/shamanicmounts/tags/worldgen/biome/pelts/<line>/<a|b|c>.json  where each pelt is at home
    data/shamanicmounts/tags/worldgen/biome/size/larger.json        a size step up (the cold)
    data/shamanicmounts/tags/worldgen/biome/size/smaller.json       a size step down (the heat)
    data/shamanicmounts/tags/block/spawnable_on.json                ground a wild mount spawns on

`has_spawns` is every line's spawn tag together; the biome modifier adds the mount to it.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src" / "main" / "resources" / "data" / "shamanicmounts" / "tags"

# Line: (spawn biomes, {pelt: home biomes}). Pelts are A, B, C in the line's own order.
LINES = {
    "eightfold": (
        ["#c:is_plains", "#c:is_savanna", "#minecraft:is_savanna", "minecraft:plains", "minecraft:sunflower_plains",
         "minecraft:meadow"],
        {"a": ["#c:is_plains", "minecraft:meadow"],                      # bay
         "b": ["#c:is_windswept", "#c:is_cold/overworld"],               # black
         "c": ["#c:is_savanna", "#minecraft:is_savanna", "#c:is_dry/overworld"]}),  # palomino
    "drum_hart": (
        ["#c:is_forest", "#minecraft:is_forest", "#c:is_birch_forest", "#c:is_flower_forest", "#c:is_snowy_plains",
         "minecraft:meadow", "minecraft:cherry_grove", "minecraft:snowy_taiga"],
        {"a": ["#c:is_forest", "#minecraft:is_forest"],                  # tan
         "b": ["#c:is_floral", "#c:is_flower_forest", "minecraft:cherry_grove"],  # red
         "c": ["#c:is_snowy", "#c:is_icy"]}),                            # white
    "elk": (
        ["#c:is_taiga", "#minecraft:is_taiga", "#c:is_tree/coniferous", "#c:is_snowy_plains", "minecraft:grove"],
        {"a": ["#c:is_taiga", "#minecraft:is_taiga"],                    # dark
         "b": ["#c:is_old_growth"],                                      # brown
         "c": ["#c:is_snowy", "#c:is_icy"]}),                            # grey
    "crane": (
        ["#c:is_swamp", "#c:is_river", "#minecraft:is_river", "#c:is_beach", "#minecraft:is_beach",
         "#c:is_stony_shores", "minecraft:mangrove_swamp"],
        {"a": ["#c:is_swamp", "#c:is_river", "#minecraft:is_river"],     # white
         "b": ["#c:is_beach", "#minecraft:is_beach", "#c:is_stony_shores"],  # grey
         "c": ["#c:is_cold/overworld", "#c:is_snowy"]}),                 # black
    "nagual": (
        ["#c:is_jungle", "#minecraft:is_jungle", "#c:is_mountain/slope", "minecraft:snowy_slopes", "minecraft:grove"],
        {"a": ["#c:is_jungle", "#minecraft:is_jungle"],                  # gold
         "b": ["minecraft:bamboo_jungle"],                               # panther
         "c": ["#c:is_mountain/slope", "#c:is_snowy", "minecraft:grove"]}),  # snow leopard
    "barghest": (
        ["#c:is_spooky", "#c:is_windswept", "#c:is_badlands", "#minecraft:is_badlands", "minecraft:dark_forest"],
        {"a": ["#c:is_spooky", "minecraft:dark_forest"],                 # black
         "b": ["#c:is_windswept"],                                       # grey
         "c": ["#c:is_badlands", "#minecraft:is_badlands"]}),            # red
    "roc": (
        ["#c:is_mountain", "#minecraft:is_mountain", "#c:is_hill", "#minecraft:is_hill", "#c:is_plateau",
         "#c:is_badlands", "#minecraft:is_badlands", "minecraft:windswept_savanna"],
        {"a": ["#c:is_mountain", "#minecraft:is_mountain", "#c:is_plateau"],  # bone
         "b": ["#c:is_windswept", "#c:is_hill", "#minecraft:is_hill"],   # storm
         "c": ["#c:is_badlands", "#minecraft:is_badlands"]}),            # red
    "shade": (
        ["#c:is_spooky", "minecraft:dark_forest", "#c:is_mushroom", "#c:is_magical", "minecraft:snowy_taiga",
         "minecraft:old_growth_spruce_taiga"],
        {"a": ["#c:is_spooky", "minecraft:dark_forest"],                 # dusk
         "b": ["#c:is_snowy", "#c:is_old_growth"],                       # silver
         "c": ["#c:is_mushroom", "#c:is_magical"]}),                     # ink
    "bear": (
        ["#c:is_forest", "#minecraft:is_forest", "#c:is_taiga", "#minecraft:is_taiga", "#c:is_snowy", "#c:is_icy",
         "#c:is_mountain/slope"],
        {"a": ["#c:is_forest", "#minecraft:is_forest", "#c:is_birch_forest"],  # black bear
         "b": ["#c:is_taiga", "#minecraft:is_taiga", "#c:is_old_growth", "#c:is_mountain"],  # grizzly
         "c": ["#c:is_snowy", "#c:is_icy"]}),                            # polar bear
    "serpent": (
        ["#c:is_swamp", "#c:is_jungle", "#minecraft:is_jungle", "#c:is_river", "#minecraft:is_river", "#c:is_lush",
         "#c:is_desert", "minecraft:mangrove_swamp"],
        {"a": ["#c:is_river", "#minecraft:is_river", "#c:is_swamp"],     # river
         "b": ["#c:is_jungle", "#minecraft:is_jungle", "#c:is_lush"],    # jungle
         "c": ["#c:is_desert", "#c:is_sandy", "#c:is_dry/overworld"]}),  # bone
}

SIZE = {
    "larger": ["#c:is_cold/overworld", "#c:is_snowy", "#c:is_icy"],
    "smaller": ["#c:is_hot/overworld", "#c:is_desert"],
}

SPAWNABLE_ON = ["#minecraft:animals_spawnable_on", "#minecraft:dirt", "#minecraft:sand", "#minecraft:terracotta",
                "minecraft:snow_block", "minecraft:ice", "minecraft:packed_ice",
                "minecraft:stone", "minecraft:gravel", "minecraft:mycelium", "minecraft:podzol", "minecraft:mud",
                "minecraft:moss_block", "minecraft:red_sandstone", "minecraft:sandstone", "minecraft:calcite"]


def entry(value):
    # Vanilla ids always exist; every tag and every other namespace is optional.
    if value.startswith("minecraft:"):
        return value
    return {"id": value, "required": False}


def write(path, values):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"replace": False, "values": [entry(v) for v in values]}, indent=2) + "\n",
                    encoding="utf-8")


def main():
    biome = DATA / "worldgen" / "biome"
    for line, (spawns, pelts) in LINES.items():
        write(biome / "spawns" / f"{line}.json", spawns)
        for pelt, homes in pelts.items():
            write(biome / "pelts" / line / f"{pelt}.json", homes)
    write(biome / "has_spawns.json", [f"#shamanicmounts:spawns/{line}" for line in LINES])
    for name, values in SIZE.items():
        write(biome / "size" / f"{name}.json", values)
    write(DATA / "block" / "spawnable_on.json", SPAWNABLE_ON)
    print("wrote", len(LINES), "lines")


if __name__ == "__main__":
    main()
