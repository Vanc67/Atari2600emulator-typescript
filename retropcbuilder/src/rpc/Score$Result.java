package rpc;

public class Score$Result {
    public int score;
    public int bonus;
    public int barsC;
    public int barsG;
    public int barsR;
    public int barsD;
    public int needPower;
    public int power;
    public String[] errors;
    public boolean ok;
    
    public Score$Result() {
        this.errors = new String[0];
    }
}
