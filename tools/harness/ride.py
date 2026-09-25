"""Ride every line and photograph it moving: the stride, the wings in the air, and the hound sitting.

Each mount is summoned tame, owned, and saddled, the player climbs on with the use key, the camera
goes to third person, and the forward key is held for a second before the picture. Fliers then hold
jump so the climb and the flap show. The pictures land beside the looks pictures.
"""
import time

from harness_lib import PLAYER, check, client, pad, run
from harness.breed import FOUNDERS, GIFTS, genome_nbt, owner_array
from harness.looks import CAMERA, PAD, chimera_nbt

FLIERS = {"crane", "roc", "chimera"}


def summon(name, genome, owner):
    run("kill @e[type=shamanicmounts:mount]")
    run("kill @e[type=item]")
    time.sleep(0.2)
    nbt = (
        '{PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Age:0,Rotation:[0f,0f],Tags:["ride"],'
        'Owner:%s,Saddled:1b,GenomeLocked:1b,Genome:%s}'
    ) % (owner, genome)
    reply = run(f"summon shamanicmounts:mount {PAD[0]} {PAD[1]} {PAD[2]} {nbt}")
    return "Summoned" in (reply or "")


def mount_up(head):
    """Stand south of the mount, look at its middle, and use it. Returns the riding reply."""
    for _ in range(6):
        run(f"tp {PLAYER} {PAD[0]:.1f} {PAD[1]} {PAD[2] + 2.2:.1f} 180 10")
        time.sleep(0.3)
        state = client("state") or ""
        if f"pos={int(PAD[0])}," in state.replace(" ", ""):
            break
    used = "error no entity"
    for aim in (0.5, 0.75, 0.3):
        run(f"tp @e[type=shamanicmounts:mount,tag=ride,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
        time.sleep(0.15)
        client(f"look {PAD[0]:.1f} {PAD[1] + head * aim:.2f} {PAD[2]:.1f}")
        time.sleep(0.15)
        used = client("useentity")
        if str(used).startswith("ok"):
            break
    time.sleep(0.4)
    gaze("up")
    return used, client("riding")


def gaze(where):
    """Aim the rider so the third-person camera frames the mount: up puts the front camera low in
    front looking up at the animal, down puts the back camera high behind looking down on it."""
    # Minecraft puts the front camera along the gaze and the back camera opposite it, so looking
    # down drops the front camera to the grass and lifts the back camera over the rider's shoulder.
    if where == "up":
        client(f"look {PAD[0]:.1f} {PAD[1] - 6:.1f} {PAD[2] - 25:.1f}")
    elif where == "down":
        client(f"look {PAD[0]:.1f} {PAD[1] - 8:.1f} {PAD[2] - 25:.1f}")
    else:
        client(f"look {PAD[0]:.1f} {PAD[1] + 1.6:.1f} {PAD[2] - 25:.1f}")


def airborne():
    return "ground=false" in str(client("riding"))


def lift_off(name):
    """Hold jump. Says whether the mount left the ground at any point while it was held."""
    client("key jump down")
    up = False
    for _ in range(4):
        time.sleep(0.3)
        up = airborne() or up
    return "hold" if up else "none"


def release():
    for key in ("forward", "jump", "sneak"):
        client(f"key {key} up")


def main():
    state = client("state", timeout=25)
    if not str(state).startswith("ok"):
        raise RuntimeError("client is not answering: " + str(state))
    run(f"op {PLAYER}")
    run("gamerule showDeathMessages false")
    run("forceload add -2 -2 3 5")
    pad()
    owner = owner_array()
    lines = [(FOUNDERS[gift][0], genome_nbt(FOUNDERS[gift][1], FOUNDERS[gift][2])) for gift in GIFTS]
    lines.append(("chimera", chimera_nbt()))
    for name, genome in lines:
        scale, head = CAMERA.get(name, (1.6, 1.35))
        if not check(f"{name} stands saddled on the pad", summon(name, genome, owner)):
            continue
        time.sleep(0.4)
        used, riding = mount_up(head)
        if not check(f"the player mounts the {name}", "ok" in str(riding) and "none" not in str(riding), f"{used} {riding}"):
            continue
        client("camera front")
        gaze("up")
        client("key forward down")
        time.sleep(0.6)
        stride = client(f"shot ride_{name}_stride")
        client("key forward up")
        client("camera back")
        gaze("down")
        client("key forward down")
        time.sleep(0.4)
        front = client(f"shot ride_{name}_back")
        client("key forward up")
        check(f"the {name} was photographed striding", stride.startswith("ok") and front.startswith("ok"))
        if name in FLIERS:
            run(f"tp @e[type=shamanicmounts:mount,tag=ride,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
            time.sleep(0.3)
            client("camera front")
            gaze("level")
            client("key forward down")
            how = lift_off(name)
            time.sleep(0.4)
            air = client(f"shot ride_{name}_climb")
            riding = client("riding")
            client("key jump up")
            client("key forward up")
            check(f"the {name} lifts off on the jump key", air.startswith("ok") and how != "none", f"{how} {riding}")
            time.sleep(2.0)
        release()
        client("key sneak down")
        time.sleep(0.15)
        client("key sneak up")
        time.sleep(0.6)
        after = client("riding")
        check(f"a sneak tap dismounts the {name}", "none" in str(after), after)
        if name == "barghest":
            client("camera first")
            run(f"tp @e[type=shamanicmounts:mount,tag=ride,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
            run(f"tp {PLAYER} {PAD[0] + 2.4:.1f} {PAD[1]} {PAD[2] + 2.4:.1f} 135 0")
            time.sleep(0.3)
            client(f"look {PAD[0]:.1f} {PAD[1] + 0.9:.2f} {PAD[2]:.1f}")
            time.sleep(1.0)
            sat = client("shot sit_barghest")
            sitting = run("data get entity @e[type=shamanicmounts:mount,tag=ride,limit=1] Sitting") or ""
            check("the hound sits when its rider steps off", sat.startswith("ok") and "1b" in sitting, sitting.strip()[:80])
    release()
    client("camera first")
    run("kill @e[type=shamanicmounts:mount]")
    run("gamerule showDeathMessages true")
    run(f"tp {PLAYER} 8.5 102 8.5")


if __name__ == "__main__":
    main()
