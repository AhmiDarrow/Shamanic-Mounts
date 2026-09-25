"""Drive Shamanic Mounts through a real client and server and report PASS/FAIL.

Needs both of these already up, in this order:

    gradlew runHarnessServer
    gradlew runHarnessClient

The server is a flat world on 127.0.0.1:25576 with RCON on 25586 (password in
build/harness-server/server.properties). The client is ShamanQA. Tribal Power's
showcase server on 25585 is a different process and is not used here.

    python tools/system_harness.py [--only boot,items,book] [--list]
"""
import argparse
import importlib
import sys
import time
import traceback
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from harness_lib import PLAYER, check, client, run, say, section, summary  # noqa: E402
from verification_rcon import command as rcon  # noqa: E402

ORDER = ["boot", "items", "book", "looks", "ride", "gifts", "bags", "perf", "breed"]
ROOT = Path(__file__).resolve().parents[1]


def ensure_files():
    """eula, rcon, and the client's first-run warnings. Existing files are left alone."""
    server = ROOT / "build" / "harness-server"
    client_dir = ROOT / "build" / "harness-client"
    server.mkdir(parents=True, exist_ok=True)
    client_dir.mkdir(parents=True, exist_ok=True)
    eula = server / "eula.txt"
    if not eula.is_file():
        eula.write_text("eula=true\n", encoding="utf-8")
    props = server / "server.properties"
    if not props.is_file():
        props.write_text(
            "\n".join([
                "accepts-transfers=false",
                "allow-flight=true",
                "broadcast-rcon-to-ops=true",
                "difficulty=peaceful",
                "enable-rcon=true",
                "enable-status=true",
                "enforce-secure-profile=false",
                "force-gamemode=true",
                "gamemode=creative",
                "generate-structures=false",
                "level-name=world",
                "level-type=minecraft:flat",
                "max-tick-time=-1",
                "motd=Shamanic Mounts harness",
                "online-mode=false",
                "op-permission-level=4",
                "player-idle-timeout=0",
                "rcon.password=shaman",
                "rcon.port=25586",
                "server-port=25576",
                "simulation-distance=6",
                "spawn-animals=true",
                "spawn-monsters=false",
                "spawn-protection=0",
                "sync-chunk-writes=false",
                "view-distance=6",
                "white-list=false",
                "",
            ]),
            encoding="utf-8",
        )
    options = client_dir / "options.txt"
    if not options.is_file():
        options.write_text(
            "pauseOnLostFocus:false\nskipMultiplayerWarning:true\nonboardAccessibility:false\n",
            encoding="utf-8",
        )


def wait_for_player(seconds=240):
    end = time.time() + seconds
    while time.time() < end:
        try:
            listed = rcon("list")
        except (OSError, TimeoutError, ConnectionError, RuntimeError) as failure:
            say(f"waiting for rcon ({failure})")
            time.sleep(3)
            continue
        if PLAYER in listed:
            return True
        time.sleep(2)
    return False


def prepare():
    say("preparing world")
    for cmd in [
        f"op {PLAYER}",
        f"gamemode creative {PLAYER}",
        "gamerule doDaylightCycle false",
        "gamerule doWeatherCycle false",
        "gamerule doMobSpawning false",
        "difficulty peaceful",
        "time set 6000",
        "weather clear",
        "gamerule keepInventory true",
        "forceload add 0 0 32 32",
    ]:
        run(cmd)
    time.sleep(0.5)
    run("fill 0 100 0 16 100 16 minecraft:grass_block")
    run("fill 0 101 0 16 116 16 minecraft:air")
    run("setblock 8 101 8 minecraft:stone")
    run("setblock 10 101 8 minecraft:crafting_table")
    run(f"tp {PLAYER} 8.5 102 8.5")
    time.sleep(0.6)
    # The command poller is alive once state answers.
    client("quiet")
    reply = client("state", timeout=40)
    if reply.startswith("timeout"):
        check("the client command file answers", False, reply)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--only", default="")
    parser.add_argument("--list", action="store_true")
    args = parser.parse_args()
    available = sorted(path.stem for path in (Path(__file__).parent / "harness").glob("*.py") if not path.stem.startswith("_"))
    if args.list:
        print("\n".join(available))
        return 0
    wanted = [name for name in ORDER if name in available]
    if args.only:
        wanted = [name for name in args.only.split(",") if name]
    ensure_files()
    if not wait_for_player():
        say("ShamanQA never joined. Is runHarnessServer up, then runHarnessClient?")
        return 1
    prepare()
    for name in wanted:
        section(name)
        module = importlib.import_module(f"harness.{name}")
        try:
            module.main()
        except Exception:
            traceback.print_exc()
            check("section ran without a harness error", False)
        run(f"clear {PLAYER}")
        client("close")
        time.sleep(0.3)
    return summary()


if __name__ == "__main__":
    sys.exit(1 if main() else 0)
