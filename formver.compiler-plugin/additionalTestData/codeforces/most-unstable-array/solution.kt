import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.Pure
import org.jetbrains.kotlin.formver.plugin.implies
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

/** Returns the greatest possible sum of adjacent differences for one test case. */
@Pure
@AlwaysVerify
fun maximumInstability(n: Int, m: Int): Int {
    preconditions {
        1 <= n && n <= 1_000_000_000
        1 <= m && m <= 1_000_000_000
    }
    // One element has no edge, two elements have one edge of size at most m,
    // and three or more elements can realize two such edges around an endpoint.
    postconditions<Int> { result ->
        (n == 1) implies (result == 0)
        (n == 2) implies (result == m)
        (n >= 3) implies (result == 2 * m)
    }

    return when (n) {
        1 -> 0
        2 -> m
        else -> 2 * m
    }
}
