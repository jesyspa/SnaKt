// CHECKERS_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun makeUnique(): @Unique Any? = null

fun `continue conforms to unique`() {
    while (true) {
        val x: @Unique Nothing = continue
    }
}

fun `continue in elvis conforms to unique`() {
    while (true) {
        val x: @Unique Any = makeUnique() ?: continue
    }
}

fun `continue conforms to shared`() {
    while (true) {
        val x: Nothing = continue
    }
}

fun `continue in elvis conforms to shared`() {
    while (true) {
        val x: Any = makeUnique() ?: continue
    }
}
