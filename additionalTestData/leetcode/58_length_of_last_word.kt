// FULL_JDK
import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>lengthOfLastWord<!>(s: String): Int {
    preconditions {
        s.length > 0
        forAll<Int> { i ->
            (0 <= i && i < s.length) implies
                    (s[i] == ' ' || ('a' <= s[i] && s[i] <= 'z') ||
                            ('A' <= s[i] && s[i] <= 'Z'))
        }
        exists<Int> { i ->
            0 <= i && i < s.length && s[i] != ' '
        }
    }
    postconditions<Int> { result ->
        result > 0 && result <= s.length
        exists<Int> { end ->
            result <= end && end <= s.length &&
                    s[end - 1] != ' ' &&
                    forAll<Int> { i ->
                        (end <= i && i < s.length) implies (s[i] == ' ')
                    } &&
                    forAll<Int> { i ->
                        (end - result <= i && i < end) implies (s[i] != ' ')
                    } &&
                    (end - result == 0 || s[end - result - 1] == ' ')
        }
    }

    var end = s.length
    while (end > 0 && s[end - 1] == ' ') {
        loopInvariants {
            0 <= end && end <= s.length
            forAll<Int> { i ->
                (end <= i && i < s.length) implies (s[i] == ' ')
            }
            exists<Int> { i ->
                0 <= i && i < end && s[i] != ' '
            }
        }
        --end
    }

    var start = end
    while (start > 0 && s[start - 1] != ' ') {
        loopInvariants {
            0 <= start && start <= end && end <= s.length
            end > 0 && s[end - 1] != ' '
            forAll<Int> { i ->
                (end <= i && i < s.length) implies (s[i] == ' ')
            }
            forAll<Int> { i ->
                (start <= i && i < end) implies (s[i] != ' ')
            }
        }
        --start
    }

    return end - start
}
