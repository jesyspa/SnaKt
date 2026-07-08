// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*

fun <!VIPER_TEXT!>simpleExists<!>(s: String): Int {
    preconditions { s.length > 0 }
    postconditions<Int> {
        exists<Int> { 0 <= it && it < s.length }
    }
    return 0
}
