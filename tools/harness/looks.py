"""Photograph every line and the chimera up close, so eyes, seams, and wings can be checked by eye.

Each mount is summoned alone on the pad with a locked founder genome, faced toward the camera, and
shot from the front, the side, and a low three-quarter view. The pictures land in
build/harness-client/screenshots as showcase-<n>-look_<line>_<view>.png. The checks here are only
that the mount is there and the client took the picture; the pictures are for a person.
"""
import time

from harness_lib import PLAYER, check, client, pad, run
from harness.breed import FOUNDERS, GIFTS, genome_nbt, owner_array, strand

PAD = (8.0, 101, 26.0)
# How far forward of the entity's centre each head sits, in blocks, for the close face pictures.
FACE = {
    "eightfold": 1.7, "drum": 1.6, "elk": 2.1, "crane": 2.3, "nagual": 1.4, "barghest": 1.6, "roc": 2.0,
    "shade": 1.2, "chimera": 2.0, "bear": 1.8, "serpent": 2.2, "mix": 1.6,
}
# Standing size in blocks and the height of the head, per line, so the camera frames each one.
CAMERA = {
    "eightfold": (1.6, 1.5), "drum": (1.6, 1.6), "elk": (2.2, 2.1), "crane": (1.7, 2.3), "nagual": (1.5, 1.1),
    "barghest": (1.6, 1.4), "roc": (2.2, 1.9), "shade": (1.4, 1.0), "chimera": (2.1, 1.9), "bear": (2.0, 1.7), "serpent": (1.6, 1.1), "mix": (1.6, 1.6),
}
CHIMERA_BODY = {"TORSO": "steed", "HEAD": "steed", "LEGS": "eight", "GAIT": "sea", "TAIL": "plume", "REALM": "lower"}


def mix_nbt():
    """A hart dam over an eightfold sire: hart body, the short spare pair, a flag, one road and one call."""
    return "{HeadM:1b,FootM:1b,TailM:1b,Chimera:0b,Maternal:%s,Paternal:%s}" % (
        strand(FOUNDERS["call"][1]), strand(FOUNDERS["road"][1]))


def chimera_nbt(pelt="a"):
    body = {**CHIMERA_BODY, **{gift.upper(): gift for gift in GIFTS}, "PELT": pelt}
    return "{HeadM:1b,FootM:1b,TailM:1b,Chimera:1b,Maternal:%s,Paternal:%s}" % (strand(body), strand(body))


def summon(name, genome, owner, extra=""):
    run("kill @e[type=shamanicmounts:mount]")
    run("kill @e[type=item]")
    time.sleep(0.2)
    nbt = (
        '{NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Age:0,Rotation:[0f,0f],'
        'Tags:["look"],Owner:%s,GenomeLocked:1b,Genome:%s%s}'
    ) % (owner, genome, extra)
    reply = run(f"summon shamanicmounts:mount {PAD[0]} {PAD[1]} {PAD[2]} {nbt}")
    return "Summoned" in (reply or "")


def stand(x, z, yaw, eye_y, at_z=None):
    for _ in range(8):
        run(f"tp {PLAYER} {x:.2f} {PAD[1]} {z:.2f} {yaw} 0")
        time.sleep(0.25)
        state = client("state") or ""
        flat = state.replace(" ", "")
        if f"pos={int(x)}," in flat or f"pos={int(x) - 1}," in flat:
            break
    client(f"look {PAD[0]:.2f} {eye_y:.2f} {PAD[2] if at_z is None else at_z:.2f}")
    time.sleep(0.25)


def shoot(line, view, x, z, yaw, eye_y, at_z=None):
    stand(x, z, yaw, eye_y, at_z)
    time.sleep(0.35)
    reply = client(f"shot look_{line}_{view}")
    return reply.startswith("ok")


def main():
    state = client("state", timeout=25)
    if not str(state).startswith("ok"):
        raise RuntimeError("client is not answering: " + str(state))
    run(f"op {PLAYER}")
    run("forceload add -2 -2 3 5")
    pad()
    client("hud off")
    run("time set 6000")
    run("gamerule showDeathMessages false")
    owner = owner_array()
    lines = [(gift, FOUNDERS[gift][0], genome_nbt(FOUNDERS[gift][1], FOUNDERS[gift][2])) for gift in GIFTS]
    lines.append(("chimera", "chimera", chimera_nbt()))
    lines.append(("mix", "mix", mix_nbt()))
    for gift, name, genome in lines:
        summoned = summon(name, genome, owner)
        if not check(f"{name} stands on the pad", summoned):
            continue
        time.sleep(0.4)
        scale, head = CAMERA.get(name, (1.6, 1.35))
        head_y = PAD[1] + head
        # The mount faces south (+z), toward a camera standing south of the pad.
        run(f"tp @e[type=shamanicmounts:mount,tag=look,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
        took = shoot(name, "front", PAD[0], PAD[2] + 1.3 + scale * 0.7, 180, head_y)
        took &= shoot(name, "side", PAD[0] + 1.6 + scale * 1.1, PAD[2] + 0.3, 90, PAD[1] + scale * 0.6)
        took &= shoot(name, "quarter", PAD[0] + 1.3 + scale * 0.8, PAD[2] + 1.3 + scale * 0.8, 135, PAD[1] + scale * 0.65)
        check(f"{name} was photographed front, side, and quarter", took)
        # A wider three-quarter view for the founders sheet, with the whole animal in frame.
        reach = max(scale, head)
        took = shoot(name, "sheet", PAD[0] + 2.6 + reach * 1.7, PAD[2] + 2.6 + reach * 1.7, 135, PAD[1] + reach * 0.5)
        check(f"{name} was photographed for the sheet", took)
        # The face, close: a level side profile and a straight front view of the head.
        forward = FACE.get(name, 1.5)
        # The mount faces south, so its head is toward +z.
        took = shoot(name, "face_side", PAD[0] + 2.1, PAD[2] + forward, 90, head_y, at_z=PAD[2] + forward)
        took &= shoot(name, "face_front", PAD[0], PAD[2] + forward + 2.1, 180, head_y, at_z=PAD[2] + forward)
        check(f"{name} was photographed face on", took)
        # Two points of the stride, held still by the client: legs spread, then a leg lifted.
        client("pose 0 1")
        took = shoot(name, "stride_a", PAD[0] + 1.6 + scale * 1.1, PAD[2] + 0.3, 90, PAD[1] + scale * 0.55)
        client("pose -1.18 1")
        took &= shoot(name, "stride_b", PAD[0] + 1.3 + scale * 0.8, PAD[2] + 1.3 + scale * 0.8, 135, PAD[1] + scale * 0.55)
        client("pose off")
        check(f"{name} was photographed mid-stride", took)
    # The other two pelts of every line, and the ends of the wingspan gene.
    for gift, name, genome in lines[:-1]:
        for pelt in (1, 2):
            # Both copies of the pelt gene set to the second or third pelt.
            code = "abc"[pelt]
            if gift == "chimera":
                genome = chimera_nbt(code)
            else:
                genome = genome_nbt({**FOUNDERS[gift][1], "PELT": code}, FOUNDERS[gift][2])
            if not check(f"{name} in pelt {pelt} stands on the pad", summon(name, genome, owner)):
                continue
            time.sleep(1.2)
            scale, head = CAMERA.get(name, (1.6, 1.35))
            run(f"tp @e[type=shamanicmounts:mount,tag=look,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
            took = shoot(name, f"pelt{pelt}_quarter", PAD[0] + 1.3 + scale * 0.8, PAD[2] + 1.3 + scale * 0.8, 135, PAD[1] + scale * 0.65)
            check(f"{name} in pelt {pelt} was photographed", took)
    vast = genome_nbt({**FOUNDERS["dream"][1], "WINGSPAN": "vast"}, {"REALM": "sideways"})
    small = genome_nbt({**FOUNDERS["ferry"][1], "WINGSPAN": "small"}, None)
    for name, genome, scale in (("roc_vast", vast, 2.2), ("crane_small", small, 1.7)):
        if not check(f"{name} stands on the pad", summon(name, genome, owner)):
            continue
        time.sleep(1.2)
        run(f"tp @e[type=shamanicmounts:mount,tag=look,limit=1] {PAD[0]} {PAD[1]} {PAD[2]} 0 0")
        took = shoot(name, "quarter", PAD[0] + 1.3 + scale * 1.4, PAD[2] + 1.3 + scale * 1.4, 135, PAD[1] + scale * 0.65)
        check(f"{name} was photographed", took)
    run("kill @e[type=shamanicmounts:mount]")
    client("hud on")
    run("gamerule showDeathMessages true")
    run(f"tp {PLAYER} 8.5 102 8.5")


if __name__ == "__main__":
    main()
