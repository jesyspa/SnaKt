// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

class Box(@Unique val a: Any)

fun f_unique(@Unique box: Box) {}
fun f_borrowed(@Borrowed box: Box) {}
fun f_unique_borrowed(@Unique @Borrowed box: Box) {}

