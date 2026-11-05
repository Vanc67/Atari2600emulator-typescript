import java.io.IOException;

/**
 * Toolchain self test.
 *
 * Every construct here is one the JVM -> smali translator has to handle:
 * loops, switches, arrays, strings, long arithmetic, exceptions, static and
 * instance fields, virtual and interface calls, try/finally, nested types.
 *
 * The probe prints one deterministic line per check. `selftest.sh` runs it on
 * the JVM and on the built dex (through dvmlite) and diffs the two outputs:
 * identical output from two different engines means the translation and the
 * dex are right.
 */
public final class Probe {

    interface Op {
        int apply(int a, int b);
    }

    static final class Add implements Op {
        public int apply(int a, int b) { return a + b; }
    }

    static final class Mul implements Op {
        public int apply(int a, int b) { return a * b; }
    }

    static int staticCounter;
    private int value;
    private long wide;
    private String name;
    private int[] data;

    public Probe(int v) {
        value = v;
        wide = (long) v * 1000003L;
        name = "P" + v;
        data = new int[4];
    }

    public int value() { return value; }

    public long wide() { return wide; }

    public String name() { return name; }

    public Probe set(int index, int v) {
        data[index] = v;
        return this;
    }

    public int get(int index) { return data[index]; }

    static int fib(int n) {
        return n < 2 ? n : fib(n - 1) + fib(n - 2);
    }

    static int sumLoop(int n) {
        int s = 0;
        for (int i = 0; i < n; i++) s += i;
        return s;
    }

    static int whileLoop(int n) {
        int s = 0;
        int i = n;
        while (i > 0) {
            s += (i & 1) == 0 ? 1 : 2;
            i--;
        }
        return s;
    }

    static int switchDense(int x) {
        switch (x) {
            case 0: return 10;
            case 1: return 11;
            case 2: return 12;
            case 3: return 13;
            case 4: return 14;
            default: return -1;
        }
    }

    static int switchSparse(int x) {
        switch (x) {
            case 1: return 100;
            case 100: return 200;
            case 10000: return 300;
            default: return -1;
        }
    }

    static int arrays() {
        int[] a = new int[6];
        for (int i = 0; i < a.length; i++) a[i] = i * i - 3;
        int s = 0;
        for (int i = 0; i < a.length; i++) s += a[i];
        byte[] b = new byte[4];
        b[0] = (byte) 200;
        b[1] = (byte) -1;
        short[] sh = new short[2];
        sh[0] = (short) 40000;
        char[] c = new char[2];
        c[0] = 'A';
        long[] l = new long[3];
        l[2] = 1234567890123L;
        return s + b[0] + b[1] + sh[0] + c[0] + (int) l[2];
    }

    static int strings() {
        String a = "Hello";
        String b = "hello";
        String c = a.toLowerCase();
        int n = a.length() + b.length();
        int eq = a.equals("Hello") ? 1 : 0;
        int eqi = a.equalsIgnoreCase(b) ? 1 : 0;
        int idx = "abcdef".indexOf("cd");
        return n + eq + eqi + idx + (c.equals(b) ? 1 : 0);
    }

    static long longs() {
        long a = 1234567890123L;
        long b = 7L;
        long c = a / b;
        long d = a % b;
        long e = a >>> 3;
        long f = a >> 3;
        long g = a << 2;
        return c + d + e + f + g + (a > b ? 1 : 0) + (a == 1234567890123L ? 2 : 0);
    }

    static int ints(int a, int b) {
        int r = a + b;
        r = r * 3 - b;
        r ^= 0x5A5A;
        r |= 0x100;
        r &= 0xFFFF;
        r >>= 2;
        r <<= 1;
        r = -r;
        return r;
    }

    static int div(int a, int b) {
        try {
            return a / b;
        } catch (ArithmeticException e) {
            return -999;
        } finally {
            staticCounter++;
        }
    }

    static String exceptions(int which) {
        try {
            if (which == 0) throw new RuntimeException("boom");
            if (which == 1) throw new IOException("io");
            return "none";
        } catch (IOException e) {
            return "io:" + e.getMessage();
        } catch (RuntimeException e) {
            return "rt:" + e.getMessage();
        }
    }

    static int objects() {
        Probe p = new Probe(7);
        p.set(0, 42).set(1, 43);
        Probe q = new Probe(9);
        return p.value() + q.value() + p.get(0) + p.get(1) + q.get(0)
                + (int) (p.wide() % 1000) + p.name().length();
    }

    static int virtualDispatch(int mode, int a, int b) {
        Op op = mode == 0 ? (Op) new Add() : (Op) new Mul();
        return op.apply(a, b);
    }

    static int ternaryChain(int x) {
        return x < 0 ? -1 : (x == 0 ? 0 : (x < 10 ? 1 : 2));
    }

    static int recursion(int n) {
        if (n <= 1) return 1;
        return n * recursion(n - 1);
    }

    static String concatAll(int n) {
        String s = "";
        for (int i = 0; i < n; i++) {
            s = s + i + ",";
        }
        return s;
    }

    static int statics() {
        staticCounter = 0;
        staticCounter += 5;
        staticCounter *= 3;
        return staticCounter;
    }

    static int casts(long big) {
        int i = (int) big;
        byte b = (byte) i;
        short s = (short) i;
        char c = (char) i;
        long back = (long) b + (long) s + (long) c + (long) i;
        return b + s + c + (int) (back & 0xFF);
    }

    static int nested(int n) {
        int total = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if ((i + j) % 3 == 0) continue;
                if (i * j > 40) break;
                total += i * j;
            }
        }
        return total;
    }

    static boolean booleans(int a, boolean flag) {
        boolean big = a > 100 && !flag;
        boolean small = a < 10 || flag;
        return big ^ small;
    }

    static int arrayObjects() {
        Probe[] ps = new Probe[3];
        for (int i = 0; i < ps.length; i++) ps[i] = new Probe(i + 1);
        int s = 0;
        for (int i = 0; i < ps.length; i++) s += ps[i].value();
        Probe[][] grid = new Probe[2][];
        grid[1] = new Probe[2];
        grid[1][0] = ps[0];
        return s + (grid[1][0] == null ? 0 : grid[1][0].value());
    }

    static int stringCompare() {
        String[] names = new String[3];
        names[0] = "bbb";
        names[1] = "aaa";
        names[2] = "ccc";
        for (int i = 1; i < names.length; i++) {
            String v = names[i];
            int j = i - 1;
            while (j >= 0 && names[j].compareToIgnoreCase(v) > 0) {
                names[j + 1] = names[j];
                j--;
            }
            names[j + 1] = v;
        }
        return (names[0] + names[1] + names[2]).length();
    }

    public static void main(String[] args) {
        System.out.println("fib=" + fib(12));
        System.out.println("sum=" + sumLoop(100));
        System.out.println("while=" + whileLoop(17));
        System.out.println("switchDense=" + switchDense(3) + "," + switchDense(9));
        System.out.println("switchSparse=" + switchSparse(100) + ","
                + switchSparse(10000) + "," + switchSparse(5));
        System.out.println("arrays=" + arrays());
        System.out.println("strings=" + strings());
        System.out.println("longs=" + longs());
        System.out.println("ints=" + ints(5, 3));
        System.out.println("div=" + div(100, 5) + "," + div(1, 0) + ","
                + staticCounter);
        System.out.println("exceptions=" + exceptions(0) + "," + exceptions(1)
                + "," + exceptions(2));
        System.out.println("objects=" + objects());
        System.out.println("dispatch=" + virtualDispatch(0, 6, 7) + ","
                + virtualDispatch(1, 6, 7));
        System.out.println("ternary=" + ternaryChain(-5) + ternaryChain(0)
                + ternaryChain(5) + ternaryChain(50));
        System.out.println("recursion=" + recursion(9));
        System.out.println("concat=" + concatAll(4));
        System.out.println("statics=" + statics());
        System.out.println("casts=" + casts(0x7FFFFF80L));
        System.out.println("nested=" + nested(9));
        System.out.println("booleans=" + booleans(50, false) + ","
                + booleans(5, true) + "," + booleans(500, true));
        System.out.println("arrayObjects=" + arrayObjects());
        System.out.println("stringCompare=" + stringCompare());
        System.out.println("done");
    }
}
