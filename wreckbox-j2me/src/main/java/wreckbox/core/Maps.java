package wreckbox.core;

/** Generated from Wreckbox 1.8; do not hand-edit. */
public final class Maps {
  public static final int COUNT = 8;
  public static final String[] IDS = {
    "city",
    "suburb",
    "megapolis",
    "industrial",
    "port",
    "npp",
    "base",
    "volcano"
  };
  public static final String[] NAMES = {
    "Город",
    "Пригород",
    "Мегаполис",
    "Промзона",
    "Порт",
    "Атомград",
    "Военная база",
    "Деревня под вулканом"
  };
  public static final int[] WIDTHS = {
    1200,
    1200,
    1440,
    1200,
    1200,
    1440,
    1440,
    1080
  };
  public static int find(String id) {
    for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
    return -1;
  }

  private Maps() {}
}
