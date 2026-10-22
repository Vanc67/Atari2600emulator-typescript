package physicsplayground.game;

import physicsplayground.core.Vec2;

/** Platform boundary for the original 20 synthesized sound presets. */
public interface AudioSink {
    AudioSink NONE = new AudioSink() { public void play(String id, Vec2 position, float volumeDb) {} };
    void play(String id, Vec2 position, float volumeDb);
}
