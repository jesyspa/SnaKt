// LOCALITY_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed

class A(
    var x: Any
)

class B(
    var x: Any
)

fun `assign local if-expression to local`(x: @Borrowed Any) {
    var z: @Borrowed Any = if (false) { x } else { Any() }
}

fun `assign local if-expression to global`(x: @Borrowed Any) {
    var z: Any = <!LOCALITY_MISMATCH!>if (false) { x } else { Any() }<!>
}

fun `assign local if-expression to local in loop`(x: @Borrowed Any) {
    var z: @Borrowed Any = Any()

    while (true) {
        z = if (false) { x } else { Any() }
    }
}

fun `assign local when-expression to local`(x: @Borrowed Any) {
    var z: @Borrowed Any = when {
        false -> x
        else -> Any()
    }
}

fun `assign local when-expression to global`(x: @Borrowed Any) {
    var z: Any = <!LOCALITY_MISMATCH!>when {
        false -> x
        else -> Any()
    }<!>
}

fun `assign local when-expression to global in loop`(x: @Borrowed Any) {
    var z: Any = Any()

    while (true) {
        z = <!LOCALITY_MISMATCH!>when {
            false -> x
            else -> Any()
        }<!>
    }
}

fun `assign local try-expression to global`(x: @Borrowed Any) {
    var z: Any = <!LOCALITY_MISMATCH!>try {
        x
    } catch (_: Throwable) {
        Any()
    }<!>
}

fun `assign local try-expression to global in loop`(x: @Borrowed Any) {
    var z: Any = Any()

    while (true) {
        z = <!LOCALITY_MISMATCH!>try {
            x
        } catch (_: Throwable) {
            Any()
        }<!>
    }
}

fun `assign local nested control-flow to global in loop`(x: @Borrowed Any) {
    var z: Any = Any()

    while (true) {
        z = <!LOCALITY_MISMATCH!>if (false) {
            when {
                false -> x
                else -> Any()
            }
        } else {
            Any()
        }<!>

        z = <!LOCALITY_MISMATCH!>try {
            if (false) { x } else { Any() }
        } catch (_: Throwable) {
            Any()
        }<!>
    }
}

fun `assign local nested control-flow to local in loop`(x: @Borrowed Any) {
    var z: @Borrowed Any = Any()

    while (true) {
        z = when {
            false -> if (false) { x } else { Any() }
            else -> x
        }
    }
}

fun `assign local property if-expression to global`(x: @Borrowed A, y: A) {
    var z: Any = if (false) { x.x } else { y.x }
}

fun `assign local property when-expression to global in loop`(x: @Borrowed A, y: A) {
    var z: Any = Any()

    while (true) {
        z = when {
            false -> x.x
            else -> y.x
        }
    }
}

fun `assign global property when-expression to global in loop`(x: A, y: A) {
    var z: Any = Any()

    while (true) {
        z = when {
            false -> x.x
            else -> y.x
        }
    }
}

fun `assign local property try-expression to global`(x: @Borrowed A, y: A) {
    var z: Any = try {
        x.x
    } catch (_: Throwable) {
        y.x
    }
}

fun `assign local property if-expression to global property in loop`(x: B, y: @Borrowed B, z: B) {
    while (true) {
        x.x = if (false) { y.x } else { z.x }
    }
}

fun `assign local property when-expression to local property in loop`(x: @Borrowed B, y: B, z: @Borrowed B) {
    while (true) {
        x.x = when {
            false -> y.x
            else -> z.x
        }
    }
}

fun `assign local property try-expression to global property`(x: B, y: @Borrowed B, z: B) {
    x.x = try {
        y.x
    } catch (_: Throwable) {
        z.x
    }
}

fun `assign local property try-expression to local property`(x: @Borrowed B, y: @Borrowed B, z: B) {
    x.x = try {
        y.x
    } catch (_: Throwable) {
        z.x
    }
}
