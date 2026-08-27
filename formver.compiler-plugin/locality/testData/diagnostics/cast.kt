// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A

fun `assign local cast-expression to global`(x: @Borrowed Any) {
    var z: String = <!LOCALITY_MISMATCH!>x as String<!>
}

fun `assign local safe-cast-expression to global`(x: @Borrowed Any) {
    var z: String? = <!LOCALITY_MISMATCH!>x as? String<!>
}

fun `assign local not-null-expression to global`(x: @Borrowed Any?) {
    var z: Any = <!LOCALITY_MISMATCH!>x!!<!>
}

fun `assign local to implicit local`(x: @Borrowed Any) {
    var a = x as A
}

fun `assign local cast to implicit global`(x: @Borrowed Any, y: A) {
    var a = y

    a = <!LOCALITY_MISMATCH!>x as A<!>
}

fun `assign implicit local to explicit global`(x: @Borrowed Any) {
    var a = x as A

    var b: Any = <!LOCALITY_MISMATCH!>a<!>
}

fun `assign global to implicit local`(x: @Borrowed Any, y: A) {
    var a = x as A

    a = y
}

fun `assign local smartcast expression to local`(x: @Borrowed Any) {
    var a: @Borrowed A
    var b: @Borrowed A = A()

    a = when (x) {
        is A -> x
        else -> b
    }
}

fun `assign global smartcast expression to local`(x: Any) {
    var a: @Borrowed A
    var b: @Borrowed A = A()

    a = when (x) {
        is A -> x
        else -> A()
    }
}

fun `assign local smartcast expression to global`(x: @Borrowed Any) {
    var a: A
    var b: @Borrowed A = A()

    a = <!LOCALITY_MISMATCH!>when (x) {
        is A -> x
        else -> b
    }<!>
}
