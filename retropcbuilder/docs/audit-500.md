# Audit of the 500 features against the sources

Sources scanned: 83 java files, `\uXXXX` escapes decoded,
words folded Cyrillic -> Latin so that Russian strings and
English identifiers both match.

**358 of 500 items have a textual match in the code**, 142 items
have no keyword anywhere in the sources (they are candidates
for implementation, or they live behind a generic entry).

--   1. Несовпадение крепежа (винты M3 и саморезы).
OK   2. Залипание сокета (гнутые ножки, выпрямлять пинцетом).      hits=12
        src/rpc/CalendarHistory.java:62  a0[2] = "пинцет-ювелир • выпрямить 100 погнутых ножек сокета без отлома";
        src/rpc/City.java:36  a0[1] = "бесконечные павильоны с чипами, сокетами, переходниками и мануалами.";
OK   3. Длина кабелей (короткий кабель питания CPU).               hits=24
        src/rpc/Browser.java:463  s1 = "блоки питания по новому стандарту";
        src/rpc/Compat.java:51  return "at-блок питания не встанет в atx-корпус";
OK   4. Перепутанные контакты Power/Reset.                         hits=64
        src/rpc/Browser.java:375  a10[6] = "контакты окислились —";
        src/rpc/BuildView.java:69  * @param pcon     true when the machine is assembled and powered
OK   5. Термопаста-изолятор (много/мало — перегрев).               hits=15
        src/rpc/Browser.java:221  a1[7] = "   будет перегрев и bsod.";
        src/rpc/Browser.java:465  s = "многоядерность для всех";
--   6. Статическое электричество (собирать на ковре опасно).
OK   7. Пыль как ресурс (забивает кулеры).                         hits=18
        src/rpc/Browser.java:207  a0[6] = "4. пыль и ёмкости: опыт ремонта";
        src/rpc/Browser.java:634  a6[8] = "и tdp. и продувайте пыль.";
OK   8. Кривой запуск кулера (3-pin в 4-pin).                      hits=28
        src/rpc/Browser.java:249  a3[7] = "забыл снять плёнку с кулера»";
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
OK   9. Звук парковки головок HDD.                                 hits=17
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
OK  10. Вздутие конденсаторов от времени.                          hits=4
        src/rpc/CalendarHistory.java:48  a[38] = "2018 • аппаратная трассировка лучей в реальном времени (nvidia rtx 2080 ray tracing).";
        src/rpc/CalendarHistory.java:63  a0[3] = "паяльный спец • заменить 50 вздутых low-esr конденсаторов";
OK  11. Синий экран смерти (BSOD) с таймером.                      hits=31
        src/rpc/AudioEngine.java:197  public void playbsod() {
        src/rpc/Browser.java:221  a1[7] = "   будет перегрев и bsod.";
OK  12. Лицензионные ключи.                                        hits=22
        src/rpc/Browser.java:550  a3[6] = "форм-факторы и ключи.";
        src/rpc/Funcs.java:106  "прикрутить материнскую плату", "подключить 24-pin",
OK  13. Поиск драйверов.                                           hits=10
        src/rpc/Browser.java:30  a[9] = "поиск";
        src/rpc/Browser.java:701  a6[0] = "поиск по сети";
OK  14. Установка Windows 95 с 20 дискет.                          hits=150
        src/rpc/AudioEngine.java:142  this.playfloppyseek();
        src/rpc/AudioEngine.java:163  private void playfloppyseek() {
--  15. Кракозябры в DOS.
OK  16. Обрыв Dial-up.                                             hits=4
        src/rpc/Funcs.java:268  "подключить dial-up модем", "перевести модем на тон",
        src/rpc/Funcs.java:281  "побороть обрыв связи", "починить витую пару",
--  17. Дефрагментация HDD.
~   18. Чистка реестра.                                            hits=1
        src/rpc/Funcs.java:183  "ультразвуковая чистка платы"
--  19. Конфликт IRQ.
OK  20. Сброс пароля BIOS через батарейку CMOS.                    hits=63
        src/rpc/Browser.java:217  a1[3] = "2. проверить обороты (bios).";
        src/rpc/BuildView.java:383  default: return "bios";
OK  21. Защита от пиратства (требует диск).                        hits=76
        src/rpc/AudioEngine.java:133  public void playdiskaccess(int i, int i0, int i1, boolean b) {
        src/rpc/Browser.java:467  s1 = "nvme: диски быстрее шины";
~   22. No-CD кряк.                                                hits=1
        src/rpc/City.java:134  a3[2] = "3. «поставить no-cd патч на игры клуба»";
OK  23. Нехватка места на диске.                                   hits=13
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/ExpStress.java:176  this.addlog(new stringbuffer("bsod: нехватка системных ресурсов! требуются твики (маска: 0x").append(integer.t…
--  24. Тормоза в 3D (низкий FPS).
OK  25. Залипание шариковой мыши.                                  hits=3
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/Funcs.java:167  "пропаять джек наушников", "заменить кнопки мыши",
~   26. Битая дискета с сейвом.                                    hits=1
        src/rpc/SoftSim.java:72  this.floppystatus = new stringbuffer("✓ дискета #").append(this.floppycurrent).append(" прочитана! вставьте ди…
OK  27. Doom на калькуляторе.                                      hits=31
        src/rpc/AudioEngine.java:201  public void playdoomriff() {
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
OK  28. Сгоревший пиксель на мониторе.                             hits=2
        src/rpc/Exp.java:179  a11[7] = "half-life 2 (directx 7 fallback)|принудительный запуск source engine в режиме mat_dxlevel 70 без пик…
        src/rpc/Funcs.java:165  "заменить лампу в мониторе", "настроить фокус монитора",
~   29. Скачивание патча на 56k модеме.                            hits=1
        src/rpc/Game.java:7971  this.toastmsg(new stringbuffer("симка в модеме: до ").append(rpc.game.mobilespeed(i)).append(" мбит/с (").appe…
OK  30. Установка эмулятора Dendy.                                 hits=13
        src/rpc/BuildView.java:404  case 3: return "установка системы";
        src/rpc/Exp.java:165  a10[7] = "haiku os r1 beta (beos successor)|установка молниеносной многопоточной ос beos нового поколения";
OK  31. Обман на барахолке.                                        hits=2
        src/rpc/Funcs.java:187  "торг на барахолке", "сторговать десять процентов",
        src/rpc/Funcs.java:188  "сторговать треть", "заметить обман продавца",
OK  32. Торг с продавцом.                                          hits=13
        src/rpc/Browser.java:190  return "торг, б/у, риск «не то на фото»";
        src/rpc/Browser.java:738  a3[1] = (i1 != 6) ? (i1 != 7) ? "новое, с гарантией" : "дешево и из китая" : "б/у железо, торг у капота";
--  33. Инфляция.
OK  34. Курс валют.                                                hits=7
        src/rpc/Browser.java:356  a9[0] = "музей: экскурсия";
        src/rpc/Browser.java:392  a0[6] = "курс кредита: стабилен";
~   35. Кража на почте.                                            hits=1
        src/rpc/City.java:102  a0[0] = "1. «вскрыть коробку прямо на почте (проверка на кирпич)»";
OK  36. Гарантия в магазине.                                       hits=4
        src/rpc/Browser.java:193  return "новая, гарантия";
        src/rpc/Browser.java:636  a6[10] = "гарантия: 12 месяцев";
--  37. Распродажи (Черная пятница).
~   38. Скупка лома.                                               hits=1
        src/rpc/CalendarHistory.java:62  a0[2] = "пинцет-ювелир • выпрямить 100 погнутых ножек сокета без отлома";
OK  39. Кредит в банке.                                            hits=17
        src/rpc/Browser.java:392  a0[6] = "курс кредита: стабилен";
        src/rpc/Funcs.java:198  "взять кредит", "погасить кредит",
OK  40. Майнинг-ферма.                                             hits=2
        src/rpc/Funcs.java:258  "майнинг-ферма", "nas из старого пк",
        src/rpc/Market.java:27  a[6] = "майнинг-ферма";
--  41. Найм сотрудников.
~   42. Утечка данных.                                             hits=1
        src/rpc/Browser.java:541  a2[5] = "долговременное хранение данных:";
--  43. Пиратство (торренты).
OK  44. Реклама на BBS.                                            hits=3
        src/rpc/Browser.java:393  a0[7] = "реклама: собирай правильно";
        src/rpc/Game.java:9244  a[8] = new stringbuffer("реклама: уровень ").append(this.corp.marketing).tostring();
--  45. Баг-репорты.
OK  46. Хейтеры в интернете.                                       hits=2
        src/rpc/Game.java:7848  this.toastmsg("нужен патч-обход: скачайте в интернете");
        src/rpc/Game.java:7989  this.toastmsg("сначала скачайте doom в интернете");
OK  47. Антивирус находит вирус.                                   hits=3
        src/rpc/Funcs.java:141  "удалить вирус", "поставить антивирус",
        src/rpc/Funcs.java:365  "найти дискету с вирусом", "найти cd с демо",
--  48. Слияние компаний.
~   49. Роялти за ОС.                                              hits=1
        src/rpc/Game.java:9241  a[5] = "начать день (+роялти)";
--  50. Кража серверов.
OK  51. Светодиоды на корпусе.                                     hits=2
        src/rpc/Funcs.java:248  "собрать пк в корпусе тостера", "собрать пк в корпусе nes",
        src/rpc/LifeSim.java:246  this.lasteventmsg = "кот барсик уютно свернулся клубочком на теплом корпусе пк.";
OK  52. Звук открытия CD-ROM.                                      hits=18
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/Browser.java:492  s1 = "cd-rom: 150 кб/с хватает всем";
OK  53. Перекрученный кабель мыши.                                 hits=8
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/Funcs.java:107  "подключить 4-pin cpu", "подключить sata кабель",
--  54. Гул трансформатора ЭЛТ.
OK  55. Коврик для мыши.                                           hits=4
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/Funcs.java:167  "пропаять джек наушников", "заменить кнопки мыши",
OK  56. Кнопка Turbo на корпусе.                                   hits=7
        src/rpc/Exp.java:115  a6[4] = "msi wind u100 (turbo oc)|intel atom n270 1.6ghz + кнопка turbo|1gb ddr2|windows xp home";
        src/rpc/Exp.java:151  a8[12] = "серверный xeon e5-2678 v3|intel xeon e5-2678 v3 12c/24t turbo unlock + gtx 1080 ti|32gb ddr3/ddr4 ec…
OK  57. Блокировка Power (держать 5 сек).                          hits=45
        src/rpc/BuildView.java:69  * @param pcon     true when the machine is assembled and powered
        src/rpc/BuildView.java:295  // powered off, but the glass still reflects the room
OK  58. Время суток за окном.                                      hits=4
        src/rpc/BuildView.java:405  case 4: return "промотать время";
        src/rpc/Funcs.java:151  "включить скринсейвер с рыбами", "настроить дату и время",
OK  59. Кот в комнате.                                             hits=3
        src/rpc/Game.java:4144  ? "ночь. в комнате тихо."
        src/rpc/LifeSim.java:111  this.lasteventmsg = "за окном кружится легкий снег. в комнате тепло и пахнет чаем.";
OK  60. Мусор на столе.                                            hits=2
        src/rpc/City.java:112  a1[0] = "1. «искать рабочие процессоры и видеокарты среди мусора»";
        src/rpc/Market.java:24  a[3] = "мусорка";
OK  61. Осциллограф.                                               hits=2
        src/rpc/Funcs.java:173  "заменить вздутые банки", "проверить осциллографом",
        src/rpc/Funcs.java:328  "купить осциллограф", "поговорить с продавцом",
OK  62. Тестер ёмкости.                                            hits=8
        src/rpc/Browser.java:207  a0[6] = "4. пыль и ёмкости: опыт ремонта";
        src/rpc/Diag.java:41  return "2 коротких — ёмкости";
OK  63. Перепайка ножек процессора.                                hits=15
        src/rpc/Browser.java:542  a2[6] = "разъём для установки процессора:";
        src/rpc/Browser.java:710  a6[9] = new stringbuffer("«какой сокет у ").append((a1.length <= 0) ? "процессора" : a.get(a1[0]).name).append…
OK  64. Залипание кнопки Power.                                    hits=46
        src/rpc/BuildView.java:69  * @param pcon     true when the machine is assembled and powered
        src/rpc/BuildView.java:295  // powered off, but the glass still reflects the room
OK  65. Замена предохранителя БП.                                  hits=3
        src/rpc/Exp.java:168  a10[10] = "reactos 0.4.15 (open-source nt)|открытая замена windows nt с нативным запуском win32 софта";
        src/rpc/Exp.java:177  a11[5] = "starfield (creation engine 2)|удаление объемного тумана и замена текстур планет на 128x128 пикселей"…
OK  66. Высохшая термопаста.                                       hits=5
        src/rpc/Funcs.java:251  "термопаста из зубной пасты", "майонез вместо пасты",
        src/rpc/Game.java:5985  "пайка, термопаста, пыль, конденсаторы",
OK  67. Шум подшипника кулера.                                     hits=6
        src/rpc/Browser.java:249  a3[7] = "забыл снять плёнку с кулера»";
        src/rpc/City.java:37  a0[2] = "ряды ящиков с б/у платами, старыми кулерами и котами в мешке.";
OK  68. Вздутие батарейки CMOS.                                    hits=22
        src/rpc/Diag.java:13  final public static int f_cmos = 9;
        src/rpc/Diag.java:46  return "1 длинный + 3 — батарейка cmos";
OK  69. Скрученные контакты PCIe.                                  hits=26
        src/rpc/Browser.java:375  a10[6] = "контакты окислились —";
        src/rpc/Browser.java:462  s0 = "ddr5 дешевеет, pcie 5.0 приходит";
--  70. Битые сектора HDD.
OK  71. Занятая телефонная линия.                                  hits=2
        src/rpc/Game.java:6814  return "adsl / выделенная линия";
        src/rpc/WorkshopTools.java:86  this.toolfeedback = "линия +12v просажена до 10.85v, пульсации 140мв. бп требует ремонта.";
OK  72. Помехи в линии.                                            hits=2
        src/rpc/Game.java:7481  a[2] = new stringbuffer("тип линии: ").append(this.netkind()).tostring();
        src/rpc/WorkshopTools.java:121  this.toolfeedback = new stringbuffer("✓ линии питания в норме: +12v (").append(this.v12).append("v), +5v (").a…
OK  73. Настройка локальной сети.                                  hits=14
        src/rpc/Browser.java:351  a8[8] = "посетителей сегодня: 12";
        src/rpc/Browser.java:701  a6[0] = "поиск по сети";
--  74. Хаб vs Коммутатор.
--  75. Кривой патч-корд.
OK  76. FTP-сервер на старом ПК.                                   hits=2
        src/rpc/Funcs.java:275  "поднять ftp-сервер", "поднять http-сервер",
        src/rpc/Inst.java:319  a[i2] = rpc.inst.dl("патч обхода cpu", 1500, 2015, 2, 1, "10/11 на старом камне");
OK  77. Докачка файлов.                                            hits=6
        src/rpc/Game.java:8423  a[0] = new stringbuffer("этап: ").append((i0 >= 20) ? (i0 >= 60) ? (i0 >= 90) ? "первая загрузка" : "установка…
        src/rpc/Shell.java:428  this.println(new stringbuffer("файлов в системе: ").append(this.files()).tostring());
OK  78. Вирус из сети.                                             hits=15
        src/rpc/Browser.java:351  a8[8] = "посетителей сегодня: 12";
        src/rpc/Browser.java:701  a6[0] = "поиск по сети";
OK  79. Провайдер отключил интернет.                               hits=46
        src/rpc/BuildView.java:45  case 17: return new int[] { 48, 70, 26, 10 };    /* network */
        src/rpc/BuildView.java:194  // sound / network / riser
~   80. Подключение к BBS.                                         hits=1
        src/rpc/Exp.java:66  a2[7] = "внешнее подключение шины через expresscard / sdio / m.2";
OK  81. Подсветка кулера.                                          hits=7
        src/rpc/Browser.java:249  a3[7] = "забыл снять плёнку с кулера»";
        src/rpc/City.java:37  a0[2] = "ряды ящиков с б/у платами, старыми кулерами и котами в мешке.";
~   82. Покраска корпуса.                                          hits=1
        src/rpc/Funcs.java:113  "поставить вентилятор на выдув", "наклеить ножки корпуса",
OK  83. Вырезание окна в корпусе.                                  hits=2
        src/rpc/Funcs.java:248  "собрать пк в корпусе тостера", "собрать пк в корпусе nes",
        src/rpc/LifeSim.java:246  this.lasteventmsg = "кот барсик уютно свернулся клубочком на теплом корпусе пк.";
~   84. Снятие антибликовой пленки.                                hits=1
        src/rpc/Exp.java:188  a12[2] = "скальпирование (delid) и жидкий металл|снятие теплораспределительной крышки ihs, нанесение gallium l…
OK  85. Тефлоновые ножки для мыши.                                 hits=8
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/Funcs.java:113  "поставить вентилятор на выдув", "наклеить ножки корпуса",
OK  86. Смазка свитчей клавиатуры.                                 hits=3
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/Exp.java:200  a13[2] = "своя ос реального времени на c и ассемблере|написание mbr загрузчика, переход в защищенный 32-бит ре…
--  87. Кабель-менеджмент.
--  88. Наклейки из журналов.
--  89. Подписывание кнопок маркером.
OK  90. Самодельный кулер из бутылки.                              hits=28
        src/rpc/Browser.java:205  a0[4] = "2. кулер гудит — что делать";
        src/rpc/Browser.java:214  a1[0] = "тема: кулер гудит";
OK  91. Пролитый кофе на клавиатуру.                               hits=8
        src/rpc/City.java:39  a0[4] = "уютный столик, горячий кофе, сплетни о новинках it и заказы от клиентов.";
        src/rpc/City.java:120  this.dialoguetext = "«ваш кофе готов! а вот тот солидный мужчина ищет мастера для апгрейда пк бухгалтерии.»";
OK  92. Выдернутый шнур питания.                                   hits=17
        src/rpc/Browser.java:463  s1 = "блоки питания по новому стандарту";
        src/rpc/Compat.java:51  return "at-блок питания не встанет в atx-корпус";
~   93. Кот на клавиатуре (Alt+F4).                                hits=1
        src/rpc/Funcs.java:311  "кот охотится на курсор", "кот на клавиатуре",
OK  94. Забитый пылью радиатор.                                    hits=12
        src/rpc/Browser.java:216  a1[2] = "1. продуть радиатор от пыли.";
        src/rpc/City.java:150  this.dialoguetext = new stringbuffer("«привет, электронщик! для ").append(i0).append(" года у меня есть отборн…
OK  95. Мусор в CD-ROM.                                            hits=3
        src/rpc/Browser.java:492  s1 = "cd-rom: 150 кб/с хватает всем";
        src/rpc/City.java:112  a1[0] = "1. «искать рабочие процессоры и видеокарты среди мусора»";
~   96. Сгоревший USB-порт.                                        hits=1
        src/rpc/Funcs.java:175  "починить usb-порт", "перепаять hdmi",
OK  97. Слетевшая Windows.                                         hits=99
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
--  98. Удаленный ярлык.
OK  99. Забытый пароль BIOS.                                       hits=32
        src/rpc/Browser.java:217  a1[3] = "2. проверить обороты (bios).";
        src/rpc/BuildView.java:383  default: return "bios";
~  100. Сосед с перфоратором.                                      hits=1
        src/rpc/City.java:124  a2[2] = "3. «послушать сплетни за соседним столиком»";
OK 101. Секретные коды в BIOS.                                     hits=32
        src/rpc/Browser.java:217  a1[3] = "2. проверить обороты (bios).";
        src/rpc/BuildView.java:383  default: return "bios";
OK 102. Doom в 8K.                                                 hits=30
        src/rpc/AudioEngine.java:201  public void playdoomriff() {
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
OK 103. Код 5318008 на калькуляторе.                               hits=3
        src/rpc/CalendarHistory.java:65  a0[5] = "doom на всем • запустить doom на кпк, нетбуке и калькуляторе";
        src/rpc/Funcs.java:241  "linux на калькуляторе", "dos на телефоне",
OK 104. Секретный магазин на свалке.                               hits=5
        src/rpc/Browser.java:730  a2[6] = new stringbuffer("цена: ").append(i3).append(" (в магазине ").append(a1.price).append(")").tostring();
        src/rpc/Funcs.java:211  "собрать пк под заказ дорого", "открыть свой магазин"
OK 105. Фирменная мелодия Sound Blaster.                           hits=179
        src/rpc/AudioEngine.java:5  private boolean soundenabled;
        src/rpc/AudioEngine.java:20  this.soundenabled = true;
-- 106. Сгорание ПК от молнии.
-- 107. Динозаврик (404).
OK 108. Замедление через Turbo.                                    hits=15
        src/rpc/Exp.java:66  a2[7] = "внешнее подключение шины через expresscard / sdio / m.2";
        src/rpc/Exp.java:115  a6[4] = "msi wind u100 (turbo oc)|intel atom n270 1.6ghz + кнопка turbo|1gb ddr2|windows xp home";
OK 109. Скрытая ОС (Linux).                                        hits=15
        src/rpc/CalendarHistory.java:21  a[11] = "1991 • линус торвальдс публикует первую версию ядра linux 0.01. распад ссср.";
        src/rpc/Corp.java:354  return (i0 >= 2000) ? "microsoft / linux" : "microsoft / ibm";
OK 110. Разгон до сгорания.                                        hits=15
        src/rpc/Browser.java:206  a0[5] = "3. разгон: стоит ли оно того";
        src/rpc/Browser.java:228  a2[0] = "тема: разгон";
OK 111. Дождь, снег, листья за окном.                              hits=4
        src/rpc/LifeSim.java:67  a1[1] = "дождь";
        src/rpc/LifeSim.java:69  a1[3] = "снегопад";
OK 112. Свет из окна на стол.                                      hits=20
        src/rpc/Browser.java:733  a2[9] = "3 — купить и на стол";
        src/rpc/BuildView.java:379  case 2: return "стол";
OK 113. Ламповый свет.                                             hits=4
        src/rpc/Funcs.java:164  "починить шлейф матрицы", "заменить подсветку",
        src/rpc/Funcs.java:253  "подсветка из гирлянды", "покрасить корпус",
OK 114. Радиоприёмник в комнате.                                   hits=4
        src/rpc/Funcs.java:363  "найти кассетный плеер", "найти радиоприёмник",
        src/rpc/Game.java:4144  ? "ночь. в комнате тихо."
OK 115. Звук дождя по подоконнику.                                 hits=17
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
~  116. Плед и кружка с паром.                                     hits=1
        src/rpc/LifeSim.java:464  this.lasteventmsg = "вы сладко выспались под теплым пледом. наступило бодрое утро!";
-- 117. Шум холодильника.
-- 118. Разговоры за стеной.
~  119. Осенние листья.                                            hits=1
        src/rpc/LifeSim.java:482  this.lasteventmsg = "вы полили цветок на подоконнике. зеленые листья расправились.";
-- 120. Морозные узоры зимой.
OK 121. Выход на улицу (магазин, рынок).                           hits=27
        src/rpc/Browser.java:412  a2[0] = "рынок";
        src/rpc/Browser.java:730  a2[6] = new stringbuffer("цена: ").append(i3).append(" (в магазине ").append(a1.price).append(")").tostring();
OK 122. Встреча с продавцом (скидка).                              hits=2
        src/rpc/CalendarHistory.java:57  a[47] = "2027 • встреча нового 2027 года! великий финал эпохи и вечная слава мастеру!";
        src/rpc/Funcs.java:328  "купить осциллограф", "поговорить с продавцом",
OK 123. Сосед просит починить ПК.                                  hits=19
        src/rpc/City.java:124  a2[2] = "3. «послушать сплетни за соседним столиком»";
        src/rpc/Funcs.java:153  "починить синий экран", "переустановить систему",
OK 124. Погода влияет на рынок.                                    hits=75
        src/rpc/AudioEngine.java:205  public void playrainnoise(int i) {
        src/rpc/Browser.java:412  a2[0] = "рынок";
OK 125. Дорога на рынок (время).                                   hits=15
        src/rpc/Browser.java:412  a2[0] = "рынок";
        src/rpc/BuildView.java:377  case 0: return "базар";
-- 126. Поездка на автобусе.
OK 127. Кафе на углу.                                              hits=4
        src/rpc/City.java:29  a[4] = "☕ кафе на углу";
        src/rpc/Funcs.java:332  "выиграть конкурс демо", "сходить в кафе",
OK 128. Встреча с другом (пиратский диск).                         hits=74
        src/rpc/AudioEngine.java:133  public void playdiskaccess(int i, int i0, int i1, boolean b) {
        src/rpc/Browser.java:467  s1 = "nvme: диски быстрее шины";
~  129. Находка выброшенного ПК.                                   hits=1
        src/rpc/City.java:182  return "находка! вы откопали винтажную материнскую плату и кулер с медным сердечником!";
-- 130. Ночная прогулка.
OK 131. Реальный календарь (47 лет).                               hits=2
        src/rpc/Game.java:477  a[6] = "календарь 1980-2027 & достижения";
        src/rpc/Game.java:488  t[6] = "календарь";
OK 132. Ускорение времени в меню.                                  hits=8
        src/rpc/CalendarHistory.java:48  a[38] = "2018 • аппаратная трассировка лучей в реальном времени (nvidia rtx 2080 ray tracing).";
        src/rpc/Exp.java:200  a13[2] = "своя ос реального времени на c и ассемблере|написание mbr загрузчика, переход в защищенный 32-бит ре…
OK 133. Сезоны (влияют на комнату и цены).                         hits=5
        src/rpc/Browser.java:416  a2[4] = "цены на память:";
        src/rpc/Browser.java:741  a3[4] = new stringbuffer("цены: ").append(rpc.browser.pricenote(i1)).tostring();
OK 134. Праздники (Новый год, 8 марта, 23 февраля).                hits=4
        src/rpc/City.java:188  return "☕ вы взяли новый заказ на ремонт и выпили отличный кофе!";
        src/rpc/LifeSim.java:157  return "23 февраля";
OK 135. Финал: Новый 2027 год.                                     hits=535
        src/rpc/Assets.java:4  final private static int[] brand;
        src/rpc/Assets.java:10  final private static int cache = 24;
OK 136. День рождения героя.                                       hits=50
        src/rpc/AudioEngine.java:5  private boolean soundenabled;
        src/rpc/AudioEngine.java:20  this.soundenabled = true;
-- 137. Отпуск.
OK 138. Заказы на праздники.                                       hits=8
        src/rpc/BuildView.java:390  case 1: return "доставка: заказы";
        src/rpc/City.java:39  a0[4] = "уютный столик, горячий кофе, сплетни о новинках it и заказы от клиентов.";
OK 139. Выход на пенсию (к 2026).                                  hits=10
        src/rpc/CalendarHistory.java:12  a[2] = "1982 • выход культового домашнего компьютера commodore 64 и журнала «радио» с рк-86.";
        src/rpc/CalendarHistory.java:39  a[29] = "2009 • выход популярнейшей операционной системы windows 7 и майнинг первого блока bitcoin.";
OK 140. Итоги года.                                                hits=21
        src/rpc/Browser.java:517  a0[2] = "статьи по годам:";
        src/rpc/Browser.java:599  a4[1] = new stringbuffer("каталог ").append(i0).append(" года").tostring();
-- 141. Диалоги с покупателями.
OK 142. Торг.                                                      hits=12
        src/rpc/Browser.java:190  return "торг, б/у, риск «не то на фото»";
        src/rpc/Browser.java:738  a3[1] = (i1 != 6) ? (i1 != 7) ? "новое, с гарантией" : "дешево и из китая" : "б/у железо, торг у капота";
OK 143. Сплетни на рынке (новинки следующего года).                hits=25
        src/rpc/Browser.java:517  a0[2] = "статьи по годам:";
        src/rpc/Browser.java:599  a4[1] = new stringbuffer("каталог ").append(i0).append(" года").tostring();
OK 144. Заказ по телефону.                                         hits=27
        src/rpc/BuildView.java:390  case 1: return "доставка: заказы";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
OK 145. Переписка на Avito.                                        hits=9
        src/rpc/Browser.java:10  final public static int s_avito = 6;
        src/rpc/Browser.java:27  a[6] = "avito";
OK 146. Отзывы в интернете.                                        hits=2
        src/rpc/Game.java:7848  this.toastmsg("нужен патч-обход: скачайте в интернете");
        src/rpc/Game.java:7989  this.toastmsg("сначала скачайте doom в интернете");
-- 147. Друзья в BBS/ICQ.
-- 148. Учитель информатики в 80-х.
OK 149. Конкурент через дорогу.                                    hits=11
        src/rpc/Exp.java:66  a2[7] = "внешнее подключение шины через expresscard / sdio / m.2";
        src/rpc/Exp.java:175  a11[3] = "doom eternal (vulkan api)|эмуляция инструкций vulkan через vkd3d и direct3d 11 транслятор";
OK 150. Истории у костра на свалке.                                hits=2
        src/rpc/Game.java:7039  this.g.drawtext("или поищите на свалке.", 8, i0 + 24 + this.fh0, 0);
        src/rpc/LifeSim.java:282  this.lasteventmsg = new stringbuffer("с новым ").append(this.curyear).append(" годом! новая эпоха в истории it…
OK 151. Телевизор (новости, фильмы).                               hits=6
        src/rpc/Browser.java:23  a[2] = "новости it";
        src/rpc/Browser.java:386  a0[0] = new stringbuffer("новости it — ").append(i0).tostring();
OK 152. Журналы (Радио, Игромания, Хакер).                         hits=51
        src/rpc/AudioEngine.java:213  public void playradiochiptune(int i) {
        src/rpc/CalendarHistory.java:12  a[2] = "1982 • выход культового домашнего компьютера commodore 64 и журнала «радио» с рк-86.";
-- 153. Музыка на кассете/CD.
~  154. Радиопередача (попса 80-х, рейв 90-х, рок 2000-х).         hits=1
        src/rpc/LifeSim.java:105  a[4] = "радиопередача • новости технологий";
-- 155. Кинотеатр.
OK 156. Компьютерный клуб 90-х.                                    hits=11
        src/rpc/City.java:28  a[3] = "компьютерный клуб 90-х";
        src/rpc/City.java:129  this.dialoguenpc = "админ клуба «кибер-подвал»";
~  157. Книги по электронике.                                      hits=1
        src/rpc/Funcs.java:337  "пойти в библиотеку", "взять книгу по электронике",
OK 158. Плакаты на стене.                                          hits=3
        src/rpc/j2me/HostImpl.java:7  import javax.microedition.lcdui.commandlistener;
        src/rpc/j2me/HostImpl.java:133  * blocked on the lock object that the command listener notifies.
-- 159. Мемы 2010-х.
~  160. Итоги десятилетия.                                         hits=1
        src/rpc/Game.java:4338  this.header("итоги 2027", "fin");
OK 161. Сборка ПК на время.                                        hits=11
        src/rpc/BuildView.java:405  case 4: return "промотать время";
        src/rpc/Exp.java:158  a10[0] = "windows 11 23h2 tiny11|сборка без tpm 2.0/secureboot, отключение winsxs, эмуляция acpi 5.0";
OK 162. Чистка от пыли.                                            hits=12
        src/rpc/Browser.java:216  a1[2] = "1. продуть радиатор от пыли.";
        src/rpc/Funcs.java:125  "замерить шум дб", "поставить фильтр от пыли",
OK 163. Разгон (шкала напряжения).                                 hits=16
        src/rpc/Browser.java:206  a0[5] = "3. разгон: стоит ли оно того";
        src/rpc/Browser.java:228  a2[0] = "тема: разгон";
OK 164. Установка Windows (дискеты).                               hits=111
        src/rpc/BuildView.java:404  case 3: return "установка системы";
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
OK 165. Ремонт (паяльник).                                         hits=18
        src/rpc/Browser.java:207  a0[6] = "4. пыль и ёмкости: опыт ремонта";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
OK 166. Погладь кота.                                              hits=15
        src/rpc/CalendarHistory.java:67  a0[7] = "хозяин барсика • довести счастье кота до 100%";
        src/rpc/City.java:37  a0[2] = "ряды ящиков с б/у платами, старыми кулерами и котами в мешке.";
-- 167. Полив цветка.
OK 168. Настройка ТВ (антенна).                                    hits=6
        src/rpc/CityArt.java:148  /** a side block: walls, windows, roof, antenna. */
        src/rpc/CityArt.java:169  // chimney / antenna
-- 169. Перемотка кассеты карандашом.
OK 170. Выключение ПК перед сном.                                  hits=2
        src/rpc/Funcs.java:111  "выставить перемычки", "подключить переднюю панель",
        src/rpc/LifeSim.java:105  a[4] = "радиопередача • новости технологий";
OK 171. Коллекционер видеокарт.                                    hits=34
        src/rpc/Browser.java:343  a8[0] = "зал: видеокарты";
        src/rpc/Browser.java:487  s1 = "agp: видеокартам стало тесно в pci";
OK 172. Ретро-геймер (Doom).                                       hits=30
        src/rpc/AudioEngine.java:201  public void playdoomriff() {
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
~  173. Хакер.                                                     hits=1
        src/rpc/Funcs.java:336  "посмотреть фильм про хакеров", "купить попкорн",
-- 174. Меценат (ПК детдому).
-- 175. Долгожитель (до 2027).
OK 176. Мастер на все руки (100 ПК).                               hits=24
        src/rpc/AudioEngine.java:6  private int mastervolume;
        src/rpc/AudioEngine.java:21  this.mastervolume = 85;
OK 177. Друг кота.                                                 hits=17
        src/rpc/CalendarHistory.java:67  a0[7] = "хозяин барсика • довести счастье кота до 100%";
        src/rpc/City.java:37  a0[2] = "ряды ящиков с б/у платами, старыми кулерами и котами в мешке.";
OK 178. Фотограф.                                                  hits=3
        src/rpc/Funcs.java:235  "сфотографировать сетап", "залипать в потолок"
        src/rpc/Funcs.java:298  "дать кошачью мяту", "сфотографировать кота",
OK 179. Писатель (статья в журнал).                                hits=4
        src/rpc/Browser.java:583  a8[9] = new stringbuffer("статья менялась ").append(10 + i0 % 40).append(" раз").tostring();
        src/rpc/CalendarHistory.java:12  a[2] = "1982 • выход культового домашнего компьютера commodore 64 и журнала «радио» с рк-86.";
OK 180. Легенда рынка.                                             hits=9
        src/rpc/Browser.java:670  a2[6] = new stringbuffer("доля рынка: ").append(a.share(a1)).append("%").tostring();
        src/rpc/CalendarHistory.java:11  a[1] = "1981 • дебют легендарного ibm pc 5150 на процессоре intel 8088 и pc-dos 1.0.";
-- 181. Уборка комнаты.
OK 182. Готовка еды (пельмени, чай).                               hits=2
        src/rpc/Funcs.java:216  "сходить на кухню", "помыть кружку", "сварить пельмени",
        src/rpc/LifeSim.java:453  this.lasteventmsg = "вы сварили горячие пельмени со сметаной. сытость на максимуме!";
OK 183. Сон (без сна падает энергия).                              hits=4
        src/rpc/Game.java:3391  text.draw(this.g, "энергия "
        src/rpc/Game.java:3393  tx += this.g.textwidth("энергия 100%", 0) + 8;
OK 184. Кот спит на системнике (перегрев).                         hits=8
        src/rpc/Browser.java:221  a1[7] = "   будет перегрев и bsod.";
        src/rpc/City.java:94  a[2] = "3. «провести аудит серверной и устранить перегрев стоек»";
OK 185. Мебель (стол, стул, шкаф).                                 hits=18
        src/rpc/Browser.java:733  a2[9] = "3 — купить и на стол";
        src/rpc/BuildView.java:379  case 2: return "стол";
~  186. Декор (постеры, коврик).                                   hits=1
        src/rpc/Funcs.java:375  "найти коврик с логотипом", "найти дискетницу",
OK 187. Коммуналка (свет, интернет).                               hits=49
        src/rpc/BuildView.java:45  case 17: return new int[] { 48, 70, 26, 10 };    /* network */
        src/rpc/BuildView.java:194  // sound / network / riser
-- 188. Стирка одежды.
OK 189. Ремонт труб.                                               hits=14
        src/rpc/Browser.java:207  a0[6] = "4. пыль и ёмкости: опыт ремонта";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
-- 190. Обогреватель зимой.
OK 191. Собеседование.                                             hits=3
        src/rpc/City.java:90  this.dialoguetext = new stringbuffer("«мы ищем ведущего системного архитектора на проект ").append(i0).append(…
        src/rpc/City.java:92  a[0] = "1. «пройти собеседование по архитектуре x86 и шинам»";
-- 192. Офисная рутина.
-- 193. Карьерная лестница.
OK 194. Начальник (настроение).                                    hits=2
        src/rpc/Game.java:3399  text.draw(this.g, "настроение "
        src/rpc/Game.java:4096  + "%  настроение "
-- 195. Конкуренция (демпинг).
-- 196. Налоги.
~  197. Бухгалтерия.                                               hits=1
        src/rpc/Game.java:540  a[3] = this.makeorder("мария (бухгалтерия)", "не включается после скачка сети", 3, 2900, 55, i, "заменить неис…
-- 198. Кризис 1998/2008.
-- 199. Слияние компаний.
-- 200. Увольнение.
OK 201. Моддинг корпуса.                                           hits=2
        src/rpc/Exp.java:220  a14[12] = "моддинг bios под nvme ssd на платах без uefi|внедрение nvmexpressdxe драйвера в образ ami/award bio…
        src/rpc/Funcs.java:113  "поставить вентилятор на выдув", "наклеить ножки корпуса",
-- 202. Пиксель-арт (скины).
OK 203. Программирование (продажа софта).                          hits=2
        src/rpc/Exp.java:45  a0[3] = "создание софта & игр";
        src/rpc/Exp.java:168  a10[10] = "reactos 0.4.15 (open-source nt)|открытая замена windows nt с нативным запуском win32 софта";
~  204. Создание игр.                                              hits=1
        src/rpc/Exp.java:45  a0[3] = "создание софта & игр";
OK 205. Музыка (трекер).                                           hits=2
        src/rpc/Exp.java:199  a13[1] = "4kb демосцена с процедурной музыкой и фракталами|упаковка 3d-графики, плазмы и трекерного синтезатор…
        src/rpc/Inst.java:408  a5[3] = "торрент-трекеры";
OK 206. Создание сайта (HTML).                                     hits=2
        src/rpc/Exp.java:45  a0[3] = "создание софта & игр";
        src/rpc/Game.java:9150  this.footer("ok: листать 3: купить", "1: к сайтам");
OK 207. Стриминг (2020-е).                                         hits=2
        src/rpc/Inst.java:423  a7[2] = "стриминг";
        src/rpc/Inst.java:431  a8[3] = "стриминг";
~  208. Блог о ретро-железе.                                       hits=1
        src/rpc/Inst.java:398  a4[1] = "блоги";
-- 209. Фотография на пленку.
-- 210. Косплей на Хэллоуин.
OK 211. Новости по радио.                                          hits=53
        src/rpc/AudioEngine.java:213  public void playradiochiptune(int i) {
        src/rpc/Browser.java:23  a[2] = "новости it";
-- 212. Пандемия 2020.
~  213. Падение метеорита 2013.                                    hits=1
        src/rpc/CalendarHistory.java:43  a[33] = "2013 • падение челябинского метеорита. релиз grand theft auto v и консолей ps4/xbox one.";
-- 214. Выборы.
-- 215. Олимпиада 1980/2014.
~  216. Технологические прорывы (iPhone 2007).                     hits=1
        src/rpc/CalendarHistory.java:37  a[27] = "2007 • первый iphone, релиз crysis («can it run crysis?») и windows vista.";
~  217. Смерть легенды (Стив Джобс).                               hits=1
        src/rpc/Exp.java:166  a10[8] = "nextstep 3.3 / openstep 4.2|легендарная объектно-ориентированная ос стива джобса на x86 пк";
-- 218. Праздники.
OK 219. Погодные аномалии (перегрев ПК).                           hits=7
        src/rpc/Browser.java:221  a1[7] = "   будет перегрев и bsod.";
        src/rpc/City.java:94  a[2] = "3. «провести аудит серверной и устранить перегрев стоек»";
OK 220. Отключение света в 90-х.                                   hits=2
        src/rpc/Exp.java:158  a10[0] = "windows 11 23h2 tiny11|сборка без tpm 2.0/secureboot, отключение winsxs, эмуляция acpi 5.0";
        src/rpc/Exp.java:176  a11[4] = "the witcher 3: wild hunt next-gen|патч на отключение трассировки лучей и упрощённую геометрию листвы…
-- 221. Соседи сверху шумят.
-- 222. Друзья приходят с пиццей.
-- 223. Влюблённость.
-- 224. Свидание (одежда).
OK 225. Помощь другу (бесплатный ремонт).                          hits=15
        src/rpc/Browser.java:207  a0[6] = "4. пыль и ёмкости: опыт ремонта";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
-- 226. Звонок родителей.
-- 227. Кот соседей.
-- 228. Бабушка с вареньем.
-- 229. Школа и экзамены.
-- 230. Выпускной.
OK 231. Установка драйверов.                                       hits=15
        src/rpc/BuildView.java:404  case 3: return "установка системы";
        src/rpc/Exp.java:160  a10[2] = "mac os x 10.4 tiger (x86 hackintosh)|инъекция kext-драйверов appleacpiplatform и патч mach_kernel по…
OK 232. Конфликт версий DirectX.                                   hits=5
        src/rpc/CalendarHistory.java:32  a[22] = "2002 • видеокарта ati radeon 9700 pro с аппаратной поддержкой directx 9.0.";
        src/rpc/Exp.java:173  a11[1] = "crysis (2007) very high directx 10|запуск бенчмарка crysis 'can it run crysis?' на предельных настро…
~  233. Чистка реестра.                                            hits=1
        src/rpc/Funcs.java:183  "ультразвуковая чистка платы"
OK 234. Установка Linux (дуалбут).                                 hits=28
        src/rpc/BuildView.java:404  case 3: return "установка системы";
        src/rpc/CalendarHistory.java:21  a[11] = "1991 • линус торвальдс публикует первую версию ядра linux 0.01. распад ссср.";
~  235. Пиратские игры с вирусом.                                  hits=1
        src/rpc/Funcs.java:365  "найти дискету с вирусом", "найти cd с демо",
OK 236. Установка модов.                                           hits=13
        src/rpc/BuildView.java:404  case 3: return "установка системы";
        src/rpc/Exp.java:165  a10[7] = "haiku os r1 beta (beos successor)|установка молниеносной многопоточной ос beos нового поколения";
OK 237. Антивирус находит вирус.                                   hits=3
        src/rpc/Funcs.java:141  "удалить вирус", "поставить антивирус",
        src/rpc/Funcs.java:365  "найти дискету с вирусом", "найти cd с демо",
-- 238. Загрузочная флешка.
OK 239. Форматирование диска.                                      hits=9
        src/rpc/AudioEngine.java:133  public void playdiskaccess(int i, int i0, int i1, boolean b) {
        src/rpc/Funcs.java:145  "поставить photoshop", "поставить игру с диска",
~  240. Восстановление данных.                                     hits=1
        src/rpc/Browser.java:541  a2[5] = "долговременное хранение данных:";
-- 241. Будильник.
-- 242. Зарядка.
-- 243. Завтрак (яичница).
OK 244. Проверка почты.                                            hits=5
        src/rpc/BuildView.java:406  case 5: return "проверка сборки: post";
        src/rpc/City.java:41  a0[6] = "очередь за посылками с чипами. проверка коробки: нет ли там кирпича?";
-- 245. Дневник.
-- 246. Вечерний чай у окна.
OK 247. Бессонница (чтение мануала).                               hits=4
        src/rpc/City.java:36  a0[1] = "бесконечные павильоны с чипами, сокетами, переходниками и мануалами.";
        src/rpc/Shell.java:501  this.println("чтение списков пакетов... готово");
OK 248. Суббота — банный день.                                     hits=50
        src/rpc/AudioEngine.java:5  private boolean soundenabled;
        src/rpc/AudioEngine.java:20  this.soundenabled = true;
~  249. Воскресенье — отдых.                                       hits=1
        src/rpc/Game.java:4137  string title = "отдых";
OK 250. Конец дня (ложиться спать).                                hits=4
        src/rpc/Funcs.java:206  "вложиться в акции", "продать акции",
        src/rpc/Funcs.java:221  "включить лампу", "выключить лампу", "лечь спать",
OK 251. Почта России (задержки).                                   hits=2
        src/rpc/CityArt.java:478  text.draw(g, "почта", x + w / 2 - 14, y + 10, 0,
        src/rpc/Inst.java:368  a0[3] = "первая почта";
OK 252. Курьер приходит, когда тебя нет.                           hits=2
        src/rpc/Browser.java:462  s0 = "ddr5 дешевеет, pcie 5.0 приходит";
        src/rpc/Browser.java:482  s1 = "sata приходит на смену ide";
~  253. Самовывоз со склада.                                       hits=1
        src/rpc/City.java:100  this.dialoguetext = "«вам посылка со склада микроэлектроники! распишитесь в квитанции.»";
-- 254. Пьяный курьер.
OK 255. Сосед забрал посылку.                                      hits=3
        src/rpc/City.java:103  a0[1] = "2. «забрать посылку домой на верстак»";
        src/rpc/City.java:124  a2[2] = "3. «послушать сплетни за соседним столиком»";
OK 256. Проверка при получении.                                    hits=4
        src/rpc/BuildView.java:406  case 5: return "проверка сборки: post";
        src/rpc/City.java:41  a0[6] = "очередь за посылками с чипами. проверка коробки: нет ли там кирпича?";
-- 257. Возврат брака.
OK 258. Зимняя доставка.                                           hits=8
        src/rpc/BuildView.java:378  case 1: return "доставка";
        src/rpc/BuildView.java:390  case 1: return "доставка: заказы";
-- 259. Почтальон 80-х.
OK 260. Курьер за 30 минут (2020-е).                               hits=19
        src/rpc/Funcs.java:222  "вздремнуть двадцать минут", "задёрнуть шторы",
        src/rpc/Game.java:270  /** last autosave in milliseconds (every 5 minutes of play) */
OK 261. Сборка на время.                                           hits=11
        src/rpc/BuildView.java:405  case 4: return "промотать время";
        src/rpc/Exp.java:158  a10[0] = "windows 11 23h2 tiny11|сборка без tpm 2.0/secureboot, отключение winsxs, эмуляция acpi 5.0";
OK 262. Не тот сокет (возврат).                                    hits=27
        src/rpc/Browser.java:336  a7[5] = new stringbuffer("сокет: ").append((a1.length <= 0) ? "—" : rpc.score.sockname((a.get(a1[a1.length - 1…
        src/rpc/Browser.java:439  a4[3] = "забыл про сокет — не влезло.";
OK 263. БП не тянет.                                               hits=3
        src/rpc/Game.java:7772  this.toastmsg("бп не тянет — нужен мощнее");
        src/rpc/Games.java:1318  return new stringbuffer("видео не тянет: класс ").append(data[i][3]).tostring();
OK 264. Кулер не влезает в корпус.                                 hits=42
        src/rpc/Browser.java:205  a0[4] = "2. кулер гудит — что делать";
        src/rpc/Browser.java:214  a1[0] = "тема: кулер гудит";
OK 265. Видеокарта упирается в HDD.                                hits=13
        src/rpc/Browser.java:487  s1 = "agp: видеокартам стало тесно в pci";
        src/rpc/Browser.java:520  a0[5] = "3. видеокарта";
OK 266. Забыл снять пленку с термопасты.                           hits=4
        src/rpc/Browser.java:249  a3[7] = "забыл снять плёнку с кулера»";
        src/rpc/Browser.java:439  a4[3] = "забыл про сокет — не влезло.";
OK 267. Конфликт памяти.                                           hits=17
        src/rpc/Browser.java:706  a6[5] = new stringbuffer("модулей памяти: ").append(a5.length).tostring();
        src/rpc/CalendarHistory.java:42  a[32] = "2012 • стандарт быстрой памяти ddr3-2400 и видеокарты nvidia geforce gtx 680 (kepler).";
-- 268. Шум и вибрация.
-- 269. Дешевый БП сгорает.
OK 270. Ведро 2005 под Windows 11.                                 hits=99
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 271. Гроза (молния в дом).                                      hits=2
        src/rpc/CalendarHistory.java:61  a0[1] = "гроза митино • сбить цену у продавца на 30% три раза подряд";
        src/rpc/LifeSim.java:68  a1[2] = "гроза";
OK 272. Веерное отключение света.                                  hits=2
        src/rpc/Exp.java:158  a10[0] = "windows 11 23h2 tiny11|сборка без tpm 2.0/secureboot, отключение winsxs, эмуляция acpi 5.0";
        src/rpc/Exp.java:176  a11[4] = "the witcher 3: wild hunt next-gen|патч на отключение трассировки лучей и упрощённую геометрию листвы…
-- 273. Прорыв трубы (затопило ПК).
OK 274. Мышь в корпусе.                                            hits=9
        src/rpc/Db.java:46  a[16] = "мышь";
        src/rpc/Funcs.java:248  "собрать пк в корпусе тостера", "собрать пк в корпусе nes",
OK 275. Скачок напряжения.                                         hits=2
        src/rpc/Funcs.java:203  "пережить скачок инфляции", "рассчитать бюджет",
        src/rpc/WorkshopTools.java:33  a[4] = "⚡ мультиметр (напряжения)";
OK 276. Кража видеокарты.                                          hits=8
        src/rpc/Browser.java:343  a8[0] = "зал: видеокарты";
        src/rpc/Browser.java:604  a4[6] = "видеокарты:";
-- 277. Пожар БП.
OK 278. Кот скинул монитор.                                        hits=42
        src/rpc/Browser.java:502  s1 = "мониторы: зелёный или янтарный?";
        src/rpc/BuildView.java:9  * plus the peripherals around it (monitor, keyboard, mouse, printer).
-- 279. Пыльная буря.
-- 280. Поломка лифта.
OK 281. Новый год.                                                 hits=2
        src/rpc/City.java:188  return "☕ вы взяли новый заказ на ремонт и выпили отличный кофе!";
        src/rpc/WorkshopTools.java:185  this.toolfeedback = new stringbuffer("✓ вздутый электролит c").append(this.selcap + 1).append(" выпаян, запаян…
OK 282. День рождения.                                             hits=50
        src/rpc/AudioEngine.java:5  private boolean soundenabled;
        src/rpc/AudioEngine.java:20  this.soundenabled = true;
OK 283. 8 марта / 23 февраля.                                      hits=2
        src/rpc/LifeSim.java:157  return "23 февраля";
        src/rpc/LifeSim.java:160  return "8 марта";
-- 284. 1 сентября.
~  285. Последний звонок.                                          hits=1
        src/rpc/Game.java:4773  this.toastmsg("2027: последний год — финиш подходит к концу");
OK 286. Новый 2027 (финал).                                        hits=535
        src/rpc/Assets.java:4  final private static int[] brand;
        src/rpc/Assets.java:10  final private static int cache = 24;
-- 287. Хэллоуин.
OK 288. День Победы.                                               hits=50
        src/rpc/AudioEngine.java:5  private boolean soundenabled;
        src/rpc/AudioEngine.java:20  this.soundenabled = true;
-- 289. Масленица.
OK 290. Старый Новый год.                                          hits=6
        src/rpc/Browser.java:234  a2[6] = "старый техпроцесс частоту";
        src/rpc/City.java:188  return "☕ вы взяли новый заказ на ремонт и выпили отличный кофе!";
OK 291. Заказ от школы.                                            hits=26
        src/rpc/BuildView.java:390  case 1: return "доставка: заказы";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
OK 292. Заказ от банка.                                            hits=26
        src/rpc/BuildView.java:390  case 1: return "доставка: заказы";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
OK 293. Заказ от геймера.                                          hits=26
        src/rpc/BuildView.java:390  case 1: return "доставка: заказы";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
OK 294. Заказ от бабушки.                                          hits=26
        src/rpc/BuildView.java:390  case 1: return "доставка: заказы";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
~  295. Аукцион.                                                   hits=1
        src/rpc/Funcs.java:201  "выставить раритет на аукцион", "поймать распродажу",
-- 296. Гарантийный случай.
-- 297. Клиент-мошенник.
-- 298. Отзыв в газете.
-- 299. Конкурент украл идею.
-- 300. Франшиза в другом городе.
OK 301. Анимированный корпус (кулеры, LEDs).                       hits=18
        src/rpc/Browser.java:246  a3[4] = new stringbuffer("корпус: ").append(rpc.browser.caseof(i0, a)).tostring();
        src/rpc/CityArt.java:11  * facades turn to glass and leds.  weather, day/night, holidays and a
OK 302. Плавная вставка деталей в слот.                            hits=270
        src/rpc/Browser.java:326  a6[9] = new stringbuffer().append(a.count()).append(" деталей 1979-2026").tostring();
        src/rpc/Browser.java:472  s1 = "m.2: маленький слот, большой смысл";
OK 303. Экран монитора (BIOS, загрузка Windows).                   hits=148
        src/rpc/Browser.java:217  a1[3] = "2. проверить обороты (bios).";
        src/rpc/BuildView.java:383  default: return "bios";
OK 304. Частицы пыли в комнате.                                    hits=14
        src/rpc/Browser.java:216  a1[2] = "1. продуть радиатор от пыли.";
        src/rpc/Funcs.java:125  "замерить шум дб", "поставить фильтр от пыли",
OK 305. Кот ходит по комнате.                                      hits=14
        src/rpc/Browser.java:462  s0 = "ddr5 дешевеет, pcie 5.0 приходит";
        src/rpc/Browser.java:482  s1 = "sata приходит на смену ide";
-- 306. Капли дождя на стекле.
OK 307. Тени от монитора.                                          hits=8
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
        src/rpc/Funcs.java:165  "заменить лампу в мониторе", "настроить фокус монитора",
OK 308. Свечение монитора в темноте.                               hits=4
        src/rpc/Funcs.java:165  "заменить лампу в мониторе", "настроить фокус монитора",
        src/rpc/Funcs.java:181  "перепаять конденсатор монитора", "починить кнопку reset",
-- 309. Искры при КЗ.
-- 310. Дым от сгоревшего ПК.
-- 311. Пар от чая.
OK 312. Молния освещает комнату.                                   hits=2
        src/rpc/Funcs.java:218  "открыть окно", "закрыть окно", "проветрить комнату",
        src/rpc/Game.java:4711  this.footer("←→ год", "ok: в комнату");
OK 313. Радуга после дождя.                                        hits=6
        src/rpc/Funcs.java:182  "восстановить дорожки после залития",
        src/rpc/Funcs.java:196  "купить карту с рук", "проверить карту после майнинга",
OK 314. 3D-иконки в меню.                                          hits=6
        src/rpc/Funcs.java:12  * the list is browsable in game (меню → 500 функций), filterable by chapter,
        src/rpc/Game.java:5223  this.footer((this.sandbox) ? "песочница" : "ok — сборка", "1: меню");
OK 315. Курсор-стрелка Windows.                                    hits=99
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 316. Движущиеся полоски загрузки.                               hits=2
        src/rpc/Game.java:574  this.toast = new stringbuffer("ошибка загрузки базы: ").append(a1.tostring()).tostring();
        src/rpc/Setup.java:71  return "порядок загрузки: с чего ставим ос";
OK 317. Карта города.                                              hits=18
        src/rpc/Browser.java:487  s1 = "agp: видеокартам стало тесно в pci";
        src/rpc/Browser.java:520  a0[5] = "3. видеокарта";
~  318. Полки в магазине.                                          hits=1
        src/rpc/Browser.java:730  a2[6] = new stringbuffer("цена: ").append(i3).append(" (в магазине ").append(a1.price).append(")").tostring();
-- 319. Портрет героя.
OK 320. Смена сезонов (цвет).                                      hits=7
        src/rpc/Browser.java:497  s1 = "vga: 256 цветов уже реальность";
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
OK 321. Звук запуска Windows.                                      hits=121
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
OK 322. Экран «Добро пожаловать».                                  hits=6
        src/rpc/Funcs.java:153  "починить синий экран", "переустановить систему",
        src/rpc/Funcs.java:284  "поставить сетевой экран", "отбиться от ddos",
OK 323. Часы на панели задач.                                      hits=3
        src/rpc/Browser.java:361  a9[5] = "сегодня это часы в тостере».";
        src/rpc/Game.java:7632  this.toastmsg(new stringbuffer("часы bios: 12.05.").append((this.slots[1] != null) ? this.slots[1].year : this…
-- 324. Очистка корзины.
~  325. Скринсейвер (трубки).                                      hits=1
        src/rpc/Funcs.java:151  "включить скринсейвер с рыбами", "настроить дату и время",
-- 326. Смена обоев.
OK 327. BSOD с белым текстом.                                      hits=25
        src/rpc/AudioEngine.java:197  public void playbsod() {
        src/rpc/Browser.java:221  a1[7] = "   будет перегрев и bsod.";
OK 328. Логотип BIOS (Award, AMI).                                 hits=34
        src/rpc/Browser.java:217  a1[3] = "2. проверить обороты (bios).";
        src/rpc/BuildView.java:383  default: return "bios";
OK 329. Звук кулера.                                               hits=23
        src/rpc/Browser.java:249  a3[7] = "забыл снять плёнку с кулера»";
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
-- 330. Мигание HDD LED.
OK 331. Memory Test 640K OK.                                       hits=30
        src/rpc/Corp.java:22  public int testing;
        src/rpc/Corp.java:98  this.testing = 1;
-- 332. Размытость на ЭЛТ.
OK 333. Низкое разрешение (пиксели).                               hits=2
        src/rpc/Exp.java:178  a11[6] = "black myth: wukong (unreal engine 5)|nanite и lumen отключены, разрешение рендера 25%, эмуляция dx12…
        src/rpc/Game.java:8254  a4[2] = new stringbuffer("разрешение: ").append(a.f[2]).append("x").append(a.f[3]).tostring();
~  334. Ковер под ногами (статическое электричество).              hits=1
        src/rpc/LifeSim.java:474  this.lasteventmsg = "вы пропылесосили ковер, вынесли банки и протерли стол от пыли. чистота!";
-- 335. Провода под столом.
-- 336. Балкон (подышать).
OK 337. Соседский ремонт (тряска экрана).                          hits=16
        src/rpc/Browser.java:207  a0[6] = "4. пыль и ёмкости: опыт ремонта";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
OK 338. Старый телефон на столе.                                   hits=11
        src/rpc/Browser.java:234  a2[6] = "старый техпроцесс частоту";
        src/rpc/Funcs.java:191  "продать старый корпус", "выставить лот на avito",
-- 339. Холодильник с едой.
OK 340. Окно в подъезд.                                            hits=4
        src/rpc/Funcs.java:218  "открыть окно", "закрыть окно", "проветрить комнату",
        src/rpc/Funcs.java:231  "помыть окно", "разобрать коробки",
OK 341. Полярность питания.                                        hits=17
        src/rpc/Browser.java:463  s1 = "блоки питания по новому стандарту";
        src/rpc/Compat.java:51  return "at-блок питания не встанет в atx-корпус";
~  342. Слои термопасты.                                           hits=1
        src/rpc/WorkshopTools.java:170  this.toolfeedback = new stringbuffer("✓ идеальный слой термопасты (").append(this.pastethickness).append("%). …
OK 343. Сборка по мануалу.                                         hits=8
        src/rpc/Exp.java:158  a10[0] = "windows 11 23h2 tiny11|сборка без tpm 2.0/secureboot, отключение winsxs, эмуляция acpi 5.0";
        src/rpc/Exp.java:161  a10[3] = "ubuntu 24.04 lts (kernel 6.8 + gnome 46)|сборка ядра linux без лишних модулей, zswap и wayland на ми…
OK 344. Мультиметр.                                                hits=6
        src/rpc/City.java:144  a4[2] = "3. «проверить мультиметром прямо на месте»";
        src/rpc/Funcs.java:174  "прозвонить мультиметром", "найти короткое замыкание",
OK 345. Пайка конденсаторов.                                       hits=7
        src/rpc/CalendarHistory.java:63  a0[3] = "паяльный спец • заменить 50 вздутых low-esr конденсаторов";
        src/rpc/City.java:153  a5[1] = "2. «давай поторгуемся на партию конденсаторов!»";
OK 346. Сдувание пыли.                                             hits=11
        src/rpc/Browser.java:216  a1[2] = "1. продуть радиатор от пыли.";
        src/rpc/Funcs.java:125  "замерить шум дб", "поставить фильтр от пыли",
OK 347. Замена термопасты.                                         hits=4
        src/rpc/Exp.java:168  a10[10] = "reactos 0.4.15 (open-source nt)|открытая замена windows nt с нативным запуском win32 софта";
        src/rpc/Exp.java:177  a11[5] = "starfield (creation engine 2)|удаление объемного тумана и замена текстур планет на 128x128 пикселей"…
OK 348. Установка процессора (ключ).                               hits=62
        src/rpc/Browser.java:542  a2[6] = "разъём для установки процессора:";
        src/rpc/Browser.java:550  a3[6] = "форм-факторы и ключи.";
~  349. Смазка вентилятора.                                        hits=1
        src/rpc/Funcs.java:256  "кулер из вентилятора икеа", "пк в коробке из-под пиццы",
OK 350. Распиновка Power SW.                                       hits=44
        src/rpc/BuildView.java:69  * @param pcon     true when the machine is assembled and powered
        src/rpc/BuildView.java:295  // powered off, but the glass still reflects the room
~  351. Разговор с бабушкой.                                       hits=1
        src/rpc/Game.java:4289  this.footer("ok: разговор  ←→ асеад  тап: нобер", "лск: баз");
-- 352. Сосед-алкоголик.
OK 353. Друг детства с тортом.                                     hits=2
        src/rpc/Funcs.java:233  "сыграть в настолку", "позвонить другу",
        src/rpc/Game.java:8218  a0[5] = new stringbuffer("производитель: ").append((a.f[5] != 0) ? (a.f[5] != 0) ? "amd" : "другой" : "intel")…
OK 354. Первая любовь.                                             hits=3
        src/rpc/Game.java:7621  this.toastmsg(new stringbuffer("первая загрузка: ").append(boot[this.bootsel]).tostring());
        src/rpc/Game.java:8423  a[0] = new stringbuffer("этап: ").append((i0 >= 20) ? (i0 >= 60) ? (i0 >= 90) ? "первая загрузка" : "установка…
OK 355. Драка на рынке.                                            hits=2
        src/rpc/Game.java:7038  this.g.drawtext("закажите детали в рынке", 8, i0 + 12 + this.fh0, 0);
        src/rpc/Game.java:7495  a[4] = "на рынке в слоте «сеть».";
-- 356. Помощь старушке.
-- 357. Подработка на дядю.
-- 358. Учитель труда.
-- 359. Свадьба друга.
OK 360. Похороны (настроение).                                     hits=2
        src/rpc/Game.java:3399  text.draw(this.g, "настроение "
        src/rpc/Game.java:4096  + "%  настроение "
OK 361. Коллекционер видеокарт.                                    hits=34
        src/rpc/Browser.java:343  a8[0] = "зал: видеокарты";
        src/rpc/Browser.java:487  s1 = "agp: видеокартам стало тесно в pci";
-- 362. Мастер-ломастер.
OK 363. Скоростная сборка.                                         hits=8
        src/rpc/Exp.java:158  a10[0] = "windows 11 23h2 tiny11|сборка без tpm 2.0/secureboot, отключение winsxs, эмуляция acpi 5.0";
        src/rpc/Exp.java:161  a10[3] = "ubuntu 24.04 lts (kernel 6.8 + gnome 46)|сборка ядра linux без лишних модулей, zswap и wayland на ми…
-- 364. Меценат.
-- 365. Долгожитель.
OK 366. Легенда.                                                   hits=7
        src/rpc/CalendarHistory.java:11  a[1] = "1981 • дебют легендарного ibm pc 5150 на процессоре intel 8088 и pc-dos 1.0.";
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
~  367. Хакер.                                                     hits=1
        src/rpc/Funcs.java:336  "посмотреть фильм про хакеров", "купить попкорн",
-- 368. Богач.
OK 369. Друг кота.                                                 hits=17
        src/rpc/CalendarHistory.java:67  a0[7] = "хозяин барсика • довести счастье кота до 100%";
        src/rpc/City.java:37  a0[2] = "ряды ящиков с б/у платами, старыми кулерами и котами в мешке.";
OK 370. Хранитель времени.                                         hits=3
        src/rpc/CalendarHistory.java:48  a[38] = "2018 • аппаратная трассировка лучей в реальном времени (nvidia rtx 2080 ray tracing).";
        src/rpc/Exp.java:200  a13[2] = "своя ос реального времени на c и ассемблере|написание mbr загрузчика, переход в защищенный 32-бит ре…
OK 371. Максимум графики.                                          hits=5
        src/rpc/Exp.java:59  a2[0] = "урезание графики, разрешения и эффектов до минимума";
        src/rpc/Exp.java:199  a13[1] = "4kb демосцена с процедурной музыкой и фракталами|упаковка 3d-графики, плазмы и трекерного синтезатор…
OK 372. Оптимизация под эмулятор.                                  hits=7
        src/rpc/Exp.java:182  a11[10] = "alan wake 2 (mesh shaders bypass)|обход обязательного требования mesh shaders через эмулятор vkd3d-…
        src/rpc/Exp.java:187  a12[1] = "предел шины bclk / fsb и делителей ram|форсирование шины до упора материнской платы, оптимизация суб…
OK 373. Процедурная графика.                                       hits=2
        src/rpc/Game.java:9661  a[3] = "тип: текст/графика";
        src/rpc/Game.java:9707  s = "графика при таком озу не поднимется";
OK 374. Анимация загрузки.                                         hits=2
        src/rpc/Game.java:574  this.toast = new stringbuffer("ошибка загрузки базы: ").append(a1.tostring()).tostring();
        src/rpc/Setup.java:71  return "порядок загрузки: с чего ставим ос";
OK 375. Звук в реальном времени.                                   hits=19
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
OK 376. Время идёт всегда.                                         hits=6
        src/rpc/Browser.java:547  a3[3] = "энергозависима и нужна всегда.";
        src/rpc/BuildView.java:405  case 4: return "промотать время";
OK 377. Автосохранение каждые 5 минут.                             hits=19
        src/rpc/Funcs.java:222  "вздремнуть двадцать минут", "задёрнуть шторы",
        src/rpc/Game.java:270  /** last autosave in milliseconds (every 5 minutes of play) */
~  378. Удобное управление (стрелки, OK).                          hits=1
        src/rpc/Setup.java:83  return "стрелки — выбор, ok — изменить";
~  379. Крупные шрифты.                                            hits=1
        src/rpc/Game.java:8403  this.g.drawtext("ставится дольше (шрифты).", 8, i6 + (this.fh0 + 2), 0);
OK 380. Темная/светлая тема.                                       hits=17
        src/rpc/Browser.java:214  a1[0] = "тема: кулер гудит";
        src/rpc/Browser.java:228  a2[0] = "тема: разгон";
-- 381. Мысли героя.
OK 382. Смена эпох.                                                hits=7
        src/rpc/CalendarHistory.java:10  a[0] = "1980 • xxii летние олимпийские игры в москве. эпоха микропроцессоров z80 и 6502.";
        src/rpc/CalendarHistory.java:57  a[47] = "2027 • встреча нового 2027 года! великий финал эпохи и вечная слава мастеру!";
OK 383. Ностальгия (диск с Doom).                                  hits=103
        src/rpc/AudioEngine.java:133  public void playdiskaccess(int i, int i0, int i1, boolean b) {
        src/rpc/AudioEngine.java:201  public void playdoomriff() {
OK 384. Письмо себе в будущее.                                     hits=2
        src/rpc/Funcs.java:277  "настроить почту", "отправить письмо",
        src/rpc/Funcs.java:278  "получить письмо", "настроить новостную группу",
-- 385. Конец игры (2027).
OK 386. Секретный финал (Win11 на хламе).                          hits=535
        src/rpc/Assets.java:4  final private static int[] brand;
        src/rpc/Assets.java:10  final private static int cache = 24;
-- 387. Разные концовки.
-- 388. Титры со статистикой.
OK 389. Новая игра+.                                               hits=17
        src/rpc/Browser.java:193  return "новая, гарантия";
        src/rpc/Corp.java:41  a0[2] = "игра";
-- 390. Благодарности.
OK 391. Windows 11 на ведре 2005.                                  hits=99
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 392. Hackintosh на AMD.                                         hits=2
        src/rpc/Exp.java:160  a10[2] = "mac os x 10.4 tiger (x86 hackintosh)|инъекция kext-драйверов appleacpiplatform и патч mach_kernel по…
        src/rpc/Funcs.java:239  "win11 на железе 2005 года", "hackintosh на обычном pc",
OK 393. Android x86 на нетбуке.                                    hits=7
        src/rpc/CalendarHistory.java:65  a0[5] = "doom на всем • запустить doom на кпк, нетбуке и калькуляторе";
        src/rpc/Exp.java:100  a5[3] = "asus eee pad transformer tf101|nvidia tegra 2 (dual cortex-a9 1.0ghz)|1gb lpddr2|android 3.0 honeycom…
OK 394. Linux на калькуляторе.                                     hits=17
        src/rpc/CalendarHistory.java:21  a[11] = "1991 • линус торвальдс публикует первую версию ядра linux 0.01. распад ссср.";
        src/rpc/CalendarHistory.java:65  a0[5] = "doom на всем • запустить doom на кпк, нетбуке и калькуляторе";
OK 395. Doom на холодильнике.                                      hits=30
        src/rpc/AudioEngine.java:201  public void playdoomriff() {
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
OK 396. Windows 95 на ПК 2026.                                     hits=99
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 397. Игра в 4K на ЭЛТ.                                          hits=12
        src/rpc/Corp.java:41  a0[2] = "игра";
        src/rpc/Exp.java:43  a0[1] = "тяжёлая игра";
OK 398. Сервер из хлама.                                           hits=9
        src/rpc/City.java:42  a0[7] = "собеседования, созвоны, карьера от стажера до it-директора и серверные стойки.";
        src/rpc/City.java:94  a[2] = "3. «провести аудит серверной и устранить перегрев стоек»";
-- 399. ПК для бабушки.
-- 400. Крипто-ферма в гараже.
OK 401. Курс валют.                                                hits=7
        src/rpc/Browser.java:356  a9[0] = "музей: экскурсия";
        src/rpc/Browser.java:392  a0[6] = "курс кредита: стабилен";
-- 402. Инфляция.
OK 403. Кредит.                                                    hits=17
        src/rpc/Browser.java:392  a0[6] = "курс кредита: стабилен";
        src/rpc/Funcs.java:198  "взять кредит", "погасить кредит",
-- 404. Вклады.
OK 405. Акции.                                                     hits=2
        src/rpc/Browser.java:672  a2[8] = "вердикт редакции:";
        src/rpc/Funcs.java:206  "вложиться в акции", "продать акции",
-- 406. Налоги.
-- 407. Штрафы.
-- 408. Лотерея.
OK 409. Клад на свалке.                                            hits=5
        src/rpc/City.java:100  this.dialoguetext = "«вам посылка со склада микроэлектроники! распишитесь в квитанции.»";
        src/rpc/City.java:197  return "посылка вскрыта: внутри новенький чипсет и свежие термопрокладки!";
~  410. Аукцион.                                                   hits=1
        src/rpc/Funcs.java:201  "выставить раритет на аукцион", "поймать распродажу",
OK 411. Реальное время.                                            hits=3
        src/rpc/BuildView.java:405  case 4: return "промотать время";
        src/rpc/Funcs.java:151  "включить скринсейвер с рыбами", "настроить дату и время",
OK 412. Смена поколений.                                           hits=2
        src/rpc/Game.java:5864  "поколений")
        src/rpc/Game.java:5873  /** russian plural form for a count (1 поколение / 2 поколения / 5 поколений). */
-- 413. Семья (дети).
~  414. Наследие (сын продолжает дело).                            hits=1
        src/rpc/Browser.java:195  return "дешево, 20% подделок";
-- 415. Конец игры (2027).
OK 416. Новая игра+.                                               hits=17
        src/rpc/Browser.java:193  return "новая, гарантия";
        src/rpc/Corp.java:41  a0[2] = "игра";
OK 417. Мультиплеер (обмен деталями).                              hits=3
        src/rpc/Funcs.java:197  "купить плату на запчасти", "обменять деталь",
        src/rpc/Funcs.java:335  "обменяться контактами", "пойти в кино",
OK 418. Достижения.                                                hits=2
        src/rpc/Game.java:477  a[6] = "календарь 1980-2027 & достижения";
        src/rpc/Game.java:4662  string head = "достижения: " + done + "/"
-- 419. Статистика.
~  420. Эпилог.                                                    hits=1
        src/rpc/Game.java:4370  this.footer("ok: эпилог о 2027", "лск: лаб");
OK 421. Живые обои Windows 98.                                     hits=100
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 422. Анимированный курсор (шлейф).                              hits=2
        src/rpc/Funcs.java:164  "починить шлейф матрицы", "заменить подсветку",
        src/rpc/Funcs.java:311  "кот охотится на курсор", "кот на клавиатуре",
OK 423. Экран загрузки (синие квадратики).                         hits=8
        src/rpc/Funcs.java:153  "починить синий экран", "переустановить систему",
        src/rpc/Funcs.java:284  "поставить сетевой экран", "отбиться от ddos",
OK 424. Кнопка Turbo (светится).                                   hits=5
        src/rpc/Exp.java:115  a6[4] = "msi wind u100 (turbo oc)|intel atom n270 1.6ghz + кнопка turbo|1gb ddr2|windows xp home";
        src/rpc/Exp.java:151  a8[12] = "серверный xeon e5-2678 v3|intel xeon e5-2678 v3 12c/24t turbo unlock + gtx 1080 ti|32gb ddr3/ddr4 ec…
OK 425. Перегрев (артефакты на экране).                            hits=8
        src/rpc/Browser.java:221  a1[7] = "   будет перегрев и bsod.";
        src/rpc/City.java:94  a[2] = "3. «провести аудит серверной и устранить перегрев стоек»";
OK 426. Разбитый экран (трещина).                                  hits=6
        src/rpc/Funcs.java:153  "починить синий экран", "переустановить систему",
        src/rpc/Funcs.java:284  "поставить сетевой экран", "отбиться от ddos",
OK 427. Свет от монитора на лицо.                                  hits=7
        src/rpc/Funcs.java:164  "починить шлейф матрицы", "заменить подсветку",
        src/rpc/Funcs.java:165  "заменить лампу в мониторе", "настроить фокус монитора",
OK 428. Кабели за системником.                                     hits=2
        src/rpc/Funcs.java:112  "уложить кабели стяжками", "поставить вентилятор на вдув",
        src/rpc/Funcs.java:126  "спрятать кабели за лоток", "собрать тестовый стенд",
OK 429. Пыль в лучах света.                                        hits=16
        src/rpc/Browser.java:207  a0[6] = "4. пыль и ёмкости: опыт ремонта";
        src/rpc/Browser.java:634  a6[8] = "и tdp. и продувайте пыль.";
OK 430. Пиксельная графика.                                        hits=2
        src/rpc/Game.java:9661  a[3] = "тип: текст/графика";
        src/rpc/Game.java:9707  s = "графика при таком озу не поднимется";
OK 431. Фоновый звук улицы.                                        hits=17
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
OK 432. Звук шагов.                                                hits=17
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
-- 433. Скрип двери.
-- 434. Тиканье часов.
OK 435. Гроза (молния, потом гром).                                hits=4
        src/rpc/CalendarHistory.java:61  a0[1] = "гроза митино • сбить цену у продавца на 30% три раза подряд";
        src/rpc/LifeSim.java:68  a1[2] = "гроза";
OK 436. Щелчок дискеты.                                            hits=2
        src/rpc/SoftSim.java:57  this.floppystatus = "установка windows 95: чтение дискеты #1 (io.sys / msdos.sys)...";
        src/rpc/SoftSim.java:83  this.floppystatus = new stringbuffer("чтение файлов дискеты #").append(this.floppycurrent).append(" из ").appe…
~  437. Жужжание CD-ROM.                                           hits=1
        src/rpc/Browser.java:492  s1 = "cd-rom: 150 кб/с хватает всем";
-- 438. Скрип кресла.
OK 439. Звук набора текста.                                        hits=17
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
-- 440. Затухание вентиляторов.
-- 441. Вид из окна (дом, деревья, площадка).
~  442. Кот на подоконнике.                                        hits=1
        src/rpc/LifeSim.java:482  this.lasteventmsg = "вы полили цветок на подоконнике. зеленые листья расправились.";
OK 443. Вянущий цветок.                                            hits=2
        src/rpc/Funcs.java:220  "полить кактус", "полить фикус", "пересадить цветок",
        src/rpc/LifeSim.java:482  this.lasteventmsg = "вы полили цветок на подоконнике. зеленые листья расправились.";
OK 444. Кружка (хочет пить).                                       hits=26
        src/rpc/Browser.java:733  a2[9] = "3 — купить и на стол";
        src/rpc/Browser.java:744  a3[7] = "3 — купить выбранное";
~  445. Книги по электронике.                                      hits=1
        src/rpc/Funcs.java:337  "пойти в библиотеку", "взять книгу по электронике",
-- 446. Аквариум с рыбками.
OK 447. Телевизор (новости, мультики).                             hits=6
        src/rpc/Browser.java:23  a[2] = "новости it";
        src/rpc/Browser.java:386  a0[0] = new stringbuffer("новости it — ").append(i0).tostring();
OK 448. Пылесос в шкафу.                                           hits=3
        src/rpc/Acoustics.java:100  return "как пылесос";
        src/rpc/Funcs.java:219  "протереть пыль", "подмести пол", "пропылесосить",
~  449. Кровать (отдых).                                           hits=1
        src/rpc/Game.java:4137  string title = "отдых";
-- 450. Шкаф с одеждой.
-- 451. Сапёр.
-- 452. Косынка.
-- 453. Пинбол.
OK 454. Змейка на телефоне.                                        hits=2
        src/rpc/Funcs.java:241  "linux на калькуляторе", "dos на телефоне",
        src/rpc/Game.java:7530  return "гигабит, 4g в каждом телефоне";
-- 455. Тетрис на телевизоре.
OK 456. Сборка на время.                                           hits=11
        src/rpc/BuildView.java:405  case 4: return "промотать время";
        src/rpc/Exp.java:158  a10[0] = "windows 11 23h2 tiny11|сборка без tpm 2.0/secureboot, отключение winsxs, эмуляция acpi 5.0";
-- 457. Поймай пылинку.
OK 458. Почини провод.                                             hits=21
        src/rpc/City.java:130  this.dialoguetext = "«здорово! у нас 4-й комп виснет на третьей карте в doom. починишь по сетке — дам час бесп…
        src/rpc/Funcs.java:153  "починить синий экран", "переустановить систему",
OK 459. Установи Windows.                                          hits=116
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 460. Разгон.                                                    hits=15
        src/rpc/Browser.java:206  a0[5] = "3. разгон: стоит ли оно того";
        src/rpc/Browser.java:228  a2[0] = "тема: разгон";
OK 461. Windows 11 на ведре 2005.                                  hits=99
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 462. Hackintosh (OpenCore).                                     hits=3
        src/rpc/Exp.java:160  a10[2] = "mac os x 10.4 tiger (x86 hackintosh)|инъекция kext-драйверов appleacpiplatform и патч mach_kernel по…
        src/rpc/Funcs.java:239  "win11 на железе 2005 года", "hackintosh на обычном pc",
OK 463. Android x86.                                               hits=5
        src/rpc/Exp.java:100  a5[3] = "asus eee pad transformer tf101|nvidia tegra 2 (dual cortex-a9 1.0ghz)|1gb lpddr2|android 3.0 honeycom…
        src/rpc/Exp.java:102  a5[5] = "samsung galaxy tab 7.0 (p1000)|samsung hummingbird 1.0ghz|512mb ram|android 2.2 froyo";
OK 464. Windows 95 на ПК 2026.                                     hits=99
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 465. Linux на калькуляторе.                                     hits=17
        src/rpc/CalendarHistory.java:21  a[11] = "1991 • линус торвальдс публикует первую версию ядра linux 0.01. распад ссср.";
        src/rpc/CalendarHistory.java:65  a0[5] = "doom на всем • запустить doom на кпк, нетбуке и калькуляторе";
OK 466. Doom на принтере.                                          hits=30
        src/rpc/AudioEngine.java:201  public void playdoomriff() {
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
OK 467. Сервер из хлама.                                           hits=9
        src/rpc/City.java:42  a0[7] = "собеседования, созвоны, карьера от стажера до it-директора и серверные стойки.";
        src/rpc/City.java:94  a[2] = "3. «провести аудит серверной и устранить перегрев стоек»";
OK 468. Майнинг-ферма в гараже.                                    hits=2
        src/rpc/Funcs.java:258  "майнинг-ферма", "nas из старого пк",
        src/rpc/Market.java:27  a[6] = "майнинг-ферма";
-- 469. ПК для бабушки.
OK 470. Ретро-сборка 1998 (Doom/Quake).                            hits=36
        src/rpc/AudioEngine.java:201  public void playdoomriff() {
        src/rpc/Browser.java:349  a8[6] = "«в 1998-м это тянуло quake»";
OK 471. Реальное время (как тамагочи).                             hits=3
        src/rpc/BuildView.java:405  case 4: return "промотать время";
        src/rpc/Funcs.java:151  "включить скринсейвер с рыбами", "настроить дату и время",
-- 472. Сезоны.
OK 473. Смена поколений.                                           hits=2
        src/rpc/Game.java:5864  "поколений")
        src/rpc/Game.java:5873  /** russian plural form for a count (1 поколение / 2 поколения / 5 поколений). */
OK 474. Новый год 2027.                                            hits=2
        src/rpc/City.java:188  return "☕ вы взяли новый заказ на ремонт и выпили отличный кофе!";
        src/rpc/WorkshopTools.java:185  this.toolfeedback = new stringbuffer("✓ вздутый электролит c").append(this.selcap + 1).append(" выпаян, запаян…
~  475. Итоги.                                                     hits=1
        src/rpc/Game.java:4338  this.header("итоги 2027", "fin");
OK 476. Максимум графики.                                          hits=5
        src/rpc/Exp.java:59  a2[0] = "урезание графики, разрешения и эффектов до минимума";
        src/rpc/Exp.java:199  a13[1] = "4kb демосцена с процедурной музыкой и фракталами|упаковка 3d-графики, плазмы и трекерного синтезатор…
OK 477. Оптимизация через .pak.                                    hits=13
        src/rpc/Exp.java:66  a2[7] = "внешнее подключение шины через expresscard / sdio / m.2";
        src/rpc/Exp.java:175  a11[3] = "doom eternal (vulkan api)|эмуляция инструкций vulkan через vkd3d и direct3d 11 транслятор";
OK 478. Процедурный звук.                                          hits=17
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
-- 479. Автосохранение.
OK 480. Идеальная работа (без вылетов).                            hits=9
        src/rpc/Browser.java:338  a7[7] = "табличка: «работал без";
        src/rpc/Browser.java:376  a10[7] = "протереть ластиком и работает.";
OK 481. Забыл зонт.                                                hits=5
        src/rpc/Avatar.java:139  /** distance from the avatar to a point, horizontal only. */
        src/rpc/Browser.java:249  a3[7] = "забыл снять плёнку с кулера»";
OK 482. Мышь застряла.                                             hits=7
        src/rpc/Db.java:46  a[16] = "мышь";
        src/rpc/Funcs.java:313  "кот принёс мышь", "завести второго кота",
OK 483. Пыль на полке.                                             hits=16
        src/rpc/Browser.java:207  a0[6] = "4. пыль и ёмкости: опыт ремонта";
        src/rpc/Browser.java:634  a6[8] = "и tdp. и продувайте пыль.";
~  484. Кот скинул вазу.                                           hits=1
        src/rpc/Funcs.java:307  "кот лёг на радиатор", "кот скинул мышку",
OK 485. Муха в комнате.                                            hits=3
        src/rpc/Game.java:4144  ? "ночь. в комнате тихо."
        src/rpc/LifeSim.java:111  this.lasteventmsg = "за окном кружится легкий снег. в комнате тепло и пахнет чаем.";
OK 486. Соседский Wi-Fi.                                           hits=10
        src/rpc/Funcs.java:272  "настроить роутер", "сменить пароль wi-fi",
        src/rpc/Funcs.java:287  "скачать торрент", "раздать wi-fi с телефона",
OK 487. Забыл пароль от роутера.                                   hits=3
        src/rpc/Browser.java:249  a3[7] = "забыл снять плёнку с кулера»";
        src/rpc/Browser.java:439  a4[3] = "забыл про сокет — не влезло.";
-- 488. Текстуры пропали.
-- 489. Моделька застряла.
~  490. Бесконечные деньги (баг).                                  hits=1
        src/rpc/City.java:36  a0[1] = "бесконечные павильоны с чипами, сокетами, переходниками и мануалами.";
~  491. Сейв сломался.                                             hits=1
        src/rpc/Funcs.java:151  "включить скринсейвер с рыбами", "настроить дату и время",
OK 492. Игра не сворачивается.                                     hits=12
        src/rpc/Corp.java:41  a0[2] = "игра";
        src/rpc/Exp.java:43  a0[1] = "тяжёлая игра";
OK 493. Синий экран из-за модов.                                   hits=7
        src/rpc/Browser.java:466  s0 = "видеопамять дорожает из-за майнинга";
        src/rpc/Funcs.java:153  "починить синий экран", "переустановить систему",
OK 494. Драйвер видеокарты слетел.                                 hits=21
        src/rpc/Browser.java:343  a8[0] = "зал: видеокарты";
        src/rpc/Browser.java:604  a4[6] = "видеокарты:";
OK 495. Звук пропал.                                               hits=17
        src/rpc/Browser.java:491  s0 = "звук: sound blaster стал стандартом";
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
OK 496. Курсор завис.                                              hits=2
        src/rpc/Browser.java:547  a3[3] = "энергозависима и нужна всегда.";
        src/rpc/Funcs.java:311  "кот охотится на курсор", "кот на клавиатуре",
-- 497. Автосохранение не сработало.
OK 498. Сгорел блок питания.                                       hits=22
        src/rpc/Browser.java:463  s1 = "блоки питания по новому стандарту";
        src/rpc/CalendarHistory.java:39  a[29] = "2009 • выход популярнейшей операционной системы windows 7 и майнинг первого блока bitcoin.";
OK 499. Вздулись конденсаторы.                                     hits=6
        src/rpc/City.java:143  a4[1] = "2. «сбить цену вдвое: тут же конденсаторы вздуты!»";
        src/rpc/Diag.java:198  a0[i5] = new rpc.diag$fault(8, 1, new stringbuffer("вздулись конденсаторы (").append(rpc.diag.capsleft(a2)).ap…
OK 500. Термопаста засохла.                                        hits=5
        src/rpc/Funcs.java:251  "термопаста из зубной пасты", "майонез вместо пасты",
        src/rpc/Game.java:5985  "пайка, термопаста, пыль, конденсаторы",
