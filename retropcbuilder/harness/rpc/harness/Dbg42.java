package rpc.harness;
import rpc.Game;

// RasterGfx lives in this package

/** the opening screen, the world it opens, and the clock that must move. */
public final class Dbg42 {
    static TestHost host = new TestHost("data");

    public static void main(String[] a) {
        // a fresh device opens on the year picker, not in the same grey room
        Game g = new Game();
        g.init(host, new RasterGfx(240, 320));
        System.out.println("fresh mode=" + g.mode() + " (57 = the year picker)");

        g.openStart();
        System.out.println("openStart year=" + g.startYear() + " tod=" + g.startTod()
                + " season=" + g.startSeason() + " weather=" + g.startWeather());

        for (int i = 0; i < 5; i++) g.input(Game.ACT_DOWN);
        int r1 = g.startRow();
        for (int i = 0; i < 5; i++) g.input(Game.ACT_UP);
        System.out.println("rows: 5 DOWN -> " + r1 + ", 5 UP -> " + g.startRow());
        g.input(Game.ACT_NUM3);
        System.out.println("NUM3 -> row " + g.startRow());

        // the year is walked one at a time and stops at both ends
        g.input(Game.ACT_NUM1);
        int y0 = g.startYear();
        for (int i = 0; i < 3; i++) g.input(Game.ACT_RIGHT);
        int y1 = g.startYear();
        for (int i = 0; i < 200; i++) g.input(Game.ACT_RIGHT);
        int yMax = g.startYear();
        for (int i = 0; i < 500; i++) g.input(Game.ACT_LEFT);
        int yMin = g.startYear();
        System.out.println("year: " + y0 + " +3 -> " + y1 + ", max " + yMax
                + ", min " + yMin);

        g.input(Game.ACT_NUM2);
        int t0 = g.startTod();
        for (int i = 0; i < 5; i++) g.input(Game.ACT_RIGHT);
        System.out.println("tod " + t0 + " +5 -> " + g.startTod());
        g.input(Game.ACT_NUM4);
        int w0 = g.startWeather();
        for (int i = 0; i < 6; i++) g.input(Game.ACT_RIGHT);
        System.out.println("weather " + w0 + " +6 -> " + g.startWeather());

        // choosing a year steps out onto the street
        g.input(Game.ACT_NUM1);
        for (int i = 0; i < 4; i++) g.input(Game.ACT_RIGHT);
        int year = g.startYear();
        g.input(Game.ACT_NUM2);
        for (int i = 0; i < 3; i++) g.input(Game.ACT_RIGHT);
        g.input(Game.ACT_NUM3);
        g.input(Game.ACT_NUM4);
        g.input(Game.ACT_FIRE);
        rpc.LifeSim l = g.life();
        System.out.println("after OK: mode=" + g.mode() + " (48 = the street)"
                + " year=" + l.curYear + " hour=" + l.curHour
                + " tod=" + l.timeOfDay + " season=" + l.season
                + " weather=" + l.weather);
        g.input(Game.ACT_BACK);
        System.out.println("after BACK: mode=" + g.mode() + " (0 = the menu)");

        // the clock has to move on every screen, not only in the room
        Game g2 = new Game();
        g2.init(host, new RasterGfx(240, 320));
        int d0 = g2.dayNumber(), h0 = g2.life().curHour, m0 = g2.life().curMinute;
        for (int i = 0; i < 240; i++) g2.paint();
        System.out.println("clock on the menu: " + h0 + ":" + m0 + " -> "
                + g2.life().curHour + ":" + g2.life().curMinute
                + "  day " + d0 + " -> " + g2.dayNumber());

        g2.openStart();
        g2.input(Game.ACT_NUM2);
        for (int i = 0; i < 3; i++) g2.input(Game.ACT_RIGHT);
        g2.input(Game.ACT_NUM3);
        g2.input(Game.ACT_NUM4);
        g2.input(Game.ACT_FIRE);
        int d1 = g2.dayNumber(), h1 = g2.life().curHour, m1 = g2.life().curMinute;
        for (int i = 0; i < 240; i++) g2.paint();
        System.out.println("clock on the street: " + h1 + ":" + m1 + " -> "
                + g2.life().curHour + ":" + g2.life().curMinute
                + "  day " + d1 + " -> " + g2.dayNumber());
    }
}
