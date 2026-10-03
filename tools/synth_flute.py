"""Mount Flute calls, synthesised (original audio, nothing sampled).

A wooden end-blown flute: a soft fundamental with weak upper harmonics, airy
breath noise that tracks each note, a chiff where a breath starts, slurred
glides between notes, slow vibrato that blooms on held notes, and a small
room. Four short phrases in D minor pentatonic; the flute plays them in turn.

    python tools/synth_flute.py        # writes sounds/item/flute/call_<n>.ogg (ffmpeg)
    python tools/synth_flute.py --wav  # keep .wav next to them for listening
"""
from __future__ import annotations

import shutil
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np
from scipy import signal

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/shamanicmounts/sounds/item/flute"
RATE = 44100
RNG = np.random.default_rng(11)

NOTE = {"A4": 440.00, "C5": 523.25, "D5": 587.33, "E5": 659.26, "F5": 698.46, "G5": 783.99, "A5": 880.00}

# Each phrase is a list of (note, seconds, breath-after). Notes inside one breath are slurred;
# a breath gap re-tongues the next note with a fresh chiff.
PHRASES = {
    # rising call, held top
    "call_1": [("A4", 0.18, 0), ("D5", 0.18, 0), ("F5", 0.75, 0)],
    # falling answer that comes home
    "call_2": [("F5", 0.16, 0), ("E5", 0.14, 0), ("D5", 0.16, 0), ("A4", 0.22, 0.06), ("D5", 0.6, 0)],
    # two high pushes, then down
    "call_3": [("G5", 0.2, 0.05), ("A5", 0.32, 0), ("G5", 0.12, 0), ("F5", 0.55, 0)],
    # grace-note lift into a long low note
    "call_4": [("C5", 0.07, 0), ("D5", 0.35, 0.05), ("A4", 0.14, 0), ("C5", 0.14, 0), ("D5", 0.7, 0)],
}


def phrase(notes) -> np.ndarray:
    freq, level, starts, holds = [], [], [], []
    at, new_breath = 0, True
    for name, seconds, gap in notes:
        n = int(seconds * RATE)
        if new_breath:
            starts.append(at)
        freq.append(np.full(n, NOTE[name]))
        level.append(np.ones(n))
        holds.append((at, n))
        at += n
        new_breath = gap > 0
        if gap > 0:
            g = int(gap * RATE)
            freq.append(np.full(g, NOTE[name]))
            level.append(np.zeros(g))
            at += g
    freq = np.concatenate(freq)
    level = np.concatenate(level)
    n = len(freq)
    t = np.arange(n) / RATE
    # Slur: pitch moves to each new note over ~25 ms instead of jumping.
    k = 0.9991
    freq = signal.lfilter([1 - k], [1, -k], freq, zi=[freq[0] * k])[0]
    # Breath envelope: 35 ms swell, 90 ms let-go, a slight sag across long notes.
    rise = signal.lfilter([1 - 0.99935], [1, -0.99935], level)
    fall = signal.lfilter([1 - 0.99975], [1, -0.99975], level[::-1])[::-1]
    amp = np.clip(np.minimum(rise, fall) * 1.1, 0, 1)
    # Vibrato blooms only once a note has been held ~0.2 s.
    bloom = np.zeros(n)
    for s, m in holds:
        bloom[s:s + m] = np.clip((np.arange(m) / RATE - 0.2) / 0.25, 0, 1)
    vib = 1.0 + 0.009 * np.sin(2 * np.pi * 5.0 * t) * bloom
    amp = amp * (1.0 + 0.08 * np.sin(2 * np.pi * 5.0 * t + 0.6) * bloom)
    phase = 2 * np.pi * np.cumsum(freq * vib) / RATE
    tone = np.sin(phase) + 0.22 * np.sin(2 * phase + 0.3) + 0.09 * np.sin(3 * phase + 0.8) + 0.03 * np.sin(4 * phase)
    # Airy breath: slow noise riding the note's own phase is a narrow band that follows every slur
    # without any filter switching, plus a faint fixed band of air.
    noise = RNG.standard_normal(n)
    lb, la = signal.butter(2, 180 / (RATE / 2))
    slow = signal.lfilter(lb, la, RNG.standard_normal(n))
    slow /= np.std(slow) + 1e-9
    breath = slow * np.sin(phase) + 0.5 * signal.lfilter(lb, la, RNG.standard_normal(n)) / (np.std(slow) + 1e-9) * np.sin(2 * phase)
    breath /= np.std(breath) + 1e-9
    ab, aa = signal.butter(2, [1200 / (RATE / 2), 4000 / (RATE / 2)], btype="band")
    hiss = signal.lfilter(ab, aa, noise) * 0.02
    # Chiff where each breath starts.
    puff = np.zeros(n)
    for s in starts:
        m = min(int(0.04 * RATE), n - s)
        puff[s:s + m] = np.exp(-np.arange(m) / (0.012 * RATE))
    cb, ca = signal.butter(2, [900 / (RATE / 2), 3500 / (RATE / 2)], btype="band")
    chiff = puff * signal.lfilter(cb, ca, noise) * 0.25
    dry = amp * (tone + 0.09 * breath + hiss) + chiff
    return room(dry)


def room(dry: np.ndarray) -> np.ndarray:
    """A soft, slightly longer tail than the whistle: the flute is played, not blown at."""
    tail = int(0.8 * RATE)
    ir = RNG.standard_normal(tail) * np.exp(-np.arange(tail) / (0.16 * RATE))
    b, a = signal.butter(2, 2800 / (RATE / 2))
    ir = signal.lfilter(b, a, ir)
    ir[0] = 0.0
    wet = np.zeros(len(dry) + tail)
    full = signal.fftconvolve(dry, ir)
    wet[: len(full)] = full[: len(wet)]
    wet /= np.max(np.abs(wet)) + 1e-9
    out = np.concatenate([dry, np.zeros(tail)]) + 0.22 * wet
    fade = int(0.12 * RATE)
    out[-fade:] *= np.linspace(1, 0, fade)
    return out / (np.max(np.abs(out)) + 1e-9) * 0.89


def write_wav(path: Path, data: np.ndarray):
    pcm = (np.clip(data, -1, 1) * 32767).astype(np.int16)
    with wave.open(str(path), "wb") as wf:
        wf.setnchannels(1)                          # mono, so the game places it in the world
        wf.setsampwidth(2)
        wf.setframerate(RATE)
        wf.writeframes(pcm.tobytes())


def main():
    keep = "--wav" in sys.argv
    ffmpeg = shutil.which("ffmpeg")
    OUT.mkdir(parents=True, exist_ok=True)
    for name, notes in PHRASES.items():
        wav = OUT / f"{name}.wav"
        data = phrase(notes)
        write_wav(wav, data)
        if ffmpeg:
            subprocess.run([ffmpeg, "-y", "-loglevel", "error", "-i", str(wav), "-vn", "-c:a", "libvorbis", "-q:a", "5",
                            str(wav.with_suffix(".ogg"))], check=True)
            if not keep:
                wav.unlink()
        print(name, f"{len(data) / RATE:.2f} s")
    if not ffmpeg:
        print("ffmpeg missing: .wav written, convert to .ogg before shipping")


if __name__ == "__main__":
    main()
