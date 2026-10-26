package rpc;

public class Vm {
    final public static int LANG_BASIC = 0;
    final public static int LANG_ASM = 1;
    final public static int LANG_C = 2;
    final public static String[] LANG_NAME;
    final public static int[] LANG_YEAR;
    final public static String[] LANG_NOTE;
    final private static int OP_SET = 1;
    final private static int OP_ADD = 2;
    final private static int OP_MUL = 3;
    final private static int OP_PRINT = 4;
    final private static int OP_LOOP = 5;
    final private static int OP_NEXT = 6;
    final private static int OP_IFGT = 7;
    final private static int OP_ENDIF = 8;
    final private static int OP_MSG = 9;
    final private static int OP_SUB = 10;
    final private static int OP_BRACE = 11;
    final private static int OP_NOP = 12;
    final public static int MAX_LINES = 40;
    
    static {
        String[] a = new String[3];
        a[0] = "BASIC-";
        a[1] = "ASM-";
        a[2] = "C-";
        LANG_NAME = a;
        int[] a0 = new int[3];
        a0[0] = 1980;
        a0[1] = 1985;
        a0[2] = 1990;
        LANG_YEAR = a0;
        String[] a1 = new String[3];
        a1[0] = "\u0441\u0442\u0440\u043e\u043a\u0438-\u043e\u043f\u0435\u0440\u0430\u0442\u043e\u0440\u044b, \u043f\u0435\u0440\u0435\u043c\u0435\u043d\u043d\u044b\u0435-\u0431\u0443\u043a\u0432\u044b";
        a1[1] = "\u0440\u0435\u0433\u0438\u0441\u0442\u0440\u044b \u0438 \u043f\u0435\u0440\u0435\u0445\u043e\u0434\u044b, \u043a\u0430\u043a \u0432 MASM";
        a1[2] = "\u0444\u0438\u0433\u0443\u0440\u043d\u044b\u0435 \u0441\u043a\u043e\u0431\u043a\u0438 \u0438 \u0442\u043e\u0447\u043a\u0438 \u0441 \u0437\u0430\u043f\u044f\u0442\u043e\u0439";
        LANG_NOTE = a1;
    }
    
    public Vm() {
    }
    
    public static String[] palette(int i, int i0) {
        if (i == 0) {
            String[] a = new String[14];
            a[0] = "A=0";
            a[1] = "A=1";
            a[2] = "B=1";
            a[3] = "A=A+1";
            a[4] = "A=A+B";
            a[5] = "B=B+1";
            a[6] = "A=A*2";
            a[7] = "A=A-1";
            a[8] = "PRINT A";
            a[9] = "FOR 3";
            a[10] = "NEXT";
            a[11] = "IF A>10";
            a[12] = "END";
            a[13] = "?\"\u0425\u0415\u041b\u041b\u041e\"";
            return a;
        }
        if (i != 1) {
            String[] a0 = new String[14];
            a0[0] = "int a=0;";
            a0[1] = "int a=1;";
            a0[2] = "int b=1;";
            a0[3] = "a=a+1;";
            a0[4] = "a=a+b;";
            a0[5] = "b=b+1;";
            a0[6] = "a=a-1;";
            a0[7] = "a=a*2;";
            a0[8] = "print(a);";
            a0[9] = "loop(3){";
            a0[10] = "}";
            a0[11] = "if(a>10){";
            a0[12] = "}";
            a0[13] = "print(\"\u0425\u0415\u041b\u041b\u041e\");";
            return a0;
        }
        String[] a1 = new String[14];
        a1[0] = "MOV A,0";
        a1[1] = "MOV A,1";
        a1[2] = "MOV B,1";
        a1[3] = "ADD A,1";
        a1[4] = "ADD A,B";
        a1[5] = "ADD B,1";
        a1[6] = "SUB A,1";
        a1[7] = "MUL A,2";
        a1[8] = "OUT A";
        a1[9] = "REP 3";
        a1[10] = "ENDREP";
        a1[11] = "CMP A,10";
        a1[12] = "JEND";
        a1[13] = "MSG \"\u0425\u0415\u041b\u041b\u041e\"";
        return a1;
    }
    
    public static int paletteSize(int i) {
        return rpc.Vm.palette(i, 0).length;
    }
    
    private static rpc.Vm$Op parseLine(int i, String s) {
        String s0 = (s != null) ? s.trim() : "";
        if (s0.length() == 0) {
            rpc.Vm$Op a = null;
            return a;
        }
        if (!s0.startsWith("'") && !s0.startsWith("REM ") && !s0.equals((Object)"REM") && !s0.startsWith("//") && !s0.startsWith(";") && !s0.startsWith("#!")) {
            int i0 = 0;
            while(true) {
                if (i0 < s0.length()) {
                    int i1 = s0.charAt(i0);
                    if (i1 >= 48) {
                        int i2 = s0.charAt(i0);
                        if (i2 <= 57) {
                            i0 = i0 + 1;
                            continue;
                        }
                    }
                }
                label0: if (i0 > 0 && i0 < s0.length()) {
                    int i3 = s0.charAt(i0);
                    label1: {
                        if (i3 == 32) {
                            break label1;
                        }
                        int i4 = s0.charAt(i0);
                        if (i4 != 9) {
                            break label0;
                        }
                    }
                    s0 = s0.substring(i0).trim();
                    if (s0.length() == 0) {
                        return new rpc.Vm$Op(12, 0, 0, 0, 0, (String)null);
                    }
                }
                String s1 = rpc.Vm.up(s0);
                if (i == 0) {
                    return rpc.Vm.parseBasic(s1, s);
                }
                if (i != 1) {
                    return rpc.Vm.parseC(s1, s);
                }
                return rpc.Vm.parseAsm(s1, s);
            }
        }
        return new rpc.Vm$Op(12, 0, 0, 0, 0, (String)null);
    }
    
    private static String up(String s) {
        StringBuffer a = new StringBuffer(s.length());
        int i = 0;
        while(i < s.length()) {
            int i0 = s.charAt(i);
            label1: {
                label2: {
                    if (i0 < 97) {
                        break label2;
                    }
                    if (i0 > 122) {
                        break label2;
                    }
                    int i1 = (char)(i0 - 32);
                    a.append((char)i1);
                    break label1;
                }
                label0: {
                    if (i0 == 1072) {
                        break label0;
                    }
                    if (i0 == 1040) {
                        break label0;
                    }
                    a.append((char)i0);
                    break label1;
                }
                a.append((char)65);
            }
            i = i + 1;
        }
        return a.toString();
    }
    
    private static int varSlot(String s) {
        if (s != null && s.length() != 0) {
            int i = s.charAt(0);
            if (i >= 65 && i <= 90) {
                return i - 65;
            }
            if (i >= 97 && i <= 122) {
                return i - 97;
            }
            return 0;
        }
        return 0;
    }
    
    private static boolean isVarName(String s) {
        if (s.length() == 0) {
            return false;
        }
        int i = s.charAt(0);
        label0: {
            label2: {
                if (i < 65) {
                    break label2;
                }
                if (i <= 90) {
                    break label0;
                }
            }
            label1: {
                if (i < 97) {
                    break label1;
                }
                if (i <= 122) {
                    break label0;
                }
            }
            return false;
        }
        return true;
    }
    
    private static int[] value(String s, int i) {
        String s0 = s.trim();
        int i0 = s0.indexOf(43);
        int i1 = s0.indexOf(45);
        if (i0 > 0) {
            String s1 = s0.substring(0, i0).trim();
            String s2 = s0.substring(i0 + 1).trim();
            int i2 = (rpc.Vm.isVarName(s2)) ? 0 : rpc.Util.parseInt(s2);
            int[] a = new int[5];
            a[0] = rpc.Vm.varSlot(s1);
            a[1] = i2;
            a[2] = 2;
            a[3] = (rpc.Vm.isVarName(s2)) ? 1 : 0;
            a[4] = rpc.Vm.varSlot(s2);
            return a;
        }
        if (i1 > 0) {
            String s3 = s0.substring(0, i1).trim();
            String s4 = s0.substring(i1 + 1).trim();
            int i3 = (rpc.Vm.isVarName(s4)) ? 0 : rpc.Util.parseInt(s4);
            int[] a0 = new int[5];
            a0[0] = rpc.Vm.varSlot(s3);
            a0[1] = i3;
            a0[2] = 10;
            a0[3] = (rpc.Vm.isVarName(s4)) ? 1 : 0;
            a0[4] = rpc.Vm.varSlot(s4);
            return a0;
        }
        int i4 = s0.indexOf(42);
        if (i4 > 0) {
            String s5 = s0.substring(0, i4).trim();
            String s6 = s0.substring(i4 + 1).trim();
            int i5 = (rpc.Vm.isVarName(s6)) ? 0 : rpc.Util.parseInt(s6);
            int[] a1 = new int[5];
            a1[0] = rpc.Vm.varSlot(s5);
            a1[1] = i5;
            a1[2] = 3;
            a1[3] = (rpc.Vm.isVarName(s6)) ? 1 : 0;
            a1[4] = rpc.Vm.varSlot(s6);
            return a1;
        }
        if (!rpc.Vm.isVarName(s0)) {
            int[] a2 = new int[5];
            a2[1] = rpc.Util.parseInt(s0);
            a2[2] = i;
            a2[4] = -1;
            return a2;
        }
        int[] a3 = new int[5];
        a3[0] = rpc.Vm.varSlot(s0);
        a3[2] = i;
        a3[3] = 1;
        a3[4] = rpc.Vm.varSlot(s0);
        return a3;
    }
    
    private static String quoted(String s) {
        int i = s.indexOf(34);
        if (i < 0) {
            i = s.indexOf(39);
        }
        if (i < 0) {
            String s0 = null;
            return s0;
        }
        int i0 = s.charAt(i);
        int i1 = s.indexOf(i0, i + 1);
        if (i1 < 0) {
            String s1 = null;
            return s1;
        }
        return s.substring(i + 1, i1);
    }
    
    private static rpc.Vm$Op parseBasic(String s, String s0) {
        if (s.startsWith("?")) {
            String s1 = rpc.Vm.quoted(s0);
            if (s1 == null) {
                s1 = "";
            }
            return new rpc.Vm$Op(9, 0, 0, 0, 0, s1);
        } else {
            if (!s.startsWith("PRINT ") && !s.startsWith("? ")) {
                int i = 0;
                if (!s.startsWith("FOR ") && !s.startsWith("FOR I=")) {
                    if (!s.equals((Object)"NEXT") && !s.startsWith("NEXT ")) {
                        if (s.startsWith("IF ")) {
                            String s2 = s.substring(3).trim();
                            int i0 = s2.indexOf(62);
                            if (i0 < 0) {
                                rpc.Vm$Op a = null;
                                return a;
                            }
                            String s3 = s2.substring(0, i0).trim();
                            int i1 = rpc.Util.parseInt(s2.substring(i0 + 1).trim());
                            return new rpc.Vm$Op(7, rpc.Vm.varSlot(s3), i1, 0, 0, (String)null);
                        } else {
                            if (s.equals((Object)"END")) {
                                return new rpc.Vm$Op(8, 0, 0, 0, 0, (String)null);
                            }
                            int i2 = s.indexOf(61);
                            if (i2 <= 0) {
                                rpc.Vm$Op a0 = null;
                                return a0;
                            }
                            String s4 = s.substring(0, i2).trim();
                            int[] a1 = rpc.Vm.value(s.substring(i2 + 1).trim(), 1);
                            return new rpc.Vm$Op(a1[2], rpc.Vm.varSlot(s4), a1[1], a1[3], a1[4], (String)null);
                        }
                    }
                    return new rpc.Vm$Op(6, 0, 0, 0, 0, (String)null);
                }
                String s5 = s.substring(4).trim();
                int i3 = s5.indexOf(" TO ");
                String s6 = (i3 <= 0) ? s5 : s5.substring(i3 + 4).trim();
                int i4 = s6.indexOf(61);
                if (i4 >= 0) {
                    s6 = s6.substring(i4 + 1).trim();
                }
                int i5 = rpc.Util.parseInt(s6);
                if (i5 <= 0) {
                    i5 = 1;
                }
                int i6 = s5.indexOf(61);
                if (i6 <= 0) {
                    i = 0;
                } else {
                    String s7 = s5.substring(0, i6).trim();
                    i = (rpc.Vm.isVarName(s7)) ? rpc.Vm.varSlot(s7) : 0;
                }
                return new rpc.Vm$Op(5, i, i5, 0, 0, (String)null);
            }
            String s8 = (s.startsWith("PRINT ")) ? s.substring(6).trim() : s.substring(2).trim();
            String s9 = rpc.Vm.quoted(s0);
            if (s9 != null) {
                return new rpc.Vm$Op(9, 0, 0, 0, 0, s9);
            }
            if (!rpc.Vm.isVarName(s8)) {
                return new rpc.Vm$Op(4, 0, rpc.Util.parseInt(s8), 0, 0, (String)null);
            }
            return new rpc.Vm$Op(4, rpc.Vm.varSlot(s8), 0, 1, 0, (String)null);
        }
    }
    
    private static rpc.Vm$Op parseAsm(String s, String s0) {
        int i = s.indexOf(32);
        String s1 = (i >= 0) ? s.substring(0, i) : s;
        String s2 = (i >= 0) ? s.substring(i + 1).trim() : "";
        if (s1.equals((Object)"MSG")) {
            String s3 = rpc.Vm.quoted(s0);
            if (s3 == null) {
                s3 = "";
            }
            return new rpc.Vm$Op(9, 0, 0, 0, 0, s3);
        } else if (s1.equals((Object)"MOV")) {
            int i0 = s2.indexOf(44);
            if (i0 < 0) {
                rpc.Vm$Op a = null;
                return a;
            }
            return new rpc.Vm$Op(1, rpc.Vm.varSlot(s2.substring(0, i0)), rpc.Util.parseInt(s2.substring(i0 + 1)), 0, -1, (String)null);
        } else {
            if (!s1.equals((Object)"ADD") && !s1.equals((Object)"SUB") && !s1.equals((Object)"MUL")) {
                if (s1.equals((Object)"OUT")) {
                    return new rpc.Vm$Op(4, rpc.Vm.varSlot(s2), 0, 1, 0, (String)null);
                }
                if (s1.equals((Object)"REP")) {
                    return new rpc.Vm$Op(5, 0, rpc.Util.parseInt(s2), 0, 0, (String)null);
                }
                if (s1.equals((Object)"ENDREP")) {
                    return new rpc.Vm$Op(6, 0, 0, 0, 0, (String)null);
                }
                if (s1.equals((Object)"CMP")) {
                    int i1 = s2.indexOf(44);
                    if (i1 < 0) {
                        rpc.Vm$Op a0 = null;
                        return a0;
                    }
                    return new rpc.Vm$Op(7, rpc.Vm.varSlot(s2.substring(0, i1)), rpc.Util.parseInt(s2.substring(i1 + 1)), 0, 0, (String)null);
                } else {
                    if (!s1.equals((Object)"JEND")) {
                        rpc.Vm$Op a1 = null;
                        return a1;
                    }
                    return new rpc.Vm$Op(8, 0, 0, 0, 0, (String)null);
                }
            }
            int i2 = s2.indexOf(44);
            if (i2 < 0) {
                rpc.Vm$Op a2 = null;
                return a2;
            }
            return new rpc.Vm$Op((s1.equals((Object)"ADD")) ? 2 : (s1.equals((Object)"SUB")) ? 10 : 3, rpc.Vm.varSlot(s2.substring(0, i2)), rpc.Util.parseInt(s2.substring(i2 + 1)), 0, -1, (String)null);
        }
    }
    
    private static rpc.Vm$Op parseC(String s, String s0) {
        if (s.startsWith("PRINT")) {
            String s1 = rpc.Vm.quoted(s0);
            if (s1 != null) {
                return new rpc.Vm$Op(9, 0, 0, 0, 0, s1);
            }
            int i = s.indexOf(40);
            int i0 = s.indexOf(41);
            if (i >= 0 && i0 >= i) {
                return new rpc.Vm$Op(4, rpc.Vm.varSlot(s.substring(i + 1, i0).trim()), 0, 1, 0, (String)null);
            }
            rpc.Vm$Op a = null;
            return a;
        } else {
            if (s.startsWith("LOOP")) {
                return new rpc.Vm$Op(5, 0, rpc.Util.parseInt(s.substring(s.indexOf(40) + 1)), 0, 0, (String)null);
            }
            if (s.startsWith("FOR")) {
                int i1 = s.indexOf(59);
                int i2 = s.indexOf(59, i1 + 1);
                if (i1 > 0 && i2 > i1) {
                    String s2 = s.substring(i1 + 1, i2).trim();
                    int i3 = s2.indexOf(60);
                    if (i3 >= 0) {
                        int i4 = rpc.Util.parseInt(s2.substring(i3 + 1).trim());
                        if (i4 <= 0) {
                            i4 = 1;
                        }
                        return new rpc.Vm$Op(5, 0, i4, 0, 0, (String)null);
                    }
                }
                rpc.Vm$Op a0 = null;
                return a0;
            } else if (s.startsWith("WHILE")) {
                int i5 = s.indexOf(40);
                int i6 = s.indexOf(41);
                if (i5 >= 0 && i6 >= i5) {
                    int i7 = s.indexOf(60, i5);
                    if (i7 < 0) {
                        rpc.Vm$Op a1 = null;
                        return a1;
                    }
                    int i8 = rpc.Util.parseInt(s.substring(i7 + 1, i6).trim());
                    if (i8 <= 0) {
                        i8 = 1;
                    }
                    return new rpc.Vm$Op(5, 0, i8, 0, 0, (String)null);
                }
                rpc.Vm$Op a2 = null;
                return a2;
            } else if (s.startsWith("IF")) {
                int i9 = s.indexOf(40);
                int i10 = s.indexOf(41);
                if (i9 >= 0 && i10 >= i9) {
                    String s3 = s.substring(i9 + 1, i10).trim();
                    int i11 = s3.indexOf(62);
                    if (i11 < 0) {
                        rpc.Vm$Op a3 = null;
                        return a3;
                    }
                    return new rpc.Vm$Op(7, rpc.Vm.varSlot(s3.substring(0, i11).trim()), rpc.Util.parseInt(s3.substring(i11 + 1)), 0, 0, (String)null);
                }
                rpc.Vm$Op a4 = null;
                return a4;
            } else {
                if (s.equals((Object)"}")) {
                    return new rpc.Vm$Op(11, 0, 0, 0, 0, (String)null);
                }
                if (s.startsWith("INT ")) {
                    int i12 = s.indexOf(61);
                    String s4 = (i12 <= 0) ? s.substring(4) : s.substring(4, i12);
                    String s5 = (i12 <= 0) ? "0" : s.substring(i12 + 1);
                    while(s5.endsWith(";")) {
                        s5 = s5.substring(0, s5.length() - 1);
                    }
                    return new rpc.Vm$Op(1, rpc.Vm.varSlot(s4), rpc.Util.parseInt(s5), 0, -1, (String)null);
                } else {
                    int i13 = s.indexOf(61);
                    if (i13 <= 0) {
                        rpc.Vm$Op a5 = null;
                        return a5;
                    }
                    String s6 = s.substring(0, i13).trim();
                    String s7 = s.substring(i13 + 1).trim();
                    while(s7.endsWith(";")) {
                        s7 = s7.substring(0, s7.length() - 1);
                    }
                    int[] a6 = rpc.Vm.value(s7, 1);
                    return new rpc.Vm$Op(a6[2], rpc.Vm.varSlot(s6), a6[1], a6[3], a6[4], (String)null);
                }
            }
        }
    }
    
    public static rpc.Vm$Run run(int i, String[] a) {
        rpc.Vm$Run a0 = new rpc.Vm$Run();
        int i0 = (a != null) ? a.length : 0;
        int[] a1 = new int[i0];
        int[] a2 = new int[i0];
        int[] a3 = new int[i0];
        int[] a4 = new int[i0];
        int[] a5 = new int[i0];
        String[] a6 = new String[i0];
        int[] a7 = new int[16];
        int i1 = 0;
        int i2 = 0;
        int i3 = 0;
        while(i3 < i0) {
            if (a[i3] != null && a[i3].trim().length() != 0) {
                rpc.Vm$Op a8 = rpc.Vm.parseLine(i, a[i3]);
                if (a8 == null) {
                    a0.ok = false;
                    a0.error = new StringBuffer("\u0421\u0442\u0440\u043e\u043a\u0430 ").append(i3 + 1).append(": \u0441\u0438\u043d\u0442\u0430\u043a\u0441\u0438\u0441").toString();
                    return a0;
                }
                int i4 = a8.order;
                int i5 = i2;
                if (i4 != 12) {
                    label1: if (a8.order != 11) {
                        int i6 = a8.order;
                        i2 = i5;
                        label2: {
                            if (i6 == 5) {
                                break label2;
                            }
                            int i7 = a8.order;
                            i5 = i2;
                            if (i7 != 7) {
                                break label1;
                            }
                        }
                        int i8 = a7.length;
                        i2 = i5;
                        if (i5 < i8) {
                            i2 = i5 + 1;
                            a7[i5] = a8.order;
                        }
                    } else {
                        int i9 = 0;
                        int i10 = 0;
                        if (i5 <= 0) {
                            i9 = 8;
                        } else {
                            i5 = i5 + -1;
                            i9 = a7[i5];
                        }
                        i2 = i5;
                        if (i9 != 5) {
                            i10 = 8;
                            i2 = i5;
                        } else {
                            i10 = 6;
                        }
                        a8.order = i10;
                    }
                    a1[i1] = a8.order;
                    a2[i1] = a8.a;
                    a3[i1] = a8.v;
                    a4[i1] = a8.ve;
                    a5[i1] = a8.vei;
                    a6[i1] = a8.text;
                    i1 = i1 + 1;
                }
            }
            i3 = i3 + 1;
        }
        if (i1 == 0) {
            a0.ok = false;
            a0.error = "\u041f\u0443\u0441\u0442\u0430\u044f \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430";
            return a0;
        }
        int[] a9 = new int[26];
        int[] a10 = new int[8];
        int[] a11 = new int[8];
        int[] a12 = new int[8];
        StringBuffer a13 = new StringBuffer();
        int[] a14 = new int[32];
        int i11 = 0;
        int i12 = 0;
        int i13 = 0;
        int i14 = 0;
        label0: while(true) {
            int i15 = i12;
            if (i13 < i1) {
                i14 = i14 + 1;
                if (i14 <= 3000) {
                    int i16 = a1[i13];
                    if (i16 != 1) {
                        if (i16 != 2) {
                            if (i16 != 10) {
                                if (i16 != 3) {
                                    if (i16 != 4) {
                                        if (i16 != 9) {
                                            i12 = i15;
                                            if (i16 != 5) {
                                                if (i16 != 6) {
                                                    if (i16 != 7) {
                                                        i12 = i15;
                                                    } else {
                                                        int i17 = a9[a2[i13]];
                                                        int i18 = a3[i13];
                                                        i12 = i15;
                                                        if (i17 > i18) {
                                                            i12 = i15;
                                                        } else {
                                                            i13 = i13 + 1;
                                                            int i19 = 1;
                                                            while(true) {
                                                                if (i13 < i1 && i19 > 0) {
                                                                    if (a1[i13] != 7) {
                                                                        if (a1[i13] == 8) {
                                                                            i19 = i19 + -1;
                                                                        }
                                                                    } else {
                                                                        i19 = i19 + 1;
                                                                    }
                                                                    i13 = i13 + 1;
                                                                    continue;
                                                                }
                                                                continue label0;
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    i12 = i15;
                                                    if (i11 <= 0) {
                                                        i12 = i15;
                                                    } else {
                                                        int i20 = i11 - 1;
                                                        a10[i20] = a10[i20] - 1;
                                                        if ((a10[i11 - 1]) > 0) {
                                                            if ((a12[i11 - 1]) > 0) {
                                                                int i21 = a12[i11 - 1];
                                                                a9[i21] = a9[i21] + 1;
                                                            }
                                                            i13 = a11[i11 - 1];
                                                            continue;
                                                        }
                                                        i11 = i11 + -1;
                                                    }
                                                }
                                            } else if (i11 < a10.length) {
                                                a10[i11] = (a3[i13] > 0) ? a3[i13] : 1;
                                                a11[i11] = i13 + 1;
                                                a12[i11] = a2[i13];
                                                if ((a2[i13]) > 0) {
                                                    a9[a2[i13]] = 1;
                                                }
                                                i11 = i11 + 1;
                                            }
                                        } else {
                                            String s = null;
                                            String s0 = a6[i13];
                                            i12 = i15;
                                            if (s0 != null) {
                                                s = a6[i13];
                                                i12 = i15;
                                            } else {
                                                s = "";
                                            }
                                            a13.append(s).append((char)10);
                                        }
                                    } else {
                                        boolean b = (a4[i13] != 0) ? a9[a2[i13]] != 0 : a3[i13] != 0;
                                        a13.append(b ? 1 : 0).append((char)10);
                                        int i22 = a14.length;
                                        i12 = i15;
                                        if (i15 < i22) {
                                            i12 = i15 + 1;
                                            a14[i15] = b ? 1 : 0;
                                        }
                                    }
                                } else {
                                    int i23 = 0;
                                    int i24 = a2[i13];
                                    int i25 = a9[i24];
                                    boolean b0 = a4[i13] != 0;
                                    i12 = i15;
                                    if (b0) {
                                        i23 = a9[a5[i13]];
                                    } else {
                                        i23 = a3[i13];
                                        i12 = i15;
                                    }
                                    a9[i24] = i25 * i23;
                                }
                            } else {
                                int i26 = 0;
                                int i27 = a2[i13];
                                int i28 = a9[i27];
                                boolean b1 = a4[i13] != 0;
                                i12 = i15;
                                if (b1) {
                                    i26 = a9[a5[i13]];
                                } else {
                                    i26 = a3[i13];
                                    i12 = i15;
                                }
                                a9[i27] = i28 - i26;
                            }
                        } else {
                            int i29 = 0;
                            int i30 = a2[i13];
                            int i31 = a9[i30];
                            boolean b2 = a4[i13] != 0;
                            i12 = i15;
                            if (b2) {
                                i29 = a9[a5[i13]];
                            } else {
                                i29 = a3[i13];
                                i12 = i15;
                            }
                            a9[i30] = i31 + i29;
                        }
                    } else {
                        boolean b3 = false;
                        int i32 = a2[i13];
                        boolean b4 = a4[i13] != 0;
                        i12 = i15;
                        if (b4) {
                            b3 = a9[a5[i13]] != 0;
                        } else {
                            b3 = a3[i13] != 0;
                            i12 = i15;
                        }
                        a9[i32] = b3 ? 1 : 0;
                    }
                    i13 = i13 + 1;
                    a0.steps = i14;
                    continue;
                }
                a0.ok = false;
                a0.error = "\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430 \u0437\u0430\u0446\u0438\u043a\u043b\u0438\u043b\u0430\u0441\u044c (\u0441\u0442\u043e\u043f)";
                i12 = i15;
            }
            a0.lines = rpc.Util.split(a13.toString(), (char)10);
            if (a0.lines.length > 0 && a0.lines[a0.lines.length - 1].length() == 0) {
                String[] a15 = new String[a0.lines.length - 1];
                System.arraycopy((Object)a0.lines, 0, (Object)a15, 0, a15.length);
                a0.lines = a15;
            }
            a0.nums = new int[i12];
            System.arraycopy((Object)a14, 0, (Object)a0.nums, 0, i12);
            return a0;
        }
    }
    
    public static boolean solves(rpc.Vm$Run a, int i) {
        if (a != null && a.ok && a.error == null && a.countNums() > 0 && a.lastNum() == i) {
            return true;
        }
        return false;
    }
}
