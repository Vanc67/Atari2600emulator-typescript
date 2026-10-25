# apk-toolchain — сборка подписанного APK без Gradle, Android Studio и javac

Это **конвейер**, а не приложение. Он превращает любую статическую веб-сборку
(`index.html` + ассеты) в настоящий устанавливаемый `.apk`, подписанный по
схемам v1+v2+v3.

Он появился из ограничения среды, а не из любви к искусству: в песочнице, где
это написано, есть `gcc`, Node и Python, но

* нет `javac` — доступный рантайм (`jdk4py` из PyPI) это JRE: `java`, `keytool`,
  `jfr`, но не компилятор;
* нет Android SDK и нет `dl.google.com`, откуда он обычно ставится;
* недоступны `repo.maven.apache.org` и `objects.githubusercontent.com`, то есть
  ни Maven-артефактов, ни бинарников из GitHub-релизов;
* каждый ECJ-джарник, найденный через GitHub code search, оказался указателем
  Git LFS (`media.githubusercontent.com` тоже закрыт).

Поэтому два шага сделаны вручную:

1. **`.class` пишется байтами.** `tools/mkclass.py` — минимальный писатель
   формата Java class-файла (constant pool, поля, методы, атрибут `Code`,
   двухпроходная резолюция меток). Целевая версия — **49 (Java 5)** намеренно:
   class-файлы младше 50 проверяются выводом типов, поэтому не нужен
   `StackMapTable`, а это самая ломкая часть ручного байткода.
   `tools/mkactivity.py` собирает из этого `MainActivity`.
2. **Иконки рисуются без Pillow.** `tools/make_icons.py` — свой кодировщик PNG
   на `zlib` + процедурная отрисовка: скруглённый тайл, градиент, экран, кнопка
   Home. Пять плотностей от mdpi до xxxhdpi.

Остальное — настоящие Android build-tools. `aapt2`, `d8`, `apksigner`,
`zipalign` и `android.jar` взяты из зеркала SDK на GitHub
(`ItzVladik/android-sdk`, build-tools 36.1.0 + platforms/android-33) и
скачаны поблочно через Git blob API, потому что целый тарбол репозитория —
556 МБ, а нужных файлов всего 31 на 158 МБ.

## Как пользоваться

```bash
./tools/build-apk.sh                              # payload = ./www
./tools/build-apk.sh --payload ../../dist         # любая статическая сборка (vite, webpack…)
./tools/build-apk.sh --out build/app.apk --version-name 1.2 --version-code 3
./tools/build-apk.sh --stop-after dex             # остановиться после classes.dex
./tools/build-apk.sh -v                           # печатать каждую команду
```

Переменные окружения: `ANDROID_SDK`, `JAVA_HOME`, `APK_KEYSTORE`,
`APK_KEYSTORE_PASS`. Без `JAVA_HOME` скрипт сам ищет `jdk4py` в venv'ах и
`/usr/lib/jvm`.

Шаги: `mkactivity.py` → `d8` → `make_icons.py` → `aapt2 compile`/`link` →
добавить `classes.dex` в пакет (STORED, чтобы `zipalign` выровнял его по 4
байтам, как хочет ART) → `zipalign -f 4` → `keytool` (если ключа нет) →
`apksigner sign`.

## Проверено фактически

`apksigner verify --print-certs --verbose` на собранном APK:

```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
Number of signers: 1
Signer #1 certificate DN: CN=apk-toolchain, OU=Arena, O=Arena, L=Unknown, ST=Unknown, C=US
Signer #1 key algorithm: RSA / 2048 bits
```

`aapt2 dump badging`:

```
package: name='com.arena.apkshell' versionCode='1' versionName='1.0' compileSdkVersion='33'
targetSdkVersion:'33'
uses-permission: name='android.permission.INTERNET'
application-label:'iOS on QEMU'
application-icon-160..640: res/mipmap-{m,h,xh,xxh,xxxh}dpi-v4/ic_launcher.png
launchable-activity: name='com.arena.apkshell.MainActivity'
```

`dexdump -d build/dex/classes.dex` подтверждает, что байткод именно тот, что
описан в `android/src/com/arena/apkshell/MainActivity.java`: `onCreate` вызывает
`super.onCreate`, `addFlags(1152)`, `setSystemUiVisibility(1284)`, создаёт
`WebView`, прогоняет 14 настроек `WebSettings`, ставит `WebChromeClient` и
`WebViewClient`, `setBackgroundColor(-16777216)`, `setOverScrollMode(2)`,
`loadUrl("file:///android_asset/www/index.html")`, `setContentView`;
`onBackPressed` компилируется в две correct ветки `if-eqz` вокруг
`canGoBack()`/`goBack()`.

Содержимое пакета:

```
AndroidManifest.xml        resources.arsc
classes.dex                (STORED, выровнен)
assets/www/index.html      (полезная нагрузка)
res/mipmap-*-v4/ic_launcher.png
META-INF/{MANIFEST.MF,APKSHELL.SF,APKSHELL.RSA}
```

## Что в payload'е по умолчанию

`www/index.html` — оффлайн-справочник «Настоящая iOS в QEMU»: таблица SoC и
наличия моделей машин, четыре конфигурации, которые действительно работают
(iOS 14 / iPhone 11 через Inferno, iOS 16 / iPhone X через `lgrachov/ios-qemu`,
iPhoneOS 1.0 / iPod touch 1G через `qemu-ios`, iOS 12.1 shell через
`xnu-qemu-arm64`), требования к железу и причина, по которой iOS 7–9 на iPhone
4s не запустится ни у кого. Это приложение-справочник к каталогу
[`../ios-on-qemu/`](../ios-on-qemu/), а не эмулятор.

## Ограничения — честно

* Это **WebView-обёртка**. Никакой нативной производительности, никакого
  прямого доступа к железу beyond того, что даёт WebView: `localStorage`,
  WebAudio, `getUserMedia` (но `onPermissionRequest` в `WebChromeClient` не
  переопределён — для камеры/микрофона нужен второй класс), DeviceOrientation.
* Ключ **debug**, самоподписанный, пароль `android`, лежит в `build/` и
  заигнорен git'ом. Для публикации нужен свой release-ключ и `--ks`.
* `compileSdk`/`targetSdk` = 33, потому что такой `android.jar` скачан.
  Поднять — значит скачать другой `android.jar`.
* Нет нативных библиотек (`jniLibs`), нет split APK, нет AAB, нет ProGuard/R8
  (d8 вызван без shrinking), нет ресурсов кроме иконки и одной строки.
* `android:screenOrientation="portrait"` и `Theme.NoTitleBar.Fullscreen`
  зашиты в манифест; навигационная панель Android **не** прячется
  (`IMMERSIVE_STICKY` не включён сознательно — он съедает свайп снизу, который
  payload-приложения обычно биндят на себя).
* Чтобы поменять Java-логику, править нужно **оба** файла:
  `android/src/.../MainActivity.java` (читаемый оригинал) и
  `tools/mkactivity.py` (то, что реально собирается). Первый не компилируется —
  это документация. Сверять их удобнее всего через `dexdump -d`.
