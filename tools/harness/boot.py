"""The dedicated server and the joined client are up, and the mod loaded on both."""
from harness_lib import CLIENT, PLAYER, check, log_text, run


def main():
    listed = run("list")
    check("ShamanQA is on the harness server", PLAYER in listed, listed.strip()[:160])
    server_log = log_text("build/harness-server/logs/latest.log")
    client_log = log_text("build/harness-client/logs/latest.log")
    check("the server loaded Shamanic Mounts", "Shamanic Mounts genome ready" in server_log)
    check(
        "the dedicated server did not load a client class",
        "NoClassDefFoundError" not in server_log and "ClassNotFoundException" not in server_log,
        "see harness-server/logs/latest.log" if "NoClassDefFoundError" in server_log or "ClassNotFoundException" in server_log else "",
    )
    check(
        "recipes parsed",
        "Parsing error loading recipe shamanicmounts" not in server_log and "Parsing error loading recipe shamanicmounts" not in client_log,
    )
    # The server list already proves the join. This catches a client that crashed on the way in.
    crashed = "The game crashed" in client_log or "Failed to start the minecraft server" in client_log
    reached = bool(client_log) and CLIENT.is_dir() and not crashed and "25576" in client_log
    detail = ""
    if not client_log:
        detail = "no client log"
    elif crashed:
        detail = "crash"
    elif "25576" not in client_log:
        detail = "no port in the log"
    check("the client reached the harness world", reached, detail)
