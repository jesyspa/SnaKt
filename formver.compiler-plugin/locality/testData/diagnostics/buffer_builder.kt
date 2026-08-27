// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class Buffer

fun @Borrowed Buffer.append(text: Any) {}

// Lends a scoped buffer to `action` for the duration of the call; the buffer must not escape.
fun format(action: (@Borrowed Buffer) -> Unit): Any {
    val buffer = Buffer()
    action(buffer)
    return Any()
}

// Runs `action` synchronously and never retains it, so it may capture borrowed state.
fun withScope(action: @Borrowed () -> Unit) {
    action()
}

fun writeHeader(buffer: @Borrowed Buffer) {
    buffer.append("header")
}

fun writeFooter(buffer: @Borrowed Buffer) {
    buffer.append("footer")
}

var savedBuffer: Buffer? = null

fun `build a document from borrowed buffer operations`() {
    format { buffer ->
        writeHeader(buffer)
        buffer.append("body")
        writeFooter(buffer)
    }
}

fun `capture the borrowed buffer in a nested scope`() {
    format { buffer ->
        buffer.append("start")
        withScope {
            writeFooter(buffer)
        }
    }
}

fun `leak the buffer out of its builder scope`() {
    format { buffer ->
        savedBuffer = <!LOCALITY_MISMATCH!>buffer<!>
    }
}
