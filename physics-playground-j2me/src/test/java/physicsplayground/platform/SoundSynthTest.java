package physicsplayground.platform;

import java.util.Vector;
import physicsplayground.core.Content;
import physicsplayground.core.Definitions;
import physicsplayground.core.GodotHash;

public final class SoundSynthTest {
    private static int checks, failures;
    private static void check(boolean value, String label) { checks++; if (!value) { failures++; System.out.println("FAIL " + label); } }
    private static boolean same(byte[] a, byte[] b) { if (a.length != b.length) return false; for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false; return true; }
    private static int integer(byte[] bytes, int at) { return (bytes[at] & 255) | (bytes[at + 1] & 255) << 8 | (bytes[at + 2] & 255) << 16 | (bytes[at + 3] & 255) << 24; }
    public static void main(String[] args) throws Exception {
        check(SoundSynth.PRESETS.length == 13, "thirteen synth primitives"); check(GodotHash.integer(0) == 0x2aeaa2ab && GodotHash.integer(5) == 0x6a96ca97, "Godot integer hash");
        check(GodotHash.string("core:gunshot_light") == 0x68843839, "Godot DJB2 string hash");
        Vector sounds = Content.loadDefault().registry.allSounds(); check(sounds.size() == 20, "twenty original sound definitions"); int totalSamples = 0; byte[] first = null, explosion = null;
        for (int i = 0; i < sounds.size(); i++) {
            Definitions.Sound sound = (Definitions.Sound) sounds.elementAt(i); check(SoundSynth.hasPreset(sound.synth), "preset exists " + sound.id);
            byte[] pcm = SoundSynth.build(sound); int samples = Math.max((int) (sound.duration * 22050), 64); totalSamples += samples;
            check(pcm.length == samples * 2, "PCM duration " + sound.id); int peak = 0; boolean nonzero = false;
            for (int p = 0; p < samples; p++) { int value = (short) ((pcm[p * 2] & 255) | pcm[p * 2 + 1] << 8); peak = Math.max(peak, Math.abs(value)); if (value != 0) nonzero = true; }
            check(nonzero, "non-silent " + sound.id); check(peak >= 28000 && peak <= 29491, "normalized " + sound.id + " peak=" + peak);
            if (i == 0) first = pcm; if ("core:explosion".equals(sound.id)) explosion = pcm;
        }
        check(same(first, SoundSynth.build((Definitions.Sound) sounds.elementAt(0))), "sound synthesis deterministic");
        check(totalSamples > 150000, "full PCM content generated samples=" + totalSamples);
        WavInputStream wav = new WavInputStream(explosion, 11025); byte[] bytes = new byte[44 + explosion.length]; int at = 0;
        while (at < bytes.length) { int read = wav.read(bytes, at, Math.min(997, bytes.length - at)); check(read > 0, "WAV stream progresses"); at += read; }
        check(wav.read() == -1 && wav.available() == 0, "WAV stream reaches EOF"); check(bytes[0] == 'R' && bytes[1] == 'I' && bytes[8] == 'W' && bytes[36] == 'd', "RIFF/WAVE/data markers");
        check(integer(bytes, 4) == bytes.length - 8 && integer(bytes, 40) == explosion.length, "WAV lengths"); check(integer(bytes, 24) == 11025 && integer(bytes, 28) == 22050, "pitch through sample rate");
        for (int i = 0; i < explosion.length; i++) if (bytes[44 + i] != explosion[i]) { check(false, "WAV preserves PCM"); break; }
        System.out.println("SoundSynthTest: " + checks + " checks, " + failures + " failure(s), " + totalSamples + " samples"); if (failures > 0) System.exit(1);
    }
}
