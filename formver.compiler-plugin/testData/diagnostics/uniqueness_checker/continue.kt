// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun `continue conforms to unique`() {
    while (true) {
        val x: @Unique Nothing = continue
    }
}
