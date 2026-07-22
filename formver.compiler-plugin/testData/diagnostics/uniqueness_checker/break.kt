// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun `break conforms to unique`() {
    while (true) {
        val x: @Unique Nothing = break
    }
}

fun `break conforms to shared`() {
    while (true) {
        val x: Nothing = break
    }
}
