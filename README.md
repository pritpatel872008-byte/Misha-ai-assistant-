# Misha AI Assistant

Native Android assistant project (Kotlin + Jetpack Compose).

## Build

GitHub Actions workflow: `.github/workflows/android-apk.yml`

1. Open the repository's **Actions** tab.
2. Select **Build Android APK**.
3. Select **Run workflow** and choose the implementation branch.
4. Download the `Misha-AI-APK` artifact from a successful run.

The debug APK is an installable development build, not a Play Store-signed release.

## Important Android limits

- Microphone access and background listening depend on Android version, runtime permissions, foreground-service rules, and device power management.
- Third-party app automation depends on the target app's UI and user-enabled Accessibility service; UI changes can break automation.
- SMS, calls, contacts, location, camera, and Accessibility require explicit permissions and must be exercised on a real device.
- A successful Gradle build does not prove every feature works on a device. Feature claims require functional verification.

## Development safety

Work on feature branches, preserve the existing audited UI, do not commit API keys, signing keys, or personal data, and do not report actions as successful unless the app verifies the result.
