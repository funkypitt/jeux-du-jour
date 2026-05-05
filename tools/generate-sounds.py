#!/usr/bin/env python3
"""Generate short sound effect .ogg files for jeux-du-jour using sine wave synthesis.

Requires: numpy, soundfile
Install:  pip install numpy soundfile

Output: app/src/main/res/raw/*.ogg
"""

import os
import numpy as np

try:
    import soundfile as sf
except ImportError:
    print("ERROR: soundfile not installed. Run: pip install numpy soundfile")
    raise SystemExit(1)

SAMPLE_RATE = 44100
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")


def ensure_output_dir():
    os.makedirs(OUTPUT_DIR, exist_ok=True)


def envelope(length: int, attack: int = 100, release: int = 500) -> np.ndarray:
    """Simple attack-release envelope."""
    env = np.ones(length)
    attack = min(attack, length)
    release = min(release, length)
    env[:attack] = np.linspace(0, 1, attack)
    env[-release:] = np.linspace(1, 0, release)
    return env


def sine(freq: float, duration: float, volume: float = 0.7) -> np.ndarray:
    """Generate a sine wave."""
    t = np.linspace(0, duration, int(SAMPLE_RATE * duration), endpoint=False)
    return volume * np.sin(2 * np.pi * freq * t)


def generate_tap():
    """Short click sound - 800Hz, 30ms, fast decay."""
    duration = 0.03
    samples = int(SAMPLE_RATE * duration)
    t = np.linspace(0, duration, samples, endpoint=False)
    signal = 0.5 * np.sin(2 * np.pi * 800 * t)
    env = np.exp(-t * 150)  # Fast exponential decay
    return signal * env


def generate_pop():
    """Bubbly pop - frequency sweep 600Hz->400Hz, 80ms."""
    duration = 0.08
    samples = int(SAMPLE_RATE * duration)
    t = np.linspace(0, duration, samples, endpoint=False)
    freq = np.linspace(600, 400, samples)
    phase = 2 * np.pi * np.cumsum(freq) / SAMPLE_RATE
    signal = 0.6 * np.sin(phase)
    env = np.exp(-t * 40)
    return signal * env


def generate_correct():
    """Ascending two-tone chime - C5 (523Hz) -> E5 (659Hz), 200ms each."""
    tone1 = sine(523.25, 0.15, 0.5) * envelope(int(SAMPLE_RATE * 0.15), 50, 200)
    tone2 = sine(659.25, 0.20, 0.5) * envelope(int(SAMPLE_RATE * 0.20), 50, 400)
    gap = np.zeros(int(SAMPLE_RATE * 0.02))
    return np.concatenate([tone1, gap, tone2])


def generate_wrong():
    """Low buzz - 200Hz, 150ms, slightly distorted."""
    duration = 0.15
    samples = int(SAMPLE_RATE * duration)
    t = np.linspace(0, duration, samples, endpoint=False)
    signal = 0.5 * np.sin(2 * np.pi * 200 * t) + 0.2 * np.sin(2 * np.pi * 250 * t)
    env = envelope(samples, 100, 600)
    return np.clip(signal * env * 1.3, -0.7, 0.7)


def generate_rank_up():
    """Ascending arpeggio - C5, E5, G5, 400ms total."""
    c5 = sine(523.25, 0.12, 0.45) * envelope(int(SAMPLE_RATE * 0.12), 50, 200)
    e5 = sine(659.25, 0.12, 0.45) * envelope(int(SAMPLE_RATE * 0.12), 50, 200)
    g5 = sine(783.99, 0.18, 0.50) * envelope(int(SAMPLE_RATE * 0.18), 50, 400)
    gap = np.zeros(int(SAMPLE_RATE * 0.02))
    return np.concatenate([c5, gap, e5, gap, g5])


def generate_win():
    """Major chord resolve - C5, E5, G5, C6 layered, 800ms."""
    duration = 0.8
    samples = int(SAMPLE_RATE * duration)
    t = np.linspace(0, duration, samples, endpoint=False)
    env = envelope(samples, 200, int(SAMPLE_RATE * 0.4))

    signal = (
        0.30 * np.sin(2 * np.pi * 523.25 * t) +  # C5
        0.25 * np.sin(2 * np.pi * 659.25 * t) +  # E5
        0.20 * np.sin(2 * np.pi * 783.99 * t) +  # G5
        0.20 * np.sin(2 * np.pi * 1046.50 * t)   # C6
    )
    return signal * env


def generate_lose():
    """Descending minor - E5, C5, A4, 400ms total."""
    e5 = sine(659.25, 0.13, 0.45) * envelope(int(SAMPLE_RATE * 0.13), 50, 200)
    c5 = sine(523.25, 0.13, 0.45) * envelope(int(SAMPLE_RATE * 0.13), 50, 200)
    a4 = sine(440.00, 0.20, 0.40) * envelope(int(SAMPLE_RATE * 0.20), 50, 500)
    gap = np.zeros(int(SAMPLE_RATE * 0.02))
    return np.concatenate([e5, gap, c5, gap, a4])


SOUNDS = {
    "tap": generate_tap,
    "pop": generate_pop,
    "correct": generate_correct,
    "wrong": generate_wrong,
    "rank_up": generate_rank_up,
    "win": generate_win,
    "lose": generate_lose,
}


def main():
    ensure_output_dir()
    for name, generator in SOUNDS.items():
        signal = generator()
        # Normalize to prevent clipping
        peak = np.max(np.abs(signal))
        if peak > 0:
            signal = signal / peak * 0.85
        path = os.path.join(OUTPUT_DIR, f"{name}.ogg")
        sf.write(path, signal, SAMPLE_RATE, format="OGG", subtype="VORBIS")
        print(f"  Generated {name}.ogg ({len(signal)/SAMPLE_RATE*1000:.0f}ms)")
    print(f"\nDone! {len(SOUNDS)} files written to {os.path.abspath(OUTPUT_DIR)}")


if __name__ == "__main__":
    main()
