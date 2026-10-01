"""Frame rate, tick time, and draw cost with a herd in view.

Forty tame mounts of mixed lines stand in front of the camera; then a hundred wild fliers mill about in view,
whose wings fold and open as they hop, and must not make the renderer rebuild a cut plan.
"""
import re
import time

from harness_lib import PLAYER, check, client, pad, run, say
from harness.breed import FOUNDERS, GIFTS, genome_nbt, owner_array
from harness.looks import PAD

LIMIT_FPS = 30
LIMIT_TICK_MS = 25.0


def fps():
    reply = client("fps") or ""
    match = re.search(r"ok (\d+)", reply)
    return int(match.group(1)) if match else -1


def render_stats(seconds):
    """Mount draw cost over `seconds`: draws, microseconds per draw (total, flush, signature), cubes, replans, quads."""
    client("renderstats")
    time.sleep(seconds)
    reply = client("renderstats") or ""
    return {key: float(value) for key, value in re.findall(r"(\w+)=([\d.]+)", reply)}, reply


def flap():
    client("wing 2")
    time.sleep(0.5)
    client("wing off")
    time.sleep(0.5)


def tick_ms():
    out = run("neoforge tps") or ""
    match = re.search(r"Overall: [\d.]+ TPS \(([\d.]+) ms/tick\)", out)
    return float(match.group(1)) if match else -1.0


def main():
    if not str(client("state", timeout=25)).startswith("ok"):
        raise RuntimeError("client is not answering")
    run(f"op {PLAYER}")
    pad()
    run("kill @e[type=shamanicmounts:mount]")
    owner = owner_array()
    run(f"tp {PLAYER} {PAD[0]:.1f} {PAD[1] + 2} {PAD[2] + 14:.1f} 180 12")
    time.sleep(0.6)
    client(f"look {PAD[0]:.1f} {PAD[1] + 1:.1f} {PAD[2] - 4:.1f}")
    time.sleep(1.0)
    alone = fps()
    gifts = list(GIFTS)
    for index in range(40):
        gift = gifts[index % len(gifts)]
        x = PAD[0] - 9 + (index % 10) * 2.0
        z = PAD[2] - (index // 10) * 4.0
        nbt = '{PersistenceRequired:1b,Silent:1b,Rotation:[0f,0f],Tags:["perf"],Owner:%s,GenomeLocked:1b,Genome:%s,Pelt:%d}' % (
            owner, genome_nbt(FOUNDERS[gift][1], FOUNDERS[gift][2]), index % 3)
        run(f"summon shamanicmounts:mount {x:.1f} {PAD[1]} {z:.1f} {nbt}")
    time.sleep(3.0)
    samples = [fps() for _ in (time.sleep(0.5), time.sleep(0.5), time.sleep(0.5))]
    herd = max(samples)
    ticks = tick_ms()
    client("shot perf_herd")
    stats, reply = render_stats(5.0)
    say(f"  forty tame mounts: {reply}")
    draws = stats.get("draws", 0.0)
    quads = stats.get("quads", 0.0)
    say(f"  per draw: {stats.get('us', -1):.1f} us, {quads / draws if draws else 0:.0f} quads submitted")
    check("forty standing mounts rebuild no cut plan", draws > 0 and stats.get("replans", -1) == 0, reply)
    check(f"forty mounts in view keep the client above {LIMIT_FPS} fps", herd >= LIMIT_FPS, f"alone {alone} fps, herd {samples}")
    check(f"the server ticks under {LIMIT_TICK_MS:.0f} ms with forty mounts", 0 <= ticks < LIMIT_TICK_MS, f"{ticks} ms")
    run("kill @e[type=shamanicmounts:mount,tag=perf]")
    wild_fliers()
    run(f"tp {PLAYER} 8.5 102 8.5")


def wild_fliers():
    """A hundred wild cranes and rocs on the pad, with steps to hop; their wings fold and open as they go."""
    for index in range(12):
        run(f"setblock {PAD[0] - 9 + index * 2} {PAD[1]} {PAD[2] - 6 - (index % 4) * 3} minecraft:stone")
    lines = ("ferry", "dream")
    for index in range(100):
        gift = lines[index % 2]
        x = PAD[0] - 9 + (index % 10) * 2.0
        z = PAD[2] - (index // 10) * 2.0
        nbt = '{PersistenceRequired:1b,Silent:1b,Tags:["perf"],GenomeLocked:1b,Genome:%s,Pelt:%d}' % (
            genome_nbt(FOUNDERS[gift][1], FOUNDERS[gift][2]), index % 3)
        run(f"summon shamanicmounts:mount {x:.1f} {PAD[1] + 1} {z:.1f} {nbt}")
    time.sleep(6.0)
    stats, reply = render_stats(5.0)
    say(f"  a hundred wild fliers: {reply}")
    # A hop opens the wings and a landing folds them. Holding every mount in the air and letting go does the same to
    # all of them at once: after the first open and fold, neither layout may be cut again.
    flap()
    client("renderstats")
    for _ in range(3):
        flap()
    reply = client("renderstats") or ""
    stats = {key: float(value) for key, value in re.findall(r"(\w+)=([\d.]+)", reply)}
    say(f"  three more opens and folds: {reply}")
    check("wild fliers opening and folding their wings rebuild no cut plan", stats.get("draws", 0) > 0
          and stats.get("replans", -1) == 0, reply)
    run("kill @e[type=shamanicmounts:mount,tag=perf]")
    run(f"fill {PAD[0] - 9} {PAD[1]} {PAD[2] - 15} {PAD[0] + 13} {PAD[1]} {PAD[2] - 6} minecraft:air")


if __name__ == "__main__":
    main()
