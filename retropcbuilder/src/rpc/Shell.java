package rpc;

public class Shell {
    final public static int OS_DOS = 0;
    final public static int OS_UNIX = 1;
    final private static int MAX_NODES = 64;
    final private static int MAX_LINES = 220;
    private String[] name;
    private int[] parent;
    private boolean[] dir;
    private String[] body;
    private int nodes;
    private int cwd;
    private int kind;
    public String osName;
    private int ramKb;
    private int cpuMhz;
    private int diskMb;
    private String[] out;
    private int outN;
    private int scroll;
    public String prompt;
    public String lastCmd;
    
    public Shell() {
        this.name = new String[64];
        this.parent = new int[64];
        this.dir = new boolean[64];
        this.body = new String[64];
        this.nodes = 0;
        this.cwd = 0;
        this.kind = 0;
        this.osName = "MS-DOS";
        this.ramKb = 640;
        this.cpuMhz = 8;
        this.diskMb = 20;
        this.out = new String[220];
        this.outN = 0;
        this.scroll = 0;
        this.prompt = "C:\\>";
        this.lastCmd = "";
        this.reset(0, "MS-DOS", 640, 8, 20);
    }
    
    public Shell(int i, String s, int i0, int i1, int i2) {
        this.name = new String[64];
        this.parent = new int[64];
        this.dir = new boolean[64];
        this.body = new String[64];
        this.nodes = 0;
        this.cwd = 0;
        this.kind = 0;
        this.osName = "MS-DOS";
        this.ramKb = 640;
        this.cpuMhz = 8;
        this.diskMb = 20;
        this.out = new String[220];
        this.outN = 0;
        this.scroll = 0;
        this.prompt = "C:\\>";
        this.lastCmd = "";
        this.reset(i, s, i0, i1, i2);
    }
    
    public void reset(int i, String s, int i0, int i1, int i2) {
        this.kind = i;
        if (s == null) {
            s = "OS";
        }
        this.osName = s;
        this.ramKb = i0;
        this.cpuMhz = i1;
        this.diskMb = i2;
        this.nodes = 0;
        this.outN = 0;
        this.scroll = 0;
        this.cwd = this.addNode((this.kind != 0) ? "/" : "C:", -1, true, (String)null);
        this.addNode("AUTOEXEC.BAT", this.cwd, false, "@ECHO OFF\nPROMPT $P$G\nPATH C:\\DOS\n");
        this.addNode("CONFIG.SYS", this.cwd, false, "DEVICE=C:\\DOS\\HIMEM.SYS\nFILES=20\nBUFFERS=15\n");
        int i3 = this.addNode((this.kind != 0) ? "bin" : "DOS", this.cwd, true, (String)null);
        this.addNode((this.kind != 0) ? "sh" : "COMMAND.COM", i3, false, "");
        this.addNode((this.kind != 0) ? "editor" : "EDIT.COM", i3, false, "");
        int i4 = this.addNode((this.kind != 0) ? "home" : "WORK", this.cwd, true, (String)null);
        this.addNode("README.TXT", i4, false, "\u042d\u0442\u043e \u0442\u0432\u043e\u044f \u0441\u0438\u0441\u0442\u0435\u043c\u0430. \u041d\u0430\u0431\u0435\u0440\u0438 HELP \u2014 \u0443\u0432\u0438\u0434\u0438\u0448\u044c \u043a\u043e\u043c\u0430\u043d\u0434\u044b.\n\u0424\u0430\u0439\u043b\u044b \u043c\u043e\u0436\u043d\u043e \u0441\u043e\u0437\u0434\u0430\u0432\u0430\u0442\u044c: ECHO \u0442\u0435\u043a\u0441\u0442 > note.txt\n");
        if (this.kind == 0) {
            i4 = this.cwd;
        }
        this.cwd = i4;
        this.println(new StringBuffer("\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 ").append(this.osName).append(" ...").toString());
        this.println(new StringBuffer("\u041f\u0430\u043c\u044f\u0442\u044c: ").append(this.ramKb).append(" \u041a\u0411, \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440 ").append(i1).append(" \u041c\u0413\u0446").toString());
        this.println("\u041d\u0430\u0431\u0435\u0440\u0438 HELP \u0434\u043b\u044f \u0441\u043f\u0438\u0441\u043a\u0430 \u043a\u043e\u043c\u0430\u043d\u0434.");
        this.updatePrompt();
    }
    
    private int addNode(String s, int i, boolean b, String s0) {
        if (this.nodes >= 64) {
            return i;
        }
        this.name[this.nodes] = s;
        this.parent[this.nodes] = i;
        this.dir[this.nodes] = b;
        this.body[this.nodes] = s0;
        int i0 = this.nodes;
        this.nodes = i0 + 1;
        return i0;
    }
    
    private int find(String s, int i) {
        int i0 = 1;
        while(i0 < this.nodes) {
            if (this.parent[i0] == i && this.name[i0].equalsIgnoreCase(s)) {
                return i0;
            }
            i0 = i0 + 1;
        }
        return -1;
    }
    
    public void println(String s) {
        if (this.outN >= 220) {
            int i = 1;
            while(i < 220) {
                this.out[i - 1] = this.out[i];
                i = i + 1;
            }
            this.outN = 219;
            if (this.scroll > 0) {
                this.scroll = this.scroll - 1;
            }
        }
        String[] a = this.out;
        int i0 = this.outN;
        this.outN = i0 + 1;
        if (s == null) {
            s = "";
        }
        a[i0] = s;
        this.scroll = 0;
    }
    
    public void clear() {
        this.outN = 0;
        this.scroll = 0;
    }
    
    public String line(int i, int i0) {
        int i1 = this.outN - i0 - this.scroll + i;
        if (i1 >= 0 && i1 < this.outN) {
            return this.out[i1];
        }
        return "";
    }
    
    public int lines() {
        return this.outN;
    }
    
    public String at(int i) {
        String s = null;
        label2: {
            label0: {
                label1: {
                    if (i < 0) {
                        break label1;
                    }
                    if (i < this.outN) {
                        break label0;
                    }
                }
                s = "";
                break label2;
            }
            s = this.out[i];
        }
        return s;
    }
    
    public void scrollUp(int i) {
        this.scroll = this.scroll + i;
        int i0 = this.outN - 4;
        if (this.scroll > i0) {
            if (i0 < 0) {
                i0 = 0;
            }
            this.scroll = i0;
        }
        if (this.scroll < 0) {
            this.scroll = 0;
        }
    }
    
    public void scrollDown(int i) {
        this.scroll = this.scroll - i;
        if (this.scroll < 0) {
            this.scroll = 0;
        }
    }
    
    public int osKind() {
        return this.kind;
    }
    
    private void updatePrompt() {
        if (this.kind != 0) {
            this.prompt = new StringBuffer(String.valueOf((Object)this.path(this.cwd))).append(" $ ").toString();
        } else {
            String s = this.path(this.cwd);
            this.prompt = new StringBuffer(String.valueOf((Object)((s.equals((Object)"C:")) ? "C:\\" : new StringBuffer(String.valueOf((Object)s)).append("\\").toString()))).append(">").toString();
        }
    }
    
    private String path(int i) {
        if (i <= 0) {
            return (this.kind != 0) ? "/" : "C:";
        }
        String s = this.path(this.parent[i]);
        if (this.kind != 0) {
            return (s.equals((Object)"/")) ? new StringBuffer("/").append(this.name[i]).toString() : new StringBuffer(String.valueOf((Object)s)).append("/").append(this.name[i]).toString();
        }
        return (s.equals((Object)"C:")) ? new StringBuffer("C:\\").append(this.name[i]).toString() : new StringBuffer(String.valueOf((Object)s)).append("\\").append(this.name[i]).toString();
    }
    
    public boolean run(String s) {
        if (s == null) {
            return false;
        }
        String s0 = s.trim();
        if (s0.length() == 0) {
            return false;
        }
        this.lastCmd = s0;
        this.println(new StringBuffer(String.valueOf((Object)this.prompt)).append(" ").append(s0).toString());
        int i = s0.indexOf(62);
        String s1 = null;
        if (i > 0) {
            s1 = s0.substring(i + 1).trim();
            s0 = s0.substring(0, i).trim();
        }
        int i0 = this.outN;
        boolean b = this.exec(s0);
        if (s1 != null && s1.length() > 0) {
            StringBuffer a = new StringBuffer();
            int i1 = i0;
            while(i1 < this.outN) {
                a.append(this.out[i1]).append((char)10);
                i1 = i1 + 1;
            }
            this.writeFile(s1, a.toString());
            this.outN = i0;
            this.println(new StringBuffer("\u0417\u0430\u043f\u0438\u0441\u0430\u043d\u043e \u0432 ").append(s1).toString());
        }
        this.updatePrompt();
        return b;
    }
    
    private boolean exec(String s) {
        String s0 = null;
        String s1 = rpc.Shell.lower(s);
        int i = s1.indexOf(32);
        if (i <= 0) {
            s0 = "";
        } else {
            s1 = s1.substring(0, i);
            s0 = s.substring(i + 1).trim();
        }
        boolean b = s1.equals((Object)"help");
        label1: {
            label36: {
                label37: {
                    if (b) {
                        break label37;
                    }
                    if (!s1.equals((Object)"?")) {
                        break label36;
                    }
                }
                this.help();
                break label1;
            }
            boolean b0 = s1.equals((Object)"cls");
            label34: {
                label35: {
                    if (b0) {
                        break label35;
                    }
                    if (!s1.equals((Object)"clear")) {
                        break label34;
                    }
                }
                this.clear();
                break label1;
            }
            boolean b1 = s1.equals((Object)"ver");
            label32: {
                label33: {
                    if (b1) {
                        break label33;
                    }
                    if (!s1.equals((Object)"uname")) {
                        break label32;
                    }
                }
                this.println(new StringBuffer(String.valueOf((Object)this.osName)).append("  [\u0432\u0435\u0440\u0441\u0438\u044f 1.0, ").append((this.kind != 0) ? "UNIX-\u043f\u043e\u0434\u043e\u0431\u043d\u0430\u044f" : "DOS-\u0441\u043e\u0432\u043c\u0435\u0441\u0442\u0438\u043c\u0430\u044f").append("]").toString());
                break label1;
            }
            boolean b2 = s1.equals((Object)"dir");
            label30: {
                label31: {
                    if (b2) {
                        break label31;
                    }
                    if (!s1.equals((Object)"ls")) {
                        break label30;
                    }
                }
                this.list();
                break label1;
            }
            boolean b3 = s1.equals((Object)"cd");
            label28: {
                label29: {
                    if (b3) {
                        break label29;
                    }
                    if (!s1.equals((Object)"chdir")) {
                        break label28;
                    }
                }
                this.cd(s0);
                break label1;
            }
            boolean b4 = s1.equals((Object)"md");
            label26: {
                label27: {
                    if (b4) {
                        break label27;
                    }
                    if (!s1.equals((Object)"mkdir")) {
                        break label26;
                    }
                }
                this.mkdir(s0);
                break label1;
            }
            boolean b5 = s1.equals((Object)"rd");
            label24: {
                label25: {
                    if (b5) {
                        break label25;
                    }
                    if (!s1.equals((Object)"rmdir")) {
                        break label24;
                    }
                }
                this.rmdir(s0);
                break label1;
            }
            boolean b6 = s1.equals((Object)"type");
            label22: {
                label23: {
                    if (b6) {
                        break label23;
                    }
                    if (!s1.equals((Object)"cat")) {
                        break label22;
                    }
                }
                this.type(s0);
                break label1;
            }
            boolean b7 = s1.equals((Object)"copy");
            label20: {
                label21: {
                    if (b7) {
                        break label21;
                    }
                    if (!s1.equals((Object)"cp")) {
                        break label20;
                    }
                }
                this.copy(s0);
                break label1;
            }
            boolean b8 = s1.equals((Object)"del");
            label18: {
                label19: {
                    if (b8) {
                        break label19;
                    }
                    if (!s1.equals((Object)"rm")) {
                        break label18;
                    }
                }
                this.del(s0);
                break label1;
            }
            boolean b9 = s1.equals((Object)"ren");
            label16: {
                label17: {
                    if (b9) {
                        break label17;
                    }
                    if (!s1.equals((Object)"mv")) {
                        break label16;
                    }
                }
                this.ren(s0);
                break label1;
            }
            if (s1.equals((Object)"echo")) {
                this.println(s0);
            } else {
                boolean b10 = s1.equals((Object)"mem");
                label14: {
                    label15: {
                        if (b10) {
                            break label15;
                        }
                        if (!s1.equals((Object)"free")) {
                            break label14;
                        }
                    }
                    this.mem();
                    break label1;
                }
                if (s1.equals((Object)"df")) {
                    this.println(new StringBuffer("\u0414\u0438\u0441\u043a C: \u0437\u0430\u043d\u044f\u0442\u043e ").append(this.diskMb / 3).append(" \u041c\u0411 \u0438\u0437 ").append(this.diskMb).append(" \u041c\u0411").toString());
                    this.println(new StringBuffer("\u0424\u0430\u0439\u043b\u043e\u0432 \u0432 \u0441\u0438\u0441\u0442\u0435\u043c\u0435: ").append(this.files()).toString());
                } else {
                    boolean b11 = s1.equals((Object)"chkdsk");
                    label12: {
                        label13: {
                            if (b11) {
                                break label13;
                            }
                            if (!s1.equals((Object)"fsck")) {
                                break label12;
                            }
                        }
                        this.println("\u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0434\u0438\u0441\u043a\u0430 C: ... ok");
                        this.println(new StringBuffer("  \u0444\u0430\u0439\u043b\u043e\u0432: ").append(this.files()).append(", \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u043e\u0432: ").append(this.nodes - this.files()).toString());
                        break label1;
                    }
                    if (s1.equals((Object)"format")) {
                        this.println("\u0424\u043e\u0440\u043c\u0430\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 C: \u2014 \u0437\u0430\u043d\u044f\u043b\u043e 3 \u043c\u0438\u043d\u0443\u0442\u044b");
                        this.println("\u0412\u0441\u0435 \u0444\u0430\u0439\u043b\u044b \u043f\u043e\u0442\u0435\u0440\u044f\u043d\u044b. \u042d\u0442\u043e \u0431\u044b\u043b\u043e \u043e\u043f\u0440\u043e\u043c\u0435\u0442\u0447\u0438\u0432\u043e.");
                    } else {
                        boolean b12 = s1.equals((Object)"time");
                        label10: {
                            label11: {
                                if (b12) {
                                    break label11;
                                }
                                if (!s1.equals((Object)"date")) {
                                    break label10;
                                }
                            }
                            this.println((s1.equals((Object)"time")) ? "\u0422\u0435\u043a\u0443\u0449\u0435\u0435 \u0432\u0440\u0435\u043c\u044f: 12:00" : "\u0414\u0430\u0442\u0430: 01-01-1990");
                            break label1;
                        }
                        boolean b13 = s1.equals((Object)"ps");
                        label9: {
                            if (b13) {
                                break label9;
                            }
                            if (s1.equals((Object)"top")) {
                                break label9;
                            }
                            if (s1.equals((Object)"man")) {
                                this.man(s0);
                                break label1;
                            } else if (s1.equals((Object)"grep")) {
                                this.grep(s0);
                                break label1;
                            } else {
                                boolean b14 = s1.equals((Object)"chmod");
                                label8: {
                                    if (b14) {
                                        break label8;
                                    }
                                    if (s1.equals((Object)"attrib")) {
                                        break label8;
                                    }
                                    if (s1.equals((Object)"whoami")) {
                                        this.println("root");
                                        break label1;
                                    } else if (s1.equals((Object)"sudo")) {
                                        this.println("\u0412\u044b \u0438 \u0442\u0430\u043a root. Sudo \u043d\u0435 \u043d\u0443\u0436\u0435\u043d.");
                                        break label1;
                                    } else {
                                        boolean b15 = s1.equals((Object)"apt");
                                        label6: {
                                            label7: {
                                                if (b15) {
                                                    break label7;
                                                }
                                                if (!s1.equals((Object)"pkg")) {
                                                    break label6;
                                                }
                                            }
                                            this.println("\u0427\u0442\u0435\u043d\u0438\u0435 \u0441\u043f\u0438\u0441\u043a\u043e\u0432 \u043f\u0430\u043a\u0435\u0442\u043e\u0432... \u0413\u043e\u0442\u043e\u0432\u043e");
                                            this.println("\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e 0 \u043d\u043e\u0432\u044b\u0445 \u043f\u0430\u043a\u0435\u0442\u043e\u0432 (\u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0430 \u043d\u0435\u0442)");
                                            break label1;
                                        }
                                        if (s1.equals((Object)"edit")) {
                                            this.println("\u0420\u0435\u0434\u0430\u043a\u0442\u043e\u0440 \u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d \u0432 \u043c\u0435\u043d\u044e \u041e\u0421: \u00ab\u0420\u0435\u0434\u0430\u043a\u0442\u043e\u0440 \u043a\u043e\u0434\u0430\u00bb.");
                                            break label1;
                                        } else {
                                            boolean b16 = s1.equals((Object)"run");
                                            label4: {
                                                label5: {
                                                    if (b16) {
                                                        break label5;
                                                    }
                                                    if (!s1.equals((Object)"exec")) {
                                                        break label4;
                                                    }
                                                }
                                                StringBuffer a = new StringBuffer("\u0417\u0430\u043f\u0443\u0441\u043a ");
                                                if (s0.length() == 0) {
                                                    s0 = "\u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b";
                                                }
                                                this.println(a.append(s0).append(" ...").toString());
                                                this.println("\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430 \u043e\u0442\u0440\u0430\u0431\u043e\u0442\u0430\u043b\u0430 (\u0432\u044b\u0432\u043e\u0434 \u0432 \u043c\u0435\u043d\u044e \u00ab\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b\u00bb).");
                                                break label1;
                                            }
                                            boolean b17 = s1.equals((Object)"exit");
                                            label2: {
                                                label3: {
                                                    if (b17) {
                                                        break label3;
                                                    }
                                                    if (!s1.equals((Object)"logout")) {
                                                        break label2;
                                                    }
                                                }
                                                this.println("\u0412\u044b\u0445\u043e\u0434 \u0432 \u043c\u0435\u043d\u044e \u0441\u0438\u0441\u0442\u0435\u043c\u044b.");
                                                return true;
                                            }
                                            boolean b18 = s1.equals((Object)"sudo");
                                            label0: {
                                                if (!b18) {
                                                    break label0;
                                                }
                                                if (!s0.startsWith("rm")) {
                                                    break label0;
                                                }
                                                this.println("\u041d\u0435\u0442.");
                                                break label1;
                                            }
                                            this.println(new StringBuffer("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430: ").append(s1).toString());
                                            this.println((this.kind != 0) ? "\u0421\u043c\u043e\u0442\u0440\u0438 man \u0438\u043b\u0438 help" : "\u0421\u043c\u043e\u0442\u0440\u0438 HELP");
                                            break label1;
                                        }
                                    }
                                }
                                StringBuffer a0 = new StringBuffer("\u0410\u0442\u0440\u0438\u0431\u0443\u0442\u044b \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u044b: ");
                                if (s0.length() <= 0) {
                                    s0 = "-";
                                }
                                this.println(a0.append(s0).toString());
                                break label1;
                            }
                        }
                        this.println("  PID  \u041f\u0420\u041e\u0426\u0415\u0421\u0421        \u041f\u0410\u041c\u042f\u0422\u042c");
                        this.println(new StringBuffer("    1  ").append((this.kind != 0) ? "init" : "COMMAND.COM").append("        ").append(this.ramKb / 20).append(" \u041a\u0411").toString());
                        this.println(new StringBuffer("    2  ").append((this.kind != 0) ? "editor" : "EDIT.COM").append("         ").append(this.ramKb / 8).append(" \u041a\u0411").toString());
                        this.println(new StringBuffer("    3  ").append((this.kind != 0) ? "x11" : "MOUSE.COM").append("           ").append(this.ramKb / 6).append(" \u041a\u0411").toString());
                    }
                }
            }
        }
        return false;
    }
    
    private void help() {
        if (this.kind != 0) {
            this.println("ls  cd  mkdir  rmdir  cat  cp  mv  rm  echo  pwd");
            this.println("free  df  ps  top  man  grep  chmod  whoami  uname");
            this.println("apt  clear  help  exit   (\u043f\u0435\u0440\u0435\u043d\u0430\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435: ls > list.txt)");
        } else {
            this.println("DIR  CD  MD  RD  TYPE  COPY  DEL  REN");
            this.println("ECHO  MEM  CHKDSK  FORMAT  TIME  DATE  CLS  VER");
            this.println("EDIT  RUN  HELP  EXIT   (\u0432\u044b\u0432\u043e\u0434 \u0432 \u0444\u0430\u0439\u043b: ECHO \u0434\u0430 > note.txt)");
        }
    }
    
    private void list() {
        int i = 0;
        int i0 = 0;
        int i1 = 0;
        long j = 0L;
        int i2 = 1;
        while(i2 < this.nodes) {
            if (this.parent[i2] == this.cwd) {
                i = i + 1;
                if (this.dir[i2]) {
                    i1 = i1 + 1;
                    this.println(new StringBuffer("  <DIR>      ").append(this.name[i2]).toString());
                } else {
                    i0 = i0 + 1;
                    int i3 = (this.body[i2] != null) ? this.body[i2].length() : 0;
                    j = j + (long)i3;
                    this.println(new StringBuffer("  ").append(this.pad(i3)).append("  ").append(this.name[i2]).toString());
                }
            }
            i2 = i2 + 1;
        }
        if (i == 0) {
            this.println("  (\u043f\u0443\u0441\u0442\u043e)");
        }
        this.println(new StringBuffer("  \u0444\u0430\u0439\u043b\u043e\u0432: ").append(i0).append(", \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u043e\u0432: ").append(i1).append(", ").append(j).append(" \u0431\u0430\u0439\u0442").toString());
    }
    
    private String pad(int i) {
        String s = new StringBuffer().append(i).toString();
        while(s.length() < 8) {
            s = new StringBuffer(" ").append(s).toString();
        }
        return s;
    }
    
    private void cd(String s) {
        if (s.length() != 0 && !s.equals((Object)".")) {
            if (s.equals((Object)"..")) {
                if (this.parent[this.cwd] >= 0) {
                    this.cwd = this.parent[this.cwd];
                }
                return;
            } else {
                int i = this.find(s, this.cwd);
                label1: {
                    label0: {
                        if (i < 0) {
                            break label0;
                        }
                        if (!this.dir[i]) {
                            break label0;
                        }
                        this.cwd = i;
                        break label1;
                    }
                    if (i < 0) {
                        this.println(new StringBuffer("\u041a\u0430\u0442\u0430\u043b\u043e\u0433 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: ").append(s).toString());
                    } else {
                        this.println("\u042d\u0442\u043e \u0444\u0430\u0439\u043b, \u0430 \u043d\u0435 \u043a\u0430\u0442\u0430\u043b\u043e\u0433");
                    }
                }
                return;
            }
        }
        this.println(this.path(this.cwd));
    }
    
    private void mkdir(String s) {
        if (s.length() == 0) {
            this.println("\u0423\u043a\u0430\u0436\u0438\u0442\u0435 \u0438\u043c\u044f \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0430");
            return;
        }
        if (this.find(s, this.cwd) >= 0) {
            this.println(new StringBuffer("\u0423\u0436\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442: ").append(s).toString());
            return;
        }
        this.addNode(s, this.cwd, true, (String)null);
        this.println(new StringBuffer("\u041a\u0430\u0442\u0430\u043b\u043e\u0433 \u0441\u043e\u0437\u0434\u0430\u043d: ").append(s).toString());
    }
    
    private void rmdir(String s) {
        int i = this.find(s, this.cwd);
        if (i < 0) {
            this.println(new StringBuffer("\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e: ").append(s).toString());
            return;
        }
        int i0 = 1;
        while(i0 < this.nodes) {
            if (this.parent[i0] == i) {
                this.println("\u041a\u0430\u0442\u0430\u043b\u043e\u0433 \u043d\u0435 \u043f\u0443\u0441\u0442");
                return;
            }
            i0 = i0 + 1;
        }
        this.remove(i);
        this.println(new StringBuffer("\u0423\u0434\u0430\u043b\u0451\u043d \u043a\u0430\u0442\u0430\u043b\u043e\u0433 ").append(s).toString());
    }
    
    private void remove(int i) {
        this.dir[i] = false;
        this.name[i] = "";
        this.body[i] = null;
        this.parent[i] = -2;
    }
    
    private void type(String s) {
        int i = this.find(s, this.cwd);
        if (i >= 0 && !this.dir[i]) {
            String s0 = (this.body[i] != null) ? this.body[i] : "";
            int i0 = 0;
            int i1 = 0;
            while(i1 <= s0.length()) {
                int i2 = s0.length();
                label0: {
                    label1: {
                        if (i1 == i2) {
                            break label1;
                        }
                        int i3 = s0.charAt(i1);
                        if (i3 != 10) {
                            break label0;
                        }
                    }
                    this.println(s0.substring(i0, i1));
                    i0 = i1 + 1;
                }
                i1 = i1 + 1;
            }
            return;
        }
        this.println(new StringBuffer("\u0424\u0430\u0439\u043b \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: ").append(s).toString());
    }
    
    private void copy(String s) {
        int i = s.indexOf(32);
        if (i < 0) {
            this.println("COPY \u043e\u0442\u043a\u0443\u0434\u0430 \u043a\u0443\u0434\u0430");
            return;
        }
        String s0 = s.substring(0, i).trim();
        String s1 = s.substring(i + 1).trim();
        int i0 = this.find(s0, this.cwd);
        if (i0 >= 0 && !this.dir[i0]) {
            int i1 = this.find(s1, this.cwd);
            if (i1 < 0) {
                this.addNode(s1, this.cwd, false, this.body[i0]);
            } else {
                this.body[i1] = this.body[i0];
            }
            this.println(new StringBuffer("\u0421\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u043e ").append(s0).append(" -> ").append(s1).toString());
            return;
        }
        this.println(new StringBuffer("\u0418\u0441\u0442\u043e\u0447\u043d\u0438\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: ").append(s0).toString());
    }
    
    private void del(String s) {
        int i = this.find(s, this.cwd);
        if (i < 0) {
            this.println(new StringBuffer("\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e: ").append(s).toString());
            return;
        }
        this.remove(i);
        this.println(new StringBuffer("\u0423\u0434\u0430\u043b\u0435\u043d\u043e: ").append(s).toString());
    }
    
    private void ren(String s) {
        int i = s.indexOf(32);
        if (i < 0) {
            this.println("REN \u0441\u0442\u0430\u0440\u043e\u0435 \u043d\u043e\u0432\u043e\u0435");
            return;
        }
        String s0 = s.substring(0, i).trim();
        String s1 = s.substring(i + 1).trim();
        int i0 = this.find(s0, this.cwd);
        if (i0 < 0) {
            this.println(new StringBuffer("\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e: ").append(s0).toString());
            return;
        }
        this.name[i0] = s1;
        this.println(new StringBuffer("\u041f\u0435\u0440\u0435\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u043d\u043e: ").append(s0).append(" -> ").append(s1).toString());
    }
    
    private void mem() {
        int i = 0;
        int i0 = 0;
        while(i0 < this.nodes) {
            if (this.body[i0] != null) {
                i = i + (this.body[i0].length() / 1024 + 1);
            }
            i0 = i0 + 1;
        }
        this.println(new StringBuffer("\u0412\u0441\u0435\u0433\u043e \u043f\u0430\u043c\u044f\u0442\u0438: ").append(this.ramKb).append(" \u041a\u0411").toString());
        this.println(new StringBuffer("\u0417\u0430\u043d\u044f\u0442\u043e \u0441\u0438\u0441\u0442\u0435\u043c\u043e\u0439: ").append(this.ramKb / 3).append(" \u041a\u0411").toString());
        this.println(new StringBuffer("\u0421\u0432\u043e\u0431\u043e\u0434\u043d\u043e: ").append(this.ramKb - this.ramKb / 3 - i).append(" \u041a\u0411").toString());
    }
    
    private void man(String s) {
        if (s.length() == 0) {
            this.println("man <\u043a\u043e\u043c\u0430\u043d\u0434\u0430> \u2014 \u0441\u043f\u0440\u0430\u0432\u043a\u0430 \u043f\u043e \u043a\u043e\u043c\u0430\u043d\u0434\u0435");
            return;
        }
        this.println(new StringBuffer("MAN ").append(rpc.Shell.upper(s)).toString());
        if (s.equals((Object)"ls")) {
            this.println("  \u0441\u043f\u0438\u0441\u043e\u043a \u0444\u0430\u0439\u043b\u043e\u0432 \u0432 \u0442\u0435\u043a\u0443\u0449\u0435\u043c \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0435");
        } else if (s.equals((Object)"cat")) {
            this.println("  \u043f\u043e\u043a\u0430\u0437\u0430\u0442\u044c \u0441\u043e\u0434\u0435\u0440\u0436\u0438\u043c\u043e\u0435 \u0444\u0430\u0439\u043b\u0430");
        } else if (s.equals((Object)"grep")) {
            this.println("  \u0438\u0441\u043a\u0430\u0442\u044c \u0442\u0435\u043a\u0441\u0442 \u0432 \u0444\u0430\u0439\u043b\u0435: grep \u0441\u043b\u043e\u0432\u043e \u0444\u0430\u0439\u043b");
        } else if (s.equals((Object)"ps")) {
            this.println("  \u0441\u043f\u0438\u0441\u043e\u043a \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0432");
        } else {
            this.println("  \u0441\u043f\u0440\u0430\u0432\u043a\u0438 \u043d\u0435\u0442, \u043d\u043e \u043a\u043e\u043c\u0430\u043d\u0434\u0430 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442");
        }
    }
    
    private void grep(String s) {
        int i = s.indexOf(32);
        if (i < 0) {
            this.println("grep \u0441\u043b\u043e\u0432\u043e \u0444\u0430\u0439\u043b");
            return;
        }
        String s0 = s.substring(0, i);
        String s1 = s.substring(i + 1).trim();
        int i0 = this.find(s1, this.cwd);
        if (i0 >= 0 && !this.dir[i0]) {
            String s2 = (this.body[i0] != null) ? this.body[i0] : "";
            String s3 = rpc.Shell.lower(s0);
            int i1 = 0;
            int i2 = 0;
            int i3 = 0;
            while(i3 <= s2.length()) {
                int i4 = s2.length();
                label0: {
                    label1: {
                        if (i3 == i4) {
                            break label1;
                        }
                        int i5 = s2.charAt(i3);
                        if (i5 != 10) {
                            break label0;
                        }
                    }
                    String s4 = s2.substring(i1, i3);
                    if (rpc.Shell.lower(s4).indexOf(s3) >= 0) {
                        this.println(s4);
                        i2 = i2 + 1;
                    }
                    i1 = i3 + 1;
                }
                i3 = i3 + 1;
            }
            if (i2 == 0) {
                this.println("\u0421\u043e\u0432\u043f\u0430\u0434\u0435\u043d\u0438\u0439 \u043d\u0435\u0442");
            }
            return;
        }
        this.println(new StringBuffer("\u0424\u0430\u0439\u043b \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: ").append(s1).toString());
    }
    
    private void writeFile(String s, String s0) {
        int i = this.find(s, this.cwd);
        label2: {
            label0: {
                label1: {
                    if (i < 0) {
                        break label1;
                    }
                    if (!this.dir[i]) {
                        break label0;
                    }
                }
                this.addNode(s, this.cwd, false, s0);
                break label2;
            }
            this.body[i] = s0;
        }
    }
    
    private int files() {
        int i = 0;
        int i0 = 0;
        while(i0 < this.nodes) {
            if (!this.dir[i0] && this.parent[i0] >= -1 && this.name[i0].length() > 0) {
                i = i + 1;
            }
            i0 = i0 + 1;
        }
        return i;
    }
    
    private static String lower(String s) {
        char[] a = s.toCharArray();
        int i = 0;
        while(i < a.length) {
            int i0 = a[i];
            if (i0 >= 65) {
                int i1 = a[i];
                if (i1 <= 90) {
                    int i2 = a[i];
                    int i3 = (char)(i2 + 32);
                    a[i] = (char)i3;
                }
            }
            i = i + 1;
        }
        return new String(a);
    }
    
    private static String upper(String s) {
        char[] a = s.toCharArray();
        int i = 0;
        while(i < a.length) {
            int i0 = a[i];
            if (i0 >= 97) {
                int i1 = a[i];
                if (i1 <= 122) {
                    int i2 = a[i];
                    int i3 = (char)(i2 - 32);
                    a[i] = (char)i3;
                }
            }
            i = i + 1;
        }
        return new String(a);
    }
}
