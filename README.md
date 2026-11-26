# A-Music (temporary name)

Android music player. Change the name with `APP_NAME` in `gradle.properties`.

## Build
Requires JDK 17 and Android SDK 36.

    ./gradlew :core:common:test testDebugUnitTest lintDebug assembleDebug

APK: `app/build/outputs/apk/debug/`. GitHub Actions uploads it as `A-Music-debug-apk`.

## Install
Copy the APK to the phone (Android 8.0 or newer) and open it, allowing installation
from that source. Or run `adb install <apk>`.

## Documentation
- [Architecture](documentation/ARCHITECTURE.md)
- [Development log](documentation/DEVLOG.md)
