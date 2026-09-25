"""The mount screen: tack slots, Saddle Bags on and off, stowing, and the follow, stay, wander choice.

The screen opens with sneak and use. Bags go on by shift-clicking the item from the hotbar, which
makes the bag rows live; an apple is stowed the same way; taking the bags off tips the apple out.
The elk's bearing gives a third row. Follow is proved by walking away and waiting for the mount.
"""
import re
import time

from harness_lib import PLAYER, check, client, pad, press_button, run
from harness.breed import FOUNDERS, genome_nbt, owner_array
from harness.looks import PAD
from harness.ride import summon

MOUNT = "@e[type=shamanicmounts:mount,tag=ride,limit=1]"


def pos(selector):
    out = run(f"data get entity {selector} Pos") or ""
    found = re.findall(r"(-?\d+(?:\.\d+)?)d", out)
    return tuple(float(v) for v in found[:3]) if len(found) >= 3 else None


def nbt(key):
    out = run(f"data get entity {MOUNT} {key}") or ""
    return out.split("following entity data:", 1)[-1].strip() if "following entity data:" in out else out.strip()


def open_screen(head):
    client("close")
    for _ in range(6):
        run(f"tp {PLAYER} {PAD[0]:.1f} {PAD[1]} {PAD[2] + 2.2:.1f} 180 10")
        time.sleep(0.3)
        if f"pos={int(PAD[0])}," in (client("state") or "").replace(" ", ""):
            break
    run(f"tp {MOUNT} {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
    time.sleep(0.2)
    client("key sneak down")
    time.sleep(0.25)
    used = "error"
    for aim in (0.5, 0.75, 0.3):
        client(f"look {PAD[0]:.1f} {PAD[1] + head * aim:.2f} {PAD[2]:.1f}")
        time.sleep(0.15)
        used = client("useentity")
        if str(used).startswith("ok"):
            break
    time.sleep(0.5)
    client("key sneak up")
    return used, client("state") or ""


def slot_count(state):
    match = re.search(r"slots=(\d+)\[", state)
    return int(match.group(1)) if match else -1


def hotbar0(state):
    return slot_count(state) - 9


def main():
    if not str(client("state", timeout=25)).startswith("ok"):
        raise RuntimeError("client is not answering")
    run(f"op {PLAYER}")
    run("gamerule showDeathMessages false")
    run("forceload add -2 -2 3 5")
    pad()
    run(f"clear {PLAYER}")
    owner = owner_array()

    check("a tame eightfold stands on the pad", summon("eightfold", genome_nbt(*FOUNDERS["road"][1:]), owner))
    time.sleep(0.6)
    used, state = open_screen(1.5)
    check("sneak and use opens the mount screen", "screen=MountChestScreen" in state, f"{used} {state[:120]}")
    check("the saddle sits in its tack slot", "0:shamanicmounts:shamanic_saddlex1" in state, state[:120])
    check("the menu holds three tack slots, two bag rows, and the inventory", slot_count(state) == 3 + 10 + 36, state[:80])
    client("shot bags_empty")

    run(f"item replace entity {PLAYER} hotbar.0 with shamanicmounts:saddle_bags 1")
    run(f"item replace entity {PLAYER} hotbar.1 with minecraft:apple 3")
    time.sleep(0.3)
    client(f"click {hotbar0(state)} 0 QUICK_MOVE")
    time.sleep(0.4)
    state = client("state") or ""
    check("shift-clicking Saddle Bags straps them on", "1:shamanicmounts:saddle_bagsx1" in state, state[:160])
    check("the mount says it has bags", "1b" in nbt("Tack[1].count") or "saddle_bags" in nbt("Tack"), nbt("Tack")[:120])
    client(f"click {hotbar0(state) + 1} 0 QUICK_MOVE")
    time.sleep(0.4)
    state = client("state") or ""
    check("with bags on, shift-clicking an apple stows it in the bags", "3:minecraft:applex3" in state, state[:160])
    check("the bags' contents are saved on the mount", "minecraft:apple" in nbt("Chest"), nbt("Chest")[:80])
    client("shot bags_on")

    for mode in ("Stay", "Follow", "Wander"):
        pressed = press_button(mode)
        time.sleep(0.5)
        saved = nbt("Mode")
        sitting = nbt("Sitting")
        expect_sit = "1b" if mode == "Stay" else "0b"
        check(f"{mode} is saved and the mount {'sits' if mode == 'Stay' else 'stands'}",
              pressed.startswith("ok") and mode.upper() in saved.upper() and sitting == expect_sit, f"{pressed} mode={saved} sitting={sitting}")

    run(f"item replace entity {PLAYER} hotbar.2 with minecraft:iron_horse_armor 1")
    time.sleep(0.3)
    client(f"click {hotbar0(state) + 2} 0 QUICK_MOVE")
    time.sleep(0.5)
    state = client("state") or ""
    armor = run(f"attribute {MOUNT} minecraft:generic.armor get") or ""
    check("shift-clicking iron horse armor buckles it on for five armor", "2:minecraft:iron_horse_armorx1" in state and "5.0" in armor,
          f"{state[:80]} | {armor.strip()[:60]}")
    client("shot bags_armor")
    client("click 2 0 QUICK_MOVE")
    time.sleep(0.5)
    armor = run(f"attribute {MOUNT} minecraft:generic.armor get") or ""
    check("taking the armor off drops the protection again", "2:minecraft:iron_horse_armor" not in (client("state") or "") and "0.0" in armor, armor.strip()[:60])

    # Bags off: the apple tips out onto the ground.
    run("kill @e[type=item]")
    client("click 1 0 QUICK_MOVE")
    time.sleep(0.6)
    state = client("state") or ""
    items = run("execute if entity @e[type=item,nbt={Item:{id:\"minecraft:apple\"}}]") or ""
    # The player stands close enough to catch the apple as it falls, so it counts either way.
    caught = run(f"clear {PLAYER} minecraft:apple 0") or ""
    check("taking the bags off tips the apple out and empties the bags", "1:shamanicmounts:saddle_bags" not in state
          and ("passed" in items or "Found" in caught) and "minecraft:apple" not in nbt("Chest"),
          f"{state[:80]} | {items.strip()[:40]} | {caught.strip()[:40]}")
    client("close")
    run("kill @e[type=item]")

    used, state = open_screen(1.5)
    press_button("Follow")
    client("close")
    before = pos(MOUNT)
    run(f"tp {PLAYER} {PAD[0] + 14:.1f} {PAD[1]} {PAD[2]:.1f} 90 0")
    closed = False
    for _ in range(24):
        time.sleep(0.5)
        here = pos(MOUNT)
        if here and abs(here[0] - (PAD[0] + 14)) < 6:
            closed = True
            break
    check("on follow, the mount comes after its owner", closed, f"{before} -> {pos(MOUNT)}")
    used, state = open_screen(1.5)
    press_button("Wander")
    client("close")
    time.sleep(0.5)
    check("wander stands the mount up", nbt("Sitting") == "0b" and "WANDER" in nbt("Mode").upper())

    check("a tame elk stands on the pad", summon("elk", genome_nbt(*FOUNDERS["bearing"][1:]), owner))
    time.sleep(0.6)
    used, state = open_screen(2.1)
    check("the elk's screen has three bag rows", "screen=MountChestScreen" in state and slot_count(state) == 3 + 15 + 36, f"{used} {state[:100]}")
    run(f"item replace entity {PLAYER} hotbar.0 with shamanicmounts:saddle_bags 1")
    time.sleep(0.3)
    client(f"click {hotbar0(state)} 0 QUICK_MOVE")
    time.sleep(0.6)
    client("shot bags_elk")
    client("close")

    check("a tame barghest stands on the pad", summon("barghest", genome_nbt(*FOUNDERS["omen"][1:]), owner))
    time.sleep(0.6)
    run(f"tp {PLAYER} {PAD[0]:.1f} {PAD[1]} {PAD[2] + 2.2:.1f} 180 10")
    time.sleep(0.3)
    client(f"look {PAD[0]:.1f} {PAD[1] + 0.7:.2f} {PAD[2]:.1f}")
    time.sleep(0.2)
    client("useentity")
    time.sleep(0.5)
    riding = client("riding")
    client("key sneak down")
    time.sleep(0.15)
    client("key sneak up")
    time.sleep(0.8)
    check("a barghest stays when its rider steps off", "none" not in str(riding) and "STAY" in nbt("Mode").upper() and nbt("Sitting") == "1b",
          f"{riding} mode={nbt('Mode')} sitting={nbt('Sitting')}")

    # A foal wears nothing: its tack slots refuse the saddle, the bags, and armor until it is grown.
    client("close")
    check("a tame foal stands on the pad", summon("eightfold", genome_nbt(*FOUNDERS["road"][1:]), owner))
    run(f"data merge entity {MOUNT} {{Age:-24000,Saddled:0b,Tack:[]}}")
    run(f"clear {PLAYER}")
    run(f"item replace entity {PLAYER} hotbar.0 with shamanicmounts:shamanic_saddle 1")
    run(f"item replace entity {PLAYER} hotbar.1 with minecraft:iron_horse_armor 1")
    time.sleep(0.4)
    used, state = open_screen(0.8)
    check("sneak and use opens a foal's screen", "screen=MountChestScreen" in state, f"{used} {state[:120]}")
    client(f"click {hotbar0(state)} 0 QUICK_MOVE")
    client(f"click {hotbar0(state) + 1} 0 QUICK_MOVE")
    time.sleep(0.4)
    state = client("state") or ""
    worn = re.search(r"[\[,](0:shamanicmounts:shamanic_saddle|2:minecraft:iron_horse_armor)", state)
    check("a foal's tack slots refuse the saddle and armor", worn is None and "saddle" not in nbt("Tack")
          and "40:shamanicmounts:shamanic_saddle" in state, state[:160])
    client("close")

    run("kill @e[type=shamanicmounts:mount]")
    run("kill @e[type=item]")
    run("gamerule showDeathMessages true")
    run(f"clear {PLAYER}")
    run(f"tp {PLAYER} 8.5 102 8.5")


if __name__ == "__main__":
    main()
