# PR2 PdfByteReader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `PdfParseException` and `PdfByteReader` in `commonMain` — the seekable byte-level foundation every parsing layer above depends on.

**Architecture:** Two plain Kotlin classes in `commonMain`, no `expect/actual`, no platform dependencies. `PdfByteReader` wraps a `ByteArray` with a mutable `position: Int`. All error paths throw `PdfParseException` carrying the byte offset at the time of failure.

**Tech Stack:** Kotlin 2.1.21, kotlin.test (commonTest)

## Global Constraints

- All source in `kparchment/src/commonMain/kotlin/io/kparchment/`
- All tests in `kparchment/src/commonTest/kotlin/io/kparchment/`
- Package: `io.kparchment`
- `PdfParseException.byteOffset` type: `Long`
- `PdfByteReader.position` is publicly readable, privately settable
- `seek(data.size)` is valid (EOF boundary); `seek(index > data.size)` throws
- `readByte()`, `peek()`, `readBytes(n)` throw `PdfParseException` at EOF
- `readLine()` returns `""` at EOF — no exception
- `readLine()` handles both `\n` and `\r\n`; strips terminator from result
- No `expect/actual` — pure `ByteArray` in `commonMain` only
- Run tests with: `./gradlew :kparchment:jvmTest`

---

### Task 1: PdfParseException

**Files:**
- Create: `kparchment/src/commonMain/kotlin/io/kparchment/PdfParseException.kt`
- Test: `kparchment/src/commonTest/kotlin/io/kparchment/PdfByteReaderTest.kt`

**Interfaces:**
- Produces: `PdfParseException(message: String, byteOffset: Long)` — used by Task 2 and every parsing layer in future PRs

- [ ] **Step 1: Write the failing test**

Create `kparchment/src/commonTest/kotlin/io/kparchment/PdfByteReaderTest.kt`:

```kotlin
package io.kparchment

import kotlin.test.Test
import kotlin.test.assertEquals

class PdfByteReaderTest {

    @Test
    fun pdfParseExceptionCarriesMessageAndOffset() {
        val ex = PdfParseException("test error", 42L)
        assertEquals("test error", ex.message)
        assertEquals(42L, ex.byteOffset)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :kparchment:jvmTest
```

Expected: FAIL — `error: unresolved reference: PdfParseException`

- [ ] **Step 3: Create `PdfParseException.kt`**

```kotlin
package io.kparchment

class PdfParseException(
    message: String,
    val byteOffset: Long
) : Exception(message)
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :kparchment:jvmTest
```

Expected:
```
PdfByteReaderTest > pdfParseExceptionCarriesMessageAndOffset PASSED

BUILD SUCCESSFUL
```

- [ ] **Step 5: Commit**

```bash
git add kparchment/src/commonMain/kotlin/io/kparchment/PdfParseException.kt \
        kparchment/src/commonTest/kotlin/io/kparchment/PdfByteReaderTest.kt
git commit -m "feat: add PdfParseException with byte offset"
```

---

### Task 2: PdfByteReader

**Files:**
- Create: `kparchment/src/commonMain/kotlin/io/kparchment/PdfByteReader.kt`
- Modify: `kparchment/src/commonTest/kotlin/io/kparchment/PdfByteReaderTest.kt`

**Interfaces:**
- Consumes: `PdfParseException(message: String, byteOffset: Long)` from Task 1
- Produces:
  ```kotlin
  class PdfByteReader(data: ByteArray) {
      val position: Int
      fun seek(index: Int): Unit
      fun readByte(): Byte
      fun readBytes(n: Int): ByteArray
      fun readLine(): String
      fun peek(): Byte
      fun isEof(): Boolean
  }
  ```

- [ ] **Step 1: Write all 17 failing tests**

Replace the contents of `PdfByteReaderTest.kt` with the full test suite (keeping the existing `pdfParseExceptionCarriesMessageAndOffset` test):

```kotlin
package io.kparchment

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfByteReaderTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun readerOf(vararg bytes: Byte) = PdfByteReader(byteArrayOf(*bytes))
    private fun readerOf(s: String) = PdfByteReader(s.encodeToByteArray())

    // ── PdfParseException ────────────────────────────────────────────────────

    @Test
    fun pdfParseExceptionCarriesMessageAndOffset() {
        val ex = PdfParseException("test error", 42L)
        assertEquals("test error", ex.message)
        assertEquals(42L, ex.byteOffset)
    }

    // ── position and seek ────────────────────────────────────────────────────

    @Test
    fun initialPositionIsZero() {
        val r = readerOf(0x00, 0x01, 0x02)
        assertEquals(0, r.position)
    }

    @Test
    fun seekSetsPosition() {
        val r = readerOf(0x00, 0x01, 0x02)
        r.seek(2)
        assertEquals(2, r.position)
    }

    @Test
    fun seekNegativeThrows() {
        val r = readerOf(0x00)
        assertFailsWith<PdfParseException> { r.seek(-1) }
    }

    @Test
    fun seekBeyondSizeThrows() {
        val r = readerOf(0x00, 0x01)
        assertFailsWith<PdfParseException> { r.seek(3) }
    }

    @Test
    fun seekToExactEofIsValid() {
        val r = readerOf(0x00, 0x01)
        r.seek(2) // data.size == 2, valid EOF boundary
        assertTrue(r.isEof())
    }

    // ── readByte ─────────────────────────────────────────────────────────────

    @Test
    fun readByteReturnsCorrectByteAndAdvancesPosition() {
        val r = readerOf(0x41, 0x42)
        assertEquals(0x41.toByte(), r.readByte())
        assertEquals(1, r.position)
    }

    @Test
    fun readByteAtEofThrows() {
        val r = readerOf(0x41)
        r.readByte()
        assertFailsWith<PdfParseException> { r.readByte() }
    }

    // ── peek ─────────────────────────────────────────────────────────────────

    @Test
    fun peekReturnsByteWithoutAdvancingPosition() {
        val r = readerOf(0x41, 0x42)
        assertEquals(0x41.toByte(), r.peek())
        assertEquals(0, r.position)
    }

    @Test
    fun peekAtEofThrows() {
        val r = PdfByteReader(ByteArray(0))
        assertFailsWith<PdfParseException> { r.peek() }
    }

    // ── readBytes ────────────────────────────────────────────────────────────

    @Test
    fun readBytesReturnsSliceAndAdvancesPosition() {
        val r = readerOf(0x41, 0x42, 0x43)
        val result = r.readBytes(3)
        assertContentEquals(byteArrayOf(0x41, 0x42, 0x43), result)
        assertEquals(3, r.position)
    }

    @Test
    fun readBytesInsufficientDataThrows() {
        val r = readerOf(0x41, 0x42)
        assertFailsWith<PdfParseException> { r.readBytes(5) }
    }

    // ── isEof ────────────────────────────────────────────────────────────────

    @Test
    fun isEofReturnsFalseMidDataAndTrueAtEnd() {
        val r = readerOf(0x41)
        assertFalse(r.isEof())
        r.readByte()
        assertTrue(r.isEof())
    }

    // ── readLine ─────────────────────────────────────────────────────────────

    @Test
    fun readLineStopsAtLfAndStripsIt() {
        val r = readerOf("hello\nworld")
        assertEquals("hello", r.readLine())
        assertEquals(6, r.position) // "hello" (5) + '\n' (1)
    }

    @Test
    fun readLineStopsAtCrLfAndStripesBoth() {
        val r = readerOf("hello\r\nworld")
        assertEquals("hello", r.readLine())
        assertEquals(7, r.position) // "hello" (5) + '\r' (1) + '\n' (1)
    }

    @Test
    fun readLineAtEofReturnsEmptyString() {
        val r = PdfByteReader(ByteArray(0))
        assertEquals("", r.readLine())
    }

    @Test
    fun readLineOnBlankLineReturnsEmptyString() {
        val r = readerOf("\nhello")
        assertEquals("", r.readLine())
        assertEquals(1, r.position)
    }

    // ── seek backward ────────────────────────────────────────────────────────

    @Test
    fun seekBackwardThenReadReturnsCorrectByte() {
        val r = readerOf(0x41, 0x42, 0x43)
        r.seek(2)
        r.seek(0)
        assertEquals(0x41.toByte(), r.readByte())
    }
}
```

- [ ] **Step 2: Run tests to verify they all fail**

```bash
./gradlew :kparchment:jvmTest
```

Expected: FAIL — `error: unresolved reference: PdfByteReader` (many errors)

- [ ] **Step 3: Create `PdfByteReader.kt`**

```kotlin
package io.kparchment

class PdfByteReader(private val data: ByteArray) {

    var position: Int = 0
        private set

    fun isEof(): Boolean = position == data.size

    fun seek(index: Int) {
        if (index < 0 || index > data.size) {
            throw PdfParseException("seek out of bounds: $index", index.toLong())
        }
        position = index
    }

    fun readByte(): Byte {
        if (isEof()) throw PdfParseException("unexpected EOF", position.toLong())
        return data[position++]
    }

    fun peek(): Byte {
        if (isEof()) throw PdfParseException("unexpected EOF", position.toLong())
        return data[position]
    }

    fun readBytes(n: Int): ByteArray {
        if (position + n > data.size) {
            throw PdfParseException("unexpected EOF reading $n bytes", position.toLong())
        }
        val result = data.copyOfRange(position, position + n)
        position += n
        return result
    }

    fun readLine(): String {
        if (isEof()) return ""
        val sb = StringBuilder()
        while (!isEof()) {
            val b = data[position++]
            val c = b.toInt().toChar()
            when {
                c == '\n' -> return sb.toString()
                c == '\r' -> {
                    if (!isEof() && data[position].toInt().toChar() == '\n') {
                        position++ // consume the \n in \r\n
                    }
                    return sb.toString()
                }
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
```

- [ ] **Step 4: Run all tests to verify they pass**

```bash
./gradlew :kparchment:jvmTest
```

Expected:
```
PdfByteReaderTest > initialPositionIsZero PASSED
PdfByteReaderTest > seekSetsPosition PASSED
PdfByteReaderTest > seekNegativeThrows PASSED
PdfByteReaderTest > seekBeyondSizeThrows PASSED
PdfByteReaderTest > seekToExactEofIsValid PASSED
PdfByteReaderTest > readByteReturnsCorrectByteAndAdvancesPosition PASSED
PdfByteReaderTest > readByteAtEofThrows PASSED
PdfByteReaderTest > peekReturnsByteWithoutAdvancingPosition PASSED
PdfByteReaderTest > peekAtEofThrows PASSED
PdfByteReaderTest > readBytesReturnsSliceAndAdvancesPosition PASSED
PdfByteReaderTest > readBytesInsufficientDataThrows PASSED
PdfByteReaderTest > isEofReturnsFalseMidDataAndTrueAtEnd PASSED
PdfByteReaderTest > readLineStopsAtLfAndStripsIt PASSED
PdfByteReaderTest > readLineStopsAtCrLfAndStripesBoth PASSED
PdfByteReaderTest > readLineAtEofReturnsEmptyString PASSED
PdfByteReaderTest > readLineOnBlankLineReturnsEmptyString PASSED
PdfByteReaderTest > seekBackwardThenReadReturnsCorrectByte PASSED
PdfByteReaderTest > pdfParseExceptionCarriesMessageAndOffset PASSED

BUILD SUCCESSFUL
```

- [ ] **Step 5: Run JS tests**

```bash
./gradlew :kparchment:jsTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add kparchment/src/commonMain/kotlin/io/kparchment/PdfByteReader.kt \
        kparchment/src/commonTest/kotlin/io/kparchment/PdfByteReaderTest.kt
git commit -m "feat: implement PdfByteReader with full test coverage"
```
