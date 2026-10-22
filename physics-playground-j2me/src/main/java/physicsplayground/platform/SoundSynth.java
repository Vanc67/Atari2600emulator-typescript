package physicsplayground.platform;

import physicsplayground.core.Definitions;
import physicsplayground.core.GodotHash;
import physicsplayground.core.Json;
import physicsplayground.core.Math2;
import physicsplayground.core.Random;

/** PCM port of the recovered SoundSynth; returned samples are mono little-endian signed 16-bit. */
public final class SoundSynth {
    public static final int MIX_RATE = 22050;
    public static final String[] PRESETS = {"gunshot","explosion","impact_metal","impact_wood","impact_flesh","impact_generic","glass_break","reload","click","ricochet","fire_crackle","beep","swoosh"};
    private SoundSynth() {}
    public static boolean hasPreset(String preset) { for (int i = 0; i < PRESETS.length; i++) if (PRESETS[i].equals(preset)) return true; return false; }
    public static byte[] build(Definitions.Sound sound) {
        Random random = new Random(GodotHash.string(sound.id) & 0xffffffffL);
        int count = Math.max((int) (sound.duration * MIX_RATE), 64); float[] samples = new float[count];
        float brightness = (float) Json.number(sound.params.get("brightness"), 1); String synth = sound.synth;
        if ("gunshot".equals(synth)) {
            noise(samples, random, 0, .035 * sound.duration / .35, 1, .55 * brightness, false); tone(samples, 0, 95, .06, .8, true); noise(samples, random, 0, .18 * sound.duration / .35, .35, .12, false);
        } else if ("explosion".equals(synth)) {
            noise(samples, random, 0, sound.duration * .35, 1, .06 * brightness, false); tone(samples, 0, 42, sound.duration * .4, .9, true); noise(samples, random, 0, .05, .6, .5, false);
        } else if ("impact_metal".equals(synth)) {
            tone(samples, 0, 523 * brightness, sound.duration * .35, .5, true); tone(samples, 0, 1307 * brightness, sound.duration * .35, .3, true);
            tone(samples, 0, 2210 * brightness, sound.duration * .35, .22, true); tone(samples, 0, 3150 * brightness, sound.duration * .35, .12, true); noise(samples, random, 0, .01, .5, .7, false);
        } else if ("impact_wood".equals(synth)) {
            noise(samples, random, 0, .03, .8, .25 * brightness, false); tone(samples, 0, 190, .05, .6, true); tone(samples, 0, 420, .03, .25, true);
        } else if ("impact_flesh".equals(synth)) {
            noise(samples, random, 0, .05, .7, .08, false); tone(samples, 0, 85, .07, .6, true);
        } else if ("impact_generic".equals(synth)) noise(samples, random, 0, .04, .8, .2 * brightness, false);
        else if ("glass_break".equals(synth)) {
            noise(samples, random, 0, .03, .8, .9, false);
            for (int i = 0; i < 14; i++) tone(samples, random.range(0, sound.duration * .6f), random.range(2500, 6500), random.range(.03f, .12f), random.range(.15f, .4f), true);
        } else if ("reload".equals(synth)) {
            noise(samples, random, 0, .012, .9, .8, false); tone(samples, 0, 1800, .02, .3, true);
            noise(samples, random, sound.duration * .55, .015, 1, .7, false); tone(samples, sound.duration * .55, 1300, .03, .35, true);
        } else if ("click".equals(synth)) {
            noise(samples, random, 0, .008, 1, .85, false); tone(samples, 0, 2400, .012, .3, true);
        } else if ("ricochet".equals(synth)) {
            sweep(samples, 3600 * brightness, 1100, sound.duration, .45); noise(samples, random, 0, .01, .6, .8, false);
        } else if ("fire_crackle".equals(synth)) {
            for (int i = 0; i < 18; i++) noise(samples, random, random.range(0, sound.duration * .95f), random.range(.003f, .012f), random.range(.2f, .7f), .6, false);
            noise(samples, random, 0, sound.duration * .6, .12, .04, false);
        } else if ("beep".equals(synth)) tone(samples, 0, 880 * brightness, sound.duration, .5, false);
        else if ("swoosh".equals(synth)) noise(samples, random, 0, sound.duration * .5, .5, .15, true);
        float peak = 0; for (int i = 0; i < count; i++) peak = Math.max(peak, Math.abs(samples[i])); double gain = peak > 0 ? .9 / peak : 1;
        byte[] pcm = new byte[count * 2];
        for (int i = 0; i < count; i++) { int value = Math2.round(samples[i] * gain * 32767); value = Math.max(-32768, Math.min(32767, value)); pcm[i * 2] = (byte) value; pcm[i * 2 + 1] = (byte) (value >> 8); }
        return pcm;
    }
    private static void noise(float[] samples, Random random, double start, double decay, double amplitude, double cutoff, boolean swell) {
        int first = (int) (start * MIX_RATE); double filtered = 0, coefficient = clamp(cutoff, .001, 1);
        for (int i = first; i < samples.length; i++) {
            double t = (i - first) / (double) MIX_RATE, envelope = Math2.exp(-t / Math.max(decay, .0005));
            if (swell) envelope *= Math.min(t / Math.max(decay * .5, .0005), 1); if (envelope < .001) break;
            filtered += (random.range(-1f, 1f) - filtered) * coefficient; samples[i] += (float) (filtered * envelope * amplitude);
        }
    }
    private static void tone(float[] samples, double start, double frequency, double decay, double amplitude, boolean decaying) {
        int first = (int) (start * MIX_RATE);
        for (int i = first; i < samples.length; i++) {
            double t = (i - first) / (double) MIX_RATE, envelope = decaying ? Math2.exp(-t / Math.max(decay, .0005)) : Math.min(Math.min(t, decay - t) * 200, 1);
            if (decaying && envelope < .001 || !decaying && t > decay) break;
            samples[i] += (float) (Math.sin(Math.PI * 2 * frequency * t) * envelope * amplitude);
        }
    }
    private static void sweep(float[] samples, double fromFrequency, double toFrequency, double duration, double amplitude) {
        double phase = 0; int count = Math.min((int) (duration * MIX_RATE), samples.length);
        for (int i = 0; i < count; i++) {
            double t = i / (double) MIX_RATE, frequency = fromFrequency + (toFrequency - fromFrequency) * (t / duration); phase += Math.PI * 2 * frequency / MIX_RATE;
            samples[i] += (float) (Math.sin(phase) * Math2.exp(-t / (duration * .4)) * amplitude);
        }
    }
    private static double clamp(double value, double min, double max) { return value < min ? min : value > max ? max : value; }
}
