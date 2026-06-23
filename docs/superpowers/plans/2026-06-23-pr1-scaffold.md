# PR1 Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bootstrap kparchment as a Kotlin Multiplatform library module targeting JVM, Android, iOS, and JS with a passing CI workflow.

**Architecture:** Single Gradle module (`kparchment/`) with KMP targets declared in `kparchment/build.gradle.kts`. Source sets fan out from `commonMain` using `applyDefaultHierarchyTemplate()` to auto-wire `iosMain` as an intermediate source set. No library logic — placeholder test only.

**Tech Stack:** Kotlin 2.1.21, Kotlin Multiplatform, AGP 8.10.1, Gradle 8.13, GitHub Actions

## Global Constraints

- Package: `io.kparchment` throughout all source sets
- Kotlin version: `2.1.21`
- AGP version: `8.10.1`
- Gradle version: `8.13`
- Java toolchain: `17` (required by AGP 8.x)
- Android `compileSdk = 35`, `minSdk = 21`
- JS target must enable both `browser()` and `nodejs()`
- All source dirs use `.gitkeep` to track empty directories in git
- CI runs on `macos-latest` (required for iOS compilation)

---

### Task 1: Bootstrap Gradle Wrapper

**Files:**
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/wrapper/gradle-wrapper.jar` (generated)
- Create: `gradlew` (generated)
- Create: `gradlew.bat` (generated)
- Create: `gradle.properties`

**Interfaces:**
- Produces: `./gradlew` command available for all subsequent tasks

- [ ] **Step 1: Install Gradle if not already present**

On macOS with Homebrew:
```bash
brew install gradle
gradle --version
```
Expected output includes: `Gradle 8.x` (any 8.x is fine for bootstrapping)

- [ ] **Step 2: Generate the Gradle wrapper**

```bash
cd /path/to/kparchment
gradle wrapper --gradle-version 8.13 --distribution-type bin
```

Expected output:
```
BUILD SUCCESSFUL
```

This generates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, and `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 3: Verify the wrapper works**

```bash
./gradlew --version
```

Expected output includes:
```
Gradle 8.13
```

- [ ] **Step 4: Create `gradle.properties`**

```properties
kotlin.code.style=official
android.useAndroidX=true
```

- [ ] **Step 5: Commit**

```bash
git add gradlew gradlew.bat gradle/wrapper/ gradle.properties
git commit -m "chore: add Gradle 8.13 wrapper"
```

---

### Task 2: Version Catalog and Settings

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `settings.gradle.kts`

**Interfaces:**
- Produces: `libs.plugins.kotlin.multiplatform` and `libs.plugins.android.library` aliases available to all `build.gradle.kts` files

- [ ] **Step 1: Create `gradle/libs.versions.toml`**

```toml
[versions]
kotlin = "2.1.21"
agp = "8.10.1"

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "agp" }
```

- [ ] **Step 2: Create `settings.gradle.kts`**

```kotlin
rootProject.name = "kparchment"
include(":kparchment")
```

- [ ] **Step 3: Verify Gradle syncs without error**

```bash
./gradlew help
```

Expected: `BUILD SUCCESSFUL` (no plugin or version resolution errors)

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts
git commit -m "chore: add version catalog and settings"
```

---

### Task 3: Root Build File

**Files:**
- Create: `build.gradle.kts` (root)

**Interfaces:**
- Consumes: `libs.plugins.kotlin.multiplatform`, `libs.plugins.android.library` from Task 2
- Produces: plugins declared at root level with `apply false` so submodules can apply them

- [ ] **Step 1: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
}
```

- [ ] **Step 2: Verify Gradle syncs without error**

```bash
./gradlew help
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "chore: add root build file"
```

---

### Task 4: Library Module Build File and Source Directories

**Files:**
- Create: `kparchment/build.gradle.kts`
- Create: `kparchment/src/commonMain/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/jvmMain/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/jvmTest/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/androidMain/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/androidTest/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/iosMain/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/iosArm64Main/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/iosX64Main/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/iosSimulatorArm64Main/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/jsMain/kotlin/io/kparchment/.gitkeep`
- Create: `kparchment/src/jsTest/kotlin/io/kparchment/.gitkeep`

**Interfaces:**
- Consumes: plugins from Task 3
- Produces: `:kparchment:assemble` task that compiles all targets

- [ ] **Step 1: Create `kparchment/build.gradle.kts`**

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

- [ ] **Step 2: Create all source directories**

Run these commands to create the empty directories tracked by git:

```bash
mkdir -p kparchment/src/commonMain/kotlin/io/kparchment
mkdir -p kparchment/src/commonTest/kotlin/io/kparchment
mkdir -p kparchment/src/jvmMain/kotlin/io/kparchment
mkdir -p kparchment/src/jvmTest/kotlin/io/kparchment
mkdir -p kparchment/src/androidMain/kotlin/io/kparchment
mkdir -p kparchment/src/androidTest/kotlin/io/kparchment
mkdir -p kparchment/src/iosMain/kotlin/io/kparchment
mkdir -p kparchment/src/iosArm64Main/kotlin/io/kparchment
mkdir -p kparchment/src/iosX64Main/kotlin/io/kparchment
mkdir -p kparchment/src/iosSimulatorArm64Main/kotlin/io/kparchment
mkdir -p kparchment/src/jsMain/kotlin/io/kparchment
mkdir -p kparchment/src/jsTest/kotlin/io/kparchment

touch kparchment/src/commonMain/kotlin/io/kparchment/.gitkeep
touch kparchment/src/jvmMain/kotlin/io/kparchment/.gitkeep
touch kparchment/src/jvmTest/kotlin/io/kparchment/.gitkeep
touch kparchment/src/androidMain/kotlin/io/kparchment/.gitkeep
touch kparchment/src/androidTest/kotlin/io/kparchment/.gitkeep
touch kparchment/src/iosMain/kotlin/io/kparchment/.gitkeep
touch kparchment/src/iosArm64Main/kotlin/io/kparchment/.gitkeep
touch kparchment/src/iosX64Main/kotlin/io/kparchment/.gitkeep
touch kparchment/src/iosSimulatorArm64Main/kotlin/io/kparchment/.gitkeep
touch kparchment/src/jsMain/kotlin/io/kparchment/.gitkeep
touch kparchment/src/jsTest/kotlin/io/kparchment/.gitkeep
```

Note: `commonTest` gets a real `.kt` file in Task 5, so no `.gitkeep` needed there.

- [ ] **Step 3: Verify the module compiles**

```bash
./gradlew :kparchment:assemble
```

Expected: `BUILD SUCCESSFUL`

If you see `SDK location not found`, you need to set your Android SDK path. Create `local.properties` in the root:
```properties
sdk.dir=/Users/<your-username>/Library/Android/sdk
```
(This file should NOT be committed — it's already in `.gitignore`.)

- [ ] **Step 4: Commit**

```bash
git add kparchment/
git commit -m "chore: add library module with KMP targets"
```

---

### Task 5: Placeholder Test

**Files:**
- Create: `kparchment/src/commonTest/kotlin/io/kparchment/PlaceholderTest.kt`

**Interfaces:**
- Consumes: `kotlin("test")` dependency declared in Task 4's `commonTest.dependencies`
- Produces: `:kparchment:jvmTest` passes (confirms test infrastructure works)

- [ ] **Step 1: Create the placeholder test**

```kotlin
// kparchment/src/commonTest/kotlin/io/kparchment/PlaceholderTest.kt
package io.kparchment

import kotlin.test.Test
import kotlin.test.assertTrue

class PlaceholderTest {

    @Test
    fun placeholder() {
        assertTrue(true)
    }
}
```

- [ ] **Step 2: Run the test on JVM**

```bash
./gradlew :kparchment:jvmTest
```

Expected:
```
PlaceholderTest > placeholder PASSED

BUILD SUCCESSFUL
```

- [ ] **Step 3: Run the test on JS**

```bash
./gradlew :kparchment:jsTest
```

Expected: `BUILD SUCCESSFUL`

Note: JS tests require Node.js. Install via `brew install node` if missing.

- [ ] **Step 4: Run check on all targets**

```bash
./gradlew check
```

Expected: `BUILD SUCCESSFUL`

iOS targets will compile and run tests on macOS (requires Xcode). If Xcode is not installed, iOS compilation will fail — that's expected in that environment; CI handles it.

- [ ] **Step 5: Commit**

```bash
git add kparchment/src/commonTest/
git commit -m "test: add placeholder test to verify KMP test infrastructure"
```

---

### Task 6: CI Workflow

**Files:**
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: `./gradlew check` from Task 5
- Produces: CI runs on every push and PR to `master`

- [ ] **Step 1: Create `.github/workflows/ci.yml`**

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

- [ ] **Step 2: Verify the YAML is valid**

```bash
cat .github/workflows/ci.yml
```

Confirm the file looks correct — no tab characters (YAML requires spaces), correct indentation.

- [ ] **Step 3: Commit and push**

```bash
git add .github/
git commit -m "ci: add GitHub Actions workflow running gradlew check on all targets"
git push origin master
```

- [ ] **Step 4: Verify CI passes on GitHub**

Open the repository on GitHub → Actions tab → confirm the `CI` workflow run triggered and passes.

Expected: all steps green, `BUILD SUCCESSFUL` in the `Check all targets` step logs.
