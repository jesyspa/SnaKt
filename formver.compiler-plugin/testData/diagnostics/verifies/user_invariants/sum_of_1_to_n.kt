import org.jetbrains.kotlin.formver.plugin.*

@AlwaysVerify
fun <!VIPER_TEXT!>recursiveSumOfIntegersUpToN<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { res -> res == n * (n + 1) / 2 }

    if (n == 0) return 0
    else return n + recursiveSumOfIntegersUpToN(n - 1)
}

@AlwaysVerify
fun <!VIPER_TEXT!>sumOfIntegersUpToN<!>(n: Int): Int {
    preconditions { n >= 0 }
    postconditions<Int> { res -> res == n * (n + 1) / 2 }

    var sum = 0
    var i = 0
    while (i < n) {
        loopInvariants {
            i <= n
            sum == i * (i + 1) / 2
        }
        sum += i + 1
        ++i
    }
    return sum
}