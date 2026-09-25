"""Saddle and herd book: names, textures, and a real crafting table."""
import struct
import time
import zlib

from harness_lib import PLAYER, ROOT, check, client, inventory_count, log_text, run

TABLE = (10, 101, 8)


def png_facts(path):
    """Color type and how the pixels read. Color type 6 is RGBA, 4 is grey+alpha."""
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return {"error": "not a png"}
    index = 8
    width = height = color = None
    idat = b""
    while index < len(data):
        length = struct.unpack(">I", data[index : index + 4])[0]
        kind = data[index + 4 : index + 8]
        chunk = data[index + 8 : index + 8 + length]
        if kind == b"IHDR":
            width, height, _depth, color = struct.unpack(">IIBB", chunk[:10])
        elif kind == b"IDAT":
            idat += chunk
        elif kind == b"IEND":
            break
        index += 12 + length
    bpp = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color]
    raw = zlib.decompress(idat)
    stride = width * bpp
    rows = []
    prev = bytearray(stride)
    offset = 0

    def paeth(left, up, up_left):
        estimate = left + up - up_left
        distances = (abs(estimate - left), abs(estimate - up), abs(estimate - up_left))
        return (left, up, up_left)[distances.index(min(distances))]

    for _y in range(height):
        filt = raw[offset]
        offset += 1
        row = bytearray(raw[offset : offset + stride])
        offset += stride
        if filt == 1:
            for x in range(stride):
                row[x] = (row[x] + (row[x - bpp] if x >= bpp else 0)) & 255
        elif filt == 2:
            for x in range(stride):
                row[x] = (row[x] + prev[x]) & 255
        elif filt == 3:
            for x in range(stride):
                row[x] = (row[x] + (((row[x - bpp] if x >= bpp else 0) + prev[x]) // 2)) & 255
        elif filt == 4:
            for x in range(stride):
                row[x] = (row[x] + paeth(row[x - bpp] if x >= bpp else 0, prev[x], prev[x - bpp] if x >= bpp else 0)) & 255
        rows.append(row)
        prev = row
    transparent = opaque = near_black = 0
    for row in rows:
        for x in range(0, stride, bpp):
            pixel = row[x : x + bpp]
            alpha = pixel[-1] if color in (4, 6) else 255
            if alpha == 0:
                transparent += 1
            else:
                opaque += 1
                if max(pixel[:3]) < 16:
                    near_black += 1
    return {
        "width": width,
        "height": height,
        "color": color,
        "transparent": transparent,
        "opaque": opaque,
        "near_black": near_black,
    }


def place(slot, menu_slot, grid_slot):
    """Move one hotbar stack onto one crafting-grid slot."""
    client(f"click {menu_slot} 0 PICKUP")
    client(f"click {grid_slot} 0 PICKUP")


def main():
    run(f"clear {PLAYER}")
    run(f"item replace entity {PLAYER} hotbar.0 with shamanicmounts:shamanic_saddle 1")
    run(f"item replace entity {PLAYER} hotbar.1 with shamanicmounts:herd_book 1")
    client("hotbar 0")
    time.sleep(0.3)
    saddle_name = client("name")
    check("the saddle's name is Shamanic Saddle", saddle_name == "ok Shamanic Saddle", saddle_name)
    client("shot held_saddle")
    client("hotbar 1")
    time.sleep(0.3)
    book_name = client("name")
    check("the herd book's name is Herd Book", book_name == "ok Herd Book", book_name)
    client("shot held_book")
    check("the player is holding one of each", inventory_count("shamanicmounts:shamanic_saddle") == 1 and inventory_count("shamanicmounts:herd_book") == 1)

    for name in ("shamanic_saddle.png", "saddle_bags.png", "herd_book.png", "diamond_apple.png"):
        path = ROOT / "src/main/resources/assets/shamanicmounts/textures/item" / name
        facts = png_facts(path)
        check(
            f"{name} has an alpha channel and is not a black square",
            facts.get("color") in (4, 6)
            and facts.get("transparent", 0) > 0
            and facts.get("opaque", 0) > 0
            and facts.get("near_black", 0) < facts.get("opaque", 1),
            str(facts),
        )

    tab = client("creative")
    time.sleep(0.4)
    check(
        "the Shamanic Mounts creative tab lists the saddle, the book, the apple, and ten eggs",
        tab.startswith("ok Shamanic Mounts") and tab.count("egg_") == 10 and "shamanic_saddle" in tab and "herd_book" in tab
        and "diamond_apple" in tab and "saddle_bags" in tab,
        tab[:160],
    )
    client("shot creative_tab")
    client("close")

    client_log = log_text("build/harness-client/logs/latest.log")
    check(
        "the client did not report a missing shamanicmounts texture",
        "Missing textures" not in client_log and "unable to load shamanicmounts:" not in client_log,
    )

    craft_saddle()
    craft_bags()
    craft_book()
    craft_apple()


def craft_bags():
    run(f"clear {PLAYER}")
    run(f"item replace entity {PLAYER} hotbar.0 with minecraft:leather 4")
    run(f"item replace entity {PLAYER} hotbar.1 with minecraft:chest 1")
    client("hotbar 0")
    _used, state = open_table()
    check("the crafting table opens for the bags", "screen=CraftingScreen" in state, state[:180])
    if "screen=CraftingScreen" not in state:
        return
    client("click 37 0 PICKUP")
    for slot in (2, 4, 6, 8):
        client(f"click {slot} 1 PICKUP")
        time.sleep(0.1)
    client("click 37 0 PICKUP")
    client("click 38 0 PICKUP")
    client("click 5 0 PICKUP")
    time.sleep(0.5)
    state = client("state")
    check("four leather around a chest craft saddle bags", "0:shamanicmounts:saddle_bagsx1" in state, state[:300])
    client("click 0 0 QUICK_MOVE")
    time.sleep(0.3)
    check("the crafted bags land in the inventory", inventory_count("shamanicmounts:saddle_bags") >= 1)
    client("close")


def open_table():
    client("close")
    client(f"look {TABLE[0] + 0.5} {TABLE[1] + 0.5} {TABLE[2] + 0.5}")
    used = client(f"use {TABLE[0]} {TABLE[1]} {TABLE[2]}")
    time.sleep(0.4)
    state = client("state")
    return used, state


def craft_saddle():
    run(f"clear {PLAYER}")
    run(f"item replace entity {PLAYER} hotbar.0 with minecraft:gold_ingot 1")
    run(f"item replace entity {PLAYER} hotbar.1 with minecraft:leather 2")
    run(f"item replace entity {PLAYER} hotbar.2 with minecraft:saddle 1")
    client("hotbar 0")
    used, state = open_table()
    check("the crafting table opens", "screen=CraftingScreen" in state, state[:220])
    if "screen=CraftingScreen" not in state:
        return
    # Grid is slots 1-9, hotbar starts at 37. Pattern is " G " / "LSL" on the top two rows.
    place(0, 37, 2)
    client("click 38 0 PICKUP")
    client("click 4 1 PICKUP")
    client("click 6 1 PICKUP")
    client("click 38 0 PICKUP")
    place(2, 39, 5)
    time.sleep(0.3)
    state = client("state")
    check(
        "gold, leather, and a vanilla saddle craft a shamanic saddle",
        "0:shamanicmounts:shamanic_saddlex1" in state,
        state[:300],
    )
    client("click 0 0 QUICK_MOVE")
    time.sleep(0.3)
    check("the crafted saddle lands in the inventory", inventory_count("shamanicmounts:shamanic_saddle") >= 1)
    client("close")


def craft_book():
    run(f"clear {PLAYER}")
    run(f"item replace entity {PLAYER} hotbar.0 with minecraft:book 1")
    run(f"item replace entity {PLAYER} hotbar.1 with minecraft:gold_ingot 1")
    client("hotbar 0")
    _used, state = open_table()
    check("the crafting table opens again", "screen=CraftingScreen" in state, state[:180])
    if "screen=CraftingScreen" not in state:
        return
    place(0, 37, 1)
    place(1, 38, 2)
    time.sleep(0.3)
    state = client("state")
    check(
        "a book and a gold ingot craft a herd book",
        "0:shamanicmounts:herd_bookx1" in state,
        state[:300],
    )
    client("click 0 0 QUICK_MOVE")
    time.sleep(0.3)
    check("the crafted herd book lands in the inventory", inventory_count("shamanicmounts:herd_book") >= 1)
    client("shot crafted_book")
    client("close")


def craft_apple():
    run(f"clear {PLAYER}")
    run(f"item replace entity {PLAYER} hotbar.0 with minecraft:diamond 8")
    run(f"item replace entity {PLAYER} hotbar.1 with minecraft:apple 1")
    client("hotbar 0")
    _used, state = open_table()
    check("the crafting table opens for the apple", "screen=CraftingScreen" in state, state[:180])
    if "screen=CraftingScreen" not in state:
        return
    client("click 37 0 PICKUP")
    for slot in (1, 2, 3, 4, 6, 7, 8, 9):
        client(f"click {slot} 1 PICKUP")
    client("click 37 0 PICKUP")
    client("click 38 0 PICKUP")
    client("click 5 0 PICKUP")
    time.sleep(0.3)
    state = client("state")
    check(
        "eight diamonds around an apple craft a diamond apple",
        "0:shamanicmounts:diamond_applex1" in state,
        state[:300],
    )
    client("click 0 0 QUICK_MOVE")
    time.sleep(0.3)
    check("the crafted diamond apple lands in the inventory", inventory_count("shamanicmounts:diamond_apple") >= 1)
    client("close")
    run(f"item replace entity {PLAYER} hotbar.0 with shamanicmounts:diamond_apple 1")
    client("hotbar 0")
    time.sleep(0.2)
    held = client("name")
    check("the apple's name is Diamond Apple", held == "ok Diamond Apple", held)
    client("shot held_apple")
