package rpc;

public class Langs {
    final public static int EN = 0;
    final public static int RU = 1;
    final public static int DE = 2;
    final public static int FR = 3;
    final public static int ES = 4;
    final public static int IT = 5;
    final public static int PT = 6;
    final public static int COUNT = 7;
    final public static String[] CODE;
    final public static String[] NAME;
    final public static int[] SINCE;
    final public static int K_MENU = 0;
    final public static int K_PROGS = 1;
    final public static int K_EDITOR = 2;
    final public static int K_NET = 3;
    final public static int K_TASKS = 4;
    final public static int K_WEB = 5;
    final public static int K_GAMES = 6;
    final public static int K_INSTALL = 7;
    final public static int K_BSOD = 8;
    final public static int K_REBOOT = 9;
    final public static int K_FILES = 10;
    final public static int K_HELLO = 11;
    final public static int K_LANG = 12;
    final public static int K_OK = 13;
    final public static int K_LOADING = 14;
    final public static int K_MINE = 15;
    final public static int K_EXIT = 16;
    final public static int K_PROBLEM = 17;
    final public static int K_DUMP = 18;
    final public static int K_REASON = 19;
    final private static String[][] T;
    
    static {
        String[] a = new String[7];
        a[0] = "EN";
        a[1] = "RU";
        a[2] = "DE";
        a[3] = "FR";
        a[4] = "ES";
        a[5] = "IT";
        a[6] = "PT";
        CODE = a;
        String[] a0 = new String[7];
        a0[0] = "English";
        a0[1] = "\u0420\u0443\u0441\u0441\u043a\u0438\u0439";
        a0[2] = "Deutsch";
        a0[3] = "Fran\u00e7ais";
        a0[4] = "Espa\u00f1ol";
        a0[5] = "Italiano";
        a0[6] = "Portugu\u00eas";
        NAME = a0;
        int[] a1 = new int[7];
        a1[0] = 1979;
        a1[1] = 1991;
        a1[2] = 1983;
        a1[3] = 1985;
        a1[4] = 1988;
        a1[5] = 1987;
        a1[6] = 1991;
        SINCE = a1;
        String[][] a2 = new String[7][];
        String[] a3 = new String[20];
        a3[0] = "Menu";
        a3[1] = "Programs";
        a3[2] = "Code editor";
        a3[3] = "Network";
        a3[4] = "Tasks";
        a3[5] = "Internet";
        a3[6] = "Games";
        a3[7] = "Install OS";
        a3[8] = "Fatal error";
        a3[9] = "Press any key to restart";
        a3[10] = "Files";
        a3[11] = "Hello";
        a3[12] = "Language";
        a3[13] = "OK";
        a3[14] = "Loading";
        a3[15] = "Mining";
        a3[16] = "Exit to build";
        a3[17] = "A problem occurred.";
        a3[18] = "Dumping memory ...";
        a3[19] = "Causes: dust, wear, patches.";
        a2[0] = a3;
        String[] a4 = new String[20];
        a4[0] = "\u041c\u0435\u043d\u044e";
        a4[1] = "\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b";
        a4[2] = "\u0420\u0435\u0434\u0430\u043a\u0442\u043e\u0440 \u043a\u043e\u0434\u0430";
        a4[3] = "\u0421\u0435\u0442\u044c";
        a4[4] = "\u0417\u0430\u0434\u0430\u0447\u0438";
        a4[5] = "\u0418\u043d\u0442\u0435\u0440\u043d\u0435\u0442";
        a4[6] = "\u0418\u0433\u0440\u044b";
        a4[7] = "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u041e\u0421";
        a4[8] = "\u041a\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043a\u0430\u044f \u043e\u0448\u0438\u0431\u043a\u0430";
        a4[9] = "\u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u043a\u043b\u0430\u0432\u0438\u0448\u0443";
        a4[10] = "\u0424\u0430\u0439\u043b\u044b";
        a4[11] = "\u041f\u0440\u0438\u0432\u0435\u0442";
        a4[12] = "\u042f\u0437\u044b\u043a";
        a4[13] = "\u041e\u041a";
        a4[14] = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430";
        a4[15] = "\u041c\u0430\u0439\u043d\u0438\u043d\u0433";
        a4[16] = "\u0412\u044b\u0445\u043e\u0434 \u0432 \u0441\u0431\u043e\u0440\u043a\u0443";
        a4[17] = "\u0412\u043e\u0437\u043d\u0438\u043a\u043b\u0430 \u043f\u0440\u043e\u0431\u043b\u0435\u043c\u0430.";
        a4[18] = "\u0421\u0431\u043e\u0440 \u0434\u0430\u043c\u043f\u0430 \u043f\u0430\u043c\u044f\u0442\u0438 ...";
        a4[19] = "\u041f\u0440\u0438\u0447\u0438\u043d\u044b: \u043f\u044b\u043b\u044c, \u0438\u0437\u043d\u043e\u0441, \u0411\u041f.";
        a2[1] = a4;
        String[] a5 = new String[20];
        a5[0] = "Men\u00fc";
        a5[1] = "Programme";
        a5[2] = "Editor";
        a5[3] = "Netzwerk";
        a5[4] = "Aufgaben";
        a5[5] = "Internet";
        a5[6] = "Spiele";
        a5[7] = "System installieren";
        a5[8] = "Schwerer Fehler";
        a5[9] = "Taste dr\u00fccken";
        a5[10] = "Dateien";
        a5[11] = "Hallo";
        a5[12] = "Sprache";
        a5[13] = "OK";
        a5[14] = "L\u00e4dt";
        a5[15] = "Mining";
        a5[16] = "Zur\u00fcck zum Bau";
        a5[17] = "Ein Problem ist aufgetreten.";
        a5[18] = "Speicherabbild ...";
        a5[19] = "Ursachen: Staub, Verschlei\u00df.";
        a2[2] = a5;
        String[] a6 = new String[20];
        a6[0] = "Menu";
        a6[1] = "Programmes";
        a6[2] = "\u00c9diteur";
        a6[3] = "R\u00e9seau";
        a6[4] = "T\u00e2ches";
        a6[5] = "Internet";
        a6[6] = "Jeux";
        a6[7] = "Installer l'OS";
        a6[8] = "Erreur fatale";
        a6[9] = "Appuyez sur une touche";
        a6[10] = "Fichiers";
        a6[11] = "Bonjour";
        a6[12] = "Langue";
        a6[13] = "OK";
        a6[14] = "Chargement";
        a6[15] = "Minage";
        a6[16] = "Quitter";
        a6[17] = "Un probl\u00e8me est survenu.";
        a6[18] = "Vidage m\u00e9moire ...";
        a6[19] = "Causes : poussi\u00e8re, usure.";
        a2[3] = a6;
        String[] a7 = new String[20];
        a7[0] = "Men\u00fa";
        a7[1] = "Programas";
        a7[2] = "Editor";
        a7[3] = "Red";
        a7[4] = "Tareas";
        a7[5] = "Internet";
        a7[6] = "Juegos";
        a7[7] = "Instalar SO";
        a7[8] = "Error grave";
        a7[9] = "Pulse una tecla";
        a7[10] = "Archivos";
        a7[11] = "Hola";
        a7[12] = "Idioma";
        a7[13] = "OK";
        a7[14] = "Cargando";
        a7[15] = "Miner\u00eda";
        a7[16] = "Salir";
        a7[17] = "Ha ocurrido un problema.";
        a7[18] = "Volcando memoria ...";
        a7[19] = "Causas: polvo, desgaste.";
        a2[4] = a7;
        String[] a8 = new String[20];
        a8[0] = "Men\u00f9";
        a8[1] = "Programmi";
        a8[2] = "Editor";
        a8[3] = "Rete";
        a8[4] = "Attivit\u00e0";
        a8[5] = "Internet";
        a8[6] = "Giochi";
        a8[7] = "Installa SO";
        a8[8] = "Errore grave";
        a8[9] = "Premere un tasto";
        a8[10] = "File";
        a8[11] = "Ciao";
        a8[12] = "Lingua";
        a8[13] = "OK";
        a8[14] = "Caricamento";
        a8[15] = "Mining";
        a8[16] = "Esci";
        a8[17] = "Si \u00e8 verificato un problema.";
        a8[18] = "Scarico memoria ...";
        a8[19] = "Cause: polvere, usura.";
        a2[5] = a8;
        String[] a9 = new String[20];
        a9[0] = "Menu";
        a9[1] = "Programas";
        a9[2] = "Editor";
        a9[3] = "Rede";
        a9[4] = "Tarefas";
        a9[5] = "Internet";
        a9[6] = "Jogos";
        a9[7] = "Instalar SO";
        a9[8] = "Erro grave";
        a9[9] = "Pressione uma tecla";
        a9[10] = "Arquivos";
        a9[11] = "Ol\u00e1";
        a9[12] = "Idioma";
        a9[13] = "OK";
        a9[14] = "Carregando";
        a9[15] = "Minera\u00e7\u00e3o";
        a9[16] = "Sair";
        a9[17] = "Ocorreu um problema.";
        a9[18] = "Despejando mem\u00f3ria ...";
        a9[19] = "Causas: poeira, desgaste.";
        a2[6] = a9;
        T = a2;
    }
    
    public Langs() {
    }
    
    public static String t(int i, int i0) {
        label0: {
            label1: {
                if (i < 0) {
                    break label1;
                }
                if (i < 7) {
                    break label0;
                }
            }
            i = 0;
        }
        if (i0 >= 0 && i0 < T[i].length) {
            return T[i][i0];
        }
        return "";
    }
    
    public static boolean available(int i, int i0) {
        if (i >= 0 && i < 7) {
            if (i0 < SINCE[i]) {
                return false;
            }
            return true;
        }
        return false;
    }
    
    public static int count(int i) {
        int i0 = 0;
        int i1 = 0;
        while(i1 < 7) {
            if (rpc.Langs.available(i1, i)) {
                i0 = i0 + 1;
            }
            i1 = i1 + 1;
        }
        return i0;
    }
    
    public static int nth(int i, int i0) {
        int i1 = 0;
        int i2 = 0;
        while(i2 < 7) {
            if (rpc.Langs.available(i2, i)) {
                if (i1 == i0) {
                    return i2;
                }
                i1 = i1 + 1;
            }
            i2 = i2 + 1;
        }
        return 0;
    }
    
    public static int installPenalty(int i) {
        if (i == 0) {
            return 0;
        }
        if (i != 1) {
            return 8;
        }
        return 12;
    }
    
    public static boolean cyrillic(int i) {
        if (i != 1) {
            return false;
        }
        return true;
    }
}
