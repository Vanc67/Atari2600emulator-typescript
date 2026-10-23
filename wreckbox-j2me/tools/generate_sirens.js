#!/usr/bin/env node
/* Rebuild the emergency-vehicle sirens recovered from Zv.siren in Wreckbox 1.8.
 * The original uses a sawtooth plus a 0.35-gain square through a 1100 Hz,
 * Q=0.6 bandpass.  Keep the three vehicle-specific durations and envelopes.
 * Output is mono 8 kHz AMR-NB MR4.75 so the K510i does not need a large PCM clip.
 */
const fs = require('fs');
const path = require('path');

const OUT = path.resolve(__dirname, '../src/main/resources/audio');
const CHECK = process.argv.includes('--check');
const INPUT_RATE = 48000;
const OUTPUT_RATE = 8000;
const PHONE_GAIN = 4; // loudness only; recovered timing, pitches and filter are unchanged
const clips = [];
for (const seconds of [24, 32, 22]) {
  for (const kind of ['fire', 'med', 'police']) {
    clips.push({ kind, seconds, duration: seconds === 24 ? 2.4 : seconds === 32 ? 3.2 : 2.2 });
  }
}

function makeEncoder() {
  const filename = require.resolve('benz-amr-recorder/src/amrnb.js');
  // The browser worker hardcodes MR12.2.  Use the same opencore encoder in its
  // MR4.75 mode, matching the other compressed audio resources in this port.
  const source = fs.readFileSync(filename, 'utf8')
    .replace('AMR.encode(samples, sampleRate, 7)', 'AMR.encode(samples, sampleRate, 0)');
  if (source === fs.readFileSync(filename, 'utf8')) throw new Error('AMR encoder mode hook not found');
  const module = { exports: null };
  let encoded = null;
  const self = { postMessage(message) { encoded = message.amr; } };
  new Function('module', 'self', source)(module, self);
  module.exports();
  return samples => {
    encoded = null;
    self.onmessage({ data: { command: 'encode', samples, sampleRate: OUTPUT_RATE } });
    if (!encoded) throw new Error('AMR encoder returned no data');
    return Buffer.from(encoded);
  };
}

function frequencies(clip, t) {
  if (clip.kind === 'med') {
    const high = Math.floor(t / 0.55) % 2 !== 0;
    return high ? [880, 882] : [660, 661.5];
  }
  const police = clip.kind === 'police';
  const period = police ? 0.36 : 2.6;
  const low = police ? 700 : 520;
  const high = police ? 1500 : 1180;
  const phase = (t % period) / period;
  let hz;
  if (phase < 0.5) hz = low * Math.pow(high / low, phase * 2);
  else hz = high * Math.pow(low / high, (phase - 0.5) * 2);
  return [hz, hz * 1.004];
}

function envelope(t, duration) {
  const floor = 0.0001;
  const peak = 0.045;
  const attack = Math.min(0.9, 0.35 * duration);
  if (t < attack) return floor * Math.pow(peak / floor, t / attack);
  if (t >= duration - 0.6) {
    return peak * Math.pow(floor / peak, (t - duration + 0.6) / 0.6);
  }
  return peak;
}

function synthesize(clip) {
  // RBJ constant-peak-gain bandpass, the same biquad topology as Web Audio.
  const omega = 2 * Math.PI * 1100 / INPUT_RATE;
  const alpha = Math.sin(omega) / (2 * 0.6);
  const a0 = 1 + alpha;
  const b0 = alpha / a0, b2 = -alpha / a0;
  const a1 = -2 * Math.cos(omega) / a0, a2 = (1 - alpha) / a0;
  let x1 = 0, x2 = 0, y1 = 0, y2 = 0;
  let sawPhase = 0, squarePhase = 0;
  const outputSamples = Math.round(clip.duration * OUTPUT_RATE);
  // One extra encoder frame prevents amr.js's strict loop bound from dropping
  // the final real 20 ms frame.  The padding is not emitted by the encoder.
  const pcm = new Float32Array(outputSamples + 160);
  const ratio = INPUT_RATE / OUTPUT_RATE;
  for (let out = 0; out < outputSamples; out++) {
    let sum = 0;
    for (let sub = 0; sub < ratio; sub++) {
      const t = (out * ratio + sub) / INPUT_RATE;
      const hz = frequencies(clip, t);
      sawPhase += hz[0] / INPUT_RATE;
      squarePhase += hz[1] / INPUT_RATE;
      sawPhase -= Math.floor(sawPhase);
      squarePhase -= Math.floor(squarePhase);
      const saw = 2 * (sawPhase - Math.floor(sawPhase + 0.5));
      const square = Math.sin(2 * Math.PI * squarePhase) >= 0 ? 1 : -1;
      const input = saw + 0.35 * square;
      const filtered = b0 * input + b2 * x2 - a1 * y1 - a2 * y2;
      x2 = x1; x1 = input; y2 = y1; y1 = filtered;
      sum += filtered * envelope(t, clip.duration) * PHONE_GAIN;
    }
    const sample = sum / ratio;
    pcm[out] = Math.max(-0.95, Math.min(0.95, sample));
  }
  return pcm;
}

const encode = makeEncoder();
let failed = false;
for (const clip of clips) {
  const name = `${clip.kind}Siren${clip.seconds}.amr`;
  const bytes = encode(synthesize(clip));
  const frames = Math.round(clip.duration * 50);
  const expected = 6 + frames * 13;
  if (bytes.length !== expected || bytes.subarray(0, 6).toString() !== '#!AMR\n') {
    throw new Error(`${name}: invalid AMR length ${bytes.length}, expected ${expected}`);
  }
  const destination = path.join(OUT, name);
  if (CHECK) {
    const actual = fs.readFileSync(destination);
    if (!actual.equals(bytes)) {
      console.error(`MISMATCH ${name}`);
      failed = true;
    } else console.log(`OK ${name} ${bytes.length} B ${clip.duration.toFixed(1)} s`);
  } else {
    fs.writeFileSync(destination, bytes);
    console.log(`${name} ${bytes.length} B ${clip.duration.toFixed(1)} s`);
  }
}
if (failed) process.exitCode = 1;
