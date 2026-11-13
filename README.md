# HomeoRemedica for Android

Native Kotlin and Jetpack Compose client for [HomeoRemedica](https://github.com/rasagyavatsal/homeoremedica-app):
the same books and cited answers as the web app, through its API. Historical reference only; not medical
advice.

## Build

Requires JDK 17+ and the Android SDK (compileSdk 37); the Gradle wrapper fetches the rest.

```sh
./gradlew testDebugUnitTest assembleDebug   # APK in app/build/outputs/apk/debug/
./gradlew installDebug -Phomeoremedica.apiBaseUrl=http://10.0.2.2:8000
```

Builds use the production server; the second command points one at `npm start` in homeoremedica-app
from the emulator. Debug builds allow plain HTTP to `10.0.2.2` and `localhost` only.

Accounts need `app/google-services.json`. Without it, chats stay on the device. To get it, add an
Android app `com.rasagyavatsal.homeoremedica` to the Firebase project with the SHA-1s of the debug key,
the upload key and Play's signing key, then download its `google-services.json` into `app/`.

## Device

```sh
make              # list the commands
make run          # build, install and open the app
make local        # the same, against npm start on this machine
make emu          # open the screen with scrcpy
make logs         # follow the app's log
make shot         # screenshot to build/screenshots/
make test         # unit tests
make e2e          # end-to-end tests; TEST=ChatTest runs one class
```

`make e2e` starts homeoremedica-app's e2e server and the Firebase emulators unless they are running,
so it needs homeoremedica-app checked out beside this repository (or `APP_DIR`) with `npm install`, plus
Node and Java. The device reaches them through `adb reverse`.

### Remote emulator

To run the emulator on another Linux machine, install the Android SDK there, and run
`emulator -avd <name> -no-window -ports 5554,5555` as a systemd user service named `android-emulator`,
with lingering on:

```sh
loginctl enable-linger
```

Before its first boot, copy this machine's `~/.android/adbkey` and `adbkey.pub` to that machine's
`~/.android/`. Then put its SSH address in `local.mk`:

```make
EMULATOR_HOST = user@192.168.1.20
```

The make commands start it, tunnel adb over SSH and tunnel port 8000 back, so `10.0.2.2:8000` still
reaches `npm start` here.

```sh
make connect      # connect adb without a window
make disconnect   # close the tunnel, leave the emulator running
make restart      # restart the remote emulator
make stop         # stop it
```

## Release

```sh
./gradlew publishBundle    # upload to Play's internal testing
./gradlew publishListing   # update the store listing from app/src/main/play
```

These need `keystore.properties` with the upload key (copy `keystore.properties.example`) and a Play
service account key named by `homeoremedica.playCredentials` in `~/.gradle/gradle.properties` or held in
`ANDROID_PUBLISHER_CREDENTIALS`. `versionCode` is set one above Play's highest, so upload this way.

Merging release-please's PR on `main` tags a `v*` release and bumps `version.txt`; the `Deploy` workflow
then runs `./gradlew test publishBundle` with the secrets `ANDROID_KEYSTORE_BASE64`,
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` and `PLAY_SERVICE_ACCOUNT_JSON`.

## License

[MIT](LICENSE).
