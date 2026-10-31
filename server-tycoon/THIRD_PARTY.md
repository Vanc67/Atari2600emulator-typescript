# Third-party assets

## Simple Icons

Device, operating-system and application SVG paths in `www/js/brands.js` come from the `simple-icons` npm package (CC0-1.0). Brand names and logos remain trademarks of their respective owners; they are used to identify emulated hardware and software.

## Music — CC0

The Ogg Vorbis files in `www/audio` were obtained from the `Stayingfalse/TechNews-Bot` GitHub repository, whose `assets/music/LICENSES.md` documents the original OpenGameArt sources and CC0 status. The unmodified license/source list is bundled beside the tracks.

- Electro Swing type — G_P — CC0.
- Funky Menu Loop — iamoneabe — CC0.
- Coffee House Bump — OatCog — CC0.

## Original procedural lo-fi

`night-rack-lofi.ogg` and `cold-aisle-lofi.ogg` were created specifically for Server Tycoon 4.0 with the reproducible `tools/generate_lofi.py` synthesizer. They use generated chords, bass, drums and vinyl noise, with no third-party samples.

Server fan/HDD soundscapes are synthesized at runtime with WebAudio and do not use audio files.

## Ultimate 7.0 procedural audio and graphics

The 20-track, ten-hour lo-fi radio in `www/js/long-lofi.js` is generated at runtime from original oscillators, seeded arrangements and synthetic vinyl noise. It embeds no third-party recordings or samples. PC fan, HDD/SSD and coil sounds in `www/js/pc-sound.js` are also synthesized locally.

PC cases, dumpster art, OS wallpapers and interface graphics are inline SVG created in the project source. No raster images are required.

No asset is downloaded at runtime.
