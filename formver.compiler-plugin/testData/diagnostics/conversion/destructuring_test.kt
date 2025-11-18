// Test for destructuring declarations

import org.jetbrains.kotlin.formver.plugin.verify

data class Point(val x: Int, val y: Int)

data class Triple(val a: Int, val b: Int, val c: Int)

fun <!VIPER_TEXT!>testSimpleDestructuring<!>() {
    val point = Point(3, 4)
    val (x, y) = point
}

fun <!VIPER_TEXT!>testTripleDestructuring<!>() {
    val triple = Triple(1, 2, 3)
    val (a, b, c) = triple
}

