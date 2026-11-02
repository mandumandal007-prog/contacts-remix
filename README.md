# Contacts Remix

An original Kotlin + Jetpack Compose contacts app inspired by the publicly listed OnePlus Contacts release. This is an independent implementation, not affiliated with OnePlus, and includes no copied proprietary code, branding, or assets.

## Features
- On-device contacts list, search, and favorites
- Contact calling card with call, message, and edit actions
- Add contacts through the Android Contacts app
- Keypad opens the system dialer for calls
- Settings screen with dark theme toggle and contact refresh
- Android 13+ support (minSdk 33), including Realme C25s class devices

The app reads the Android Contacts Provider after asking for the contacts permission. Call and message actions hand off to the device's system apps. Call history is not read by this app.

## Build
Run `./gradlew assembleDebug`. GitHub Actions builds and publishes the debug APK as a workflow artifact on pushes to `main`.

## Reference
APKMirror lists OnePlus Contacts 16.80.15, uploaded August 11, 2026, with Android 15+ minimum, arm64-v8a, nodpi, and a listed APK size of 88.88 MB. The release page does not provide feature notes or screenshots, so the app uses general contacts-app conventions as inspiration rather than claiming to reproduce unverified OEM-specific features.

Reference: https://www.apkmirror.com/apk/oneplus-ltd/oneplus-contacts-4/oneplus-contacts-16-80-15-release/
