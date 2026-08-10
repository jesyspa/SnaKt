// FULL_JDK
// WITH_STDLIB

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify

@AlwaysVerify
fun <!VIPER_TEXT!>size_read_after_loop<!>(a: List<Int>, b: List<Int>): Int {
    var i = 0
    while (i < a.size) {
        i = i + 1
    }
    return b.size
}

// The loop invariant asks for full permission to the size of both `l` and its alias `m`, which is twice
// the permission the caller can hold. Method parameters of the same type have the same limitation.
@AlwaysVerify
fun <!VIPER_TEXT!>size_of_aliased_list<!>(l: List<Int>): Int {
    val m = l
    var i = 0
    <!VIPER_VERIFICATION_ERROR!>while (i < m.size) {
        i = i + 1
    }<!>
    return i
}
