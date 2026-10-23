package javax.microedition.rms;
public class RecordStore {
    public static RecordStore openRecordStore(String name, boolean create) throws RecordStoreException { return null; }
    public void closeRecordStore() throws RecordStoreException {}
    public int addRecord(byte[] d, int o, int l) throws RecordStoreException { return 0; }
    public byte[] getRecord(int id) throws RecordStoreException { return null; }
    public void setRecord(int id, byte[] d, int o, int l) throws RecordStoreException {}
    public int getNumRecords() throws RecordStoreException { return 0; }
    public int getRecordSize(int id) throws RecordStoreException { return 0; }
}
