#!/usr/bin/env python3
"""Оригинальные lo-fi треки Server Tycoon. Нужны numpy и imageio-ffmpeg."""
from pathlib import Path
import math
import subprocess
import sys
import wave

import numpy as np
import imageio_ffmpeg

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "www" / "audio"
SR = 22050


def midi(note):
    return 440.0 * 2 ** ((note - 69) / 12)


def add(dst, signal, start):
    index = int(start * SR)
    if index >= len(dst):
        return
    length = min(len(signal), len(dst) - index)
    dst[index:index + length] += signal[:length]


def env_time(duration):
    return np.arange(max(1, int(duration * SR))) / SR


def electric_piano(freq, duration, gain=.1):
    t = env_time(duration)
    envelope = (1 - np.exp(-t * 55)) * np.exp(-t * 1.25)
    tone = np.sin(2 * np.pi * freq * t) + .32 * np.sin(2 * np.pi * freq * 2 * t) + .12 * np.sin(2 * np.pi * freq * 3 * t)
    return gain * tone * envelope * (1 + .06 * np.sin(2 * np.pi * 4.5 * t))


def bass(freq, duration, gain=.15):
    t = env_time(duration)
    return gain * (np.sin(2 * np.pi * freq * t) + .25 * np.sin(2 * np.pi * freq * 2 * t)) * (1 - np.exp(-t * 30)) * np.exp(-t * 2.2)


def kick(gain=.34):
    t = env_time(.42)
    phase = 2 * np.pi * (72 * t - 24 * t * t)
    return gain * np.sin(phase) * np.exp(-t * 13)


def snare(rng, gain=.14):
    t = env_time(.3)
    noise = rng.normal(0, 1, len(t))
    noise = np.convolve(noise, np.ones(5) / 5, mode="same")
    return gain * (.8 * noise + .35 * np.sin(2 * np.pi * 175 * t)) * np.exp(-t * 18)


def hat(rng, gain=.035):
    t = env_time(.11)
    noise = rng.normal(0, 1, len(t))
    high = np.concatenate([[0], np.diff(noise)])
    return gain * high * np.exp(-t * 48)


def generate(name, bpm, chords, seed, brightness=1.0):
    rng = np.random.default_rng(seed)
    seconds = 64
    beat = 60 / bpm
    total = np.zeros(seconds * SR, dtype=np.float64)
    vinyl = rng.normal(0, .0018, len(total))
    vinyl = np.convolve(vinyl, np.ones(21) / 21, mode="same")
    total += vinyl
    for _ in range(240):
        t = env_time(rng.uniform(.006, .035))
        click = rng.normal(0, .02, len(t)) * np.exp(-t * rng.uniform(100, 260))
        add(total, click, rng.uniform(0, seconds))

    bar = beat * 4
    for bar_index in range(int(seconds / bar) + 1):
        chord = chords[bar_index % len(chords)]
        start = bar_index * bar
        for note in chord:
            add(total, electric_piano(midi(note), bar * .94, .055 * brightness), start)
        add(total, bass(midi(chord[0] - 24), beat * 1.45, .16), start)
        add(total, bass(midi(chord[0] - 24), beat * 1.1, .12), start + beat * 2)
        if bar_index % 2 == 1:
            add(total, electric_piano(midi(chord[-1] + 12), beat * 1.6, .035), start + beat * 2.55)

    for index in range(int(seconds / beat) + 1):
        start = index * beat
        if index % 4 in (0, 2):
            add(total, kick(.32), start)
        if index % 4 in (1, 3):
            add(total, snare(rng, .13), start)
        if index % 4 == 2:
            add(total, kick(.18), start + beat * .55)
        for half in (0, .5):
            swing = .055 if half else 0
            add(total, hat(rng, .024 if (index + half) % 2 else .035), start + (half + swing) * beat)

    # Мягкая огибающая и tape-like saturation.
    total = np.tanh(total * 1.25)
    fade = int(SR * 1.2)
    total[:fade] *= np.linspace(0, 1, fade)
    total[-fade:] *= np.linspace(1, 0, fade)
    total *= .82 / max(.01, np.max(np.abs(total)))
    return (total * 32767).astype("<i2")


def write_track(name, bpm, chords, seed, brightness=1.0):
    pcm = generate(name, bpm, chords, seed, brightness)
    wav_path = Path("/tmp") / f"{name}.wav"
    ogg_path = OUT / f"{name}.ogg"
    with wave.open(str(wav_path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(SR)
        wav.writeframes(pcm.tobytes())
    subprocess.run([
        imageio_ffmpeg.get_ffmpeg_exe(), "-y", "-loglevel", "error", "-i", str(wav_path),
        "-c:a", "libvorbis", "-q:a", "3", "-ar", str(SR), "-ac", "1", str(ogg_path)
    ], check=True)
    wav_path.unlink()
    print(f"{ogg_path.name}: {ogg_path.stat().st_size} bytes")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # Dm9 — G13 — Cmaj9 — Am9.
    write_track("night-rack-lofi", 78, [[50, 57, 60, 64, 69], [55, 59, 62, 65, 69], [48, 55, 59, 62, 64], [57, 60, 64, 67, 71]], 2601)
    # Am7 — Fmaj7 — Cadd9 — G6, более тёмный и медленный второй трек.
    write_track("cold-aisle-lofi", 72, [[57, 60, 64, 67], [53, 57, 60, 64], [48, 55, 60, 62], [55, 59, 62, 64]], 2602, .82)


if __name__ == "__main__":
    main()
