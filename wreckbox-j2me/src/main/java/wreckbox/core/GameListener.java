package wreckbox.core;

public interface GameListener {
  void sound(String id, float volume);
  void message(String text);
}
