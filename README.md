# Contacts Remix

An original Kotlin + Jetpack Compose contacts and phone app inspired by publicly described OnePlus Contacts reference imagery and flows. It is not affiliated with OnePlus and contains no copied OEM code or assets.

Target: Android 13+ (minSdk 33). Visual design uses a clean, light OnePlus-inspired red accent, compact contact rows, alphabetical navigation, contact details and familiar bottom navigation. Uses Android's Contacts Provider and system dialer intents for device contacts and calls.

## Current scope
- Contacts list with search and alphabetical index
- Contact details, add and edit through Android Contacts app, favorites
- Recents and a simple keypad that hands calls off to the system phone app
- Android 13 runtime contacts permission

Reference: https://www.apkmirror.com/apk/oneplus-ltd/oneplus-contacts-4/oneplus-contacts-16-80-15-release/ . APKMirror lists version 16.80.15 (version code 160800015), uploaded August 11, 2026, Android 15+ minimum, arm64-v8a, nodpi. The release page does not show feature notes or screenshots. This project therefore implements the requested general contacts/dialer experience, not an OEM-exact recreation.

Build locally with `./gradlew assembleDebug`. GitHub Actions builds an APK on pushes to `main`.
