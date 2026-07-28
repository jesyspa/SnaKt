// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

fun `assign global to local`(x: Any) {
    var y: @Borrowed Any

    y = x
}

fun `assign local to global`(x: @Borrowed Any) {
    var y: Any

    y = <!LOCALITY_MISMATCH!>x<!>
}

fun `assign local to local`(x: @Borrowed Any) {
    var y: @Borrowed Any

    y = x
}

fun `assign local then global to local`(x: @Borrowed Any, y: Any) {
    var z: @Borrowed Any

    z = x
    z = y
}

fun `assign local to local in loop`(x: @Borrowed Any) {
    var z: @Borrowed Any

    while (true) {
        z = x
    }
}

fun `assign recursively`() {
    var a = <!UNRESOLVED_REFERENCE!>a<!>
}

val `recursive property` = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>`recursive property`<!>

fun `assign recursive implicit property`() {
    var a: Any

    a = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>`recursive property`<!>
}
