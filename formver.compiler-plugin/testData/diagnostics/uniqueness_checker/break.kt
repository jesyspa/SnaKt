// CHECKERS_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun makeUnique(): @Unique Any? = null

fun `break conforms to unique`() {
    while (true) {
        val x: @Unique Nothing = break
    }
}

fun `break in elvis conforms to unique`() {
    while (true) {
        val x: @Unique Any = makeUnique() ?: break
    }
}

fun `break conforms to shared`() {
    while (true) {
        val x: Nothing = break
    }
}

fun `break in elvis conforms to shared`() {
    while (true) {
        val x: Any = makeUnique() ?: break
    }
}
