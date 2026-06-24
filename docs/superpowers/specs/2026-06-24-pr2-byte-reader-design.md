# PR2 PdfByteReader Design

**Date:** 2026-06-24
**Scope:** Seekable `ByteArray` abstraction and typed parse exception. Foundation for all parsing layers above. No platform IO — pure `commonMain`.

---

## Goal

Implement `PdfByteReader` and `PdfParseException` in `commonMain`. These are the lowest layer of the kparchment stack — every parser above depends on them. No `expect/actual`, no platform code.

---

## Files

Both files in `kparchment/src/commonMain/kotlin/io/kparchment/`:

| File | Responsibility |
|---|---|
| `PdfParseException.kt` | Typed exception carrying byte offset + message |
| `PdfByteReader.kt` | Seekable wrapper over a `ByteArray` |

Test file: `kparchment/src/commonTest/kotlin/io/kparchment/PdfByteReaderTest.kt`

---

## `PdfParseException`

```kotlin
class PdfParseException(
    message: String,
    val byteOffset: Long
) : Exception(message)
```

- `byteOffset` is `Long` to match the final PR13 signature — no breaking change later.
- All parsing layers throw this. Raw platform exceptions must never escape the public API.

---

## `PdfByteReader`

```kotlin
class PdfByteReader(private val data: ByteArray) {

    var position: Int = 0
        private set

    fun seek(index: Int): Unit
    fun readByte(): Byte
    fun readBytes(n: Int): ByteArray
    fun readLine(): String
    fun peek(): Byte
    fun isEof(): Boolean
}
```

`position` is publicly readable (useful for error reporting) but privately settable.

### Approach

Plain class with mutable `position`. Alternatives considered:
- **Interface + implementation** — rejected (YAGNI; only one implementation needed for PR2–PR13)
- **Extension functions on `ByteArray`** — rejected (callers would thread position manually through every call, making the parser above it painful)

### Behavior

| Method | Normal | Error |
|---|---|---|
| `seek(index)` | Sets `position = index` | `index < 0` or `index > data.size` → `PdfParseException("seek out of bounds: $index", index.toLong())` |
| `readByte()` | Returns `data[position]`, increments `position` | At EOF → `PdfParseException("unexpected EOF", position.toLong())` |
| `readBytes(n)` | Returns `data[position..<position+n]`, advances by `n` | Fewer than `n` bytes remain → `PdfParseException("unexpected EOF reading $n bytes", position.toLong())` |
| `peek()` | Returns `data[position]`, position unchanged | At EOF → `PdfParseException("unexpected EOF", position.toLong())` |
| `readLine()` | Reads until `\n`, `\r\n`, or EOF; returns string without terminator | At EOF → returns `""` (no exception) |
| `isEof()` | Returns `position == data.size` | Never throws |

### `readLine()` detail

Reads byte-by-byte into a `StringBuilder`. Stops at:
- `\n` — consumed, not included in result
- `\r\n` — both consumed, not included in result
- EOF — returns accumulated bytes as string

Returns `""` if called at EOF or if the line is blank (terminator immediately).

Both line endings must be handled — the PDF spec allows both throughout the file.

### `seek()` boundary

`seek(data.size)` is valid — seeking to exactly EOF is legal (position at EOF, `isEof()` returns true). Only `index > data.size` throws.

---

## Tests

All in `commonTest/kotlin/io/kparchment/PdfByteReaderTest.kt`.

### `PdfByteReader`

1. Fresh reader has `position == 0`
2. `seek(n)` sets `position` to `n`
3. `seek(-1)` throws `PdfParseException`
4. `seek(data.size + 1)` throws `PdfParseException`
5. `seek(data.size)` is valid (EOF boundary — does not throw)
6. `readByte()` returns correct byte and increments `position`
7. `readByte()` at EOF throws `PdfParseException`
8. `peek()` returns correct byte without advancing `position`
9. `peek()` at EOF throws `PdfParseException`
10. `readBytes(3)` returns correct slice and advances `position` by 3
11. `readBytes(n)` when fewer than `n` bytes remain throws `PdfParseException`
12. `isEof()` returns `false` mid-data, `true` at `position == data.size`
13. `readLine()` stops at `\n`, returns content without terminator
14. `readLine()` stops at `\r\n`, returns content without terminator
15. `readLine()` at EOF returns `""`
16. `readLine()` on a blank line returns `""`
17. Seek backward then read returns bytes from new position

### `PdfParseException`

18. `message` is accessible and `byteOffset` matches the position at time of throw

---

## Out of Scope

- `expect/actual` — no platform IO in this PR
- Streaming (non-`ByteArray`) readers — future concern
- Character encoding — `readLine()` treats bytes as Latin-1; full encoding is PR10
