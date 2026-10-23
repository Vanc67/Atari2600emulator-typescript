package javax.microedition.rms;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Compile-time stub of the MIDP RMS. The desktop harness keeps the records in
 * memory so the settings round-trip can be tested without a device.
 */
public class RecordStore {

    private static final Hashtable STORES = new Hashtable();

    private final Vector records = new Vector();

    public static RecordStore openRecordStore(String name, boolean create) throws RecordStoreException {
        synchronized (STORES) {
            RecordStore rs = (RecordStore) STORES.get(name);
            if (rs == null && create) {
                rs = new RecordStore();
                STORES.put(name, rs);
            }
            return rs;
        }
    }

    public void closeRecordStore() throws RecordStoreException { }

    public int addRecord(byte[] d, int o, int l) throws RecordStoreException {
        byte[] copy = new byte[l];
        System.arraycopy(d, o, copy, 0, l);
        records.addElement(copy);
        return records.size();
    }

    public byte[] getRecord(int id) throws RecordStoreException {
        if (id < 1 || id > records.size()) return null;
        return (byte[]) records.elementAt(id - 1);
    }

    public void setRecord(int id, byte[] d, int o, int l) throws RecordStoreException {
        byte[] copy = new byte[l];
        System.arraycopy(d, o, copy, 0, l);
        if (id < 1 || id > records.size()) records.addElement(copy);
        else records.setElementAt(copy, id - 1);
    }

    public int getNumRecords() throws RecordStoreException { return records.size(); }

    public int getRecordSize(int id) throws RecordStoreException {
        byte[] r = getRecord(id);
        return r == null ? 0 : r.length;
    }
}
