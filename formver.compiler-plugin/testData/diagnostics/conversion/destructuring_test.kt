// Test for destructuring declarations

import org.jetbrains.kotlin.formver.plugin.AlwaysVerify
import org.jetbrains.kotlin.formver.plugin.verify

data class Point(val x: Int, val y: Int)

data class Triple(val a: Int, val b: Int, val c: Int)

@AlwaysVerify
fun <!VIPER_TEXT!>testSimpleDestructuring<!>() {
    val point = Point(3, 4)
    val (x, y) = point
    verify(x == 3)
    verify(y == 4)
}

@AlwaysVerify
fun <!VIPER_TEXT!>testTripleDestructuring<!>() {
    val triple = Triple(1, 2, 3)
    val (a, b, c) = triple
    val sum = a + b + c
    verify(sum == 6)
}

@AlwaysVerify
fun <!VIPER_TEXT!>testDestructuringInLoop<!>() {
    val point = Point(5, 10)
    val (x, y) = point
    var counter = 0
    while (counter < x) {
        counter = counter + 1
    }
    verify(counter == 5)
}
