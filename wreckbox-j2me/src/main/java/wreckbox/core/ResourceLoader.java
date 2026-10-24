package wreckbox.core;

import java.io.InputStream;

/** Platform resource hook; paths keep the original leading slash. */
public interface ResourceLoader {
  /** Returns null when the resource is unavailable. */
  InputStream open(String path);
}
