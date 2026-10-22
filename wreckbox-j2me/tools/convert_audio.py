#!/usr/bin/env python3
"""Convert the recovered Wreckbox one-shots to streaming AMR-NB phone assets.
Usage: python3 tools/convert_audio.py /path/to/ffmpeg
The MP3 originals remain in original/recovered/audio; only the small AMR set is
packaged in the MIDlet.  No PCM bank is created.
"""
import pathlib
import subprocess
import sys

NAMES = """
arc_00 bomb_whistle_00 bomber_00 boom_00 boom_01 boom_02 boom_low_00 boom_low_01 boom_far_00
brk_brick_00 brk_concrete_00 brk_metal_00 brk_wood_00 chute_00 crackL creak_00 creak_01 crunch_00
diesel_start_00 fireL flameS glass_00 glass_big_00 groan_00 hit_soft_00 hit_earth_00 hiss_00 hole
laser pour_00 pour_01 rocketL rumbleL scream_m_00 scream_m_01 scream_f_00 shot_00 sirenS sirenC
splash_00 splash_small_00 splash_big_00 thunder_00 ui_click_00 waterS
""".split()


def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: convert_audio.py /path/to/ffmpeg")
    root = pathlib.Path(__file__).resolve().parents[1]
    source = root / "original" / "recovered" / "audio"
    target = root / "src" / "main" / "resources" / "audio"
    target.mkdir(parents=True, exist_ok=True)
    for name in NAMES:
        mp3 = source / (name + ".mp3")
        if not mp3.is_file():
            raise FileNotFoundError(mp3)
        output = target / (name + ".amr")
        subprocess.run([
            sys.argv[1], "-y", "-loglevel", "error", "-i", str(mp3),
            "-map_metadata", "-1", "-vn", "-ac", "1", "-ar", "8000",
            "-c:a", "libopencore_amrnb", "-b:a", "4.75k", "-t", "8",
            str(output),
        ], check=True)
        data = output.read_bytes()
        if not data.startswith(b"#!AMR\n"):
            raise RuntimeError("not AMR-NB: " + str(output))
    print("converted", len(NAMES), "original sounds to", target)


if __name__ == "__main__":
    main()
