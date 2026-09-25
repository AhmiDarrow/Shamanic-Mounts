"""Pictures for the store page and the cover, and the lying-down pose. Not in the default order.

    python tools/system_harness.py --only store

1. The founders sheet: every line in a wide three-quarter view aimed at the middle of the whole
   animal, head included (showcase-*-look_<line>_sheet.png, read by tools/founders_sheet.py).
2. The stay pose: four body plans lying down, from the side and the three-quarter view.
3. The cover: the eightfold, a gliding crane, and the shade on a lime concrete stage, for tools/store_cover.py to key out.
"""
import time

from harness_lib import PLAYER, check, client, pad, run
from harness.breed import FOUNDERS, GIFTS, genome_nbt, owner_array
from harness.looks import CAMERA, PAD, chimera_nbt, shoot, summon

MOUNT = "@e[type=shamanicmounts:mount,tag=look,limit=1]"


def face_south():
    run(f"tp {MOUNT} {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
    time.sleep(0.3)


def sheet_shot(name):
    """Far enough back for the tallest heads, aimed halfway up the animal."""
    scale, head = CAMERA.get(name, (1.6, 1.35))
    reach = max(scale, head)
    return shoot(name, "sheet", PAD[0] + 2.6 + reach * 1.7, PAD[2] + 2.6 + reach * 1.7, 135, PAD[1] + reach * 0.5)


def clear_stage():
    """The stage walls stand taller than the pad's reset reaches, so take them down explicitly."""
    x, y, z = int(PAD[0]), PAD[1], int(PAD[2])
    run(f"fill {x - 9} {y} {z - 20} {x + 12} {y + 22} {z + 20} minecraft:air")
    run(f"fill {x - 9} {y - 1} {z - 20} {x + 12} {y - 1} {z + 20} minecraft:grass_block")


def main():
    if not str(client("state", timeout=25)).startswith("ok"):
        raise RuntimeError("client is not answering")
    run(f"op {PLAYER}")
    run("forceload add -2 -2 3 5")
    pad()
    clear_stage()
    client("hud off")
    run("time set 6000")
    run("weather clear")
    run("gamerule showDeathMessages false")
    owner = owner_array()
    lines = [(FOUNDERS[gift][0], genome_nbt(FOUNDERS[gift][1], FOUNDERS[gift][2])) for gift in GIFTS]
    lines.append(("chimera", chimera_nbt()))

    for name, genome in lines:
        if not check(f"{name} stands for the sheet", summon(name, genome, owner)):
            continue
        time.sleep(0.8)
        face_south()
        check(f"{name} was photographed for the sheet", sheet_shot(name))

    # Staying, a mount lies down with its legs folded under a level body.
    genomes = dict(lines)
    for name in ("eightfold", "bear", "crane", "nagual"):
        if not check(f"{name} lies down on stay", summon(name, genomes[name], owner, ',Sitting:1b,Mode:"STAY"')):
            continue
        time.sleep(1.6)
        face_south()
        scale, _ = CAMERA.get(name, (1.6, 1.35))
        took = shoot(name, "stay_side", PAD[0] + 1.8 + scale * 1.2, PAD[2] + 0.3, 90, PAD[1] + scale * 0.35)
        took &= shoot(name, "stay_quarter", PAD[0] + 1.5 + scale * 1.0, PAD[2] + 1.5 + scale * 1.0, 135, PAD[1] + scale * 0.35)
        check(f"{name} lying down was photographed", took)

    # The cover stage: a lime floor and two tall lime walls behind the mount, open to the sky.
    x, y, z = int(PAD[0]), PAD[1], int(PAD[2])
    run(f"fill {x - 9} {y - 1} {z - 20} {x + 12} {y - 1} {z + 20} minecraft:lime_concrete")
    run(f"fill {x - 9} {y} {z - 20} {x + 12} {y + 22} {z - 20} minecraft:lime_concrete")
    run(f"fill {x - 9} {y} {z - 20} {x - 9} {y + 22} {z + 20} minecraft:lime_concrete")
    time.sleep(1.0)
    # The cover cast: the eightfold turned toward the camera, the crane gliding, the shade small.
    import math
    # The crane is held in its glide three blocks up and shot from below, so its wings spread in a V.
    for name, lift, turn, fly in (("eightfold", 0.0, 38, False), ("crane", 3.0, 62, True), ("shade", 0.0, 38, False)):
        extra = ",NoGravity:1b" if lift else ""
        if not check(f"{name} stands on the cover stage", summon(name, genomes[name], owner, extra)):
            continue
        run(f"tp {MOUNT} {PAD[0]} {PAD[1] + lift} {PAD[2]} 0 0")
        # A mount without AI never leaves the ground in the client's eyes, so hold the glide by hand.
        client("wing 1" if fly else "wing off")
        time.sleep(1.6)
        scale, head = CAMERA.get(name, (1.6, 1.35))
        reach = max(scale, head)
        distance = 2.6 + reach * 1.9
        angle = math.radians(turn)
        took = shoot(name, "cover", PAD[0] + distance * math.cos(angle), PAD[2] + distance * math.sin(angle), 90 + turn,
                     PAD[1] + lift + reach * 0.5)
        # Two more frames: the idle motes drift, so a per-pixel median of three removes them.
        for frame in ("b", "c"):
            time.sleep(0.6)
            took &= client(f"shot look_{name}_cover_{frame}").startswith("ok")
        check(f"{name} was photographed for the cover", took)

    client("wing off")
    run("kill @e[type=shamanicmounts:mount]")
    clear_stage()
    pad()
    client("hud on")
    run("gamerule showDeathMessages true")
    run(f"tp {PLAYER} 8.5 102 8.5")


if __name__ == "__main__":
    main()
