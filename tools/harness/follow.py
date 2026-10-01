"""Follow by default, and following mounts crossing dimensions with their owner.

A wild eightfold is calmed with a golden apple, saddled, then tamed by the rein prompts, and must come out on follow, stay on
follow when its rider steps off, and walk after its owner. A second mount is told to stay on its screen and
must still be staying after a ride. Then the owner goes overworld to nether, nether to overworld, overworld
to the end and back by command: the following mount must arrive beside them each time, exactly once across
all three dimensions, while the staying mount and a following mount 40 blocks off stay put. Riding, the
owner is teleported by command and must arrive still in the saddle, and finally rides through a lit nether
portal, where the mount carries its rider itself and must not be moved a second time.
"""
import math
import re
import time

from harness_lib import PLAYER, check, client, pad, press_button, run
from harness.bags import open_screen
from harness.breed import FOUNDERS, genome_nbt, owner_array
from harness.looks import PAD
from harness.ride import mount_up

OVER = "minecraft:overworld"
NETHER = "minecraft:the_nether"
END = "minecraft:the_end"
DIMS = (OVER, NETHER, END)
# Glass boxes in each other dimension, well away from where a portal would open.
BOX = {NETHER: (-100, 100, -100), END: (200, 64, 0)}
HEAD = 1.5


def sel(tag, here=False):
    return f"@e[type=shamanicmounts:mount,tag={tag},limit=1{',distance=0..' if here else ''}]"


def count(dim, tag):
    out = run(f"execute in {dim} if entity @e[type=shamanicmounts:mount,tag={tag},distance=0..]") or ""
    match = re.search(r"count: (\d+)", out)
    return int(match.group(1)) if match else 0


def where(tag):
    """(dimension, x, y, z) of the one mount with this tag, or None."""
    for dim in DIMS:
        out = run(f"execute in {dim} run data get entity {sel(tag, True)} Pos") or ""
        found = re.findall(r"(-?\d+(?:\.\d+)?)d", out)
        if len(found) >= 3:
            return (dim, *(float(v) for v in found[:3]))
    return None


def me():
    out = run(f"data get entity {PLAYER} Pos") or ""
    found = re.findall(r"(-?\d+(?:\.\d+)?)d", out)
    dim = run(f"data get entity {PLAYER} Dimension") or ""
    name = re.search(r'"([^"]+)"', dim)
    return (name.group(1) if name else "?", *(float(v) for v in found[:3])) if len(found) >= 3 else None


def nbt(tag, key, dim=OVER):
    out = run(f"execute in {dim} run data get entity {sel(tag, True)} {key}") or ""
    return out.split("following entity data:", 1)[-1].strip() if "following entity data:" in out else out.strip()


def summon(tag, extra, x, z):
    genome = genome_nbt(*FOUNDERS["road"][1:])
    body = ('{PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Age:0,Rotation:[0f,0f],Tags:["%s"],'
            'GenomeLocked:1b,Genome:%s%s}') % (tag, genome, extra)
    return "Summoned" in (run(f"summon shamanicmounts:mount {x} {PAD[1]} {z} {body}") or "")


def box(dim):
    x, y, z = BOX[dim]
    run(f"execute in {dim} run forceload add {x - 16} {z - 16} {x + 16} {z + 16}")
    time.sleep(1.5)
    run(f"execute in {dim} run fill {x - 8} {y - 1} {z - 8} {x + 8} {y + 8} {z + 8} minecraft:glass hollow")
    run(f"execute in {dim} run fill {x - 7} {y} {z - 7} {x + 7} {y} {z + 7} minecraft:stone")


def go(dim, x, y, z):
    run(f"execute in {dim} run tp {PLAYER} {x + 0.5} {y + 1} {z + 0.5} 0 0")
    time.sleep(2.0)


def tap_sneak():
    client("key sneak down")
    time.sleep(0.15)
    client("key sneak up")
    time.sleep(0.6)


def crossed(label, dim, near=6.0):
    """The following mount is in `dim`, once, beside the player, on its feet, and nowhere else."""
    counts = {d: count(d, "fol") for d in DIMS}
    spot, player = where("fol"), me()
    close = spot is not None and player is not None and spot[0] == dim and player[0] == dim \
        and math.dist(spot[1:], player[1:]) < near
    check(f"{label}: the following mount arrives once, beside its owner",
          counts[dim] == 1 and sum(counts.values()) == 1 and close, f"{counts} mount={spot} owner={player}")
    return close


def main():
    if not str(client("state", timeout=25)).startswith("ok"):
        raise RuntimeError("client is not answering")
    run(f"op {PLAYER}")
    run("gamerule showDeathMessages false")
    run("forceload add -2 -2 3 5")
    run("kill @e[type=shamanicmounts:mount]")
    pad()
    run(f"clear {PLAYER}")
    owner = owner_array()
    for dim in BOX:
        box(dim)

    # Saves from 0.1.5 and earlier: wander was the taming default and a guard's stay came from stepping off.
    old = {
        "old_wander": ("road", ',Mode:"WANDER"', "FOLLOW", "0b"),
        "old_guard": ("omen", ',Mode:"STAY",Sitting:1b', "FOLLOW", "0b"),
        "old_stay": ("road", ',Mode:"STAY",Sitting:1b', "STAY", "1b"),
        "new_wander": ("road", ',Mode:"WANDER",ModeVersion:1', "WANDER", "0b"),
    }
    for index, (tag, (gift, extra, mode, sitting)) in enumerate(old.items()):
        genome = genome_nbt(*FOUNDERS[gift][1:])
        body = ('{NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Age:0,Tags:["%s"],Owner:%s,'
                'GenomeLocked:1b,Genome:%s%s}') % (tag, owner, genome, extra)
        run(f"summon shamanicmounts:mount {PAD[0] - 12 + index * 4} {PAD[1]} {PAD[2] + 20} {body}")
        time.sleep(0.3)
        saved, sat = nbt(tag, "Mode"), nbt(tag, "Sitting")
        check(f"{tag.replace('_', ' ')} loads as {mode.lower()}", mode in saved.upper() and sat == sitting, f"{saved} {sat}")
    run("kill @e[type=shamanicmounts:mount]")

    # A real taming: calm a wild adult, put the saddle on, then answer each rein prompt.
    run(f"effect give {PLAYER} minecraft:resistance 120 4 true")
    check("a wild eightfold stands on the pad", summon("fol", "", PAD[0], PAD[2]))
    run(f"tp {sel('fol')} {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
    run(f"tp {PLAYER} {PAD[0]:.1f} {PAD[1]} {PAD[2] + 2.2:.1f}")
    time.sleep(0.4)
    client(f"look {PAD[0]:.1f} {PAD[1] + HEAD * 0.5:.2f} {PAD[2]:.1f}")
    time.sleep(0.15)
    run(f"item replace entity {PLAYER} hotbar.0 with minecraft:golden_apple 1")
    client("hotbar 0")
    time.sleep(0.2)
    used = client("useentity")
    time.sleep(0.4)
    calm_text = nbt("fol", "Calm")
    calm_match = re.search(r"-?\d+", calm_text)
    calm_ticks = int(calm_match.group()) if calm_match else -1
    check("a golden apple calms it", calm_ticks > 2000, f"{used} {calm_text}")
    run(f"item replace entity {PLAYER} hotbar.0 with shamanicmounts:shamanic_saddle 1")
    client("hotbar 0")
    time.sleep(0.2)
    used = client("useentity")
    time.sleep(0.4)
    riding = client("riding")
    check("the saddle goes on and does not mount", "none" in str(riding) and nbt("fol", "Saddled") == "1b",
          f"{used} {riding} saddled={nbt('fol', 'Saddled')}")
    run(f"item replace entity {PLAYER} hotbar.0 with minecraft:air")
    client("hotbar 0")
    time.sleep(0.2)
    used = client("useentity")
    riding = client("riding")
    check("mounting starts the rein trial", "none" not in str(riding), f"{used} {riding}")
    held = None
    tamed = False
    try:
        deadline = time.time() + 45
        while time.time() < deadline:
            reply = client("cue") or ""
            cue = reply.split()[-1] if reply.startswith("ok") else "none"
            if cue in ("left", "right", "forward", "back") and cue != held:
                if held:
                    client(f"key {held} up")
                client(f"key {cue} down")
                held = cue
            elif cue == "none":
                tamed = "I;" in nbt("fol", "Owner")
                if tamed:
                    break
    finally:
        for name in ("left", "right", "forward", "back"):
            client(f"key {name} up")
    if not tamed:
        tamed = "I;" in nbt("fol", "Owner")
    check("the rein trial tames it", tamed, nbt("fol", "Owner")[:60])
    check("a new tame is on follow", "FOLLOW" in nbt("fol", "Mode").upper() and nbt("fol", "Sitting") == "0b",
          f"{nbt('fol', 'Mode')} {nbt('fol', 'Sitting')}")
    tap_sneak()
    check("stepping off leaves it on follow, standing", "none" in str(client("riding"))
          and "FOLLOW" in nbt("fol", "Mode").upper() and nbt("fol", "Sitting") == "0b",
          f"{nbt('fol', 'Mode')} {nbt('fol', 'Sitting')}")
    run(f"tp {PLAYER} {PAD[0] + 14:.1f} {PAD[1]} {PAD[2]:.1f} 90 0")
    came = False
    for _ in range(24):
        time.sleep(0.5)
        spot = where("fol")
        if spot and abs(spot[1] - (PAD[0] + 14)) < 6:
            came = True
            break
    check("after stepping off, it walks after its owner", came, str(where("fol")))

    # An explicit stay outlasts a ride.
    check("a second tame stands on the pad", summon("ride", f",Owner:{owner},Saddled:1b", PAD[0], PAD[2]))
    time.sleep(0.5)
    # Held still and out of the way while the other mount is handled.
    run(f"data merge entity {sel('fol')} {{NoAI:1b}}")
    run(f"tp {sel('fol')} {PAD[0] + 14:.1f} {PAD[1]} {PAD[2]:.1f}")
    used, state = open_screen(HEAD)
    press_button("Stay")
    time.sleep(0.4)
    client("close")
    check("told to stay, it sits", "STAY" in nbt("ride", "Mode").upper() and nbt("ride", "Sitting") == "1b")
    used, riding = mount_up(HEAD)
    check("the staying mount can still be ridden", "none" not in str(riding), f"{used} {riding}")
    client("key forward down")
    time.sleep(0.6)
    client("key forward up")
    tap_sneak()
    time.sleep(0.6)
    check("stepped off, it stays and lies down again", "STAY" in nbt("ride", "Mode").upper() and nbt("ride", "Sitting") == "1b",
          f"{nbt('ride', 'Mode')} {nbt('ride', 'Sitting')}")

    # A following mount too far away to come.
    check("a far following tame stands 40 blocks off",
          summon("far", f",Owner:{owner},NoAI:1b,Mode:\"FOLLOW\",ModeVersion:1", PAD[0] + 40, PAD[2]))

    # Across dimensions by command, with the staying mount and the far one left behind.
    run(f"tp {PLAYER} {PAD[0] + 2:.1f} {PAD[1]} {PAD[2] + 3:.1f}")
    run(f"tp {sel('fol')} {PAD[0] + 3:.1f} {PAD[1]} {PAD[2] + 3:.1f}")
    run(f"data merge entity {sel('fol')} {{NoAI:0b}}")
    time.sleep(0.5)
    go(NETHER, *BOX[NETHER])
    crossed("overworld to nether", NETHER)
    check("the arrived mount is still on follow", "FOLLOW" in nbt("fol", "Mode", NETHER).upper(), nbt("fol", "Mode", NETHER))
    check("the staying mount stays in the overworld", count(OVER, "ride") == 1 and count(NETHER, "ride") == 0)
    check("a following mount 40 blocks off stays in the overworld", count(OVER, "far") == 1 and count(NETHER, "far") == 0)
    client("shot follow_nether")
    go(OVER, int(PAD[0]) + 2, PAD[1] - 1, int(PAD[2]) + 3)
    crossed("nether to overworld", OVER)
    go(END, *BOX[END])
    crossed("overworld to the end", END)
    go(OVER, int(PAD[0]) + 2, PAD[1] - 1, int(PAD[2]) + 3)
    crossed("the end to the overworld", OVER)

    # Riding when a command teleport moves the rider: the mount comes, and the rider is in the saddle again.
    used, riding = mount_up_tag("fol")
    check("the owner rides the following mount", "none" not in str(riding), f"{used} {riding}")
    go(NETHER, *BOX[NETHER])
    time.sleep(0.5)
    riding = client("riding")
    crossed("ridden, by command", NETHER)
    check("the rider arrives in the saddle", "mount ground=" in str(riding), str(riding))
    client("shot follow_ridden_nether")
    tap_sneak()
    go(OVER, int(PAD[0]) + 2, PAD[1] - 1, int(PAD[2]) + 3)
    crossed("back from the ride", OVER)

    # Ridden through a lit nether portal: the mount carries its rider over by itself, and is not moved twice.
    px, py, pz = 30, int(PAD[1]), 44
    run(f"fill {px - 1} {py} {pz} {px + 2} {py + 4} {pz} minecraft:obsidian")
    run(f"fill {px} {py + 1} {pz} {px + 1} {py + 3} {pz} minecraft:nether_portal[axis=x]")
    run(f"tp {sel('fol')} {px + 1.0} {py} {pz + 3.5} 180 0")
    run(f"tp {PLAYER} {px + 1.0} {py} {pz + 5.5} 180 0")
    # A mount set down beside its owner keeps away from portals for 15 seconds; this one has crossed lately.
    run(f"data merge entity {sel('fol')} {{PortalCooldown:0}}")
    time.sleep(0.5)
    used, riding = mount_up_at("fol", (px + 1.0, py, pz + 3.5), 180)
    check("the owner rides up to the portal", "none" not in str(riding), f"{used} {riding}")
    client(f"look {px + 1.0} {py + 1.5} {pz - 10}")
    client("key forward down")
    arrived = False
    for _ in range(20):
        time.sleep(0.5)
        player = me()
        if player and player[0] == NETHER:
            arrived = True
            break
    client("key forward up")
    time.sleep(2.0)
    riding = client("riding")
    check("through the portal, the owner reaches the nether", arrived, str(me()))
    counts = {d: count(d, "fol") for d in DIMS}
    check("ridden through a portal, the mount crosses once with its rider in the saddle",
          counts[NETHER] == 1 and sum(counts.values()) == 1 and "mount ground=" in str(riding), f"{counts} {riding}")
    tap_sneak()
    go(OVER, int(PAD[0]) + 2, PAD[1] - 1, int(PAD[2]) + 3)
    crossed("home from the portal ride", OVER)
    check("the staying mount never moved", count(OVER, "ride") == 1 and "STAY" in nbt("ride", "Mode").upper())

    run(f"fill {px - 1} {py} {pz} {px + 2} {py + 4} {pz} minecraft:air")
    run("kill @e[type=shamanicmounts:mount]")
    run("kill @e[type=item]")
    for dim, (x, y, z) in BOX.items():
        run(f"execute in {dim} run forceload remove {x - 16} {z - 16} {x + 16} {z + 16}")
    run("gamerule showDeathMessages true")
    run(f"clear {PLAYER}")
    run(f"execute in {OVER} run tp {PLAYER} 8.5 102 8.5")


def mount_up_tag(tag):
    """Ride the mount with this tag in the overworld, from the south, at the pad."""
    return mount_up_at(tag, PAD, 0)


def mount_up_at(tag, spot, yaw):
    x, y, z = spot
    used, riding = "error", "none"
    for aim in (0.5, 0.75, 0.3):
        run(f"execute in {OVER} run tp {sel(tag)} {x} {y} {z} {yaw} 0")
        run(f"execute in {OVER} run tp {PLAYER} {x:.1f} {y} {z + 2.2:.1f}")
        time.sleep(0.4)
        client(f"look {x:.1f} {y + HEAD * aim:.2f} {z:.1f}")
        time.sleep(0.15)
        used = client("useentity")
        time.sleep(0.4)
        riding = client("riding")
        if "none" not in str(riding):
            break
    return used, riding


if __name__ == "__main__":
    main()
