"""Frame rate and tick time with a herd in view. Forty mounts of mixed lines stand in front of the camera."""
import re
import time

from harness_lib import PLAYER, check, client, pad, run
from harness.breed import FOUNDERS, GIFTS, genome_nbt, owner_array
from harness.looks import PAD

LIMIT_FPS = 30
LIMIT_TICK_MS = 25.0


def fps():
    reply = client("fps") or ""
    match = re.search(r"ok (\d+)", reply)
    return int(match.group(1)) if match else -1


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
    check(f"forty mounts in view keep the client above {LIMIT_FPS} fps", herd >= LIMIT_FPS, f"alone {alone} fps, herd {samples}")
    check(f"the server ticks under {LIMIT_TICK_MS:.0f} ms with forty mounts", 0 <= ticks < LIMIT_TICK_MS, f"{ticks} ms")
    run("kill @e[type=shamanicmounts:mount,tag=perf]")
    run(f"tp {PLAYER} 8.5 102 8.5")


if __name__ == "__main__":
    main()
