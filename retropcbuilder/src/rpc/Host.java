package rpc;

abstract public interface Host {
    abstract public java.io.InputStream open(String arg);
    
    
    abstract public String load(String arg);
    
    
    abstract public void save(String arg, String arg0);
    
    
    abstract public void vibrate(int arg);
    
    
    abstract public void exitApp();
    
    
    abstract public long now();
    
    
    abstract public void tone(int arg, int arg0);
    
    
    abstract public void noise(int arg, int arg0);
    
    
    abstract public String textInput(String arg, String arg0, int arg1);
}
