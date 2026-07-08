// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.Borrowed
import org.jetbrains.kotlin.formver.plugin.Unique

fun nondet(): Boolean {
    return false
}

fun consume(x: @Unique Any) {}

fun `assign local after accessing it as statement`(x: @Unique Any) {
    x
    val y: @Unique Any = x
}

// Var assignments

fun `assign shared`(x: Any) {
    var y: Any

    y = x

    consume(<!UNIQUENESS_MISMATCH!>y<!>)
}

fun `assign borrowed`(x: @Borrowed Any) {
    var y: @Borrowed Any

    y = x

    consume(<!LOCALITY_MISMATCH, UNIQUENESS_MISMATCH!>y<!>)
}

fun `assign unique`(x: @Unique Any) {
    var y: @Unique Any

    y = x

    consume(y)
}

fun `assign unique-borrowed`(x: @Unique @Borrowed Any) {
    var y: @Unique @Borrowed Any

    y = x

    consume(<!LOCALITY_MISMATCH!>y<!>)
}

// Var declarations

fun `assign shared in declaration`(x: Any) {
    var y = x

    consume(<!UNIQUENESS_MISMATCH!>y<!>)
}

fun `assign borrowed in declaration`(x: @Borrowed Any) {
    var y: @Borrowed Any = x

    consume(<!LOCALITY_MISMATCH, UNIQUENESS_MISMATCH!>y<!>)
}

fun `assign unique in unique declaration`(x: @Unique Any) {
    var y: @Unique Any = x

    consume(y)
}

fun `assign unique in shared declaration`(x: @Unique Any) {
    var y = x

    consume(y)
}

fun `assign unique-borrowed in borrowed declaration`(x: @Unique @Borrowed Any) {
    var y: @Borrowed Any = x

    consume(<!LOCALITY_MISMATCH, UNIQUENESS_MISMATCH!>y<!>)
}

fun `assign unique-borrowed in unique-borrowed declaration`(x: @Unique @Borrowed Any) {
    var y: @Unique @Borrowed Any = x

    consume(<!LOCALITY_MISMATCH!>y<!>)
}

// Assignment chaining

fun `chain shared assignments`(x: Any) {
    var y: Any
    var z: Any

    y = x
    z = y

    consume(<!UNIQUENESS_MISMATCH!>z<!>)
}

fun `chain borrowed assignments`(x: @Borrowed Any) {
    var y: @Borrowed Any
    var z: @Borrowed Any

    y = x
    z = y

    consume(<!LOCALITY_MISMATCH, UNIQUENESS_MISMATCH!>z<!>)
}

fun `chain unique assignments`(x: @Unique Any) {
    var y: @Unique Any
    var z: @Unique Any

    y = x
    z = y

    consume(z)
}

fun `chain unique-borrowed assignments`(x: @Unique @Borrowed Any) {
    var y: @Unique @Borrowed Any
    var z: @Unique @Borrowed Any

    y = x
    z = y

    consume(<!LOCALITY_MISMATCH!>z<!>)
}

// Conditional assignments

fun `assign unique or shared`(x: @Unique Any, y: Any) {
    var z: Any;

    if (nondet()) {
        z = x
    } else {
        z = y
    }

    consume(<!UNIQUENESS_MISMATCH!>z<!>)
}

fun `move unique and then reassign`(x: @Unique Any) {
    var y = x
    var z = y
    y = 1
    consume(y)
}

// Looping assignments

fun `assign unique to shared in loop`(x: @Unique Any, y: @Unique Any) {
    var z = y;

    while (nondet()) {
        z = <!INVALID_MOVED_ACCESS!>x<!>
    }

    consume(z)
}

fun `assign local in if`(x: @Unique Any) {
    val y: @Unique Any = if (false) {
        val y: @Unique Any = x
        y
    } else {
        x
    }
}
