package rpc;

/**
 * Procedural pixel-art icon set.
 *
 * No image files are involved: every icon is a 16x16 character grid that is
 * painted with the current palette, so icons stay crisp on any screen size
 * and cost nothing to load.  This replaced the "abstract shapes" of the 4.8
 * build (the room was literally an orange rectangle with two lines through
 * it) with recognisable objects: monitor, tower, mug, bed, door, ...
 */
public final class Icons {

    public static final int MONITOR = 0;
    public static final int CASE = 1;
    public static final int MUG = 2;
    public static final int BED = 3;
    public static final int DOOR = 4;
    public static final int CITY = 5;
    public static final int FLASK = 6;
    public static final int BOARD = 7;
    public static final int CALENDAR = 8;
    public static final int INFO = 9;
    public static final int WRENCH = 10;
    public static final int CAT = 11;
    public static final int RADIO = 12;
    public static final int LAMP = 13;
    public static final int CART = 14;
    public static final int CHIP = 15;
    public static final int COUNT = 16;

    private static final int GRID = 16;

    /**
     * Icon definitions.  '#' outline, '+' body, 'o' filled area, '@' light,
     * '.' and ' ' transparent.
     */
    private static final String[][] ART = {
        /* MONITOR */ {
            "................",
            ".##############.",
            ".#++++++++++++#.",
            ".#+oooooooooo+#.",
            ".#+o@@@@@@@@o+#.",
            ".#+o@......@o+#.",
            ".#+o@......@o+#.",
            ".#+o@@@@@@@@o+#.",
            ".#+oooooooooo+#.",
            ".#++++++++++++#.",
            ".##############.",
            "......####......",
            "......####......",
            "....########....",
            "................",
            "................",
        },
        /* CASE */ {
            "................",
            "..############..",
            "..#++++++++++#..",
            "..#+..@....++#..",
            "..#++++++++++#..",
            "..#+########+#..",
            "..#+#......#+#..",
            "..#+#..oo..#+#..",
            "..#+#......#+#..",
            "..#+########+#..",
            "..#++++++++++#..",
            "..#++@+++++++#..",
            "..#++++++++++#..",
            "..############..",
            "................",
            "................",
        },
        /* MUG */ {
            "................",
            "....o..o...+....",
            "...o..o..o......",
            "....o..o..o.....",
            "................",
            "..##########....",
            "..#++++++++#.#..",
            "..#+oooooo+#..#.",
            "..#+oooooo+#...#",
            "..#+oooooo+#..#.",
            "..#++++++++#.#..",
            "..##########....",
            "...########.....",
            "................",
            "................",
            "................",
        },
        /* BED */ {
            "................",
            "................",
            "................",
            "..#.............",
            "..#.............",
            ".############...",
            ".#@@@@@@@@@@#...",
            ".############...",
            ".#oooooooooo#...",
            ".############...",
            "..#........#....",
            "..#........#....",
            "..#........#....",
            "..##########....",
            "................",
            "................",
        },
        /* DOOR */ {
            "................",
            "..############..",
            "..#++++++++++#..",
            "..#+########+#..",
            "..#+#......#+#..",
            "..#+#......#+#..",
            "..#+#......#+#..",
            "..#+########+#..",
            "..#++++++++++#..",
            "..#++++++++++#..",
            "..#+########+#..",
            "..#+#......#+#..",
            "..#+#....@.#+#..",
            "..#+########+#..",
            "..############..",
            "................",
        },
        /* CITY */ {
            "................",
            "....##..........",
            "....##...###....",
            "..######.##.....",
            "..#....#.##..#..",
            "..#.@..#.##..#..",
            "..#....#.##..#..",
            ".###..###.##..#.",
            ".#....#....#..#.",
            ".#.@..#.@..#..#.",
            ".#....#....#..#.",
            "##############..",
            "..#..#..#..#....",
            "..#..#..#..#....",
            "................",
            "................",
        },
        /* FLASK */ {
            "................",
            ".....######.....",
            "......####......",
            ".......##.......",
            ".......##.......",
            "......#++#......",
            ".....#++++#.....",
            "....#++++++#....",
            "...#++++++++#...",
            "..#++++++++++#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "..#oooooooooo#..",
            "...##########...",
            "................",
            "................",
        },
        /* BOARD */ {
            "................",
            "..############..",
            "..#++++++++++#..",
            "..#+########+#..",
            "..#+#......#+#..",
            "..#+#.+..#.#+#..",
            "..#+#....#.#+#..",
            "..#+#..#...#+#..",
            "..#+#.#....#+#..",
            "..#+########+#..",
            "..#++++++++++#..",
            "..#+@+@+@+@++#..",
            "..#++++++++++#..",
            "..############..",
            "................",
            "................",
        },
        /* CALENDAR */ {
            "................",
            "...##....##.....",
            "..############..",
            "..#++++++++++#..",
            "..############..",
            "..#+@@@+@@@@+#..",
            "..#++++++++++#..",
            "..#+.+.+.+..+#..",
            "..#++++++++++#..",
            "..#+.+.+.+..+#..",
            "..#++++++++++#..",
            "..#+.+.+.+..+#..",
            "..#++++++++++#..",
            "..############..",
            "................",
            "................",
        },
        /* INFO */ {
            "................",
            ".....######.....",
            "...##++++++##...",
            "..#++++++++++#..",
            "..#+++####+++#..",
            ".#++++####++++#.",
            ".#++++++##++++#.",
            ".#++++++##++++#.",
            ".#++++++##++++#.",
            ".#+++++####+++#.",
            "..#+++####+++#..",
            "..#++++++++++#..",
            "...##++++++##...",
            ".....######.....",
            "................",
            "................",
        },
        /* WRENCH */ {
            "................",
            "..........##....",
            ".........#++#...",
            "........#+##+#..",
            "........#+#.#+..",
            ".......#+#..#+..",
            "......#+#...#...",
            ".....#+#....#...",
            "....#+#.........",
            "...#+#..........",
            "..#+#...........",
            ".#+#............",
            ".#+#............",
            "..##............",
            "................",
            "................",
        },
        /* CAT */ {
            "................",
            "................",
            "................",
            "..##........##..",
            ".#++#......#++#.",
            ".#+o#......#o+#.",
            ".#+oo######oo+#.",
            ".#+oooooooooo+#.",
            "..#oo@oo@oooo#..",
            "..#oooooooooo#..",
            "...##########...",
            "...##......##...",
            "..##........##..",
            "................",
            "................",
            "................",
        },
        /* RADIO */ {
            "................",
            "...........#....",
            "..........#.....",
            "..############..",
            "..#++++++++++#..",
            "..#+oooooo+++#..",
            "..#+o@@@@o+++#..",
            "..#+oooooo+++#..",
            "..#++++++++++#..",
            "..#++@++++@++#..",
            "..#++++++++++#..",
            "..#++##++##++#..",
            "..############..",
            "................",
            "................",
            "................",
        },
        /* LAMP */ {
            "................",
            ".....######.....",
            "....#++++++#....",
            "...#++++++++#...",
            "...#++++++++#...",
            "...##########...",
            "......####......",
            ".......##.......",
            ".......##.......",
            ".......##.......",
            ".......##.......",
            "......####......",
            "....########....",
            "...##########...",
            "................",
            "................",
        },
        /* CART */ {
            "................",
            "....##..........",
            "...#++#.........",
            "..############..",
            "..#++++++++++#..",
            "..#+######+++#..",
            "..#+#oooo#+++#..",
            "..#+######+++#..",
            "..#++++++++++#..",
            "..#+######+++#..",
            "..#++++++++++#..",
            "..############..",
            "....##....##....",
            "....##....##....",
            "................",
            "................",
        },
        /* CHIP */ {
            "................",
            "....#..#..#.....",
            "..#..##..##..#..",
            "..############..",
            "...++++++++++...",
            "..#+########+#..",
            "..#+#......#+#..",
            "..#+#.oooo.#+#..",
            "..#+#.oooo.#+#..",
            "..#+#......#+#..",
            "..#+########+#..",
            "...++++++++++...",
            "..############..",
            "..#..##..##..#..",
            "....#..#..#.....",
            "................",
        },
    };

    private Icons() {
    }

    /**
     * Draws an icon.
     *
     * @param x      left edge
     * @param y      top edge
     * @param px     size of one icon pixel (1..3). The icon is 16*px square.
     * @param body   main colour
     * @param accent highlight colour
     * @param dim    outline colour
     */
    public static void draw(Gfx g, int id, int x, int y, int px,
                            int body, int accent, int dim) {
        if (id < 0 || id >= ART.length || px < 1) {
            return;
        }
        String[] art = ART[id];
        for (int row = 0; row < GRID && row < art.length; row++) {
            String line = art[row];
            int len = line.length();
            int col = 0;
            while (col < GRID) {
                char c = col < len ? line.charAt(col) : ' ';
                if (c == ' ' || c == '.') {
                    col++;
                    continue;
                }
                // merge horizontal runs of the same colour
                int run = 1;
                while (col + run < GRID && col + run < len
                        && line.charAt(col + run) == c) {
                    run++;
                }
                int rgb;
                if (c == '#') {
                    rgb = dim;
                } else if (c == '+') {
                    rgb = body;
                } else if (c == 'o') {
                    rgb = accent;
                } else {
                    rgb = 0xFFFFFF;
                }
                g.setColor(rgb);
                g.fillRect(x + col * px, y + row * px, run * px, px);
                col += run;
            }
        }
    }

    public static int size(int px) {
        return GRID * px;
    }

    /**
     * Draws an icon into a square of {@code size} pixels (any size, not only
     * multiples of 16).  Cells are mapped with thousandths so runs stay
     * contiguous and no gaps appear.
     */
    public static void drawSized(Gfx g, int id, int x, int y, int size,
                                 int body, int accent, int dim) {
        if (id < 0 || id >= ART.length || size < 4) {
            return;
        }
        int cell = size * 1000 / GRID;
        String[] art = ART[id];
        for (int row = 0; row < GRID && row < art.length; row++) {
            int y0 = y + row * cell / 1000;
            int y1 = y + (row + 1) * cell / 1000;
            if (y1 <= y0) {
                y1 = y0 + 1;
            }
            String line = art[row];
            int len = line.length();
            int col = 0;
            while (col < GRID) {
                char c = col < len ? line.charAt(col) : ' ';
                if (c == ' ' || c == '.') {
                    col++;
                    continue;
                }
                int run = 1;
                while (col + run < GRID && col + run < len
                        && line.charAt(col + run) == c) {
                    run++;
                }
                int rgb = c == '#' ? dim : (c == '+' ? body
                        : (c == 'o' ? accent : 0xFFFFFF));
                g.setColor(rgb);
                int x0 = x + col * cell / 1000;
                int x1 = x + (col + run) * cell / 1000;
                g.fillRect(x0, y0, Math.max(1, x1 - x0), y1 - y0);
                col += run;
            }
        }
    }

    /** Icon used by each main menu entry. */
    public static int forMenu(int entry) {
        switch (entry) {
            case 0: return MONITOR;
            case 1: return BOARD;
            case 2: return CITY;
            case 3: return WRENCH;
            case 4: return CART;
            case 5: return FLASK;
            case 6: return CALENDAR;
            case 7: return INFO;
            default: return CHIP;
        }
    }
}
