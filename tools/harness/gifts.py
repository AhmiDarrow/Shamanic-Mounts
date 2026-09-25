"""Use every ridden gift once and watch it land: the effect on the player, the world, or the mount.

Drum: Regeneration on the rider. Elk: a pig in front is thrown back. Nagual: the cat vanishes,
the rider gets Speed, and damage brings it back. Barghest: a zombie glows at night. Shade: a held
sneak keeps the rider on and quiets a zombie; sneak and use blinks six blocks. Roc: a held jump
climbs and the bar drains. Chimera: every one of them. Each gets a picture as it fires.
"""
import re
import time

from harness_lib import PLAYER, check, client, pad, run
from harness.breed import FOUNDERS, genome_nbt, owner_array
from harness.looks import CAMERA, PAD, chimera_nbt
from harness.ride import gaze, mount_up, release, summon


def effect(who, name):
    """who is the inside of a selector, such as name=ShamanQA or type=zombie,tag=target."""
    out = run(f'execute if entity @e[{who},limit=1,nbt={{active_effects:[{{id:"minecraft:{name}"}}]}}]') or ""
    return "passed" in out


ME = f"name={PLAYER}"


def pos(selector):
    out = run(f"data get entity {selector} Pos") or ""
    found = re.findall(r"(-?\d+(?:\.\d+)?)d", out)
    return tuple(float(v) for v in found[:3]) if len(found) >= 3 else None


def tag_value(selector, key):
    out = run(f"data get entity {selector} {key}") or ""
    match = re.search(r"(-?\d+)", out.split("following")[-1] if "following" in out else out.rsplit(":", 1)[-1])
    return int(match.group(1)) if match else None


def press_use():
    client("key use down")
    time.sleep(0.15)
    client("key use up")


def ride(name, genome, owner):
    scale, head = CAMERA.get(name, (1.6, 1.35))
    if not check(f"{name} stands saddled on the pad", summon(name, genome, owner)):
        return False
    time.sleep(0.4)
    used, riding = mount_up(head)
    return check(f"the player mounts the {name}", "ok" in str(riding) and "none" not in str(riding), f"{used} {riding}")


def step_off():
    release()
    client("key sneak down")
    time.sleep(0.15)
    client("key sneak up")
    time.sleep(0.5)
    run(f"effect clear {PLAYER}")


def drum(owner):
    if not ride("drum", genome_nbt(*FOUNDERS["call"][1:]), owner):
        return
    client("camera front")
    press_use()
    time.sleep(0.25)
    shot = client("shot gift_drum")
    time.sleep(0.3)
    check("the drum gives the rider Regeneration", effect(ME, "regeneration"))
    check("the drum rider sees in the dark", effect(ME, "night_vision"))
    check("the drum was photographed", shot.startswith("ok"))
    step_off()


def ram(owner):
    if not ride("elk", genome_nbt(*FOUNDERS["bearing"][1:]), owner):
        return
    run("kill @e[type=pig]")
    run(f"tp @e[type=shamanicmounts:mount,tag=ride,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
    run(f"summon pig {PAD[0]} {PAD[1]} {PAD[2] - 2.5} {{NoAI:1b,PersistenceRequired:1b,Tags:[\"target\"]}}")
    time.sleep(0.4)
    client("camera front")
    client("swing")
    time.sleep(0.3)
    shot = client("shot gift_ram")
    time.sleep(0.6)
    health = tag_value("@e[type=pig,tag=target,limit=1]", "Health")
    check("the elk's ram strikes the pig in front of it", health is not None and health < 10, f"health={health}")
    check("the ram was photographed", shot.startswith("ok"))
    run("kill @e[type=pig]")
    step_off()


def nagual(owner):
    if not ride("nagual", genome_nbt(*FOUNDERS["skin"][1:]), owner):
        return
    client("camera front")
    press_use()
    time.sleep(0.3)
    shot = client("shot gift_send_away")
    time.sleep(0.4)
    away = tag_value("@e[type=shamanicmounts:mount,tag=ride,limit=1]", "Away") or 0
    riding = client("riding")
    check("the nagual goes away and the rider is on foot", away > 0 and "none" in str(riding), f"away={away} {riding}")
    check("the sent-away rider has Speed and Jump Boost", effect(ME, "speed") and effect(ME, "jump_boost"))
    check("the send-away was photographed", shot.startswith("ok"))
    # Creative shrugs off ordinary damage; the void does not, and one point is nothing.
    run(f"damage {PLAYER} 1 minecraft:out_of_world")
    time.sleep(0.6)
    back = client("shot gift_return")
    away = tag_value("@e[type=shamanicmounts:mount,tag=ride,limit=1]", "Away")
    check("damage calls the nagual back", away == 0 and back.startswith("ok"), f"away={away}")
    run(f"effect clear {PLAYER}")


def scent(owner):
    if not ride("barghest", genome_nbt(*FOUNDERS["omen"][1:]), owner):
        return
    run("difficulty easy")
    run("time set 18000")
    run("kill @e[type=zombie]")
    run(f"summon zombie {PAD[0]} {PAD[1]} {PAD[2] - 12} {{NoAI:1b,PersistenceRequired:1b,Tags:[\"target\"]}}")
    client("camera front")
    time.sleep(1.2)
    shot = client("shot gift_scent")
    check("a zombie twelve blocks off glows while the hound is ridden", effect("type=zombie,tag=target", "glowing"))
    check("the scent was photographed", shot.startswith("ok"))
    step_off()
    run("kill @e[type=zombie]")
    run("time set 6000")
    run("difficulty peaceful")


def shade(owner):
    if not ride("shade", genome_nbt(*FOUNDERS["veil"][1:]), owner):
        return
    run("difficulty easy")
    run("kill @e[type=zombie]")
    run(f"summon zombie {PAD[0] + 3} {PAD[1]} {PAD[2] - 4} {{PersistenceRequired:1b,Tags:[\"target\"]}}")
    client("camera front")
    client("key sneak down")
    time.sleep(0.9)
    riding = client("riding")
    check("holding sneak keeps the rider on the shade", "none" not in str(riding), riding)
    shot = client("shot gift_hide")
    time.sleep(0.6)
    hidden = run("data get entity @e[type=shamanicmounts:mount,tag=ride,limit=1] Hidden") or ""
    check("the held sneak hides the rider and the cat", "1b" in hidden, hidden.strip()[:60])
    check("the hide was photographed", shot.startswith("ok"))
    gaze("level")
    time.sleep(0.2)
    before = pos(PLAYER)
    press_use()
    time.sleep(0.4)
    blink = client("shot gift_blink")
    after = pos(PLAYER)
    client("key sneak up")
    time.sleep(0.4)
    still = client("riding")
    jumped = before is not None and after is not None and abs(after[2] - before[2]) > 4.0
    check("sneak and use blinks the cat six blocks", jumped, f"{before} -> {after}")
    check("the blink was photographed", blink.startswith("ok"))
    check("releasing a long sneak keeps the rider on", "none" not in str(still), still)
    client("key sneak down")
    time.sleep(0.15)
    client("key sneak up")
    time.sleep(0.5)
    check("a sneak tap steps off the shade", "none" in str(client("riding")))
    run("kill @e[type=zombie]")
    run("difficulty peaceful")
    run(f"effect clear {PLAYER}")


def roc(owner):
    if not ride("roc", genome_nbt(*FOUNDERS["dream"][1:]), owner):
        return
    run(f"tp @e[type=shamanicmounts:mount,tag=ride,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
    time.sleep(0.3)
    full = tag_value("@e[type=shamanicmounts:mount,tag=ride,limit=1]", "Stamina")
    client("camera front")
    gaze("level")
    client("key forward down")
    client("key jump down")
    time.sleep(1.4)
    shot = client("shot gift_climb")
    riding = client("riding")
    spent = tag_value("@e[type=shamanicmounts:mount,tag=ride,limit=1]", "Stamina")
    client("key jump up")
    client("key forward up")
    check("the roc climbs on a held jump and the bar drains", "ground=false" in str(riding) and spent is not None and full is not None and spent < full, f"{full} -> {spent} {riding}")
    check("the climb was photographed", shot.startswith("ok"))
    time.sleep(2.5)
    step_off()


def bear(owner):
    if not ride("bear", genome_nbt(*FOUNDERS["might"][1:]), owner):
        return
    run("kill @e[type=pig]")
    run(f"tp @e[type=shamanicmounts:mount,tag=ride,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
    run(f"summon pig {PAD[0]} {PAD[1]} {PAD[2] - 2.8} {{NoAI:1b,PersistenceRequired:1b,Tags:[\"target\"]}}")
    time.sleep(0.4)
    client("camera front")
    check("the bear's rider has Resistance from both copies of the might", effect(ME, "resistance"))
    client("swing")
    time.sleep(0.3)
    shot = client("shot gift_maul")
    time.sleep(0.6)
    health = tag_value("@e[type=pig,tag=target,limit=1]", "Health")
    slowed = effect("type=pig,tag=target", "slowness")
    check("the bear's maul strikes the pig in front and slows it", health is not None and health < 10 and slowed, f"health={health} slowed={slowed}")
    check("the maul was photographed", shot.startswith("ok"))
    run("kill @e[type=pig]")
    step_off()


def serpent(owner):
    if not ride("serpent", genome_nbt(*FOUNDERS["coil"][1:]), owner):
        return
    run("kill @e[type=pig]")
    run(f"tp @e[type=shamanicmounts:mount,tag=ride,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
    run(f"summon pig {PAD[0]} {PAD[1]} {PAD[2] - 2.8} {{NoAI:1b,PersistenceRequired:1b,Tags:[\"target\"]}}")
    time.sleep(0.4)
    client("camera front")
    check("the serpent's rider breathes water from both copies of the coil", effect(ME, "water_breathing"))
    client("swing")
    time.sleep(0.4)
    shot = client("shot gift_coil")
    time.sleep(0.5)
    held = effect("type=pig,tag=target", "slowness") and effect("type=pig,tag=target", "poison")
    check("the serpent's coil holds and poisons the pig in front", held)
    check("the coil was photographed", shot.startswith("ok"))
    run("kill @e[type=pig]")
    # A pool: the serpent swims through it and rises on jump.
    run(f"fill {int(PAD[0]) - 3} 98 {int(PAD[2]) - 12} {int(PAD[0]) + 3} 100 {int(PAD[2]) - 6} minecraft:water")
    run(f"tp @e[type=shamanicmounts:mount,tag=ride,limit=1] {PAD[0]} 99 {PAD[2] - 9} 0 0")
    time.sleep(0.6)
    before = pos(MOUNT_SEL)
    client("key jump down")
    time.sleep(1.0)
    after = pos(MOUNT_SEL)
    client("key jump up")
    swim = client("shot gift_swim")
    check("the serpent rises through water on a held jump", before is not None and after is not None and after[1] > before[1] + 0.4, f"{before} -> {after}")
    check("the swim was photographed", swim.startswith("ok"))
    run(f"fill {int(PAD[0]) - 3} 98 {int(PAD[2]) - 12} {int(PAD[0]) + 3} 100 {int(PAD[2]) - 6} minecraft:grass_block")
    run(f"fill {int(PAD[0]) - 3} 101 {int(PAD[2]) - 12} {int(PAD[0]) + 3} 101 {int(PAD[2]) - 6} minecraft:air")
    step_off()


MOUNT_SEL = "@e[type=shamanicmounts:mount,tag=ride,limit=1]"


def chimera(owner):
    if not ride("chimera", chimera_nbt(), owner):
        return
    client("camera front")
    press_use()
    time.sleep(0.4)
    shot = client("shot gift_chimera_drum")
    check("the chimera's use plays the drum first", effect(ME, "regeneration"))
    client("key sneak down")
    time.sleep(0.9)
    gaze("level")
    time.sleep(0.2)
    before = pos(PLAYER)
    press_use()
    time.sleep(0.4)
    after = pos(PLAYER)
    client("key sneak up")
    jumped = before is not None and after is not None and abs(after[2] - before[2]) > 4.0
    check("the chimera's sneak and use blinks", jumped, f"{before} -> {after}")
    check("the chimera was photographed", shot.startswith("ok"))
    step_off()


def main():
    state = client("state", timeout=25)
    if not str(state).startswith("ok"):
        raise RuntimeError("client is not answering: " + str(state))
    run(f"op {PLAYER}")
    run("gamerule showDeathMessages false")
    run("forceload add -2 -2 3 5")
    pad()
    run(f"effect clear {PLAYER}")
    owner = owner_array()
    for step in (drum, ram, nagual, scent, shade, roc, bear, serpent, chimera):
        try:
            step(owner)
        finally:
            release()
            client("camera first")
    run("kill @e[type=shamanicmounts:mount]")
    run("gamerule showDeathMessages true")
    run(f"tp {PLAYER} 8.5 102 8.5")


if __name__ == "__main__":
    main()
