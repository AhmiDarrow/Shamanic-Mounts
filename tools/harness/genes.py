"""Body dominance and blend, size, and pelt, as they come out of real genomes on the live server.

Each case is summoned on the pad with a locked genome and photographed from the three-quarter view
as showcase-*-gene_<case>.png, so a person can see the cross. The checks read what the server made
of it: the size gene moves the mount's health, and a cross stands with the dominant body's height.
"""
import re
import time

from harness_lib import PLAYER, check, client, pad, run
from harness.breed import FOUNDERS, genome_nbt, owner_array, strand
from harness.looks import PAD, shoot, summon

MOUNT = "@e[type=shamanicmounts:mount,tag=look,limit=1]"


def cross(dam, sire):
    """A genome from two founder strands: the dam's line over the sire's line."""
    return "{HeadM:1b,FootM:1b,TailM:1b,Chimera:0b,Maternal:%s,Paternal:%s}" % (strand(dam), strand(sire))


def health():
    out = run(f"attribute {MOUNT} minecraft:generic.max_health base get") or ""
    found = re.search(r"(-?\d+(?:\.\d+)?)\s*$", out.strip())
    return float(found.group(1)) if found else -1.0


def main():
    if not str(client("state", timeout=25)).startswith("ok"):
        raise RuntimeError("client is not answering")
    run(f"op {PLAYER}")
    run("forceload add -2 -2 3 5")
    pad()
    client("hud off")
    run("time set 6000")
    run("gamerule showDeathMessages false")
    owner = owner_array()
    steed = FOUNDERS["road"][1]
    serpent = FOUNDERS["coil"][1]
    bear = FOUNDERS["might"][1]
    hart = FOUNDERS["bearing"][1]
    bird = FOUNDERS["ferry"][1]
    cases = [
        ("steed_over_serpent", cross(steed, serpent), 1.8),
        ("bear_over_steed", cross(steed, bear), 2.0),
        ("hart_over_bird", cross(bird, hart), 2.0),
        ("steed_xs", genome_nbt({**steed, "SIZE": "xs"}, None), 1.6),
        ("steed_xl", genome_nbt({**steed, "SIZE": "xl"}, None), 1.8),
        ("steed_palomino", genome_nbt({**steed, "PELT": "c"}, None), 1.6),
    ]
    healths = {}
    for name, genome, scale in cases:
        if not check(f"the {name.replace('_', ' ')} genome stands on the pad", summon(name, genome, owner)):
            continue
        time.sleep(1.0)
        run(f"tp {MOUNT} {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
        healths[name] = health()
        took = shoot(name, "gene", PAD[0] + 1.4 + scale * 1.0, PAD[2] + 1.4 + scale * 1.0, 135, PAD[1] + scale * 0.6)
        check(f"the {name.replace('_', ' ')} was photographed", took)
    check("an XS eightfold has less health than an XL one", 0 < healths.get("steed_xs", -1) < healths.get("steed_xl", -1),
          str(healths))
    check("size scales the base health: XS 22, XL 30", healths.get("steed_xs") == 22.0 and healths.get("steed_xl") == 30.0,
          str(healths))
    run("kill @e[type=shamanicmounts:mount]")

    # Wild spawns follow the biome: the line lives there, and its pelt is the one at home there.
    for biome, allowed, bear_pelt in (("snowy_plains", {"hart", "bear"}, "c"), ("forest", {"hart", "bear"}, "a")):
        run(f"fillbiome {int(PAD[0]) - 8} 90 {int(PAD[2]) - 8} {int(PAD[0]) + 8} 120 {int(PAD[2]) + 8} minecraft:{biome}")
        lines, bears = [], []
        for _ in range(30):
            run(f"summon shamanicmounts:mount {PAD[0]} {PAD[1]} {PAD[2]}")
            torso = run(f"data get entity @e[type=shamanicmounts:mount,limit=1,sort=nearest] Genome.Maternal.TORSO") or ""
            pelts = [run(f"data get entity @e[type=shamanicmounts:mount,limit=1,sort=nearest] Genome.{side}.PELT") or ""
                     for side in ("Maternal", "Paternal")]
            found = re.search(r'"([a-z]+)"', torso.split("data:", 1)[-1])
            line = found.group(1) if found else "?"
            lines.append(line)
            if line == "bear":
                copies = [re.search(r'"([a-c])"', p.split("data:", 1)[-1]) for p in pelts]
                bears.append("".join(c.group(1) if c else "?" for c in copies))
            run("kill @e[type=shamanicmounts:mount]")
        check(f"wild mounts in {biome} are only lines that live there", set(lines) <= allowed and len(lines) == 30,
              f"{sorted(set(lines))}")
        shown = [pair for pair in bears if (bear_pelt == "c" and pair == "cc") or (bear_pelt == "a" and "a" in pair)]
        check(f"bears in {biome} mostly wear the pelt at home there", bears and len(shown) * 2 >= len(bears),
              f"{bears}")
    run(f"fillbiome {int(PAD[0]) - 8} 90 {int(PAD[2]) - 8} {int(PAD[0]) + 8} 120 {int(PAD[2]) + 8} minecraft:plains")
    client("hud on")
    run("gamerule showDeathMessages true")
    run(f"tp {PLAYER} 8.5 102 8.5")


if __name__ == "__main__":
    main()
