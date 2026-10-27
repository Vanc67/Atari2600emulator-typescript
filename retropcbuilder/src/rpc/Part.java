package rpc;

public class Part {
    final public static int CPU = 1;
    final public static int GPU = 2;
    final public static int MB = 3;
    final public static int RAM = 4;
    final public static int HDD = 5;
    final public static int FDD = 6;
    final public static int SSD = 7;
    final public static int ODD = 8;
    final public static int PSU = 9;
    final public static int CASE = 10;
    final public static int COOL = 11;
    final public static int MON = 12;
    final public static int SND = 13;
    final public static int NET = 14;
    final public static int KB = 15;
    final public static int MOUSE = 16;
    final public static int JOY = 17;
    final public static int MISC = 18;
    final public static int IGPU = 19;
    final public static int PRN = 20;
    public int index;
    public int cat;
    public int year;
    public int price;
    public String name;
    public int[] f;
    public int cond;
    public int dust;
    
    public Part() {
        this.f = new int[6];
        this.cond = 100;
        this.dust = 0;
    }
    
    public String catName() {
        switch(this.cat) {
            case 20: {
                return "\u041f\u0435\u0440\u0438\u0444\u0435\u0440\u0438\u044f";
            }
            case 19: {
                return "\u0412\u0441\u0442\u0440\u043e\u0439\u043a\u0430";
            }
            case 18: {
                return "\u041f\u043b\u0430\u0442\u0430";
            }
            case 17: {
                return "\u0413\u0435\u0439\u043c\u043f\u0430\u0434";
            }
            case 16: {
                return "\u041c\u044b\u0448\u044c";
            }
            case 15: {
                return "\u041a\u043b\u0430\u0432\u0430";
            }
            case 14: {
                return "\u0421\u0435\u0442\u044c";
            }
            case 13: {
                return "\u0417\u0432\u0443\u043a";
            }
            case 12: {
                return "\u041c\u043e\u043d\u0438\u0442\u043e\u0440";
            }
            case 11: {
                return "\u041a\u0443\u043b\u0435\u0440";
            }
            case 10: {
                return "\u041a\u043e\u0440\u043f\u0443\u0441";
            }
            case 9: {
                return "\u0411\u041f";
            }
            case 8: {
                return "\u041f\u0440\u0438\u0432\u043e\u0434";
            }
            case 7: {
                return "SSD";
            }
            case 6: {
                return "FDD";
            }
            case 5: {
                return "HDD";
            }
            case 4: {
                return "\u041e\u0417\u0423";
            }
            case 3: {
                return "\u041f\u043b\u0430\u0442\u0430";
            }
            case 2: {
                return "\u0412\u0438\u0434\u0435\u043e";
            }
            case 1: {
                return "CPU";
            }
            default: {
                return "?";
            }
        }
    }
}
