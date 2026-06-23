# PR1 Scaffold Design — kparchment

**Date:** 2026-06-23
**Scope:** Initial Kotlin Multiplatform project scaffolding. No library features — establishes build structure, source set hierarchy, and CI.

---

## Targets

Only Stable platforms from the [Kotlin Multiplatform supported platforms matrix](https://kotlinlang.org/docs/multiplatform/supported-platforms.html):

| Platform | KMP targets | Source set |
|---|---|---|
| JVM (Desktop + Server) | `jvm()` | `jvmMain`, `jvmTest` |
| Android | `androidTarget()` | `androidMain`, `androidTest` |
| iOS (device) | `iosArm64()` | `iosArm64Main` (via `iosMain`) |
| iOS (Intel sim) | `iosX64()` | `iosX64Main` (via `iosMain`) |
| iOS (Apple Silicon sim) | `iosSimulatorArm64()` | `iosSimulatorArm64Main` (via `iosMain`) |
| Web / JS | `js(IR)` | `jsMain`, `jsTest` |

Excluded (Beta): `wasmJs`, `wasmWasi`, `watchOS`, `tvOS`
Excluded (niche): macOS native, Linux native, Windows native

---

## Module Structure

Single library module. Multi-module was considered and rejected — PDF parsing is a cohesive API with shared byte-stream parsing primitives. Text extraction, metadata, and document structure are not independently useful subsystems. Can be revisited if concrete reasons emerge.

```
kparchment/
├── .github/
│   └── workflows/
│       └── ci.yml
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml
├── kparchment/
│   ├── src/
│   │   ├── commonMain/kotlin/io/kparchment/
│   │   ├── commonTest/kotlin/io/kparchment/
│   │   ├── jvmMain/kotlin/io/kparchment/
│   │   ├── jvmTest/kotlin/io/kparchment/
│   │   ├── androidMain/kotlin/io/kparchment/
│   │   ├── androidTest/kotlin/io/kparchment/
│   │   ├── iosMain/kotlin/io/kparchment/
│   │   ├── iosArm64Main/kotlin/io/kparchment/
│   │   ├── iosX64Main/kotlin/io/kparchment/
│   │   ├── iosSimulatorArm64Main/kotlin/io/kparchment/
│   │   ├── jsMain/kotlin/io/kparchment/
│   │   └── jsTest/kotlin/io/kparchment/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── LICENSE
└── README.md
```

---

## Gradle Configuration

### `gradle/libs.versions.toml`

```toml
[versions]
kotlin = "2.1.21"
agp = "8.10.1"

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "agp" }
```

### `settings.gradle.kts`

```kotlin
rootProject.name = "kparchment"
include(":kparchment")
```

### Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
}
```

### `kparchment/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    js(IR) { browser(); nodejs() }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies { }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "io.kparchment"
    compileSdk = 35
    defaultConfig { minSdk = 21 }
}
```

`applyDefaultHierarchyTemplate()` auto-wires `iosMain` as an intermediate source set for the three iOS targets.

---

## CI

Single macOS job. macOS runner required for iOS compilation; all other targets also compile on macOS.

```yaml
name: CI

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

jobs:
  check:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Check all targets
        run: ./gradlew check
```

Java 17 is required by AGP 8.x. `gradle/actions/setup-gradle` handles Gradle caching automatically.

Future optimization: split into a `linux` job (JVM/Android/JS) and `macos` job (iOS) to reduce macOS runner minutes (~10x more expensive than Linux).

---

## Testing Locally

```bash
# Inner loop — fastest
./gradlew :kparchment:jvmTest

# Android unit tests (no emulator needed)
./gradlew :kparchment:testDebugUnitTest

# JS tests (requires Node.js)
./gradlew :kparchment:jsTest

# iOS (macOS + Xcode required)
./gradlew :kparchment:iosSimulatorArm64Test

# Full check (mirrors CI)
./gradlew check

# Compile only, no tests
./gradlew :kparchment:assemble
```

---

## Out of Scope for PR1

- Publishing to Maven Central
- Any PDF parsing logic
- Code coverage, lint, or static analysis
- wasmJs, wasmWasi, macOS native, Linux native, Windows native targets
