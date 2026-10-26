package rpc;

public class LifeSim {
    final public static int TIME_MORNING = 0;
    final public static int TIME_DAY = 1;
    final public static int TIME_SUNSET = 2;
    final public static int TIME_NIGHT = 3;
    final public static String[] TIME_NAMES;
    final public static int SEASON_WINTER = 0;
    final public static int SEASON_SPRING = 1;
    final public static int SEASON_SUMMER = 2;
    final public static int SEASON_AUTUMN = 3;
    final public static String[] SEASON_NAMES;
    final public static int WEATHER_CLEAR = 0;
    final public static int WEATHER_RAIN = 1;
    final public static int WEATHER_STORM = 2;
    final public static int WEATHER_SNOW = 3;
    final public static int WEATHER_LEAVES = 4;
    final public static String[] WEATHER_NAMES;
    public int curYear;
    public int curMonth;
    public int curDay;
    public int curHour;
    public int curMinute;
    public int timeOfDay;
    public int season;
    public int weather;
    public int energy;
    public int hunger;
    public int mood;
    public boolean lampOn;
    public int teaLevel;
    public boolean teaHot;
    public int teaSteamTick;
    public int roomDust;
    public int trashCount;
    public int plantWater;
    public int catState;
    public int catHappiness;
    public int catTick;
    public int catX;
    public int catY;
    public boolean radioOn;
    public int radioStation;
    public String[] radioTracks;
    public boolean tvOn;
    public int tvChannel;
    public int animationTick;
    public boolean lightningActive;
    public String lastEventMsg;
    
    static {
        String[] a = new String[4];
        a[0] = "\u0423\u0442\u0440\u043e";
        a[1] = "\u0414\u0435\u043d\u044c";
        a[2] = "\u0417\u0430\u043a\u0430\u0442";
        a[3] = "\u041d\u043e\u0447\u044c";
        TIME_NAMES = a;
        String[] a0 = new String[4];
        a0[0] = "\u0417\u0438\u043c\u0430";
        a0[1] = "\u0412\u0435\u0441\u043d\u0430";
        a0[2] = "\u041b\u0435\u0442\u043e";
        a0[3] = "\u041e\u0441\u0435\u043d\u044c";
        SEASON_NAMES = a0;
        String[] a1 = new String[5];
        a1[0] = "\u042f\u0441\u043d\u043e";
        a1[1] = "\u0414\u043e\u0436\u0434\u044c";
        a1[2] = "\u0413\u0440\u043e\u0437\u0430";
        a1[3] = "\u0421\u043d\u0435\u0433\u043e\u043f\u0430\u0434";
        a1[4] = "\u041b\u0438\u0441\u0442\u043e\u043f\u0430\u0434";
        WEATHER_NAMES = a1;
    }
    
    public LifeSim() {
        this.curYear = 1980;
        this.curMonth = 1;
        this.curDay = 1;
        this.curHour = 10;
        this.curMinute = 0;
        this.timeOfDay = 1;
        this.season = 0;
        this.weather = 0;
        this.energy = 100;
        this.hunger = 20;
        this.mood = 85;
        this.lampOn = true;
        this.teaLevel = 100;
        this.teaHot = true;
        this.teaSteamTick = 0;
        this.roomDust = 15;
        this.trashCount = 2;
        this.plantWater = 80;
        this.catState = 1;
        this.catHappiness = 90;
        this.catTick = 0;
        this.catX = 140;
        this.catY = 160;
        this.radioOn = true;
        this.radioStation = 0;
        String[] a = new String[5];
        a[0] = "Synthwave FM \u2022 \u00abNeon Horizon 1984\u00bb";
        a[1] = "Eurodance 90s \u2022 \u00abRhythm of the Net\u00bb";
        a[2] = "Rock Wave \u2022 \u00abOverclocked Dreams\u00bb";
        a[3] = "Lo-Fi Chiptune \u2022 \u00abLate Night Coding\u00bb";
        a[4] = "\u0420\u0430\u0434\u0438\u043e\u043f\u0435\u0440\u0435\u0434\u0430\u0447\u0430 \u2022 \u041d\u043e\u0432\u043e\u0441\u0442\u0438 \u0442\u0435\u0445\u043d\u043e\u043b\u043e\u0433\u0438\u0439";
        this.radioTracks = a;
        this.tvOn = false;
        this.tvChannel = 0;
        this.animationTick = 0;
        this.lightningActive = false;
        this.lastEventMsg = "\u0417\u0430 \u043e\u043a\u043d\u043e\u043c \u043a\u0440\u0443\u0436\u0438\u0442\u0441\u044f \u043b\u0435\u0433\u043a\u0438\u0439 \u0441\u043d\u0435\u0433. \u0412 \u043a\u043e\u043c\u043d\u0430\u0442\u0435 \u0442\u0435\u043f\u043b\u043e \u0438 \u043f\u0430\u0445\u043d\u0435\u0442 \u0447\u0430\u0435\u043c.";
        this.reset(1980);
    }
    
    public void reset(int i) {
        this.curYear = Math.max(1979, Math.min(2027, i));
        this.curMonth = 1;
        this.curDay = 1;
        this.curHour = 10;
        this.curMinute = 0;
        this.energy = 100;
        this.hunger = 20;
        this.mood = 85;
        this.lampOn = true;
        this.teaLevel = 100;
        this.teaHot = true;
        this.plantWater = 80;
        this.roomDust = 15;
        this.trashCount = 2;
        this.catHappiness = 90;
        this.catState = 1;
        this.radioOn = true;
        this.radioStation = 0;
        this.updateEnvironment();
    }
    
    public void tickTime(int i) {
        this.curMinute = this.curMinute + i;
        while(this.curMinute >= 60) {
            this.curMinute = this.curMinute - 60;
            this.curHour = this.curHour + 1;
            this.onHourPassed();
        }
        while(this.curHour >= 24) {
            this.curHour = this.curHour - 24;
            this.curDay = this.curDay + 1;
            this.onDayPassed();
        }
        this.animationTick = this.animationTick + 1;
        this.updateEnvironment();
        this.updateCat();
    }
    
    private void onHourPassed() {
        this.hunger = Math.min(100, this.hunger + 3);
        if (this.energy > 0) {
            this.energy = Math.max(0, this.energy - 2);
        }
        if (this.teaHot && this.curHour % 2 == 0) {
            this.teaHot = false;
        }
        if (this.curHour % 4 == 0) {
            this.roomDust = Math.min(100, this.roomDust + 1);
        }
        if (this.curHour % 6 == 0) {
            this.plantWater = Math.max(0, this.plantWater - 2);
        }
        if (this.curHour % 8 == 0) {
            this.randomizeWeather();
        }
        if (this.curHour == 21 && this.catState == 0) {
            this.catState = 1;
            this.lastEventMsg = "\u041a\u043e\u0442 \u0411\u0430\u0440\u0441\u0438\u043a \u0443\u044e\u0442\u043d\u043e \u0441\u0432\u0435\u0440\u043d\u0443\u043b\u0441\u044f \u043a\u043b\u0443\u0431\u043e\u0447\u043a\u043e\u043c \u043d\u0430 \u0442\u0435\u043f\u043b\u043e\u043c \u043a\u043e\u0440\u043f\u0443\u0441\u0435 \u041f\u041a.";
        }
    }
    
    private void onDayPassed() {
        int i = 0;
        label2: if (this.curMonth != 2) {
            int i0 = this.curMonth;
            label0: {
                label1: {
                    if (i0 == 4) {
                        break label1;
                    }
                    if (this.curMonth == 6) {
                        break label1;
                    }
                    if (this.curMonth == 9) {
                        break label1;
                    }
                    if (this.curMonth != 11) {
                        break label0;
                    }
                }
                i = 30;
                break label2;
            }
            i = 31;
        } else {
            i = 28;
        }
        if (this.curDay > i) {
            this.curDay = 1;
            this.curMonth = this.curMonth + 1;
            if (this.curMonth > 12) {
                this.curMonth = 1;
                this.curYear = Math.min(2027, this.curYear + 1);
                this.lastEventMsg = new StringBuffer("\u0421 \u041d\u043e\u0432\u044b\u043c ").append(this.curYear).append(" \u0413\u043e\u0434\u043e\u043c! \u041d\u043e\u0432\u0430\u044f \u044d\u043f\u043e\u0445\u0430 \u0432 \u0438\u0441\u0442\u043e\u0440\u0438\u0438 IT!").toString();
                rpc.Sound.ok();
            }
        }
    }
    
    public void updateEnvironment() {
        int i = this.curHour;
        label11: {
            label13: {
                if (i < 6) {
                    break label13;
                }
                if (this.curHour >= 11) {
                    break label13;
                }
                this.timeOfDay = 0;
                break label11;
            }
            int i0 = this.curHour;
            label12: {
                if (i0 < 11) {
                    break label12;
                }
                if (this.curHour >= 18) {
                    break label12;
                }
                this.timeOfDay = 1;
                break label11;
            }
            int i1 = this.curHour;
            label9: {
                label10: {
                    if (i1 < 18) {
                        break label10;
                    }
                    if (this.curHour < 22) {
                        break label9;
                    }
                }
                this.timeOfDay = 3;
                break label11;
            }
            this.timeOfDay = 2;
        }
        int i2 = this.curMonth;
        label5: {
            label7: {
                label8: {
                    if (i2 == 12) {
                        break label8;
                    }
                    if (this.curMonth == 1) {
                        break label8;
                    }
                    if (this.curMonth != 2) {
                        break label7;
                    }
                }
                this.season = 0;
                break label5;
            }
            int i3 = this.curMonth;
            label6: {
                if (i3 < 3) {
                    break label6;
                }
                if (this.curMonth > 5) {
                    break label6;
                }
                this.season = 1;
                break label5;
            }
            int i4 = this.curMonth;
            label3: {
                label4: {
                    if (i4 < 6) {
                        break label4;
                    }
                    if (this.curMonth <= 8) {
                        break label3;
                    }
                }
                this.season = 3;
                break label5;
            }
            this.season = 2;
        }
        if (this.weather != 2) {
            this.lightningActive = false;
        } else {
            boolean b = false;
            int i5 = this.animationTick % 45;
            label2: {
                label0: {
                    label1: {
                        if (i5 == 0) {
                            break label1;
                        }
                        if (this.animationTick % 45 != 2) {
                            break label0;
                        }
                    }
                    b = true;
                    break label2;
                }
                b = false;
            }
            this.lightningActive = b;
        }
    }
    
    public void randomizeWeather() {
        int i = (this.curDay * 13 + this.curHour * 7 + this.curMonth * 31) % 100;
        if (this.season != 0) {
            if (this.season != 3) {
                if (this.season != 2) {
                    this.weather = (i >= 40) ? 0 : 1;
                } else {
                    this.weather = (i >= 25) ? (i >= 50) ? 0 : 1 : 2;
                }
            } else {
                this.weather = (i >= 50) ? (i >= 80) ? 0 : 4 : 1;
            }
        } else {
            this.weather = (i >= 65) ? 0 : 3;
        }
    }
    
    private void updateCat() {
        this.catTick = this.catTick + 1;
        if (this.catTick % 40 == 0) {
            this.catState = this.catTick / 40 % 4;
            if (this.catState == 3) {
                this.lastEventMsg = "\u041a\u043e\u0442 \u0411\u0430\u0440\u0441\u0438\u043a \u043f\u0440\u044b\u0433\u043d\u0443\u043b \u043d\u0430 \u0441\u0442\u043e\u043b \u0438 \u0443\u043b\u0451\u0433\u0441\u044f \u0440\u044f\u0434\u043e\u043c \u0441 \u043a\u043b\u0430\u0432\u0438\u0430\u0442\u0443\u0440\u043e\u0439!";
            }
        }
    }
    
    public String petCat() {
        this.catHappiness = Math.min(100, this.catHappiness + 15);
        this.mood = Math.min(100, this.mood + 10);
        this.catState = 1;
        rpc.Sound.ok();
        this.lastEventMsg = "\u0412\u044b \u043f\u043e\u0433\u043b\u0430\u0434\u0438\u043b\u0438 \u0411\u0430\u0440\u0441\u0438\u043a\u0430. \u041a\u043e\u0442 \u0433\u0440\u043e\u043c\u043a\u043e \u043c\u0443\u0440\u043b\u044b\u0447\u0435\u0442 \u0438 \u0442\u0440\u0451\u0442\u0441\u044f \u043e \u0440\u0443\u043a\u0443.";
        return this.lastEventMsg;
    }
    
    public String drinkTea() {
        if (this.teaLevel <= 0) {
            this.teaLevel = 100;
            this.teaHot = true;
            rpc.Sound.ok();
            this.lastEventMsg = "\u0412\u044b \u0437\u0430\u0432\u0430\u0440\u0438\u043b\u0438 \u0441\u0432\u0435\u0436\u0438\u0439 \u0433\u043e\u0440\u044f\u0447\u0438\u0439 \u0447\u0430\u0439 \u0441 \u043b\u0438\u043c\u043e\u043d\u043e\u043c. \u041f\u043e \u043a\u043e\u043c\u043d\u0430\u0442\u0435 \u043f\u043e\u0448\u0451\u043b \u043f\u0430\u0440.";
            return this.lastEventMsg;
        }
        this.teaLevel = Math.max(0, this.teaLevel - 35);
        this.hunger = Math.max(0, this.hunger - 15);
        this.energy = Math.min(100, this.energy + 12);
        this.mood = Math.min(100, this.mood + 8);
        rpc.Sound.click();
        this.lastEventMsg = "\u0412\u044b \u0441\u0434\u0435\u043b\u0430\u043b\u0438 \u043d\u0435\u0441\u043a\u043e\u043b\u044c\u043a\u043e \u0433\u043b\u043e\u0442\u043a\u043e\u0432 \u0430\u0440\u043e\u043c\u0430\u0442\u043d\u043e\u0433\u043e \u0447\u0430\u044f. \u041f\u0440\u0438\u044f\u0442\u043d\u043e\u0435 \u0442\u0435\u043f\u043b\u043e \u0440\u0430\u0437\u043b\u0438\u043b\u043e\u0441\u044c \u043f\u043e \u0442\u0435\u043b\u0443.";
        return this.lastEventMsg;
    }
    
    public String eatFood() {
        this.hunger = 0;
        this.energy = Math.min(100, this.energy + 25);
        this.mood = Math.min(100, this.mood + 15);
        this.trashCount = this.trashCount + 1;
        rpc.Sound.ok();
        this.lastEventMsg = "\u0412\u044b \u0441\u0432\u0430\u0440\u0438\u043b\u0438 \u0433\u043e\u0440\u044f\u0447\u0438\u0435 \u043f\u0435\u043b\u044c\u043c\u0435\u043d\u0438 \u0441\u043e \u0441\u043c\u0435\u0442\u0430\u043d\u043e\u0439. \u0421\u044b\u0442\u043e\u0441\u0442\u044c \u043d\u0430 \u043c\u0430\u043a\u0441\u0438\u043c\u0443\u043c\u0435!";
        return this.lastEventMsg;
    }
    
    public String sleep() {
        this.tickTime(((this.curHour >= 7) ? 24 - this.curHour + 8 : 8 - this.curHour) * 60);
        this.energy = 100;
        this.hunger = Math.min(100, this.hunger + 20);
        this.mood = Math.min(100, this.mood + 20);
        this.teaHot = false;
        rpc.Sound.ok();
        this.lastEventMsg = "\u0412\u044b \u0441\u043b\u0430\u0434\u043a\u043e \u0432\u044b\u0441\u043f\u0430\u043b\u0438\u0441\u044c \u043f\u043e\u0434 \u0442\u0435\u043f\u043b\u044b\u043c \u043f\u043b\u0435\u0434\u043e\u043c. \u041d\u0430\u0441\u0442\u0443\u043f\u0438\u043b\u043e \u0431\u043e\u0434\u0440\u043e\u0435 \u0443\u0442\u0440\u043e!";
        return this.lastEventMsg;
    }
    
    public String cleanRoom() {
        this.roomDust = 0;
        this.trashCount = 0;
        this.energy = Math.max(0, this.energy - 10);
        this.mood = Math.min(100, this.mood + 15);
        rpc.Sound.ok();
        this.lastEventMsg = "\u0412\u044b \u043f\u0440\u043e\u043f\u044b\u043b\u0435\u0441\u043e\u0441\u0438\u043b\u0438 \u043a\u043e\u0432\u0435\u0440, \u0432\u044b\u043d\u0435\u0441\u043b\u0438 \u0431\u0430\u043d\u043a\u0438 \u0438 \u043f\u0440\u043e\u0442\u0435\u0440\u043b\u0438 \u0441\u0442\u043e\u043b \u043e\u0442 \u043f\u044b\u043b\u0438. \u0427\u0438\u0441\u0442\u043e\u0442\u0430!";
        return this.lastEventMsg;
    }
    
    public String waterPlant() {
        this.plantWater = 100;
        this.mood = Math.min(100, this.mood + 5);
        rpc.Sound.click();
        this.lastEventMsg = "\u0412\u044b \u043f\u043e\u043b\u0438\u043b\u0438 \u0446\u0432\u0435\u0442\u043e\u043a \u043d\u0430 \u043f\u043e\u0434\u043e\u043a\u043e\u043d\u043d\u0438\u043a\u0435. \u0417\u0435\u043b\u0435\u043d\u044b\u0435 \u043b\u0438\u0441\u0442\u044c\u044f \u0440\u0430\u0441\u043f\u0440\u0430\u0432\u0438\u043b\u0438\u0441\u044c.";
        return this.lastEventMsg;
    }
    
    public String toggleLamp() {
        this.lampOn = !this.lampOn;
        rpc.Sound.click();
        this.lastEventMsg = (this.lampOn) ? "\u041d\u0430\u0441\u0442\u043e\u043b\u044c\u043d\u0430\u044f \u043b\u0430\u043c\u043f\u0430 \u0437\u0430\u0436\u0433\u043b\u0430\u0441\u044c \u0442\u0435\u043f\u043b\u044b\u043c \u043b\u0430\u043c\u043f\u043e\u0432\u044b\u043c \u0441\u0432\u0435\u0442\u043e\u043c." : "\u041d\u0430\u0441\u0442\u043e\u043b\u044c\u043d\u0430\u044f \u043b\u0430\u043c\u043f\u0430 \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u0430.";
        return this.lastEventMsg;
    }
    
    public String toggleRadio() {
        this.radioOn = !this.radioOn;
        if (this.radioOn) {
            this.radioStation = (this.radioStation + 1) % this.radioTracks.length;
            rpc.Sound.ok();
            this.lastEventMsg = new StringBuffer("\u0420\u0430\u0434\u0438\u043e \u0432\u043a\u043b\u044e\u0447\u0435\u043d\u043e: ").append(this.radioTracks[this.radioStation]).toString();
        } else {
            rpc.Sound.click();
            this.lastEventMsg = "\u0420\u0430\u0434\u0438\u043e \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u043e.";
        }
        return this.lastEventMsg;
    }
}
