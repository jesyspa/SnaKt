import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun integerSquareRoot(x: Int): Int {
    preconditions {
        0 <= x
    }
    postconditions<Int> { root ->
        0 <= root && root <= 46340
        root * root <= x
        root == 46340 || x < (root + 1) * (root + 1)
    }

    var root = 0
    while (root < 46340 && (root + 1) * (root + 1) <= x) {
        loopInvariants {
            0 <= root && root <= 46340
            root * root <= x
        }
        root += 1
    }
    return root
}
