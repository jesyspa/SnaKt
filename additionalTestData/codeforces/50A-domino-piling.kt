import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.postconditions
import org.jetbrains.kotlin.formver.plugin.preconditions

/** Pure solution for Codeforces 50A; see 50A-domino-piling.md. */
@AlwaysVerify
fun maximumDominoes(rows: Int, columns: Int): Int {
    preconditions {
        1 <= rows && rows <= 16
        rows <= columns && columns <= 16
    }
    postconditions<Int> { result ->
        result == rows * columns / 2
        0 <= result
        2 * result <= rows * columns
        rows * columns - 2 * result < 2
    }

    return rows * columns / 2
}
