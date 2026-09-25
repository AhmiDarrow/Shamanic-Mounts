"""Start, or restart, the live harness server and client, and wait until ShamanQA is in the world.

    python tools/harness_up.py            # restart both
    python tools/harness_up.py --client   # restart only the client (after a client-side change)
    python tools/harness_up.py --server   # restart only the server (after a server-side change)

The server owns 25576, and the client is the process connected to it, so both are found by port rather
than by command line. Gradle run tasks are started detached, with their output in build/harness-*-run.log.
"""
import argparse
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from verification_rcon import command as rcon  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
PORT = 25576
PLAYER = "ShamanQA"
# A hidden console of its own, so a Ctrl+C in the shell that started it never reaches gradle.
NEW_CONSOLE = 0x00000010 | 0x00000200


def netstat():
    out = subprocess.run(["netstat", "-ano", "-p", "tcp"], capture_output=True, text=True).stdout
    rows = []
    for line in out.splitlines():
        parts = line.split()
        if len(parts) == 5 and parts[0] == "TCP":
            rows.append(parts)
    return rows


def server_pid():
    for _proto, local, _remote, state, pid in netstat():
        if local.endswith(f":{PORT}") and state == "LISTENING":
            return int(pid)
    return None


def client_pid():
    for _proto, _local, remote, state, pid in netstat():
        if remote.endswith(f":{PORT}") and state == "ESTABLISHED" and int(pid) != server_pid():
            return int(pid)
    return None


def kill(pid, label):
    """Stop one process and wait until it is really gone; the client also loses its socket."""
    if not pid:
        return
    subprocess.run(["taskkill", "/PID", str(pid), "/F"], capture_output=True)
    end = time.time() + 30
    while time.time() < end:
        alive = subprocess.run(["tasklist", "/FI", f"PID eq {pid}"], capture_output=True, text=True).stdout
        if str(pid) not in alive:
            break
        time.sleep(1)
    print(f"stopped {label} {pid}", flush=True)
    time.sleep(2)


def stray_clients():
    """Client java processes that no longer hold a connection but are still running with the harness flag."""
    out = subprocess.run(
        ["powershell", "-NoProfile", "-Command",
         "Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'java' -and $_.CommandLine -match 'shamanicmounts.harness=true' } | ForEach-Object { $_.ProcessId }"],
        capture_output=True, text=True).stdout
    return [int(line) for line in out.split() if line.strip().isdigit()]


def launch(task, log):
    handle = open(ROOT / "build" / log, "w", encoding="utf-8")
    info = subprocess.STARTUPINFO()
    info.dwFlags |= subprocess.STARTF_USESHOWWINDOW
    info.wShowWindow = 0
    subprocess.Popen(
        [str(ROOT / "gradlew.bat"), task, "--offline", "-q"],
        cwd=ROOT, stdout=handle, stderr=subprocess.STDOUT, creationflags=NEW_CONSOLE, startupinfo=info,
    )
    print(f"launched {task}", flush=True)


def wait_rcon(seconds):
    end = time.time() + seconds
    while time.time() < end:
        try:
            rcon("list")
            return True
        except Exception:
            time.sleep(3)
    return False


def wait_player(seconds):
    end = time.time() + seconds
    while time.time() < end:
        try:
            if PLAYER in rcon("list"):
                return True
        except Exception:
            pass
        time.sleep(3)
    return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--client", action="store_true", help="restart only the client")
    parser.add_argument("--server", action="store_true", help="restart only the server")
    args = parser.parse_args()
    both = not args.client and not args.server
    if both or args.server:
        kill(client_pid(), "client")
        kill(server_pid(), "server")
        # The herd book remembers every foal the breed ladder made; each session starts with an empty herd.
        herd = ROOT / "build" / "harness-server" / "world" / "data" / "shamanicmounts_herd.dat"
        if herd.is_file():
            herd.unlink()
            print("cleared the herd book", flush=True)
        launch("runHarnessServer", "harness-server-run.log")
        if not wait_rcon(240):
            print("server never answered on rcon", flush=True)
            return 1
        print("server up", flush=True)
    if both or args.client or args.server:
        if not both and args.server and client_pid():
            print("client still attached", flush=True)
        elif args.client or both or not client_pid():
            kill(client_pid(), "client")
            for pid in stray_clients():
                kill(pid, "stray client")
            launch("runHarnessClient", "harness-client-run.log")
    if not wait_player(300):
        print("ShamanQA never joined", flush=True)
        return 1
    print(f"client joined (pid {client_pid()})", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
