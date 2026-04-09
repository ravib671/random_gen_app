# Random Number Cycles (Android)

This Android app has two independent sections (**Part A** and **Part B**) to generate random numbers from `1..N`.

## Features
- Two separate generator panels with their own controls.
- Repeat mode:
  - **ON**: each click gives a random number from `1..N`.
  - **OFF**: each cycle emits every number in `1..N` exactly once (shuffled order).
- Cycle progress/status indicator and cycle completion message.

## Build APK locally
> Use Java 17 for Android Gradle compatibility.

```bash
# from project root
export JAVA_HOME=/path/to/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"

# if you use local Gradle
gradle assembleDebug
```

Generated APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install on phone
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
