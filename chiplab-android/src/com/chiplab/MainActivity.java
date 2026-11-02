package com.chiplab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * ChipLab - chip creation simulator.
 * One Activity, one custom Canvas view, no XML layouts: the whole game
 * (menu, catalog, microscope, live simulation, constructor, quests) is
 * drawn by hand so it runs on any Android 5.0+ device.
 */
public class MainActivity extends Activity {
    GameView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new GameView(this);
        setContentView(view);
    }

    @Override
    public void onBackPressed() {
        if (view != null && view.handleBack()) return;
        super.onBackPressed();
    }

    // ================================================================ VIEW
    public class GameView extends View {
        // screens
        static final int SCR_MENU = 0, SCR_CATALOG = 1, SCR_CHIP = 2, SCR_BUILD = 3, SCR_QUESTS = 4;
        // chip tabs
        static final int TAB_OVERVIEW = 0, TAB_MICRO = 1, TAB_RUN = 2, TAB_BIN = 3;
        // button ids
        static final int B_BACK = 1, B_CATALOG = 2, B_BUILDER = 3, B_QUESTS = 4, B_PROMPT = 5,
                B_IMPORT = 6, B_HELP = 7, B_TAB0 = 10, B_TAB1 = 11, B_TAB2 = 12, B_TAB3 = 13,
                B_STEP = 20, B_RUN = 21, B_RESET = 22, B_SPEED = 23, B_EXPORT = 24, B_DELETE = 25,
                B_ZOOM0 = 26, B_ZOOM1 = 27, B_ZOOM2 = 28, B_BINMODE = 29,
                B_RAM_PLUS = 30, B_RAM_MINUS = 31, B_RAM_TEST = 32, B_RAM_CLEAR = 33,
                B_CT_TYPE = 40, B_CT_BITS = 41, B_CT_REGS_M = 42, B_CT_REGS_P = 43,
                B_CT_MEM = 44, B_CT_SCREEN = 45, B_CT_NAME = 46, B_CT_MAKE = 47,
                B_FILTER_BASE = 70,
                B_PAD_LEFT = 90, B_PAD_UP = 91, B_PAD_DOWN = 92, B_PAD_RIGHT = 93,
                B_PAD_A = 94, B_PAD_B = 95,
                B_CHIP_BASE = 1000, B_BLOCK_BASE = 2000, B_CELL_BASE = 3000, B_BIT_BASE = 5000;
        // catalog categories
        static final int CAT_ALL = 0, CAT_CPU = 1, CAT_MEMORY = 2, CAT_VIDEO = 3,
                CAT_CIRCUIT = 4, CAT_DEVICE = 5, CAT_PC = 6, CAT_MINE = 7;

        // colors
        static final int C_BG = 0xFF0B1020, C_PANEL = 0xFF151C30, C_PANEL2 = 0xFF1D2742,
                C_TEXT = 0xFFE8ECF5, C_DIM = 0xFF8A94AD, C_AMBER = 0xFFFFC107, C_CYAN = 0xFF38E1C6,
                C_RED = 0xFFEF5350, C_GREEN = 0xFF66BB6A, C_LINE = 0xFF2A3554;
        final int[] KIND_COLORS = {0xFFAB47BC, 0xFFFFC107, 0xFF26A69A, 0xFFFF7043, 0xFF90A4AE, 0xFF42A5F5, 0xFF7C5CFF};
        final int[] PALETTE = {
                0xFF101216, 0xFFFFC107, 0xFFFF7043, 0xFFEF5350, 0xFFAB47BC, 0xFF5C6BC0, 0xFF42A5F5, 0xFF26C6DA,
                0xFF26A69A, 0xFF66BB6A, 0xFF9CCC65, 0xFFD4E157, 0xFFFFEE58, 0xFFFFCA28, 0xFFFFA726, 0xFFFFFFFF};

        final Activity act;
        final Paint p = new Paint();
        final Paint pMono = new Paint();
        final Paint pBold = new Paint();
        float density = 1f;
        int W, H;

        int screen = SCR_MENU;
        int chipTab = TAB_OVERVIEW;
        int catalogFilter = CAT_ALL;
        final String[] CAT_NAMES = {"ВСЕ", "ПРОЦЕССОРЫ", "ПАМЯТЬ", "ВИДЕО",
                "СХЕМЫ", "УСТРОЙСТВА", "ПК", "МОИ"};
        float scrollY = 0;
        float maxScroll = 0;

        // data
        final ArrayList<ChipBin> chips = new ArrayList<ChipBin>();
        final ArrayList<Boolean> chipCustom = new ArrayList<Boolean>();
        final ArrayList<Integer> chipCats = new ArrayList<Integer>(); // cached catalog categories
        String promptText = "";
        int curChip = -1;
        Cpu cpu;
        boolean running = false;
        int speed = 0; // 0 slow 1 fast 2 turbo
        long lastStepMs = 0;
        int microZoom = 1; // 0 die 1 block 2 transistors
        int selBlock = 0;
        boolean binHex = false;
        String cachedBinText;
        int cachedBinFor = -1;
        boolean cachedBinHex;
        ArrayList<String> cachedBinLines;
        int cachedBinWidth = -1;
        int selCell = 0;
        int ramTestIdx = -1;
        long ramTestMs = 0;

        // progress
        SharedPreferences prefs;
        int credits = 1000;
        int questMask = 0;

        // constructor state
        int ctType = ChipBin.TYPE_CPU, ctBits = 8, ctRegs = 4, ctMemIdx = 2, ctScreenIdx = 0;
        String ctName = "МОЙ-ЧИП-8";
        final int[] MEM_OPTIONS = {4, 16, 64, 256, 512, 1024};
        final int[][] SCREEN_OPTIONS = {{0, 0}, {16, 8}, {16, 16}, {32, 16},
                {32, 32}, {64, 32}, {64, 64}};
        final int[] TYPE_CYCLE = {ChipBin.TYPE_CPU, ChipBin.TYPE_RAM, ChipBin.TYPE_GPU, ChipBin.TYPE_DISPLAY};

        // touch / buttons
        class Btn { int id; RectF r; }
        final ArrayList<Btn> btns = new ArrayList<Btn>();
        int priorityBtnCount = 0; // hits [0..priority) are fixed UI, searched first
        float touchDownX, touchDownY, lastTouchY;
        boolean dragging = false;

        // microscope pulses
        class Pulse { float sx, sy, ex, ey; long t0; int color; }
        final ArrayList<Pulse> pulses = new ArrayList<Pulse>();
        int lastActiveKind = -1;
        RectF dieRect; // inner floorplan of last drawn die (pulses/block hits)

        // realistic silicon layout (DieLayout) caches: geometry per chip,
        // a baked static bitmap per layout+size and a dynamic overlay bitmap
        // that is re-baked only when the simulated state actually changes.
        final HashMap<String, DieLayout> dieLayouts = new HashMap<String, DieLayout>();
        final HashMap<String, Bitmap> dieStaticBmps = new HashMap<String, Bitmap>();
        final HashMap<String, Bitmap> dieDynBmps = new HashMap<String, Bitmap>();
        final HashMap<String, Long> dieDynStamps = new HashMap<String, Long>();
        final HashMap<String, Cpu> dieThumbCpus = new HashMap<String, Cpu>();
        final Paint diePaint = new Paint();
        long dieUiTick = 0;      // bumps on manual memory edits
        long lastDieBakeMs = 0;  // dynamic re-bake throttle

        final String[] QUEST_NAMES = {
                "Первый бит", "Счёт до пятнадцати", "Фибоначчи в кремнии", "Сумма Гаусса",
                "Свой завод", "Кремниевый художник", "Чип от искусственного интеллекта"};
        final String[] QUEST_DESC = {
                "Открой 1-битный процессор, вкладка РАБОТА: нажми ПУСК и сделай 30 тактов.",
                "4-битный процессор должен вывести (OUT) число 15.",
                "8-битный процессор: в ОЗУ должны появиться числа 13 и 21 (программа Фибоначчи).",
                "16-битный процессор должен вывести 5050 - сумму чисел от 1 до 100.",
                "Собери любой свой чип в КОНСТРУКТОРЕ.",
                "На видеокарте или экране должны загореться 100 пикселей.",
                "Скопируй ПРОМТ ДЛЯ ИИ из меню, попроси любую нейросеть сделать чип и импортируй её Base64."};
        final int[] QUEST_REWARD = {50, 80, 120, 200, 100, 150, 250};

        public GameView(Activity a) {
            super(a);
            act = a;
            density = a.getResources().getDisplayMetrics().density;
            p.setAntiAlias(true);
            pMono.setAntiAlias(true);
            pMono.setTypeface(Typeface.MONOSPACE);
            pBold.setAntiAlias(true);
            pBold.setTypeface(Typeface.DEFAULT_BOLD);
            diePaint.setFilterBitmap(true);
            prefs = a.getSharedPreferences("chiplab", Context.MODE_PRIVATE);
            credits = prefs.getInt("credits", 1000);
            questMask = prefs.getInt("quests", 0);
            loadAssets();
        }

        float dp(float v) { return v * density; }

        // ---------------------------------------------------- data loading
        byte[] readAsset(String name) {
            try {
                InputStream in = act.getAssets().open(name);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                in.close();
                return bos.toByteArray();
            } catch (Exception e) {
                return null;
            }
        }

        void loadAssets() {
            // The whole bundled library is discovered dynamically: dropping a
            // new numbered .bin into assets is enough for it to appear here.
            try {
                String[] all = act.getAssets().list("");
                if (all != null) {
                    Arrays.sort(all);
                    for (int i = 0; i < all.length; i++) {
                        if (all[i].endsWith(".bin")) {
                            byte[] data = readAsset(all[i]);
                            if (data == null) continue;
                            try {
                                chips.add(ChipBin.parse(data));
                                chipCustom.add(Boolean.FALSE);
                            } catch (Exception ignored) { }
                        } else if ("prompt.txt".equals(all[i])) {
                            byte[] pb = readAsset("prompt.txt");
                            if (pb != null) promptText = new String(pb, "UTF-8");
                        }
                    }
                }
            } catch (Exception ignored) { }
            int customCount = prefs.getInt("custom_count", 0);
            for (int i = 0; i < customCount; i++) {
                String b64 = prefs.getString("custom_" + i, null);
                if (b64 == null) continue;
                try {
                    chips.add(ChipBin.parse(Base64.decode(b64, Base64.DEFAULT)));
                    chipCustom.add(Boolean.TRUE);
                } catch (Exception ignored) { }
            }
        }

        boolean nameHas(String n, String[] keys) {
            for (int i = 0; i < keys.length; i++) {
                if (n.contains(keys[i])) return true;
            }
            return false;
        }

        int chipCategory(int idx) {
            if (idx < 0 || idx >= chips.size()) return CAT_ALL;
            if (chipCats.size() != chips.size()) {
                chipCats.clear();
                for (int i = 0; i < chips.size(); i++) {
                    chipCats.add(Integer.valueOf(computeChipCategory(i)));
                }
            }
            return chipCats.get(idx).intValue();
        }

        int computeChipCategory(int idx) {
            if (chipCustom.get(idx)) return CAT_MINE;
            ChipBin ch = chips.get(idx);
            String n = ch.name.toUpperCase();
            if (n.startsWith("СХЕМА") || n.contains("СХЕМА:")) return CAT_CIRCUIT;
            if (nameHas(n, new String[]{"ПК", "КОМПЬЮТЕР", "НОУТБУК", "СЕРВЕР",
                    "РАБОЧАЯ СТАНЦИЯ", "СУПЕРКОМПЬЮТЕР", "ТЕРМИНАЛ"})) return CAT_PC;
            if (nameHas(n, new String[]{"ТЕТРИС", "ТАМАГОЧИ", "ЗМЕЙКА", "ПОНГ",
                    "ФЛАППИ", "АРКАНОИД", "ГОНКИ", "ЗАЩИТНИК", "ПРИСТАВКА",
                    "КАЛЬКУЛЯТОР", "СИНТЕЗАТОР", "ЧАСЫ", "РОБОТ", "ДРОН",
                    "ПЛАНШЕТ", "ИГРА «", "ИГРОВАЯ ПРИСТАВКА"})) return CAT_DEVICE;
            if (ch.type == ChipBin.TYPE_RAM || ch.type == ChipBin.TYPE_ROM) return CAT_MEMORY;
            if (ch.type == ChipBin.TYPE_GPU || ch.type == ChipBin.TYPE_DISPLAY) return CAT_VIDEO;
            return CAT_CPU;
        }

        int catalogCount(int cat) {
            if (cat == CAT_ALL) return chips.size();
            int count = 0;
            for (int i = 0; i < chips.size(); i++) {
                if (chipCategory(i) == cat) count++;
            }
            return count;
        }

        boolean usesGamepad() {
            if (curChip < 0 || curChip >= chips.size() || cpu == null) return false;
            ChipBin ch = chips.get(curChip);
            if (!ch.hasScreen() || ch.memSize <= 250) return false;
            int cat = chipCategory(curChip);
            if (cat == CAT_DEVICE || cat == CAT_PC) return true;
            String n = ch.name.toUpperCase();
            return nameHas(n, new String[]{"ТЕТРИС", "ТАМАГОЧИ", "ЗМЕЙКА", "ПОНГ",
                    "ФЛАППИ", "АРКАНОИД", "ГОНКИ", "ПРИСТАВКА", "КАЛЬКУЛЯТОР",
                    "СИНТЕЗАТОР", "РОБОТ", "ДРОН", "ПК", "КОМПЬЮТЕР"});
        }

        void pressPad(int buttonId) {
            if (cpu == null || cpu.mem.length <= 250) return;
            int[] masks = {1, 4, 8, 2, 16, 32}; // left, up, down, right, A, B
            int idx = buttonId - B_PAD_LEFT;
            if (idx < 0 || idx >= masks.length) return;
            cpu.mem[250] = masks[idx] & cpu.chip.mask();
            cpu.lastMemAddr = 250;
            cpu.lastMemWrite = true;
            cpu.lastMemCycle = cpu.cycles;
            dieUiTick++;
            if (!cpu.halted) running = true;
        }

        float drawGamepad(Canvas c, float x, float y, float w) {
            String[] labels = {"<", "^", "v", ">", "A", "B"};
            int[] ids = {B_PAD_LEFT, B_PAD_UP, B_PAD_DOWN, B_PAD_RIGHT, B_PAD_A, B_PAD_B};
            float bw = (w - dp(25)) / 6f;
            for (int i = 0; i < labels.length; i++) {
                drawButton(c, ids[i], labels[i], x + i * (bw + dp(5)), y, bw, dp(40),
                        i >= 4, false);
            }
            textC(c, "геймпад устройства: код кнопки пишется в ячейку ввода 250",
                    x + w / 2, y + dp(54), dp(11), C_DIM, p);
            return dp(60);
        }

        void saveProgress() {
            SharedPreferences.Editor e = prefs.edit();
            e.putInt("credits", credits);
            e.putInt("quests", questMask);
            e.apply();
        }

        void saveCustomChips() {
            SharedPreferences.Editor e = prefs.edit();
            int idx = 0;
            for (int i = 0; i < chips.size(); i++) {
                if (chipCustom.get(i)) {
                    e.putString("custom_" + idx, Base64.encodeToString(chips.get(i).toBytes(), Base64.DEFAULT));
                    idx++;
                }
            }
            e.putInt("custom_count", idx);
            e.apply();
        }

        void toast(String s) {
            Toast.makeText(act, s, Toast.LENGTH_SHORT).show();
        }

        String rankName() {
            int done = Integer.bitCount(questMask);
            if (done >= 7) return "Легенда кремния";
            if (done >= 5) return "Архитектор чипов";
            if (done >= 3) return "Инженер";
            if (done >= 1) return "Техник";
            return "Стажёр";
        }

        void completeQuest(int id) {
            if (id < 0 || id >= QUEST_NAMES.length) return;
            if ((questMask & (1 << id)) != 0) return;
            questMask |= (1 << id);
            credits += QUEST_REWARD[id];
            saveProgress();
            toast("Заказ выполнен: «" + QUEST_NAMES[id] + "» +" + QUEST_REWARD[id] + " кредитов!");
        }

        boolean outContains(int v) {
            if (cpu == null) return false;
            for (int i = 0; i < cpu.outLogSize; i++) if (cpu.outLog[i] == v) return true;
            return cpu.lastOut == v;
        }

        boolean memContains(int v) {
            if (cpu == null) return false;
            for (int i = 0; i < cpu.mem.length; i++) if (cpu.mem[i] == v) return true;
            return false;
        }

        void checkQuests() {
            if (cpu == null || curChip < 0) return;
            ChipBin c = chips.get(curChip);
            if (c.bits == 1 && c.isCpuLike() && cpu.cycles >= 30) completeQuest(0);
            if (c.bits == 4 && c.isCpuLike() && outContains(15)) completeQuest(1);
            if (c.bits == 8 && c.isCpuLike() && cpu.cycles > 20 && memContains(13) && memContains(21)) completeQuest(2);
            if (c.isCpuLike() && outContains(5050)) completeQuest(3);
            if (cpu.framebuffer != null && cpu.litPixels() >= 100) completeQuest(5);
        }

        // ---------------------------------------------------- navigation
        void go(int scr) {
            screen = scr;
            scrollY = 0;
            running = false;
        }

        void openChip(int idx) {
            curChip = idx;
            cpu = new Cpu(chips.get(idx));
            chipTab = TAB_OVERVIEW;
            selBlock = 0;
            selCell = 0;
            ramTestIdx = -1;
            running = false;
            cachedBinFor = -1;
            pulses.clear();
            go(SCR_CHIP);
        }

        boolean handleBack() {
            if (screen == SCR_CHIP) { go(SCR_CATALOG); return true; }
            if (screen != SCR_MENU) { go(SCR_MENU); return true; }
            return false;
        }

        // ---------------------------------------------------- touch
        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int a = e.getAction();
            if (a == MotionEvent.ACTION_DOWN) {
                touchDownX = e.getX();
                touchDownY = e.getY();
                lastTouchY = e.getY();
                dragging = false;
                return true;
            }
            if (a == MotionEvent.ACTION_MOVE) {
                float dy = e.getY() - lastTouchY;
                lastTouchY = e.getY();
                if (Math.abs(e.getY() - touchDownY) > dp(10)) dragging = true;
                if (dragging && maxScroll > 0) {
                    scrollY -= dy;
                    if (scrollY < 0) scrollY = 0;
                    if (scrollY > maxScroll) scrollY = maxScroll;
                    invalidate();
                }
                return true;
            }
            if (a == MotionEvent.ACTION_UP) {
                if (!dragging) {
                    float x = e.getX(), y = e.getY();
                    int hitId = -1;
                    for (int i = priorityBtnCount - 1; i >= 0 && hitId < 0; i--) {
                        if (btns.get(i).r.contains(x, y)) hitId = btns.get(i).id;
                    }
                    for (int i = btns.size() - 1; i >= priorityBtnCount && hitId < 0; i--) {
                        if (btns.get(i).r.contains(x, y)) hitId = btns.get(i).id;
                    }
                    if (hitId >= 0) onButton(hitId);
                }
                dragging = false;
                invalidate();
                return true;
            }
            return true;
        }

        void addHit(int id, float x, float y, float w, float h) {
            Btn b = new Btn();
            b.id = id;
            b.r = new RectF(x, y, x + w, y + h);
            btns.add(b);
        }

        void onButton(int id) {
            if (id >= B_CHIP_BASE && id < B_BLOCK_BASE) { openChip(id - B_CHIP_BASE); return; }
            if (id >= B_BLOCK_BASE && id < B_CELL_BASE) {
                selBlock = id - B_BLOCK_BASE;
                if (chipTab == TAB_OVERVIEW) chipTab = TAB_MICRO;
                if (microZoom == 0) microZoom = 1;
                return;
            }
            if (id >= B_CELL_BASE && id < B_BIT_BASE) { selCell = id - B_CELL_BASE; return; }
            if (id >= B_BIT_BASE) {
                int bit = id - B_BIT_BASE;
                if (cpu != null && selCell >= 0 && selCell < cpu.mem.length) {
                    cpu.mem[selCell] ^= (1 << bit);
                    cpu.mem[selCell] &= cpu.chip.mask();
                    cpu.lastMemAddr = selCell;
                    cpu.lastMemWrite = true;
                    cpu.lastMemCycle = cpu.cycles;
                    dieUiTick++;
                }
                return;
            }
            if (id >= B_FILTER_BASE && id < B_FILTER_BASE + CAT_NAMES.length) {
                catalogFilter = id - B_FILTER_BASE;
                scrollY = 0;
                return;
            }
            if (id >= B_PAD_LEFT && id <= B_PAD_B) {
                pressPad(id);
                return;
            }
            switch (id) {
                case B_BACK: handleBack(); break;
                case B_CATALOG: go(SCR_CATALOG); break;
                case B_BUILDER: go(SCR_BUILD); break;
                case B_QUESTS: go(SCR_QUESTS); break;
                case B_PROMPT: showPromptDialog(); break;
                case B_IMPORT: showImportDialog(); break;
                case B_HELP: showTextDialog("Как играть", HELP_TEXT, false); break;
                case B_TAB0: chipTab = TAB_OVERVIEW; scrollY = 0; running = false; break;
                case B_TAB1: chipTab = TAB_MICRO; scrollY = 0; break;
                case B_TAB2: chipTab = TAB_RUN; scrollY = 0; break;
                case B_TAB3: chipTab = TAB_BIN; scrollY = 0; running = false; break;
                case B_STEP: if (cpu != null) { cpu.step(); checkQuests(); } break;
                case B_RUN: running = !running; lastStepMs = 0; break;
                case B_RESET: if (cpu != null) { cpu.reset(); pulses.clear(); } running = false; break;
                case B_SPEED: speed = (speed + 1) % 3; break;
                case B_EXPORT: if (curChip >= 0) showExportDialog(chips.get(curChip)); break;
                case B_DELETE: deleteCurrent(); break;
                case B_ZOOM0: microZoom = 0; break;
                case B_ZOOM1: microZoom = 1; break;
                case B_ZOOM2: microZoom = 2; break;
                case B_BINMODE: binHex = !binHex; scrollY = 0; break;
                case B_RAM_PLUS: if (cpu != null) { cpu.mem[selCell] = (cpu.mem[selCell] + 1) & cpu.chip.mask(); markMem(); } break;
                case B_RAM_MINUS: if (cpu != null) { cpu.mem[selCell] = (cpu.mem[selCell] - 1) & cpu.chip.mask(); markMem(); } break;
                case B_RAM_TEST: ramTestIdx = 0; ramTestMs = 0; break;
                case B_RAM_CLEAR:
                    if (cpu != null) { for (int i = 0; i < cpu.mem.length; i++) cpu.mem[i] = 0; dieUiTick++; }
                    break;
                case B_CT_TYPE: cycleType(); break;
                case B_CT_BITS: ctBits = ctBits % 16 + 1; fixCtName(); break;
                case B_CT_REGS_M: ctRegs = Math.max(1, ctRegs - 1); break;
                case B_CT_REGS_P: ctRegs = Math.min(16, ctRegs + 1); break;
                case B_CT_MEM: ctMemIdx = (ctMemIdx + 1) % MEM_OPTIONS.length; fixConstructor(); break;
                case B_CT_SCREEN: ctScreenIdx = (ctScreenIdx + 1) % SCREEN_OPTIONS.length; fixConstructor(); break;
                case B_CT_NAME: showNameDialog(); break;
                case B_CT_MAKE: makeCustomChip(); break;
                default: break;
            }
        }

        void markMem() {
            cpu.lastMemAddr = selCell;
            cpu.lastMemWrite = true;
            cpu.lastMemCycle = cpu.cycles;
            dieUiTick++;
        }

        void fixCtName() {
            if (ctName.startsWith("МОЙ-ЧИП") || ctName.startsWith("МОЁ-ОЗУ") || ctName.startsWith("МОЙ-ЭКРАН")) {
                ctName = defaultCtName();
            }
        }

        String defaultCtName() {
            if (ctType == ChipBin.TYPE_RAM) return "МОЁ-ОЗУ-" + MEM_OPTIONS[ctMemIdx];
            if (ctType == ChipBin.TYPE_GPU || ctType == ChipBin.TYPE_DISPLAY) return "МОЙ-ЭКРАН-" + ctBits;
            return "МОЙ-ЧИП-" + ctBits;
        }

        void cycleType() {
            int cur = 0;
            for (int i = 0; i < TYPE_CYCLE.length; i++) if (TYPE_CYCLE[i] == ctType) cur = i;
            ctType = TYPE_CYCLE[(cur + 1) % TYPE_CYCLE.length];
            if (ctType == ChipBin.TYPE_RAM) ctScreenIdx = 0;
            if ((ctType == ChipBin.TYPE_GPU || ctType == ChipBin.TYPE_DISPLAY) && ctScreenIdx == 0) ctScreenIdx = 2;
            fixConstructor();
            fixCtName();
        }

        void fixConstructor() {
            if (ctScreenIdx > 0 && MEM_OPTIONS[ctMemIdx] < 64) ctMemIdx = 2; // screen demo needs mem[60..61]
            if (ctType == ChipBin.TYPE_RAM) ctScreenIdx = 0;
        }

        int constructorCost() {
            int tr = ChipBin.estimateTransistors(ctType, ctBits, ctType == ChipBin.TYPE_RAM ? 1 : ctRegs,
                    MEM_OPTIONS[ctMemIdx], ctScreenIdx > 0);
            return 10 + tr / 100;
        }

        void makeCustomChip() {
            fixConstructor();
            int cost = constructorCost();
            if (credits < cost) {
                toast("Не хватает кредитов (" + cost + "). Выполняй заказы завода!");
                return;
            }
            credits -= cost;
            int regs = ctType == ChipBin.TYPE_RAM ? 1 : ctRegs;
            if (ctScreenIdx > 0 && regs < 4) regs = 4;
            ChipBin c = ChipBin.buildCustom(ctType, ctBits, regs, MEM_OPTIONS[ctMemIdx],
                    SCREEN_OPTIONS[ctScreenIdx][0], SCREEN_OPTIONS[ctScreenIdx][1], ctName);
            chips.add(c);
            chipCustom.add(Boolean.TRUE);
            saveProgress();
            saveCustomChips();
            completeQuest(4);
            toast("Чип собран: " + c.name + " (" + c.transistors + " транзисторов)");
            openChip(chips.size() - 1);
        }

        void deleteCurrent() {
            if (curChip < 0 || !chipCustom.get(curChip)) return;
            chips.remove(curChip);
            chipCustom.remove(curChip);
            curChip = -1;
            cpu = null;
            saveCustomChips();
            toast("Чип удалён");
            go(SCR_CATALOG);
        }

        // ---------------------------------------------------- dialogs
        void copyToClipboard(String label, String text) {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(label, text));
        }

        String clipboardText() {
            try {
                ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip()) {
                    ClipData cd = cm.getPrimaryClip();
                    if (cd != null && cd.getItemCount() > 0) {
                        CharSequence t = cd.getItemAt(0).getText();
                        if (t != null) return t.toString();
                    }
                }
            } catch (Exception ignored) { }
            return "";
        }

        void showTextDialog(String title, String text, boolean withCopy) {
            final String finalText = text;
            TextView tv = new TextView(act);
            tv.setText(text);
            tv.setTextSize(13);
            tv.setPadding((int) dp(16), (int) dp(12), (int) dp(16), (int) dp(12));
            tv.setTextIsSelectable(true);
            ScrollView sv = new ScrollView(act);
            sv.addView(tv);
            AlertDialog.Builder b = new AlertDialog.Builder(act);
            b.setTitle(title);
            b.setView(sv);
            if (withCopy) {
                b.setPositiveButton("Копировать", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        copyToClipboard("chiplab", finalText);
                        toast("Скопировано в буфер обмена");
                    }
                });
            }
            b.setNegativeButton("Закрыть", null);
            b.show();
        }

        void showPromptDialog() {
            if (promptText == null || promptText.length() == 0) {
                toast("prompt.txt не найден в assets");
                return;
            }
            showTextDialog("Промт для ИИ (скопируй целиком)", promptText, true);
        }

        void showExportDialog(ChipBin c) {
            String b64 = Base64.encodeToString(c.toBytes(), Base64.DEFAULT);
            showTextDialog("Base64 чипа «" + c.name + "»", b64, true);
        }

        void showImportDialog() {
            final EditText et = new EditText(act);
            et.setHint("Вставь сюда Base64, который выдала нейросеть...");
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            et.setMinLines(5);
            String clip = clipboardText();
            if (clip.length() > 40) et.setText(clip);
            ScrollView sv = new ScrollView(act);
            sv.addView(et);
            AlertDialog.Builder b = new AlertDialog.Builder(act);
            b.setTitle("Импорт .bin от ИИ");
            b.setView(sv);
            b.setPositiveButton("Импортировать", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String s = et.getText().toString().trim();
                    importBase64(s);
                }
            });
            b.setNegativeButton("Отмена", null);
            b.show();
        }

        void importBase64(String s) {
            if (s.length() == 0) { toast("Пусто - вставь Base64"); return; }
            // tolerate surrounding text: keep only base64 characters
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
                        || ch == '+' || ch == '/' || ch == '=') sb.append(ch);
            }
            try {
                byte[] data = Base64.decode(sb.toString(), Base64.DEFAULT);
                ChipBin c = ChipBin.parse(data);
                chips.add(c);
                chipCustom.add(Boolean.TRUE);
                saveCustomChips();
                completeQuest(6);
                toast("Импортировано: " + c.name);
                openChip(chips.size() - 1);
            } catch (Exception e) {
                toast("Не получилось: " + e.getMessage());
            }
        }

        void showNameDialog() {
            final EditText et = new EditText(act);
            et.setText(ctName);
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            AlertDialog.Builder b = new AlertDialog.Builder(act);
            b.setTitle("Имя чипа");
            b.setView(et);
            b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String s = et.getText().toString().trim();
                    if (s.length() > 0) ctName = s.toUpperCase();
                    invalidate();
                }
            });
            b.setNegativeButton("Отмена", null);
            b.show();
        }

        static final String HELP_TEXT =
                "ChipLab - симулятор создания микросхем.\n\n"
                + "КАЖДЫЙ ЧИП - ЭТО .BIN ФАЙЛ\n"
                + "Процессоры, память, видеокарты, схемы, игровые устройства и целые ПК описаны одним форматом CHIPBIN1: "
                + "разрядность, регистры, память, блоки кристалла с транзисторами и программа. В каталоге есть фильтры: процессоры, память, видео, схемы, устройства, ПК и мои чипы. "
                + "Вкладка «.BIN» покажет файл как есть: структуру с цифрами и шестнадцатеричный дамп.\n\n"
                + "ПРОСМОТР ПРОЦЕССОРА\n"
                + "В любом устройстве - Тетрисе, Тамагочи, приставке, роботе, дроне или ПК - нажми большую кнопку «ПРОСМОТР ПРОЦЕССОРА: КАК ОН РАБОТАЕТ» на обзоре или вкладке РАБОТА. Откроется микроскоп: кристалл, блоки, регистры-триггеры, ячейки памяти и цепочка сумматоров АЛУ с настоящими сигналами 0 и 1. "
                + "Нажимай ШАГ/ПУСК и кнопки геймпада прямо под микроскопом.\n\n"
                + "ИГРЫ И УСТРОЙСТВА\n"
                + "У Тетриса, Тамагочи, Змейки, Понга, калькулятора, ПК и других устройств на вкладке РАБОТА есть виртуальный геймпад. Он записывает код кнопки в ячейку памяти 250, а программа устройства сама читает и очищает её процессором. Для плавной игры включи скорость x100.\n\n"
                + "РАБОТА\n"
                + "Честная симуляция по тактам: регистры, флаги Z и C, память, вывод OUT и экран. "
                + "У чипов памяти можно трогать ячейки и переключать биты пальцем.\n\n"
                + "КОНСТРУКТОР\n"
                + "Собери свой процессор любой разрядности 1..16 бит, до 16 регистров, до 1024 ячеек ОЗУ или экран до 64x64: игра посчитает транзисторы и "
                + "сгенерирует настоящий .bin файл, который тут же заработает.\n\n"
                + "ЧИПЫ ОТ НЕЙРОСЕТЕЙ\n"
                + "В меню есть готовый ПРОМТ ДЛЯ ИИ: скопируй его в любую нейросеть, попроси чип "
                + "(например «8-битный процессор, который умножает 6 на 7»), а её ответ в Base64 "
                + "вставь в «Импорт .bin от ИИ» - чип появится в каталоге и оживёт.\n\n"
                + "ЗАКАЗЫ ЗАВОДА\n"
                + "Задания дают кредиты на новые чипы и звания - от Стажёра до Легенды кремния.";

        // ---------------------------------------------------- sim update
        void updateSim(long now) {
            if (cpu != null && running && !cpu.halted && cpu.chip.isCpuLike()) {
                if (speed == 2) {
                    // Enough cycles for interactive devices to consume the
                    // gamepad cell and redraw several game frames per second.
                    cpu.run(cpu.chip.hasScreen() ? 2400 : 1000);
                    checkQuests();
                } else {
                    long interval = speed == 0 ? 220 : 30;
                    int perTick = speed == 0 ? 1 : 6;
                    if (lastStepMs == 0 || now - lastStepMs >= interval) {
                        cpu.run(perTick);
                        lastStepMs = now;
                        checkQuests();
                    }
                }
                if (cpu.halted) running = false;
                if (cpu.activeKind != lastActiveKind) {
                    lastActiveKind = cpu.activeKind;
                    spawnPulse(cpu.activeKind, now);
                }
            }
            if (ramTestIdx >= 0 && cpu != null) {
                if (ramTestMs == 0 || now - ramTestMs >= 25) {
                    ramTestMs = now;
                    int m = cpu.chip.mask();
                    for (int k = 0; k < 2 && ramTestIdx < cpu.mem.length; k++, ramTestIdx++) {
                        cpu.mem[ramTestIdx] = (ramTestIdx * 7 + 3) & m;
                        cpu.lastMemAddr = ramTestIdx;
                        cpu.lastMemWrite = true;
                        cpu.lastMemCycle = cpu.cycles;
                        selCell = ramTestIdx;
                    }
                    if (ramTestIdx >= cpu.mem.length) {
                        ramTestIdx = -1;
                        toast("Тест памяти завершён");
                    }
                }
            }
        }

        void spawnPulse(int kind, long now) {
            if (dieRect == null || curChip < 0) return;
            ChipBin.Block[] bs = chips.get(curChip).blocksOrDefault();
            float[] from = blockCenter(bs, ChipBin.KIND_CONTROL);
            float[] to = blockCenter(bs, kind);
            if (from == null || to == null) return;
            Pulse pl = new Pulse();
            pl.sx = from[0]; pl.sy = from[1]; pl.ex = to[0]; pl.ey = to[1];
            pl.t0 = now;
            pl.color = KIND_COLORS[kind >= 0 && kind < KIND_COLORS.length ? kind : 0];
            pulses.add(pl);
            while (pulses.size() > 20) pulses.remove(0);
        }

        float[] blockCenter(ChipBin.Block[] bs, int kind) {
            float unit = dieRect.width() / 16f;
            float unitH = dieRect.height() / 16f;
            for (int i = 0; i < bs.length; i++) {
                if (bs[i].kind == kind) {
                    return new float[]{dieRect.left + (bs[i].x + bs[i].w / 2f) * unit,
                            dieRect.top + (bs[i].y + bs[i].h / 2f) * unitH};
                }
            }
            return null;
        }

        // ---------------------------------------------------- drawing utils
        void rect(Canvas c, float x, float y, float w, float h, int color) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            c.drawRect(x, y, x + w, y + h, p);
        }

        void roundRect(Canvas c, float x, float y, float w, float h, float r, int color) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            c.drawRoundRect(new RectF(x, y, x + w, y + h), r, r, p);
        }

        void strokeRect(Canvas c, float x, float y, float w, float h, int color, float sw) {
            p.setStyle(Paint.Style.STROKE);
            p.setColor(color);
            p.setStrokeWidth(sw);
            c.drawRect(x, y, x + w, y + h, p);
            p.setStyle(Paint.Style.FILL);
        }

        void text(Canvas c, String s, float x, float y, float size, int color, Paint paint) {
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTextAlign(Paint.Align.LEFT);
            c.drawText(s, x, y, paint);
        }

        void textC(Canvas c, String s, float cx, float y, float size, int color, Paint paint) {
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(s, cx, y, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        void textR(Canvas c, String s, float right, float y, float size, int color, Paint paint) {
            paint.setColor(color);
            paint.setTextSize(size);
            paint.setTextAlign(Paint.Align.RIGHT);
            c.drawText(s, right, y, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        float drawButton(Canvas c, int id, String label, float x, float y, float w, float h,
                         boolean primary, boolean selected) {
            int bg = selected ? C_AMBER : (primary ? 0xFF2E7D5B : C_PANEL2);
            int fg = selected ? 0xFF141414 : C_TEXT;
            roundRect(c, x, y, w, h, dp(8), bg);
            if (!primary && !selected) strokeRect(c, x, y, w, h, C_LINE, dp(1));
            pBold.setTextSize(dp(14));
            float tw = pBold.measureText(label);
            float size = dp(14);
            if (tw > w - dp(10)) {
                size = size * (w - dp(10)) / tw;
            }
            textC(c, label, x + w / 2, y + h / 2 + size * 0.36f, size, fg, pBold);
            addHit(id, x, y, w, h);
            return y + h;
        }

        /** Word-wrap measured text; returns list of lines. */
        ArrayList<String> wrap(String s, float maxW, Paint paint, float size) {
            paint.setTextSize(size);
            ArrayList<String> lines = new ArrayList<String>();
            String[] paras = s.split("\n", -1);
            for (int pi = 0; pi < paras.length; pi++) {
                String para = paras[pi];
                if (para.length() == 0) { lines.add(""); continue; }
                int start = 0;
                char[] chars = para.toCharArray();
                while (start < para.length()) {
                    int n = paint.breakText(chars, start, para.length() - start, maxW, null);
                    if (n <= 0) n = 1;
                    int end = start + n;
                    if (end < para.length()) {
                        int sp = para.lastIndexOf(' ', end - 1);
                        if (sp > start) end = sp;
                    }
                    lines.add(para.substring(start, end).trim());
                    start = end;
                    while (start < para.length() && para.charAt(start) == ' ') start++;
                }
            }
            return lines;
        }

        float drawLines(Canvas c, ArrayList<String> lines, float x, float y, float lineH,
                        float size, int color, Paint paint, float clipTop, float clipBottom) {
            paint.setTextSize(size);
            paint.setColor(color);
            for (int i = 0; i < lines.size(); i++) {
                float ly = y + (i + 1) * lineH;
                if (ly > clipTop && ly < clipBottom + lineH) {
                    c.drawText(lines.get(i), x, ly, paint);
                }
            }
            return lines.size() * lineH;
        }

        void drawHeader(Canvas c, String title, boolean back) {
            float hh = dp(52);
            rect(c, 0, 0, W, hh, C_PANEL);
            rect(c, 0, hh, W, dp(1), C_LINE);
            float x = dp(10);
            if (back) {
                drawButton(c, B_BACK, "< НАЗАД", x, dp(8), dp(84), dp(36), false, false);
                x += dp(94);
            }
            text(c, title, x, dp(34), dp(17), C_TEXT, pBold);
            textR(c, credits + " кр.", W - dp(10), dp(34), dp(14), C_AMBER, pBold);
        }

        // ------------------------------------------ realistic die rendering
        // Structural key: the builder re-creates its preview ChipBin every
        // frame, so identity keys would never hit the cache.
        String chipKey(ChipBin chip) {
            StringBuffer sb = new StringBuffer();
            sb.append(chip.name).append('|').append(chip.type).append('|').append(chip.bits)
                    .append('|').append(chip.regs).append('|').append(chip.memSize).append('|')
                    .append(chip.screenW).append('x').append(chip.screenH).append('|')
                    .append(chip.transistors);
            int hv = 0x1234567;
            for (int i = 0; i < chip.program.length; i++) hv = hv * 31 + chip.program[i];
            sb.append('|').append(chip.program.length).append('|').append(hv);
            ChipBin.Block[] bs = chip.blocksOrDefault();
            for (int i = 0; i < bs.length; i++) {
                ChipBin.Block b = bs[i];
                sb.append('|').append(b.name).append(',').append(b.x).append(',').append(b.y)
                        .append(',').append(b.w).append(',').append(b.h).append(',').append(b.kind)
                        .append(',').append(b.param).append(',').append(b.transistors);
            }
            return sb.toString();
        }

        DieLayout getDieLayout(String key, ChipBin chip, ChipBin.Block block, int mode) {
            DieLayout d = dieLayouts.get(key);
            if (d != null) return d;
            if (dieLayouts.size() >= 14) dieLayouts.clear();
            if (mode == 0) d = DieLayout.wholeDie(chip);
            else d = DieLayout.blockDetail(chip, block, mode == 2);
            dieLayouts.put(key, d);
            return d;
        }

        /** Pre-run simulation used so catalog/menu thumbnails show live data. */
        Cpu thumbCpu(String key, ChipBin chip) {
            Cpu t = dieThumbCpus.get(key);
            if (t != null) return t;
            t = new Cpu(chip);
            if (chip.isCpuLike()) {
                t.run(chip.hasScreen() ? 12000 : 240);
            } else if (t.mem.length > 0) {
                t.lastMemAddr = Math.min(0x5A, t.mem.length - 1);
                t.lastMemWrite = true;
                t.lastMemCycle = t.cycles;
            }
            dieThumbCpus.put(key, t);
            return t;
        }

        long dieStateStamp(Cpu sim) {
            if (sim == null) return dieUiTick;
            long s = sim.cycles * 1000003L ^ (sim.outCount * 9176L) ^ ((sim.lastMemAddr + 2) * 131L)
                    ^ (sim.fbCursor * 17L) ^ (sim.lastOut * 7L) ^ ((sim.lastRegWrite + 2) * 3L)
                    ^ (dieUiTick * 1009L) ^ sim.pc ^ (sim.halted ? 1L : 0L);
            for (int i = 0; i < sim.regs.length; i++) s ^= (sim.regs[i] + 1L) << (i % 24);
            return s;
        }

        void drawSegRect(Canvas bc, DieLayout.Seg s, float kx, float ky) {
            float x0 = s.x * kx, y0 = s.y * ky;
            float x1 = (s.x + s.w) * kx, y1 = (s.y + s.h) * ky;
            if (x1 < x0 + 1f) x1 = x0 + 1f;
            if (y1 < y0 + 1f) y1 = y0 + 1f;
            bc.drawRect(x0, y0, x1, y1, diePaint);
        }

        Bitmap bakeStaticDie(DieLayout layout, int pxW, int pxH) {
            Bitmap bmp = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888);
            Canvas bc = new Canvas(bmp);
            bc.drawColor(0xFF05030F);
            float kx = pxW / layout.widthU, ky = pxH / layout.heightU;
            diePaint.setStyle(Paint.Style.FILL);
            // All segments in true z-order at base colour; the dynamic overlay
            // then only repaints conductors whose live colour differs.
            for (int i = 0; i < layout.segs.size(); i++) {
                DieLayout.Seg s = layout.segs.get(i);
                // Dynamic channel markers are invisible until energized.
                if (s.layer == DieLayout.L_MARK && DieLayout.isDynamic(s)) continue;
                diePaint.setColor(DieLayout.baseColor(s));
                drawSegRect(bc, s, kx, ky);
            }
            for (int i = 0; i < layout.labels.size(); i++) {
                DieLayout.Label lb = layout.labels.get(i);
                float ts = lb.size * kx;
                if (ts < 7f) continue;
                pMono.setColor(lb.color);
                pMono.setTextSize(ts);
                pMono.setTextAlign(Paint.Align.LEFT);
                bc.drawText(lb.text, lb.x * kx, lb.y * ky + ts * 0.85f, pMono);
            }
            return bmp;
        }

        void bakeDynamicDie(Bitmap bmp, DieLayout layout, ChipBin chip, Cpu sim) {
            bmp.eraseColor(0x00000000);
            Canvas bc = new Canvas(bmp);
            float kx = bmp.getWidth() / layout.widthU, ky = bmp.getHeight() / layout.heightU;
            DieLayout.Ctx ctx = new DieLayout.Ctx(chip, sim);
            diePaint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < layout.dynamicSegs.size(); i++) {
                DieLayout.Seg s = layout.dynamicSegs.get(i);
                int col = DieLayout.colorFor(s, ctx);
                if (col == DieLayout.baseColor(s) || (col >>> 24) == 0) continue;
                diePaint.setColor(col);
                drawSegRect(bc, s, kx, ky);
            }
        }

        /** Draws one layout (whole die or block detail) at x,y,w; returns height. */
        float drawLayoutImage(Canvas c, DieLayout layout, String key, ChipBin chip, Cpu sim,
                              float x, float y, float w, long now) {
            float h = w * layout.heightU / layout.widthU;
            int pxW = (int) Math.min(1024, Math.max(256, Math.round(w / 64f) * 64));
            int pxH = Math.max(64, Math.round(pxW * h / w));
            String bmpKey = key + "@" + pxW;
            Bitmap stat = dieStaticBmps.get(bmpKey);
            if (stat == null) {
                if (dieStaticBmps.size() >= 12) {
                    for (Bitmap b0 : dieStaticBmps.values()) b0.recycle();
                    for (Bitmap b0 : dieDynBmps.values()) b0.recycle();
                    dieStaticBmps.clear();
                    dieDynBmps.clear();
                    dieDynStamps.clear();
                }
                stat = bakeStaticDie(layout, pxW, pxH);
                dieStaticBmps.put(bmpKey, stat);
            }
            Bitmap dyn = dieDynBmps.get(bmpKey);
            long stamp = dieStateStamp(sim);
            Long old = dieDynStamps.get(bmpKey);
            if (dyn == null || old == null || old.longValue() != stamp) {
                if (dyn == null || now - lastDieBakeMs >= 50) {
                    if (dyn == null) {
                        dyn = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888);
                        dieDynBmps.put(bmpKey, dyn);
                    }
                    bakeDynamicDie(dyn, layout, chip, sim);
                    dieDynStamps.put(bmpKey, Long.valueOf(stamp));
                    lastDieBakeMs = now;
                }
            }
            RectF dst = new RectF(x, y, x + w, y + h);
            c.drawBitmap(stat, null, dst, diePaint);
            if (dyn != null) c.drawBitmap(dyn, null, dst, diePaint);
            strokeRect(c, x, y, w, h, 0xFF4A3F8F, dp(1.2f));
            return h;
        }

        // chip die rendering (shared by overview / microscope / catalog)
        void drawDie(Canvas c, ChipBin chip, float x, float y, float size, boolean detailed,
                     int selected, long now, boolean registerHits) {
            String key = chipKey(chip);
            DieLayout layout = getDieLayout(key + "#die", chip, null, 0);
            Cpu sim = (cpu != null && cpu.chip == chip) ? cpu : thumbCpu(key, chip);
            drawLayoutImage(c, layout, key + "#die", chip, sim, x, y, size, now);
            float k = size / layout.widthU;
            if (registerHits) {
                dieRect = new RectF(x + layout.innerX * k, y + layout.innerY * k,
                        x + (layout.innerX + layout.innerW) * k,
                        y + (layout.innerY + layout.innerH) * k);
            }
            // block overlays: name plates, selection, activity, tap targets
            ChipBin.Block[] bs = chip.blocksOrDefault();
            for (int i = 0; i < bs.length && i < layout.blockAreas.size(); i++) {
                ChipBin.Block b = bs[i];
                DieLayout.Area a = layout.blockAreas.get(i);
                float bx = x + a.x * k, by = y + a.y * k, bw = a.w * k, bh = a.h * k;
                if (detailed) {
                    boolean active = cpu != null && sim == cpu && cpu.activeKind == b.kind
                            && (running || cpu.cycles > 0);
                    if (active) {
                        strokeRect(c, bx + 1, by + 1, bw - 2, bh - 2,
                                KIND_COLORS[b.kind >= 0 && b.kind < 7 ? b.kind : 0], dp(1.6f));
                    }
                    if (i == selected) strokeRect(c, bx, by, bw, bh, 0xFFFFFFFF, dp(2));
                    if (bw > dp(54) && bh > dp(20)) {
                        float ts = Math.min(dp(10), bh * 0.30f);
                        String label = b.name;
                        pBold.setTextSize(ts);
                        while (label.length() > 1 && pBold.measureText(label) > bw - dp(10)) {
                            label = label.substring(0, label.length() - 1);
                        }
                        float tw = pBold.measureText(label);
                        rect(c, bx + dp(2), by + dp(2), tw + dp(8), ts + dp(5), 0xB30A0618);
                        text(c, label, bx + dp(6), by + dp(4) + ts * 0.92f, ts, 0xFFFFFFFF, pBold);
                    }
                }
                if (registerHits) addHit(B_BLOCK_BASE + i, bx, by, bw, bh);
            }
            // pulses
            if (detailed) {
                for (int i = pulses.size() - 1; i >= 0; i--) {
                    Pulse pl = pulses.get(i);
                    float t = (now - pl.t0) / 380f;
                    if (t >= 1f) { pulses.remove(i); continue; }
                    for (int kk = 0; kk < 3; kk++) {
                        float tt = t - kk * 0.07f;
                        if (tt < 0) continue;
                        float px = pl.sx + (pl.ex - pl.sx) * tt;
                        float py = pl.sy + (pl.ey - pl.sy) * tt;
                        p.setColor(pl.color);
                        c.drawCircle(px, py, dp(3.4f - kk * 0.9f), p);
                    }
                }
            }
        }

        void drawFbInto(Canvas c, float x, float y, float w, float h) {
            if (cpu == null || cpu.framebuffer == null) return;
            int sw = cpu.chip.screenW, sh = cpu.chip.screenH;
            float cw = w / sw, ch = h / sh;
            for (int yy = 0; yy < sh; yy++) {
                for (int xx = 0; xx < sw; xx++) {
                    int v = cpu.framebuffer[yy * sw + xx];
                    if (v == 0) continue;
                    p.setColor(PALETTE[v & 15]);
                    c.drawRect(x + xx * cw, y + yy * ch, x + (xx + 1) * cw, y + (yy + 1) * ch, p);
                }
            }
        }

        void drawMemMini(Canvas c, float x, float y, float w, float h) {
            if (cpu == null) return;
            int n = cpu.mem.length;
            int cols = n <= 16 ? 4 : n <= 64 ? 8 : 16;
            int rows = (n + cols - 1) / cols;
            float cw = w / cols, ch = h / rows;
            int max = Math.max(1, cpu.chip.mask());
            for (int i = 0; i < n; i++) {
                int cx = i % cols, cy = i / cols;
                float inten = cpu.mem[i] / (float) max;
                int col = i == cpu.lastMemAddr ? (cpu.lastMemWrite ? C_RED : C_GREEN)
                        : blend(0xFF14332E, 0xFF38E1C6, 0.15f + 0.85f * inten);
                p.setColor(col);
                c.drawRect(x + cx * cw + 0.5f, y + cy * ch + 0.5f, x + (cx + 1) * cw - 0.5f, y + (cy + 1) * ch - 0.5f, p);
            }
        }

        void drawRegsMini(Canvas c, float x, float y, float w, float h) {
            if (cpu == null) return;
            int bits = Math.max(1, cpu.chip.bits);
            float rh = h / Math.max(1, cpu.regs.length);
            float bw = w / bits;
            for (int r = 0; r < cpu.regs.length; r++) {
                for (int b = 0; b < bits; b++) {
                    int bit = (cpu.regs[r] >> (bits - 1 - b)) & 1;
                    p.setColor(bit == 1 ? C_AMBER : 0xFF3A3325);
                    c.drawRect(x + b * bw + 0.5f, y + r * rh + 0.5f, x + (b + 1) * bw - 0.5f, y + (r + 1) * rh - 0.5f, p);
                }
            }
        }

        int blend(int a, int b, float t) {
            if (t < 0) t = 0; else if (t > 1) t = 1;
            int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
            int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
            int r = (int) (ar + (br - ar) * t), g = (int) (ag + (bg - ag) * t), bl = (int) (ab + (bb - ab) * t);
            return 0xFF000000 | (r << 16) | (g << 8) | bl;
        }

        String bitsStr(int value, int bits) {
            StringBuffer sb = new StringBuffer();
            for (int i = bits - 1; i >= 0; i--) sb.append(((value >> i) & 1) == 1 ? '1' : '0');
            return sb.toString();
        }

        String hexStr(int value, int bits) {
            int digits = Math.max(1, (bits + 3) / 4);
            String s = Integer.toHexString(value).toUpperCase();
            while (s.length() < digits) s = "0" + s;
            return s;
        }

        // ---------------------------------------------------- main draw
        @Override
        protected void onDraw(Canvas c) {
            W = getWidth();
            H = getHeight();
            if (W <= 0 || H <= 0) return;
            long now = System.currentTimeMillis();
            updateSim(now);
            btns.clear();
            priorityBtnCount = 0;
            c.drawColor(C_BG);
            float contentH = 0;
            switch (screen) {
                case SCR_MENU: contentH = drawMenu(c, now); break;
                case SCR_CATALOG: contentH = drawCatalog(c, now); break;
                case SCR_CHIP: contentH = drawChip(c, now); break;
                case SCR_BUILD: contentH = drawBuilder(c, now); break;
                case SCR_QUESTS: contentH = drawQuests(c, now); break;
                default: break;
            }
            maxScroll = Math.max(0, contentH - H + dp(70));
            if (scrollY > maxScroll) scrollY = maxScroll;
            postInvalidateDelayed(33);
        }

        float drawMenu(Canvas c, long now) {
            float cx = W / 2f;
            float y = dp(30);
            // chip logo
            float s = dp(92);
            ChipBin fake = chips.size() > 2 ? chips.get(2) : null;
            if (fake != null) {
                Cpu save = cpu;
                cpu = null;
                drawDie(c, fake, cx - s / 2, y, s, false, -1, now, false);
                cpu = save;
            }
            y += s + dp(26);
            textC(c, "ChipLab", cx, y, dp(38), C_AMBER, pBold);
            y += dp(24);
            textC(c, "СИМУЛЯТОР СОЗДАНИЯ ЧИПОВ", cx, y, dp(14), C_CYAN, pBold);
            y += dp(18);
            textC(c, "процессоры - схемы - игры - тамагочи - ПК", cx, y, dp(12), C_DIM, p);
            y += dp(26);
            float bw = Math.min(W - dp(48), dp(340)), bx = cx - bw / 2;
            float bh = dp(48), gap = dp(10);
            y = drawButton(c, B_CATALOG, "КАТАЛОГ ЧИПОВ (" + chips.size() + ")", bx, y, bw, bh, true, false) + gap;
            y = drawButton(c, B_BUILDER, "КОНСТРУКТОР ЧИПА", bx, y, bw, bh, false, false) + gap;
            y = drawButton(c, B_QUESTS, "ЗАКАЗЫ ЗАВОДА (" + Integer.bitCount(questMask) + "/" + QUEST_NAMES.length + ")", bx, y, bw, bh, false, false) + gap;
            y = drawButton(c, B_PROMPT, "ПРОМТ ДЛЯ ИИ", bx, y, bw, bh, false, false) + gap;
            y = drawButton(c, B_IMPORT, "ИМПОРТ .BIN ОТ ИИ", bx, y, bw, bh, false, false) + gap;
            y = drawButton(c, B_HELP, "КАК ИГРАТЬ", bx, y, bw, bh, false, false) + gap;
            y += dp(8);
            textC(c, "Звание: " + rankName() + " - Кредиты: " + credits, cx, y, dp(14), C_TEXT, pBold);
            y += dp(20);
            textC(c, "Каждый чип - один .bin файл формата CHIPBIN1.", cx, y, dp(11.5f), C_DIM, p);
            return y + dp(20);
        }

        float drawCatalog(Canvas c, long now) {
            drawHeader(c, "Каталог чипов и устройств", true);
            float top = dp(60);
            drawButton(c, B_IMPORT, "+ ИМПОРТ ОТ ИИ", dp(10), top, (W - dp(30)) / 2, dp(38), false, false);
            drawButton(c, B_BUILDER, "+ КОНСТРУКТОР", W / 2 + dp(5), top, (W - dp(30)) / 2, dp(38), false, false);
            float fy = top + dp(46);
            float fw = (W - dp(20) - dp(9)) / 4f;
            for (int i = 0; i < CAT_NAMES.length; i++) {
                float fx = dp(10) + (i % 4) * (fw + dp(3));
                float fyy = fy + (i / 4) * dp(37);
                drawButton(c, B_FILTER_BASE + i, CAT_NAMES[i] + " " + catalogCount(i),
                        fx, fyy, fw, dp(32), false, catalogFilter == i);
            }
            fy += dp(76);
            textC(c, "показано: " + catalogCount(catalogFilter) + " из " + chips.size()
                    + " - нажми карточку, а внутри будет кнопка просмотра процессора",
                    W / 2, fy + dp(4), dp(11.5f), C_DIM, p);
            priorityBtnCount = btns.size();
            float y = fy + dp(16) - scrollY;
            float margin = dp(10);
            int shown = 0;
            c.save();
            c.clipRect(0, fy + dp(8), W, H);
            for (int i = 0; i < chips.size(); i++) {
                if (catalogFilter != CAT_ALL && chipCategory(i) != catalogFilter) continue;
                shown++;
                ChipBin ch = chips.get(i);
                float cardH = dp(92);
                if (y + cardH > dp(52) && y < H) {
                    roundRect(c, margin, y, W - 2 * margin, cardH, dp(10), C_PANEL);
                    strokeRect(c, margin, y, W - 2 * margin, cardH, C_LINE, dp(1));
                    Cpu save = cpu;
                    cpu = null;
                    drawDie(c, ch, margin + dp(10), y + dp(12), dp(66), false, -1, now, false);
                    cpu = save;
                    float tx = margin + dp(90);
                    String name = ch.name;
                    pBold.setTextSize(dp(14.5f));
                    while (name.length() > 1 && pBold.measureText(name) > W - tx - dp(14)) {
                        name = name.substring(0, name.length() - 1);
                    }
                    text(c, name, tx, y + dp(22), dp(14.5f), C_TEXT, pBold);
                    String line = ChipBin.typeName(ch.type) + " - " + ch.bits + " бит"
                            + (ch.isCpuLike() ? " - R:" + ch.regs : "")
                            + " - MEM:" + ch.memSize
                            + (ch.hasScreen() ? " - " + ch.screenW + "x" + ch.screenH : "");
                    text(c, line, tx, y + dp(42), dp(12), C_CYAN, pMono);
                    text(c, "транзисторов: " + ch.transistors + " - файл: "
                            + (ch.raw != null ? ch.raw.length : 0) + " байт .bin", tx, y + dp(60), dp(12), C_DIM, p);
                    if (chipCustom.get(i)) {
                        text(c, "МОИ - мой чип / от ИИ", tx, y + dp(78), dp(11), C_AMBER, pBold);
                    } else {
                        text(c, CAT_NAMES[chipCategory(i)] + " - пример из игры",
                                tx, y + dp(78), dp(11), C_DIM, p);
                    }
                    addHit(B_CHIP_BASE + i, margin, y, W - 2 * margin, cardH);
                }
                y += cardH + dp(10);
            }
            c.restore();
            if (shown == 0) {
                textC(c, "в этой категории пока пусто - собери чип в конструкторе или импортируй от ИИ",
                        W / 2, fy + dp(52), dp(12.5f), C_DIM, p);
                return fy + dp(80);
            }
            return y + scrollY;
        }

        float drawChip(Canvas c, long now) {
            if (curChip < 0 || curChip >= chips.size()) { go(SCR_CATALOG); return H; }
            ChipBin chip = chips.get(curChip);
            String title = chip.name;
            pBold.setTextSize(dp(17));
            while (title.length() > 1 && pBold.measureText(title) > W - dp(190)) {
                title = title.substring(0, title.length() - 1);
            }
            drawHeader(c, title, true);
            // tabs
            float tw = W / 4f;
            float ty = dp(56);
            String[] tabs = {"ОБЗОР", "МИКРОСКОП", "РАБОТА", ".BIN"};
            int[] tids = {B_TAB0, B_TAB1, B_TAB2, B_TAB3};
            for (int i = 0; i < 4; i++) {
                drawButton(c, tids[i], tabs[i], i * tw + dp(3), ty, tw - dp(6), dp(36), false, chipTab == i);
            }
            float top = ty + dp(46);
            priorityBtnCount = btns.size();
            if (chipTab == TAB_OVERVIEW) return drawOverview(c, chip, top, now);
            if (chipTab == TAB_MICRO) return drawMicro(c, chip, top, now);
            if (chipTab == TAB_RUN) return drawRun(c, chip, top, now);
            return drawBinTab(c, chip, top, now);
        }

        float drawOverview(Canvas c, ChipBin chip, float top, long now) {
            float y = top - scrollY;
            float margin = dp(12);
            float size = Math.min(W - 2 * margin - dp(16), dp(330));
            float dx = (W - size) / 2;
            if (y + size > top - dp(40) && y < H) {
                drawDie(c, chip, dx, y + dp(10), size, true, selBlock, now, true);
            }
            y += size + dp(24);
            textC(c, "нажми на блок - откроется под микроскопом", W / 2, y, dp(11.5f), C_DIM, p);
            y += dp(10);
            drawButton(c, B_TAB1,
                    chip.isCpuLike() ? "ПРОСМОТР ПРОЦЕССОРА: КАК ОН РАБОТАЕТ"
                            : "ПРОСМОТР КРИСТАЛЛА ПОД МИКРОСКОПОМ",
                    margin, y, W - 2 * margin, dp(44), true, false);
            y += dp(54);
            // stats
            roundRect(c, margin, y, W - 2 * margin, dp(118), dp(10), C_PANEL);
            float sx = margin + dp(12), sy = y + dp(20);
            String[] rows = {
                    "Тип: " + ChipBin.typeName(chip.type),
                    "Разрядность: " + chip.bits + " бит (0.." + chip.mask() + ")",
                    chip.isCpuLike() ? "Регистров: " + chip.regs : "Программа: нет, это память",
                    "Память: " + chip.memSize + " ячеек x " + chip.bits + " бит",
                    "Транзисторов: " + chip.transistors,
            };
            for (int i = 0; i < rows.length; i++) {
                text(c, rows[i], sx, sy + i * dp(19), dp(13), C_TEXT, i == 0 ? pBold : p);
            }
            String scrLine = chip.hasScreen() ? "Экран: " + chip.screenW + "x" + chip.screenH : "Экран: нет";
            text(c, scrLine, W / 2 + dp(6), sy, dp(13), C_CYAN, pBold);
            text(c, "Частота: " + (chip.clockKhz >= 1000 ? (chip.clockKhz / 1000.0) + " МГц" : chip.clockKhz + " кГц"),
                    W / 2 + dp(6), sy + dp(19), dp(13), C_TEXT, p);
            text(c, "Файл: " + (chip.raw != null ? chip.raw.length : 0) + " байт", W / 2 + dp(6), sy + dp(38), dp(13), C_TEXT, p);
            text(c, "Категория: " + CAT_NAMES[chipCategory(curChip)], W / 2 + dp(6), sy + dp(57), dp(13), C_AMBER, pBold);
            y += dp(130);
            // description
            text(c, "КАК ЭТО РАБОТАЕТ", margin, y + dp(6), dp(13), C_AMBER, pBold);
            y += dp(12);
            ArrayList<String> lines = wrap(chip.desc, W - 2 * margin - dp(8), p, dp(13.5f));
            float hh = drawLines(c, lines, margin + dp(2), y, dp(17), dp(13.5f), C_TEXT, p, top, H);
            y += hh + dp(16);
            float bw = (W - 2 * margin - dp(8)) / 2;
            drawButton(c, B_TAB2, chip.isCpuLike() ? "ЗАПУСТИТЬ" : "ОТКРЫТЬ ЯЧЕЙКИ", margin, y, bw, dp(42), true, false);
            drawButton(c, B_EXPORT, "ЭКСПОРТ BASE64", margin + bw + dp(8), y, bw, dp(42), false, false);
            y += dp(52);
            if (chipCustom.get(curChip)) {
                drawButton(c, B_DELETE, "УДАЛИТЬ ЧИП", margin, y, W - 2 * margin, dp(38), false, false);
                y += dp(48);
            }
            return y + scrollY;
        }

        // ---------------------------------------------------- microscope
        float drawMicro(Canvas c, ChipBin chip, float top, long now) {
            float y = top;
            float margin = dp(12);
            // zoom segmented
            float zw = (W - 2 * margin - dp(8)) / 3;
            drawButton(c, B_ZOOM0, "КРИСТАЛЛ", margin, y, zw, dp(34), false, microZoom == 0);
            drawButton(c, B_ZOOM1, "БЛОК", margin + zw + dp(4), y, zw, dp(34), false, microZoom == 1);
            drawButton(c, B_ZOOM2, "ТРАНЗИСТОРЫ", margin + 2 * zw + dp(8), y, zw, dp(34), false, microZoom == 2);
            y += dp(42);
            ChipBin.Block[] bsTop = chip.blocksOrDefault();
            if (selBlock < 0 || selBlock >= bsTop.length) selBlock = 0;
            if (microZoom == 2) {
                // Macro shot: the selected block's real layout, full width.
                String key = chipKey(chip);
                ChipBin.Block bb = bsTop[selBlock];
                DieLayout macro = getDieLayout(key + "#blk2#" + selBlock, chip, bb, 2);
                Cpu sim = (cpu != null && cpu.chip == chip) ? cpu : thumbCpu(key, chip);
                float iw = W - 2 * margin;
                float ih = drawLayoutImage(c, macro, key + "#blk2#" + selBlock, chip, sim,
                        margin, y + dp(4), iw, now);
                y += ih + dp(12);
                textC(c, "топология блока: слои масок, транзисторы, дорожки", W / 2, y + dp(4), dp(11.5f), C_DIM, p);
                y += dp(16);
            } else {
                float size = Math.min(W - 2 * margin - dp(16), dp(320));
                float dx = (W - size) / 2;
                drawDie(c, chip, dx, y + dp(8), size, true, selBlock, now, true);
                y += size + dp(18);
            }
            // sim mini controls
            if (chip.isCpuLike()) {
                float bw = (W - 2 * margin - dp(16)) / 4;
                drawButton(c, B_RESET, "СБРОС", margin, y, bw, dp(36), false, false);
                drawButton(c, B_STEP, "ШАГ", margin + bw + dp(5), y, bw, dp(36), false, false);
                drawButton(c, B_RUN, running ? "ПАУЗА" : "ПУСК", margin + 2 * bw + dp(10), y, bw, dp(36), true, false);
                drawButton(c, B_SPEED, "x" + (speed == 0 ? 1 : speed == 1 ? 8 : 100), margin + 3 * bw + dp(15), y, bw, dp(36), false, false);
                y += dp(44);
                if (cpu != null) {
                    text(c, "такт: " + cpu.cycles + " - PC=" + cpu.pc + " - " +
                            (cpu.halted ? "HALT (стоп)" : Cpu.disasm(chip.program, cpu.pc)),
                            margin, y + dp(4), dp(12.5f), C_CYAN, pMono);
                }
                y += dp(16);
                if (usesGamepad()) {
                    y += drawGamepad(c, margin, y, W - 2 * margin);
                }
            }
            priorityBtnCount = btns.size();
            // selected block detail
            ChipBin.Block[] bs = chip.blocksOrDefault();
            if (selBlock < 0 || selBlock >= bs.length) selBlock = 0;
            ChipBin.Block b = bs[selBlock];
            y -= scrollY;
            float detailY = y;
            float detailH = 0;
            text(c, b.name, margin, y + dp(8), dp(15), KIND_COLORS[b.kind >= 0 && b.kind < 7 ? b.kind : 0], pBold);
            textR(c, b.transistors + " транзисторов", W - margin, y + dp(8), dp(12.5f), C_DIM, p);
            y += dp(20);
            float innerW = W - 2 * margin;
            if (microZoom == 1) {
                // The block enlarged: real synthesized layout over live signals.
                String key = chipKey(chip);
                DieLayout det = getDieLayout(key + "#blk1#" + selBlock, chip, b, 1);
                Cpu sim = (cpu != null && cpu.chip == chip) ? cpu : thumbCpu(key, chip);
                y += drawLayoutImage(c, det, key + "#blk1#" + selBlock, chip, sim,
                        margin, y, innerW, now) + dp(12);
            }
            if (cpu != null) {
                switch (b.kind) {
                    case ChipBin.KIND_REGS:
                        detailH = drawRegsDetail(c, margin, y, innerW);
                        break;
                    case ChipBin.KIND_RAM:
                        detailH = drawMemDetail(c, margin, y, innerW, chip);
                        break;
                    case ChipBin.KIND_ALU:
                        detailH = drawAluDetail(c, margin, y, innerW, now);
                        break;
                    case ChipBin.KIND_SCREEN:
                        detailH = drawScreenDetail(c, margin, y, innerW);
                        break;
                    default:
                        detailH = drawTransistorGrid(c, margin, y, innerW, b, chip, now);
                        break;
                }
            }
            y += detailH;
            String hint = microHint(b.kind, microZoom);
            ArrayList<String> lines = wrap(hint, innerW, p, dp(12.5f));
            float hh = drawLines(c, lines, margin, y + dp(4), dp(16), dp(12.5f), C_DIM, p, top, H);
            y += hh + dp(16);
            // block chips selector (small buttons under detail)
            text(c, "Блоки: нажимай на кристалл выше или сюда:", margin, y + dp(4), dp(12), C_DIM, p);
            y += dp(12);
            float bx = margin;
            for (int i = 0; i < bs.length; i++) {
                String s = (i + 1) + "";
                float bw2 = dp(36);
                if (bx + bw2 > W - margin) { bx = margin; y += dp(42); }
                drawButton(c, B_BLOCK_BASE + i, s, bx, y, bw2, dp(34), false, i == selBlock);
                bx += bw2 + dp(6);
            }
            y += dp(44);
            return y + scrollY;
        }

        String microHint(int kind, int zoom) {
            switch (kind) {
                case ChipBin.KIND_REGS:
                    return "Каждый бит регистра - триггер из 6 транзисторов. Жёлтый = 1 (ток течёт), тёмный = 0. "
                            + "Значения НАСТОЯЩИЕ: нажми ШАГ выше и смотри, как биты переключаются по тактам.";
                case ChipBin.KIND_RAM:
                    return "Ячейки памяти с реальными значениями. Зелёная вспышка = чтение, красная = запись. "
                            + "Адрес выбирает дешифратор: старшие биты - строку, младшие - столбец.";
                case ChipBin.KIND_ALU:
                    return "АЛУ собрано из полных сумматоров - по одному на бит. A и B - операнды последней операции, "
                            + "SUM - результат, стрелки между разрядами - перенос (carry). Один сумматор = 2 XOR + 2 AND + OR, около 28 транзисторов.";
                case ChipBin.KIND_SCREEN:
                    return "Каждый пиксель - ячейка видеопамяти и свой транзистор-ключ. OUT пишет пиксели по порядку: "
                            + "слева направо, сверху вниз, как луч старого телевизора.";
                case ChipBin.KIND_BUS:
                    return "Шина - пучок дорожек, по которым бегут биты между блоками. Импульсы на кристалле показывают, какой блок сейчас работает.";
                case ChipBin.KIND_CONTROL:
                    return "Устройство управления читает код команды и по тактам включает нужные блоки: регистры, АЛУ, память. Это дирижёр всего чипа.";
                default:
                    return "Логические вентили из транзисторов MOSFET: затвор (G) открывает канал между стоком (D) и истоком (S). "
                            + "Из таких ключей собираются AND, OR, XOR и всё остальное.";
            }
        }

        float drawRegsDetail(Canvas c, float x, float y, float w) {
            float startY = y;
            int bits = Math.max(1, cpu.chip.bits);
            float labelW = dp(38);
            float valW = dp(76);
            float cellW = Math.min((w - labelW - valW) / bits, dp(30));
            float cellH = dp(microZoom == 2 ? 34 : 24);
            for (int r = 0; r < cpu.regs.length; r++) {
                float ry = y + r * (cellH + dp(6));
                boolean hot = r == cpu.lastRegWrite;
                text(c, "R" + r, x, ry + cellH * 0.68f, dp(14), hot ? C_AMBER : C_TEXT, pBold);
                for (int bIdx = 0; bIdx < bits; bIdx++) {
                    int bit = (cpu.regs[r] >> (bits - 1 - bIdx)) & 1;
                    float cx = x + labelW + bIdx * cellW;
                    roundRect(c, cx + 1, ry, cellW - 2, cellH, dp(3), bit == 1 ? C_AMBER : 0xFF262D47);
                    if (microZoom == 2) {
                        // 6 transistor dots inside the flip-flop cell
                        for (int tI = 0; tI < 6; tI++) {
                            float ddx = cx + cellW * (0.22f + 0.28f * (tI % 3));
                            float ddy = ry + cellH * (tI < 3 ? 0.34f : 0.68f);
                            p.setColor(bit == 1 ? 0xFF6D4C00 : 0xFF48536E);
                            c.drawCircle(ddx, ddy, dp(1.8f), p);
                        }
                    } else {
                        textC(c, bit == 1 ? "1" : "0", cx + cellW / 2, ry + cellH * 0.68f, dp(12),
                                bit == 1 ? 0xFF141414 : C_DIM, pBold);
                    }
                }
                float vx = x + labelW + bits * cellW + dp(6);
                text(c, "=" + cpu.regs[r], vx, ry + cellH * 0.68f, dp(13), C_CYAN, pMono);
            }
            float fy = y + cpu.regs.length * (cellH + dp(6)) + dp(8);
            drawLamp(c, x + labelW, fy, "Z (ноль)", cpu.flagZ);
            drawLamp(c, x + labelW + dp(110), fy, "C (перенос)", cpu.flagC);
            return fy + dp(24) - startY;
        }

        void drawLamp(Canvas c, float x, float y, String label, boolean on) {
            p.setColor(on ? C_GREEN : 0xFF2A3B2E);
            c.drawCircle(x + dp(7), y + dp(4), dp(7), p);
            text(c, label, x + dp(20), y + dp(9), dp(12.5f), on ? C_TEXT : C_DIM, pBold);
        }

        float drawMemDetail(Canvas c, float x, float y, float w, ChipBin chip) {
            float startY = y;
            int n = cpu.mem.length;
            int cols = n <= 16 ? 4 : n <= 64 ? 8 : 16;
            float cw = w / cols;
            float ch = dp(n <= 16 ? 34 : 21);
            int rows = (n + cols - 1) / cols;
            boolean big = microZoom == 2 && chip.bits <= 8 && n <= 64;
            if (big) ch = dp(30);
            for (int i = 0; i < n; i++) {
                float cx = x + (i % cols) * cw, cy = y + (i / cols) * (ch + dp(2));
                boolean hot = i == cpu.lastMemAddr;
                int bg = hot ? (cpu.lastMemWrite ? C_RED : C_GREEN) : C_PANEL2;
                roundRect(c, cx + 1, cy, cw - 2, ch, dp(3), bg);
                if (big) {
                    // bits inside cell
                    int bits = chip.bits;
                    float bw = (cw - dp(6)) / bits;
                    for (int bIdx = 0; bIdx < bits; bIdx++) {
                        int bit = (cpu.mem[i] >> (bits - 1 - bIdx)) & 1;
                        p.setColor(bit == 1 ? C_AMBER : 0xFF333D5C);
                        c.drawRect(cx + dp(3) + bIdx * bw, cy + dp(5), cx + dp(3) + (bIdx + 1) * bw - 1, cy + ch - dp(5), p);
                    }
                } else {
                    textC(c, hexStr(cpu.mem[i], chip.bits), cx + cw / 2, cy + ch * 0.68f,
                            dp(n <= 16 ? 14 : 10.5f), hot ? 0xFF141414 : C_TEXT, pMono);
                }
            }
            float yy = y + rows * (ch + dp(2)) + dp(6);
            if (cpu.lastMemAddr >= 0) {
                text(c, "последний доступ: адрес " + cpu.lastMemAddr + " (" + (cpu.lastMemWrite ? "запись" : "чтение")
                        + ") значение " + cpu.mem[cpu.lastMemAddr], x, yy, dp(12), C_DIM, pMono);
                yy += dp(16);
            }
            return yy - startY;
        }

        float drawScreenDetail(Canvas c, float x, float y, float w) {
            float startY = y;
            if (cpu.framebuffer == null) {
                text(c, "у этого чипа нет экрана", x, y + dp(14), dp(13), C_DIM, p);
                return dp(24);
            }
            int sw = cpu.chip.screenW, sh = cpu.chip.screenH;
            float cw = w / sw;
            float chPix = Math.min(cw, dp(26));
            // center
            float fx = x + (w - cw * sw) / 2;
            rect(c, fx - dp(4), y - dp(4), cw * sw + dp(8), chPix * sh + dp(8), 0xFF05070D);
            for (int yy = 0; yy < sh; yy++) {
                for (int xx = 0; xx < sw; xx++) {
                    int v = cpu.framebuffer[yy * sw + xx];
                    float px = fx + xx * cw, py = y + yy * chPix;
                    p.setColor(PALETTE[v & 15]);
                    c.drawRect(px + 0.5f, py + 0.5f, px + cw - 0.5f, py + chPix - 0.5f, p);
                    if (microZoom == 2 && cw > dp(14)) {
                        // transistor dot per pixel
                        p.setColor(v != 0 ? 0xFFFFFFFF : 0xFF30364A);
                        c.drawCircle(px + cw / 2, py + chPix / 2, dp(1.4f), p);
                    }
                }
            }
            // cursor
            int curX = cpu.fbCursor % sw, curY = cpu.fbCursor / sw;
            strokeRect(c, fx + curX * cw, y + curY * chPix, cw, chPix, 0xFFFFFFFF, dp(1.4f));
            float yy = y + chPix * sh + dp(14);
            text(c, "следующий OUT нарисует пиксель x=" + curX + " y=" + curY
                    + " - горит пикселей: " + cpu.litPixels(), x, yy, dp(12), C_DIM, pMono);
            return yy + dp(8) - startY;
        }

        float drawAluDetail(Canvas c, float x, float y, float w, long now) {
            float startY = y;
            int bits = Math.max(1, cpu.chip.bits);
            String opName = cpu.aluOp >= 0 ? opShortName(cpu.aluOp) : "-";
            text(c, "последняя операция: " + opName + "   A=" + cpu.aluA + "  B=" + cpu.aluB + "  = " + cpu.aluResult,
                    x, y + dp(6), dp(12.5f), C_TEXT, pMono);
            y += dp(22);
            float cw = Math.min(w / bits, dp(46));
            float fx = x + (w - cw * bits) / 2;
            float boxH = dp(38);
            // compute carry chain for ADD (educational); for others show result bits
            int[] carry = new int[bits + 1];
            if (cpu.aluOp == 0x03 || cpu.aluOp == 0x0B) {
                int cc = 0;
                for (int i = 0; i < bits; i++) {
                    int ab = (cpu.aluA >> i) & 1, bb = (cpu.aluB >> i) & 1;
                    carry[i] = cc;
                    cc = (ab + bb + cc) >= 2 ? 1 : 0;
                }
                carry[bits] = cc;
            }
            for (int i = 0; i < bits; i++) {
                int pos = bits - 1 - i; // display MSB left
                float bx = fx + i * cw;
                int ab = (cpu.aluA >> pos) & 1, bb = (cpu.aluB >> pos) & 1, sb = (cpu.aluResult >> pos) & 1;
                // input wires
                drawWireBit(c, bx + cw * 0.32f, y, ab, "A" + pos);
                drawWireBit(c, bx + cw * 0.68f, y, bb, "B" + pos);
                float by = y + dp(30);
                roundRect(c, bx + dp(2), by, cw - dp(4), boxH, dp(4), sb == 1 ? 0xFF6D4C41 : C_PANEL2);
                textC(c, "+", bx + cw / 2, by + boxH * 0.62f, dp(15), C_TEXT, pBold);
                // sum wire
                p.setColor(sb == 1 ? C_AMBER : 0xFF3A425E);
                p.setStrokeWidth(dp(2));
                c.drawLine(bx + cw / 2, by + boxH, bx + cw / 2, by + boxH + dp(12), p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(bx + cw / 2, by + boxH + dp(16), dp(4), p);
                textC(c, sb == 1 ? "1" : "0", bx + cw / 2, by + boxH + dp(32), dp(11), sb == 1 ? C_AMBER : C_DIM, pBold);
                // carry arrow toward next (more significant) bit = visually left
                if (i > 0) {
                    int carryFromRight = carry[pos];
                    int col = carryFromRight == 1 ? C_RED : 0xFF3A425E;
                    p.setColor(col);
                    p.setStrokeWidth(dp(1.8f));
                    float ay = by + boxH * 0.78f;
                    c.drawLine(bx + cw, ay, bx + dp(2), ay, p);
                    p.setStyle(Paint.Style.FILL);
                    // arrow head
                    c.drawCircle(bx + dp(3), ay, dp(2.2f), p);
                }
            }
            y += dp(30) + boxH + dp(42);
            if (microZoom == 2) {
                // one enlarged full adder bit 0 with transistor glyphs
                text(c, "БИТ 0 ПОД МИКРОСКОПОМ: вентили XOR/AND из транзисторов", x, y + dp(4), dp(11.5f), C_AMBER, pBold);
                y += dp(14);
                int a0 = cpu.aluA & 1, b0 = cpu.aluB & 1;
                int s0 = cpu.aluResult & 1;
                float gh = dp(120);
                roundRect(c, x, y, w, gh, dp(8), 0xFF10182E);
                // rails
                p.setStrokeWidth(dp(1.6f));
                p.setColor(0xFFFF8A65);
                c.drawLine(x + dp(14), y + dp(16), x + w - dp(14), y + dp(16), p);
                p.setColor(0xFF5C6BC0);
                c.drawLine(x + dp(14), y + gh - dp(16), x + w - dp(14), y + gh - dp(16), p);
                p.setStyle(Paint.Style.FILL);
                text(c, "Vdd (питание)", x + dp(14), y + dp(12), dp(10), 0xFFFF8A65, pMono);
                text(c, "GND (земля)", x + dp(14), y + gh - dp(4), dp(10), 0xFF5C6BC0, pMono);
                // 6 transistors: XOR-ish pattern glowing by inputs
                boolean[] on = {a0 == 1, b0 == 1, (a0 ^ b0) == 1, (a0 & b0) == 1, s0 == 1, (a0 | b0) == 1};
                String[] gl = {"A", "B", "A^B", "A&B", "SUM", "Cout"};
                float tw2 = w / 6;
                for (int i = 0; i < 6; i++) {
                    float tcx = x + tw2 * i + tw2 / 2;
                    float tcy = y + gh / 2;
                    drawMosfet(c, tcx, tcy, dp(15), on[i]);
                    textC(c, gl[i] + "=" + (on[i] ? 1 : 0), tcx, tcy + dp(38), dp(10.5f), on[i] ? C_AMBER : C_DIM, pMono);
                }
                y += gh + dp(8);
            }
            return y - startY;
        }

        void drawWireBit(Canvas c, float x, float y, int bit, String label) {
            p.setColor(bit == 1 ? C_AMBER : 0xFF3A425E);
            p.setStrokeWidth(dp(2));
            c.drawLine(x, y + dp(8), x, y + dp(28), p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(x, y + dp(6), dp(3.6f), p);
            textC(c, bit == 1 ? "1" : "0", x, y - dp(1), dp(9.5f), bit == 1 ? C_AMBER : C_DIM, pMono);
        }

        String opShortName(int op) {
            switch (op) {
                case 0x03: return "ADD";
                case 0x04: return "SUB";
                case 0x05: return "AND";
                case 0x06: return "OR";
                case 0x07: return "XOR";
                case 0x08: return "NOT";
                case 0x09: return "SHL";
                case 0x0A: return "SHR";
                case 0x0B: return "INC";
                case 0x0C: return "DEC";
                case 0x15: return "CMP";
                default: return "op " + ChipBin.hex2(op);
            }
        }

        void drawMosfet(Canvas c, float cx, float cy, float s, boolean on) {
            int col = on ? C_AMBER : 0xFF4A5578;
            p.setColor(col);
            p.setStrokeWidth(dp(2));
            // gate vertical + terminal
            c.drawLine(cx - s * 0.55f, cy, cx - s * 0.18f, cy, p);
            c.drawLine(cx - s * 0.18f, cy - s * 0.62f, cx - s * 0.18f, cy + s * 0.62f, p);
            // channel segments
            c.drawLine(cx + s * 0.12f, cy - s * 0.62f, cx + s * 0.12f, cy - s * 0.14f, p);
            c.drawLine(cx + s * 0.12f, cy + s * 0.14f, cx + s * 0.12f, cy + s * 0.62f, p);
            if (on) c.drawLine(cx + s * 0.12f, cy - s * 0.14f, cx + s * 0.12f, cy + s * 0.14f, p);
            // drain/source terminals
            c.drawLine(cx + s * 0.12f, cy - s * 0.62f, cx + s * 0.62f, cy - s * 0.62f, p);
            c.drawLine(cx + s * 0.62f, cy - s * 0.62f, cx + s * 0.62f, cy - s * 1.0f, p);
            c.drawLine(cx + s * 0.12f, cy + s * 0.62f, cx + s * 0.62f, cy + s * 0.62f, p);
            c.drawLine(cx + s * 0.62f, cy + s * 0.62f, cx + s * 0.62f, cy + s * 1.0f, p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx - s * 0.55f, cy, dp(2), p);
            text(c, "G", cx - s * 0.95f, cy + dp(3), dp(9), col, pMono);
            text(c, "D", cx + s * 0.68f, cy - s * 0.85f, dp(9), col, pMono);
            text(c, "S", cx + s * 0.68f, cy + s * 1.05f, dp(9), col, pMono);
        }

        float drawTransistorGrid(Canvas c, float x, float y, float w, ChipBin.Block b, ChipBin chip, long now) {
            float startY = y;
            int count = Math.max(4, Math.min(b.transistors, microZoom == 2 ? 60 : 28));
            int cols = microZoom == 2 ? 6 : 7;
            float cw = w / cols;
            float chh = dp(microZoom == 2 ? 52 : 40);
            int seed = b.name.hashCode();
            long phase = cpu != null ? cpu.cycles : now / 400;
            for (int i = 0; i < count; i++) {
                float cx = x + (i % cols) * cw + cw / 2;
                float cy = y + (i / cols) * chh + chh / 2;
                long hsh = (seed * 31L + i * 2654435761L) ^ (phase * (i % 5 + 1));
                boolean on = ((hsh >> (i % 13)) & 1) == 1 || (cpu != null && running && (i % 7 == (int) (cpu.cycles % 7)));
                roundRect(c, cx - cw / 2 + dp(2), cy - chh / 2 + dp(2), cw - dp(4), chh - dp(4), dp(4),
                        on ? 0xFF3A2F4D : 0xFF171E33);
                drawMosfet(c, cx, cy, dp(microZoom == 2 ? 13 : 10), on);
            }
            int rows = (count + cols - 1) / cols;
            y += rows * chh + dp(6);
            text(c, "показано " + count + " из " + b.transistors + " транзисторов блока"
                    + (count < b.transistors ? " (остальные такие же, честное слово)" : ""),
                    x, y + dp(4), dp(11.5f), C_DIM, p);
            y += dp(14);
            return y - startY;
        }

        // ---------------------------------------------------- run tab
        float drawRun(Canvas c, ChipBin chip, float top, long now) {
            float margin = dp(12);
            float y = top;
            if (!chip.isCpuLike()) {
                return drawRamExplorer(c, chip, top, now);
            }
            // controls fixed at top of tab
            float bw = (W - 2 * margin - dp(15)) / 4;
            drawButton(c, B_RESET, "СБРОС", margin, y, bw, dp(40), false, false);
            drawButton(c, B_STEP, "ШАГ", margin + bw + dp(5), y, bw, dp(40), false, false);
            drawButton(c, B_RUN, running ? "ПАУЗА" : "ПУСК", margin + 2 * bw + dp(10), y, bw, dp(40), true, false);
            drawButton(c, B_SPEED, "СКОР. x" + (speed == 0 ? 1 : speed == 1 ? 8 : 100), margin + 3 * bw + dp(15), y, bw, dp(40), false, false);
            y += dp(48);
            drawButton(c, B_TAB1, "ПРОСМОТР ПРОЦЕССОРА: КАК ОН РАБОТАЕТ",
                    margin, y, W - 2 * margin, dp(40), false, false);
            priorityBtnCount = btns.size();
            y += dp(50);
            y -= scrollY;
            float contentTop = y;
            if (cpu == null) return H;
            // status line
            roundRect(c, margin, y, W - 2 * margin, dp(62), dp(10), C_PANEL);
            text(c, "ТАКТ: " + cpu.cycles, margin + dp(12), y + dp(20), dp(14), C_TEXT, pBold);
            text(c, cpu.halted ? "HALT - программа завершена. СБРОС запустит сначала." : "следующая команда по PC=" + cpu.pc,
                    margin + dp(12), y + dp(40), dp(12), cpu.halted ? C_RED : C_DIM, p);
            textR(c, cpu.halted ? "СТОП" : Cpu.disasm(chip.program, cpu.pc), W - margin - dp(12), y + dp(22), dp(14), C_CYAN, pMono);
            drawLamp(c, margin + dp(12), y + dp(44), "Z", cpu.flagZ);
            drawLamp(c, margin + dp(62), y + dp(44), "C", cpu.flagC);
            y += dp(72);
            // screen first if present
            if (cpu.framebuffer != null) {
                text(c, "ЭКРАН " + chip.screenW + "x" + chip.screenH, margin, y + dp(10), dp(13), C_AMBER, pBold);
                y += dp(16);
                y += drawScreenDetail(c, margin, y, W - 2 * margin) + dp(6);
                if (usesGamepad()) {
                    text(c, "УПРАВЛЕНИЕ УСТРОЙСТВОМ", margin, y + dp(10), dp(13), C_AMBER, pBold);
                    y += dp(16);
                    y += drawGamepad(c, margin, y, W - 2 * margin) + dp(4);
                }
            }
            // registers
            text(c, "РЕГИСТРЫ", margin, y + dp(10), dp(13), C_AMBER, pBold);
            y += dp(16);
            y += drawRegsDetail(c, margin, y, W - 2 * margin) + dp(4);
            // output LEDs
            text(c, "ВЫХОД (OUT)", margin, y + dp(10), dp(13), C_AMBER, pBold);
            y += dp(18);
            int bits = Math.max(1, chip.bits);
            float ledS = Math.min((W - 2 * margin - dp(110)) / bits, dp(26));
            int outVal = cpu.lastOut < 0 ? 0 : cpu.lastOut;
            for (int i = 0; i < bits; i++) {
                int bit = (outVal >> (bits - 1 - i)) & 1;
                float cx = margin + i * (ledS + dp(3)) + ledS / 2;
                p.setColor(cpu.lastOut >= 0 && bit == 1 ? 0xFFFFD54F : 0xFF232A44);
                c.drawCircle(cx, y + ledS / 2, ledS / 2, p);
                strokeRect(c, cx - ledS / 2, y, ledS, ledS, C_LINE, dp(0.6f));
            }
            text(c, cpu.lastOut >= 0 ? "= " + cpu.lastOut + " (0x" + hexStr(outVal, bits) + ")" : "пока тихо",
                    margin + bits * (ledS + dp(3)) + dp(8), y + ledS * 0.72f, dp(15), C_CYAN, pMono);
            y += ledS + dp(8);
            // out history
            if (cpu.outLogSize > 0) {
                StringBuffer sb = new StringBuffer("было: ");
                int from = Math.max(0, cpu.outLogSize - 14);
                for (int i = from; i < cpu.outLogSize; i++) {
                    if (i > from) sb.append(", ");
                    sb.append(cpu.outLog[i]);
                }
                ArrayList<String> ls = wrap(sb.toString(), W - 2 * margin, pMono, dp(11.5f));
                y += drawLines(c, ls, margin, y, dp(15), dp(11.5f), C_DIM, pMono, 0, H) + dp(4);
            }
            // memory
            text(c, "ПАМЯТЬ (" + chip.memSize + " ячеек)", margin, y + dp(10), dp(13), C_AMBER, pBold);
            y += dp(16);
            y += drawMemDetail(c, margin, y, W - 2 * margin, chip) + dp(8);
            // program listing near pc
            text(c, "ПРОГРАММА (стрелка - текущая команда)", margin, y + dp(10), dp(13), C_AMBER, pBold);
            y += dp(18);
            int pcWalk = 0;
            int shown = 0;
            ArrayList<int[]> addrs = new ArrayList<int[]>();
            while (pcWalk < chip.program.length && shown < 200) {
                addrs.add(new int[]{pcWalk});
                pcWalk += Cpu.instrLen(chip.program[pcWalk] & 0xFF);
                shown++;
            }
            // find window around pc
            int pcIdx = 0;
            for (int i = 0; i < addrs.size(); i++) if (addrs.get(i)[0] == cpu.pc) pcIdx = i;
            int from2 = Math.max(0, Math.min(pcIdx - 3, addrs.size() - 14));
            if (from2 < 0) from2 = 0;
            int to2 = Math.min(addrs.size(), from2 + 14);
            for (int i = from2; i < to2; i++) {
                int a = addrs.get(i)[0];
                boolean cur = a == cpu.pc && !cpu.halted;
                if (cur) roundRect(c, margin - dp(4), y + dp(2), W - 2 * margin + dp(8), dp(19), dp(4), 0xFF27345E);
                text(c, (cur ? "> " : "  ") + ChipBin.hex2(a) + ": " + Cpu.disasm(chip.program, a),
                        margin + dp(4), y + dp(16), dp(13), cur ? C_AMBER : C_DIM, pMono);
                y += dp(20);
            }
            y += dp(10);
            return y + scrollY;
        }

        float drawRamExplorer(Canvas c, ChipBin chip, float top, long now) {
            float margin = dp(12);
            float y = top - scrollY;
            if (cpu == null) return H;
            if (selCell >= cpu.mem.length) selCell = 0;
            text(c, "ЯЧЕЙКИ ПАМЯТИ - нажимай на ячейку", margin, y + dp(10), dp(13), C_AMBER, pBold);
            y += dp(18);
            int n = cpu.mem.length;
            int cols = n <= 16 ? 4 : n <= 64 ? 8 : 16;
            float cw = (W - 2 * margin) / cols;
            float chh = dp(n <= 16 ? 46 : n <= 64 ? 30 : 22);
            int rows = (n + cols - 1) / cols;
            for (int i = 0; i < n; i++) {
                float cx = margin + (i % cols) * cw, cy = y + (i / cols) * (chh + dp(3));
                boolean sel = i == selCell;
                boolean hot = i == cpu.lastMemAddr;
                roundRect(c, cx + 1, cy, cw - 2, chh, dp(4), sel ? C_AMBER : hot ? 0xFF2E5E34 : C_PANEL2);
                textC(c, hexStr(cpu.mem[i], chip.bits), cx + cw / 2, cy + chh * 0.52f,
                        dp(n <= 16 ? 16 : 11), sel ? 0xFF141414 : C_TEXT, pMono);
                if (n <= 64) {
                    textC(c, "" + i, cx + cw / 2, cy + chh * 0.86f, dp(8.5f), sel ? 0xFF555555 : C_DIM, pMono);
                }
                addHit(B_CELL_BASE + i, cx, cy, cw, chh);
            }
            y += rows * (chh + dp(3)) + dp(14);
            // selected cell editor
            roundRect(c, margin, y, W - 2 * margin, dp(150), dp(10), C_PANEL);
            int addrBits = 1;
            while ((1 << addrBits) < n) addrBits++;
            text(c, "АДРЕС " + selCell + " = " + bitsStr(selCell, addrBits) + " (двоичный)", margin + dp(12), y + dp(22), dp(13), C_CYAN, pMono);
            int halfBits = addrBits / 2;
            int rowBits = addrBits - halfBits;
            int row = selCell >> halfBits, col = selCell & ((1 << halfBits) - 1);
            text(c, "дешифратор: строка " + row + " (старшие " + rowBits + " бит), столбец " + col,
                    margin + dp(12), y + dp(40), dp(12), C_DIM, p);
            // bit toggles
            int bits = Math.max(1, chip.bits);
            float bw = Math.min((W - 2 * margin - dp(24)) / bits, dp(44));
            float bx = margin + dp(12);
            for (int i = 0; i < bits; i++) {
                int bitPos = bits - 1 - i;
                int bit = (cpu.mem[selCell] >> bitPos) & 1;
                roundRect(c, bx + i * bw + 1, y + dp(52), bw - 2, dp(40), dp(5), bit == 1 ? C_AMBER : 0xFF262D47);
                textC(c, bit == 1 ? "1" : "0", bx + i * bw + bw / 2, y + dp(78), dp(16), bit == 1 ? 0xFF141414 : C_DIM, pBold);
                addHit(B_BIT_BASE + bitPos, bx + i * bw, y + dp(52), bw, dp(40));
            }
            text(c, "значение: " + cpu.mem[selCell] + " (0x" + hexStr(cpu.mem[selCell], bits) + ") - нажми на бит, чтобы переключить",
                    margin + dp(12), y + dp(112), dp(12.5f), C_TEXT, p);
            text(c, "ячейка SRAM: 6 транзисторов держат каждый бит, пока есть питание",
                    margin + dp(12), y + dp(132), dp(11.5f), C_DIM, p);
            y += dp(162);
            float bw2 = (W - 2 * margin - dp(15)) / 4;
            drawButton(c, B_RAM_MINUS, "-1", margin, y, bw2, dp(40), false, false);
            drawButton(c, B_RAM_PLUS, "+1", margin + bw2 + dp(5), y, bw2, dp(40), false, false);
            drawButton(c, B_RAM_TEST, "ТЕСТ ЗАПИСИ", margin + 2 * bw2 + dp(10), y, bw2, dp(40), true, false);
            drawButton(c, B_RAM_CLEAR, "ОЧИСТИТЬ", margin + 3 * bw2 + dp(15), y, bw2, dp(40), false, false);
            y += dp(54);
            return y + scrollY;
        }

        // ---------------------------------------------------- bin tab
        float drawBinTab(Canvas c, ChipBin chip, float top, long now) {
            float margin = dp(10);
            float y = top;
            float bw = (W - 2 * margin - dp(8)) / 3;
            drawButton(c, B_BINMODE, binHex ? "HEX-ДАМП" : "СТРУКТУРА", margin, y, bw, dp(36), false, true);
            drawButton(c, B_BINMODE, binHex ? "СТРУКТУРА" : "HEX-ДАМП", margin + bw + dp(4), y, bw, dp(36), false, false);
            drawButton(c, B_EXPORT, "BASE64", margin + 2 * bw + dp(8), y, bw, dp(36), false, false);
            y += dp(44);
            if (cachedBinFor != curChip || cachedBinHex != binHex) {
                cachedBinText = binHex ? chip.hexDump() : chip.describe();
                cachedBinFor = curChip;
                cachedBinHex = binHex;
                cachedBinLines = null;
            }
            if (cachedBinLines == null || cachedBinWidth != W) {
                String[] rawLines = cachedBinText.split("\n", -1);
                cachedBinLines = new ArrayList<String>();
                for (int i = 0; i < rawLines.length; i++) {
                    if (binHex) {
                        cachedBinLines.add(rawLines[i]);
                    } else {
                        ArrayList<String> wr = wrap(rawLines[i], W - 2 * margin - dp(6), pMono, dp(11));
                        if (wr.size() == 0) cachedBinLines.add("");
                        else cachedBinLines.addAll(wr);
                    }
                }
                cachedBinWidth = W;
            }
            ArrayList<String> lines = cachedBinLines;
            float yy = y - scrollY;
            float hh = drawLines(c, lines, margin + dp(2), yy, dp(14.5f), dp(binHex ? 10 : 11),
                    binHex ? 0xFF9CCC65 : C_TEXT, pMono, top + dp(40), H);
            return y + hh + dp(20);
        }

        // ---------------------------------------------------- builder
        float drawBuilder(Canvas c, long now) {
            drawHeader(c, "Конструктор чипа", true);
            float margin = dp(12);
            float y = dp(66) - scrollY;
            float top = dp(66);
            // preview
            ChipBin preview = ChipBin.buildCustom(ctType, ctBits,
                    ctType == ChipBin.TYPE_RAM ? 1 : ctRegs, MEM_OPTIONS[ctMemIdx],
                    SCREEN_OPTIONS[ctScreenIdx][0], SCREEN_OPTIONS[ctScreenIdx][1], ctName);
            float size = Math.min(W - dp(160), dp(190));
            Cpu save = cpu;
            cpu = null;
            drawDie(c, preview, margin + dp(8), y + dp(8), size, false, -1, now, false);
            cpu = save;
            float ix = margin + size + dp(26);
            text(c, ctName, ix, y + dp(26), dp(16), C_AMBER, pBold);
            text(c, ChipBin.typeName(ctType), ix, y + dp(46), dp(12.5f), C_CYAN, pBold);
            text(c, ctBits + " бит - MEM:" + MEM_OPTIONS[ctMemIdx], ix, y + dp(64), dp(12.5f), C_TEXT, pMono);
            text(c, "транзисторов: ~" + preview.transistors, ix, y + dp(82), dp(12.5f), C_TEXT, pMono);
            text(c, "размер .bin: " + preview.raw.length + " байт", ix, y + dp(100), dp(12.5f), C_DIM, pMono);
            text(c, "цена сборки: " + constructorCost() + " кр.", ix, y + dp(124), dp(13.5f), C_AMBER, pBold);
            y += size + dp(30);
            // controls
            y = ctRow(c, y, "ТИП ЧИПА", ChipBin.typeName(ctType), B_CT_TYPE, margin);
            y = ctRow(c, y, "РАЗРЯДНОСТЬ", ctBits + " бит (0.." + ((1 << ctBits) - 1) + ")", B_CT_BITS, margin);
            if (ctType != ChipBin.TYPE_RAM) {
                // registers with - value +
                text(c, "РЕГИСТРОВ", margin, y + dp(14), dp(13), C_DIM, pBold);
                float bw = dp(46);
                drawButton(c, B_CT_REGS_M, "-", W - margin - bw * 3 - dp(70), y, bw, dp(38), false, false);
                textC(c, "" + ctRegs, W - margin - bw * 2 - dp(35), y + dp(26), dp(17), C_TEXT, pBold);
                drawButton(c, B_CT_REGS_P, "+", W - margin - bw, y, bw, dp(38), false, false);
                y += dp(50);
            }
            y = ctRow(c, y, "ПАМЯТЬ", MEM_OPTIONS[ctMemIdx] + " ячеек", B_CT_MEM, margin);
            if (ctType != ChipBin.TYPE_RAM) {
                String scr = ctScreenIdx == 0 ? "нет экрана"
                        : SCREEN_OPTIONS[ctScreenIdx][0] + "x" + SCREEN_OPTIONS[ctScreenIdx][1] + " пикселей";
                y = ctRow(c, y, "ЭКРАН", scr, B_CT_SCREEN, margin);
            }
            y = ctRow(c, y, "ИМЯ", ctName, B_CT_NAME, margin);
            y += dp(6);
            drawButton(c, B_CT_MAKE, "СОБРАТЬ ЧИП (.BIN) ЗА " + constructorCost() + " КР.",
                    margin, y, W - 2 * margin, dp(48), true, false);
            y += dp(60);
            ArrayList<String> lines = wrap("Игра честно сгенерирует файл CHIPBIN1: заголовок с цифрами, "
                    + "блоки кристалла с транзисторами и демо-программу под выбранную разрядность. "
                    + "Чип сразу появится в каталоге - его можно смотреть под микроскопом и запускать. "
                    + "Кредиты заканчиваются? Выполняй заказы завода.", W - 2 * margin, p, dp(12.5f));
            y += drawLines(c, lines, margin, y, dp(16), dp(12.5f), C_DIM, p, top, H) + dp(14);
            return y + scrollY;
        }

        float ctRow(Canvas c, float y, String label, String value, int btnId, float margin) {
            text(c, label, margin, y + dp(14), dp(13), C_DIM, pBold);
            float bw = dp(190);
            drawButton(c, btnId, value + "  >", W - margin - bw, y, bw, dp(38), false, false);
            return y + dp(50);
        }

        // ---------------------------------------------------- quests
        float drawQuests(Canvas c, long now) {
            drawHeader(c, "Заказы завода", true);
            float margin = dp(12);
            float y = dp(66);
            text(c, "Звание: " + rankName(), margin, y + dp(8), dp(15), C_AMBER, pBold);
            textR(c, credits + " кредитов", W - margin, y + dp(8), dp(14), C_CYAN, pBold);
            y += dp(18) - scrollY;
            float startY = y;
            for (int i = 0; i < QUEST_NAMES.length; i++) {
                boolean done = (questMask & (1 << i)) != 0;
                float cardH = dp(86);
                roundRect(c, margin, y, W - 2 * margin, cardH, dp(10), done ? 0xFF17301D : C_PANEL);
                strokeRect(c, margin, y, W - 2 * margin, cardH, done ? C_GREEN : C_LINE, dp(1));
                // checkbox drawn
                float cbx = margin + dp(12), cby = y + dp(14), cbs = dp(22);
                strokeRect(c, cbx, cby, cbs, cbs, done ? C_GREEN : C_DIM, dp(2));
                if (done) {
                    p.setColor(C_GREEN);
                    p.setStrokeWidth(dp(2.6f));
                    c.drawLine(cbx + dp(4), cby + cbs * 0.55f, cbx + cbs * 0.42f, cby + cbs - dp(4), p);
                    c.drawLine(cbx + cbs * 0.42f, cby + cbs - dp(4), cbx + cbs - dp(3), cby + dp(4), p);
                    p.setStyle(Paint.Style.FILL);
                }
                float tx = cbx + cbs + dp(12);
                text(c, (i + 1) + ". " + QUEST_NAMES[i], tx, y + dp(22), dp(14.5f), done ? C_GREEN : C_TEXT, pBold);
                textR(c, "+" + QUEST_REWARD[i] + " кр.", W - margin - dp(10), y + dp(22), dp(13), C_AMBER, pBold);
                ArrayList<String> ls = wrap(QUEST_DESC[i], W - tx - margin - dp(6), p, dp(12));
                for (int li = 0; li < ls.size() && li < 3; li++) {
                    text(c, ls.get(li), tx, y + dp(40) + li * dp(15), dp(12), C_DIM, p);
                }
                y += cardH + dp(10);
            }
            return y + scrollY + dp(10);
        }
    }
}
