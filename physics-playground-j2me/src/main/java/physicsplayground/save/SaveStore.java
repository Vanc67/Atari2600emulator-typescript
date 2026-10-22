package physicsplayground.save;

import java.io.IOException;

/** Storage bridge implemented by RMS on MIDP and memory/files in desktop tests. */
public interface SaveStore {
    String PRIMARY = "", PENDING = ".tmp", BACKUP = ".bak";
    boolean exists(String slot, String suffix);
    String read(String slot, String suffix) throws IOException;
    /** Atomically write a read-back-verified primary, preserving the previous backup. */
    void write(String slot, String text) throws IOException;
    void promotePending(String slot) throws IOException;
}
