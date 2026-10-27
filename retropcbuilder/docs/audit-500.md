# Audit of the 500 features against the sources

Sources scanned: 82 java files, `\uXXXX` escapes decoded,
words folded Cyrillic -> Latin so that Russian strings and
English identifiers both match.

**352 of 500 items have a textual match in the code**, 148 items
have no keyword anywhere in the sources (they are candidates
for implementation, or they live behind a generic entry).

--   1. Несовпадение крепежа (винты M3 и саморезы).
OK   2. Залипание сокета (гнутые ножки, выпрямлять пинцетом).      hits=12
        src/rpc/CalendarHistory.java:62  a0[2] = "пинцет-ювелир • выпрямить 100 погнутых ножек сокета без отлома";
        src/rpc/WorkshopTools.java:29  a[0] = "пинцет (ножки cpu)";
OK   3. Длина кабелей (короткий кабель питания CPU).               hits=24
        src/rpc/Score.java:44  a[10] = "блок питания";
        src/rpc/Score.java:468  a0[i1] = "не выбран блок питания";
OK   4. Перепутанные контакты Power/Reset.                         hits=64
        src/rpc/Shell.java:42  this.reset(0, "ms-dos", 640, 8, 20);
        src/rpc/Shell.java:62  this.reset(i, s, i0, i1, i2);
OK   5. Термопаста-изолятор (много/мало — перегрев).               hits=15
        src/rpc/Game.java:531  a[0] = this.makeorder("иван (офис)", "пк перегревается и вырубается", 0, 1800, 40, i, "прочистить кулер от пыл…
        src/rpc/Game.java:2898  this.g.drawtext(new stringbuffer("! перегрев: ").append(this.expstress.temp).append(" c !").tostring(), i + 4,…
--   6. Статическое электричество (собирать на ковре опасно).
OK   7. Пыль как ресурс (забивает кулеры).                         hits=18
        src/rpc/WorkshopTools.java:32  a[3] = "сжатый воздух (пыль)";
        src/rpc/WorkshopTools.java:127  this.toolfeedback = new stringbuffer("пыль сдувается... осталось ").append(this.dustremaining).append("% пыли …
OK   8. Кривой запуск кулера (3-pin в 4-pin).                      hits=28
        src/rpc/Shell.java:519  stringbuffer a = new stringbuffer("запуск ");
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
OK   9. Звук парковки головок HDD.                                 hits=17
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
OK  10. Вздутие конденсаторов от времени.                          hits=4
        src/rpc/CalendarHistory.java:48  a[38] = "2018 • аппаратная трассировка лучей в реальном времени (nvidia rtx 2080 ray tracing).";
        src/rpc/CalendarHistory.java:63  a0[3] = "паяльный спец • заменить 50 вздутых low-esr конденсаторов";
OK  11. Синий экран смерти (BSOD) с таймером.                      hits=31
        src/rpc/Game.java:55  final public static int m_bsod = 23;
        src/rpc/Game.java:293  private int bsodcode;
OK  12. Лицензионные ключи.                                        hits=22
        src/rpc/Score.java:639  a0[i23] = new stringbuffer("старый ").append(s0).append("-монитор не подключить к новой карте").tostring();
        src/rpc/Game.java:10158  this.footer("ok: пропустить загрузку", "1: выключить");
OK  13. Поиск драйверов.                                           hits=10
        src/rpc/Game.java:8061  a[0] = new stringbuffer("этап: ").append((i0 >= 20) ? (i0 >= 60) ? (i0 >= 90) ? "первая загрузка" : "установка…
        src/rpc/Browser.java:30  a[9] = "поиск";
OK  14. Установка Windows 95 с 20 дискет.                          hits=144
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
--  15. Кракозябры в DOS.
OK  16. Обрыв Dial-up.                                             hits=4
        src/rpc/Game.java:6449  return "dial-up через телефонную линию";
        src/rpc/Game.java:7154  return "56k dial-up, первые сайты";
--  17. Дефрагментация HDD.
~   18. Чистка реестра.                                            hits=1
        src/rpc/Funcs.java:183  "ультразвуковая чистка платы"
--  19. Конфликт IRQ.
OK  20. Сброс пароля BIOS через батарейку CMOS.                    hits=63
        src/rpc/Os.java:62  a[2] = rpc.os.task(0, 21, "сумма 1..6 через цикл", 60 + (i - 1980) * 3);
        src/rpc/Setup.java:21  return new stringbuffer(string.valueof((object)string.valueof((object)s))).append(" bios ").append((i >= 1996)…
OK  21. Защита от пиратства (требует диск).                        hits=76
        src/rpc/Shell.java:18  private int diskmb;
        src/rpc/Shell.java:36  this.diskmb = 20;
~   22. No-CD кряк.                                                hits=1
        src/rpc/City.java:134  a3[2] = "3. «поставить no-cd патч на игры клуба»";
OK  23. Нехватка места на диске.                                   hits=13
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/SoftSim.java:32  this.floppystatus = "вставьте дискету #1 «microsoft setup disk 1» в привод a:";
--  24. Тормоза в 3D (низкий FPS).
OK  25. Залипание шариковой мыши.                                  hits=3
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/Funcs.java:167  "пропаять джек наушников", "заменить кнопки мыши",
~   26. Битая дискета с сейвом.                                    hits=1
        src/rpc/SoftSim.java:72  this.floppystatus = new stringbuffer("✓ дискета #").append(this.floppycurrent).append(" прочитана! вставьте ди…
OK  27. Doom на калькуляторе.                                      hits=31
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
        src/rpc/CalendarHistory.java:34  a[24] = "2004 • релиз half-life 2, world of warcraft, doom 3 и стандарта шины pci express.";
OK  28. Сгоревший пиксель на мониторе.                             hits=2
        src/rpc/Exp.java:179  a11[7] = "half-life 2 (directx 7 fallback)|принудительный запуск source engine в режиме mat_dxlevel 70 без пик…
        src/rpc/Funcs.java:165  "заменить лампу в мониторе", "настроить фокус монитора",
~   29. Скачивание патча на 56k модеме.                            hits=1
        src/rpc/Game.java:7609  this.toastmsg(new stringbuffer("симка в модеме: до ").append(rpc.game.mobilespeed(i)).append(" мбит/с (").appe…
OK  30. Установка эмулятора Dendy.                                 hits=13
        src/rpc/Game.java:7529  this.toastmsg((i <= 0) ? "установка началась" : "установка с обходом проверок");
        src/rpc/Game.java:7635  this.toastmsg("сначала установите ос (пункт «установка ос»)");
OK  31. Обман на барахолке.                                        hits=2
        src/rpc/Funcs.java:187  "торг на барахолке", "сторговать десять процентов",
        src/rpc/Funcs.java:188  "сторговать треть", "заметить обман продавца",
OK  32. Торг с продавцом.                                          hits=13
        src/rpc/City.java:54  a[1] = "2. «скинь 30% — я постоянный клиент!» (торг)";
        src/rpc/City.java:153  a5[1] = "2. «давай поторгуемся на партию конденсаторов!»";
--  33. Инфляция.
OK  34. Курс валют.                                                hits=7
        src/rpc/Game.java:7180  a[1] = new stringbuffer("курс сети: ").append(this.netkind()).tostring();
        src/rpc/Browser.java:356  a9[0] = "музей: экскурсия";
~   35. Кража на почте.                                            hits=1
        src/rpc/City.java:102  a0[0] = "1. «вскрыть коробку прямо на почте (проверка на кирпич)»";
OK  36. Гарантия в магазине.                                       hits=4
        src/rpc/Browser.java:193  return "новая, гарантия";
        src/rpc/Browser.java:636  a6[10] = "гарантия: 12 месяцев";
--  37. Распродажи (Черная пятница).
~   38. Скупка лома.                                               hits=1
        src/rpc/CalendarHistory.java:62  a0[2] = "пинцет-ювелир • выпрямить 100 погнутых ножек сокета без отлома";
OK  39. Кредит в банке.                                            hits=17
        src/rpc/Game.java:2243  this.toastmsg(new stringbuffer("не хватает кредитов: нужно ").append(a.price).append(", есть ").append(this.ca…
        src/rpc/Game.java:6079  this.toastmsg(new stringbuffer("ремонт стоит ").append(i0).append(", а кредитов ").append(this.cash).tostring(…
OK  40. Майнинг-ферма.                                             hits=2
        src/rpc/Market.java:27  a[6] = "майнинг-ферма";
        src/rpc/Funcs.java:258  "майнинг-ферма", "nas из старого пк",
--  41. Найм сотрудников.
~   42. Утечка данных.                                             hits=1
        src/rpc/Browser.java:541  a2[5] = "долговременное хранение данных:";
--  43. Пиратство (торренты).
OK  44. Реклама на BBS.                                            hits=3
        src/rpc/Game.java:8870  a[8] = new stringbuffer("реклама: уровень ").append(this.corp.marketing).tostring();
        src/rpc/Game.java:8914  a0[5] = "лаборатория, реклама и тесты.";
--  45. Баг-репорты.
OK  46. Хейтеры в интернете.                                       hits=2
        src/rpc/Game.java:7486  this.toastmsg("нужен патч-обход: скачайте в интернете");
        src/rpc/Game.java:7627  this.toastmsg("сначала скачайте doom в интернете");
OK  47. Антивирус находит вирус.                                   hits=3
        src/rpc/Inst.java:331  a[i4] = rpc.inst.dl("антивирус", 1800, 1996, 0, 0, "чистит и находит «заражённые» файлы");
        src/rpc/Funcs.java:141  "удалить вирус", "поставить антивирус",
--  48. Слияние компаний.
~   49. Роялти за ОС.                                              hits=1
        src/rpc/Game.java:8867  a[5] = "начать день (+роялти)";
--  50. Кража серверов.
OK  51. Светодиоды на корпусе.                                     hits=2
        src/rpc/LifeSim.java:173  this.lasteventmsg = "кот барсик уютно свернулся клубочком на теплом корпусе пк.";
        src/rpc/Funcs.java:248  "собрать пк в корпусе тостера", "собрать пк в корпусе nes",
OK  52. Звук открытия CD-ROM.                                      hits=18
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
OK  53. Перекрученный кабель мыши.                                 hits=8
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/Game.java:6455  return "кабельный интернет";
--  54. Гул трансформатора ЭЛТ.
OK  55. Коврик для мыши.                                           hits=4
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/Funcs.java:167  "пропаять джек наушников", "заменить кнопки мыши",
OK  56. Кнопка Turbo на корпусе.                                   hits=7
        src/rpc/LifeSim.java:173  this.lasteventmsg = "кот барсик уютно свернулся клубочком на теплом корпусе пк.";
        src/rpc/Ports.java:390  a[0] = "кнопка power";
OK  57. Блокировка Power (держать 5 сек).                          hits=45
        src/rpc/Score.java:382  public static int needpower(rpc.part a, rpc.part a0) {
        src/rpc/Score.java:556  int i19 = rpc.score.needpower(a1, a3);
OK  58. Время суток за окном.                                      hits=4
        src/rpc/Shell.java:458  this.println((s1.equals((object)"time")) ? "текущее время: 12:00" : "дата: 01-01-1990");
        src/rpc/LifeSim.java:111  this.lasteventmsg = "за окном кружится легкий снег. в комнате тепло и пахнет чаем.";
OK  59. Кот в комнате.                                             hits=3
        src/rpc/LifeSim.java:111  this.lasteventmsg = "за окном кружится легкий снег. в комнате тепло и пахнет чаем.";
        src/rpc/LifeSim.java:362  this.lasteventmsg = "вы заварили свежий горячий чай с лимоном. по комнате пошёл пар.";
OK  60. Мусор на столе.                                            hits=2
        src/rpc/City.java:112  a1[0] = "1. «искать рабочие процессоры и видеокарты среди мусора»";
        src/rpc/Market.java:24  a[3] = "мусорка";
OK  61. Осциллограф.                                               hits=2
        src/rpc/Funcs.java:173  "заменить вздутые банки", "проверить осциллографом",
        src/rpc/Funcs.java:328  "купить осциллограф", "поговорить с продавцом",
OK  62. Тестер ёмкости.                                            hits=8
        src/rpc/Game.java:7329  this.toastmsg((i2 <= 0) ? "вздутых ёмкостей нет" : new stringbuffer("перепаял ёмкости: +").append(i2).append("…
        src/rpc/Game.java:7429  this.toastmsg(new stringbuffer("перепаял ёмкости: +").append(i1).append("% (").append(a0.name).append(")").tos…
OK  63. Перепайка ножек процессора.                                hits=15
        src/rpc/CalendarHistory.java:14  a[4] = "1984 • релиз apple macintosh 128k с графическим интерфейсом и процессора intel 80286.";
        src/rpc/CalendarHistory.java:16  a[6] = "1986 • анонс 32-битного процессора intel 80386dx. начало эры защищенного режима.";
OK  64. Залипание кнопки Power.                                    hits=46
        src/rpc/Score.java:382  public static int needpower(rpc.part a, rpc.part a0) {
        src/rpc/Score.java:556  int i19 = rpc.score.needpower(a1, a3);
OK  65. Замена предохранителя БП.                                  hits=3
        src/rpc/Game.java:7413  this.toastmsg("диск щёлкает — только замена");
        src/rpc/Exp.java:168  a10[10] = "reactos 0.4.15 (open-source nt)|открытая замена windows nt с нативным запуском win32 софта";
OK  66. Высохшая термопаста.                                       hits=5
        src/rpc/WorkshopTools.java:31  a[2] = "термопаста (слой кпт-8)";
        src/rpc/WorkshopTools.java:176  this.toolfeedback = new stringbuffer("! слишком толстый слой (").append(this.pastethickness).append("%). термо…
OK  67. Шум подшипника кулера.                                     hits=6
        src/rpc/WorkshopTools.java:129  this.toolfeedback = "✓ радиаторы и лопасти кулера идеально очищены от пыли!";
        src/rpc/Game.java:3121  this.g.drawtext("нажмите [ok/тап] для смены кулера", 8, i10 + this.fh0 + 3, 0);
OK  68. Вздутие батарейки CMOS.                                    hits=22
        src/rpc/WorkshopTools.java:9  final public static int tool_cmos = 5;
        src/rpc/WorkshopTools.java:24  public boolean cmosresetdone;
OK  69. Скрученные контакты PCIe.                                  hits=26
        src/rpc/Score.java:170  a11[8] = "pcie";
        src/rpc/Score.java:171  a11[9] = "pcie2";
--  70. Битые сектора HDD.
OK  71. Занятая телефонная линия.                                  hits=2
        src/rpc/WorkshopTools.java:86  this.toolfeedback = "линия +12v просажена до 10.85v, пульсации 140мв. бп требует ремонта.";
        src/rpc/Game.java:6452  return "adsl / выделенная линия";
OK  72. Помехи в линии.                                            hits=2
        src/rpc/WorkshopTools.java:121  this.toolfeedback = new stringbuffer("✓ линии питания в норме: +12v (").append(this.v12).append("v), +5v (").a…
        src/rpc/Game.java:7119  a[2] = new stringbuffer("тип линии: ").append(this.netkind()).tostring();
OK  73. Настройка локальной сети.                                  hits=14
        src/rpc/Game.java:534  a[3] = this.makeorder("мария (бухгалтерия)", "не включается после скачка сети", 3, 2900, 55, i, "заменить неис…
        src/rpc/Game.java:7157  return "adsl и локальные сети";
--  74. Хаб vs Коммутатор.
--  75. Кривой патч-корд.
OK  76. FTP-сервер на старом ПК.                                   hits=2
        src/rpc/Inst.java:319  a[i2] = rpc.inst.dl("патч обхода cpu", 1500, 2015, 2, 1, "10/11 на старом камне");
        src/rpc/Funcs.java:275  "поднять ftp-сервер", "поднять http-сервер",
OK  77. Докачка файлов.                                            hits=6
        src/rpc/Shell.java:428  this.println(new stringbuffer("файлов в системе: ").append(this.files()).tostring());
        src/rpc/Shell.java:441  this.println(new stringbuffer("  файлов: ").append(this.files()).append(", каталогов: ").append(this.nodes - t…
OK  78. Вирус из сети.                                             hits=15
        src/rpc/Game.java:534  a[3] = this.makeorder("мария (бухгалтерия)", "не включается после скачка сети", 3, 2900, 55, i, "заменить неис…
        src/rpc/Game.java:7157  return "adsl и локальные сети";
OK  79. Провайдер отключил интернет.                               hits=46
        src/rpc/Shell.java:502  this.println("установлено 0 новых пакетов (интернета нет)");
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
~   80. Подключение к BBS.                                         hits=1
        src/rpc/Exp.java:66  a2[7] = "внешнее подключение шины через expresscard / sdio / m.2";
OK  81. Подсветка кулера.                                          hits=7
        src/rpc/WorkshopTools.java:129  this.toolfeedback = "✓ радиаторы и лопасти кулера идеально очищены от пыли!";
        src/rpc/Game.java:3121  this.g.drawtext("нажмите [ok/тап] для смены кулера", 8, i10 + this.fh0 + 3, 0);
~   82. Покраска корпуса.                                          hits=1
        src/rpc/Funcs.java:113  "поставить вентилятор на выдув", "наклеить ножки корпуса",
OK  83. Вырезание окна в корпусе.                                  hits=2
        src/rpc/LifeSim.java:173  this.lasteventmsg = "кот барсик уютно свернулся клубочком на теплом корпусе пк.";
        src/rpc/Funcs.java:248  "собрать пк в корпусе тостера", "собрать пк в корпусе nes",
~   84. Снятие антибликовой пленки.                                hits=1
        src/rpc/Exp.java:188  a12[2] = "скальпирование (delid) и жидкий металл|снятие теплораспределительной крышки ihs, нанесение gallium l…
OK  85. Тефлоновые ножки для мыши.                                 hits=8
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/WorkshopTools.java:29  a[0] = "пинцет (ножки cpu)";
OK  86. Смазка свитчей клавиатуры.                                 hits=3
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/Exp.java:200  a13[2] = "своя ос реального времени на c и ассемблере|написание mbr загрузчика, переход в защищенный 32-бит ре…
--  87. Кабель-менеджмент.
--  88. Наклейки из журналов.
--  89. Подписывание кнопок маркером.
OK  90. Самодельный кулер из бутылки.                              hits=28
        src/rpc/Score.java:68  a0[12] = "кулер";
        src/rpc/Score.java:577  a0[i16] = new stringbuffer("кулер не тянет cpu (").append(a6.f[1]).append(" вт < ").append(a1.f[3]).append(" в…
OK  91. Пролитый кофе на клавиатуру.                               hits=8
        src/rpc/City.java:39  a0[4] = "уютный столик, горячий кофе, сплетни о новинках it и заказы от клиентов.";
        src/rpc/City.java:120  this.dialoguetext = "«ваш кофе готов! а вот тот солидный мужчина ищет мастера для апгрейда пк бухгалтерии.»";
OK  92. Выдернутый шнур питания.                                   hits=17
        src/rpc/Score.java:44  a[10] = "блок питания";
        src/rpc/Score.java:468  a0[i1] = "не выбран блок питания";
~   93. Кот на клавиатуре (Alt+F4).                                hits=1
        src/rpc/Funcs.java:311  "кот охотится на курсор", "кот на клавиатуре",
OK  94. Забитый пылью радиатор.                                    hits=12
        src/rpc/WorkshopTools.java:101  this.toolfeedback = "радиатор забит войлоком пыли. продуйте баллоном и нанесите свежую пасту.";
        src/rpc/WorkshopTools.java:129  this.toolfeedback = "✓ радиаторы и лопасти кулера идеально очищены от пыли!";
OK  95. Мусор в CD-ROM.                                            hits=3
        src/rpc/City.java:112  a1[0] = "1. «искать рабочие процессоры и видеокарты среди мусора»";
        src/rpc/Browser.java:492  s1 = "cd-rom: 150 кб/с хватает всем";
~   96. Сгоревший USB-порт.                                        hits=1
        src/rpc/Funcs.java:175  "починить usb-порт", "перепаять hdmi",
OK  97. Слетевшая Windows.                                         hits=93
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
--  98. Удаленный ярлык.
OK  99. Забытый пароль BIOS.                                       hits=32
        src/rpc/Setup.java:21  return new stringbuffer(string.valueof((object)string.valueof((object)s))).append(" bios ").append((i >= 1996)…
        src/rpc/Setup.java:57  a0[0] = new stringbuffer("bios: ").append(rpc.setup.name(a)).tostring();
~  100. Сосед с перфоратором.                                      hits=1
        src/rpc/City.java:124  a2[2] = "3. «послушать сплетни за соседним столиком»";
OK 101. Секретные коды в BIOS.                                     hits=32
        src/rpc/Setup.java:21  return new stringbuffer(string.valueof((object)string.valueof((object)s))).append(" bios ").append((i >= 1996)…
        src/rpc/Setup.java:57  a0[0] = new stringbuffer("bios: ").append(rpc.setup.name(a)).tostring();
OK 102. Doom в 8K.                                                 hits=30
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
        src/rpc/CalendarHistory.java:34  a[24] = "2004 • релиз half-life 2, world of warcraft, doom 3 и стандарта шины pci express.";
OK 103. Код 5318008 на калькуляторе.                               hits=3
        src/rpc/CalendarHistory.java:65  a0[5] = "doom на всем • запустить doom на кпк, нетбуке и калькуляторе";
        src/rpc/Funcs.java:241  "linux на калькуляторе", "dos на телефоне",
OK 104. Секретный магазин на свалке.                               hits=5
        src/rpc/Game.java:6677  this.g.drawtext("или поищите на свалке.", 8, i0 + 24 + this.fh0, 0);
        src/rpc/Browser.java:730  a2[6] = new stringbuffer("цена: ").append(i3).append(" (в магазине ").append(a1.price).append(")").tostring();
OK 105. Фирменная мелодия Sound Blaster.                           hits=158
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
-- 106. Сгорание ПК от молнии.
-- 107. Динозаврик (404).
OK 108. Замедление через Turbo.                                    hits=15
        src/rpc/Os.java:62  a[2] = rpc.os.task(0, 21, "сумма 1..6 через цикл", 60 + (i - 1980) * 3);
        src/rpc/Ports.java:391  a[1] = "turbo";
OK 109. Скрытая ОС (Linux).                                        hits=15
        src/rpc/CalendarHistory.java:21  a[11] = "1991 • линус торвальдс публикует первую версию ядра linux 0.01. распад ссср.";
        src/rpc/Corp.java:354  return (i0 >= 2000) ? "microsoft / linux" : "microsoft / ibm";
OK 110. Разгон до сгорания.                                        hits=15
        src/rpc/CalendarHistory.java:28  a[18] = "1998 • релиз windows 98, half-life и легендарного хита разгона celeron 300a (450 mhz!).";
        src/rpc/CalendarHistory.java:41  a[31] = "2011 • легендарный разгон sandy bridge (core i7-2600k 5.0 ghz) и дебют skyrim.";
OK 111. Дождь, снег, листья за окном.                              hits=4
        src/rpc/LifeSim.java:67  a1[1] = "дождь";
        src/rpc/LifeSim.java:69  a1[3] = "снегопад";
OK 112. Свет из окна на стол.                                      hits=20
        src/rpc/LifeSim.java:343  this.lasteventmsg = "кот барсик прыгнул на стол и улёгся рядом с клавиатурой!";
        src/rpc/LifeSim.java:401  this.lasteventmsg = "вы пропылесосили ковер, вынесли банки и протерли стол от пыли. чистота!";
OK 113. Ламповый свет.                                             hits=4
        src/rpc/LifeSim.java:416  this.lasteventmsg = (this.lampon) ? "настольная лампа зажглась теплым ламповым светом." : "настольная лампа вы…
        src/rpc/Funcs.java:164  "починить шлейф матрицы", "заменить подсветку",
OK 114. Радиоприёмник в комнате.                                   hits=4
        src/rpc/LifeSim.java:111  this.lasteventmsg = "за окном кружится легкий снег. в комнате тепло и пахнет чаем.";
        src/rpc/LifeSim.java:362  this.lasteventmsg = "вы заварили свежий горячий чай с лимоном. по комнате пошёл пар.";
OK 115. Звук дождя по подоконнику.                                 hits=17
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
~  116. Плед и кружка с паром.                                     hits=1
        src/rpc/LifeSim.java:391  this.lasteventmsg = "вы сладко выспались под теплым пледом. наступило бодрое утро!";
-- 117. Шум холодильника.
-- 118. Разговоры за стеной.
~  119. Осенние листья.                                            hits=1
        src/rpc/LifeSim.java:409  this.lasteventmsg = "вы полили цветок на подоконнике. зеленые листья расправились.";
-- 120. Морозные узоры зимой.
OK 121. Выход на улицу (магазин, рынок).                           hits=26
        src/rpc/Shell.java:537  this.println("выход в меню системы.");
        src/rpc/CalendarHistory.java:12  a[2] = "1982 • выход культового домашнего компьютера commodore 64 и журнала «радио» с рк-86.";
OK 122. Встреча с продавцом (скидка).                              hits=2
        src/rpc/CalendarHistory.java:57  a[47] = "2027 • встреча нового 2027 года! великий финал эпохи и вечная слава мастеру!";
        src/rpc/Funcs.java:328  "купить осциллограф", "поговорить с продавцом",
OK 123. Сосед просит починить ПК.                                  hits=19
        src/rpc/Game.java:533  a[2] = this.makeorder("сергей (инженер)", "пк пищит спикером и не включается", 2, 3400, 65, i, "найти неисправ…
        src/rpc/City.java:124  a2[2] = "3. «послушать сплетни за соседним столиком»";
OK 124. Погода влияет на рынок.                                    hits=46
        src/rpc/CalendarHistory.java:61  a0[1] = "гроза митино • сбить цену у продавца на 30% три раза подряд";
        src/rpc/LifeSim.java:14  final public static int weather_clear = 0;
OK 125. Дорога на рынок (время).                                   hits=14
        src/rpc/Shell.java:458  this.println((s1.equals((object)"time")) ? "текущее время: 12:00" : "дата: 01-01-1990");
        src/rpc/CalendarHistory.java:61  a0[1] = "гроза митино • сбить цену у продавца на 30% три раза подряд";
-- 126. Поездка на автобусе.
OK 127. Кафе на углу.                                              hits=4
        src/rpc/Game.java:467  a[2] = "выход в город (митино, клуб, кафе)";
        src/rpc/City.java:29  a[4] = "☕ кафе на углу";
OK 128. Встреча с другом (пиратский диск).                         hits=74
        src/rpc/Shell.java:18  private int diskmb;
        src/rpc/Shell.java:36  this.diskmb = 20;
~  129. Находка выброшенного ПК.                                   hits=1
        src/rpc/City.java:182  return "находка! вы откопали винтажную материнскую плату и кулер с медным сердечником!";
-- 130. Ночная прогулка.
OK 131. Реальный календарь (47 лет).                               hits=2
        src/rpc/Game.java:471  a[6] = "календарь 1980-2027 & достижения";
        src/rpc/Game.java:482  t[6] = "календарь";
OK 132. Ускорение времени в меню.                                  hits=8
        src/rpc/Shell.java:506  this.println("редактор доступен в меню ос: «редактор кода».");
        src/rpc/Shell.java:524  this.println("программа отработала (вывод в меню «программы»).");
OK 133. Сезоны (влияют на комнату и цены).                         hits=5
        src/rpc/Game.java:4367  this.footer("←→ год", "ok: в комнату");
        src/rpc/City.java:168  return new stringbuffer("сплетня: «говорят, в ").append(a.curyear + 1).append(" году цены на память ddr подско…
OK 134. Праздники (Новый год, 8 марта, 23 февраля).                hits=2
        src/rpc/WorkshopTools.java:185  this.toolfeedback = new stringbuffer("✓ вздутый электролит c").append(this.selcap + 1).append(" выпаян, запаян…
        src/rpc/City.java:188  return "☕ вы взяли новый заказ на ремонт и выпили отличный кофе!";
OK 135. Финал: Новый 2027 год.                                     hits=529
        src/rpc/Shell.java:4  final public static int os_dos = 0;
        src/rpc/Shell.java:5  final public static int os_unix = 1;
OK 136. День рождения героя.                                       hits=46
        src/rpc/LifeSim.java:55  a[1] = "день";
        src/rpc/Corp$Chip.java:6  public int nodenm;
-- 137. Отпуск.
OK 138. Заказы на праздники.                                       hits=8
        src/rpc/Game.java:5626  "basic / asm / c • заказы • майнинг",
        src/rpc/Game.java:5659  return "доставка: заказы";
OK 139. Выход на пенсию (к 2026).                                  hits=10
        src/rpc/Shell.java:537  this.println("выход в меню системы.");
        src/rpc/CalendarHistory.java:12  a[2] = "1982 • выход культового домашнего компьютера commodore 64 и журнала «радио» с рк-86.";
OK 140. Итоги года.                                                hits=20
        src/rpc/CalendarHistory.java:57  a[47] = "2027 • встреча нового 2027 года! великий финал эпохи и вечная слава мастеру!";
        src/rpc/Game.java:4301  text.draw(this.g, "событие года", 9, ey + 3, 0,
-- 141. Диалоги с покупателями.
OK 142. Торг.                                                      hits=12
        src/rpc/City.java:54  a[1] = "2. «скинь 30% — я постоянный клиент!» (торг)";
        src/rpc/City.java:153  a5[1] = "2. «давай поторгуемся на партию конденсаторов!»";
OK 143. Сплетни на рынке (новинки следующего года).                hits=25
        src/rpc/CalendarHistory.java:57  a[47] = "2027 • встреча нового 2027 года! великий финал эпохи и вечная слава мастеру!";
        src/rpc/Game.java:4301  text.draw(this.g, "событие года", 9, ey + 3, 0,
OK 144. Заказ по телефону.                                         hits=27
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
        src/rpc/Game.java:5626  "basic / asm / c • заказы • майнинг",
OK 145. Переписка на Avito.                                        hits=9
        src/rpc/Game.java:5656  return "рынки: базар, барахолка, avito";
        src/rpc/Browser.java:10  final public static int s_avito = 6;
OK 146. Отзывы в интернете.                                        hits=2
        src/rpc/Game.java:7486  this.toastmsg("нужен патч-обход: скачайте в интернете");
        src/rpc/Game.java:7627  this.toastmsg("сначала скачайте doom в интернете");
-- 147. Друзья в BBS/ICQ.
-- 148. Учитель информатики в 80-х.
OK 149. Конкурент через дорогу.                                    hits=11
        src/rpc/Os.java:62  a[2] = rpc.os.task(0, 21, "сумма 1..6 через цикл", 60 + (i - 1980) * 3);
        src/rpc/Game.java:5922  this.toastmsg(new stringbuffer("готовый системник из ").append(a.length).append(" деталей — приедет через ").a…
OK 150. Истории у костра на свалке.                                hits=2
        src/rpc/LifeSim.java:209  this.lasteventmsg = new stringbuffer("с новым ").append(this.curyear).append(" годом! новая эпоха в истории it…
        src/rpc/Game.java:6677  this.g.drawtext("или поищите на свалке.", 8, i0 + 24 + this.fh0, 0);
OK 151. Телевизор (новости, фильмы).                               hits=6
        src/rpc/LifeSim.java:105  a[4] = "радиопередача • новости технологий";
        src/rpc/Browser.java:23  a[2] = "новости it";
OK 152. Журналы (Радио, Игромания, Хакер).                         hits=50
        src/rpc/CalendarHistory.java:12  a[2] = "1982 • выход культового домашнего компьютера commodore 64 и журнала «радио» с рк-86.";
        src/rpc/LifeSim.java:43  public boolean radioon;
-- 153. Музыка на кассете/CD.
~  154. Радиопередача (попса 80-х, рейв 90-х, рок 2000-х).         hits=1
        src/rpc/LifeSim.java:105  a[4] = "радиопередача • новости технологий";
-- 155. Кинотеатр.
OK 156. Компьютерный клуб 90-х.                                    hits=10
        src/rpc/LifeSim.java:173  this.lasteventmsg = "кот барсик уютно свернулся клубочком на теплом корпусе пк.";
        src/rpc/Game.java:467  a[2] = "выход в город (митино, клуб, кафе)";
~  157. Книги по электронике.                                      hits=1
        src/rpc/Funcs.java:337  "пойти в библиотеку", "взять книгу по электронике",
OK 158. Плакаты на стене.                                          hits=3
        src/rpc/j2me/HostImpl.java:7  import javax.microedition.lcdui.commandlistener;
        src/rpc/j2me/HostImpl.java:133  * blocked on the lock object that the command listener notifies.
-- 159. Мемы 2010-х.
-- 160. Итоги десятилетия.
OK 161. Сборка ПК на время.                                        hits=11
        src/rpc/Shell.java:458  this.println((s1.equals((object)"time")) ? "текущее время: 12:00" : "дата: 01-01-1990");
        src/rpc/Game.java:466  a[1] = "песочница: сборка пк (1979-2027)";
OK 162. Чистка от пыли.                                            hits=12
        src/rpc/LifeSim.java:401  this.lasteventmsg = "вы пропылесосили ковер, вынесли банки и протерли стол от пыли. чистота!";
        src/rpc/WorkshopTools.java:101  this.toolfeedback = "радиатор забит войлоком пыли. продуйте баллоном и нанесите свежую пасту.";
OK 163. Разгон (шкала напряжения).                                 hits=16
        src/rpc/CalendarHistory.java:28  a[18] = "1998 • релиз windows 98, half-life и легендарного хита разгона celeron 300a (450 mhz!).";
        src/rpc/CalendarHistory.java:41  a[31] = "2011 • легендарный разгон sandy bridge (core i7-2600k 5.0 ghz) и дебют skyrim.";
OK 164. Установка Windows (дискеты).                               hits=105
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 165. Ремонт (паяльник).                                         hits=18
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
        src/rpc/WorkshopTools.java:30  a[1] = "паяльник (конденсаторы)";
OK 166. Погладь кота.                                              hits=15
        src/rpc/CalendarHistory.java:67  a0[7] = "хозяин барсика • довести счастье кота до 100%";
        src/rpc/City.java:37  a0[2] = "ряды ящиков с б/у платами, старыми кулерами и котами в мешке.";
-- 167. Полив цветка.
OK 168. Настройка ТВ (антенна).                                    hits=3
        src/rpc/Exp.java:173  a11[1] = "crysis (2007) very high directx 10|запуск бенчмарка crysis 'can it run crysis?' на предельных настро…
        src/rpc/Exp.java:183  a11[11] = "s.t.a.l.k.e.r. 2: heart of chornobyl|запуск зоны отчуждения на сверхнизких настройках памяти";
-- 169. Перемотка кассеты карандашом.
OK 170. Выключение ПК перед сном.                                  hits=2
        src/rpc/LifeSim.java:105  a[4] = "радиопередача • новости технологий";
        src/rpc/Funcs.java:111  "выставить перемычки", "подключить переднюю панель",
OK 171. Коллекционер видеокарт.                                    hits=34
        src/rpc/CalendarHistory.java:32  a[22] = "2002 • видеокарта ati radeon 9700 pro с аппаратной поддержкой directx 9.0.";
        src/rpc/CalendarHistory.java:42  a[32] = "2012 • стандарт быстрой памяти ddr3-2400 и видеокарты nvidia geforce gtx 680 (kepler).";
OK 172. Ретро-геймер (Doom).                                       hits=30
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
        src/rpc/CalendarHistory.java:34  a[24] = "2004 • релиз half-life 2, world of warcraft, doom 3 и стандарта шины pci express.";
~  173. Хакер.                                                     hits=1
        src/rpc/Funcs.java:336  "посмотреть фильм про хакеров", "купить попкорн",
-- 174. Меценат (ПК детдому).
-- 175. Долгожитель (до 2027).
OK 176. Мастер на все руки (100 ПК).                               hits=24
        src/rpc/CalendarHistory.java:57  a[47] = "2027 • встреча нового 2027 года! великий финал эпохи и вечная слава мастеру!";
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
OK 177. Друг кота.                                                 hits=17
        src/rpc/CalendarHistory.java:67  a0[7] = "хозяин барсика • довести счастье кота до 100%";
        src/rpc/Game.java:7856  a0[5] = new stringbuffer("производитель: ").append((a.f[5] != 0) ? (a.f[5] != 0) ? "amd" : "другой" : "intel")…
OK 178. Фотограф.                                                  hits=3
        src/rpc/Funcs.java:235  "сфотографировать сетап", "залипать в потолок"
        src/rpc/Funcs.java:298  "дать кошачью мяту", "сфотографировать кота",
OK 179. Писатель (статья в журнал).                                hits=4
        src/rpc/CalendarHistory.java:12  a[2] = "1982 • выход культового домашнего компьютера commodore 64 и журнала «радио» с рк-86.";
        src/rpc/Browser.java:583  a8[9] = new stringbuffer("статья менялась ").append(10 + i0 % 40).append(" раз").tostring();
OK 180. Легенда рынка.                                             hits=9
        src/rpc/CalendarHistory.java:11  a[1] = "1981 • дебют легендарного ibm pc 5150 на процессоре intel 8088 и pc-dos 1.0.";
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
-- 181. Уборка комнаты.
OK 182. Готовка еды (пельмени, чай).                               hits=2
        src/rpc/LifeSim.java:380  this.lasteventmsg = "вы сварили горячие пельмени со сметаной. сытость на максимуме!";
        src/rpc/Funcs.java:216  "сходить на кухню", "помыть кружку", "сварить пельмени",
OK 183. Сон (без сна падает энергия).                              hits=4
        src/rpc/Game.java:3325  text.draw(this.g, "энергия "
        src/rpc/Game.java:3327  tx += this.g.textwidth("энергия 100%", 0) + 8;
OK 184. Кот спит на системнике (перегрев).                         hits=8
        src/rpc/Game.java:531  a[0] = this.makeorder("иван (офис)", "пк перегревается и вырубается", 0, 1800, 40, i, "прочистить кулер от пыл…
        src/rpc/Game.java:2898  this.g.drawtext(new stringbuffer("! перегрев: ").append(this.expstress.temp).append(" c !").tostring(), i + 4,…
OK 185. Мебель (стол, стул, шкаф).                                 hits=18
        src/rpc/LifeSim.java:343  this.lasteventmsg = "кот барсик прыгнул на стол и улёгся рядом с клавиатурой!";
        src/rpc/LifeSim.java:401  this.lasteventmsg = "вы пропылесосили ковер, вынесли банки и протерли стол от пыли. чистота!";
~  186. Декор (постеры, коврик).                                   hits=1
        src/rpc/Funcs.java:375  "найти коврик с логотипом", "найти дискетницу",
OK 187. Коммуналка (свет, интернет).                               hits=49
        src/rpc/Shell.java:502  this.println("установлено 0 новых пакетов (интернета нет)");
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
-- 188. Стирка одежды.
OK 189. Ремонт труб.                                               hits=14
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
        src/rpc/WorkshopTools.java:86  this.toolfeedback = "линия +12v просажена до 10.85v, пульсации 140мв. бп требует ремонта.";
-- 190. Обогреватель зимой.
OK 191. Собеседование.                                             hits=3
        src/rpc/City.java:90  this.dialoguetext = new stringbuffer("«мы ищем ведущего системного архитектора на проект ").append(i0).append(…
        src/rpc/City.java:92  a[0] = "1. «пройти собеседование по архитектуре x86 и шинам»";
-- 192. Офисная рутина.
-- 193. Карьерная лестница.
OK 194. Начальник (настроение).                                    hits=2
        src/rpc/Game.java:3333  text.draw(this.g, "настроение "
        src/rpc/Game.java:3870  + "%  настроение "
-- 195. Конкуренция (демпинг).
-- 196. Налоги.
~  197. Бухгалтерия.                                               hits=1
        src/rpc/Game.java:534  a[3] = this.makeorder("мария (бухгалтерия)", "не включается после скачка сети", 3, 2900, 55, i, "заменить неис…
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
        src/rpc/Game.java:8776  this.footer("ok: листать 3: купить", "1: к сайтам");
        src/rpc/Exp.java:45  a0[3] = "создание софта & игр";
OK 207. Стриминг (2020-е).                                         hits=2
        src/rpc/Inst.java:423  a7[2] = "стриминг";
        src/rpc/Inst.java:431  a8[3] = "стриминг";
~  208. Блог о ретро-железе.                                       hits=1
        src/rpc/Inst.java:398  a4[1] = "блоги";
-- 209. Фотография на пленку.
-- 210. Косплей на Хэллоуин.
OK 211. Новости по радио.                                          hits=52
        src/rpc/CalendarHistory.java:12  a[2] = "1982 • выход культового домашнего компьютера commodore 64 и журнала «радио» с рк-86.";
        src/rpc/LifeSim.java:43  public boolean radioon;
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
        src/rpc/Game.java:531  a[0] = this.makeorder("иван (офис)", "пк перегревается и вырубается", 0, 1800, 40, i, "прочистить кулер от пыл…
        src/rpc/Game.java:2898  this.g.drawtext(new stringbuffer("! перегрев: ").append(this.expstress.temp).append(" c !").tostring(), i + 4,…
OK 220. Отключение света в 90-х.                                   hits=2
        src/rpc/Exp.java:158  a10[0] = "windows 11 23h2 tiny11|сборка без tpm 2.0/secureboot, отключение winsxs, эмуляция acpi 5.0";
        src/rpc/Exp.java:176  a11[4] = "the witcher 3: wild hunt next-gen|патч на отключение трассировки лучей и упрощённую геометрию листвы…
-- 221. Соседи сверху шумят.
-- 222. Друзья приходят с пиццей.
-- 223. Влюблённость.
-- 224. Свидание (одежда).
OK 225. Помощь другу (бесплатный ремонт).                          hits=15
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
        src/rpc/WorkshopTools.java:86  this.toolfeedback = "линия +12v просажена до 10.85v, пульсации 140мв. бп требует ремонта.";
-- 226. Звонок родителей.
-- 227. Кот соседей.
-- 228. Бабушка с вареньем.
-- 229. Школа и экзамены.
-- 230. Выпускной.
OK 231. Установка драйверов.                                       hits=15
        src/rpc/Game.java:7529  this.toastmsg((i <= 0) ? "установка началась" : "установка с обходом проверок");
        src/rpc/Game.java:7635  this.toastmsg("сначала установите ос (пункт «установка ос»)");
OK 232. Конфликт версий DirectX.                                   hits=5
        src/rpc/CalendarHistory.java:32  a[22] = "2002 • видеокарта ati radeon 9700 pro с аппаратной поддержкой directx 9.0.";
        src/rpc/Exp.java:173  a11[1] = "crysis (2007) very high directx 10|запуск бенчмарка crysis 'can it run crysis?' на предельных настро…
~  233. Чистка реестра.                                            hits=1
        src/rpc/Funcs.java:183  "ультразвуковая чистка платы"
OK 234. Установка Linux (дуалбут).                                 hits=28
        src/rpc/CalendarHistory.java:21  a[11] = "1991 • линус торвальдс публикует первую версию ядра linux 0.01. распад ссср.";
        src/rpc/Corp.java:354  return (i0 >= 2000) ? "microsoft / linux" : "microsoft / ibm";
~  235. Пиратские игры с вирусом.                                  hits=1
        src/rpc/Funcs.java:365  "найти дискету с вирусом", "найти cd с демо",
OK 236. Установка модов.                                           hits=13
        src/rpc/Game.java:7529  this.toastmsg((i <= 0) ? "установка началась" : "установка с обходом проверок");
        src/rpc/Game.java:7635  this.toastmsg("сначала установите ос (пункт «установка ос»)");
OK 237. Антивирус находит вирус.                                   hits=3
        src/rpc/Inst.java:331  a[i4] = rpc.inst.dl("антивирус", 1800, 1996, 0, 0, "чистит и находит «заражённые» файлы");
        src/rpc/Funcs.java:141  "удалить вирус", "поставить антивирус",
-- 238. Загрузочная флешка.
OK 239. Форматирование диска.                                      hits=9
        src/rpc/Shell.java:440  this.println("проверка диска c: ... ok");
        src/rpc/Shell.java:445  this.println("форматирование c: — заняло 3 минуты");
~  240. Восстановление данных.                                     hits=1
        src/rpc/Browser.java:541  a2[5] = "долговременное хранение данных:";
-- 241. Будильник.
-- 242. Зарядка.
-- 243. Завтрак (яичница).
OK 244. Проверка почты.                                            hits=5
        src/rpc/Shell.java:440  this.println("проверка диска c: ... ok");
        src/rpc/City.java:41  a0[6] = "очередь за посылками с чипами. проверка коробки: нет ли там кирпича?";
-- 245. Дневник.
-- 246. Вечерний чай у окна.
OK 247. Бессонница (чтение мануала).                               hits=4
        src/rpc/Shell.java:501  this.println("чтение списков пакетов... готово");
        src/rpc/City.java:36  a0[1] = "бесконечные павильоны с чипами, сокетами, переходниками и мануалами.";
OK 248. Суббота — банный день.                                     hits=46
        src/rpc/LifeSim.java:55  a[1] = "день";
        src/rpc/Corp$Chip.java:6  public int nodenm;
~  249. Воскресенье — отдых.                                       hits=1
        src/rpc/Game.java:3909  string title = "отдых";
OK 250. Конец дня (ложиться спать).                                hits=4
        src/rpc/Game.java:3676  case room.hot_bed: return "ok: спать";
        src/rpc/Game.java:3946  this.footer("ok: спать", "назад");
~  251. Почта России (задержки).                                   hits=1
        src/rpc/Inst.java:368  a0[3] = "первая почта";
OK 252. Курьер приходит, когда тебя нет.                           hits=2
        src/rpc/Browser.java:462  s0 = "ddr5 дешевеет, pcie 5.0 приходит";
        src/rpc/Browser.java:482  s1 = "sata приходит на смену ide";
~  253. Самовывоз со склада.                                       hits=1
        src/rpc/City.java:100  this.dialoguetext = "«вам посылка со склада микроэлектроники! распишитесь в квитанции.»";
-- 254. Пьяный курьер.
OK 255. Сосед забрал посылку.                                      hits=3
        src/rpc/Game.java:5956  this.toastmsg((i1 > 0) ? new stringbuffer("оплачено, приедет через ").append(i1).append(" дн.").tostring() : n…
        src/rpc/City.java:103  a0[1] = "2. «забрать посылку домой на верстак»";
OK 256. Проверка при получении.                                    hits=4
        src/rpc/Shell.java:440  this.println("проверка диска c: ... ok");
        src/rpc/City.java:41  a0[6] = "очередь за посылками с чипами. проверка коробки: нет ли там кирпича?";
-- 257. Возврат брака.
OK 258. Зимняя доставка.                                           hits=8
        src/rpc/Game.java:5622  this.db.count() + " деталей • доставка сутками",
        src/rpc/Game.java:5659  return "доставка: заказы";
-- 259. Почтальон 80-х.
OK 260. Курьер за 30 минут (2020-е).                               hits=18
        src/rpc/Shell.java:445  this.println("форматирование c: — заняло 3 минуты");
        src/rpc/LifeSim.java:24  public int curminute;
OK 261. Сборка на время.                                           hits=11
        src/rpc/Shell.java:458  this.println((s1.equals((object)"time")) ? "текущее время: 12:00" : "дата: 01-01-1990");
        src/rpc/Game.java:466  a[1] = "песочница: сборка пк (1979-2027)";
OK 262. Не тот сокет (возврат).                                    hits=27
        src/rpc/CalendarHistory.java:38  a[28] = "2008 • архитектура intel nehalem (core i7-920, сокет lga 1366, трехканальная ddr3).";
        src/rpc/CalendarHistory.java:53  a[43] = "2023 • бум генеративных нейросетей (chatgpt/llm/stable diffusion) и сокет lga 1700.";
OK 263. БП не тянет.                                               hits=3
        src/rpc/Score.java:577  a0[i16] = new stringbuffer("кулер не тянет cpu (").append(a6.f[1]).append(" вт < ").append(a1.f[3]).append(" в…
        src/rpc/Game.java:7410  this.toastmsg("бп не тянет — нужен мощнее");
OK 264. Кулер не влезает в корпус.                                 hits=42
        src/rpc/LifeSim.java:173  this.lasteventmsg = "кот барсик уютно свернулся клубочком на теплом корпусе пк.";
        src/rpc/Score.java:45  a[11] = "корпус";
OK 265. Видеокарта упирается в HDD.                                hits=13
        src/rpc/CalendarHistory.java:32  a[22] = "2002 • видеокарта ati radeon 9700 pro с аппаратной поддержкой directx 9.0.";
        src/rpc/CalendarHistory.java:52  a[42] = "2022 • видеокарта nvidia geforce rtx 4090 и процессоры amd ryzen 7000 (zen 4).";
OK 266. Забыл снять пленку с термопасты.                           hits=4
        src/rpc/WorkshopTools.java:170  this.toolfeedback = new stringbuffer("✓ идеальный слой термопасты (").append(this.pastethickness).append("%). …
        src/rpc/Browser.java:249  a3[7] = "забыл снять плёнку с кулера»";
OK 267. Конфликт памяти.                                           hits=17
        src/rpc/Shell.java:779  this.println(new stringbuffer("всего памяти: ").append(this.ramkb).append(" кб").tostring());
        src/rpc/CalendarHistory.java:42  a[32] = "2012 • стандарт быстрой памяти ddr3-2400 и видеокарты nvidia geforce gtx 680 (kepler).";
-- 268. Шум и вибрация.
-- 269. Дешевый БП сгорает.
OK 270. Ведро 2005 под Windows 11.                                 hits=93
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
        src/rpc/LifeSim.java:173  this.lasteventmsg = "кот барсик уютно свернулся клубочком на теплом корпусе пк.";
        src/rpc/Score.java:50  a[16] = "мышь";
OK 275. Скачок напряжения.                                         hits=2
        src/rpc/WorkshopTools.java:33  a[4] = "⚡ мультиметр (напряжения)";
        src/rpc/Funcs.java:203  "пережить скачок инфляции", "рассчитать бюджет",
OK 276. Кража видеокарты.                                          hits=8
        src/rpc/CalendarHistory.java:42  a[32] = "2012 • стандарт быстрой памяти ddr3-2400 и видеокарты nvidia geforce gtx 680 (kepler).";
        src/rpc/CalendarHistory.java:46  a[36] = "2016 • видеокарты nvidia geforce gtx 1080 (pascal) и массовое внедрение nvme ssd.";
-- 277. Пожар БП.
OK 278. Кот скинул монитор.                                        hits=40
        src/rpc/Score.java:47  a[13] = "монитор";
        src/rpc/Score.java:69  a0[13] = "монитор";
-- 279. Пыльная буря.
-- 280. Поломка лифта.
OK 281. Новый год.                                                 hits=2
        src/rpc/WorkshopTools.java:185  this.toolfeedback = new stringbuffer("✓ вздутый электролит c").append(this.selcap + 1).append(" выпаян, запаян…
        src/rpc/City.java:188  return "☕ вы взяли новый заказ на ремонт и выпили отличный кофе!";
OK 282. День рождения.                                             hits=46
        src/rpc/LifeSim.java:55  a[1] = "день";
        src/rpc/Corp$Chip.java:6  public int nodenm;
-- 283. 8 марта / 23 февраля.
-- 284. 1 сентября.
-- 285. Последний звонок.
OK 286. Новый 2027 (финал).                                        hits=529
        src/rpc/Shell.java:4  final public static int os_dos = 0;
        src/rpc/Shell.java:5  final public static int os_unix = 1;
-- 287. Хэллоуин.
OK 288. День Победы.                                               hits=46
        src/rpc/LifeSim.java:55  a[1] = "день";
        src/rpc/Corp$Chip.java:6  public int nodenm;
-- 289. Масленица.
OK 290. Старый Новый год.                                          hits=6
        src/rpc/Score.java:639  a0[i23] = new stringbuffer("старый ").append(s0).append("-монитор не подключить к новой карте").tostring();
        src/rpc/WorkshopTools.java:185  this.toolfeedback = new stringbuffer("✓ вздутый электролит c").append(this.selcap + 1).append(" выпаян, запаян…
OK 291. Заказ от школы.                                            hits=26
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
        src/rpc/Game.java:5626  "basic / asm / c • заказы • майнинг",
OK 292. Заказ от банка.                                            hits=26
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
        src/rpc/Game.java:5626  "basic / asm / c • заказы • майнинг",
OK 293. Заказ от геймера.                                          hits=26
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
        src/rpc/Game.java:5626  "basic / asm / c • заказы • майнинг",
OK 294. Заказ от бабушки.                                          hits=26
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
        src/rpc/Game.java:5626  "basic / asm / c • заказы • майнинг",
~  295. Аукцион.                                                   hits=1
        src/rpc/Funcs.java:201  "выставить раритет на аукцион", "поймать распродажу",
-- 296. Гарантийный случай.
-- 297. Клиент-мошенник.
-- 298. Отзыв в газете.
-- 299. Конкурент украл идею.
-- 300. Франшиза в другом городе.
OK 301. Анимированный корпус (кулеры, LEDs).                       hits=17
        src/rpc/LifeSim.java:173  this.lasteventmsg = "кот барсик уютно свернулся клубочком на теплом корпусе пк.";
        src/rpc/Score.java:45  a[11] = "корпус";
OK 302. Плавная вставка деталей в слот.                            hits=269
        src/rpc/CalendarHistory.java:27  a[17] = "1997 • процессор pentium ii в картридже slot 1. суперкомпьютер deep blue побеждает каспарова.";
        src/rpc/CalendarHistory.java:56  a[46] = "2026 • 50-летие микрокомпьютерной революции. все 56,750 деталей в едином строю!";
OK 303. Экран монитора (BIOS, загрузка Windows).                   hits=142
        src/rpc/Shell.java:89  this.println(new stringbuffer("загрузка ").append(this.osname).append(" ...").tostring());
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
OK 304. Частицы пыли в комнате.                                    hits=14
        src/rpc/LifeSim.java:111  this.lasteventmsg = "за окном кружится легкий снег. в комнате тепло и пахнет чаем.";
        src/rpc/LifeSim.java:362  this.lasteventmsg = "вы заварили свежий горячий чай с лимоном. по комнате пошёл пар.";
OK 305. Кот ходит по комнате.                                      hits=13
        src/rpc/LifeSim.java:111  this.lasteventmsg = "за окном кружится легкий снег. в комнате тепло и пахнет чаем.";
        src/rpc/LifeSim.java:362  this.lasteventmsg = "вы заварили свежий горячий чай с лимоном. по комнате пошёл пар.";
-- 306. Капли дождя на стекле.
OK 307. Тени от монитора.                                          hits=8
        src/rpc/Shell.java:501  this.println("чтение списков пакетов... готово");
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 308. Свечение монитора в темноте.                               hits=4
        src/rpc/Score.java:646  a0[i24] = "карта слишком новая для этого монитора (нет разъёма)";
        src/rpc/Funcs.java:165  "заменить лампу в мониторе", "настроить фокус монитора",
-- 309. Искры при КЗ.
-- 310. Дым от сгоревшего ПК.
-- 311. Пар от чая.
OK 312. Молния освещает комнату.                                   hits=2
        src/rpc/Game.java:4367  this.footer("←→ год", "ok: в комнату");
        src/rpc/Funcs.java:218  "открыть окно", "закрыть окно", "проветрить комнату",
OK 313. Радуга после дождя.                                        hits=5
        src/rpc/Game.java:534  a[3] = this.makeorder("мария (бухгалтерия)", "не включается после скачка сети", 3, 2900, 55, i, "заменить неис…
        src/rpc/Game.java:7604  this.toastmsg("сим-карты для интернета появятся после 2003");
OK 314. 3D-иконки в меню.                                          hits=6
        src/rpc/Shell.java:506  this.println("редактор доступен в меню ос: «редактор кода».");
        src/rpc/Shell.java:524  this.println("программа отработала (вывод в меню «программы»).");
OK 315. Курсор-стрелка Windows.                                    hits=93
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 316. Движущиеся полоски загрузки.                               hits=2
        src/rpc/Setup.java:71  return "порядок загрузки: с чего ставим ос";
        src/rpc/Game.java:567  this.toast = new stringbuffer("ошибка загрузки базы: ").append(a1.tostring()).tostring();
OK 317. Карта города.                                              hits=18
        src/rpc/CalendarHistory.java:32  a[22] = "2002 • видеокарта ati radeon 9700 pro с аппаратной поддержкой directx 9.0.";
        src/rpc/CalendarHistory.java:52  a[42] = "2022 • видеокарта nvidia geforce rtx 4090 и процессоры amd ryzen 7000 (zen 4).";
~  318. Полки в магазине.                                          hits=1
        src/rpc/Browser.java:730  a2[6] = new stringbuffer("цена: ").append(i3).append(" (в магазине ").append(a1.price).append(")").tostring();
-- 319. Портрет героя.
OK 320. Смена сезонов (цвет).                                      hits=7
        src/rpc/CalendarHistory.java:17  a[7] = "1987 • стандарт ibm ps/2: разъем vga 640x480 16 цветов, порты ps/2 для клавиатуры и мыши.";
        src/rpc/LifeSim.java:409  this.lasteventmsg = "вы полили цветок на подоконнике. зеленые листья расправились.";
OK 321. Звук запуска Windows.                                      hits=115
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
OK 322. Экран «Добро пожаловать».                                  hits=6
        src/rpc/Game.java:7239  this.toastmsg("спикер выключен — ошибку видно только на экране");
        src/rpc/Game.java:8040  a.drawtext("и текст синего экрана. свой", 8, i6, 0);
OK 323. Часы на панели задач.                                      hits=3
        src/rpc/Game.java:7270  this.toastmsg(new stringbuffer("часы bios: 12.05.").append((this.slots[1] != null) ? this.slots[1].year : this…
        src/rpc/Browser.java:361  a9[5] = "сегодня это часы в тостере».";
-- 324. Очистка корзины.
~  325. Скринсейвер (трубки).                                      hits=1
        src/rpc/Funcs.java:151  "включить скринсейвер с рыбами", "настроить дату и время",
-- 326. Смена обоев.
OK 327. BSOD с белым текстом.                                      hits=25
        src/rpc/Game.java:55  final public static int m_bsod = 23;
        src/rpc/Game.java:293  private int bsodcode;
OK 328. Логотип BIOS (Award, AMI).                                 hits=34
        src/rpc/Setup.java:10  a0[0] = "award";
        src/rpc/Setup.java:13  a0[3] = "award";
OK 329. Звук кулера.                                               hits=23
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
-- 330. Мигание HDD LED.
OK 331. Memory Test 640K OK.                                       hits=30
        src/rpc/Corp.java:22  public int testing;
        src/rpc/Corp.java:98  this.testing = 1;
-- 332. Размытость на ЭЛТ.
OK 333. Низкое разрешение (пиксели).                               hits=2
        src/rpc/Game.java:7892  a4[2] = new stringbuffer("разрешение: ").append(a.f[2]).append("x").append(a.f[3]).tostring();
        src/rpc/Exp.java:178  a11[6] = "black myth: wukong (unreal engine 5)|nanite и lumen отключены, разрешение рендера 25%, эмуляция dx12…
~  334. Ковер под ногами (статическое электричество).              hits=1
        src/rpc/LifeSim.java:401  this.lasteventmsg = "вы пропылесосили ковер, вынесли банки и протерли стол от пыли. чистота!";
-- 335. Провода под столом.
-- 336. Балкон (подышать).
OK 337. Соседский ремонт (тряска экрана).                          hits=16
        src/rpc/CalendarHistory.java:66  a0[6] = "мастер-ремонтник • выполнить 50 клиентских заказов в мастерской";
        src/rpc/WorkshopTools.java:86  this.toolfeedback = "линия +12v просажена до 10.85v, пульсации 140мв. бп требует ремонта.";
OK 338. Старый телефон на столе.                                   hits=11
        src/rpc/Score.java:639  a0[i23] = new stringbuffer("старый ").append(s0).append("-монитор не подключить к новой карте").tostring();
        src/rpc/Game.java:6449  return "dial-up через телефонную линию";
-- 339. Холодильник с едой.
OK 340. Окно в подъезд.                                            hits=4
        src/rpc/LifeSim.java:111  this.lasteventmsg = "за окном кружится легкий снег. в комнате тепло и пахнет чаем.";
        src/rpc/Funcs.java:218  "открыть окно", "закрыть окно", "проветрить комнату",
OK 341. Полярность питания.                                        hits=17
        src/rpc/Score.java:44  a[10] = "блок питания";
        src/rpc/Score.java:468  a0[i1] = "не выбран блок питания";
~  342. Слои термопасты.                                           hits=1
        src/rpc/WorkshopTools.java:170  this.toolfeedback = new stringbuffer("✓ идеальный слой термопасты (").append(this.pastethickness).append("%). …
OK 343. Сборка по мануалу.                                         hits=8
        src/rpc/Game.java:466  a[1] = "песочница: сборка пк (1979-2027)";
        src/rpc/Game.java:477  t[1] = "сборка пк";
OK 344. Мультиметр.                                                hits=6
        src/rpc/WorkshopTools.java:33  a[4] = "⚡ мультиметр (напряжения)";
        src/rpc/City.java:144  a4[2] = "3. «проверить мультиметром прямо на месте»";
OK 345. Пайка конденсаторов.                                       hits=7
        src/rpc/CalendarHistory.java:63  a0[3] = "паяльный спец • заменить 50 вздутых low-esr конденсаторов";
        src/rpc/WorkshopTools.java:188  this.toolfeedback = new stringbuffer("конденсатор c").append(this.selcap + 1).append(" исправен, пайка блестит…
OK 346. Сдувание пыли.                                             hits=11
        src/rpc/LifeSim.java:401  this.lasteventmsg = "вы пропылесосили ковер, вынесли банки и протерли стол от пыли. чистота!";
        src/rpc/WorkshopTools.java:101  this.toolfeedback = "радиатор забит войлоком пыли. продуйте баллоном и нанесите свежую пасту.";
OK 347. Замена термопасты.                                         hits=4
        src/rpc/WorkshopTools.java:170  this.toolfeedback = new stringbuffer("✓ идеальный слой термопасты (").append(this.pastethickness).append("%). …
        src/rpc/Game.java:7413  this.toastmsg("диск щёлкает — только замена");
OK 348. Установка процессора (ключ).                               hits=62
        src/rpc/CalendarHistory.java:14  a[4] = "1984 • релиз apple macintosh 128k с графическим интерфейсом и процессора intel 80286.";
        src/rpc/CalendarHistory.java:16  a[6] = "1986 • анонс 32-битного процессора intel 80386dx. начало эры защищенного режима.";
~  349. Смазка вентилятора.                                        hits=1
        src/rpc/Funcs.java:256  "кулер из вентилятора икеа", "пк в коробке из-под пиццы",
OK 350. Распиновка Power SW.                                       hits=44
        src/rpc/Score.java:382  public static int needpower(rpc.part a, rpc.part a0) {
        src/rpc/Score.java:556  int i19 = rpc.score.needpower(a1, a3);
-- 351. Разговор с бабушкой.
-- 352. Сосед-алкоголик.
OK 353. Друг детства с тортом.                                     hits=2
        src/rpc/Game.java:7856  a0[5] = new stringbuffer("производитель: ").append((a.f[5] != 0) ? (a.f[5] != 0) ? "amd" : "другой" : "intel")…
        src/rpc/Funcs.java:233  "сыграть в настолку", "позвонить другу",
OK 354. Первая любовь.                                             hits=3
        src/rpc/Game.java:7259  this.toastmsg(new stringbuffer("первая загрузка: ").append(boot[this.bootsel]).tostring());
        src/rpc/Game.java:8061  a[0] = new stringbuffer("этап: ").append((i0 >= 20) ? (i0 >= 60) ? (i0 >= 90) ? "первая загрузка" : "установка…
OK 355. Драка на рынке.                                            hits=2
        src/rpc/Game.java:6676  this.g.drawtext("закажите детали в рынке", 8, i0 + 12 + this.fh0, 0);
        src/rpc/Game.java:7133  a[4] = "на рынке в слоте «сеть».";
-- 356. Помощь старушке.
-- 357. Подработка на дядю.
-- 358. Учитель труда.
-- 359. Свадьба друга.
OK 360. Похороны (настроение).                                     hits=2
        src/rpc/Game.java:3333  text.draw(this.g, "настроение "
        src/rpc/Game.java:3870  + "%  настроение "
OK 361. Коллекционер видеокарт.                                    hits=34
        src/rpc/CalendarHistory.java:32  a[22] = "2002 • видеокарта ati radeon 9700 pro с аппаратной поддержкой directx 9.0.";
        src/rpc/CalendarHistory.java:42  a[32] = "2012 • стандарт быстрой памяти ddr3-2400 и видеокарты nvidia geforce gtx 680 (kepler).";
-- 362. Мастер-ломастер.
OK 363. Скоростная сборка.                                         hits=8
        src/rpc/Game.java:466  a[1] = "песочница: сборка пк (1979-2027)";
        src/rpc/Game.java:477  t[1] = "сборка пк";
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
        src/rpc/Game.java:7856  a0[5] = new stringbuffer("производитель: ").append((a.f[5] != 0) ? (a.f[5] != 0) ? "amd" : "другой" : "intel")…
OK 370. Хранитель времени.                                         hits=3
        src/rpc/CalendarHistory.java:48  a[38] = "2018 • аппаратная трассировка лучей в реальном времени (nvidia rtx 2080 ray tracing).";
        src/rpc/Exp.java:200  a13[2] = "своя ос реального времени на c и ассемблере|написание mbr загрузчика, переход в защищенный 32-бит ре…
OK 371. Максимум графики.                                          hits=5
        src/rpc/LifeSim.java:380  this.lasteventmsg = "вы сварили горячие пельмени со сметаной. сытость на максимуме!";
        src/rpc/Game.java:536  a[5] = this.makeorder("ретро-клуб", "апгрейд графики для 3d-игр", 5, 5200, 90, i, "установить мощную видеокарт…
OK 372. Оптимизация под эмулятор.                                  hits=7
        src/rpc/Exp.java:182  a11[10] = "alan wake 2 (mesh shaders bypass)|обход обязательного требования mesh shaders через эмулятор vkd3d-…
        src/rpc/Exp.java:187  a12[1] = "предел шины bclk / fsb и делителей ram|форсирование шины до упора материнской платы, оптимизация суб…
OK 373. Процедурная графика.                                       hits=2
        src/rpc/Game.java:9287  a[3] = "тип: текст/графика";
        src/rpc/Game.java:9333  s = "графика при таком озу не поднимется";
OK 374. Анимация загрузки.                                         hits=2
        src/rpc/Setup.java:71  return "порядок загрузки: с чего ставим ос";
        src/rpc/Game.java:567  this.toast = new stringbuffer("ошибка загрузки базы: ").append(a1.tostring()).tostring();
OK 375. Звук в реальном времени.                                   hits=19
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
OK 376. Время идёт всегда.                                         hits=6
        src/rpc/Shell.java:458  this.println((s1.equals((object)"time")) ? "текущее время: 12:00" : "дата: 01-01-1990");
        src/rpc/Game.java:8239  a[3] = (i0 >= 12) ? "игра идёт на этом железе" : "на этом железе играть больно";
OK 377. Автосохранение каждые 5 минут.                             hits=18
        src/rpc/Shell.java:445  this.println("форматирование c: — заняло 3 минуты");
        src/rpc/LifeSim.java:24  public int curminute;
~  378. Удобное управление (стрелки, OK).                          hits=1
        src/rpc/Setup.java:83  return "стрелки — выбор, ok — изменить";
~  379. Крупные шрифты.                                            hits=1
        src/rpc/Game.java:8041  this.g.drawtext("ставится дольше (шрифты).", 8, i6 + (this.fh0 + 2), 0);
OK 380. Темная/светлая тема.                                       hits=17
        src/rpc/Shell.java:84  this.addnode("readme.txt", i4, false, "это твоя система. набери help — увидишь команды.\nфайлы можно создавать…
        src/rpc/CalendarHistory.java:19  a[9] = "1989 • процессор intel 486dx со встроенным математическим сопроцессором fpu и кэшем l1.";
-- 381. Мысли героя.
OK 382. Смена эпох.                                                hits=7
        src/rpc/CalendarHistory.java:10  a[0] = "1980 • xxii летние олимпийские игры в москве. эпоха микропроцессоров z80 и 6502.";
        src/rpc/CalendarHistory.java:57  a[47] = "2027 • встреча нового 2027 года! великий финал эпохи и вечная слава мастеру!";
OK 383. Ностальгия (диск с Doom).                                  hits=103
        src/rpc/Shell.java:18  private int diskmb;
        src/rpc/Shell.java:36  this.diskmb = 20;
OK 384. Письмо себе в будущее.                                     hits=2
        src/rpc/Funcs.java:277  "настроить почту", "отправить письмо",
        src/rpc/Funcs.java:278  "получить письмо", "настроить новостную группу",
-- 385. Конец игры (2027).
OK 386. Секретный финал (Win11 на хламе).                          hits=529
        src/rpc/Shell.java:4  final public static int os_dos = 0;
        src/rpc/Shell.java:5  final public static int os_unix = 1;
-- 387. Разные концовки.
-- 388. Титры со статистикой.
OK 389. Новая игра+.                                               hits=17
        src/rpc/LifeSim.java:209  this.lasteventmsg = new stringbuffer("с новым ").append(this.curyear).append(" годом! новая эпоха в истории it…
        src/rpc/Score.java:646  a0[i24] = "карта слишком новая для этого монитора (нет разъёма)";
-- 390. Благодарности.
OK 391. Windows 11 на ведре 2005.                                  hits=93
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 392. Hackintosh на AMD.                                         hits=2
        src/rpc/Exp.java:160  a10[2] = "mac os x 10.4 tiger (x86 hackintosh)|инъекция kext-драйверов appleacpiplatform и патч mach_kernel по…
        src/rpc/Funcs.java:239  "win11 на железе 2005 года", "hackintosh на обычном pc",
OK 393. Android x86 на нетбуке.                                    hits=7
        src/rpc/CalendarHistory.java:65  a0[5] = "doom на всем • запустить doom на кпк, нетбуке и калькуляторе";
        src/rpc/Gfx.java:9  * (android, web, ...) only has to implement these methods.
OK 394. Linux на калькуляторе.                                     hits=17
        src/rpc/CalendarHistory.java:21  a[11] = "1991 • линус торвальдс публикует первую версию ядра linux 0.01. распад ссср.";
        src/rpc/CalendarHistory.java:65  a0[5] = "doom на всем • запустить doom на кпк, нетбуке и калькуляторе";
OK 395. Doom на холодильнике.                                      hits=30
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
        src/rpc/CalendarHistory.java:34  a[24] = "2004 • релиз half-life 2, world of warcraft, doom 3 и стандарта шины pci express.";
OK 396. Windows 95 на ПК 2026.                                     hits=93
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 397. Игра в 4K на ЭЛТ.                                          hits=12
        src/rpc/Corp.java:41  a0[2] = "игра";
        src/rpc/Game.java:969  this.toastmsg("игра сохранена в память!");
OK 398. Сервер из хлама.                                           hits=9
        src/rpc/City.java:42  a0[7] = "собеседования, созвоны, карьера от стажера до it-директора и серверные стойки.";
        src/rpc/City.java:94  a[2] = "3. «провести аудит серверной и устранить перегрев стоек»";
-- 399. ПК для бабушки.
-- 400. Крипто-ферма в гараже.
OK 401. Курс валют.                                                hits=7
        src/rpc/Game.java:7180  a[1] = new stringbuffer("курс сети: ").append(this.netkind()).tostring();
        src/rpc/Browser.java:356  a9[0] = "музей: экскурсия";
-- 402. Инфляция.
OK 403. Кредит.                                                    hits=17
        src/rpc/Game.java:2243  this.toastmsg(new stringbuffer("не хватает кредитов: нужно ").append(a.price).append(", есть ").append(this.ca…
        src/rpc/Game.java:6079  this.toastmsg(new stringbuffer("ремонт стоит ").append(i0).append(", а кредитов ").append(this.cash).tostring(…
-- 404. Вклады.
OK 405. Акции.                                                     hits=2
        src/rpc/Browser.java:672  a2[8] = "вердикт редакции:";
        src/rpc/Funcs.java:206  "вложиться в акции", "продать акции",
-- 406. Налоги.
-- 407. Штрафы.
-- 408. Лотерея.
OK 409. Клад на свалке.                                            hits=5
        src/rpc/Game.java:3135  this.footer("ok: запуск • 1-4: вкладки", "0: назад к списку");
        src/rpc/Game.java:6677  this.g.drawtext("или поищите на свалке.", 8, i0 + 24 + this.fh0, 0);
~  410. Аукцион.                                                   hits=1
        src/rpc/Funcs.java:201  "выставить раритет на аукцион", "поймать распродажу",
OK 411. Реальное время.                                            hits=3
        src/rpc/Shell.java:458  this.println((s1.equals((object)"time")) ? "текущее время: 12:00" : "дата: 01-01-1990");
        src/rpc/BuildView.java:405  case 4: return "промотать время";
OK 412. Смена поколений.                                           hits=2
        src/rpc/Game.java:5502  "поколений")
        src/rpc/Game.java:5511  /** russian plural form for a count (1 поколение / 2 поколения / 5 поколений). */
-- 413. Семья (дети).
~  414. Наследие (сын продолжает дело).                            hits=1
        src/rpc/Browser.java:195  return "дешево, 20% подделок";
-- 415. Конец игры (2027).
OK 416. Новая игра+.                                               hits=17
        src/rpc/LifeSim.java:209  this.lasteventmsg = new stringbuffer("с новым ").append(this.curyear).append(" годом! новая эпоха в истории it…
        src/rpc/Score.java:646  a0[i24] = "карта слишком новая для этого монитора (нет разъёма)";
OK 417. Мультиплеер (обмен деталями).                              hits=3
        src/rpc/Inst.java:399  a4[2] = "файлообменники";
        src/rpc/Funcs.java:197  "купить плату на запчасти", "обменять деталь",
OK 418. Достижения.                                                hits=2
        src/rpc/Game.java:471  a[6] = "календарь 1980-2027 & достижения";
        src/rpc/Game.java:4318  string head = "достижения: " + done + "/"
-- 419. Статистика.
-- 420. Эпилог.
OK 421. Живые обои Windows 98.                                     hits=94
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 422. Анимированный курсор (шлейф).                              hits=2
        src/rpc/Funcs.java:164  "починить шлейф матрицы", "заменить подсветку",
        src/rpc/Funcs.java:311  "кот охотится на курсор", "кот на клавиатуре",
OK 423. Экран загрузки (синие квадратики).                         hits=8
        src/rpc/Setup.java:71  return "порядок загрузки: с чего ставим ос";
        src/rpc/Game.java:567  this.toast = new stringbuffer("ошибка загрузки базы: ").append(a1.tostring()).tostring();
OK 424. Кнопка Turbo (светится).                                   hits=5
        src/rpc/Ports.java:390  a[0] = "кнопка power";
        src/rpc/Ports.java:391  a[1] = "turbo";
OK 425. Перегрев (артефакты на экране).                            hits=8
        src/rpc/Game.java:531  a[0] = this.makeorder("иван (офис)", "пк перегревается и вырубается", 0, 1800, 40, i, "прочистить кулер от пыл…
        src/rpc/Game.java:2898  this.g.drawtext(new stringbuffer("! перегрев: ").append(this.expstress.temp).append(" c !").tostring(), i + 4,…
OK 426. Разбитый экран (трещина).                                  hits=6
        src/rpc/Game.java:7239  this.toastmsg("спикер выключен — ошибку видно только на экране");
        src/rpc/Game.java:8040  a.drawtext("и текст синего экрана. свой", 8, i6, 0);
OK 427. Свет от монитора на лицо.                                  hits=7
        src/rpc/LifeSim.java:416  this.lasteventmsg = (this.lampon) ? "настольная лампа зажглась теплым ламповым светом." : "настольная лампа вы…
        src/rpc/Score.java:646  a0[i24] = "карта слишком новая для этого монитора (нет разъёма)";
OK 428. Кабели за системником.                                     hits=2
        src/rpc/Funcs.java:112  "уложить кабели стяжками", "поставить вентилятор на вдув",
        src/rpc/Funcs.java:126  "спрятать кабели за лоток", "собрать тестовый стенд",
OK 429. Пыль в лучах света.                                        hits=16
        src/rpc/WorkshopTools.java:32  a[3] = "сжатый воздух (пыль)";
        src/rpc/WorkshopTools.java:127  this.toolfeedback = new stringbuffer("пыль сдувается... осталось ").append(this.dustremaining).append("% пыли …
OK 430. Пиксельная графика.                                        hits=2
        src/rpc/Game.java:9287  a[3] = "тип: текст/графика";
        src/rpc/Game.java:9333  s = "графика при таком озу не поднимется";
OK 431. Фоновый звук улицы.                                        hits=17
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
OK 432. Звук шагов.                                                hits=17
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
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
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
-- 440. Затухание вентиляторов.
-- 441. Вид из окна (дом, деревья, площадка).
~  442. Кот на подоконнике.                                        hits=1
        src/rpc/LifeSim.java:409  this.lasteventmsg = "вы полили цветок на подоконнике. зеленые листья расправились.";
OK 443. Вянущий цветок.                                            hits=2
        src/rpc/LifeSim.java:409  this.lasteventmsg = "вы полили цветок на подоконнике. зеленые листья расправились.";
        src/rpc/Funcs.java:220  "полить кактус", "полить фикус", "пересадить цветок",
OK 444. Кружка (хочет пить).                                       hits=26
        src/rpc/Game.java:3877  this.footer("ok: пить   2: заварить",
        src/rpc/Game.java:5179  this.footer("ok: купить", "back");
~  445. Книги по электронике.                                      hits=1
        src/rpc/Funcs.java:337  "пойти в библиотеку", "взять книгу по электронике",
-- 446. Аквариум с рыбками.
OK 447. Телевизор (новости, мультики).                             hits=6
        src/rpc/LifeSim.java:105  a[4] = "радиопередача • новости технологий";
        src/rpc/Browser.java:23  a[2] = "новости it";
OK 448. Пылесос в шкафу.                                           hits=3
        src/rpc/LifeSim.java:401  this.lasteventmsg = "вы пропылесосили ковер, вынесли банки и протерли стол от пыли. чистота!";
        src/rpc/Acoustics.java:100  return "как пылесос";
~  449. Кровать (отдых).                                           hits=1
        src/rpc/Game.java:3909  string title = "отдых";
-- 450. Шкаф с одеждой.
-- 451. Сапёр.
-- 452. Косынка.
-- 453. Пинбол.
OK 454. Змейка на телефоне.                                        hits=2
        src/rpc/Game.java:7168  return "гигабит, 4g в каждом телефоне";
        src/rpc/Funcs.java:241  "linux на калькуляторе", "dos на телефоне",
-- 455. Тетрис на телевизоре.
OK 456. Сборка на время.                                           hits=11
        src/rpc/Shell.java:458  this.println((s1.equals((object)"time")) ? "текущее время: 12:00" : "дата: 01-01-1990");
        src/rpc/Game.java:466  a[1] = "песочница: сборка пк (1979-2027)";
-- 457. Поймай пылинку.
OK 458. Почини провод.                                             hits=21
        src/rpc/WorkshopTools.java:170  this.toolfeedback = new stringbuffer("✓ идеальный слой термопасты (").append(this.pastethickness).append("%). …
        src/rpc/Game.java:533  a[2] = this.makeorder("сергей (инженер)", "пк пищит спикером и не включается", 2, 3400, 65, i, "найти неисправ…
OK 459. Установи Windows.                                          hits=110
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 460. Разгон.                                                    hits=15
        src/rpc/CalendarHistory.java:28  a[18] = "1998 • релиз windows 98, half-life и легендарного хита разгона celeron 300a (450 mhz!).";
        src/rpc/CalendarHistory.java:41  a[31] = "2011 • легендарный разгон sandy bridge (core i7-2600k 5.0 ghz) и дебют skyrim.";
OK 461. Windows 11 на ведре 2005.                                  hits=93
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 462. Hackintosh (OpenCore).                                     hits=3
        src/rpc/Exp.java:160  a10[2] = "mac os x 10.4 tiger (x86 hackintosh)|инъекция kext-драйверов appleacpiplatform и патч mach_kernel по…
        src/rpc/Funcs.java:239  "win11 на железе 2005 года", "hackintosh на обычном pc",
OK 463. Android x86.                                               hits=5
        src/rpc/Gfx.java:9  * (android, web, ...) only has to implement these methods.
        src/rpc/Exp.java:100  a5[3] = "asus eee pad transformer tf101|nvidia tegra 2 (dual cortex-a9 1.0ghz)|1gb lpddr2|android 3.0 honeycom…
OK 464. Windows 95 на ПК 2026.                                     hits=93
        src/rpc/CalendarHistory.java:15  a[5] = "1985 • запуск microsoft windows 1.0, появление чипсета amiga ocs и дискет 3.5\" hd.";
        src/rpc/CalendarHistory.java:20  a[10] = "1990 • релиз windows 3.0, изобретение world wide web (www) тимом бернерсом-ли.";
OK 465. Linux на калькуляторе.                                     hits=17
        src/rpc/CalendarHistory.java:21  a[11] = "1991 • линус торвальдс публикует первую версию ядра linux 0.01. распад ссср.";
        src/rpc/CalendarHistory.java:65  a0[5] = "doom на всем • запустить doom на кпк, нетбуке и калькуляторе";
OK 466. Doom на принтере.                                          hits=30
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
        src/rpc/CalendarHistory.java:34  a[24] = "2004 • релиз half-life 2, world of warcraft, doom 3 и стандарта шины pci express.";
OK 467. Сервер из хлама.                                           hits=9
        src/rpc/City.java:42  a0[7] = "собеседования, созвоны, карьера от стажера до it-директора и серверные стойки.";
        src/rpc/City.java:94  a[2] = "3. «провести аудит серверной и устранить перегрев стоек»";
OK 468. Майнинг-ферма в гараже.                                    hits=2
        src/rpc/Market.java:27  a[6] = "майнинг-ферма";
        src/rpc/Funcs.java:258  "майнинг-ферма", "nas из старого пк",
-- 469. ПК для бабушки.
OK 470. Ретро-сборка 1998 (Doom/Quake).                            hits=36
        src/rpc/CalendarHistory.java:23  a[13] = "1993 • рождение легендарного шутера doom от id software и процессора intel pentium 60/66.";
        src/rpc/CalendarHistory.java:26  a[16] = "1996 • 3dfx voodoo graphics совершает революцию 3d-ускорителей. релиз quake.";
OK 471. Реальное время (как тамагочи).                             hits=3
        src/rpc/Shell.java:458  this.println((s1.equals((object)"time")) ? "текущее время: 12:00" : "дата: 01-01-1990");
        src/rpc/BuildView.java:405  case 4: return "промотать время";
-- 472. Сезоны.
OK 473. Смена поколений.                                           hits=2
        src/rpc/Game.java:5502  "поколений")
        src/rpc/Game.java:5511  /** russian plural form for a count (1 поколение / 2 поколения / 5 поколений). */
OK 474. Новый год 2027.                                            hits=2
        src/rpc/WorkshopTools.java:185  this.toolfeedback = new stringbuffer("✓ вздутый электролит c").append(this.selcap + 1).append(" выпаян, запаян…
        src/rpc/City.java:188  return "☕ вы взяли новый заказ на ремонт и выпили отличный кофе!";
-- 475. Итоги.
OK 476. Максимум графики.                                          hits=5
        src/rpc/LifeSim.java:380  this.lasteventmsg = "вы сварили горячие пельмени со сметаной. сытость на максимуме!";
        src/rpc/Game.java:536  a[5] = this.makeorder("ретро-клуб", "апгрейд графики для 3d-игр", 5, 5200, 90, i, "установить мощную видеокарт…
OK 477. Оптимизация через .pak.                                    hits=13
        src/rpc/Os.java:62  a[2] = rpc.os.task(0, 21, "сумма 1..6 через цикл", 60 + (i - 1980) * 3);
        src/rpc/Game.java:5922  this.toastmsg(new stringbuffer("готовый системник из ").append(a.length).append(" деталей — приедет через ").a…
OK 478. Процедурный звук.                                          hits=17
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
-- 479. Автосохранение.
OK 480. Идеальная работа (без вылетов).                            hits=9
        src/rpc/Shell.java:524  this.println("программа отработала (вывод в меню «программы»).");
        src/rpc/Game.java:6465  return "модем из будущего — не работает";
OK 481. Забыл зонт.                                                hits=4
        src/rpc/Browser.java:249  a3[7] = "забыл снять плёнку с кулера»";
        src/rpc/Browser.java:439  a4[3] = "забыл про сокет — не влезло.";
OK 482. Мышь застряла.                                             hits=7
        src/rpc/Score.java:50  a[16] = "мышь";
        src/rpc/Score.java:72  a0[16] = "мышь";
OK 483. Пыль на полке.                                             hits=16
        src/rpc/WorkshopTools.java:32  a[3] = "сжатый воздух (пыль)";
        src/rpc/WorkshopTools.java:127  this.toolfeedback = new stringbuffer("пыль сдувается... осталось ").append(this.dustremaining).append("% пыли …
~  484. Кот скинул вазу.                                           hits=1
        src/rpc/Funcs.java:307  "кот лёг на радиатор", "кот скинул мышку",
OK 485. Муха в комнате.                                            hits=3
        src/rpc/LifeSim.java:111  this.lasteventmsg = "за окном кружится легкий снег. в комнате тепло и пахнет чаем.";
        src/rpc/LifeSim.java:362  this.lasteventmsg = "вы заварили свежий горячий чай с лимоном. по комнате пошёл пар.";
OK 486. Соседский Wi-Fi.                                           hits=10
        src/rpc/Game.java:5240  a0.append(" • wi-fi");
        src/rpc/Game.java:6458  return "гигабит и wi-fi";
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
        src/rpc/Game.java:969  this.toastmsg("игра сохранена в память!");
OK 493. Синий экран из-за модов.                                   hits=7
        src/rpc/Game.java:7239  this.toastmsg("спикер выключен — ошибку видно только на экране");
        src/rpc/Game.java:8040  a.drawtext("и текст синего экрана. свой", 8, i6, 0);
OK 494. Драйвер видеокарты слетел.                                 hits=21
        src/rpc/CalendarHistory.java:42  a[32] = "2012 • стандарт быстрой памяти ddr3-2400 и видеокарты nvidia geforce gtx 680 (kepler).";
        src/rpc/CalendarHistory.java:46  a[36] = "2016 • видеокарты nvidia geforce gtx 1080 (pascal) и массовое внедрение nvme ssd.";
OK 495. Звук пропал.                                               hits=17
        src/rpc/CalendarHistory.java:18  a[8] = "1988 • запуск звуковой карты creative sound blaster 1.0 и рождение интернет-червя морриса.";
        src/rpc/CalendarHistory.java:22  a[12] = "1992 • появление 3d-шутера wolfenstein 3d и стандарта звука sound blaster 16.";
OK 496. Курсор завис.                                              hits=2
        src/rpc/Browser.java:547  a3[3] = "энергозависима и нужна всегда.";
        src/rpc/Funcs.java:311  "кот охотится на курсор", "кот на клавиатуре",
-- 497. Автосохранение не сработало.
OK 498. Сгорел блок питания.                                       hits=22
        src/rpc/CalendarHistory.java:39  a[29] = "2009 • выход популярнейшей операционной системы windows 7 и майнинг первого блока bitcoin.";
        src/rpc/Setup.java:80  return "secure boot блокирует старые загрузчики";
OK 499. Вздулись конденсаторы.                                     hits=6
        src/rpc/WorkshopTools.java:30  a[1] = "паяльник (конденсаторы)";
        src/rpc/Game.java:5623  "пайка, термопаста, пыль, конденсаторы",
OK 500. Термопаста засохла.                                        hits=5
        src/rpc/WorkshopTools.java:31  a[2] = "термопаста (слой кпт-8)";
        src/rpc/WorkshopTools.java:176  this.toolfeedback = new stringbuffer("! слишком толстый слой (").append(this.pastethickness).append("%). термо…
