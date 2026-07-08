// FULL_JDK

import org.jetbrains.kotlin.formver.plugin.*


// `m` is the maximum character of `s` and actually occurs in it: the existential
// quantifier is the dual of `forAll` and is required to state that the maximum is
// attained. The loop invariant carries the witness forward so the postcondition holds.
fun <!VIPER_TEXT!>maxCharacter<!>(s: String): Char {
    preconditions {
        s.length > 0
    }
    postconditions<Char> { m ->
        forAll<Int> { (0 <= it && it < s.length) implies (s[it] <= m) } &&
                exists<Int> { 0 <= it && it < s.length && s[it] == m }
    }

    var m = s[0]
    var i = 1
    while (i < s.length) {
        loopInvariants {
            1 <= i && i <= s.length
            forAll<Int> { (0 <= it && it < i) implies (s[it] <= m) }
            exists<Int> { 0 <= it && it < i && s[it] == m }
        }
        if (s[i] > m) {
            m = s[i]
        }
        i += 1
    }
    return m
}
