"""Herd book pages, the local spoiler file, rename, and release.

The item itself opens an empty herd until a mount exists in the world. `seed` opens a book that already
holds Brook and Ash so the tame page can be driven on this client.
"""
import time

from harness_lib import CLIENT, PLAYER, buttons, check, client, player_uuid, press_button, report, run

PREF = CLIENT / "config" / "shamanicmounts" / "spoilers.txt"


def lines_of(fields):
    return fields.get("lines", "")


def main():
    if PREF.is_file():
        PREF.unlink()
    uuid = player_uuid()
    run(f"clear {PLAYER}")
    run(f"item replace entity {PLAYER} hotbar.0 with shamanicmounts:herd_book 1")
    client("hotbar 0")
    time.sleep(0.2)
    opened = client("useitem")
    time.sleep(0.4)
    state = client("state")
    check("using the herd book opens it", "screen=HerdBookScreen" in state, f"{opened} {state[:160]}")
    if "screen=HerdBookScreen" not in state:
        return

    _reply, widgets = buttons()
    labels = [text for _index, kind, text in widgets if kind == "Button"]
    check("spoilers off shows Basics, Lines, Tames, and Key only", labels[:4] == ["Basics", "Lines", "Tames", "Key"] and "Breeding" not in labels, str(labels))
    check("the spoiler button starts off", "Spoilers: off" in labels, str(labels))
    _reply, fields = report()
    basics = lines_of(fields)
    check("the basics page names the saddle and the four jolts", "Shamanic Saddle" in basics and "four jolts" in basics, basics[:180])
    check("the basics page does not explain the chimera", "chimera" not in basics.lower() and "both copies" not in basics.lower())
    client("shot book_basics")

    press_button("Lines")
    _reply, fields = report()
    lines_page = lines_of(fields)
    check("the lines page names every founder and the chimera", fields.get("page") == "LINES" and "Eightfold" in lines_page
          and "Chimera" in lines_page and "Barghest" in lines_page, lines_page[:120])
    check("the chimera's line is hidden while spoilers are off", "The eleventh form." in lines_page, lines_page[-80:])
    client("shot book_lines")
    picked = client("pick 0")
    _reply, fields = report()
    detail = lines_of(fields)
    check("a line opens onto its three pelts and founder genes", picked.startswith("ok") and fields.get("line") == "Eightfold"
          and "Pelts: Bay, Black, Palomino" in detail and "Lg EE" in detail, detail[:200])
    client("shot book_line_detail")
    client("bookscroll end")
    time.sleep(0.2)
    client("shot book_line_genes")
    press_button("Back")
    _reply, fields = report()
    check("Back returns to the gallery", fields.get("line") == "" and fields.get("page") == "LINES", fields.get("line", ""))
    check("the chimera cannot be opened with spoilers off", client("pick 10").startswith("error"))

    press_button("Key")
    _reply, fields = report()
    key_page = lines_of(fields)
    check("the key explains the notation and lists the body loci", fields.get("page") == "KEY" and "capital letter shows" in key_page
          and "Lg legs:" in key_page and "E eight" in key_page, key_page[:120])
    check("the key lists size, pelt, and body dominance", "Sz size: XS xs" in key_page and "Pt pelt: A first" in key_page
          and "U > S > H > D > C > B > N" in key_page, key_page[:400])
    check("the key keeps the gifts for spoilers", "Ro road" not in key_page)
    client("shot book_key")

    press_button("Tames")
    _reply, fields = report()
    check(
        "the item's herd is empty until a mount exists",
        fields.get("page") == "TAMES" and "No tames in the book yet." in lines_of(fields),
        lines_of(fields)[:120],
    )

    press_button("Spoilers: off")
    time.sleep(0.2)
    _reply, widgets = buttons()
    labels = [text for _index, kind, text in widgets if kind == "Button"]
    check("spoilers on adds the Breeding chapter", "Breeding" in labels and "Spoilers: on" in labels, str(labels))
    saved = PREF.read_text(encoding="utf-8") if PREF.is_file() else ""
    check("spoiler on is saved for this player", f"{uuid} true" in saved, saved.strip())
    press_button("Key")
    _reply, fields = report()
    check("with spoilers on the key lists the gifts", "Ro road" in lines_of(fields), lines_of(fields)[-100:])
    press_button("Breeding")
    _reply, fields = report()
    breeding = lines_of(fields)
    check(
        "the breeding page explains both copies and the chimera",
        fields.get("page") == "BREEDING" and "both copies" in breeding and "chimera" in breeding.lower(),
        breeding[:200],
    )
    check(
        "the breeding page says to feed a Diamond Apple",
        "Diamond Apple" in breeding and "five minutes" in breeding,
        breeding[:240],
    )
    client("shot book_breeding")

    press_button("Spoilers: on")
    time.sleep(0.2)
    saved = PREF.read_text(encoding="utf-8") if PREF.is_file() else ""
    _reply, widgets = buttons()
    labels = [text for _index, kind, text in widgets if kind == "Button"]
    check("spoiler off is saved, and Breeding hides again", f"{uuid} false" in saved and "Breeding" not in labels, saved.strip() + " " + str(labels))
    client("close")

    seeded = client("seed")
    time.sleep(0.3)
    check("a seeded herd opens", seeded.startswith("ok"), seeded)
    press_button("Tames")
    _reply, fields = report()
    check("the tame list names Brook and Ash", fields.get("tames") == "Brook,Ash", fields.get("tames", ""))
    picked = client("pick 1")
    check("clicking Ash opens that tame", picked.startswith("ok") and "selected=Ash" in picked, picked[:200])
    _reply, fields = report()
    hidden = lines_of(fields)
    check("with spoilers off the carried skin reads Sk s- none with no note", "Sk s-  none" in hidden and "carried" not in hidden, hidden[:240])
    check("the family line names the dam and the coat", "Dam: Brook" in hidden and "Pelt: " in hidden, hidden[:120])
    check("the tame page sums up the body, pelt, size, and sex", "Shade body, Dusk, size M, female" in hidden, hidden[:120])

    press_button("Spoilers: off")
    time.sleep(0.2)
    _reply, fields = report()
    shown = lines_of(fields)
    check("with spoilers on the carried skin says carried", "carried" in shown and fields.get("spoilers") == "true", shown[:240])
    client("shot book_tame")
    client("bookscroll end")
    time.sleep(0.2)
    client("shot book_tame_genes")
    client("bookscroll top")
    linked = client("pick 0")
    check("the dam's name links to her page", linked.startswith("ok") and "selected=Brook" in linked, linked[:160])
    press_button("Back")
    client("pick 1")

    client("type Ashen Step")
    renamed = press_button("Rename")
    _reply, fields = report()
    check("rename stores Ashen Step", renamed.startswith("ok") and fields.get("selected") == "Ashen Step", fields.get("selected", ""))
    client("type Ash Again")
    entered = client("bookenter")
    _reply, fields = report()
    check("Enter in the name box renames too", entered.startswith("ok") and fields.get("selected") == "Ash Again", fields.get("selected", ""))
    client("type Ashen Step")
    press_button("Rename")
    client("type    ")
    press_button("Rename")
    _reply, fields = report()
    check("a blank rename is refused", fields.get("selected") == "Ashen Step", fields.get("selected", ""))

    press_button("Back")
    _reply, fields = report()
    check("the list shows the new name", "Ashen Step" in fields.get("tames", "") and fields.get("selected", "") == "", fields.get("tames", ""))

    picked = client("pick 0")
    check("clicking Brook opens that tame", picked.startswith("ok") and "selected=Brook" in picked, picked[:160])
    press_button("Release")
    _reply, fields = report()
    check("the first release asks again", fields.get("confirm") == "true", fields.get("confirm", ""))
    sure = press_button("Release?")
    _reply, fields = report()
    check("release drops Brook off the tame list", sure.startswith("ok") and fields.get("tames") == "Ashen Step", fields.get("tames", ""))
    client("pick 0")
    _reply, fields = report()
    check("Ashen Step can still name the released dam", "Dam: Brook" in lines_of(fields), lines_of(fields)[:160])
    client("close")
