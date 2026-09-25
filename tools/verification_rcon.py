"""Run commands only against this project's disposable localhost harness server.

Reads rcon.port and rcon.password from build/harness-server/server.properties.
That server is 25576 / rcon 25586. Tribal Power's showcase server (25585 / 25595) is a different process.

One authenticated connection is kept open and reused. A run sends thousands of commands, and a fresh
socket per command used to exhaust Windows' local ports (WinError 10048) halfway through the breed
ladder. A dropped connection is reopened once and the command retried.
"""
from pathlib import Path
import socket
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]
_SOCK = [None]


def _props():
    return dict(
        line.split("=", 1)
        for line in (ROOT / "build/harness-server/server.properties").read_text(encoding="utf-8").splitlines()
        if "=" in line and not line.startswith("#")
    )


def _read(sock, n):
    data = b""
    while len(data) < n:
        part = sock.recv(n - len(data))
        if not part:
            raise ConnectionError("RCON closed")
        data += part
    return data


def _receive(sock):
    size = struct.unpack("<i", _read(sock, 4))[0]
    data = _read(sock, size)
    return struct.unpack("<ii", data[:8]), data[8:-2].decode(errors="replace")


def _send(sock, kind, body):
    payload = struct.pack("<ii", 42, kind) + body.encode() + b"\0\0"
    sock.sendall(struct.pack("<i", len(payload)) + payload)


def _open():
    props = _props()
    sock = socket.create_connection(("127.0.0.1", int(props["rcon.port"])), timeout=15)
    _send(sock, 3, props["rcon.password"])
    header, _ = _receive(sock)
    if header[0] < 0:
        sock.close()
        raise RuntimeError("Harness RCON authentication failed")
    return sock


def close():
    if _SOCK[0] is not None:
        try:
            _SOCK[0].close()
        except OSError:
            pass
        _SOCK[0] = None


def command(text):
    for attempt in (0, 1):
        try:
            if _SOCK[0] is None:
                _SOCK[0] = _open()
            _send(_SOCK[0], 2, text)
            return _receive(_SOCK[0])[1]
        except (OSError, ConnectionError):
            close()
            if attempt == 1:
                raise
    return ""


if __name__ == "__main__":
    try:
        print(command(" ".join(sys.argv[1:])))
    finally:
        close()
