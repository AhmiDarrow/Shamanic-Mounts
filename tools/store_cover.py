"""Put the shipped eightfold, a gliding crane, and a shade on the approved cover, in place of the first-draft steed.

    python tools/store_cover.py [--preview]
    python tools/brand_icons.py

The store section of the harness (python tools/system_harness.py --only store) photographs the
eightfold on a lime concrete stage. This keys the lime out, removes the old steed from the cover
by colour and inpainting, grades the mount toward the cover's night light, and sets it on the
island inside the ring. The untouched original is kept as art/branding/shamanic-mounts-logo-v1.png,
and the result replaces the master that brand_icons.py resamples.
"""
import glob
import os
import shutil
import sys
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SHOTS = ROOT / "build" / "harness-client" / "screenshots"
BRAND = ROOT / "art" / "branding"
MASTER = BRAND / "shamanic-mounts-logo.png"
ORIGINAL = BRAND / "shamanic-mounts-logo-v1.png"
# The old steed's box on the 1024 cover, and where the new mount's feet land.
STEED_BOX = (270, 130, 760, 672)
# The gold ring: centre and inner radius, measured on the original.
RING = (508, 371, 298)
FEET_Y = 652
CENTRE_X = 505


def latest(label):
    files = glob.glob(str(SHOTS / f"showcase-*-{label}.png"))
    if not files:
        raise SystemExit(f"no picture {label}; run the store section first")
    return max(files, key=os.path.getmtime)


def keyed(label):
    """The mount alone, as RGBA, cropped to its pixels. Lime and loose idle specks are dropped."""
    frames = [np.array(Image.open(latest(label)).convert("RGB"))]
    for frame in ("b", "c"):
        if glob.glob(str(SHOTS / f"showcase-*-{label}_{frame}.png")):
            frames.append(np.array(Image.open(latest(f"{label}_{frame}")).convert("RGB")))
    # The idle motes drift between frames; the median of three keeps only what stood still.
    rgb = np.median(np.stack(frames), axis=0).astype(np.uint8) if len(frames) == 3 else frames[0]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    # Lime concrete and grass sit near hue 45; teal feathers near 90 must survive.
    lime = (hsv[..., 0] >= 30) & (hsv[..., 0] <= 70) & (hsv[..., 1] >= 90)
    # Any sky that shows past the stage is keyed too; nothing on the mount is sky blue.
    lime |= (hsv[..., 0] >= 95) & (hsv[..., 0] <= 125) & (hsv[..., 1] >= 40) & (hsv[..., 2] >= 150)
    keep = (~lime).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(keep, connectivity=8)
    if count < 2:
        raise SystemExit(f"nothing but lime in {label}")
    biggest = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    x, y, w, h = stats[biggest, :4]
    # Keep only large pieces near the mount; the white idle motes are small and loose.
    alpha = np.zeros(keep.shape, np.uint8)
    for index in range(1, count):
        piece = labels == index
        motes = hsv[..., 1][piece].mean() < 50 and hsv[..., 2][piece].mean() > 140
        if index == biggest or (stats[index, cv2.CC_STAT_AREA] >= 400 and not motes):
            alpha[piece] = 255
    # Motes that drifted against the outline: pale pixels near the edge go, eye highlights stay.
    pale = (hsv[..., 1] < 40) & (hsv[..., 2] > 180)
    near_edge = cv2.dilate((alpha == 0).astype(np.uint8), np.ones((13, 13), np.uint8)) > 0
    alpha[pale & near_edge] = 0
    ys, xs = np.nonzero(alpha)
    top, bottom, left, right = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
    rgba = np.dstack([rgb, alpha])[top:bottom, left:right]
    return Image.fromarray(rgba, "RGBA")


def steed_mask(cover):
    """Where the old steed is: brown, its grey hoof bands, and its dark hooves, inside the ring only."""
    arr = np.array(cover).astype(np.int16)
    r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
    value = np.maximum(np.maximum(r, g), b)
    yy, xx = np.mgrid[:arr.shape[0], :arr.shape[1]]
    inside = np.hypot(xx - RING[0], yy - RING[1]) < RING[2] - 8
    island = (yy >= 560) & (yy <= STEED_BOX[3]) & (xx >= STEED_BOX[0] + 30) & (xx <= STEED_BOX[2] - 20)
    zone = (inside | island) & (yy >= STEED_BOX[1]) & (yy <= STEED_BOX[3])
    brown = r > b + 14
    grey = (abs(r - b) < 16) & (abs(g - b) < 16) & (r > 110)
    # The mane and hooves are near black but warm; the night sky is cool.
    hooves = (value < 110) & (((r >= b - 8) & (yy < 470)) | ((r >= b - 2) & (yy >= 470) & (value < 90)))
    mask = ((brown | grey | hooves) & zone).astype(np.uint8) * 255
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, np.ones((25, 25), np.uint8))
    flood = mask.copy()
    cv2.floodFill(flood, np.zeros((mask.shape[0] + 2, mask.shape[1] + 2), np.uint8), (0, 0), 255)
    mask = mask | cv2.bitwise_not(flood)
    mask = cv2.dilate(mask, np.ones((11, 11), np.uint8))
    mask[~zone] = 0
    return mask


def grade(mount):
    """Toward the cover's night: a little darker and cooler, with a teal edge from the aurora."""
    arr = np.array(mount).astype(np.float32)
    rgb, alpha = arr[..., :3], arr[..., 3:]
    night = np.array([36, 52, 96], np.float32)
    rgb = rgb * 0.86 + night * 0.10
    # A rim of aurora light along the top edges.
    a = (alpha[..., 0] > 0).astype(np.uint8)
    shifted = np.zeros_like(a)
    shifted[3:, :] = a[:-3, :]
    rim = ((a == 1) & (shifted == 0)).astype(np.float32)
    rim = cv2.GaussianBlur(rim, (0, 0), 1.2)[..., None]
    rgb = rgb * (1 - rim * 0.35) + np.array([110, 230, 200], np.float32) * rim * 0.35
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), alpha]).astype(np.uint8), "RGBA")


# The cast: picture label, width on the cover, centre x, bottom y, haze toward the sky, shadow, behind the ring,
# and a forward tilt in degrees (the rig holds a crane upright unless it is really climbing).
CAST = (
    ("look_crane_cover", 210, 170, 225, 0.22, False, True, 38),
    ("look_eightfold_cover", 540, 505, FEET_Y, 0.0, True, False, 0),
    ("look_shade_cover", 185, 845, 678, 0.0, True, False, 0),
)


def ring_pixels(cover):
    """The gold of the ring, so a mount flying behind it can be tucked under it again."""
    arr = np.array(cover).astype(np.int16)
    r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
    yy, xx = np.mgrid[:arr.shape[0], :arr.shape[1]]
    distance = np.hypot(xx - RING[0], yy - RING[1])
    gold = (r > 70) & (r > b + 25) & (g > 50)
    band = (distance > RING[2] - 6) & (distance < RING[2] + 42)
    mask = (gold & band).astype(np.uint8) * 255
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, np.ones((7, 7), np.uint8))
    return Image.fromarray(mask)


def place(base, label, width, centre_x, bottom, haze, shadow, tilt):
    mount = keyed(label)
    if tilt:
        mount = mount.rotate(tilt, resample=Image.Resampling.NEAREST, expand=True)
        mount = mount.crop(mount.getbbox())
    mount = mount.resize((width, round(mount.height * width / mount.width)), Image.Resampling.NEAREST)
    mount = grade(mount)
    if haze:
        arr = np.array(mount).astype(np.float32)
        sky = np.array([40, 70, 120], np.float32)
        arr[..., :3] = arr[..., :3] * (1 - haze) + sky * haze
        mount = Image.fromarray(arr.astype(np.uint8), "RGBA")
    x = centre_x - mount.width // 2
    y = bottom - mount.height
    if shadow:
        # A soft shadow where the feet meet the ground.
        layer = Image.new("L", base.size, 0)
        alpha = mount.getchannel("A").resize((mount.width, max(1, mount.height // 8)))
        layer.paste(alpha, (x + mount.width // 60, bottom - alpha.height // 2))
        layer = layer.filter(ImageFilter.GaussianBlur(max(3, mount.width // 50))).point(lambda v: int(v * 0.55))
        base.paste(Image.new("RGBA", base.size, (4, 6, 14, 255)), (0, 0), layer)
    base.alpha_composite(mount, (x, y))


def compose(preview):
    if not ORIGINAL.exists():
        shutil.copy2(MASTER, ORIGINAL)
    cover = Image.open(ORIGINAL).convert("RGB")
    mask = steed_mask(cover)
    bgr = cv2.cvtColor(np.array(cover), cv2.COLOR_RGB2BGR)
    filled = cv2.inpaint(bgr, mask, 9, cv2.INPAINT_TELEA)
    # Soften the fill and feather its edge, then give it the painting's grain back.
    soft = cv2.GaussianBlur(filled, (0, 0), 9)
    feather = cv2.GaussianBlur(mask.astype(np.float32) / 255.0, (0, 0), 6)[..., None]
    filled = (filled * (1 - feather) + soft * feather).astype(np.float32)
    grain = np.random.default_rng(7).normal(0, 4, filled.shape).astype(np.float32)
    filled = np.clip(filled + grain * feather, 0, 255).astype(np.uint8)
    base = Image.fromarray(cv2.cvtColor(filled, cv2.COLOR_BGR2RGB)).convert("RGBA")
    ring = ring_pixels(cover)
    for label, width, centre_x, bottom, haze, shadow, behind, tilt in CAST:
        place(base, label, width, centre_x, bottom, haze, shadow, tilt)
        if behind:
            base.paste(cover.convert("RGBA"), (0, 0), ring)
    if preview:
        out = BRAND / "cover-preview.png"
        base.convert("RGB").save(out)
        Image.fromarray(mask).save(BRAND / "cover-mask-preview.png")
        print(out)
        return
    base.convert("RGB").save(MASTER)
    print(MASTER, "written; now run python tools/brand_icons.py")


if __name__ == "__main__":
    compose("--preview" in sys.argv)
