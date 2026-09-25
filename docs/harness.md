# Live harness

A real dedicated server and a real client, driven from Python, that photograph every mount and breed
a chimera from the ten wild lines. This is the gate before a release: `boot`, `items`, `book`,
`looks`, `ride`, and `breed` must all pass.

## Running it

```
python tools/harness_up.py          # start or restart the server and the client, wait for ShamanQA
python tools/system_harness.py      # every section, in order
python tools/system_harness.py --only looks,ride
```

A server restart also clears the saved herd book, so `book` sees an empty herd. `harness_up.py --client` restarts only the client after a client-side change (renderer, textures,
screens). `--server` restarts only the server after an entity or genome change. The server is a
flat world on 127.0.0.1:25576 with RCON on 25586; the client is `ShamanQA`, 1280x800, and joins by
itself. Tribal Power's showcase server uses other ports and is never touched.

## Sections

- `boot`: both sides loaded the mod, the dedicated server pulled no client class, recipes parsed.
- `items`: names and textures of the saddle, herd book, and diamond apple, the mod's creative tab, and
  their recipes at a real crafting table.
- `book`: every page of the herd book, the spoiler switch and its saved file, rename, and release.
- `looks`: each of the eight lines, the chimera, and a hart-over-eightfold cross, alone on a pad, from
  the front, the side, a low three-quarter, a close side profile and front view of the face, and two
  held points of the stride. Pictures only; a person judges them.
- `ride`: each line saddled and mounted with the use key, photographed walking from the front and
  from behind, the fliers lifting off on a held jump, a sneak tap dismounting, and the hound sitting.
- `genes`: crosses and gene extremes on the pad, photographed as `look_<case>_gene`: steed over serpent,
  bear over steed, hart over bird, an XS and an XL eightfold, and a palomino; checks that size moves health.
- `gifts`: every ridden ability fired once from the saddle, with its effect checked in the world:
  the drum's Regeneration, the elk's ram on a pig, the nagual's send-away and return on damage, the
  hound's glow on a zombie at night, the shade's held-sneak hide, blink, and sneak-tap dismount, the
  roc's climb draining its bar, and the chimera doing the drum and the blink.
- `bags`: the mount screen opened with sneak and use, Saddle Bags strapped on and taken off again,
  an item stowed and tipped out, iron horse armor buckled on for its five armor, the elk's third row, the follow, stay, and wander buttons
  changing what the mount does, and a tame foal's tack slots refusing a saddle and armor.
- `breed`: ten founders with one gift each, a ladder of Diamond Apple matings that walks the gift
  chromosome until both strands carry every gift, then the chimera coin. Every foal must be born wild;
  the ladder hands each one to the player so it can be a parent. Ends with a picture of the
  chimera foal.
- `store` (not in the default order): the founders-sheet pictures for `tools/founders_sheet.py`, four
  body plans lying down on stay, and the eightfold on a lime stage for `tools/store_cover.py`.

Pictures land in `build/harness-client/screenshots/showcase-<n>-<label>.png`. The counter resets
when the client restarts.

## Client commands

The client polls `build/harness-client/showcase-command.txt` twice a second and answers in
`showcase-done.txt`. Beyond the item and screen commands, the rig can be driven with:

- `key <forward|back|left|right|jump|sneak|use|attack> <down|up>`: hold or release a movement key.
- `camera <first|back|front>`: the third-person camera for riding pictures.
- `riding`: the vehicle's id, type, whether it is on the ground, and its height.
- `swing`: swing the main hand, which is how the elk's ram is asked for.
- `creative`: open the creative inventory on the Shamanic Mounts tab and list what it holds.
- `hud <on|off>`: hide or show the crosshair, hand, and hotbar.
- `pose <swing> <amount>` and `pose off`: hold every mount at one point of its stride.
- `wing <1|2>` and `wing off`: hold every mount in the air, gliding or flapping.
- `pick <n>`: in the herd book, open tame n, line n, or on a tame's page relative n (0 dam, 1 sire, then foals).
- `bookscroll end|top`: scroll the open book page to its end or back to the top.
- `bookenter`: press Enter in the open tame's name box.
- `dump`: write every quad the nearest mount draws, with its atlas cell, to `showcase-dump.txt`. Use it
  when a face looks wrong: a black patch seen head-on is usually the nose pad, not a hole.
