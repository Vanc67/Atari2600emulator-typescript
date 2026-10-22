package physicsplayground.platform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.rms.RecordStore;
import physicsplayground.core.Json;
import physicsplayground.save.SaveStore;

/** Atomic UTF-8 save storage using one single-record RMS store per save candidate. */
public final class RmsSaveStore implements SaveStore {
    public boolean exists(String slot, String suffix) {
        RecordStore store = null;
        try { store = RecordStore.openRecordStore(name(slot, suffix), false); return store.getNumRecords() > 0; }
        catch (Exception ignored) { return false; }
        finally { closeQuietly(store); }
    }
    public String read(String slot, String suffix) throws IOException {
        byte[] bytes = readBytes(slot, suffix); if (bytes == null) return null;
        InputStream in = new ByteArrayInputStream(bytes); try { return Json.readUtf8(in); } finally { in.close(); }
    }
    public void write(String slot, String text) throws IOException {
        byte[] bytes = utf8(text); writeRecord(slot, SaveStore.PENDING, bytes);
        if (!same(bytes, readBytes(slot, SaveStore.PENDING))) throw new IOException("pending save verification failed");
        promotePending(slot);
    }
    public void promotePending(String slot) throws IOException {
        byte[] pending = readBytes(slot, SaveStore.PENDING); if (pending == null) throw new IOException("pending save is missing");
        byte[] primary = readBytes(slot, SaveStore.PRIMARY); if (primary != null && !same(primary, pending)) writeRecord(slot, SaveStore.BACKUP, primary);
        writeRecord(slot, SaveStore.PRIMARY, pending);
        if (!same(pending, readBytes(slot, SaveStore.PRIMARY))) throw new IOException("primary save verification failed");
        delete(slot, SaveStore.PENDING);
    }
    private byte[] readBytes(String slot, String suffix) throws IOException {
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(name(slot, suffix), false); if (store.getNumRecords() == 0) return null;
            return store.getRecord(1);
        } catch (javax.microedition.rms.RecordStoreNotFoundException missing) { return null; }
        catch (Exception error) { throw failure("RMS read failed", error); }
        finally { closeQuietly(store); }
    }
    private void writeRecord(String slot, String suffix, byte[] bytes) throws IOException {
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore(name(slot, suffix), true);
            if (store.getNumRecords() == 0) store.addRecord(bytes, 0, bytes.length); else store.setRecord(1, bytes, 0, bytes.length);
        } catch (Exception error) { throw failure("RMS write failed", error); }
        finally { closeQuietly(store); }
    }
    private void delete(String slot, String suffix) throws IOException {
        try { RecordStore.deleteRecordStore(name(slot, suffix)); }
        catch (javax.microedition.rms.RecordStoreNotFoundException ignored) {}
        catch (Exception error) { throw failure("RMS cleanup failed", error); }
    }
    private static byte[] utf8(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 128) out.write(c);
            else if (c < 2048) { out.write(192 | c >>> 6); out.write(128 | c & 63); }
            else { out.write(224 | c >>> 12); out.write(128 | c >>> 6 & 63); out.write(128 | c & 63); }
        }
        return out.toByteArray();
    }
    private static boolean same(byte[] a, byte[] b) { if (a == null || b == null || a.length != b.length) return false; for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false; return true; }
    private static String name(String slot, String suffix) {
        String value = slot == null || slot.length() == 0 ? "default" : slot; StringBuffer clean = new StringBuffer();
        for (int i = 0; i < value.length() && i < 8; i++) { char c = value.charAt(i); clean.append(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-' ? c : '_'); }
        clean.append('_').append(Integer.toHexString(value.hashCode())); if (SaveStore.PENDING.equals(suffix)) clean.append("_pending"); else if (SaveStore.BACKUP.equals(suffix)) clean.append("_backup");
        return "ppg_" + clean.toString();
    }
    private static IOException failure(String message, Exception error) { return new IOException(message + (error.getMessage() == null ? "" : ": " + error.getMessage())); }
    private static void closeQuietly(RecordStore store) { if (store != null) try { store.closeRecordStore(); } catch (Exception ignored) {} }
}
