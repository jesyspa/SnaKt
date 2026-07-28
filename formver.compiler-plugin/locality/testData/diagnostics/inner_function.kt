// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun outer(x: @Borrowed Any) {
    fun named() { x }
}
