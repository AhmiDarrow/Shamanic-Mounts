"""Breed a chimera from the wild lines.

Each founder carries one gift. The ladder walks the gift chromosome from road to veil,
recombining one neighbor at a time, then locks both copies and flips the chimera coin.
Foals are aged up so the next mating can happen. The operator bypasses the five-minute rest.
A foal that shows a gift neither parent carried stops the run.
"""
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from harness_lib import PLAYER, check, client, run  # noqa: E402

GIFTS = ("road", "call", "bearing", "ferry", "skin", "omen", "dream", "veil", "might", "coil")
LOCI = (
    "TORSO", "HEAD", "LEGS", "FOOT", "RACK", "GAIT", "WINGS", "WINGSPAN", "TAIL", "SCALE", "SIZE", "COAT", "PELT",
    "REALM", "PHASE", "SENSE", "BOND", "WARD", "TRAIL", "ROAD", "CALL", "BEARING",
    "FERRY", "SKIN", "OMEN", "DREAM", "VEIL", "MIGHT", "COIL",
)
QUIET = {
    "LEGS": "four", "FOOT": "hoof", "RACK": "none", "GAIT": "land", "WINGS": "none", "WINGSPAN": "mid",
    "TAIL": "none", "SCALE": "normal", "SIZE": "m", "COAT": "solid", "PELT": "a", "REALM": "hearth", "PHASE": "solid",
    "SENSE": "eye", "BOND": "saddle", "WARD": "none", "TRAIL": "none",
    "ROAD": "none", "CALL": "none", "BEARING": "none", "FERRY": "none", "SKIN": "none",
    "OMEN": "none", "DREAM": "none", "VEIL": "none", "MIGHT": "none", "COIL": "none",
}
# Body of each wild line, plus the one gift. Drum hart and roc are the two that are not homozygous.
FOUNDERS = {
    "road": ("eightfold", {"TORSO": "steed", "HEAD": "steed", "LEGS": "eight", "GAIT": "sea", "TAIL": "plume", "REALM": "lower", "ROAD": "road"}, None),
    "call": ("drum", {"TORSO": "hart", "HEAD": "hart", "RACK": "crown", "TAIL": "flag", "REALM": "upper", "BOND": "drum", "WARD": "longevity", "TRAIL": "star", "CALL": "call"},
             {"RACK": "full"}),
    "bearing": ("elk", {"TORSO": "hart", "HEAD": "hart", "RACK": "crown", "TAIL": "flag", "SCALE": "giant", "REALM": "upper", "BEARING": "bearing"}, None),
    "ferry": ("crane", {"TORSO": "bird", "HEAD": "bird", "LEGS": "two", "FOOT": "talon", "WINGS": "full", "TAIL": "fan", "COAT": "bone", "REALM": "upper", "WARD": "longevity", "FERRY": "ferry"}, None),
    "skin": ("nagual", {"TORSO": "cat", "HEAD": "cat", "FOOT": "paw", "TAIL": "lash", "COAT": "rosette", "REALM": "lower", "BOND": "nagual", "SKIN": "skin"}, None),
    "omen": ("barghest", {"TORSO": "hound", "HEAD": "hound", "FOOT": "paw", "TAIL": "lash", "REALM": "sideways", "SENSE": "scent", "WARD": "guard", "TRAIL": "shadow", "OMEN": "omen"}, None),
    "dream": ("roc", {"TORSO": "bird", "HEAD": "bird", "LEGS": "two", "FOOT": "talon", "WINGS": "astral", "WINGSPAN": "large", "TAIL": "fan", "SCALE": "giant", "COAT": "bone", "REALM": "upper", "DREAM": "dream"},
              {"REALM": "sideways", "WINGSPAN": "vast"}),
    "veil": ("shade", {"TORSO": "cat", "HEAD": "cat", "FOOT": "paw", "TAIL": "lash", "COAT": "dusk", "REALM": "sideways", "PHASE": "ghost", "TRAIL": "shadow", "VEIL": "veil"}, None),
    "might": ("bear", {"TORSO": "bear", "HEAD": "bear", "FOOT": "paw", "SCALE": "giant", "REALM": "lower", "WARD": "guard", "MIGHT": "might"}, None),
    "coil": ("serpent", {"TORSO": "serpent", "HEAD": "serpent", "LEGS": "none", "FOOT": "paw", "TAIL": "lash", "COAT": "rosette", "REALM": "lower", "GAIT": "sea", "COIL": "coil"}, None),
}

BRED = [0]
KEPT = []
KNOWN = {}
PEN_X = 24.0


def say(text):
    print(time.strftime("%H:%M:%S"), text, flush=True)


def strand(overrides):
    values = {**QUIET, **overrides}
    return "{" + ",".join(f'{key}:"{values[key]}"' for key in LOCI) + "}"


def genome_nbt(maternal, paternal_extra):
    paternal = {**maternal, **(paternal_extra or {})}
    body = "{HeadM:1b,FootM:1b,TailM:1b,Chimera:0b,Maternal:%s,Paternal:%s}" % (strand(maternal), strand(paternal))
    return body


def owner_array():
    out = run(f"data get entity {PLAYER} UUID")
    match = re.search(r"\[I;[^\]]+\]", out or "")
    if not match:
        raise RuntimeError("player UUID missing: " + (out or "").strip())
    return re.sub(r"\s+", "", match.group(0))


def parse_genome(text):
    if not text or "Maternal" not in text:
        return None
    found = {}
    for side in ("Maternal", "Paternal"):
        match = re.search(side + r"\s*:\s*\{([^}]*)\}", text)
        if not match:
            return None
        alleles = dict(re.findall(r'([A-Z]+)\s*:\s*"([^"]*)"', match.group(1)))
        if any(locus not in alleles for locus in LOCI):
            return None
        found[side] = alleles
    chimera = bool(re.search(r"Chimera\s*:\s*1b", text))
    return found["Maternal"], found["Paternal"], chimera


def carried(genome):
    maternal, paternal, _chimera = genome
    return {gift for gift in GIFTS if maternal[gift.upper()] == gift or paternal[gift.upper()] == gift}


def strand_has(genome, gifts):
    maternal, paternal, _chimera = genome
    need = set(gifts)
    for alleles in (maternal, paternal):
        if need.issubset({gift for gift in gifts if alleles[gift.upper()] == gift}):
            return True
    return False


def homozygous(genome, gifts):
    maternal, paternal, _chimera = genome
    return all(maternal[gift.upper()] == gift and paternal[gift.upper()] == gift for gift in gifts)


def read_genome(tag):
    if tag in KNOWN:
        return KNOWN[tag]
    text = run(f"data get entity @e[type=shamanicmounts:mount,tag={tag},limit=1] Genome")
    parsed = parse_genome(text or "")
    if parsed is None:
        raise RuntimeError(f"unreadable genome on {tag}: {(text or '').strip()[:400]}")
    KNOWN[tag] = parsed
    return parsed


def summon_founder(gift, owner):
    name, maternal, extra = FOUNDERS[gift]
    tag = "f_" + gift
    nbt = (
        '{NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Age:0,'
        'Tags:["line","%s"],CustomName:\'"%s"\',Owner:%s,GenomeLocked:1b,Genome:%s}'
    ) % (tag, name, owner, genome_nbt(maternal, extra))
    reply = run(f"summon shamanicmounts:mount 4 101 48 {nbt}")
    if "Summoned" not in (reply or ""):
        raise RuntimeError(f"founder {gift} did not summon: {(reply or '').strip()[:300]}")
    genome = read_genome(tag)
    own = carried(genome)
    if own != {gift}:
        raise RuntimeError(f"founder {gift} carries {sorted(own)}")
    KEPT.append(tag)
    park(tag)
    return tag


def park(tag):
    index = KEPT.index(tag)
    x = 4 + (index % 4) * 4
    z = 48 + (index // 4) * 4
    run(f"tp @e[type=shamanicmounts:mount,tag={tag},limit=1] {x} 101 {z}")


def stand(x, z):
    """Server teleports get undone until the client accepts them, so wait until its position matches."""
    state = ""
    for _ in range(10):
        run(f"tp {PLAYER} {x:.1f} 101 {z - 2.2:.1f} 0 8")
        time.sleep(0.3)
        state = client("state") or ""
        flat = state.replace(" ", "")
        if f"pos={int(x)},101," in flat or f"pos={int(x)},102," in flat:
            return state
    return state


def feed(x, z):
    state = stand(x, z)
    if f"pos={int(x)},101," not in state.replace(" ", ""):
        return "error player stayed at " + state[:80]
    used = "error no entity"
    for aim in (1.15, 1.7, 2.1):
        client(f"look {x:.1f} {101 + aim:.2f} {z:.1f}")
        time.sleep(0.2)
        used = client("useentity")
        if str(used).startswith("ok") and "SUCCESS" in str(used):
            return used
    return used


OWNER = [None]
TAME_FOALS = []


def breed(dam, sire):
    """One Diamond Apple mating. Returns (genome, tag) or raises if the foal invented a gift."""
    for tag in KEPT:
        if tag not in (dam, sire):
            park(tag)
    run(f"data merge entity @e[tag={dam},limit=1] {{Age:0}}")
    run(f"data merge entity @e[tag={sire},limit=1] {{Age:0}}")
    run(f"tp @e[tag={dam},limit=1] {PEN_X:.1f} 101 8.0")
    run(f"tp @e[tag={sire},limit=1] {PEN_X:.1f} 101 15.0")
    run("kill @e[type=shamanicmounts:mount,tag=!line]")
    time.sleep(0.2)
    run(f"item replace entity {PLAYER} hotbar.0 with shamanicmounts:diamond_apple 2")
    client("hotbar 0")
    first = feed(PEN_X, 8.0)
    run(f"tp @e[tag={sire},limit=1] {PEN_X:.1f} 101 15.0")
    second = feed(PEN_X, 15.0) if "SUCCESS" in str(first) else "skipped"
    time.sleep(0.45)
    count = run("execute if entity @e[type=shamanicmounts:mount,tag=!line]") or ""
    if "count: 1" not in count:
        return None, f"{count.strip()[:80]} | {first} | {second}"
    age = run("data get entity @e[type=shamanicmounts:mount,tag=!line,limit=1] Age") or ""
    if "-" not in age:
        raise RuntimeError("foal was not born a baby: " + age.strip()[:120])
    tag = "p%d" % BRED[0]
    BRED[0] += 1
    run(f"tag @e[type=shamanicmounts:mount,tag=!line,limit=1] add {tag}")
    run(f"tag @e[tag={tag},limit=1] add line")
    # Foals are born wild. The ladder hands each one to the player so it can be a parent later.
    held = run(f"data get entity @e[tag={tag},limit=1] Owner") or ""
    if "[I;" in held:
        TAME_FOALS.append(tag)
    run(f"data merge entity @e[tag={tag},limit=1] {{Owner:{OWNER[0]}}}")
    # A foal has a mind and a weight; the founders do not. Still it, or it follows its owner off the pen.
    run(f"data merge entity @e[tag={tag},limit=1] {{NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Silent:1b}}")
    genome = read_genome(tag)
    if not genome[2]:
        extra = carried(genome) - carried(read_genome(dam)) - carried(read_genome(sire))
        if extra:
            raise RuntimeError(f"{tag} carries {sorted(extra)} that {dam} and {sire} do not")
    KEPT.append(tag)
    park(tag)
    return genome, tag


def hunt(label, pred, dam, sire, tries):
    misses = 0
    last = ""
    for attempt in range(1, tries + 1):
        genome, tag = breed(dam, sire)
        if genome is None:
            misses += 1
            last = tag
            say(f"  {label} miss {attempt}: {last}")
            if misses >= 4:
                raise RuntimeError(f"{label} stopped breeding ({last})")
            continue
        misses = 0
        if pred(genome):
            say(f"  {label} kept {tag} on try {attempt} (breed {BRED[0]})")
            return tag
        # Drop the foal. Founders and the working parents stay.
        run(f"kill @e[tag={tag},limit=1]")
        KEPT.remove(tag)
        if attempt % 20 == 0:
            say(f"  {label} {attempt}/{tries}, breed {BRED[0]}")
    raise RuntimeError(f"{label} not reached in {tries} foals; last {last}")


def main():
    state = client("state", timeout=25)
    if not str(state).startswith("ok"):
        raise RuntimeError("client is not answering: " + str(state))
    run("kill @e[type=shamanicmounts:mount]")
    run(f"op {PLAYER}")
    run("forceload add 0 0 2 5")
    run("gamerule showDeathMessages false")
    # The pen and the parking rows sit off the 16x16 pad; give the player ground to stand on there.
    run(f"fill {int(PEN_X) - 4} 100 0 {int(PEN_X) + 4} 100 20 minecraft:grass_block")
    run(f"fill {int(PEN_X) - 4} 101 0 {int(PEN_X) + 4} 104 20 minecraft:air")
    run("fill 0 100 44 20 100 60 minecraft:grass_block")
    run("fill 0 101 44 20 104 60 minecraft:air")
    owner = owner_array()
    OWNER[0] = owner
    founders = {}
    for gift in GIFTS:
        founders[gift] = summon_founder(gift, owner)
    check("every wild line is in the world, one gift each", len(founders) == len(GIFTS), " ".join(founders))

    carrier = founders["road"]
    prefix = ["road"]
    for gift in GIFTS[1:]:
        want = prefix + [gift]
        left = hunt(f"bridge {gift}", lambda genome, prefix=prefix: strand_has(genome, prefix), carrier, founders[gift], 80)
        right = hunt(f"bridge {gift} again", lambda genome, prefix=prefix: strand_has(genome, prefix), carrier, founders[gift], 80)
        carrier = hunt(f"join {gift}", lambda genome, want=tuple(want): strand_has(genome, want), left, right, 300)
        prefix = want
        say(f"haplotype {'-'.join(prefix)}")
    # A mount cannot breed with itself, so the second complete strand comes through a founder.
    second = hunt("second full strand", lambda genome: strand_has(genome, GIFTS), carrier, founders["veil"], 100)
    locked = hunt("both copies of every gift", lambda genome: homozygous(genome, GIFTS), carrier, second, 160)
    clone = hunt("second complete adult", lambda genome: homozygous(genome, GIFTS), locked, second, 40)
    check("every bred foal was born wild", not TAME_FOALS, " ".join(TAME_FOALS[:5]))
    check("two adults carry every gift on both strands", homozygous(read_genome(locked), GIFTS) and homozygous(read_genome(clone), GIFTS))

    chimera_tag = None
    normals = 0
    for _ in range(16):
        genome, tag = breed(locked, clone)
        if genome is None:
            raise RuntimeError("complete pair did not breed: " + str(tag))
        if genome[2]:
            chimera_tag = tag
            break
        normals += 1
        run(f"kill @e[tag={tag},limit=1]")
        KEPT.remove(tag)
    check("two bred completes threw a chimera", chimera_tag is not None, f"normals before it {normals}")
    if chimera_tag is None:
        raise RuntimeError("sixteen foals of two completes produced no chimera")
    # breed() already aged and parked it. Read the saved flag, name, and gifts. Age was negative before the merge.
    # A wild foal has no name yet; the saved chimera flag is what makes it one.
    gifts = carried(read_genome(chimera_tag))
    check("the chimera foal carries the chimera flag and every gift", read_genome(chimera_tag)[2] and gifts == set(GIFTS), f"{sorted(gifts)}")
    say(f"chimera {chimera_tag} after {BRED[0]} foals, {normals} normal siblings before the coin")
    run(f"tp @e[tag={chimera_tag},limit=1] {PEN_X:.1f} 101 8.0 0 0")
    run(f"tp {PLAYER} {PEN_X + 2.6:.1f} 101 11.0 135 0")
    time.sleep(0.6)
    client(f"look {PEN_X:.1f} 102.4 8.0")
    time.sleep(0.4)
    client("shot lineage_chimera")
    run("kill @e[type=shamanicmounts:mount]")
    run("kill @e[type=item]")
    run("gamerule showDeathMessages true")


if __name__ == "__main__":
    main()
