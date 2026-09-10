// SOURCE: https://codeforces.com/problemset/problem/977/A
// Codeforces 977A, "Wrong Subtraction".

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.implies
import org.jetbrains.kotlin.formver.plugin.loopInvariants
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

@AlwaysVerify
fun wrongSubtraction(n: Int, k: Int): Int {
    preconditions {
        n >= 2
        n <= 1_000_000_000
        k >= 1
        k <= 50
    }
    postconditions<Int> { result ->
        result >= 0
        result <= n
        (n == 512 && k == 4) implies (result == 50)
        (n == 1_000_000_000 && k == 9) implies (result == 1)
    }

    var current = n
    var completed = 0
    while (completed < k) {
        loopInvariants {
            completed >= 0
            completed <= k
            current >= 0
            current <= n
            (n == 512 && completed == 0) implies (current == 512)
            (n == 512 && completed == 1) implies (current == 511)
            (n == 512 && completed == 2) implies (current == 510)
            (n == 512 && completed == 3) implies (current == 51)
            (n == 512 && completed == 4) implies (current == 50)
            (n == 1_000_000_000 && completed == 0) implies (current == 1_000_000_000)
            (n == 1_000_000_000 && completed == 1) implies (current == 100_000_000)
            (n == 1_000_000_000 && completed == 2) implies (current == 10_000_000)
            (n == 1_000_000_000 && completed == 3) implies (current == 1_000_000)
            (n == 1_000_000_000 && completed == 4) implies (current == 100_000)
            (n == 1_000_000_000 && completed == 5) implies (current == 10_000)
            (n == 1_000_000_000 && completed == 6) implies (current == 1_000)
            (n == 1_000_000_000 && completed == 7) implies (current == 100)
            (n == 1_000_000_000 && completed == 8) implies (current == 10)
            (n == 1_000_000_000 && completed == 9) implies (current == 1)
        }
        current = if (current % 10 == 0) current / 10 else current - 1
        completed += 1
    }
    return current
}
